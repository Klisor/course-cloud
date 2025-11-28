FROM m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
# 关键修改：jar 包名改为实际生成的 nsq-course-0.0.1-SNAPSHOT.jar
COPY target/nsq-course-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]