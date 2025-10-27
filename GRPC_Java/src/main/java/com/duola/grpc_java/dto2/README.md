# DTO2 - 结构化YOLO专属DTO设计

## 📁 目录结构

```
dto2/
├── base/                    # 基础抽象类
│   ├── AIInferenceConfig.java      # AI推理配置基础类
│   ├── InferenceResult.java        # 推理结果基础接口
│   └── AIInferenceResponse.java    # AI推理响应基础类
├── yolo/                    # YOLO相关类
│   ├── YoloInferenceConfig.java    # YOLO配置类
│   ├── YoloDetectionResult.java    # YOLO检测结果
│   ├── YoloTrackingResult.java     # YOLO跟踪结果
│   ├── YoloSegmentationResult.java # YOLO分割结果
│   └── YoloInferenceResponse.java  # YOLO响应类
├── stream/                  # 流式处理类
│   ├── StreamInferenceRequest.java  # 流式请求类
│   └── StreamInferenceResponse.java # 流式响应类
├── utils/                   # 工具类
│   ├── InferenceConfigFactory.java # 配置工厂类
│   ├── ConfigValidator.java        # 验证器接口
│   └── YoloConfigValidator.java    # YOLO验证器
└── README.md               # 说明文档
```

## 🎯 设计理念

### 1. 分层架构
- **base包**: 提供所有AI模型的基础抽象类
- **yolo包**: 专门处理YOLO相关的配置和结果
- **stream包**: 处理gRPC流式通信
- **utils包**: 提供工具类和工厂方法

### 2. 扩展性设计
- 基础类为未来TTS、ASR等AI模型预留接口
- 每个包职责单一，便于维护和扩展
- 工厂模式支持快速创建配置

## 🚀 核心组件

### base包 - 基础抽象类

#### `AIInferenceConfig`
- 所有AI推理配置的基础抽象类
- 对应ai.proto中的InferenceHeader结构
- 包含模型信息、追踪上下文、租户上下文等通用字段

#### `InferenceResult`
- 推理结果的基础抽象类
- 对应ai.proto中的ResultEnvelope结构
- 为不同AI模型的结果提供统一接口

#### `AIInferenceResponse`
- 通用AI推理响应类
- 对应ai.proto中的FrameResult结构
- 支持多种AI模型结果的统一处理

### yolo包 - YOLO专属类

#### `YoloInferenceConfig`
- YOLO专属配置类，继承自AIInferenceConfig
- 包含YOLO特有的所有配置参数
- 支持检测、跟踪、分割三种模式
- 提供便捷的配置方法

#### YOLO结果类
- `YoloDetectionResult`: 检测结果
- `YoloTrackingResult`: 跟踪结果
- `YoloSegmentationResult`: 分割结果

#### `YoloInferenceResponse`
- YOLO专用响应类
- 继承自AIInferenceResponse
- 添加YOLO特有的响应字段

### stream包 - 流式处理

#### `StreamInferenceRequest`
- 流式推理请求类
- 对应ai.proto中的StreamRequest结构
- 支持open、frame、close三种事件类型

#### `StreamInferenceResponse`
- 流式推理响应类
- 对应ai.proto中的StreamResponse结构
- 支持ack、frame两种事件类型

### utils包 - 工具类

#### `InferenceConfigFactory`
- 配置工厂类
- 提供各种预设配置的创建方法
- 支持未来扩展TTS、ASR等AI模型

#### 验证器类
- `ConfigValidator`: 配置验证器接口
- `YoloConfigValidator`: YOLO配置验证器实现
- 确保配置参数的有效性，提供自动修复功能

## 💡 使用示例

### 基础使用
```java
// 使用工厂创建配置
YoloInferenceConfig config = InferenceConfigFactory.createYoloDetectionConfig();

// 自定义配置
YoloInferenceConfig config = new YoloInferenceConfig("yolov8m")
    .forSegmentation()
    .withHighAccuracy()
    .setConfidenceThreshold(0.3)
    .setDevice("gpu");
```

### 配置验证
```java
YoloConfigValidator validator = new YoloConfigValidator();
if (!validator.validate(config)) {
    String error = validator.getValidationError(config);
    config = validator.fixConfig(config);
}
```

### 流式处理
```java
// 创建流式请求
StreamInferenceRequest request = StreamInferenceRequest.open(config);

// 处理帧数据
StreamInferenceRequest frameRequest = StreamInferenceRequest.frame(frameData);

// 关闭流
StreamInferenceRequest closeRequest = StreamInferenceRequest.close(meta);
```

## 🔮 未来扩展

### TTS支持
```java
// 预留的TTS配置接口
public class TtsInferenceConfig extends AIInferenceConfig {
    // TTS特有配置
}
```

### ASR支持
```java
// 预留的ASR配置接口
public class AsrInferenceConfig extends AIInferenceConfig {
    // ASR特有配置
}
```

## 🎨 设计优势

1. **结构清晰**: 按功能分包，职责明确
2. **类型安全**: 强类型设计，编译时检查
3. **扩展性强**: 易于添加新的AI模型支持
4. **配置灵活**: 支持各种YOLO配置场景
5. **验证完善**: 自动配置验证和修复
6. **gRPC兼容**: 完美支持gRPC Stream API
7. **工厂模式**: 提供预设配置，简化使用

## 📦 包依赖关系

```
base (基础抽象)
├── yolo (YOLO实现)
├── stream (流式处理)
└── utils (工具类)
    ├── 依赖 base
    └── 依赖 yolo
```

这种结构确保了代码的模块化和可维护性，同时为未来的扩展提供了良好的基础。