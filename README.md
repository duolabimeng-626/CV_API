# CV_API
计算机视觉API开放平台
## 应用概述

<font style="color:rgb(0, 0, 0);">计算机视觉正成为人工智能时代的核心驱动力，它让机器真正具备了“看懂世界”的能力。无论是在</font>**<font style="color:rgb(0, 0, 0);">智慧交通</font>**<font style="color:rgb(0, 0, 0);">中识别车流与事故、在</font>**<font style="color:rgb(0, 0, 0);">工业制造</font>**<font style="color:rgb(0, 0, 0);">中检测产品瑕疵、在</font>**<font style="color:rgb(0, 0, 0);">智慧城市</font>**<font style="color:rgb(0, 0, 0);">中实现人流分析与应急调度，还是在</font>**<font style="color:rgb(0, 0, 0);">农业监测、医疗诊断、零售分析</font>**<font style="color:rgb(0, 0, 0);">等领域，视觉技术都在深度融合大数据、物联网与云计算的生态，构建出新的智能决策体系。基于此，我们提出搭建一个专门的</font>**<font style="color:rgb(0, 0, 0);">计算机视觉平台</font>**<font style="color:rgb(0, 0, 0);">：通过统一的 </font>**<font style="color:rgb(0, 0, 0);">API 调用接口</font>**<font style="color:rgb(0, 0, 0);">，将摄像头、无人机、机器人、传感器等设备接入系统，实时采集图像及环境数据；这些数据将汇聚至我们的</font>**<font style="color:rgb(0, 0, 0);">数据中心</font>**<font style="color:rgb(0, 0, 0);">，借助云计算与大数据算法进行统一处理、分析与建模，从而形成跨行业、跨场景的智能视觉决策能力。该平台不仅能为城市管理、产业生产、公共安全提供精准指导，还能让企业通过视觉数据洞察市场、优化流程，实现“从感知到认知”的跃升。投资这样一个平台，意味着构建未来智能社会的视觉中枢，让数据真正“看得见、看得懂、用得上”。</font>

**<font style="color:rgb(0, 0, 0);">架构演进（期望版）：</font>**<font style="color:rgb(0, 0, 0);">  
</font><font style="color:rgb(0, 0, 0);">计算机视觉正成为人工智能时代的核心驱动力，但在 Web3.0 时代，我们正在推动它迈向去中心化的新阶段。基于区块链与智能合约，我们构建了一个去中心化视觉智能网络：全球的摄像头、无人机、机器人与传感器节点通过开放协议接入网络，数据在上链后自动确权与加密，任何分析、调用或模型训练都通过智能合约结算。参与者可通过代币获得激励，实现数据价值的共享与流通。</font>

<font style="color:rgb(0, 0, 0);">该网络不再依赖单一数据中心，而由多方节点共同维护，保障数据透明、安全与可信。随着 DAO 治理机制的引入，视觉算法、数据标准与生态规则将由社区共同制定。这不仅是计算机视觉的基础设施，更是智能社会的“视觉 Web3 中枢”，让数据真正实现从感知到认知、从集中到共治的跃升。</font>

## 架构设计

