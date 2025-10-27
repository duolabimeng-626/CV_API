package com.duola.grpc_java.client;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.Properties;

public class NacosClient {
    public static void main(String[] args) throws NacosException, InterruptedException {
        Properties props = new Properties();

        // 方式一：直接写死（先验证流程）
        // String serverAddr = "127.0.0.1:8848";

        // 方式二：用 JVM 参数注入，运行时加 -DserverAddr=127.0.0.1:8848
        String serverAddr = System.getProperty("serverAddr", "127.0.0.1:8848");

        // 可选：如果你的 Nacos 开了鉴权/命名空间，需要一并设置
        String namespace = System.getProperty("nacosNamespace", ""); // 无就留空
        String username = System.getProperty("nacosUsername", "nacos");
        String password = System.getProperty("nacosPassword", "nacos");

        props.setProperty("serverAddr", serverAddr);
        if (!namespace.isEmpty()) props.setProperty("namespace", namespace);
        props.setProperty("username", username);
        props.setProperty("password", password);

        ConfigService configService = NacosFactory.createConfigService(props);

        // 用真实的 dataId / group。group 不写请用 "DEFAULT_GROUP"
        String dataId = System.getProperty("dataId", "example_data_id"); // 例如 application.yaml
        String group  = System.getProperty("group", "DEFAULT_GROUP");

        // 注意：dataId 和 group 必须与控制台里存在的配置完全一致（区分大小写）
        String content = configService.getConfig(dataId, group, 5000);
        System.out.println("Config content:" + content);
        NamingService naming = NamingFactory.createNamingService(props);
        // 以下注册请求所造成的结果均一致, 注册分组名为`DEFAULT_GROUP`, 服务名为`nacos.test.service`的实例，实例的ip为`127.0.0.1`, port为`8848`, clusterName为`DEFAULT`.
        //        naming.registerInstance("nacos.test.service", "127.0.0.1", 8848);
        Instance instance = new Instance();
        instance.setIp("127.0.0.1");
        instance.setPort(6666);
        instance.setClusterName("DEFAULT");
        naming.registerInstance("nacos.test.service", instance);
        naming.registerInstance("nacos.test.service", "DEFAULT_GROUP", instance);
        Instance instance2 = new Instance();
        instance2.setIp("127.0.0.1");
        instance2.setPort(50054);
        instance2.setClusterName("DEFAULT");
        naming.deregisterInstance("yolo-segmentation-nano", "DEFAULT_GROUP",instance2);
        Thread.sleep(60000);
    }
}
