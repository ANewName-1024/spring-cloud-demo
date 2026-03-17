FROM openjdk:21-jdk-slim

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 工作目录
WORKDIR /app

# 复制 JAR 文件
COPY target/ops-service-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8090

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
