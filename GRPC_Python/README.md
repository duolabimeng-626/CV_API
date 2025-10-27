# YOLO gRPC服务

基于YOLO模型的gRPC推理服务，支持Nacos服务注册与发现。

## 功能特性

- ✅ YOLO目标检测和跟踪
- ✅ gRPC高性能通信
- ✅ Nacos服务注册与发现
- ✅ 多模型支持（检测/分割）
- ✅ 优雅启动和关闭
- ✅ 详细的元数据配置

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 启动服务

```bash
# 使用默认配置启动
python main.py

# 指定模型和端口
python main.py --weights models/yolo/weights/yolov8n.pt --port 50051

# 启动分割模型
python main.py --model-type segmentation --model-name yolov8n-seg

# 禁用Nacos注册
python main.py --no-nacos
```

### 3. 命令行参数

```bash
python main.py --help
```

主要参数：
- `--weights`: YOLO模型权重文件路径
- `--port`: gRPC服务端口
- `--model-type`: 模型类型 (detection/segmentation)
- `--model-name`: 模型名称
- `--no-nacos`: 禁用Nacos注册
- `--nacos-server`: Nacos服务器地址

## 服务元数据

服务注册到Nacos时会包含以下元数据：

```json
{
  "model_type": "detection",
  "model_name": "yolov8n",
  "model_version": "8.0",
  "capabilities": "detection,tracking",
  "input_formats": "image/jpeg,image/png",
  "output_formats": "json,overlay_image",
  "max_image_size": "4096x4096",
  "gpu_required": "false"
}
```

## 多模型部署示例

### 检测模型
```bash
python main.py --model-type detection --model-name yolov8n --port 50051
python main.py --model-type detection --model-name yolov8s --port 50052
```

### 分割模型
```bash
python main.py --model-type segmentation --model-name yolov8n-seg --port 50053
```

## Java端调用

Java端可以通过Nacos查询服务列表，根据元数据选择合适的模型：

```java
// 查询所有YOLO服务
List<Instance> instances = namingService.selectInstances("yolo-inference-service", "DEFAULT_GROUP", true);

// 根据元数据筛选特定模型
for (Instance instance : instances) {
    Map<String, String> metadata = instance.getMetadata();
    if ("detection".equals(metadata.get("model_type")) && 
        "yolov8n".equals(metadata.get("model_name"))) {
        // 使用这个实例
    }
}
```

## 配置说明

### config.json
- `nacos`: Nacos服务器配置
- `yolo_service`: YOLO服务基础配置
- `metadata`: 服务元数据，用于服务发现和负载均衡

### 环境变量
- `YOLO_WEIGHTS`: 模型权重文件路径
- `GRPC_PORT`: gRPC服务端口
- `NACOS_SERVER`: Nacos服务器地址
- `NACOS_NAMESPACE`: Nacos命名空间

## 日志

服务日志会同时输出到控制台和 `yolo_service.log` 文件。
