# Built by the root docker-compose.yml (`docker compose up --build`), or alone:
#   docker build -t baddee-updater updaterComponent

# --- build: compile with the Maven wrapper, then split the jar into layers ---
FROM eclipse-temurin:26-jdk AS build
WORKDIR /src

# Dependencies first, so a source-only change reuses this layer.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run on the host (`./mvnw verify` needs Docker for Testcontainers).
RUN ./mvnw -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/updaterComponent-*.jar extract --layers --destination /extracted \
 && mv /extracted/application/*.jar /extracted/application/app.jar

# --- runtime: JRE only, non-root ---
FROM eclipse-temurin:26-jre
# curl for the compose healthcheck.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 1001 updater
WORKDIR /app

# Least- to most-often changing, so rebuilds reuse the dependency layers.
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./

USER updater
EXPOSE 8080
# No default profile: compose sets SPRING_PROFILES_ACTIVE=docker, AWS sets prod.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