![](https://cdn.nlark.com/yuque/__puml/ec88039f8736a71c6844822f1eece782.svg)

### 假设我现在要调用一个API

#### 环境准备

首先安装我们的客户端，并且配置对应的依赖

```xml
<dependency>
  <groupId>com.yupi</groupId>
  <artifactId>yuapi-client-sdk</artifactId>
  <version>0.0.1</version>
</dependency>
```

在yaml里面配置sk、ak

```yaml
yuapi:
  client:
    access-key: yupi
    secret-key: abcdefgh
```

#### client客户端注册使用流程：

```java
@Resource
private YuApiClient yuApiClient;
```

这里已经完成了类型的注入

```java
@Configuration
@ConfigurationProperties("yuapi.client")
@Data
@ComponentScan
public class YuApiClientConfig {

    private String accessKey;

    private String secretKey;

    @Bean
    public YuApiClient yuApiClient() {
        return new YuApiClient(accessKey, secretKey);
    }

}
```

之后请求方法调用

```java
user.setUsername("test");
String username = yuApiClient.getUsernameByPost(user);
```

请求通过clientSDK里面的端口转发

```java
public String getUsernameByPost(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post(gatewayHost + "/api/name/user")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }
```

这里的8090端口就是请求的网关地址

```yaml
# 公共配置文件
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://yupi.icu">编程导航知识星球</a>
server:
  port: 8090
spring:
  cloud:
    gateway:
      default-filters:
        - AddResponseHeader=source, yupi
      routes:
        # AI测试接口路由 - 放在前面，优先级更高
        - id: ai-test-route
          uri: http://localhost:8123
          predicates:
            - Path=/api/ai/**
          filters:
            - StripPrefix=0
          order: 1
            
        # 通用API路由 - 放在后面
        - id: api_route
          uri: http://localhost:8123
          predicates:
            - Path=/api/name/**
          filters:
            - StripPrefix=0
          order: 2
            
        # 兜底路由 - 匹配所有其他/api请求
        - id: fallback-route
          uri: http://localhost:8123
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=0
          order: 3

logging:
  level:
    org.springframework.cloud.gateway: trace

# 原创_项目 [鱼皮](https://space.bilibili.com/12890453/)

dubbo:
  application:
    name: dubbo-springboot-demo-provider
  protocol:
    name: dubbo
    port: -1
  registry:
    id: nacos-registry
    address: nacos://localhost:8848
```

这里对向网关发送的消息做了加密校验

```java
private Map<String, String> getHeaderMap(String body) {
Map<String, String> hashMap = new HashMap<>();
hashMap.put("accessKey", accessKey);
// 一定不能直接发送
//        hashMap.put("secretKey", secretKey);
hashMap.put("nonce", RandomUtil.randomNumbers(4));
hashMap.put("body", body);
hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
hashMap.put("sign", genSign(body, secretKey));
return hashMap;
}
```

#### gateway（负责流量染色、鉴权、过滤）：

网关判断数据用户发送的数据是否被篡改

```java
// 3. 用户鉴权（判断 ak、sk 是否合法）
HttpHeaders headers = request.getHeaders();
String accessKey = headers.getFirst("accessKey");
String nonce = headers.getFirst("nonce");
String timestamp = headers.getFirst("timestamp");
String sign = headers.getFirst("sign");
String body = headers.getFirst("body");
// todo 实际情况应该是去数据库中查是否已分配给用户
User invokeUser = null;
try {
    invokeUser = innerUserService.getInvokeUser(accessKey);
} catch (Exception e) {
    log.error("getInvokeUser error", e);
}
if (invokeUser == null) {
    return handleNoAuth(response);
}
//        if (!"yupi".equals(accessKey)) {
//            return handleNoAuth(response);
//        }
if (Long.parseLong(nonce) > 10000L) {
    return handleNoAuth(response);
}
// 时间和当前时间不能超过 5 分钟
Long currentTime = System.currentTimeMillis() / 1000;
final Long FIVE_MINUTES = 60 * 5L;
if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES) {
    return handleNoAuth(response);
}
// 实际情况中是从数据库中查出 secretKey
String secretKey = invokeUser.getSecretKey();
String serverSign = SignUtils.genSign(body, secretKey);
if (sign == null || !sign.equals(serverSign)) {
    return handleNoAuth(response);
}
```

自定义响应结果处理器

```java
return handleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());
```

#### interface（实际调用）：

```java
@PostMapping("/user")
public String getUsernameByPost(@RequestBody User user, HttpServletRequest request)
```

# <font style="color:rgb(0, 0, 0);">Client模块补充设计</font>

<font style="color:rgb(0, 0, 0);">根据您提供的图片描述（API平台架构设计图），Client模块位于Java端的左侧，负责处理用户交互的部分，主要涉及参数的收集和传递（如ak、sk、model（type, stream）等）。图片中强调Client需要提供这些必需参数，以支持与Python端的模型服务和接口调用服务的交互。整个架构采用Java-Python混合模式，Java端聚焦用户侧交互，Python端处理模型部署和接口展示。</font>

<font style="color:rgb(0, 0, 0);">在您的设计中，Java端已明确包括Client、Interface、Gateway、Common四个模块。Client模块作为用户入口，应负责用户认证、请求构建、参数校验和初步的API调用准备。它需要与Interface模块（可能处理接口定义）、Gateway模块（网关路由和安全控制）和Common模块（公共工具如配置、日志）紧密协作。以下是对Client模块的补充设计，我会参考图片中的Client部分（参数要求和与Python端的箭头连接），扩展其功能、结构和实现建议，确保与整体架构一致。</font>

#### <font style="color:rgb(0, 0, 0);">1. Client模块的核心功能</font>

<font style="color:rgb(0, 0, 0);">Client模块的主要职责是作为用户与平台的桥梁，收集用户输入参数，并构建安全的API请求。参考图片：</font>

+ **<font style="color:rgb(0, 0, 0);">参数收集</font>**<font style="color:rgb(0, 0, 0);">：图片中明确列出ak（访问密钥）、sk（秘密密钥）、model（模型类型，如YOLO、OCR；stream表示是否流式处理）。补充：Client应支持动态参数扩展，例如添加timeout（超时时间）、input_data（输入数据，如图像URL或文本）。</font>
+ **<font style="color:rgb(0, 0, 0);">用户交互</font>**<font style="color:rgb(0, 0, 0);">：支持命令行、SDK或Web客户端形式，便于用户调用AI模型。</font>
+ **<font style="color:rgb(0, 0, 0);">认证与安全</font>**<font style="color:rgb(0, 0, 0);">：使用ak/sk进行签名验证，防止未授权访问。</font>
+ **<font style="color:rgb(0, 0, 0);">请求构建</font>**<font style="color:rgb(0, 0, 0);">：将参数封装成HTTP/GRPC请求，发送到Gateway模块。</font>
+ **<font style="color:rgb(0, 0, 0);">错误处理</font>**<font style="color:rgb(0, 0, 0);">：捕获参数无效或网络错误，返回友好提示。</font>
+ **<font style="color:rgb(0, 0, 0);">与Python端的集成</font>**<font style="color:rgb(0, 0, 0);">：通过nacos-Dubbo（图片中提到）调用Python接口服务，确保参数兼容Python模型服务的独特接口（e.g., YOLO的特定输入格式）。</font>

#### <font style="color:rgb(0, 0, 0);">2. Client模块的结构设计</font>

<font style="color:rgb(0, 0, 0);">建议采用分层结构，便于维护和扩展。模块内部可分为以下子组件：</font>

+ **<font style="color:rgb(0, 0, 0);">ClientCore</font>**<font style="color:rgb(0, 0, 0);">：核心类，负责参数解析和请求发起。</font>
+ **<font style="color:rgb(0, 0, 0);">AuthHandler</font>**<font style="color:rgb(0, 0, 0);">：认证处理器，处理ak/sk签名。</font>
+ **<font style="color:rgb(0, 0, 0);">ParamValidator</font>**<font style="color:rgb(0, 0, 0);">：参数校验器，确保输入符合模型要求。</font>
+ **<font style="color:rgb(0, 0, 0);">RequestBuilder</font>**<font style="color:rgb(0, 0, 0);">：请求构建器，生成标准化请求对象。</font>
+ **<font style="color:rgb(0, 0, 0);">ResponseParser</font>**<font style="color:rgb(0, 0, 0);">：响应解析器，处理从Gateway返回的数据（参考图片中Python接口的返回格式）。</font>

<font style="color:rgb(0, 0, 0);">使用Common模块的公共工具（如日志记录、配置加载）来支持这些子组件。</font>

#### <font style="color:rgb(0, 0, 0);">3. 关键类和方法设计</font>

<font style="color:rgb(0, 0, 0);">以下是详细的类设计示例，使用Java语言伪代码表示。参考图片的Client参数列表，我补充了更多方法以支持AI模型的多样性（e.g., 支持批处理、流式响应）。</font>

```java
// 包路径：com.example.api.client

import com.example.api.common.ConfigLoader;  // 来自Common模块
import com.example.api.common.LoggerUtil;    // 来自Common模块
import com.example.api.gateway.GatewayClient; // 与Gateway模块交互

public class ApiClient {

    private String ak;
    private String sk;
    private String baseUrl;  // 从Common模块加载配置

    // 构造函数：初始化ak/sk
    public ApiClient(String ak, String sk) {
        this.ak = ak;
        this.sk = sk;
        this.baseUrl = ConfigLoader.getBaseUrl();  // 使用Common模块加载配置
    }

    // 方法：构建并发送模型调用请求
    // 参考图片参数：model(type, stream)，补充inputData, timeout
    public ApiResponse invokeModel(String modelType, boolean isStream, Object inputData, int timeout) {
        // 参数校验
        if (modelType == null || inputData == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }

        // 签名认证
        String signature = AuthHandler.generateSignature(ak, sk, modelType);  // 使用AuthHandler

        // 构建请求
        Request request = RequestBuilder.build()
                .setHeader("ak", ak)
                .setHeader("signature", signature)
                .setParam("model", modelType)
                .setParam("stream", isStream)
                .setParam("input", inputData)
                .setParam("timeout", timeout)
                .build();

        // 通过Gateway发送请求（参考图片箭头连接到Python端）
        ApiResponse response = GatewayClient.sendRequest(baseUrl + "/invoke", request);

        // 解析响应（兼容Python返回格式，如JSON with status, data, error）
        return ResponseParser.parse(response);
    }

    // 补充方法：批处理调用，支持多个模型输入
    public List<ApiResponse> batchInvoke(List<ModelInput> inputs) {
        List<ApiResponse> results = new ArrayList<>();
        for (ModelInput input : inputs) {
            results.add(invokeModel(input.getModelType(), input.isStream(), input.getData(), input.getTimeout()));
        }
        return results;
    }

    // 错误处理示例
    private void handleError(Exception e) {
        LoggerUtil.logError("Client error: " + e.getMessage());  // 使用Common模块日志
        throw new ApiException("Invocation failed", e);
    }
}

// 辅助类：认证处理器
class AuthHandler {
    public static String generateSignature(String ak, String sk, String modelType) {
        // 使用HMAC-SHA256等算法生成签名（参考ak/sk安全机制）
        return "generated_signature";  // 伪代码
    }
}

// 辅助类：请求构建器（Builder模式，便于扩展）
class RequestBuilder {
    private Map<String, Object> params = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();

    public RequestBuilder setParam(String key, Object value) {
        params.put(key, value);
        return this;
    }

    public RequestBuilder setHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public Request build() {
        return new Request(params, headers);  // Request类定义在Common模块
    }
}

// 辅助类：响应解析器
class ResponseParser {
    public static ApiResponse parse(ApiResponse rawResponse) {
        // 解析JSON，提取data/error（参考图片Python返回格式）
        if (rawResponse.getStatus() != 200) {
            throw new ApiException(rawResponse.getErrorMsg());
        }
        return rawResponse;
    }
}
```

#### <font style="color:rgb(0, 0, 0);">4. 与其他模块的交互</font>

+ **<font style="color:rgb(0, 0, 0);">与Interface模块</font>**<font style="color:rgb(0, 0, 0);">：Client使用Interface定义的API契约（如模型接口规范），确保参数类型匹配Python模型服务（e.g., YOLO需要图像输入）。</font>
+ **<font style="color:rgb(0, 0, 0);">与Gateway模块</font>**<font style="color:rgb(0, 0, 0);">：Client将请求路由到Gateway，进行限流、负载均衡和安全过滤后，再转发到Python端（图片中箭头表示工作流）。</font>
+ **<font style="color:rgb(0, 0, 0);">与Common模块</font>**<font style="color:rgb(0, 0, 0);">：依赖Common的配置、日志和异常处理工具，提高复用性。</font>
+ **<font style="color:rgb(0, 0, 0);">与Python端</font>**<font style="color:rgb(0, 0, 0);">：间接交互，通过Dubbo调用Python接口服务。补充：Client应处理Python算法服务的参数鲁棒性（图片中强调），如自动填充默认值。</font>

#### <font style="color:rgb(0, 0, 0);">5. 设计注意事项和优化建议</font>

+ **<font style="color:rgb(0, 0, 0);">安全性</font>**<font style="color:rgb(0, 0, 0);">：ak/sk应加密存储，避免明文传输。补充token-based认证以支持长期会话。</font>
+ **<font style="color:rgb(0, 0, 0);">性能</font>**<font style="color:rgb(0, 0, 0);">：对于流式模型（stream=true），Client支持异步调用，使用Java的CompletableFuture。</font>
+ **<font style="color:rgb(0, 0, 0);">可扩展性</font>**<font style="color:rgb(0, 0, 0);">：使用注解或配置文件动态注册新模型类型，适应图片中提到的多种模型（YOLO、OCR、3D算法）。</font>
+ **<font style="color:rgb(0, 0, 0);">测试</font>**<font style="color:rgb(0, 0, 0);">：编写单元测试覆盖参数校验和签名逻辑；集成测试模拟Python端响应。</font>
+ **<font style="color:rgb(0, 0, 0);">潜在问题</font>**<font style="color:rgb(0, 0, 0);">：如果Python端接口不统一（图片中提到独特接口），Client需添加适配器层。</font>
+ **<font style="color:rgb(0, 0, 0);">参考图片扩展</font>**<font style="color:rgb(0, 0, 0);">：图片中有“分支主题7”，可能表示扩展子主题，建议Client支持插件化，如添加自定义参数处理器。</font>

<font style="color:rgb(0, 0, 0);">这个补充设计保持了图片的整体架构一致性，如果您提供更多细节（如具体模型类型或Interface模块定义），我可以进一步细化。</font>



```sql
-- 创建库
CREATE DATABASE IF NOT EXISTS duola;

-- 切换库
USE duola;

-- =================================================================
-- 1. 用户中心 (UAM)
-- =================================================================

-- 用户表 (优化)
-- 增加了 status 字段，用于管理用户状态，比 isDelete 的业务含义更清晰。
CREATE TABLE IF NOT EXISTS `user`
(
  `id`             BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
  `userAccount`    VARCHAR(256)                           NOT NULL COMMENT '账号',
  `userPassword`   VARCHAR(512)                           NOT NULL COMMENT '密码',
  `userName`       VARCHAR(256)                           NULL COMMENT '用户昵称',
  `userAvatar`     VARCHAR(1024)                          NULL COMMENT '用户头像',
  `gender`         TINYINT                                NULL COMMENT '性别 (0-女, 1-男)',
  `userRole`       VARCHAR(256) DEFAULT 'user'            NOT NULL COMMENT '用户角色：user / admin',
  `accessKey`      VARCHAR(512)                           NOT NULL COMMENT 'Access Key (AK)',
  `secretKey`      VARCHAR(512)                           NOT NULL COMMENT 'Secret Key (SK)',
  `status`         INT DEFAULT 0                          NOT NULL COMMENT '账号状态 (0-正常, 1-禁用)',
  `createTime`     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
  `updateTime`     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete`       TINYINT      DEFAULT 0                 NOT NULL COMMENT '是否删除',
  CONSTRAINT `uni_userAccount` UNIQUE (`userAccount`),
  CONSTRAINT `uni_accessKey` UNIQUE (`accessKey`)
) COMMENT '用户信息表';


-- =================================================================
-- 2. 模型与接口管理
-- =================================================================

-- AI模型信息表 (新增)
-- 对应"模型生命周期管理"微服务，管理底层的AI模型资产。
CREATE TABLE IF NOT EXISTS `model_info`
(
  `id`           BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
  `modelName`    VARCHAR(256) NOT NULL COMMENT '模型名称 (如: qwen-plus, deepseek-coder)',
  `modelType`    VARCHAR(128) NOT NULL COMMENT '模型类型 (如: LLM, Image, Embedding)',
  `provider`     VARCHAR(256) NULL COMMENT '模型提供方 (如: Alibaba, Google)',
  `status`       INT DEFAULT 0 NOT NULL COMMENT '模型状态 (0-规划中, 1-已上线, 2-已下线)',
  `creatorId`    BIGINT       NOT NULL COMMENT '创建人ID',
  `createTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
  `updateTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete`     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT 'AI模型信息表';


-- 接口信息表 (优化)
-- 关联了`model_info`，明确了每个对外接口背后由哪个AI模型提供服务。
CREATE TABLE IF NOT EXISTS `interface_info`
(
  `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
  `name`            VARCHAR(256)                           NOT NULL COMMENT '接口名称',
  `description`     VARCHAR(512)                           NULL COMMENT '描述',
  `url`             VARCHAR(512)                           NOT NULL COMMENT '接口地址 (网关路由路径)',
  `method`          VARCHAR(256)                           NOT NULL COMMENT 'HTTP请求类型',
    `modelId`         BIGINT                                 NULL COMMENT '关联的模型ID',
    `requestHeader`   TEXT                                   NULL COMMENT '请求头示例',
    `responseHeader`  TEXT                                   NULL COMMENT '响应头示例',
    `status`          INT          DEFAULT 0                 NOT NULL COMMENT '接口状态 (0-关闭, 1-开启)',
    `creatorId`       BIGINT                                 NOT NULL COMMENT '创建人ID',
    `createTime`      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime`      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`        TINYINT      DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX `idx_modelId` (`modelId`)
) COMMENT '平台接口信息表';


