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

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Maps Notification Service failures to stable, sanitized HTTP problem responses. */
@RestControllerAdvice(assignableTypes = NotificationEventController.class)
public class NotificationEventExceptionHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(NotificationEventExceptionHandler.class);
  private static final URI UNAVAILABLE_TYPE =
      URI.create("urn:problem:notification-service-unavailable");
  private static final URI REJECTED_TYPE =
      URI.create("urn:problem:notification-service-request-rejected");
  private static final URI BAD_GATEWAY_TYPE =
      URI.create("urn:problem:notification-service-invalid-response");

  /**
   * Maps connection failures and timeouts to service unavailable.
   *
   * @param exception downstream transport failure
   * @return sanitized problem response
   */
  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<ProblemDetail> handleUnavailable(ResourceAccessException exception) {
    LOGGER.warn("Notification Service is unavailable", exception);
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        UNAVAILABLE_TYPE,
        "Notification Service unavailable",
        "The notification event could not be delivered. Try again later.");
  }

  /**
   * Preserves downstream client-error statuses and maps downstream server failures to bad gateway.
   *
   * @param exception downstream HTTP error response
   * @return sanitized problem response
   */
  @ExceptionHandler(RestClientResponseException.class)
  public ResponseEntity<ProblemDetail> handleErrorResponse(
      RestClientResponseException exception) {
    HttpStatusCode downstreamStatus = exception.getStatusCode();
    if (downstreamStatus.is4xxClientError()) {
      LOGGER.warn(
          "Notification Service rejected notification request with status {}", downstreamStatus);
      return problem(
          downstreamStatus,
          REJECTED_TYPE,
          "Notification request rejected",
          "The Notification Service rejected the notification request.");
    }

    LOGGER.warn("Notification Service returned error status {}", downstreamStatus);
    return badGateway();
  }

  /**
   * Maps other REST-client failures, such as unreadable downstream responses, to bad gateway.
   *
   * @param exception downstream client failure
   * @return sanitized problem response
   */
  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ProblemDetail> handleClientFailure(RestClientException exception) {
    LOGGER.warn("Notification Service request failed", exception);
    return badGateway();
  }

  private static ResponseEntity<ProblemDetail> badGateway() {
    return problem(
        HttpStatus.BAD_GATEWAY,
        BAD_GATEWAY_TYPE,
        "Invalid Notification Service response",
        "The Notification Service could not complete the notification request.");
  }

  private static ResponseEntity<ProblemDetail> problem(
      HttpStatusCode status, URI type, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    problem.setTitle(title);
    return ResponseEntity.status(status).body(problem);
  }
}
