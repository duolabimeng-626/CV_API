#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
YOLO专用的DTO类
提供类似Java的构造函数和setter/getter方法来构造gRPC消息
"""

import time
from typing import List, Optional, Tuple
from google.protobuf import struct_pb2, any_pb2
import sys
import os

# 导入protobuf生成的类
sys.path.append(os.path.join(os.path.dirname(__file__), "..", "..", "gen"))
from gen import ai_pb2 as pb


class YoloDetectionDTO:
    """YOLO检测结果DTO类"""
    
    def __init__(self, score: float = 0.0, class_id: int = 0, class_name: str = "", 
                 x: float = 0.0, y: float = 0.0, width: float = 0.0, height: float = 0.0, 
                 track_id: int = 0):
        """
        构造函数
        
        Args:
            score: 置信度分数
            class_id: 类别ID
            class_name: 类别名称
            x: 左上角x坐标
            y: 左上角y坐标
            width: 宽度
            height: 高度
            track_id: 跟踪ID
        """
        self._score = score
        self._class_id = class_id
        self._class_name = class_name
        self._x = x
        self._y = y
        self._width = width
        self._height = height
        self._track_id = track_id
    
    # Getter方法
    @property
    def score(self) -> float:
        return self._score
    
    @property
    def class_id(self) -> int:
        return self._class_id
    
    @property
    def class_name(self) -> str:
        return self._class_name
    
    @property
    def x(self) -> float:
        return self._x
    
    @property
    def y(self) -> float:
        return self._y
    
    @property
    def width(self) -> float:
        return self._width
    
    @property
    def height(self) -> float:
        return self._height
    
    @property
    def track_id(self) -> int:
        return self._track_id
    
    # Setter方法
    def set_score(self, score: float) -> 'YoloDetectionDTO':
        self._score = score
        return self
    
    def set_class_id(self, class_id: int) -> 'YoloDetectionDTO':
        self._class_id = class_id
        return self
    
    def set_class_name(self, class_name: str) -> 'YoloDetectionDTO':
        self._class_name = class_name
        return self
    
    def set_position(self, x: float, y: float) -> 'YoloDetectionDTO':
        self._x = x
        self._y = y
        return self
    
    def set_size(self, width: float, height: float) -> 'YoloDetectionDTO':
        self._width = width
        self._height = height
        return self
    
    def set_track_id(self, track_id: int) -> 'YoloDetectionDTO':
        self._track_id = track_id
        return self
    
    def to_protobuf_struct(self) -> struct_pb2.Struct:
        """转换为protobuf Struct格式"""
        return struct_pb2.Struct(fields={
            "score": struct_pb2.Value(number_value=float(self._score)),
            "class_id": struct_pb2.Value(number_value=int(self._class_id)),
            "class_name": struct_pb2.Value(string_value=str(self._class_name)),
            "x": struct_pb2.Value(number_value=float(self._x)),
            "y": struct_pb2.Value(number_value=float(self._y)),
            "width": struct_pb2.Value(number_value=float(self._width)),
            "height": struct_pb2.Value(number_value=float(self._height)),
            "track_id": struct_pb2.Value(number_value=int(self._track_id))
        })
    
    def __str__(self) -> str:
        return f"YoloDetectionDTO(class={self._class_name}, score={self._score:.2f}, pos=({self._x:.1f},{self._y:.1f}), size=({self._width:.1f}x{self._height:.1f}), track_id={self._track_id})"


class YoloResultDTO:
    """YOLO结果DTO类"""
    
    def __init__(self, detections: List[YoloDetectionDTO] = None, coord_type: str = "pixel",
                 input_width: int = 0, input_height: int = 0, runtime_ms: float = 0.0):
        """
        构造函数
        
        Args:
            detections: 检测结果列表
            coord_type: 坐标类型 ("pixel" 或 "normalized")
            input_width: 输入图像宽度
            input_height: 输入图像高度
            runtime_ms: 运行时间(毫秒)
        """
        self._detections = detections or []
        self._coord_type = coord_type
        self._input_width = input_width
        self._input_height = input_height
        self._runtime_ms = runtime_ms
    
    # Getter方法
    @property
    def detections(self) -> List[YoloDetectionDTO]:
        return self._detections
    
    @property
    def coord_type(self) -> str:
        return self._coord_type
    
    @property
    def input_width(self) -> int:
        return self._input_width
    
    @property
    def input_height(self) -> int:
        return self._input_height
    
    @property
    def runtime_ms(self) -> float:
        return self._runtime_ms
    
    # Setter方法
    def set_detections(self, detections: List[YoloDetectionDTO]) -> 'YoloResultDTO':
        self._detections = detections
        return self
    
    def add_detection(self, detection: YoloDetectionDTO) -> 'YoloResultDTO':
        self._detections.append(detection)
        return self
    
    def set_coord_type(self, coord_type: str) -> 'YoloResultDTO':
        self._coord_type = coord_type
        return self
    
    def set_input_size(self, width: int, height: int) -> 'YoloResultDTO':
        self._input_width = width
        self._input_height = height
        return self
    
    def set_runtime_ms(self, runtime_ms: float) -> 'YoloResultDTO':
        self._runtime_ms = runtime_ms
        return self
    
    def to_protobuf_struct(self) -> struct_pb2.Struct:
        """转换为protobuf Struct格式"""
        items_lv = struct_pb2.ListValue()
        for detection in self._detections:
            items_lv.values.append(struct_pb2.Value(struct_value=detection.to_protobuf_struct()))
        
        return struct_pb2.Struct(fields={
            "items": struct_pb2.Value(list_value=items_lv),
            "coord_type": struct_pb2.Value(string_value=self._coord_type),
            "input_width": struct_pb2.Value(number_value=self._input_width),
            "input_height": struct_pb2.Value(number_value=self._input_height),
            "runtime_ms": struct_pb2.Value(number_value=self._runtime_ms)
        })
    
    def __str__(self) -> str:
        return f"YoloResultDTO(detections={len(self._detections)}, coord_type={self._coord_type}, size={self._input_width}x{self._input_height}, runtime={self._runtime_ms:.1f}ms)"


class YoloResponseDTO:
    """YOLO响应DTO类"""
    
    def __init__(self, yolo_result: YoloResultDTO = None, overlay_image: bytes = None, 
                 overlay_format: str = None):
        """
        构造函数
        
        Args:
            yolo_result: YOLO检测结果
            overlay_image: 叠图数据
            overlay_format: 叠图格式 ("jpeg", "png"等)
        """
        self._yolo_result = yolo_result or YoloResultDTO()
        self._overlay_image = overlay_image
        self._overlay_format = overlay_format
    
    # Getter方法
    @property
    def yolo_result(self) -> YoloResultDTO:
        return self._yolo_result
    
    @property
    def overlay_image(self) -> Optional[bytes]:
        return self._overlay_image
    
    @property
    def overlay_format(self) -> Optional[str]:
        return self._overlay_format
    
    # Setter方法
    def set_yolo_result(self, yolo_result: YoloResultDTO) -> 'YoloResponseDTO':
        self._yolo_result = yolo_result
        return self
    
    def set_overlay_image(self, overlay_image: bytes, overlay_format: str = "jpeg") -> 'YoloResponseDTO':
        self._overlay_image = overlay_image
        self._overlay_format = overlay_format
        return self
    
    def clear_overlay(self) -> 'YoloResponseDTO':
        self._overlay_image = None
        self._overlay_format = None
        return self
    
    def to_result_envelope(self, kind: str = "detections", input_index: int = 0) -> pb.ResultEnvelope:
        """转换为ResultEnvelope"""
        # 将YoloResult转换为Any
        any_yolo = any_pb2.Any()
        any_yolo.Pack(self._yolo_result.to_protobuf_struct())
        
        # 创建元数据
        meta = struct_pb2.Struct(fields={
            "coord_type": struct_pb2.Value(string_value=self._yolo_result.coord_type),
            "input_w": struct_pb2.Value(number_value=self._yolo_result.input_width),
            "input_h": struct_pb2.Value(number_value=self._yolo_result.input_height),
            "runtime_ms": struct_pb2.Value(number_value=self._yolo_result.runtime_ms),
        })
        
        # 创建ResultEnvelope
        result_env = pb.ResultEnvelope(
            kind=kind,
            content_type='application/x-protobuf;type="google.protobuf.Struct"',
            message=any_yolo,
            meta=meta,
            input_index=input_index,
        )
        
        return result_env
    
    def to_overlay_envelope(self, input_index: int = 0) -> Optional[pb.ResultEnvelope]:
        """转换为叠图ResultEnvelope"""
        if not self._overlay_image or not self._overlay_format:
            return None
        
        # 创建元数据
        meta = struct_pb2.Struct(fields={
            "coord_type": struct_pb2.Value(string_value=self._yolo_result.coord_type),
            "input_w": struct_pb2.Value(number_value=self._yolo_result.input_width),
            "input_h": struct_pb2.Value(number_value=self._yolo_result.input_height),
            "runtime_ms": struct_pb2.Value(number_value=self._yolo_result.runtime_ms),
        })
        
        return pb.ResultEnvelope(
            kind="overlay",
            content_type=f'image/{self._overlay_format};kind="overlay"',
            binary=self._overlay_image,
            meta=meta,
            input_index=input_index,
        )
    
    def __str__(self) -> str:
        overlay_info = f", overlay={self._overlay_format}" if self._overlay_image else ""
        return f"YoloResponseDTO({self._yolo_result}{overlay_info})"


# 便捷的工厂方法
class YoloDTOFactory:
    """YOLO DTO工厂类，提供便捷的创建方法"""
    
    @staticmethod
    def create_detection(score: float, class_id: int, class_name: str, 
                        x: float, y: float, width: float, height: float, 
                        track_id: int = 0) -> YoloDetectionDTO:
        """创建检测结果"""
        return YoloDetectionDTO(
            score=score, class_id=class_id, class_name=class_name,
            x=x, y=y, width=width, height=height, track_id=track_id
        )
    
    @staticmethod
    def create_result(detections: List[YoloDetectionDTO], input_width: int, 
                     input_height: int, runtime_ms: float = 0.0) -> YoloResultDTO:
        """创建YOLO结果"""
        return YoloResultDTO(
            detections=detections,
            coord_type="pixel",
            input_width=input_width,
            input_height=input_height,
            runtime_ms=runtime_ms
        )
    
    @staticmethod
    def create_response(yolo_result: YoloResultDTO, overlay_image: bytes = None, 
                       overlay_format: str = None) -> YoloResponseDTO:
        """创建YOLO响应"""
        return YoloResponseDTO(
            yolo_result=yolo_result,
            overlay_image=overlay_image,
            overlay_format=overlay_format
        )
