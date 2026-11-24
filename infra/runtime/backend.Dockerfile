FROM eclipse-temurin:8-jre-jammy@sha256:06b3cb77202baa40a5765d5459ef585f77031b3434ecf454d99f6c5790b35275
WORKDIR /app
COPY backend/target/seal-card-service.jar /app/service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/service.jar"]
