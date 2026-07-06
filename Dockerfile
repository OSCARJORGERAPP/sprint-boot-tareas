# Stage 1: compila y EJECUTA LOS TESTS DE INTEGRACIÓN — si un test falla,
# el build de la imagen falla y el pipeline de CI queda en rojo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B --no-transfer-progress dependency:go-offline
COPY src ./src
RUN mvn -B --no-transfer-progress test package

# Stage 2: imagen de ejecución mínima (solo JRE + jar).
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# La plataforma de deploy de la academia espera el puerto 3000.
ENV SERVER_PORT=3000
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
