# --- Stage 1: Build ---
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
# Only copy the jar from the build stage
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]