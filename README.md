<img align="right" width="250" height="47" src="docs/img/Gematik_Logo_Flag.png"/> <br/>

# Testfachdienst ZETA - Spring Boot

## Overview

- Lightweight test service used in ZETA/PEP integration scenarios.
- Provides a public hello endpoint and a fully CRUD-capable E-Rezept API backed by H2.
- Built with Spring Boot 3.3, Java 21, Spring Data JPA, Spring Security (permissive configuration),
  Lombok, SpringDoc OpenAPI, and
  Spring Boot Actuator.
- Ships with opinionated logging (console + rolling file appender) and an unauthenticated API
  surface that can be
  hardened later if required.
- Exposes context-aware health and info endpoints for deployment checks via Spring Boot Actuator.
- Ships with a container image build (Jib) and health probes that make it ready for Kubernetes;
  manifests live under
  [`k8s/`](k8s).

## Project Structure

- [TestfachdienstApplication.java](src/main/java/de/gematik/zeta/testfachdienst/TestfachdienstApplication.java)
  boots the
  Spring context.
- [HalloZetaController.java](src/main/java/de/gematik/zeta/testfachdienst/controller/HelloZetaController.java)
  exposes
  `GET /hellozeta` via `HelloZetaService`.
- [ERezeptController.java](src/main/java/de/gematik/zeta/testfachdienst/controller/ErezeptController.java)
  offers CRUD
  endpoints under `/api/erezept`.
- [ERezept.java](src/main/java/de/gematik/zeta/testfachdienst/model/Erezept.java), [ERezeptStatus.java](src/main/java/de/gematik/zeta/testfachdienst/model/ErezeptStatus.java),
  and [HelloZetaResource.java](src/main/java/de/gematik/zeta/testfachdienst/model/HelloZetaResource.java)
  define the domain
  types.
- [ErezeptRepository.java](src/main/java/de/gematik/zeta/testfachdienst/repository/ErezeptRepository.java)
  provides Spring
  Data accessors for prescriptions.
- [HelloZetaService.java](src/main/java/de/gematik/zeta/testfachdienst/service/HelloZetaService.java)
  builds the hello
  response payload.
- [SecurityConfig.java](src/main/java/de/gematik/zeta/testfachdienst/config/SecurityConfig.java)
  configures an
  unauthenticated security filter chain suitable for infrastructure probes.
- [application.yml](src/main/resources/application.yml) centralizes H2, SSL, actuator, and logging
  settings.
- [logback-spring.xml](src/main/resources/logback-spring.xml) writes structured logs to console and
  `./logs` with
  rotation.
- [libs.versions.toml](gradle/libs.versions.toml) defines dependency and plugin versions

## Configuration Highlights

- In-memory H2 database at `jdbc:h2:mem:erezeptdb`; schema auto-updates via Hibernate (
  `ddl-auto: update`).
- H2 console reachable at `/h2-console` when `H2_CONSOLE_ENABLED=true`.
- Server listens on port `8080` with the context path `/achelos_testfachdienst` by default; override
  via
  `SERVER_PORT` and `SERVER_CONTEXT_PATH`.
- TLS toggled by `SERVER_SSL_ENABLED` (Gradle `bootRun` enables HTTPS using
  `src/main/resources/tls/keystore.p12`).
- Authentication is disabled by default; all HTTP and actuator endpoints are publicly reachable so
  that infrastructure
  probes can operate without additional credentials.
- Prometheus metrics export is enabled via Micrometer; `/actuator/prometheus` is available for
  scraping.
- Package-level logging set to `DEBUG` for `de.gematik`; all other loggers default to `INFO`.
- Actuator exposes health and info endpoints; adjust `management.endpoints.web.exposure.include` in
  `application.yml` to
  publish more.

## Prerequisites

- JDK 21
- Docker / Docker Compose (optional)
- No local Gradle install required; use the wrapper (`./gradlew` or `gradlew.bat`).

## Build and Test

```bash
./gradlew clean check        # format & static analysis (Checkstyle)
./gradlew test               # run unit, repository, and controller tests
./gradlew jacocoTestReport   # create coverage reports in build/reports/jacoco/test
./gradlew bootJar            # build executable JAR in build/libs/
```

## Run Locally

### Using the Gradle wrapper (HTTPS)

```bash
./gradlew bootRun
```

Boot Run starts the app with SSL enabled. Use `https://localhost:8080/achelos_testfachdienst/...`
and add `-k`/
`--insecure` to curl while using the bundled self-signed certificate.

### Using the executable JAR (HTTP)

```bash
./gradlew bootJar
java -jar build/libs/testfachdienst-0.0.1.jar
```

The JAR defaults to plain HTTP: `http://localhost:8080/achelos_testfachdienst/...`.

### Docker Compose

```bash
docker compose up --build
```

