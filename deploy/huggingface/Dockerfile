# Single-container build of the whole stack for free hosting on Hugging Face Spaces
# (Docker SDK, 16 GB RAM CPU tier): PostgreSQL + Ollama (local LLM) + Spring Boot serving the
# Angular UI, all on port 7860. Built from the repository root by deploy/huggingface/push_space.py.

# ---- Angular UI ----
FROM node:22-alpine AS frontend
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build --configuration production

# ---- Spring Boot API (with the UI packaged as static resources) ----
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /src
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /src/dist/frontend/browser ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# ---- Runtime ----
FROM ubuntu:24.04
ARG OLLAMA_MODEL=qwen2.5:3b
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
 && apt-get install -y --no-install-recommends openjdk-21-jre-headless postgresql curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Ollama runtime (CPU) copied from the official image.
COPY --from=ollama/ollama:latest /usr/bin/ollama /usr/bin/ollama
COPY --from=ollama/ollama:latest /usr/lib/ollama /usr/lib/ollama

ENV OLLAMA_MODELS=/opt/ollama/models \
    OLLAMA_HOST=127.0.0.1:11434 \
    OLLAMA_KEEP_ALIVE=24h
# Bake the model into the image so the Space starts without a multi-GB download.
RUN mkdir -p $OLLAMA_MODELS \
 && (ollama serve > /tmp/ollama-build.log 2>&1 &) \
 && for i in $(seq 30); do curl -sf http://127.0.0.1:11434/api/tags > /dev/null && break; sleep 1; done \
 && ollama pull ${OLLAMA_MODEL} \
 && chmod -R a+rX /opt/ollama

COPY --from=backend /src/target/vehicle-driver-mapping-*.jar /app/app.jar
COPY deploy/huggingface/start.sh /app/start.sh
RUN chmod +x /app/start.sh && mkdir -p /home/user && chown -R 1000:1000 /home/user

# Spaces run the container as uid 1000.
USER 1000
ENV HOME=/home/user \
    SERVER_PORT=7860 \
    DB_URL=jdbc:postgresql://localhost:5432/vehicle_mapping \
    DB_USERNAME=postgres \
    OLLAMA_BASE_URL=http://127.0.0.1:11434 \
    OLLAMA_MODEL=${OLLAMA_MODEL} \
    AI_TIMEOUT=240s \
    SEED_DEMO_DATA=true \
    JAVA_OPTS="-Xmx768m"
EXPOSE 7860
CMD ["/app/start.sh"]
