# --- Build stage: compile the jar ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only what dependency resolution needs first, so this layer caches
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Now copy the source and build the jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# --- Runtime stage: run the jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]