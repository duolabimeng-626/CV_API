package com.duola.grpc_java.dto2.utils;

import com.duola.grpc_java.dto2.yolo.YoloInferenceConfig;

/**
 * YOLO配置验证器
 */
public class YoloConfigValidator implements ConfigValidator<YoloInferenceConfig> {
    
    @Override
    public boolean validate(YoloInferenceConfig config) {
        if (config == null) {
            return false;
        }
        
        // 验证基础参数
        if (config.getTargetWidth() == null || config.getTargetWidth() <= 0) {
            return false;
        }
        
        if (config.getTargetHeight() == null || config.getTargetHeight() <= 0) {
            return false;
        }
        
        // 验证阈值参数
        if (config.getConfidenceThreshold() == null || 
            config.getConfidenceThreshold() < 0.0 || 
            config.getConfidenceThreshold() > 1.0) {
            return false;
        }
        
        if (config.getNmsThreshold() == null || 
            config.getNmsThreshold() < 0.0 || 
            config.getNmsThreshold() > 1.0) {
            return false;
        }
        
        // 验证模式参数
        if (config.getMode() == null || 
            (!config.getMode().equals("detect") && 
             !config.getMode().equals("track") && 
             !config.getMode().equals("segment"))) {
            return false;
        }
        
        // 验证设备参数
        if (config.getDevice() != null && 
            !config.getDevice().equals("cpu") && 
            !config.getDevice().equals("gpu") && 
            !config.getDevice().equals("cuda")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getValidationError(YoloInferenceConfig config) {
        if (config == null) {
            return "配置对象不能为空";
        }
        
        if (config.getTargetWidth() == null || config.getTargetWidth() <= 0) {
            return "目标宽度必须大于0";
        }
        
        if (config.getTargetHeight() == null || config.getTargetHeight() <= 0) {
            return "目标高度必须大于0";
        }
        
        if (config.getConfidenceThreshold() == null || 
            config.getConfidenceThreshold() < 0.0 || 
            config.getConfidenceThreshold() > 1.0) {
            return "置信度阈值必须在0.0-1.0之间";
        }
        
        if (config.getNmsThreshold() == null || 
            config.getNmsThreshold() < 0.0 || 
            config.getNmsThreshold() > 1.0) {
            return "NMS阈值必须在0.0-1.0之间";
        }
        
        if (config.getMode() == null || 
            (!config.getMode().equals("detect") && 
             !config.getMode().equals("track") && 
             !config.getMode().equals("segment"))) {
            return "模式必须是detect、track或segment之一";
        }
        
        if (config.getDevice() != null && 
            !config.getDevice().equals("cpu") && 
            !config.getDevice().equals("gpu") && 
            !config.getDevice().equals("cuda")) {
            return "设备必须是cpu、gpu或cuda之一";
        }
        
        return null;
    }
    
    @Override
    public YoloInferenceConfig fixConfig(YoloInferenceConfig config) {
        if (config == null) {
            return new YoloInferenceConfig();
        }
        
        // 修复基础参数
        if (config.getTargetWidth() == null || config.getTargetWidth() <= 0) {
            config.setTargetWidth(640);
        }
        
        if (config.getTargetHeight() == null || config.getTargetHeight() <= 0) {
            config.setTargetHeight(640);
        }
        
        // 修复阈值参数
        if (config.getConfidenceThreshold() == null || 
            config.getConfidenceThreshold() < 0.0 || 
            config.getConfidenceThreshold() > 1.0) {
            config.setConfidenceThreshold(0.5);
        }
        
        if (config.getNmsThreshold() == null || 
            config.getNmsThreshold() < 0.0 || 
            config.getNmsThreshold() > 1.0) {
            config.setNmsThreshold(0.45);
        }
        
        // 修复模式参数
        if (config.getMode() == null || 
            (!config.getMode().equals("detect") && 
             !config.getMode().equals("track") && 
             !config.getMode().equals("segment"))) {
            config.setMode("detect");
        }
        
        // 修复设备参数
        if (config.getDevice() == null || 
            (!config.getDevice().equals("cpu") && 
             !config.getDevice().equals("gpu") && 
             !config.getDevice().equals("cuda"))) {
            config.setDevice("cpu");
        }
        
        return config;
    }
}
