<img align="right" width="250" height="47" src="docs/img/Gematik_Logo_Flag.png"/> <br/>

# Release Notes ZETA Testfachdienst

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
