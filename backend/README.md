# 后端服务

Java 8、Spring Boot 2.7.18、MyBatis-Plus、MySQL 8.4。

```sh
mvn -DskipTests package
```

输出`target/seal-card-service.jar`。接口前缀`/api/v1`；Cookie会话认证，普通响应包含code、message、requestId、data，影像接口返回文件流。安装与环境变量见根目录README。
