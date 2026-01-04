FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY target/cloud-storage-*.jar app.jar

RUN mkdir -p /app/storage/files /app/logs

RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}", "-jar", "app.jar"]