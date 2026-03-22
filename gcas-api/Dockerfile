# Build stage
# Use the full JDK image so we can compile and package with Maven
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom first, this lets Docker cache the dependency
# download layer separately from our source code. If only src/ changes,
# the `mvn dependency:go-offline` layer is reused (faster rebuilds).
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -q

# Now copy source and build the fat JAR, skipping tests (tests run in CI)
COPY src src
RUN ./mvnw clean package -DskipTests


# Run stage
# Use the smaller JRE-only image, we don't need the compiler at runtime
FROM eclipse-temurin:21-jre

ARG ENGINE_VERSION=v1.0.0

WORKDIR /app

# Download the engine binary as root, then lock down permissions
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    ARCH=$(arch | sed s/aarch64/arm64/ | sed s/x86_64/amd64/) && \
    curl -o /app/engine -L https://github.com/CSSE6400/ageoverflow-engine/releases/download/${ENGINE_VERSION}/engine-${ENGINE_VERSION}-linux-${ARCH} && \
    chmod +x /app/engine

# Run as a non-root user to reduce security risk
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Pull only the built JAR from the builder stage, nothing else carries over
COPY --from=builder /app/target/*.jar app.jar

# Sleep gives the Postgres container time to finish starting before we connect.
# In production you'd use a proper retry/health-check instead.
CMD ["bash", "-c", "sleep 10 && java -jar app.jar"]
