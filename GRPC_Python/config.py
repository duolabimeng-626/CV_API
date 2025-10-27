# config.py
# 配置管理模块

import os
import json
from typing import Dict, Any, Optional
from dataclasses import dataclass

@dataclass
class ModelConfig:
    """模型配置"""
    weights: str
    model_type: str
    model_name: str
    model_version: str = "8.0"
    target_width: int = 640
    target_height: int = 640
    keep_aspect_ratio: bool = True
    jpeg_quality: int = 90

@dataclass
class YoloInstanceConfig:
    """YOLO实例配置"""
    name: str
    ip: str
    port: int
    group: str = "DEFAULT_GROUP"
    cluster: str = "DEFAULT"
    model_config: ModelConfig = None
    metadata: Optional[Dict[str, str]] = None

@dataclass
class ServiceConfig:
    """服务配置"""
    name: str
    ip: str
    port: int
    group: str = "DEFAULT_GROUP"
    cluster: str = "DEFAULT"
    metadata: Optional[Dict[str, str]] = None

@dataclass
class NacosConfig:
    """Nacos配置"""
    server_ip: str
    port: int
    namespace: str
    username: str
    password: str
    group_name: str
    enable_auth: bool = True

class ConfigManager:
    """配置管理器"""
    
    def __init__(self, config_file: str = "config.json"):
        self.config_file = config_file
        self.config = self._load_config()
    
    def _load_config(self) -> Dict[str, Any]:
        """加载配置文件"""
        default_config = {
            "nacos": {
                "server_ip": "127.0.0.1",
                "port": 8848,
                "namespace": "public",
                "username": "",
                "password": "",
                "group_name": "DEFAULT_GROUP",
                "enable_auth": False
            },
            "gateway": {
                "name": "yolo-gateway",
                "ip": "127.0.0.1",
                "port": 8080,
                "group": "DEFAULT_GROUP"
            },
            "grpc": {
                "max_workers": 8,
                "max_receive_message_length": 33554432,
                "max_send_message_length": 33554432
            },
            "yolo_instances": {
                "detection_nano": {
                    "name": "yolo-detection-nano",
                    "ip": "127.0.0.1",
                    "port": 50052,
                    "group": "DEFAULT_GROUP",
                    "cluster": "DEFAULT",
                    "model_config": {
                        "weights": "models/yolo/weights/yolov8n.pt",
                        "model_type": "detection",
                        "model_name": "yolov8n",
                        "model_version": "8.0",
                        "target_width": 640,
                        "target_height": 640,
                        "keep_aspect_ratio": True,
                        "jpeg_quality": 90
                    },
                    "metadata": {
                        "version": "1.0.0",
                        "description": "YOLO Detection Service - Nano Model",
                        "protocol": "grpc",
                        "capabilities": "detection,tracking",
                        "input_formats": "image/jpeg,image/png",
                        "output_formats": "json,overlay_image",
                        "max_image_size": "4096x4096",
                        "gpu_required": "false",
                        "health_check": "/health"
                    }
                }
            },
            "default_instance": "detection_nano"
        }
        
        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    config = json.load(f)
                    # 合并默认配置
                    for key, value in default_config.items():
                        if key not in config:
                            config[key] = value
                    return config
            except Exception as e:
                print(f"加载配置文件失败: {e}，使用默认配置")
                return default_config
        else:
            # 创建默认配置文件
            self._save_config(default_config)
            return default_config
    
    def _save_config(self, config: Dict[str, Any]):
        """保存配置文件"""
        try:
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(config, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"保存配置文件失败: {e}")
    
    def get_nacos_config(self) -> NacosConfig:
        """获取Nacos配置"""
        nacos_data = self.config.get("nacos", {})
        return NacosConfig(
            server_ip=nacos_data.get("server_ip", "127.0.0.1"),
            port=nacos_data.get("port", 8848),
            namespace=nacos_data.get("namespace", "public"),
            username=nacos_data.get("username", ""),
            password=nacos_data.get("password", ""),
            group_name=nacos_data.get("group_name", "DEFAULT_GROUP"),
            enable_auth=nacos_data.get("enable_auth", False)
        )
    
    def get_service_config(self, service_type: str = "yolo_service") -> ServiceConfig:
        """获取服务配置"""
        service_data = self.config.get(service_type, {})
        return ServiceConfig(
            name=service_data.get("name", "yolo-inference-service"),
            ip=service_data.get("ip", "127.0.0.1"),
            port=service_data.get("port", 50052),
            group=service_data.get("group", "DEFAULT_GROUP"),
            cluster=service_data.get("cluster", "DEFAULT"),
            metadata=service_data.get("metadata", {})
        )
    
    def update_service_port(self, service_type: str, port: int):
        """更新服务端口"""
        if service_type in self.config:
            self.config[service_type]["port"] = port
            self._save_config(self.config)
    
    def get_all_services(self) -> Dict[str, ServiceConfig]:
        """获取所有服务配置"""
        services = {}
        for key, value in self.config.items():
            if key not in ["nacos", "grpc", "yolo_instances", "default_instance"] and isinstance(value, dict):
                services[key] = ServiceConfig(
                    name=value.get("name", key),
                    ip=value.get("ip", "127.0.0.1"),
                    port=value.get("port", 8080),
                    group=value.get("group", "DEFAULT_GROUP"),
                    cluster=value.get("cluster", "DEFAULT"),
                    metadata=value.get("metadata", {})
                )
        return services
    
    def get_yolo_instances(self) -> Dict[str, YoloInstanceConfig]:
        """获取所有YOLO实例配置"""
        instances = {}
        yolo_instances = self.config.get("yolo_instances", {})
        for instance_id, instance_data in yolo_instances.items():
            model_config_data = instance_data.get("model_config", {})
            model_config = ModelConfig(
                weights=model_config_data.get("weights", "models/yolo/weights/yolov8n.pt"),
                model_type=model_config_data.get("model_type", "detection"),
                model_name=model_config_data.get("model_name", "yolov8n"),
                model_version=model_config_data.get("model_version", "8.0"),
                target_width=model_config_data.get("target_width", 640),
                target_height=model_config_data.get("target_height", 640),
                keep_aspect_ratio=model_config_data.get("keep_aspect_ratio", True),
                jpeg_quality=model_config_data.get("jpeg_quality", 90)
            )
            
            instances[instance_id] = YoloInstanceConfig(
                name=instance_data.get("name", instance_id),
                ip=instance_data.get("ip", "127.0.0.1"),
                port=instance_data.get("port", 50052),
                group=instance_data.get("group", "DEFAULT_GROUP"),
                cluster=instance_data.get("cluster", "DEFAULT"),
                model_config=model_config,
                metadata=instance_data.get("metadata", {})
            )
        return instances
    
    def get_yolo_instance(self, instance_id: str = None) -> YoloInstanceConfig:
        """获取指定的YOLO实例配置"""
        if instance_id is None:
            instance_id = self.config.get("default_instance", "detection_nano")
        
        instances = self.get_yolo_instances()
        if instance_id not in instances:
            raise ValueError(f"YOLO实例 '{instance_id}' 不存在。可用实例: {list(instances.keys())}")
        
        return instances[instance_id]
    
    def get_default_instance_id(self) -> str:
        """获取默认实例ID"""
        return self.config.get("default_instance", "detection_nano")
    
    def get_grpc_config(self) -> Dict[str, Any]:
        """获取gRPC配置"""
        return self.config.get("grpc", {})
    
    def get_yolo_config(self) -> Dict[str, Any]:
        """获取YOLO配置"""
        return self.config.get("yolo", {})
    
    def get_default_port(self) -> int:
        """获取默认端口"""
        return self.get_grpc_config().get("default_port", 50052)
    
    def get_default_weights(self) -> str:
        """获取默认权重文件"""
        return self.get_yolo_config().get("default_weights", "yolov8n.pt")
    
    def get_default_model_type(self) -> str:
        """获取默认模型类型"""
        return self.get_yolo_config().get("default_model_type", "detection")
    
    def get_default_model_name(self) -> str:
        """获取默认模型名称"""
        return self.get_yolo_config().get("default_model_name", "yolov8n")

