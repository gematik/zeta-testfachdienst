FROM gcr.io/distroless/java21-debian12

WORKDIR /app

COPY build/libs/app.jar /app/app.jar

EXPOSE 8080 8081

USER 65532:65532

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
