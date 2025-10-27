import os
import time
import signal
import sys
import logging
from contextvars import ContextVar
from concurrent import futures

import cv2
import grpc
import numpy as np
from google.protobuf import struct_pb2, any_pb2

# 导入由 protoc 生成的 Python 代码（确保已编译到 proto_gen 目录）
sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "gen"))
from gen import ai_pb2 as pb, ai_pb2_grpc as pb_grpc

from .yolo_core import YoloCore
from .yolo_dto import YoloDetectionDTO, YoloResultDTO, YoloResponseDTO, YoloDTOFactory

# 导入配置管理器
sys.path.append(os.path.join(os.path.dirname(__file__), "..", ".."))
from config import (get_default_port, get_default_weights, get_grpc_config, 
                   get_yolo_config, get_yolo_instance, create_yolo_nacos_manager)

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ===== MDC (Mapped Diagnostic Context) for logging =====
# Provide per-request tracing fields in logs: trace_id/span_id
mdc_trace_id: ContextVar[str] = ContextVar("trace_id", default="-")
mdc_span_id: ContextVar[str] = ContextVar("span_id", default="-")

class _MdcFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        # Always attach MDC fields so formatters can print them
        try:
            record.trace_id = mdc_trace_id.get()
        except Exception:
            record.trace_id = "-"
        try:
            record.span_id = mdc_span_id.get()
        except Exception:
            record.span_id = "-"
        return True

# Attach MDC filter and a formatter that prints MDC fields
_root = logging.getLogger()
_mdc_filter = _MdcFilter()
for _h in _root.handlers:
    _h.addFilter(_mdc_filter)
    # Ensure formatter includes MDC fields
    _fmt = logging.Formatter(
        fmt='%(asctime)s %(levelname)s [trace_id=%(trace_id)s span_id=%(span_id)s] %(name)s: %(message)s'
    )
    _h.setFormatter(_fmt)


