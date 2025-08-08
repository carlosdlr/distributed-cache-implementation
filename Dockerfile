FROM openjdk:21-slim
WORKDIR /app
COPY target/distributed-cache-impl-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]