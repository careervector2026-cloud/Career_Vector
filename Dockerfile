# ------------------------------------------------------------------
# STAGE 1: Build the application
# ------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 1. Copy Maven Wrapper and Project Object Model (POM)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Grant execution permissions to the wrapper
RUN chmod +x mvnw

# 3. Download Dependencies (Cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline

# 4. Copy the actual source code
COPY src ./src

# 5. Build the JAR file (Skip tests to speed up deployment)
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------
# STAGE 2: Run the application
# ------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 6. Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# 7. Expose the port
EXPOSE 8080

# 8. Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
