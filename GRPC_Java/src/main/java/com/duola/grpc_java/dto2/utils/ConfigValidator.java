package com.duola.grpc_java.dto2.utils;

import com.duola.grpc_java.dto2.base.AIInferenceConfig;

/**
 * 配置验证器接口
 */
public interface ConfigValidator<T extends AIInferenceConfig> {
    
    /**
     * 验证配置是否有效
     */
    boolean validate(T config);
    
    /**
     * 获取验证错误信息
     */
    String getValidationError(T config);
    
    /**
     * 修复配置中的问题
     */
    T fixConfig(T config);
}