Stop with `docker compose down`. The service is exposed on host port 8080.

### Docker CLI

```bash
docker build -t testfachdienst:local .
docker run --rm -p 8080:8080 --name testfachdienst testfachdienst:local
```

## Kubernetes

The project now ships with a dedicated Kubernetes profile and sample manifests under [`k8s/`](k8s).

```bash
# Build and push the container image (adjust registry credentials as needed)
./gradlew jib

# Deploy the app and service into your cluster
kubectl apply -f k8s/

# Validate probes (management port defaults to 8081 inside the pod)
kubectl port-forward deploy/testfachdienst 8080:8080 8081:8081
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness
```

Key environment variables exposed in the manifests allow you to override ports, context path, and
SSL behaviour when
needed. The security configuration currently permits unauthenticated access to all endpoints,
enabling Kubernetes
liveness/readiness probes and Prometheus scrapes without extra wiring.

## API Reference

Unless noted otherwise, the paths below include the default context prefix
`/achelos_testfachdienst`. Override the
prefix via `SERVER_CONTEXT_PATH`. Actuator endpoints are exposed on the management server (same port
as the app by
default, `8081` when the Kubernetes profile is active).

| Method | Path (default context)                      | Description                                                  |
|--------|---------------------------------------------|--------------------------------------------------------------|
| GET    | `/hellozeta`                                | Returns the static hello payload.                            |
| GET    | `/api/erezept`                              | Lists all stored prescriptions.                              |
| POST   | `/api/erezept`                              | Creates a prescription (rejects duplicate `prescriptionId`). |
| GET    | `/api/erezept/{id}`                         | Fetches a prescription by database id.                       |
| PUT    | `/api/erezept/{id}`                         | Updates core fields on an existing prescription.             |
| DELETE | `/api/erezept/{id}`                         | Removes a prescription if it exists.                         |
| GET    | `/api/erezept/by-prescription/{businessId}` | Looks up a prescription by its domain id.                    |
| GET    | `/actuator/health`                          | Composite health indicator (includes readiness + liveness).  |
| GET    | `/actuator/health/liveness`                 | Liveness probe exposed via Spring Boot Actuator.             |
| GET    | `/actuator/health/readiness`                | Readiness probe exposed via Spring Boot Actuator.            |
| GET    | `/actuator/info`                            | Info endpoint populated via `application.yml`.               |
| GET    | `/actuator/metrics`                         | Lists available metric names.                                |
| GET    | `/actuator/metrics/{metricName}`            | Returns details and samples for a specific metric.           |
| GET    | `/actuator/prometheus`                      | Prometheus metrics scrape endpoint (enabled in k8s profile). |
| GET    | `/v3/api-docs`                              | Serves the OpenAPI document.                                 |
| GET    | `/swagger-ui/index.html`                    | Interactive Swagger UI powered by SpringDoc.                 |

Example curl against the hello endpoint (HTTP mode):

```bash
curl https://localhost:8080/achelos_testfachdienst/hellozeta
```

Example curl against the health endpoint (HTTP mode):

```bash
curl https://localhost:8080/achelos_testfachdienst/actuator/health
```

## WebSocket (STOMP) + AsyncAPI

This service also exposes a STOMP over WebSocket interface for real-time interactions with the
ERezept domain.
It reuses the same repository/service logic as the REST API
and documents its message contract via a generated AsyncAPI spec (Swagger-style),
viewable in a small bundled UI.

### Endpoints & destinations

- Handshake (WebSocket endpoint):
    - ``wss://<host>:<port>/achelos_testfachdienst/ws`` (HTTPS mode)
    - ``ws://<host>:<port>/achelos_testfachdienst/ws`` (HTTP mode)

- Application prefix (SEND here): ``/<context-path>/app`` (default context path:
  ``/achelos_testfachdienst``). If no servlet context path is configured, use ``/app``.
    - Example client destinations:
        - ``/achelos_testfachdienst/app/erezept.create`` - create and broadcast
        - ``/achelos_testfachdienst/app/erezept.read.{id}`` - fetch one and reply to caller
        - ``/achelos_testfachdienst/app/erezept.update.{id}`` - update and broadcast
        - ``/achelos_testfachdienst/app/erezept.delete.{id}`` - delete and ack to caller
        - ``/achelos_testfachdienst/app/erezept.list`` - list and reply to caller

- Broker prefixes (SUBSCRIBE here):
    - Broadcasts: ``/achelos_testfachdienst/topic/erezept`` (or ``/topic/erezept`` when no context path
      is set)
    - Per-user replies: ``/achelos_testfachdienst/user/queue/erezept`` (or ``/user/queue/erezept`` without
      a context path)

### AsyncAPI (code-first) docs

