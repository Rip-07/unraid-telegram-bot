FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B clean package

FROM eclipse-temurin:21-jre

RUN groupadd --system app && \
    useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build /workspace/target/unraid-telegram-bot-*.jar /app/app.jar

USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
