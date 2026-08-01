# ==========================================
# Stage 1: Build Stage (Eclipse Temurin JDK 21 + Maven)
# ==========================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy Maven pom.xml and source code
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Production Runtime Stage (Ubuntu Jammy JRE with full glibc for ONNX)
# ==========================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Ensure /tmp directory is writable for ONNX native library extraction
RUN chmod 1777 /tmp

# Copy compiled jar artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Default PORT environment variable (Render sets this dynamically at runtime)
ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseSerialGC"

# Expose HTTP port
EXPOSE 8080

# Execute Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
