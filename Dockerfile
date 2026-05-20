# 1. Use an official lightweight Java OpenJDK image
FROM eclipse-temurin:17-jdk-alpine

# 2. Set the working directory inside the cloud container
WORKDIR /app

# 3. Copy your project files and compile the .jar file
COPY . .
RUN ./mvnw clean package -DskipTests

# 4. Expose the port Spring Boot will run on
EXPOSE 8080

# 5. Tell the container to execute the compiled Java application
CMD ["java", "-jar", "target/PlacesBeen-0.0.1-SNAPSHOT.jar"]p", "-b"]