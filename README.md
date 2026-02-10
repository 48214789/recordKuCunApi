## 简介

这是一个使用 Spring Boot 开发的简单图片上传接口服务，支持：

- **POST `/api/upload`**：上传图片（Multipart）并携带两个参数（`userId`、`description`），服务端保存图片到本地 `uploads/` 目录，并在内存中记录一条数据。
- **GET `/api/records`**：获取所有上传记录。
- **GET `/api/records/{id}`**：根据 ID 获取单条记录。

你可以在安卓端通过 Retrofit 或其他 HTTP 客户端调用这些接口。

## 本地运行

```bash
mvn clean package
mvn spring-boot:run
```

应用启动后，默认端口为 `8080`，你可以通过以下方式简单测试：

- 上传接口：`POST http://localhost:8080/api/upload`
  - 表单字段：
    - `image`：文件（必填）
    - `userId`：字符串（示例参数1，可自行修改含义）
    - `description`：字符串（示例参数2，可自行修改含义）
- 查询全部：`GET http://localhost:8080/api/records`
- 按 ID 查询：`GET http://localhost:8080/api/records/{id}`

上传后的图片会保存在项目根目录下的 `uploads/` 文件夹，并通过：

- `http://localhost:8080/uploads/文件名`

对外访问（静态资源映射已在 `application.properties` 中配置）。

## 部署到 Zeabur（示例流程）

1. **本地打包**

```bash
mvn clean package -DskipTests
```

生成的 JAR 文件在：`target/record-java-1.0.0.jar`

2. **构建 Docker 镜像（如在本地或 CI 中构建）**

项目已经提供了简单的 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/record-java-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

你可以：

- 在本地或 CI 构建并推送镜像到镜像仓库；
- 在 Zeabur 选择 Docker 部署，并指向该镜像。

3. **Zeabur 上的基本配置**

- 端口：`8080`
- 启动命令（如果使用源码构建，而非 Docker 镜像）：
  - 构建命令：`mvn clean package -DskipTests`
  - 启动命令：`java -jar target/record-java-1.0.0.jar`

## 安卓端调用字段说明

- 图片字段名：`image`
- 参数1：`userId`（例如：用户 ID，可随意修改成你想要的业务含义和名字）
- 参数2：`description`（例如：图片说明，也可随意修改）

你可以在 Java 代码中直接把 `userId` / `description` 替换成自己想用的字段名，然后在安卓端保持一致即可。

