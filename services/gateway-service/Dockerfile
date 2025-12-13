FROM m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/nsq-course-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# 暴露端口
EXPOSE 8090

# 设置默认的环境变量
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]