# Stage 1: Build application using Maven and JDK 17
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./

# Ensure mvnw has Unix LF endings and executable permissions
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Copy source code and build production package
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Production runtime with slim JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-privileged user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy jar from builder stage
COPY --from=builder /app/target/finance-manager-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