# 全局配置管理器
config_manager = ConfigManager()

# 便捷函数
def get_nacos_config() -> NacosConfig:
    """获取Nacos配置"""
    return config_manager.get_nacos_config()

def get_yolo_service_config() -> ServiceConfig:
    """获取YOLO服务配置"""
    return config_manager.get_service_config("yolo_service")

def get_gateway_config() -> ServiceConfig:
    """获取网关配置"""
    return config_manager.get_service_config("gateway")

def get_grpc_config() -> Dict[str, Any]:
    """获取gRPC配置"""
    return config_manager.get_grpc_config()

def get_yolo_instances() -> Dict[str, YoloInstanceConfig]:
    """获取所有YOLO实例配置"""
    return config_manager.get_yolo_instances()

def get_yolo_instance(instance_id: str = None) -> YoloInstanceConfig:
    """获取指定的YOLO实例配置"""
    return config_manager.get_yolo_instance(instance_id)

def get_default_instance_id() -> str:
    """获取默认实例ID"""
    return config_manager.get_default_instance_id()

# 向后兼容的函数
def get_yolo_config() -> Dict[str, Any]:
    """获取默认YOLO实例的模型配置（向后兼容）"""
    instance = get_yolo_instance()
    return {
        "default_weights": instance.model_config.weights,
        "default_model_type": instance.model_config.model_type,
        "default_model_name": instance.model_config.model_name,
        "target_width": instance.model_config.target_width,
        "target_height": instance.model_config.target_height,
        "keep_aspect_ratio": instance.model_config.keep_aspect_ratio,
        "jpeg_quality": instance.model_config.jpeg_quality
    }

