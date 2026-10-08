<img align="right" width="250" height="47" src="docs/img/Gematik_Logo_Flag.png"/> <br/>

# Release Notes ZETA Testfachdienst

## Version 1.3.0

TestFachdienst 1.3.0

#### Release Focus:

- Added the `POST /test-support/notifications` API for triggering deterministic notification events.
- Added stable `ProblemDetail` responses for rejected, unavailable, or invalid Notification Service responses.
- Removed the `NOTIFICATION_SERVICE_TEST_SUPPORT_ENABLED` toggle; the notification test-support endpoint is available directly.
- Refreshed the checked-in Swagger documentation for the notification test-event API and its error responses.
- Refreshed the checked-in AsyncAPI snapshot version to `1.3.0`.

### Known issues:

- None documented for this release.

#### Limitations

- None documented for this release.

## Version 1.2.0

TestFachdienst 1.2.0

#### Release Focus:

- Added PushNotification Fachdienst setup endpoints for pushers and channel state handling.
- Aligned channel routes with `OpenApi_Notification_Fachdienst_V1.1.0` by addressing device channel state by `pushkey`.
- Added the official Fachdienst OpenAPI reference and refreshed checked-in Swagger documentation.
- Documented the PushNotification AFO coverage and local API documentation refresh tasks.
- Added Spring-aligned OpenTelemetry Logback export for structured application log records.
- Forward self disclosure logs through OTLP with SLF4J key/value pairs captured as log attributes when telemetry is enabled.
- Refreshed the checked-in AsyncAPI snapshot version to `1.2.0`.

### Known issues:

- None documented for this release.

#### Limitations

- PushNotification state is held in memory and is reset when the application restarts.
- The standalone container image keeps the OpenTelemetry SDK disabled by default; deployments must enable OTLP export explicitly.

## Version 1.0.0

TestFachdienst 1.0.0

#### Release Focus:

- Harden STOMP/WebSocket logging

## Version 0.5.0

TestFachdienst 0.5.0

#### Release Focus:

- Added a STOMP/WebSocket Hello ZETA endpoint at `/app/hellozeta`.
- Clients sending to `/app/hellozeta` now receive the Hello ZETA payload on their private `/user/queue/hellozeta` destination.
- The WebSocket Hello ZETA flow reuses the existing `HelloZetaService`, so the STOMP response payload matches the established HTTP Hello ZETA response.

### Known issues:

- None documented for this release.

#### Limitations

- None documented for this release.

## Version 0.4.0

#### Release Focus:

- Improved WebSocket error handling for invalid destination variables.
- Added structured WebSocket reply schemas for list and delete responses in the AsyncAPI model.
- Expanded integration and controller test coverage for HTTP, WebSocket, repository, service, and self-disclosure behavior.
- Migrated the service to Spring Boot 4 and updated build, CI, Docker, and generated API documentation accordingly.
- Removed `@PastOrPresent` and `@FutureOrPresent` annotations from `Erezept` date fields.

### Known issues:

- None documented for this release.

#### Limitations

- None documented for this release.

## Version: 0.3.0

TestFachdienst 0.3.0

This release starts the release notes for TestFachdienst beginning with version 0.3.0.
It includes targeted API behavior updates and model validation adjustments.

#### Release Focus:

- Added `GET /hellozeta/delay/{seconds}` to expose a path-based response delay for the Hello ZETA payload.
- Negative values on `GET /hellozeta/delay/{seconds}` are now rejected with `HTTP 400 Bad Request`.
- Added `GET /hellozeta/proxy-error` endpoint to return a proxy-specific error response.
- Added `ZETA-Cause: Proxy` response header with `HTTP 400 Bad Request` for the proxy error endpoint.
- Synced the checked-in REST API documentation with the Hello ZETA delay and proxy-error endpoints.
- Added controller test coverage for the new proxy error response behavior.
- Removed `@PastOrPresent` and `@FutureOrPresent` annotations from `Erezept` date fields.

### Known issues:

- None documented for this release.

#### Limitations

- Release notes are currently maintained starting with version 0.3.0.
