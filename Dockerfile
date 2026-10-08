FROM gcr.io/distroless/java21-debian12@sha256:f34fd3e4e2d7a246d764d0614f5e6ffb3a735930723fac4cfc25a72798950262

WORKDIR /app

COPY build/libs/app.jar /app/app.jar

ENV OTEL_SDK_DISABLED=true

EXPOSE 8080 8081

USER 65532:65532

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
