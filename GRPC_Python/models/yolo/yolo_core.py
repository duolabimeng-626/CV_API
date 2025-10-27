import time
import threading
import os
from collections import OrderedDict
from typing import List, Tuple, Optional

import cv2
import numpy as np
import torch
from ultralytics import YOLO

# 修复 PyTorch 2.6 兼容性问题
def patch_torch_load():
    """修复 PyTorch 2.6 的 weights_only 问题"""
    original_load = torch.load
    def patched_load(*args, **kwargs):
        if 'weights_only' not in kwargs:
            kwargs['weights_only'] = False
        return original_load(*args, **kwargs)
    torch.load = patched_load

# 应用补丁
patch_torch_load()


class SessionEntry:
    """单个会话条目：保存模型实例与最近使用时间。
    说明：保留与原 Flask 版本一致的结构，便于 LRU 淘汰与 CUDA 显存回收。
    """
    def __init__(self, model: YOLO, device: str):
        self.model = model
        self.device = device
        self.last_used = time.time()


class SessionManager:
    """线程安全 LRU 会话管理器。
    - 用 OrderedDict 保存会话，最近使用的移动到末尾；超出容量时从头部淘汰。
    - 与你原代码一致，便于直接迁移。
    """
    def __init__(self, max_sessions: int = 4):
        self.max_sessions = max_sessions
        self._lock = threading.Lock()
        self._sessions: "OrderedDict[str, SessionEntry]" = OrderedDict()

    def get(self, session_id: str) -> Optional[SessionEntry]:
        with self._lock:
            entry = self._sessions.get(session_id)
            if entry:
                entry.last_used = time.time()
                self._sessions.move_to_end(session_id)
            return entry

    def put(self, session_id: str, entry: SessionEntry):
        with self._lock:
            self._sessions[session_id] = entry
            self._sessions.move_to_end(session_id)
            self._evict_if_needed_locked()

    def touch(self, session_id: str):
        with self._lock:
            if session_id in self._sessions:
                self._sessions[session_id].last_used = time.time()
                self._sessions.move_to_end(session_id)

    def remove(self, session_id: str):
        with self._lock:
            self._sessions.pop(session_id, None)

    def _evict_if_needed_locked(self):
        # 超过容量时，按 LRU 淘汰最久未使用的会话，并尝试释放 CUDA 显存
        while len(self._sessions) > self.max_sessions:
            sid, entry = self._sessions.popitem(last=False)
            try:
                del entry.model
                if torch.cuda.is_available():
                    torch.cuda.empty_cache()
                print(f"[LRU] 会话 {sid} 已淘汰并释放资源")
            except Exception as e:
                print(f"[LRU] 淘汰会话 {sid} 释放资源异常: {e}")


