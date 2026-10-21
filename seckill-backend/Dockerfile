# 后端镜像：多阶段构建，第一阶段用 Maven 打包，第二阶段只保留 JRE + jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区跟着容器走，日志时间才和服务器一致
ENV TZ=Asia/Shanghai

COPY --from=build /build/target/seckill-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
