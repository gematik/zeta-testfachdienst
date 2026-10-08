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

import de.gematik.zeta.testfachdienst.model.PushChannel;
import de.gematik.zeta.testfachdienst.model.PushChannelsResponse;
import de.gematik.zeta.testfachdienst.model.PushErrorResponse;
import de.gematik.zeta.testfachdienst.model.PushPusher;
import de.gematik.zeta.testfachdienst.model.PushPushersResponse;
import de.gematik.zeta.testfachdienst.service.PushNotificationService;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the gemF PushNotification Fachdienst setup API.
 *
 * <p>Implements the Fachdienst setup routes from A_27104 and optional channel routes from
 * A_27190.
 */
@RestController
@RequestMapping("/push/v1")
@RequiredArgsConstructor
@Tag(
    name = "PushNotification",
    description = "gemF PushNotification setup routes for pushers and channels",
    externalDocs = @ExternalDocumentation(
        description = "gemF_PushNotification latest",
        url = "https://gemspec.gematik.de/docs/gemF/gemF_PushNotification/latest/"))
public class PushNotificationController {

  private final PushNotificationService service;

  /**
   * Lists all registered pushers according to A_27104.
   *
   * @return registered pushers for the current test context
   */
  @GetMapping("/pushers")
  @Operation(
      summary = "Gets the current pushers",
      description = "Returns all currently active pushers for the current test context. "
          + "Implements A_27104 from OpenApi_Notification_Fachdienst.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Pushers returned",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PushPushersResponse.class)))
  })
  public PushPushersResponse getPushers() {
    return new PushPushersResponse(service.listPushers());
  }

  /**
   * Creates, updates, or deletes a pusher according to A_27154, A_27155, and A_27156.
   *
   * @param request pusher request body
   * @return empty object on success or PushNotification error response
   */
  @PostMapping("/pushers/set")
  @Operation(
      summary = "Modify a pusher",
      description = "Creates or updates a pusher when kind is not null, deletes it when kind "
          + "is null. Implements A_27154, A_27155, A_27156, A_27193-02, and A_27197-01 "
          + "from OpenApi_Notification_Fachdienst.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "The pusher was set",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(value = "{}"))),
      @ApiResponse(
          responseCode = "400",
          description = "One or more pusher values were invalid",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PushErrorResponse.class)))
  })
  public ResponseEntity<?> setPusher(@RequestBody PushPusher request) {
    List<String> missing = missingPusherParameters(request);
    if (!missing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new PushErrorResponse(
              "Missing parameters: " + String.join(", ", missing),
              "M_MISSING_PARAM"));
    }
    service.setPusher(request);
    return ResponseEntity.ok(Map.of());
  }

  /**
   * Lists available default channels according to A_27190.
   *
   * @return available channels and their default status
   */
  @GetMapping("/channels")
  @Operation(
      summary = "Gets the available channels",
      description = "Returns all channels available to PushNotification clients. Implements "
          + "A_27190 from OpenApi_Notification_Fachdienst.")
  public PushChannelsResponse getChannels() {
    return new PushChannelsResponse(service.listAvailableChannels());
  }

  /**
   * Lists channels configured for one device according to A_27190.
   *
   * @param pushkey device push key
   * @return channel state or HTTP 400 when the device is unknown
   */
  @GetMapping("/channels/{pushkey}")
  @Operation(
      summary = "Gets channels for a specific device",
      description = "Returns configured channel states for a registered device. "
          + "Implements A_27190 from OpenApi_Notification_Fachdienst.")
  public ResponseEntity<?> getChannelsOfDevice(
      @Parameter(description = "Device push key", example = "1234567890")
      @PathVariable String pushkey) {
    return service.listDeviceChannels(pushkey)
        .<ResponseEntity<?>>map(channels -> ResponseEntity.ok(new PushChannelsResponse(channels)))
        .orElseGet(() -> invalidPushkey(pushkey));
  }

  /**
   * Updates channel configuration for one device according to A_27190.
   *
   * @param pushkey device push key
   * @param request channel request body
   * @return empty object on success or PushNotification error response
   */
  @PostMapping("/channels/{pushkey}")
  @Operation(
      summary = "Modify the channels for a specific device",
      description = "Updates channel subscriptions for a registered device. Implements A_27190 "
          + "from OpenApi_Notification_Fachdienst.")
  public ResponseEntity<?> setChannelsOfDevice(
      @Parameter(description = "Device push key", example = "1234567890")
      @PathVariable String pushkey,
      @RequestBody PushChannelsResponse request) {
    if (request == null || request.channels() == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new PushErrorResponse("Missing parameters: channels", "M_MISSING_PARAM"));
    }
    if (hasInvalidChannels(request.channels())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new PushErrorResponse("Missing parameters: id, status", "M_MISSING_PARAM"));
    }
    if (!service.updateDeviceChannels(pushkey, request.channels())) {
      return invalidPushkey(pushkey);
    }
    return ResponseEntity.ok(Map.of());
  }

  /**
   * Validates required pusher request parameters from OpenApi_Notification_Fachdienst.
   *
   * @param request pusher request
   * @return missing parameter names in spec order
   */
  private List<String> missingPusherParameters(PushPusher request) {
    var missing = new ArrayList<String>();
    if (request == null) {
      return List.of("kind", "app_id", "pushkey");
    }
    addIfBlank(missing, "app_id", request.appId());
    addIfBlank(missing, "pushkey", request.pushkey());
    if (request.kind() != null) {
      addIfBlank(missing, "app_display_name", request.appDisplayName());
      addIfBlank(missing, "device_display_name", request.deviceDisplayName());
      addIfBlank(missing, "lang", request.lang());
      if (request.data() == null) {
        missing.add("data");
      }
    }
    return missing;
  }

  /**
   * Adds the parameter name when the value is null or blank.
   *
   * @param missing missing parameter accumulator
   * @param name parameter name
   * @param value parameter value
   */
  private void addIfBlank(List<String> missing, String name, String value) {
    if (value == null || value.isBlank()) {
      missing.add(name);
    }
  }

  /**
   * Checks whether any channel entry is missing required data.
   *
   * @param channels channel entries
   * @return {@code true} when at least one entry is invalid
   */
  private boolean hasInvalidChannels(List<PushChannel> channels) {
    for (PushChannel channel : channels) {
      if (channel.id() == null || channel.id().isBlank() || channel.status() == null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builds a PushNotification invalid-pushkey response.
   *
   * @param pushkey invalid device push key
   * @return HTTP 400 error response
   */
  private ResponseEntity<PushErrorResponse> invalidPushkey(String pushkey) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new PushErrorResponse("Invalid pushkey: " + pushkey, "M_INVALID_PARAM"));
  }
}