-- =================================================================
-- 3. 计量与授权中心
-- =================================================================

-- 用户调用接口关系表 (保留核心设计)
-- 这张表现在是内测资格和额度的核心。管理员通过操作这张表为用户授权。
CREATE TABLE IF NOT EXISTS `user_interface_info`
(
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    `userId`          BIGINT                             NOT NULL COMMENT '调用用户ID',
    `interfaceInfoId` BIGINT                             NOT NULL COMMENT '接口ID',
    `totalNum`        INT      DEFAULT 0                 NOT NULL COMMENT '总调用次数',
    `leftNum`         INT      DEFAULT 0                 NOT NULL COMMENT '剩余调用次数',
    `status`          INT      DEFAULT 0                 NOT NULL COMMENT '授权状态 (0-正常, 1-禁用)',
    `createTime`      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime`      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`        TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    -- 确保一个用户对一个接口只有一条授权记录
    UNIQUE KEY `uni_user_interface` (`userId`, `interfaceInfoId`)
) COMMENT '用户接口调用授权表';


-- 接口调用日志表 (新增)
-- 对应"计量计费中心"的计量功能，为每一次调用提供凭据，用于扣减次数和问题排查。
CREATE TABLE IF NOT EXISTS `invocation_log`
(
    `id`               BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    `userId`           BIGINT                                 NOT NULL COMMENT '调用用户ID',
    `interfaceInfoId`  BIGINT                                 NOT NULL COMMENT '接口ID',
    `requestParams`    TEXT                                   NULL COMMENT '请求参数 (用于调试)',
    `responseContent`  TEXT                                   NULL COMMENT '响应内容 (用于调试)',
    `ip`               VARCHAR(128)                           NULL COMMENT '请求IP地址',
    `status`           INT                                    NOT NULL COMMENT '调用状态 (0-成功, 1-失败)',
    `errorMessage`     TEXT                                   NULL COMMENT '失败时的错误信息',
    `invocationTime`   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '调用时间',
    -- 建立索引以加速查询
    INDEX `idx_userId` (`userId`),
    INDEX `idx_interfaceInfoId` (`interfaceInfoId`)
) COMMENT '接口调用日志表';


```

<font style="color:rgb(0, 0, 0);">没问题。我把“</font>**<font style="color:rgb(0, 0, 0);">一次调用从数据接收→传输→推理→结果返回</font>**<font style="color:rgb(0, 0, 0);">”按</font>**<font style="color:rgb(0, 0, 0);">同步</font>**<font style="color:rgb(0, 0, 0);">与</font>**<font style="color:rgb(0, 0, 0);">异步</font>**<font style="color:rgb(0, 0, 0);">两条链路梳理，并在每一跳标注</font>**<font style="color:rgb(0, 0, 0);">谁是客户端（client）谁是服务端（server）</font>**<font style="color:rgb(0, 0, 0);">。同时把和你</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">.proto</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">对应的消息/服务也标出来，便于你核对实现。</font>

---

# <font style="color:rgb(0, 0, 0);">同步推理（小图/低时延）</font>

**<font style="color:rgb(0, 0, 0);">目标</font>**<font style="color:rgb(0, 0, 0);">：前端发一张中小图片，几百毫秒得到结果。</font>

1. **<font style="color:rgb(0, 0, 0);">Browser → API Gateway（REST/HTTPS）</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：Browser</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：API Gateway（对外入口）</font>
   - <font style="color:rgb(0, 0, 0);">行为：上传</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">image</font>`<font style="color:rgb(0, 0, 0);">（小图可直接 base64/bytes，大图推荐走对象存储直传，详见异步）；携带认证头。</font>
   - <font style="color:rgb(0, 0, 0);">结果：网关完成认证授权，把请求转给 Java。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：外层 REST 对应内部将要调用的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">vision.v1</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">gRPC。  
     </font><font style="color:rgb(0, 0, 0);">（说明性步骤，无对应 proto 字段）</font>
2. **<font style="color:rgb(0, 0, 0);">API Gateway → Java 编排层（内部 REST/HTTP）</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：API Gateway</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：Java Orchestrator</font>
   - <font style="color:rgb(0, 0, 0);">行为：把已验证的调用（含最小必要上下文）转到编排层；做限流/重试/路由决策。  
     </font><font style="color:rgb(0, 0, 0);">（说明性步骤）</font>
3. **<font style="color:rgb(0, 0, 0);">Java 编排层 → Python 模型服务（gRPC）</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Java</font>**<font style="color:rgb(0, 0, 0);">（gRPC 客户端）</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Python</font>**<font style="color:rgb(0, 0, 0);">（gRPC 服务器）</font>
   - <font style="color:rgb(0, 0, 0);">行为：</font>
     * <font style="color:rgb(0, 0, 0);">Java 选择合适的 Python 实例（服务发现/负载均衡）。</font>
     * <font style="color:rgb(0, 0, 0);">组装</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceRequest</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">header</font>`<font style="color:rgb(0, 0, 0);">（模型/选项/trace…）+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">image</font>`<font style="color:rgb(0, 0, 0);">（</font>`<font style="color:rgb(0, 0, 0);">uri</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">content</font>`<font style="color:rgb(0, 0, 0);">）后发起 gRPC 调用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">YoloService/DepthService.Infer</font>`<font style="color:rgb(0, 0, 0);">。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：</font>`<font style="color:rgb(0, 0, 0);">InferenceRequest</font>`<font style="color:rgb(0, 0, 0);">、</font>`<font style="color:rgb(0, 0, 0);">ImageRef</font>`<font style="color:rgb(0, 0, 0);">、</font>`<font style="color:rgb(0, 0, 0);">InferenceResponse</font>`<font style="color:rgb(0, 0, 0);">、</font>`<font style="color:rgb(0, 0, 0);">YoloService/DepthService.Infer</font>`<font style="color:rgb(0, 0, 0);">。  
     </font><font style="color:rgb(0, 0, 0);">例如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceRequest</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的结构与</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">oneof result</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">回包已在 proto 中定义 ，服务签名见</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">YoloService/DepthService</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">。  
     </font>`<font style="color:rgb(0, 0, 0);">ImageRef</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">支持</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">uri</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">content</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">两种来源（大图建议</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">uri</font>`<font style="color:rgb(0, 0, 0);">） 。</font>
4. **<font style="color:rgb(0, 0, 0);">Python 模型服务内部处理（推理）</font>**
   - **<font style="color:rgb(0, 0, 0);">Server 端逻辑</font>**<font style="color:rgb(0, 0, 0);">：</font>
     * <font style="color:rgb(0, 0, 0);">解码</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ImageRef</font>`<font style="color:rgb(0, 0, 0);">（content/uri）→ 预处理 → 推理 → 组织强类型结果（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Detections</font>`<font style="color:rgb(0, 0, 0);">）→</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceResponse</font>`<font style="color:rgb(0, 0, 0);">。</font>
   - <font style="color:rgb(0, 0, 0);">参考：解码/推理/组装响应的示例代码片段（</font>`<font style="color:rgb(0, 0, 0);">_decode_image</font>`<font style="color:rgb(0, 0, 0);">、打包</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">DetectResponse</font>`<font style="color:rgb(0, 0, 0);">）在样例里有示范，可对应你现有服务改造成</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">vision.v1</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">结构 。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：结果通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceResponse.result</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">oneof</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">返回（检测/分割/深度/分类/向量） 。</font>
5. **<font style="color:rgb(0, 0, 0);">Python → Java（gRPC 回包）</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Java</font>**<font style="color:rgb(0, 0, 0);">（继续等待响应）</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Python</font>**<font style="color:rgb(0, 0, 0);">（返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceResponse</font>`<font style="color:rgb(0, 0, 0);">）</font>
   - <font style="color:rgb(0, 0, 0);">行为：Python 将</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceResponse</font>`<font style="color:rgb(0, 0, 0);">（含</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">header</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">回显、</font>`<font style="color:rgb(0, 0, 0);">result</font>`<font style="color:rgb(0, 0, 0);">、可选</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">debug_uris</font>`<font style="color:rgb(0, 0, 0);">、</font>`<font style="color:rgb(0, 0, 0);">status</font>`<font style="color:rgb(0, 0, 0);">、时间戳）返回给 Java。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：</font>`<font style="color:rgb(0, 0, 0);">InferenceResponse</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">字段定义见此 。</font>
6. **<font style="color:rgb(0, 0, 0);">Java → API Gateway → Browser（REST/HTTPS 回包）</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：Browser</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：API Gateway（对外）/Java（内部）</font>
   - <font style="color:rgb(0, 0, 0);">行为：Java 将 gRPC 结果转为统一 REST 响应体，网关经 HTTPS 返回给用户。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：Java 在 REST 层将</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Box2D/CoordType</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">等按前端约定统一（通常转为像素坐标） 。</font>

---

# <font style="color:rgb(0, 0, 0);">异步批处理（大图/视频/长耗时）</font>

**<font style="color:rgb(0, 0, 0);">目标</font>**<font style="color:rgb(0, 0, 0);">：素材大、耗时长；通过任务编排与对象存储交付结果。</font>

1. **<font style="color:rgb(0, 0, 0);">Browser 直传对象存储 + 提交任务（REST/HTTPS）</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：Browser</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：API Gateway / Java</font>
   - <font style="color:rgb(0, 0, 0);">行为：</font>
     * <font style="color:rgb(0, 0, 0);">先拿预签名 URL 直传素材到对象存储（S3/MinIO）；</font>
     * <font style="color:rgb(0, 0, 0);">用素材</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">uri</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+ 批参数调用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">POST /submit</font>`<font style="color:rgb(0, 0, 0);">，由 Java 生成并返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">task_id</font>`<font style="color:rgb(0, 0, 0);">。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：对应 gRPC 的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">VisionTaskService.Submit(SubmitTaskRequest)</font>`<font style="color:rgb(0, 0, 0);">；批内仍是</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">BatchInferenceRequest</font>`<font style="color:rgb(0, 0, 0);">（多条</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceRequest</font>`<font style="color:rgb(0, 0, 0);">） 。</font>
2. **<font style="color:rgb(0, 0, 0);">Java 写入任务表 & 投递队列</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Java</font>**<font style="color:rgb(0, 0, 0);">（写 DB/MQ）</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：DB / MQ</font>
   - <font style="color:rgb(0, 0, 0);">行为：将</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">task_id</font>`<font style="color:rgb(0, 0, 0);">、素材</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">uri</font>`<font style="color:rgb(0, 0, 0);">、模型/参数等落库并投递队列；状态置</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">PENDING</font>`<font style="color:rgb(0, 0, 0);">。</font>
   - <font style="color:rgb(0, 0, 0);">流程概述在“异步批处理”小节中已有描述（状态推进、对象存储交付） 。</font>
