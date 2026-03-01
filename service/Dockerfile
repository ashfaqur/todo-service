FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre AS run
WORKDIR /app
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/todo-service.jar /app/todo-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/todo-service.jar"]
