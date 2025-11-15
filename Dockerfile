# Java 21 JRE
FROM eclipse-temurin:21-jre

ARG JAR_FILE=build/libs/SanSam-0.0.1-SNAPSHOT.jar
ARG OTEL_JAVA_AGENT_VERSION=2.7.0

WORKDIR /app

# 앱 JAR
COPY ${JAR_FILE} /app/app.jar

# OpenTelemetry Java agent 포함
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

# 기본값(필요시 docker-compose에서 override 가능)
ENV OTEL_SERVICE_NAME=SanSam
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_TRACES_SAMPLER=always_on
ENV OTEL_RESOURCE_ATTRIBUTES=deployment.environment=local

# 자바 에이전트 주입 (무코드 계측)
ENV JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar"

# 스프링 프로파일
ENV SPRING_PROFILES_ACTIVE=local,perf

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