3. **<font style="color:rgb(0, 0, 0);">Python Worker 拉取任务并推理</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Python Worker</font>**<font style="color:rgb(0, 0, 0);">（消费 MQ / 读对象存储）</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：MQ / 对象存储</font>
   - <font style="color:rgb(0, 0, 0);">行为：加载素材</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">uri</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">→ gRPC 内部同样走</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Inference*</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">结构处理（或本地直接推理）→ 中间产物/最终文件写对象存储（记录到</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">artifacts</font>`<font style="color:rgb(0, 0, 0);">）。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：最终状态通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">TaskStatus</font>`<font style="color:rgb(0, 0, 0);">（</font>`<font style="color:rgb(0, 0, 0);">total/completed/last_error/artifacts</font>`<font style="color:rgb(0, 0, 0);">）与</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">GetResult</font>`<font style="color:rgb(0, 0, 0);">返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">BatchInferenceResponse</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">。</font>
4. **<font style="color:rgb(0, 0, 0);">Browser 轮询/订阅任务状态并获取结果</font>**
   - **<font style="color:rgb(0, 0, 0);">Client</font>**<font style="color:rgb(0, 0, 0);">：Browser</font>
   - **<font style="color:rgb(0, 0, 0);">Server</font>**<font style="color:rgb(0, 0, 0);">：API Gateway / Java</font>
   - <font style="color:rgb(0, 0, 0);">行为：轮询</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">task_id</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或通过 SSE/WebSocket 订阅；完成后获得结果 JSON 与产物 URI。</font>
   - <font style="color:rgb(0, 0, 0);">与 proto 的关系：</font>`<font style="color:rgb(0, 0, 0);">VisionTaskService.GetStatus / GetResult</font>`<font style="color:rgb(0, 0, 0);">；</font>`<font style="color:rgb(0, 0, 0);">GetTaskResultResponse</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">带</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">batch</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">status</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">。</font>

---

## <font style="color:rgb(0, 0, 0);">Java ⇄ Python：谁是 client 谁是 server（总表）</font>

+ **<font style="color:rgb(0, 0, 0);">同步调用</font>**<font style="color:rgb(0, 0, 0);">：</font>
  - **<font style="color:rgb(0, 0, 0);">Java = gRPC 客户端</font>**<font style="color:rgb(0, 0, 0);">（发起</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Infer/BatchInfer</font>`<font style="color:rgb(0, 0, 0);">）</font>
  - **<font style="color:rgb(0, 0, 0);">Python = gRPC 服务端</font>**<font style="color:rgb(0, 0, 0);">（实现</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">YoloService/DepthService</font>`<font style="color:rgb(0, 0, 0);">）</font><font style="color:rgb(0, 0, 0);"> </font>
+ **<font style="color:rgb(0, 0, 0);">异步编排</font>**<font style="color:rgb(0, 0, 0);">：</font>
  - **<font style="color:rgb(0, 0, 0);">Java = REST 服务端 + 队列生产者 + 任务查询服务端</font>**<font style="color:rgb(0, 0, 0);">（</font>`<font style="color:rgb(0, 0, 0);">Submit/GetStatus/GetResult</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">对外，内部写 DB/MQ）</font><font style="color:rgb(0, 0, 0);"> </font>
  - **<font style="color:rgb(0, 0, 0);">Python = 队列消费者/Worker &（可选）推理 gRPC 服务端</font>**<font style="color:rgb(0, 0, 0);">（拿任务做推理，产物落对象存储）</font><font style="color:rgb(0, 0, 0);"> </font>

---

## <font style="color:rgb(0, 0, 0);">与</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">.proto</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的关键映射（你实现时最常用）</font>

+ **<font style="color:rgb(0, 0, 0);">请求/响应</font>**<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">InferenceRequest</font>`<font style="color:rgb(0, 0, 0);">（含</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">header</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ImageRef</font>`<font style="color:rgb(0, 0, 0);">）→</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceResponse</font>`<font style="color:rgb(0, 0, 0);">（</font>`<font style="color:rgb(0, 0, 0);">oneof result</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">status</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">debug_uris</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+ 时间戳）</font><font style="color:rgb(0, 0, 0);"> </font>
+ **<font style="color:rgb(0, 0, 0);">图像载荷</font>**<font style="color:rgb(0, 0, 0);">：小图可用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ImageRef.content</font>`<font style="color:rgb(0, 0, 0);">；大图/视频走</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ImageRef.uri</font>`<font style="color:rgb(0, 0, 0);">（推荐）</font><font style="color:rgb(0, 0, 0);"> </font>
+ **<font style="color:rgb(0, 0, 0);">批处理</font>**<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">BatchInferenceRequest/Response</font>`<font style="color:rgb(0, 0, 0);">（同构批量）</font><font style="color:rgb(0, 0, 0);"> </font>
+ **<font style="color:rgb(0, 0, 0);">异步任务</font>**<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">VisionTaskService.Submit/GetStatus/GetResult</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">TaskStatus.artifacts</font>`<font style="color:rgb(0, 0, 0);">（产物 URI）</font><font style="color:rgb(0, 0, 0);"> </font>
+ **<font style="color:rgb(0, 0, 0);">检测/分割/深度/分类/向量</font>**<font style="color:rgb(0, 0, 0);">：在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceResponse.result</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">oneof</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">下分型返回，避免“Object”失去类型信息 。</font>

---

## <font style="color:rgb(0, 0, 0);">一句话记忆</font>

+ **<font style="color:rgb(0, 0, 0);">对外</font>**<font style="color:rgb(0, 0, 0);">：Browser 是</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">REST 客户端</font>**<font style="color:rgb(0, 0, 0);">，Java 网关/编排是</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">REST 服务端</font>**<font style="color:rgb(0, 0, 0);">。</font>
+ **<font style="color:rgb(0, 0, 0);">对内</font>**<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">Java 是 gRPC 客户端</font>**<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">发请求，</font>**<font style="color:rgb(0, 0, 0);">Python 是 gRPC 服务端</font>**<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">做推理并回包；</font>**<font style="color:rgb(0, 0, 0);">异步</font>**<font style="color:rgb(0, 0, 0);">时 Python 也作为</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">Worker 客户端</font>**<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">去拉队列和对象存储。</font>
+ **<font style="color:rgb(0, 0, 0);">数据大就走 URI，小就走 bytes；回包用强类型 oneof</font>**<font style="color:rgb(0, 0, 0);">（而不是任意 object），这样编排、存储、前端都更稳。</font><font style="color:rgb(0, 0, 0);"> </font>

<font style="color:rgb(0, 0, 0);">需要的话，我可以把上面两条链路画成一张</font>**<font style="color:rgb(0, 0, 0);">时序图（Java/Python 标注清晰）</font>**<font style="color:rgb(0, 0, 0);">，并给出 Java gRPC 客户端与 Python </font>`<font style="color:rgb(0, 0, 0);">grpc.aio</font>`<font style="color:rgb(0, 0, 0);"> 服务端的</font>**<font style="color:rgb(0, 0, 0);">最小模板</font>**<font style="color:rgb(0, 0, 0);">，直接替换你现有的 REST 调用。</font>

<font style="color:rgb(0, 0, 0);">下面是一份面向</font>**<font style="color:rgb(0, 0, 0);">后端/SDK/前端网关</font>**<font style="color:rgb(0, 0, 0);">开发者的“可落地、可对齐联调”的</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">InferenceStreamService.proto 详细说明文档</font>**<font style="color:rgb(0, 0, 0);">。内容覆盖接口语义、消息结构、时序约定、错误与状态、示例载荷、版本与兼容、性能与安全建议等，便于你直接据此实现客户端/服务端或编写网关适配层。</font>

---

# <font style="color:rgb(0, 0, 0);">1) 总览</font>

**<font style="color:rgb(0, 0, 0);">服务名</font>**<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">InferenceStreamService</font>`<font style="color:rgb(0, 0, 0);">  
</font>**<font style="color:rgb(0, 0, 0);">RPC</font>**<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">rpc Stream (stream StreamRequest) returns (stream StreamResponse)</font>`<font style="color:rgb(0, 0, 0);">  
</font>**<font style="color:rgb(0, 0, 0);">模式</font>**<font style="color:rgb(0, 0, 0);">：gRPC</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">双向流</font>**<font style="color:rgb(0, 0, 0);">（bidi streaming），用于</font>**<font style="color:rgb(0, 0, 0);">序列数据在线推理</font>**<font style="color:rgb(0, 0, 0);">（如视频帧、语音分片、日志片段等）。</font>

**<font style="color:rgb(0, 0, 0);">典型流程</font>**

1. <font style="color:rgb(0, 0, 0);">客户端先发送</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamRequest.open</font>`<font style="color:rgb(0, 0, 0);">（携带</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceHeader</font>`<font style="color:rgb(0, 0, 0);">：模型、租户、追踪、推理选项、可接受结果格式等）。</font>
2. <font style="color:rgb(0, 0, 0);">客户端持续发送</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamRequest.frame</font>`<font style="color:rgb(0, 0, 0);">（一帧或一批输入，携带</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">与可选时间戳）。</font>
3. <font style="color:rgb(0, 0, 0);">服务端按帧回传</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamResponse.frame</font>`<font style="color:rgb(0, 0, 0);">，可</font>**<font style="color:rgb(0, 0, 0);">乱序</font>**<font style="color:rgb(0, 0, 0);">或</font>**<font style="color:rgb(0, 0, 0);">就序</font>**<font style="color:rgb(0, 0, 0);">（见 4.4）。</font>
4. <font style="color:rgb(0, 0, 0);">客户端发送</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamRequest.close</font>`<font style="color:rgb(0, 0, 0);">，服务端用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamResponse.ack</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">应答，随后任一端</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">onCompleted()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">关闭流。</font>

**<font style="color:rgb(0, 0, 0);">关键设计点</font>**

+ <font style="color:rgb(0, 0, 0);">输入与输出通过**信封（Envelope）**抽象：</font>`<font style="color:rgb(0, 0, 0);">InputEnvelope</font>`<font style="color:rgb(0, 0, 0);">、</font>`<font style="color:rgb(0, 0, 0);">ResultEnvelope</font>`<font style="color:rgb(0, 0, 0);">；支持 text / binary / JSON / Any 等多种载荷。</font>
+ <font style="color:rgb(0, 0, 0);">头部</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceHeader</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">统一携带模型、租户、追踪、选项与</font>**<font style="color:rgb(0, 0, 0);">内容协商</font>**<font style="color:rgb(0, 0, 0);">（</font>`<font style="color:rgb(0, 0, 0);">accept</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ `<font style="color:rgb(0, 0, 0);">CustomStatus</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">用于流级 ACK、心跳或错误信号的结构化返回。</font>
+ `<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">用于将结果与输入帧对齐。</font>

