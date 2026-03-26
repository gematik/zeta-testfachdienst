/*-
 * #%L
 * ZETA Testfachdienst
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik
 * find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.testfachdienst.controller;

import de.gematik.zeta.testfachdienst.model.HelloZetaResource;
import de.gematik.zeta.testfachdienst.service.HelloZetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller that exposes the public {@code /hellozeta} endpoints.
 *
 * <p>The controller delegates retrieval of the greeting resource to
 * {@link HelloZetaService} and augments the interaction with trace logs.</p>
 */
@RestController
@RequestMapping("/hellozeta")
@Slf4j
@Tag(
    name = "Hello ZETA",
    description = "Showcase endpoints used to verify simple REST behavior and proxy error handling")
public class HelloZetaController {

  private final HelloZetaService service;

  /**
   * Create a new controller instance with the required service dependency.
   *
   * @param service business service that provides the greeting resource
   */
  public HelloZetaController(HelloZetaService service) {
    this.service = service;
    log.debug(this.getClass().getSimpleName() + " initialized!");
  }

  /**
   * Retrieve the greeting resource and return it as the response body.
   *
   * @param responseDelay optional delay in seconds before sending the response
   * @return HTTP 200 response containing the greeting payload
   */
  @GetMapping
  @Operation(
      summary = "Return the Hello ZETA greeting",
      description = "Provides a minimal JSON payload that can be used as a smoke-test "
          + "endpoint. The optional responseDelay query parameter delays the response by the "
          + "given number of seconds.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Greeting returned successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = HelloZetaResource.class),
              examples = @ExampleObject(value = "{\"message\":\"Hello ZETA!\"}")))
  })
  public ResponseEntity<HelloZetaResource> getHelloZetaResponse(
      @Parameter(description = "Optional response delay in seconds.")
      @RequestParam(required = false) Integer responseDelay) {
    log.debug("Trying to get resource from HelloZetaService.");
    return buildHelloZetaResponse(responseDelay);
  }

  /**
   * Retrieve the greeting resource and return it as the response body after a path-based delay.
   *
   * @param responseDelay delay in seconds before sending the response
   * @return HTTP 200 response containing the greeting payload
   */
  @GetMapping("/delay/{seconds}")
  @Operation(
      summary = "Get the Hello ZETA payload with a path-based delay",
      description = "Returns the static greeting payload after waiting for the requested "
          + "non-negative number of seconds.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Greeting payload returned successfully after the requested delay",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = HelloZetaResource.class),
              examples = @ExampleObject(value = "{\"message\":\"Hello ZETA!\"}"))),
      @ApiResponse(
          responseCode = "400",
          description = "The supplied delay is negative")
  })
  public ResponseEntity<HelloZetaResource> getHelloZetaResponseWithPathDelay(
      @Parameter(description = "Required non-negative response delay in seconds.")
      @PathVariable("seconds") Integer responseDelay) {
    validatePathDelay(responseDelay);
    log.debug("Trying to get delayed resource from HelloZetaService using path variable.");
    return buildHelloZetaResponse(responseDelay);
  }

  /**
   * Return a response that signals a proxy error to the caller.
   *
   * @return HTTP 400 response with ZETA-Cause header set to Proxy
   */
  @GetMapping("/proxy-error")
  @Operation(
      summary = "Return a synthetic proxy error",
      description = "Returns the normal greeting payload but with HTTP 400 and the ZETA-Cause "
          + "header set to Proxy so callers can test error-handling flows.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "400",
          description = "Synthetic proxy error returned",
          headers = @Header(
              name = "ZETA-Cause",
              description = "Reason for the proxy-specific failure.",
              schema = @Schema(example = "Proxy")),
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = HelloZetaResource.class),
              examples = @ExampleObject(value = "{\"message\":\"Hello ZETA!\"}")))
  })
  public ResponseEntity<HelloZetaResource> getHelloZetaProxyErrorResponse() {
    log.debug("Returning proxy error response for HelloZeta.");
    HelloZetaResource resource = service.getHelloZetaResource();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .header("ZETA-Cause", "Proxy")
        .body(resource);
  }

  /**
   * Apply an optional response delay, then return the standard greeting payload.
   *
   * @param responseDelay optional delay in seconds before reading the resource
   * @return HTTP 200 response containing the greeting payload
   */
  private ResponseEntity<HelloZetaResource> buildHelloZetaResponse(Integer responseDelay) {
    if (responseDelay != null && responseDelay > 0) {
      try {
        log.debug("Delaying HelloZeta response by {} seconds.", responseDelay);
        Thread.sleep(Duration.ofSeconds(responseDelay).toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("HelloZeta response delay was interrupted.", e);
      }
    }

    HelloZetaResource resource = service.getHelloZetaResource();
    log.debug("Got resource: '{}' from service, returning resource with 200.", resource);
    return ResponseEntity.ok(resource);
  }

  private void validatePathDelay(Integer responseDelay) {
    if (responseDelay < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Delay seconds must be non-negative.");
    }
  }
}
