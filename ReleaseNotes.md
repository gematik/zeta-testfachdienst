<img align="right" width="250" height="47" src="docs/img/Gematik_Logo_Flag.png"/> <br/>

# Release Notes ZETA Testfachdienst

## Release 0.3.0

TestFachdienst 0.3.0

This release starts the release notes for TestFachdienst beginning with version 0.3.0.
It includes targeted API behavior updates and model validation adjustments.

#### Release Focus:

- Added `GET /zeta-v1/hello-zeta/proxy-error` endpoint to return a proxy-specific error response.
- Added `ZETA-Cause: Proxy` response header with `HTTP 400 Bad Request` for the proxy error endpoint.
- Added controller test coverage for the new proxy error response behavior.
- Removed `@PastOrPresent` and `@FutureOrPresent` annotations from `Erezept` date fields.

### Known issues:

- None documented for this release.

## Release 0.2.1

### added:
- Support for WebSockets

## Release 0.1.3

### added:
- Prototype of the ZETA SDK added
