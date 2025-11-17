# ═══════════════════════════════════════════════════════════════════════════
# DOCKERFILE — Multi-stage build for Insurance Policy Management System
# ═══════════════════════════════════════════════════════════════════════════
#
# INTERVIEW TIP: Multi-stage Docker builds
# ──────────────────────────────────────────
# Why multi-stage?
# A Maven build requires: JDK (compiler), Maven, source code (~500MB+ image)
# The final runtime only needs: JRE, the compiled JAR (~200MB image)
#
# Multi-stage separates BUILD from RUNTIME:
# Stage 1 (build): JDK + Maven → compile source → produces JAR file
# Stage 2 (runtime): only JRE + JAR → no build tools needed → smaller image
#
# Benefits:
# 1. Smaller image: build tools not included in final image
# 2. Security: source code not in final image (only bytecode)
# 3. Faster deploys: smaller image = faster pull from registry
# 4. Layered JARs: Spring Boot supports layered JARs for even better caching
# ═══════════════════════════════════════════════════════════════════════════

# ─── STAGE 1: BUILD ──────────────────────────────────────────────────────────
# Use official Maven image with JDK 21 (Eclipse Temurin is the OpenJDK distribution)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set working directory inside the container
WORKDIR /app

# ── Layer optimization for Docker caching ──
# Copy pom.xml FIRST and download dependencies.
# Docker caches layers. If pom.xml doesn't change, this layer is cached.
# Only copy source code after dependencies are resolved.
# This means re-builds only re-download dependencies when pom.xml changes.
COPY pom.xml .

# Download all Maven dependencies (offline later for faster builds)
# -B flag: batch mode (no interactive prompts, better for CI/CD)
RUN mvn dependency:go-offline -B

# Copy the actual source code (changes more frequently than pom.xml)
COPY src ./src

# Build the application, skip tests (tests run in CI/CD pipeline separately)
# -DskipTests: don't run JUnit tests during Docker build
# package: compile + test-compile + package into JAR
RUN mvn clean package -DskipTests -B

# ─── STAGE 2: RUNTIME ────────────────────────────────────────────────────────
# Use slim JRE image (no JDK compiler tools needed at runtime)
# Eclipse Temurin is the community distribution of OpenJDK (replaces AdoptOpenJDK)
FROM eclipse-temurin:21-jre-alpine

# METADATA: OCI image labels (industry standard for Docker image documentation)
LABEL maintainer="Insurance System Team <support@insurance.com>"
LABEL version="1.0.0"
LABEL description="Insurance Policy Management System — Spring Boot 3.x, Java 21"

# Create a non-root user for security
# INTERVIEW TIP: Never run production containers as root.
# A compromised container running as root could escape and access the host.
# Running as a dedicated user limits the blast radius of a security breach.
RUN addgroup -S insurance && adduser -S insurance -G insurance

# Set working directory
WORKDIR /app

# Copy the compiled JAR from the build stage
# The wildcard *.jar avoids hardcoding the exact version in the filename
COPY --from=build /app/target/*.jar app.jar

# Create logs directory and set ownership
RUN mkdir -p logs && chown -R insurance:insurance /app

# Switch to non-root user
USER insurance

# EXPOSE: Documents which port the container listens on
# NOTE: EXPOSE is documentation only — doesn't actually publish the port.
# Ports are published with `docker run -p 8080:8080` or in docker-compose.
EXPOSE 8080

# Health check: Docker tests this to know if the container is healthy
# INTERVIEW TIP: Kubernetes uses liveness and readiness probes (similar concept).
# This uses Spring Boot Actuator's health endpoint.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# JVM tuning for containerized environments
# INTERVIEW TIP: JVM defaults to using 25% of total system RAM.
# In a container with limits, this can cause OOM (Out of Memory) kills.
# -XX:+UseContainerSupport: makes JVM respect container memory limits (Java 11+)
# -XX:MaxRAMPercentage=75.0: use up to 75% of container's allocated RAM
# -XX:+UseG1GC: G1 garbage collector (good for low-pause, general use)
# -Djava.security.egd=...: faster startup (avoids blocking on random number generation)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
