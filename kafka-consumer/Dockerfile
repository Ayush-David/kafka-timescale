FROM openjdk:17-jre-slim
COPY target/KafkaToTimescaleDB-1.0-SNAPSHOT.jar /app/KafkaToTimescaleDB.jar
ENTRYPOINT ["java", "-jar", "/app/KafkaToTimescaleDB.jar"]