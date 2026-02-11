FROM eclipse-temurin:17-jdk-alpine AS builder
LABEL "language"="java"

WORKDIR /app
COPY . .
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q && \
    rm -rf ~/.m2/repository

FROM eclipse-temurin:17-jre-alpine
LABEL "language"="java"

WORKDIR /app
COPY --from=builder /app/target/record-java-1.0.0.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]