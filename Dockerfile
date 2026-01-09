# ============================================
# Dockerfile para Impostor Backend
# Spring Boot 3.4 + Java 21 + MySQL
# Multi-stage build optimizado con layers
# ============================================

# ========== STAGE 1: Build ==========
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Gradle primero (para cachear dependencias)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Dar permisos de ejecución al gradlew
RUN chmod +x ./gradlew

# Descargar dependencias (capa cacheada si no cambian)
RUN ./gradlew dependencies --no-daemon || true

# Copiar código fuente
COPY src src

# Compilar la aplicación (sin tests para acelerar build)
RUN ./gradlew bootJar --no-daemon -x test

# ========== STAGE 2: Runtime ==========
FROM eclipse-temurin:21-jre-alpine AS runtime

# Metadatos de la imagen
LABEL maintainer="swemmanuelgz"
LABEL description="Impostor Game Backend - Spring Boot 3.4"
LABEL version="0.0.1"

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /application

# Copiar el JAR compilado
COPY --from=builder /app/build/libs/*.jar application.jar

# Cambiar ownership al usuario no-root
RUN chown appuser:appgroup application.jar

# Usar usuario no-root
USER appuser

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Variables de entorno por defecto (sobrescribir en docker-compose o runtime)
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE="prod" \
    TZ="Europe/Madrid"

# Comando de inicio - usar el launcher de Spring Boot
ENTRYPOINT exec java $JAVA_OPTS -jar application.jar