---

# <font style="color:rgb(0, 0, 0);">2) 消息/字段语义</font>

## <font style="color:rgb(0, 0, 0);">2.1 追踪与租户</font>

### `<font style="color:rgb(0, 0, 0);">TraceContext</font>`

+ `<font style="color:rgb(0, 0, 0);">trace_id</font>`<font style="color:rgb(0, 0, 0);">：一次业务调用的全局追踪 ID（与分布式链路跟踪系统对齐，如 OpenTelemetry）。</font>
+ `<font style="color:rgb(0, 0, 0);">span_id</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">parent_span_id</font>`<font style="color:rgb(0, 0, 0);">：当前/父级跨度，用于精细链路拼接。  
  </font>**<font style="color:rgb(0, 0, 0);">服务端建议</font>**<font style="color:rgb(0, 0, 0);">：将其注入日志、metrics 与 trace。</font>

### `<font style="color:rgb(0, 0, 0);">TenantContext</font>`

+ `<font style="color:rgb(0, 0, 0);">tenant_id</font>`<font style="color:rgb(0, 0, 0);">：租户/工作区/组织 ID。</font>
+ `<font style="color:rgb(0, 0, 0);">user_id</font>`<font style="color:rgb(0, 0, 0);">：触发该调用的用户或代理主体。</font>
+ `<font style="color:rgb(0, 0, 0);">attrs</font>`<font style="color:rgb(0, 0, 0);">：自定义 K/V（如 region, plan, project_id）。  
  </font>**<font style="color:rgb(0, 0, 0);">鉴权</font>**<font style="color:rgb(0, 0, 0);">：建议在服务端对</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">tenant_id/user_id</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">与证书/Token 进行一致性校验。</font>

## <font style="color:rgb(0, 0, 0);">2.2 状态与模型</font>

### `<font style="color:rgb(0, 0, 0);">CustomStatus</font>`

+ `<font style="color:rgb(0, 0, 0);">code</font>`<font style="color:rgb(0, 0, 0);">：应用级状态码（建议与 gRPC status 解耦，用作</font>**<font style="color:rgb(0, 0, 0);">业务态</font>**<font style="color:rgb(0, 0, 0);">，如 0=OK，>0=部分失败/限流/降级）。</font>
+ `<font style="color:rgb(0, 0, 0);">message</font>`<font style="color:rgb(0, 0, 0);">：人类可读描述。</font>
+ `<font style="color:rgb(0, 0, 0);">details</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">Any</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">列表（如限流策略、余额信息、错误定位等）。</font>

### `<font style="color:rgb(0, 0, 0);">ModelSpec</font>`

+ `<font style="color:rgb(0, 0, 0);">name</font>`<font style="color:rgb(0, 0, 0);">：模型名（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">yolo-v8</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">gpt-4-vision</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ `<font style="color:rgb(0, 0, 0);">version</font>`<font style="color:rgb(0, 0, 0);">：精确版本（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">8.0.3</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或模型 checkpoint hash）。</font>
+ `<font style="color:rgb(0, 0, 0);">tags</font>`<font style="color:rgb(0, 0, 0);">：额外标签（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">accelerator=gpu</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">quant=int8</font>`<font style="color:rgb(0, 0, 0);">）。</font>

## <font style="color:rgb(0, 0, 0);">2.3 会话头</font>

### `<font style="color:rgb(0, 0, 0);">InferenceHeader</font>`

+ `<font style="color:rgb(0, 0, 0);">model</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">ModelSpec</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">trace</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">TraceContext</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">tenant</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">TenantContext</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">options</font>`<font style="color:rgb(0, 0, 0);">：自由结构体（</font>`<font style="color:rgb(0, 0, 0);">Struct</font>`<font style="color:rgb(0, 0, 0);">），用于传参（阈值、NMS、top_k、max_tokens、temperature、语言等）。</font>
+ `<font style="color:rgb(0, 0, 0);">accept</font>`<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">内容协商</font>**<font style="color:rgb(0, 0, 0);">，声明客户端可接受的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ResultEnvelope.content_type</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">优先级/集合；例如</font>
  - `<font style="color:rgb(0, 0, 0);">["application/json", "application/x-protobuf;type=\"google.protobuf.Struct\""]</font>`
  - <font style="color:rgb(0, 0, 0);">约定支持逗号分隔权重或简单“优先靠前”（由实现决定）。</font>

## <font style="color:rgb(0, 0, 0);">2.4 输入/输出信封</font>

### `<font style="color:rgb(0, 0, 0);">InputEnvelope</font>`

+ `<font style="color:rgb(0, 0, 0);">kind</font>`<font style="color:rgb(0, 0, 0);">：输入类型（例</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"image"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"text"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"audio_chunk"</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ `<font style="color:rgb(0, 0, 0);">content_type</font>`<font style="color:rgb(0, 0, 0);">：MIME（例</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"image/jpeg"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"text/plain"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"application/json"</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ `<font style="color:rgb(0, 0, 0);">payload</font>`<font style="color:rgb(0, 0, 0);">（oneof）：</font>
  - `<font style="color:rgb(0, 0, 0);">message</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">Any</font>`<font style="color:rgb(0, 0, 0);">（强类型嵌套消息：如自定义</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">BoundingBoxList</font>`<font style="color:rgb(0, 0, 0);">）。</font>
  - `<font style="color:rgb(0, 0, 0);">text</font>`<font style="color:rgb(0, 0, 0);">：字符串。</font>
  - `<font style="color:rgb(0, 0, 0);">binary</font>`<font style="color:rgb(0, 0, 0);">：原始字节（例如 JPEG/PNG/PCM）。</font>
  - `<font style="color:rgb(0, 0, 0);">json</font>`<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">Struct</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">tags</font>`<font style="color:rgb(0, 0, 0);">：K/V（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">camera_id=cam01</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">role=system</font>`<font style="color:rgb(0, 0, 0);">）。  
  </font>**<font style="color:rgb(0, 0, 0);">建议</font>**<font style="color:rgb(0, 0, 0);">：</font>
+ <font style="color:rgb(0, 0, 0);">二进制图像优先</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">binary+content_type=image/*</font>`<font style="color:rgb(0, 0, 0);">；文本转写或提示则</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">text</font>`<font style="color:rgb(0, 0, 0);">；结构化参数走</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">json</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">message</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">kind</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">用于业务路由/统计（非安全字段，不应作为唯一鉴别依据）。</font>

### `<font style="color:rgb(0, 0, 0);">ResultEnvelope</font>`

+ `<font style="color:rgb(0, 0, 0);">kind</font>`<font style="color:rgb(0, 0, 0);">：结果类型（例</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"detections"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"overlay"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"tokens"</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"segments"</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ `<font style="color:rgb(0, 0, 0);">content_type</font>`<font style="color:rgb(0, 0, 0);">：MIME；若</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">result.message</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">承载 protobuf，建议</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"application/x-protobuf;type=\"<FQN>\""</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">result</font>`<font style="color:rgb(0, 0, 0);">（oneof）：与输入对称：</font>`<font style="color:rgb(0, 0, 0);">Any</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">text</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">binary</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">json</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">meta</font>`<font style="color:rgb(0, 0, 0);">：结构化元信息（例</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">{"runtime_ms": 12.7, "model_latency_ms": 10.3}</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ `<font style="color:rgb(0, 0, 0);">input_index</font>`<font style="color:rgb(0, 0, 0);">：当一个 Frame 含多个</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">inputs</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">时，用于指出该结果对应第几个输入（从 0 开始）。  
  </font>**<font style="color:rgb(0, 0, 0);">约定</font>**<font style="color:rgb(0, 0, 0);">：</font>
+ <font style="color:rgb(0, 0, 0);">一个</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">可以返回多种</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ResultEnvelope</font>`<font style="color:rgb(0, 0, 0);">，如同时返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"detections"</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">与</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">"overlay"</font>`<font style="color:rgb(0, 0, 0);">。</font>

## <font style="color:rgb(0, 0, 0);">2.5 客户端 → 服务端</font>

### `<font style="color:rgb(0, 0, 0);">StreamOpen</font>`

+ <font style="color:rgb(0, 0, 0);">首帧</font>**<font style="color:rgb(0, 0, 0);">必须</font>**<font style="color:rgb(0, 0, 0);">先发（除非双方另有约定）；用于声明本流的上下文与协商。</font>

### `<font style="color:rgb(0, 0, 0);">StreamFrame</font>`

+ `<font style="color:rgb(0, 0, 0);">inputs</font>`<font style="color:rgb(0, 0, 0);">：该帧所含的一个或多个输入（通常是一张图或一段音频分片）。</font>
+ `<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);">：</font>**<font style="color:rgb(0, 0, 0);">严格单调递增</font>**<font style="color:rgb(0, 0, 0);">（从 0 起）以对齐返回结果。</font>
+ `<font style="color:rgb(0, 0, 0);">ts</font>`<font style="color:rgb(0, 0, 0);">：可选，采集或发送时间（</font>`<font style="color:rgb(0, 0, 0);">google.protobuf.Timestamp</font>`<font style="color:rgb(0, 0, 0);">）。  
  </font>**<font style="color:rgb(0, 0, 0);">建议</font>**<font style="color:rgb(0, 0, 0);">：每个物理帧一条</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamFrame</font>`<font style="color:rgb(0, 0, 0);">，避免一次发送过大批量而导致延迟抖动。</font>

### `<font style="color:rgb(0, 0, 0);">StreamClose</font>`

+ `<font style="color:rgb(0, 0, 0);">meta</font>`<font style="color:rgb(0, 0, 0);">：客户端主动结束的原因/统计（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">{"reason":"user_stop"}</font>`<font style="color:rgb(0, 0, 0);">）。</font>

### `<font style="color:rgb(0, 0, 0);">StreamRequest</font>`

+ `<font style="color:rgb(0, 0, 0);">oneof event = { open | frame | close }</font>`<font style="color:rgb(0, 0, 0);">：三种事件任一。</font>

## <font style="color:rgb(0, 0, 0);">2.6 服务端 → 客户端</font>

### `<font style="color:rgb(0, 0, 0);">StreamAck</font>`

