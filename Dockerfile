FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/oil-orders-1.0.0.jar app.jar
ENTRYPOINT ["java", "-Duser.timezone=Africa/Dar_es_Salaam", "-Xmx300m", "-jar", "app.jar"]