class InferenceServicer(pb_grpc.InferenceStreamServiceServicer):
    """实现 proto 中的 InferenceStreamService，
    将流式请求解析为 numpy 图像 → 调用 YoloCore → 封装为 ResultEnvelope 返回。
    """
    def __init__(self, weights: str = None):
        if weights is None:
            weights = get_default_weights()
        self.core = YoloCore(weights)

    def Stream(self, request_iterator, context):
        try:
            # Tokens for resetting MDC at the end
            _tok_trace = None
            _tok_span = None
            for request in request_iterator:
                if request.HasField('open'):
                    # Extract trace context from header if present
                    try:
                        hdr = request.open.header if request.open.HasField('header') else None
                        tr = hdr.trace if hdr and hdr.HasField('trace') else None
                        _trace_id = tr.trace_id if tr and tr.trace_id else "-"
                        _span_id = tr.span_id if tr and tr.span_id else "-"
                        # Set MDC for this stream thread
                        _tok_trace = mdc_trace_id.set(_trace_id)
                        _tok_span = mdc_span_id.set(_span_id)
                        logger.info("stream opened")
                    except Exception:
                        logger.info("stream opened (no trace context)")
                    yield pb.StreamResponse(ack=pb.StreamAck(status=pb.CustomStatus(code=0, message="Stream opened")))
                elif request.HasField('frame'):
                    frame = request.frame
                    # 先尝试找视频
                    video_bytes = None
                    for i, inp in enumerate(frame.inputs):
                        if inp.kind == "video" and inp.HasField("binary"):
                            video_bytes = inp.binary
                            break
                    if video_bytes:
                        # 写临时文件并逐帧处理
                        import tempfile, os, cv2, time
                        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as f:
                            f.write(video_bytes)
                            temp_path = f.name
                        cap = cv2.VideoCapture(temp_path)
                        idx = 0
                        try:
                            while True:
                                ok, bgr = cap.read()
                                if not ok or bgr is None:
                                    break
                                # 将单帧包装成一个“伪 frame”复用现有处理，要求 _process_image(bgr, frame_index=idx)
                                resp = self._process_image(bgr, frame_index=idx)
                                if resp:
                                    yield resp
                                idx += 1
                        finally:
                            cap.release()
                            os.unlink(temp_path)
                    else:
                        # 兼容原有逻辑：寻找 image/binary
                        result = self._process_frame(frame, context)
                        if result:
                            yield result
                elif request.HasField('close'):
                    logger.info("stream closed")
                    yield pb.StreamResponse(ack=pb.StreamAck(status=pb.CustomStatus(code=0, message="Stream closed")))
                    break
        except Exception as e:
            logger.error(f"流式处理错误: {e}")
            yield pb.StreamResponse(ack=pb.StreamAck(status=pb.CustomStatus(code=1, message=str(e))))
        finally:
            # Reset MDC to avoid leaking across requests
            try:
                if _tok_trace is not None:
                    mdc_trace_id.reset(_tok_trace)
                if _tok_span is not None:
                    mdc_span_id.reset(_tok_span)
            except Exception:
                pass

    def _process_frame(self, frame: pb.StreamFrame, context):
        """处理单个图像帧"""
        t0 = time.time()

        # 从配置获取默认参数
        yolo_config = get_yolo_config()
        session_id = None
        mode = "detect"
        persist = False
        target_w = yolo_config.get("target_width", 640)
        target_h = yolo_config.get("target_height", 640)
        keep_ar = yolo_config.get("keep_aspect_ratio", True)

        # ===== 1) 获取第一张 image/binary 输入 =====
        img_bytes, input_index = None, 0
        for i, inp in enumerate(frame.inputs):
            if inp.kind == "image" and inp.HasField("binary"):
                img_bytes, input_index = inp.binary, i
                break
        
        if not img_bytes:
            logger.error("no image binary in inputs")
            return pb.StreamResponse(
                ack=pb.StreamAck(status=pb.CustomStatus(code=1, message="no image binary in inputs"))
            )

        # ===== 2) 二进制 → OpenCV BGR =====
        nparr = np.frombuffer(img_bytes, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if image is None:
            logger.error("decode image failed")
            return pb.StreamResponse(
                ack=pb.StreamAck(status=pb.CustomStatus(code=1, message="decode image failed"))
            )

        H, W = image.shape[:2]

        # ===== 3) 预处理与推理 =====
        resized, resize_info = self.core.resize_for_yolo(image, (target_w, target_h), keep_ar)
        results = self.core.detect_or_track(resized, session_id=session_id, persist=persist, mode=mode)

        # ===== 4) 后处理：使用DTO类构造消息 =====
        detections = []
        boxes_scaled, clss, confs, tids = [], [], [], []
        if results and len(results) > 0 and results[0].boxes is not None:
            boxes_xyxy = results[0].boxes.xyxy.cpu().tolist()
            clss = results[0].boxes.cls.int().cpu().tolist()
            confs = results[0].boxes.conf.cpu().tolist()
            tids = results[0].boxes.id.int().cpu().tolist() if results[0].boxes.id is not None else []
            boxes_scaled = self.core.scale_boxes_to_original(boxes_xyxy, resize_info, image.shape)

            # 使用DTO类创建检测结果
            for i, (box, cls_id, conf) in enumerate(zip(boxes_scaled, clss, confs)):
                x1, y1, x2, y2 = box
                track_id = tids[i] if i < len(tids) else 0
                class_name = self.core.model.names[int(cls_id)] if self.core.model else str(cls_id)
                
                # 使用DTO类创建检测结果
                detection = YoloDetectionDTO(
                    score=float(conf),
                    class_id=int(cls_id),
                    class_name=class_name,
                    x=float(x1),
                    y=float(y1),
                    width=float(x2 - x1),
                    height=float(y2 - y1),
                    track_id=track_id
                )
                detections.append(detection)

        # 创建YOLO结果DTO
        yolo_result = YoloResultDTO(
            detections=detections,
            coord_type="pixel",
            input_width=W,
            input_height=H,
            runtime_ms=(time.time() - t0) * 1000.0
        )

        # 生成叠图
        overlay_image = None
        if boxes_scaled:
            overlay = self.core.draw_boxes_on_image(image, boxes_scaled, clss, confs, tids if tids else None)
            jpeg_quality = yolo_config.get("jpeg_quality", 90)
            ok, buf = cv2.imencode(".jpg", overlay, [cv2.IMWRITE_JPEG_QUALITY, jpeg_quality])
            if ok:
                overlay_image = buf.tobytes()

        # 创建YOLO响应DTO
        yolo_response = YoloResponseDTO(
            yolo_result=yolo_result,
            overlay_image=overlay_image,
            overlay_format="jpeg" if overlay_image else None
        )

        # 转换为ResultEnvelope
        results_env = [yolo_response.to_result_envelope(kind="detections", input_index=input_index)]
        
        # 添加叠图结果
        overlay_env = yolo_response.to_overlay_envelope(input_index=input_index)
        if overlay_env:
            results_env.append(overlay_env)

        # 结果元数据
        meta = struct_pb2.Struct(fields={
            "coord_type": struct_pb2.Value(string_value="pixel"),
            "input_w": struct_pb2.Value(number_value=W),
            "input_h": struct_pb2.Value(number_value=H),
            "runtime_ms": struct_pb2.Value(number_value=(time.time() - t0) * 1000.0),
            "trace_id": struct_pb2.Value(string_value=mdc_trace_id.get()),
            "span_id": struct_pb2.Value(string_value=mdc_span_id.get()),
        })

        # 返回帧结果
        return pb.StreamResponse(
            frame=pb.FrameResult(
                frame_index=frame.frame_index,
                results=results_env,
                meta=meta
            )
        )

    def _process_image(self, image_bgr, frame_index: int = 0):
        """处理单张已解码的 BGR 图像（用于视频逐帧场景）。"""
        t0 = time.time()

        # 从配置获取默认参数
        yolo_config = get_yolo_config()
        session_id = None
        mode = "detect"
        persist = False
        target_w = yolo_config.get("target_width", 640)
        target_h = yolo_config.get("target_height", 640)
        keep_ar = yolo_config.get("keep_aspect_ratio", True)

        if image_bgr is None:
            logger.error("input image is None")
            return pb.StreamResponse(
                ack=pb.StreamAck(status=pb.CustomStatus(code=1, message="input image is None"))
            )

        H, W = image_bgr.shape[:2]

        # 预处理与推理
        resized, resize_info = self.core.resize_for_yolo(image_bgr, (target_w, target_h), keep_ar)
        results = self.core.detect_or_track(resized, session_id=session_id, persist=persist, mode=mode)

        # 后处理：使用DTO类构造消息
        detections = []
        boxes_scaled, clss, confs, tids = [], [], [], []
        if results and len(results) > 0 and results[0].boxes is not None:
            boxes_xyxy = results[0].boxes.xyxy.cpu().tolist()
            clss = results[0].boxes.cls.int().cpu().tolist()
            confs = results[0].boxes.conf.cpu().tolist()
            tids = results[0].boxes.id.int().cpu().tolist() if results[0].boxes.id is not None else []
            boxes_scaled = self.core.scale_boxes_to_original(boxes_xyxy, resize_info, image_bgr.shape)

            # 使用DTO类创建检测结果
            for i, (box, cls_id, conf) in enumerate(zip(boxes_scaled, clss, confs)):
                x1, y1, x2, y2 = box
                track_id = tids[i] if i < len(tids) else 0
                class_name = self.core.model.names[int(cls_id)] if self.core.model else str(cls_id)
                
                # 使用DTO类创建检测结果
                detection = YoloDetectionDTO(
                    score=float(conf),
                    class_id=int(cls_id),
                    class_name=class_name,
                    x=float(x1),
                    y=float(y1),
                    width=float(x2 - x1),
                    height=float(y2 - y1),
                    track_id=track_id
                )
                detections.append(detection)

        # 创建YOLO结果DTO
        yolo_result = YoloResultDTO(
            detections=detections,
            coord_type="pixel",
            input_width=W,
            input_height=H,
            runtime_ms=(time.time() - t0) * 1000.0
        )

        # 生成叠图
        overlay_image = None
        if boxes_scaled:
            overlay = self.core.draw_boxes_on_image(image_bgr, boxes_scaled, clss, confs, tids if tids else None)
            jpeg_quality = yolo_config.get("jpeg_quality", 90)
            ok, buf = cv2.imencode(".jpg", overlay, [cv2.IMWRITE_JPEG_QUALITY, jpeg_quality])
            if ok:
                overlay_image = buf.tobytes()

        # 创建YOLO响应DTO
        yolo_response = YoloResponseDTO(
            yolo_result=yolo_result,
            overlay_image=overlay_image,
            overlay_format="jpeg" if overlay_image else None
        )

        # 转换为ResultEnvelope
        results_env = [yolo_response.to_result_envelope(kind="detections", input_index=0)]
        
        # 添加叠图结果
        overlay_env = yolo_response.to_overlay_envelope(input_index=0)
        if overlay_env:
            results_env.append(overlay_env)

        meta = struct_pb2.Struct(fields={
            "coord_type": struct_pb2.Value(string_value="pixel"),
            "input_w": struct_pb2.Value(number_value=W),
            "input_h": struct_pb2.Value(number_value=H),
            "runtime_ms": struct_pb2.Value(number_value=(time.time() - t0) * 1000.0),
            "trace_id": struct_pb2.Value(string_value=mdc_trace_id.get()),
            "span_id": struct_pb2.Value(string_value=mdc_span_id.get()),
        })

        return pb.StreamResponse(
            frame=pb.FrameResult(
                frame_index=frame_index,
                results=results_env,
                meta=meta
            )
        )


def serve(weights: str = None, port: int = None, model_type: str = None, 
          model_name: str = None, enable_nacos: bool = True, instance_id: str = None):
    """启动 gRPC 服务器并注册到Nacos。
    
    Args:
        weights: YOLO模型权重文件路径
        port: gRPC服务端口
        model_type: 模型类型 (detection/segmentation)
        model_name: 模型名称 (yolov8n/yolov8s等)
        enable_nacos: 是否启用Nacos注册
        instance_id: YOLO实例ID
    """
    # 获取实例配置
    try:
        instance_config = get_yolo_instance(instance_id)
        logger.info(f"使用实例配置: {instance_config.name}")
    except ValueError as e:
        logger.error(f"实例配置错误: {e}")
        return
    
    # 获取配置，优先使用参数，其次使用实例配置
    if weights is None:
        weights = os.getenv("YOLO_WEIGHTS", instance_config.model_config.weights)
    if port is None:
        port = int(os.getenv("GRPC_PORT", str(instance_config.port)))
    if model_type is None:
        model_type = instance_config.model_config.model_type
    if model_name is None:
        model_name = instance_config.model_config.model_name
    
    # 创建gRPC服务器
    grpc_config = get_grpc_config()
    server = grpc.server(
        futures.ThreadPoolExecutor(max_workers=grpc_config.get("max_workers", 8)),
        options=[
            ("grpc.max_receive_message_length", grpc_config.get("max_receive_message_length", 32 * 1024 * 1024)),
            ("grpc.max_send_message_length", grpc_config.get("max_send_message_length", 32 * 1024 * 1024)),
        ],
    )
    
    # 添加服务
    servicer = InferenceServicer(weights)
    pb_grpc.add_InferenceStreamServiceServicer_to_server(servicer, server)
    server.add_insecure_port(f"[::]:{port}")
    
    # Nacos管理器
    nacos_manager = None
    if enable_nacos:
        try:
            # 创建自定义元数据
            custom_metadata = {
                "model_type": model_type,
                "model_name": model_name,
                "model_path": weights,
                "grpc_port": str(port),
                "capabilities": "detection,tracking" if model_type == "detection" else "segmentation",
                "gpu_required": "false"  # 可以根据实际设备情况设置
            }
            
            # 创建Nacos管理器
            nacos_manager = create_yolo_nacos_manager(
                instance_id=instance_id,
                custom_metadata=custom_metadata
            )
            
            # 启动Nacos注册
            nacos_manager.start_registration()
            logger.info("✅ Nacos注册已启动")
            
        except Exception as e:
            logger.error(f"❌ Nacos注册失败: {e}")
            logger.info("继续启动gRPC服务，但不注册到Nacos")
    
    # 启动gRPC服务器
    server.start()
    logger.info(f"✅ YOLO gRPC服务已启动，监听端口: {port}")
    logger.info(f"模型: {model_name} ({model_type})")
    logger.info(f"权重文件: {weights}")
    
    # 设置优雅关闭
    def signal_handler(signum, frame):
        logger.info("收到退出信号，正在关闭服务...")
        if nacos_manager:
            logger.info("正在从Nacos注销服务...")
            nacos_manager.stop_registration()
        server.stop(grace=5.0)
        logger.info("✅ 服务已关闭")
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("收到键盘中断信号")
        if nacos_manager:
            nacos_manager.stop_registration()
        server.stop(grace=5.0)


if __name__ == "__main__":
    serve()