+ <font style="color:rgb(0, 0, 0);">对 open/close/心跳等的 ACK 或通用信号（含</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">CustomStatus</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ <font style="color:rgb(0, 0, 0);">典型：</font>
  - <font style="color:rgb(0, 0, 0);">open 后返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">status.code=0</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">表示就绪；</font>
  - <font style="color:rgb(0, 0, 0);">close 后返回统计/配额信息。</font>

### `<font style="color:rgb(0, 0, 0);">FrameResult</font>`

+ `<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);">：必须匹配某个已提交的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamFrame.frame_index</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ `<font style="color:rgb(0, 0, 0);">results</font>`<font style="color:rgb(0, 0, 0);">：一个帧可返回</font>**<font style="color:rgb(0, 0, 0);">一个或多个</font>**<font style="color:rgb(0, 0, 0);">结果信封。</font>
+ `<font style="color:rgb(0, 0, 0);">meta</font>`<font style="color:rgb(0, 0, 0);">：结构化元信息（运行时、缓存命中、batch 信息、设备 ID 等）。</font>

### `<font style="color:rgb(0, 0, 0);">StreamResponse</font>`

+ `<font style="color:rgb(0, 0, 0);">oneof event = { ack | frame }</font>`<font style="color:rgb(0, 0, 0);">。</font>

---

# <font style="color:rgb(0, 0, 0);">3) 时序、对齐与重传</font>

## <font style="color:rgb(0, 0, 0);">3.1 典型时序（就序返回）</font>

```plain
Client                         Server
------                         ------
open(header)       ─────────▶  ack(status=OK)
frame(idx=0)       ─────────▶
                                  └──▶ frame(idx=0, results=[...])
frame(idx=1)       ─────────▶
                                  └──▶ frame(idx=1, results=[...])
close(meta=...)    ─────────▶  ack(status=OK, details=summary)
```

## <font style="color:rgb(0, 0, 0);">3.2 乱序返回（允许）</font>

+ <font style="color:rgb(0, 0, 0);">服务端可并行处理，可能出现 idx=2 的结果先于 idx=1 返回。</font>
+ <font style="color:rgb(0, 0, 0);">客户端以</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult.frame_index</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">做对齐；若需要严格就序，请在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">options</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">accept</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">里宣布（由服务端实现决定）。</font>

## <font style="color:rgb(0, 0, 0);">3.3 幂等与重传</font>

+ <font style="color:rgb(0, 0, 0);">若网络中断/重试，客户端</font>**<font style="color:rgb(0, 0, 0);">不得</font>**<font style="color:rgb(0, 0, 0);">复用同一流；需新开流并</font>**<font style="color:rgb(0, 0, 0);">自管理</font>**<font style="color:rgb(0, 0, 0);">去重（可通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">TraceContext</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);">+ 自定义幂等键）。</font>
+ <font style="color:rgb(0, 0, 0);">服务端可选实现</font>**<font style="color:rgb(0, 0, 0);">去重</font>**<font style="color:rgb(0, 0, 0);">（见 6.5）。</font>

---

# <font style="color:rgb(0, 0, 0);">4) 错误、心跳与关闭</font>

## <font style="color:rgb(0, 0, 0);">4.1 gRPC 级 vs 业务级状态</font>

+ <font style="color:rgb(0, 0, 0);">gRPC status（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Status.UNAVAILABLE</font>`<font style="color:rgb(0, 0, 0);">）用于传输/系统级故障。</font>
+ `<font style="color:rgb(0, 0, 0);">CustomStatus.code/message/details</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">用于</font>**<font style="color:rgb(0, 0, 0);">业务态</font>**<font style="color:rgb(0, 0, 0);">（限流、配额、模型不可用、参数非法等）。</font>
+ <font style="color:rgb(0, 0, 0);">建议：</font>
  - **<font style="color:rgb(0, 0, 0);">Open 失败</font>**<font style="color:rgb(0, 0, 0);">：立刻</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamResponse.ack.status.code != 0</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">并可随即</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">close</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">流；</font>
  - **<font style="color:rgb(0, 0, 0);">帧级错误</font>**<font style="color:rgb(0, 0, 0);">：返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">meta</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">中标记错误，并可在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">results</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">放置错误描述（</font>`<font style="color:rgb(0, 0, 0);">kind="error"</font>`<font style="color:rgb(0, 0, 0);">）。</font>

## <font style="color:rgb(0, 0, 0);">4.2 心跳/超时</font>

+ <font style="color:rgb(0, 0, 0);">长时间无数据时，服务端可周期性发送</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">作为心跳（</font>`<font style="color:rgb(0, 0, 0);">status.code=0, message="heartbeat"</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ <font style="color:rgb(0, 0, 0);">客户端应设置 Deadline/KeepAlive 并在长时间无响应时重连。</font>

## <font style="color:rgb(0, 0, 0);">4.3 关闭顺序</font>

+ <font style="color:rgb(0, 0, 0);">客户端发送</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">close</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">→ 服务端回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">后主动</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">onCompleted()</font>`<font style="color:rgb(0, 0, 0);">；</font>
+ <font style="color:rgb(0, 0, 0);">或服务端在致命错误时</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">onError()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">并关闭连接。</font>

---

# <font style="color:rgb(0, 0, 0);">5) 示例载荷</font>

## <font style="color:rgb(0, 0, 0);">5.1 打开流（Open）</font>

**<font style="color:rgb(0, 0, 0);">客户端 → 服务端</font>**

```json
{
  "open": {
    "header": {
      "model": {"name": "yolo-v8", "version": "8.0.3", "tags": {"quant":"fp16"}},
      "trace": {"trace_id":"t-123","span_id":"s-1"},
      "tenant": {"tenant_id":"acme","user_id":"alice"},
      "options": {"score_threshold": 0.25, "nms": 0.6},
      "accept": ["application/json", "image/png"]
    }
  }
}
```

**<font style="color:rgb(0, 0, 0);">服务端 → 客户端（ACK）</font>**

```json
{
  "ack": {
    "status": {"code": 0, "message": "ready"}
  }
}
```

## <font style="color:rgb(0, 0, 0);">5.2 帧（Frame）与结果（FrameResult）</font>

**<font style="color:rgb(0, 0, 0);">客户端 → 服务端</font>**

```json
{
  "frame": {
    "inputs": [{
      "kind": "image",
      "content_type": "image/jpeg",
      "binary": "BASE64_OR_RAW_BYTES",   // gRPC 二进制字段，此处示意
      "tags": {"camera_id":"cam01"}
    }],
    "frame_index": 0,
    "ts": "2025-10-19T08:00:00Z"
  }
}
```

**<font style="color:rgb(0, 0, 0);">服务端 → 客户端</font>**

```json
{
  "frame": {
    "frame_index": 0,
    "results": [
      {
        "kind": "detections",
        "content_type": "application/json",
        "json": {
          "boxes": [
            {"x":10, "y":20, "w":100, "h":80, "score":0.91, "label":"person"}
          ]
        },
        "meta": {"runtime_ms": 12.7},
        "input_index": 0
      },
      {
        "kind": "overlay",
        "content_type": "image/png",
        "binary": "PNG_BYTES",
        "meta": {"alpha": 0.5},
        "input_index": 0
      }
    ],
    "meta": {"batch":1,"device":"gpu0"}
  }
}
```

## <font style="color:rgb(0, 0, 0);">5.3 关闭（Close）</font>

**<font style="color:rgb(0, 0, 0);">客户端 → 服务端</font>**

```json
{ "close": { "meta": { "reason":"user_stop" } } }
```

**<font style="color:rgb(0, 0, 0);">服务端 → 客户端（ACK）</font>**

```json
{ "ack": { "status": { "code": 0, "message": "closed", "details": [] } } }
```

---

# <font style="color:rgb(0, 0, 0);">6) 实现约定与最佳实践</font>

## <font style="color:rgb(0, 0, 0);">6.1 Header/Options 固化策略</font>

+ <font style="color:rgb(0, 0, 0);">服务端应在收到</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">open</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">后</font>**<font style="color:rgb(0, 0, 0);">冻结</font>**<font style="color:rgb(0, 0, 0);">该流的模型与关键推理选项，后续</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">内不再修改；若需动态修改，建议明确声明支持并在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult.meta</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">回显生效配置。</font>

## <font style="color:rgb(0, 0, 0);">6.2 内容协商（</font>`<font style="color:rgb(0, 0, 0);">accept</font>`<font style="color:rgb(0, 0, 0);">）</font>

+ <font style="color:rgb(0, 0, 0);">若客户端声明多个</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">accept</font>`<font style="color:rgb(0, 0, 0);">，服务端可按优先级选择一种或多种结果（如同帧返回 JSON + PNG overlay）。</font>
+ <font style="color:rgb(0, 0, 0);">不支持的类型：</font>
  - <font style="color:rgb(0, 0, 0);">可降级为默认类型并在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">meta</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">中标注，或</font>
  - <font style="color:rgb(0, 0, 0);">返回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack.status.code</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">非 0 并说明不支持的类型与建议。</font>

## <font style="color:rgb(0, 0, 0);">6.3 帧对齐与多输入</font>

+ <font style="color:rgb(0, 0, 0);">当</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamFrame.inputs</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">含多个输入时，</font>`<font style="color:rgb(0, 0, 0);">ResultEnvelope.input_index</font>`<font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">必须</font>**<font style="color:rgb(0, 0, 0);">指向其来源；否则视为整帧级结果（可约定</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">-1</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或省略）。</font>
+ <font style="color:rgb(0, 0, 0);">乱序返回时，客户端应基于</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">做聚合与重排（如需要就序显示）。</font>

## <font style="color:rgb(0, 0, 0);">6.4 大消息与分片</font>

+ <font style="color:rgb(0, 0, 0);">单条消息默认 4MB 限制（gRPC 默认值）。若图像/结构较大，请：</font>
  - <font style="color:rgb(0, 0, 0);">双方放宽限制（</font>`<font style="color:rgb(0, 0, 0);">maxInboundMessageSize</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">maxOutboundMessageSize</font>`<font style="color:rgb(0, 0, 0);">）；或</font>
  - <font style="color:rgb(0, 0, 0);">采用</font>**<font style="color:rgb(0, 0, 0);">对象存储引用</font>**<font style="color:rgb(0, 0, 0);">（在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InputEnvelope.json</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">填 URL+签名，服务端拉取）。</font>

## <font style="color:rgb(0, 0, 0);">6.5 幂等与去重</font>

