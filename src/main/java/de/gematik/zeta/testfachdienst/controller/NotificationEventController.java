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

import de.gematik.zeta.testfachdienst.model.NotificationChannelsQuery;
import de.gematik.zeta.testfachdienst.model.NotificationChannelsResponse;
import de.gematik.zeta.testfachdienst.model.NotificationEventRequest;
import de.gematik.zeta.testfachdienst.model.NotificationEventResponse;
import de.gematik.zeta.testfachdienst.service.NotificationEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Test-support API for notification events and resource-server channel queries. */
@RestController
@RequestMapping("/test-support/notifications")
@Tag(name = "Notification test events")
public class NotificationEventController {

  private final NotificationEventPublisher publisher;

  /**
   * Creates the notification test-support endpoint.
   *
   * @param publisher downstream event publisher
   */
  public NotificationEventController(NotificationEventPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * Emits one supported Testfachdienst event through the Notification Service.
   *
   * @param request event, user, correlation, and optional reference data
   * @param idempotencyKey optional submission key forwarded to the Notification Service
   * @return acceptance result from the Notification Service
   */
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(summary = "Emits a deterministic notification test event")
  @ApiResponse(
      responseCode = "4XX",
      description = "Invalid request or request rejected by the Notification Service",
      content = @Content(
          mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "502",
      description = "The Notification Service returned an invalid response",
      content = @Content(
          mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "503",
      description = "The Notification Service is unavailable",
      content = @Content(
          mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
          schema = @Schema(implementation = ProblemDetail.class)))
  public NotificationEventResponse trigger(
      @Valid @RequestBody NotificationEventRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return idempotencyKey == null
        ? publisher.publish(request) : publisher.publish(request, idempotencyKey);
  }

  /**
   * Obtains the user's channels from the Notification Service RS API.
   *
   * @param userId user identifier value
   * @param idType identifier type, such as kvnr or telematik-id
   * @param includeInactive whether to include inactive channels; null retains the service default
   * @return channel and device registration information from the Notification Service
   */
  @GetMapping(value = "/users/{userId}/channels", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Queries user channels through the Notification Service resource-server API",
      operationId = "getNotificationUserChannels")
  @ApiResponse(
      responseCode = "200",
      description = "User channels and device registration information returned by the NS",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = NotificationChannelsResponse.class)))
  @ApiResponse(
      responseCode = "4XX",
      description = "Invalid request or request rejected by the Notification Service",
      content = @Content(
          mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "502",
      description = "The Notification Service returned an invalid response",
      content = @Content(
          mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "503",
      description = "The Notification Service is unavailable",
      content = @Content(
          mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
          schema = @Schema(implementation = ProblemDetail.class)))
  public NotificationChannelsResponse getChannels(
      @PathVariable String userId,
      @RequestParam("id_type") String idType,
      @RequestParam(value = "include_inactive", required = false) Boolean includeInactive) {
    return publisher.getChannels(userId, idType, includeInactive);
  }

  /**
   * Queries the RS channel endpoint with JSON input parameters.
   *
   * @param userId user identifier value
   * @param query identifier type and optional inactive-channel flag
   * @return channel information returned by the Notification Service
   */
  @PostMapping(value = "/users/{userId}/channels/query",
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Triggers a Notification Service channel query from JSON parameters",
      operationId = "queryNotificationUserChannels")
  @ApiResponse(responseCode = "200", description = "User channels returned by the NS",
      content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = NotificationChannelsResponse.class)))
  public NotificationChannelsResponse queryChannels(
      @PathVariable String userId, @Valid @RequestBody NotificationChannelsQuery query) {
    return publisher.getChannels(userId, query.idType(), query.includeInactive());
  }

}