class YoloCore:
    """封装 YOLO 推理的核心逻辑：加载、预处理、推理、后处理、绘制。
    该类“无 gRPC 依赖”，便于被 Flask 或 gRPC 调用层复用。
    """
    def __init__(self, model_path: str, device: Optional[str] = None, max_sessions: int = 4):
        self.model_path = model_path

        self.device = device or ("cuda" if torch.cuda.is_available() else "mps" if torch.backends.mps.is_available() else "cpu")
        self.model: Optional[YOLO] = None
        self.model_lock = threading.Lock()  # 控制同一时刻的 GPU 推理并发（按需）
        self.session_mgr = SessionManager(max_sessions=max_sessions)
        self.is_initialized = False
        self._load_model()

    def _load_model(self):
        """加载主模型并完成一次 warmup，降低首个请求时延。"""
        try:
            print(f"[YOLO] 加载模型 {self.model_path} 到设备 {self.device}")
            self.model = YOLO(self.model_path)
            # warmup：跑一次小输入，初始化权重与 CUDA kernel
            dummy = np.zeros((320, 320, 3), dtype=np.uint8)
            with torch.inference_mode():
                _ = self.model.track(dummy, persist=False, verbose=False, device=self.device)
            self.is_initialized = True
            print("[YOLO] ✅ 模型加载成功")
        except Exception as e:
            print(f"[YOLO] ❌ 模型加载失败: {e}")
            self.is_initialized = False

    # ========== 会话相关 ==========
    def _get_model_for_session(self, session_id: Optional[str]) -> YOLO:
        """根据 session_id 选择是复用主模型，还是为该会话创建专属实例。"""
        if not session_id:
            return self.model
        entry = self.session_mgr.get(session_id)
        if entry:
            return entry.model
        # 首次访问该会话：懒加载一个新实例，并立即 warmup
        try:
            m = YOLO(self.model_path)
            dummy = np.zeros((320, 320, 3), dtype=np.uint8)
            with torch.inference_mode():
                _ = m.track(dummy, persist=False, verbose=False, device=self.device)
            entry = SessionEntry(model=m, device=self.device)
            self.session_mgr.put(session_id, entry)
            return entry.model
        except Exception as e:
            print(f"[YOLO] 创建会话模型失败 {session_id}: {e}")
            return self.model

    # ========== 预处理/后处理 ==========
    def resize_for_yolo(self, image: np.ndarray, target_size=(640, 640), keep_aspect_ratio=True):
        """将图像调整为适合 YOLO 的大小，并记录缩放/填充信息，便于还原坐标。
        返回：resized_image, resize_info(dict)
        """
        try:
            h, w = image.shape[:2]
            if keep_aspect_ratio:
                scale = min(target_size[0] / w, target_size[1] / h)
                nw, nh = int(w * scale), int(h * scale)
                resized = cv2.resize(image, (nw, nh), interpolation=cv2.INTER_LINEAR)
                canvas = np.zeros((target_size[1], target_size[0], 3), dtype=np.uint8)
                y_off = (target_size[1] - nh) // 2
                x_off = (target_size[0] - nw) // 2
                canvas[y_off:y_off + nh, x_off:x_off + nw] = resized
                resize_info = {
                    "original_size": (w, h),
                    "resized_size": (nw, nh),
                    "target_size": target_size,
                    "scale": scale,
                    "padding": (x_off, y_off),
                    "method": "aspect_ratio_preserved",
                }
                return canvas, resize_info
            else:
                resized = cv2.resize(image, target_size, interpolation=cv2.INTER_LINEAR)
                resize_info = {
                    "original_size": (w, h),
                    "resized_size": target_size,
                    "target_size": target_size,
                    "scale": None,
                    "padding": (0, 0),
                    "method": "direct_resize",
                }
                return resized, resize_info
        except Exception as e:
            print(f"[YOLO] 图像尺寸调整失败: {e}")
            return image, None

    def scale_boxes_to_original(self, boxes_xyxy: List[List[float]], resize_info: dict, original_shape) -> List[List[float]]:
        """将 xyxy 框坐标从网络输入大小还原为原图像素坐标。"""
        try:
            if not resize_info or resize_info.get("method") != "aspect_ratio_preserved":
                return boxes_xyxy
            H, W = original_shape[:2]
            target_h, target_w = resize_info["target_size"][1], resize_info["target_size"][0]
            scale = resize_info["scale"]
            x_off, y_off = resize_info["padding"]

            scaled = []
            for x1, y1, x2, y2 in boxes_xyxy:
                x1 = (x1 - x_off) / scale
                y1 = (y1 - y_off) / scale
                x2 = (x2 - x_off) / scale
                y2 = (y2 - y_off) / scale
                # 裁剪到原图范围
                x1 = max(0, min(x1, W)); y1 = max(0, min(y1, H))
                x2 = max(0, min(x2, W)); y2 = max(0, min(y2, H))
                scaled.append([x1, y1, x2, y2])
            return scaled
        except Exception as e:
            print(f"[YOLO] 坐标还原失败: {e}")
            return boxes_xyxy

    def draw_boxes_on_image(self, image: np.ndarray, boxes: List[List[float]], classes: List[int], confidences: List[float], tids: Optional[List[int]] = None):
        """在原图上绘制检测框和标签，返回叠加后的 BGR 图像。"""
        try:
            annotated = image.copy()
            for i, (box, cls_id, conf) in enumerate(zip(boxes, classes, confidences)):
                x1, y1, x2, y2 = map(int, box)
                name = self.model.names[int(cls_id)] if self.model else str(cls_id)
                color = (0, 255, 0)
                cv2.rectangle(annotated, (x1, y1), (x2, y2), color, 2)
                tid = int(tids[i]) if tids is not None else None
                label = f"ID:{tid} {name} {conf:.2f}" if tid is not None else f"{name} {conf:.2f}"
                (tw, th), bl = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 1)
                cv2.rectangle(annotated, (x1, y1 - th - bl - 5), (x1 + tw, y1), color, -1)
                cv2.putText(annotated, label, (x1, y1 - bl - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)
            return annotated
        except Exception as e:
            print(f"[YOLO] 绘制检测框失败: {e}")
            return image

    # ========== 推理入口 ==========
    def detect_or_track(self, image_bgr: np.ndarray, session_id: Optional[str], persist: bool, mode: str = "detect"):
        """根据 mode 决定调用 predict 或 track；并根据 session_id 选择模型实例。"""
        if not self.is_initialized:
            raise RuntimeError("YOLO 模型未初始化")
        model_to_use = self._get_model_for_session(session_id)
        with self.model_lock, torch.inference_mode():
            if mode in ("detect_track", "track"):
                results = model_to_use.track(image_bgr, persist=persist, verbose=False, device=self.device)
            else:
                results = model_to_use.predict(image_bgr, verbose=False, device=self.device)
        if session_id:
            self.session_mgr.touch(session_id)
        return results
