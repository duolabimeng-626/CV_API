package com.duola.grpc_java.util;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Properties;

public final class NacosUtils {

    private NacosUtils() {}

    public static Instance selectOneHealthyInstance(String serverAddr,
                                                    String namespace,
                                                    String username,
                                                    String password,
                                                    String serviceName,
                                                    String group) throws NacosException {
        Properties props = new Properties();
        props.setProperty("serverAddr", serverAddr);
        if (namespace != null && !namespace.isEmpty()) props.setProperty("namespace", namespace);
        if (username != null && !username.isEmpty()) props.setProperty("username", username);
        if (password != null && !password.isEmpty()) props.setProperty("password", password);

        NamingService namingService = NamingFactory.createNamingService(props);
        return namingService.selectOneHealthyInstance(serviceName, group);
    }

    public static ManagedChannel createPlainChannel(Instance instance, int maxInboundMb) {
        int maxBytes = maxInboundMb * 1024 * 1024;
        return ManagedChannelBuilder
                .forAddress(instance.getIp(), instance.getPort())
                .usePlaintext()
                .maxInboundMessageSize(maxBytes)
                .build();
    }
}