+ <font style="color:rgb(0, 0, 0);">建议在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">options</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">或</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">tags</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">携带业务幂等键（如</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame_uid</font>`<font style="color:rgb(0, 0, 0);">）。</font>
+ <font style="color:rgb(0, 0, 0);">服务端可缓存近 N 秒的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame_uid → 结果</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">做去重。</font>

## <font style="color:rgb(0, 0, 0);">6.6 流控与背压</font>

+ <font style="color:rgb(0, 0, 0);">服务端可基于处理队列长度/显存占用，临时回</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack.status.code=429</font>`<font style="color:rgb(0, 0, 0);">（或自定义</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">code</font>`<font style="color:rgb(0, 0, 0);">）提示限流；</font>
+ <font style="color:rgb(0, 0, 0);">客户端应降低发送速率或丢帧（策略：取最近、均匀采样等）。</font>

## <font style="color:rgb(0, 0, 0);">6.7 安全与合规</font>

+ <font style="color:rgb(0, 0, 0);">传入二进制必须视为不可信：做类型/大小校验与沙箱解码。</font>
+ <font style="color:rgb(0, 0, 0);">结合 mTLS / JWT / IAM 做</font>**<font style="color:rgb(0, 0, 0);">租户与用户</font>**<font style="color:rgb(0, 0, 0);">鉴权，并在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">TenantContext</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">与证书/Token 间做一致性校验。</font>
+ <font style="color:rgb(0, 0, 0);">记录</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">TraceContext</font>`<font style="color:rgb(0, 0, 0);">，确保可审计。</font>

## <font style="color:rgb(0, 0, 0);">6.8 观测性</font>

+ <font style="color:rgb(0, 0, 0);">在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult.meta</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">回传</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">runtime_ms</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">queue_ms</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">pre/post_ms</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">gpu_id</font>`<font style="color:rgb(0, 0, 0);">,</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">batch_size</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">等；</font>
+ <font style="color:rgb(0, 0, 0);">在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack.details</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">返回余额、配额、采样率、降级原因等信息。</font>

---

# <font style="color:rgb(0, 0, 0);">7) 版本与兼容</font>

+ **<font style="color:rgb(0, 0, 0);">新增字段</font>**<font style="color:rgb(0, 0, 0);">：仅在消息尾部追加新字段编号，保持向后兼容；已发布字段</font>**<font style="color:rgb(0, 0, 0);">不得重用编号</font>**<font style="color:rgb(0, 0, 0);">。</font>
+ **<font style="color:rgb(0, 0, 0);">oneof 扩展</font>**<font style="color:rgb(0, 0, 0);">：可新增新的 case，不得改变旧 case 语义。</font>
+ **<font style="color:rgb(0, 0, 0);">内容类型</font>**<font style="color:rgb(0, 0, 0);">：新类型通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">content_type</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">扩展，保持旧客户端的 graceful degrade。</font>
+ **<font style="color:rgb(0, 0, 0);">模型切换</font>**<font style="color:rgb(0, 0, 0);">：强建议通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">version</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">与</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">tags</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">标注差异；必要时在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack.details</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">返回迁移提示。</font>

---

# <font style="color:rgb(0, 0, 0);">8) 参考客户端/服务端伪代码（Java）</font>

**<font style="color:rgb(0, 0, 0);">Server（核心骨架）</font>**

```java
public StreamObserver<StreamRequest> Stream(StreamObserver<StreamResponse> out) {
  final Session session = new Session();

  return new StreamObserver<>() {
    @Override public void onNext(StreamRequest req) {
      switch (req.getEventCase()) {
        case OPEN -> {
          session.init(req.getOpen().getHeader());
          out.onNext(StreamResponse.newBuilder()
              .setAck(StreamAck.newBuilder()
                  .setStatus(CustomStatus.newBuilder().setCode(0).setMessage("ready")))
              .build());
        }
        case FRAME -> {
          var f = req.getFrame();
          // … 推理 …
          out.onNext(StreamResponse.newBuilder()
              .setFrame(FrameResult.newBuilder()
                  .setFrameIndex(f.getFrameIndex())
                  .addResults(buildDetectionsResult(...))
                  .putMeta("runtime_ms", 12.7) // 伪代码
              ).build());
        }
        case CLOSE -> {
          out.onNext(StreamResponse.newBuilder()
              .setAck(StreamAck.newBuilder()
                  .setStatus(CustomStatus.newBuilder().setCode(0).setMessage("closed")))
              .build());
          out.onCompleted();
        }
        case EVENT_NOT_SET -> {}
      }
    }
    @Override public void onError(Throwable t) { /* log + cleanup */ }
    @Override public void onCompleted() { /* client half-close */ out.onCompleted(); }
  };
}
```

**<font style="color:rgb(0, 0, 0);">Client（核心骨架）</font>**

```java
StreamObserver<StreamResponse> resp = new StreamObserver<>() {
  @Override public void onNext(StreamResponse r) { /* dispatch ack / frame */ }
  @Override public void onError(Throwable t) { /* retry / report */ }
  @Override public void onCompleted() { /* done */ }
};

StreamObserver<StreamRequest> req = stub.stream(resp);

// open
req.onNext(StreamRequest.newBuilder()
    .setOpen(StreamOpen.newBuilder().setHeader(buildHeader())).build());

// frames
for (long i = 0; i < N; i++) {
  req.onNext(StreamRequest.newBuilder()
      .setFrame(StreamFrame.newBuilder()
          .setFrameIndex(i)
          .addInputs(buildImageInput(...)))
      .build());
}

// close
req.onNext(StreamRequest.newBuilder()
    .setClose(StreamClose.newBuilder().putMeta("reason","user_stop")).build());
req.onCompleted();
```

---

# <font style="color:rgb(0, 0, 0);">9) 与前端/网关的对接（可选）</font>

+ <font style="color:rgb(0, 0, 0);">若对外采用</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">SSE</font>**<font style="color:rgb(0, 0, 0);">：网关把内部 gRPC</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamResponse</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">转成</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">text/event-stream</font>`<font style="color:rgb(0, 0, 0);">，将</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">序列化为 JSON 逐条发送；</font>`<font style="color:rgb(0, 0, 0);">ack</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">事件映射为</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">event: ack</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ <font style="color:rgb(0, 0, 0);">若对外采用</font><font style="color:rgb(0, 0, 0);"> </font>**<font style="color:rgb(0, 0, 0);">HTTP chunked</font>**<font style="color:rgb(0, 0, 0);">：将</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">每条作为一段 JSON chunk 推送。</font>

---

## <font style="color:rgb(0, 0, 0);">快速联调清单（Checklist）</font>

+ <font style="color:rgb(0, 0, 0);"></font><font style="color:rgb(0, 0, 0);">首包发送</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">open</font>`<font style="color:rgb(0, 0, 0);">，收到</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ack.status.code==0</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">后再发</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame</font>`
+ <font style="color:rgb(0, 0, 0);"></font>`<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">从 0 递增；服务端结果中的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">frame_index</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">与之</font>**<font style="color:rgb(0, 0, 0);">一一对应</font>**
+ <font style="color:rgb(0, 0, 0);"></font><font style="color:rgb(0, 0, 0);">大消息超 4MB？→ 放宽 gRPC 限制或改为对象存储引用</font>
+ <font style="color:rgb(0, 0, 0);"></font><font style="color:rgb(0, 0, 0);">需要就序返回？→ 在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">options</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">声明 / 服务端保证</font>
+ <font style="color:rgb(0, 0, 0);"></font><font style="color:rgb(0, 0, 0);">需要多种结果（JSON+PNG）？→ 在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">accept</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">声明</font>
+ <font style="color:rgb(0, 0, 0);"></font><font style="color:rgb(0, 0, 0);">断线重连/重试 → 新开流，携带幂等键做去重</font>
+ <font style="color:rgb(0, 0, 0);"></font><font style="color:rgb(0, 0, 0);">记录</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">TraceContext</font>`<font style="color:rgb(0, 0, 0);">；在</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">FrameResult.meta</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">回传耗时</font>

---

<font style="color:rgb(0, 0, 0);">如果你愿意，我可以根据你的</font>**<font style="color:rgb(0, 0, 0);">具体模型/结果格式</font>**<font style="color:rgb(0, 0, 0);">（比如检测框、分割掩码、token 增量）给出对应的 </font>`<font style="color:rgb(0, 0, 0);">ResultEnvelope.content_type</font>`<font style="color:rgb(0, 0, 0);"> 与 </font>`<font style="color:rgb(0, 0, 0);">Any</font>`<font style="color:rgb(0, 0, 0);">/</font>`<font style="color:rgb(0, 0, 0);">Struct</font>`<font style="color:rgb(0, 0, 0);"> 的</font>**<font style="color:rgb(0, 0, 0);">规范字段约定</font>**<font style="color:rgb(0, 0, 0);">，并生成一套</font>**<font style="color:rgb(0, 0, 0);">客户端/服务端的最小可运行样例</font>**<font style="color:rgb(0, 0, 0);">。</font>

<font style="color:rgb(0, 0, 0);">下面把这份</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ai_pb2_grpc.py</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">生成代码“翻译成人话”，并给你一套</font>**<font style="color:rgb(0, 0, 0);">可直接运行</font>**<font style="color:rgb(0, 0, 0);">的最小用法（服务端 + 客户端，双向流）。</font>

---

# <font style="color:rgb(0, 0, 0);">Python端GRPC实现</font>

1. `**<font style="color:rgb(0, 0, 0);">InferenceStreamServiceStub</font>**`**<font style="color:rgb(0, 0, 0);">（客户端桩）</font>**
   - <font style="color:rgb(0, 0, 0);">给</font>**<font style="color:rgb(0, 0, 0);">客户端</font>**<font style="color:rgb(0, 0, 0);">用的。</font>
   - <font style="color:rgb(0, 0, 0);">通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">channel.stream_stream()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">暴露了一个</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Stream(...)</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">方法，对应你的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">.proto</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">里的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">rpc Stream(...) returns (...)</font>`<font style="color:rgb(0, 0, 0);">（双向流）。</font>
   - <font style="color:rgb(0, 0, 0);">你在客户端拿着这个 stub 调用：</font>`<font style="color:rgb(0, 0, 0);">responses = stub.Stream(request_iterator)</font>`<font style="color:rgb(0, 0, 0);">。</font>
2. `**<font style="color:rgb(0, 0, 0);">InferenceStreamServiceServicer</font>**`**<font style="color:rgb(0, 0, 0);">（服务端基类）</font>**
   - <font style="color:rgb(0, 0, 0);">给</font>**<font style="color:rgb(0, 0, 0);">服务端</font>**<font style="color:rgb(0, 0, 0);">用的“接口基类”。</font>
   - <font style="color:rgb(0, 0, 0);">里面的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Stream()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">只会设置</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">UNIMPLEMENTED</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">并抛错，是让你</font>**<font style="color:rgb(0, 0, 0);">继承并实现</font>**<font style="color:rgb(0, 0, 0);">的（就像“实现接口”）。</font>
   - <font style="color:rgb(0, 0, 0);">你需要写：</font>

```python
class InferenceServicer(ai_pb2_grpc.InferenceStreamServiceServicer):
    def Stream(self, request_iterator, context):
        ...
        yield response
