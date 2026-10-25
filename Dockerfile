FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

COPY src src
RUN ./mvnw -B -DskipTests package \
    && cp target/*-0.0.1-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build --chown=10001:10001 /app.jar app.jar
USER 10001:10001

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