- JSON spec: ``https://localhost:8080/achelos_testfachdienst/springwolf/docs``
- YAML spec: ``https://localhost:8080/achelos_testfachdienst/springwolf/docs.yaml``
- UI: ``https://localhost:8080/achelos_testfachdienst/springwolf/asyncapi-ui.html``

The AsyncAPI is generated by Springwolf scanning ``@MessageMapping`` methods and
``@AsyncPublisher(operation=@AsyncOperation(...))``.
For unambiguous schemas,
we pin payload types with ``@AsyncMessage(payload = ERezept.class)`` or ``ERezept[].class`` for list
responses.

### cURL quick checks (spec & UI)

```bash
# JSON spec (should be HTTP 200 with JSON)
curl -k https://localhost:8080/achelos_testfachdienst/springwolf/docs

# UI (uses the JSON spec above)
curl -k https://localhost:8080/achelos_testfachdienst/springwolf/asyncapi-ui.html
```

> When running via ``./gradlew bootRun``, HTTPS is enabled by default in this project;
> use ``-k/--insecure`` for local self-signed certs.

## Exemplary self disclosure export 
This service allows to configure an OTLP based export of a self disclosure log record (see A_27494-01, gemSpec_ZETA)
using the official [OpenTelemetry SDK](https://opentelemetry.io/docs/languages/java/intro/) to generate and export
OTLP and gemSpec_ZETA conformant log records and [Jobrunr](https://www.jobrunr.io/) to manage the recurring background 
task.

The configuration parameters for the OTLP exporter are located at the `otlp` key in the 
[application.yaml](./src/main/resources/application.yml).
For convenience all of those options can also be configured through their respective environment variables, please 
consult the [application.yaml](./src/main/resources/application.yml) for details.


The configuration parameters for the - more or less - static values of the self disclosure are located at the 
`selfdisclosure` key in the [application.yaml](./src/main/resources/application.yml).

The configuration parameters for the `jobrunr` values are located at the `jobrunr` key in the
[application.yaml](./src/main/resources/application.yml).
Please consult the [Jobruner documentation](https://www.jobrunr.io/en/documentation/configuration/spring/) for details 
on how to configure `jobrunr`.



## Quality Tooling

- Tests use JUnit 5, AssertJ, Mockito, and Spring Boot test slices (DataJpaTest).
- JaCoCo generates HTML and XML coverage reports (`build/reports/jacoco/test/html`).
- Checkstyle (Google Java Style) executes via `./gradlew check` and in CI.
- SpringDoc exposes live API documentation without extra configuration.

## Continuous Integration

GitLab pipeline (`.gitlab-ci.yml`) runs the following stages:

1. **lint**: `gradle checkstyleMain checkstyleTest` (currently allowed to fail).
2. **unit-test**: `gradle test` with JUnit reports (currently allowed to fail).
3. **build-jar**: produces the Boot JAR and captures the project version.
4. **publish-jar**: uploads tagged artifacts to the GitLab generic package registry.
5. **build images**: builds and pushes container images with Buildah for tagged commits.

Credentials for the Harbor registry are read from CI variables; Buildah uses the `vfs` storage
driver to stay compatible
with nested overlay filesystems.

## Logging and Observability

- Application banner resides in `src/main/resources/banner.txt`.
- Logback writes to console and `./logs/spring-boot-logger.log` with size and time based rotation.
- SLF4J is used consistently across services, controllers, and configuration.
- Actuator endpoints are included out of the box; extend exposure or add custom health indicators as
  required.

## Getting Started Checklist

1. Clone the repository and install JDK 21.
2. Run `./gradlew clean check test` to verify the setup.
3. Launch the app via `./gradlew bootRun` (HTTPS) or
   `java -jar build/libs/testfachdienst-0.0.1.jar` (HTTP).
4. Hit `https://localhost:8080/achelos_testfachdienst/swagger-ui/index.html` (add `-k` to bypass the
   self-signed cert)
   or the HTTP variant based on the chosen launch mode.
5. Verify health monitoring with
   `curl https://localhost:8080/achelos_testfachdienst/actuator/health`

## License

(C) achelos GmbH, 2025, licensed for gematik GmbH

Apache License, Version 2.0

See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under
the License.

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for use. These are regularly typical conditions in connection with open source or free software. Programs described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial portions of the Software.
    2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of or in connection with the software or the use or other dealings with the software, whether in an action of contract, tort, or otherwise.
    3. The software is the result of research and development activities, therefore not necessarily quality assured and without the character of a liable product. For this reason, gematik does not provide any support or other user assistance (unless otherwise stated in individual cases and without justification of a legal obligation). Furthermore, there is no claim to further development and adaptation of the results to a more current state of the art.
3. Gematik may remove published results temporarily or permanently from the place of publication at any time without prior notice or justification.
4. Please note: Parts of this code may have been generated using AI-supported technology. Please take this into account, especially when troubleshooting, for security analyses and possible adjustments.