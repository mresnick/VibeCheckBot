# Use Gradle to build the application
FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Create the runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar file
COPY --from=build /app/build/libs/vibecheckbot.jar /app/vibecheckbot.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app/vibecheckbot.jar"] 