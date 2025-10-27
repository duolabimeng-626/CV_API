package com.duola.grpc_java.dto2.utils;

import com.duola.grpc_java.dto2.yolo.YoloInferenceConfig;

/**
 * 推理配置工厂类
 * 用于创建不同类型的AI推理配置
 */
public class InferenceConfigFactory {
    
    /**
     * 创建YOLO检测配置
     */
    public static YoloInferenceConfig createYoloDetectionConfig() {
        return new YoloInferenceConfig("yolov8n")
                .forDetection()
                .withHighSpeed();
    }
    
    /**
     * 创建YOLO跟踪配置
     */
    public static YoloInferenceConfig createYoloTrackingConfig() {
        return new YoloInferenceConfig("yolov8s")
                .forTracking()
                .withHighAccuracy();
    }
    
    /**
     * 创建YOLO分割配置
     */
    public static YoloInferenceConfig createYoloSegmentationConfig() {
        return new YoloInferenceConfig("yolov8m-seg")
                .forSegmentation()
                .withHighAccuracy();
    }
    
    /**
     * 创建自定义YOLO配置
     */
    public static YoloInferenceConfig createCustomYoloConfig(String modelName, String mode) {
        YoloInferenceConfig config = new YoloInferenceConfig(modelName);
        switch (mode.toLowerCase()) {
            case "detect":
                return config.forDetection();
            case "track":
                return config.forTracking();
            case "segment":
                return config.forSegmentation();
            default:
                return config.forDetection();
        }
    }
    
    /**
     * 创建高精度YOLO配置
     */
    public static YoloInferenceConfig createHighAccuracyYoloConfig(String modelName) {
        return new YoloInferenceConfig(modelName)
                .forDetection()
                .withHighAccuracy();
    }
    
    /**
     * 创建高速YOLO配置
     */
    public static YoloInferenceConfig createHighSpeedYoloConfig(String modelName) {
        return new YoloInferenceConfig(modelName)
                .forDetection()
                .withHighSpeed();
    }
    
    // 未来扩展：TTS配置
    // public static TtsInferenceConfig createTtsConfig() {
    //     return new TtsInferenceConfig();
    // }
    
    // 未来扩展：ASR配置
    // public static AsrInferenceConfig createAsrConfig() {
    //     return new AsrInferenceConfig();
    // }
}