```

3. `**<font style="color:rgb(0, 0, 0);">add_InferenceStreamServiceServicer_to_server(servicer, server)</font>**`
   - <font style="color:rgb(0, 0, 0);">把你实现好的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">servicer</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">注册进 gRPC 服务器（路由绑定、序列化/反序列化函数都在这里配置好了）。</font>
   - <font style="color:rgb(0, 0, 0);">你启动服务端时必须调用它。</font>
4. `**<font style="color:rgb(0, 0, 0);">InferenceStreamService</font>**`**<font style="color:rgb(0, 0, 0);">（EXPERIMENTAL）</font>**
   - <font style="color:rgb(0, 0, 0);">试验性静态包装，允许你不用先建</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Stub</font>`<font style="color:rgb(0, 0, 0);">，而是直接用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceStreamService.Stream(..., target="host:port", ...)</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">去发起调用。</font>
   - <font style="color:rgb(0, 0, 0);">一般场景直接用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Stub</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">就好。</font>
5. **<font style="color:rgb(0, 0, 0);">版本检查块</font>**
   - <font style="color:rgb(0, 0, 0);">确保运行时</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">grpcio</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">版本 ≥ 代码生成时写入的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">GRPC_GENERATED_VERSION</font>`<font style="color:rgb(0, 0, 0);">（这里是</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">1.74.0</font>`<font style="color:rgb(0, 0, 0);">）。</font>
   - <font style="color:rgb(0, 0, 0);">如果你看到那个</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">RuntimeError</font>`<font style="color:rgb(0, 0, 0);">，就升级：</font>

```bash
pip install -U grpcio==1.74.0 grpcio-tools==1.74.0
```

<font style="color:rgb(0, 0, 0);">或者用与你环境匹配的版本对</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">.proto</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">重新生成。</font>

---

# <font style="color:rgb(0, 0, 0);">如何使用：最小可运行示例</font>

<font style="color:rgb(0, 0, 0);">假设你的 proto 定义了：  
</font>`<font style="color:rgb(0, 0, 0);">service InferenceStreamService { rpc Stream (stream StreamRequest) returns (stream StreamResponse); }</font>`<font style="color:rgb(0, 0, 0);">  
</font><font style="color:rgb(0, 0, 0);">且</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ai_pb2.py</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">里有</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamRequest</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamResponse</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">两个消息。</font>

## <font style="color:rgb(0, 0, 0);">1) 服务端（同步，双向流）</font>

<font style="color:rgb(0, 0, 0);">保存为</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">server.py</font>`<font style="color:rgb(0, 0, 0);">：</font>

```python
import grpc
from concurrent import futures

import ai_pb2
import ai_pb2_grpc

# 你的业务实现：继承并实现接口
class InferenceServicer(ai_pb2_grpc.InferenceStreamServiceServicer):
    def Stream(self, request_iterator, context):
        """
        request_iterator: 迭代得到客户端不断发来的 StreamRequest
        你可以对每个请求做推理，然后 yield 出一个 StreamResponse（可多个）
        """
        for req in request_iterator:
            # 这里写你的业务逻辑，例如：把请求里的文本回声返回
            # 假设 proto 里有字段 req.text
            out_text = f"server got: {getattr(req, 'text', '')}"
            yield ai_pb2.StreamResponse(text=out_text)

def serve(host="0.0.0.0", port=50051, max_workers=10):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=max_workers))
    ai_pb2_grpc.add_InferenceStreamServiceServicer_to_server(InferenceServicer(), server)
    server.add_insecure_port(f"{host}:{port}")
    server.start()
    print(f"gRPC server listening on {host}:{port}")
    server.wait_for_termination()

if __name__ == "__main__":
    serve()
```

<font style="color:rgb(0, 0, 0);">要点：</font>

+ <font style="color:rgb(0, 0, 0);">你的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">Stream()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">是</font>**<font style="color:rgb(0, 0, 0);">生成器函数</font>**<font style="color:rgb(0, 0, 0);">，通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">yield</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">不断往回推送响应（双向流）。</font>
+ <font style="color:rgb(0, 0, 0);">如果你需要在异常时告诉客户端错误，可用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">context.abort(grpc.StatusCode.INVALID_ARGUMENT, "msg")</font>`<font style="color:rgb(0, 0, 0);">。</font>

## <font style="color:rgb(0, 0, 0);">2) 客户端（同步，双向流）</font>

<font style="color:rgb(0, 0, 0);">保存为</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">client.py</font>`<font style="color:rgb(0, 0, 0);">：</font>

```python
import grpc
import time

import ai_pb2
import ai_pb2_grpc

def make_requests():
    # 演示：向服务端推送 3 个请求，然后结束
    for i in range(3):
        yield ai_pb2.StreamRequest(text=f"hello {i}")
        time.sleep(0.2)  # 模拟间隔

def run(target="localhost:50051", timeout=10.0):
    with grpc.insecure_channel(target) as channel:
        stub = ai_pb2_grpc.InferenceStreamServiceStub(channel)
        # 双向流：传入一个“可迭代/生成器”；返回一个响应迭代器
        response_iterator = stub.Stream(make_requests(), timeout=timeout)

        # 逐个读取服务端流式返回
        for resp in response_iterator:
            print("client received:", resp.text)

if __name__ == "__main__":
    run()
```

<font style="color:rgb(0, 0, 0);">运行顺序：</font>

1. <font style="color:rgb(0, 0, 0);">先开服务端：</font>`<font style="color:rgb(0, 0, 0);">python server.py</font>`
2. <font style="color:rgb(0, 0, 0);">再跑客户端：</font>`<font style="color:rgb(0, 0, 0);">python client.py</font>`

<font style="color:rgb(0, 0, 0);">看到：</font>

```plain
gRPC server listening on 0.0.0.0:50051
```

<font style="color:rgb(0, 0, 0);">和</font>

```plain
client received: server got: hello 0
client received: server got: hello 1
client received: server got: hello 2
```

<font style="color:rgb(0, 0, 0);">就通了。</font>

---

# <font style="color:rgb(0, 0, 0);">常见需求与技巧</font>

+ **<font style="color:rgb(0, 0, 0);">改为异步（</font>**`**<font style="color:rgb(0, 0, 0);">grpc.aio</font>**`**<font style="color:rgb(0, 0, 0);">）</font>**<font style="color:rgb(0, 0, 0);">：如果你要高并发、协程化，把服务端换成</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">grpc.aio.server()</font>`<font style="color:rgb(0, 0, 0);">，方法签名改为</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">async def Stream(self, request_iterator, context): async for req in request_iterator: yield ...</font>`<font style="color:rgb(0, 0, 0);">；客户端用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">grpc.aio.insecure_channel</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">+</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">await stub.Stream(...)</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ **<font style="color:rgb(0, 0, 0);">元数据 / Deadline</font>**<font style="color:rgb(0, 0, 0);">：客户端调用时可以传</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">metadata=[('k','v')]</font>`<font style="color:rgb(0, 0, 0);">、</font>`<font style="color:rgb(0, 0, 0);">timeout=...</font>`<font style="color:rgb(0, 0, 0);">；服务端通过</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">context.invocation_metadata()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">读取，或在超时来临前</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">context.is_active()</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">判断。</font>
+ **<font style="color:rgb(0, 0, 0);">消息大小</font>**<font style="color:rgb(0, 0, 0);">：需要传大图或模型结果时，启动端/客户端 channel 都可设置</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">options=[('grpc.max_receive_message_length', 50*1024*1024), ('grpc.max_send_message_length', 50*1024*1024)]</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ **<font style="color:rgb(0, 0, 0);">状态码</font>**<font style="color:rgb(0, 0, 0);">：服务端在错误场景用</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">context.abort(StatusCode.X, "reason")</font>`<font style="color:rgb(0, 0, 0);">，客户端会收到</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">RpcError</font>`<font style="color:rgb(0, 0, 0);">。</font>
+ **<font style="color:rgb(0, 0, 0);">序列化类型</font>**<font style="color:rgb(0, 0, 0);">：</font>`<font style="color:rgb(0, 0, 0);">request_serializer/response_deserializer</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">已在生成文件里绑定到</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ai_pb2.StreamRequest.SerializeToString</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">/</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">StreamResponse.FromString</font>`<font style="color:rgb(0, 0, 0);">，你只要构造/读取</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">ai_pb2</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的消息对象即可。</font>

---

# <font style="color:rgb(0, 0, 0);">和你之前的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceServicer</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">结合</font>

<font style="color:rgb(0, 0, 0);">你之前写的：</font>

```python
class InferenceServicer(pb_grpc.InferenceStreamServiceServicer):
    def Stream(self, request_iterator, context):
        # 1) 从 request_iterator 里把流式请求解成 numpy 图像
        # 2) 调用 self.core = YoloCore(weights) 做推理
        # 3) 把结果封成 ResultEnvelope（对应 ai_pb2.StreamResponse 的字段）并 yield 出去
        ...
```

<font style="color:rgb(0, 0, 0);">把它替换到上面</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">server.py</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">InferenceServicer</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">里，然后把</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">yield ai_pb2.StreamResponse(...)</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">的字段填成你</font><font style="color:rgb(0, 0, 0);"> </font>`<font style="color:rgb(0, 0, 0);">.proto</font>`<font style="color:rgb(0, 0, 0);"> </font><font style="color:rgb(0, 0, 0);">里定义的结果字段即可。</font>

---

<font style="color:rgb(0, 0, 0);">如果你把 </font>`<font style="color:rgb(0, 0, 0);">.proto</font>`<font style="color:rgb(0, 0, 0);"> 的具体消息结构（</font>`<font style="color:rgb(0, 0, 0);">StreamRequest</font>`<font style="color:rgb(0, 0, 0);">/</font>`<font style="color:rgb(0, 0, 0);">StreamResponse</font>`<font style="color:rgb(0, 0, 0);"> 字段）贴出来，我可以把上面的最小例子直接改成你项目能用的“真实字段版本”，并给出一个简单的</font>**<font style="color:rgb(0, 0, 0);">图像 → YOLO → 结果流式返回</font>**<font style="color:rgb(0, 0, 0);">的骨架代码。</font>

