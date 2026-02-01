# ------------------------------------------------------------------
# STAGE 1: Build
# ------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------
# STAGE 2: Run (Standard Linux, IPv6 Enabled)
# ------------------------------------------------------------------
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080

# CRITICAL FIX: Removed "-Djava.net.preferIPv4Stack=true"
# This allows the app to use Render's native IPv6 routing.
ENTRYPOINT ["java", "-jar", "app.jar"]