def get_default_port() -> int:
    """获取默认实例的端口（向后兼容）"""
    return get_yolo_instance().port

def get_default_weights() -> str:
    """获取默认实例的权重文件（向后兼容）"""
    return get_yolo_instance().model_config.weights

def get_default_model_type() -> str:
    """获取默认实例的模型类型（向后兼容）"""
    return get_yolo_instance().model_config.model_type

def get_default_model_name() -> str:
    """获取默认实例的模型名称（向后兼容）"""
    return get_yolo_instance().model_config.model_name

def create_yolo_nacos_manager(instance_id: str = None, 
                            custom_metadata: dict = None) -> 'NacosServiceManager':
    """创建YOLO服务的Nacos管理器，支持多实例"""
    from nacos_service import NacosServiceManager, NacosConfig, ServiceInfo
    
    # 获取基础配置
    nacos_config = get_nacos_config()
    yolo_instance = get_yolo_instance(instance_id)
    
    # 创建Nacos配置
    nacos_cfg = NacosConfig(
        server_ip=nacos_config.server_ip,
        port=nacos_config.port,
        namespace=nacos_config.namespace,
        username=nacos_config.username,
        password=nacos_config.password,
        group_name=nacos_config.group_name
    )
    
    # 创建服务信息，包含模型元数据
    service_info = ServiceInfo(
        name=yolo_instance.name,
        ip=yolo_instance.ip,
        port=yolo_instance.port,
        group=yolo_instance.group,
        cluster=yolo_instance.cluster,
        metadata=yolo_instance.metadata
    )
    
    # 创建管理器
    manager = NacosServiceManager(nacos_cfg, service_info)
    
    # 设置元数据
    if custom_metadata:
        # 合并默认元数据和自定义元数据
        default_metadata = yolo_instance.metadata.copy()
        default_metadata.update(custom_metadata)
        manager.service_info.metadata = default_metadata
    else:
        # 使用实例的默认元数据，并添加模型配置信息
        default_metadata = yolo_instance.metadata.copy()
        default_metadata.update({
            "model_type": yolo_instance.model_config.model_type,
            "model_name": yolo_instance.model_config.model_name,
            "model_version": yolo_instance.model_config.model_version,
            "weights": yolo_instance.model_config.weights
        })
        manager.service_info.metadata = default_metadata
    
    return manager
