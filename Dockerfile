# ------------------------------------------------------------------
# STAGE 1: Build the application
# ------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 1. Copy Maven Wrapper and POM
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# 2. Download Dependencies
RUN ./mvnw dependency:go-offline

# 3. Copy Source and Build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------
# STAGE 2: Run the application (Using Standard Linux, NOT Alpine)
# ------------------------------------------------------------------
# CRITICAL CHANGE: Switched from 'alpine' to standard 'jdk' to fix DNS issues
FROM eclipse-temurin:21-jdk
WORKDIR /app

# 4. Copy the JAR
COPY --from=builder /app/target/*.jar app.jar

# 5. Expose Port
EXPOSE 8080

# 6. Run with IPv4 forced (Fixes the original timeout issue)
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]