FROM openjdk:11-jre-slim

WORKDIR /app

COPY build/libs/payment-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]