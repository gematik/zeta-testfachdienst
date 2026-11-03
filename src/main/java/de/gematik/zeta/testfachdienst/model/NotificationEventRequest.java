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

package de.gematik.zeta.testfachdienst.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Request for emitting a deterministic notification test event. */
@Schema(description = "Notification event with optional display text. The payload, including "
    + "event metadata, must not exceed 1018 UTF-8 JSON bytes.")
public record NotificationEventRequest(
    @JsonProperty("user_id") @NotBlank String userId,
    @JsonProperty("event_type") @NotNull NotificationEventType eventType,
    @JsonProperty("correlation_id") @NotBlank String correlationId,
    String reference,
    @Schema(description = "Plain-text title; defaults to ZETA Notification", example = "ZETA Test")
    @Size(max = 80)
    @Pattern(regexp = NON_BLANK_PATTERN, message = "must not be blank") String title,
    @Schema(description = "Plain-text message; defaults to a description of the event",
        example = "Hello from Testfachdienst!")
    @Size(max = 500)
    @Pattern(regexp = NON_BLANK_PATTERN, message = "must not be blank") String message) {

  // Explicit Unicode White_Space preserves Java's Unicode validation in ECMA-262 schemas.
  // [\s\S] accepts line breaks without Java-specific inline flags.
  @SuppressWarnings("checkstyle:IllegalTokenText") // Escapes belong to the published regex syntax.
  private static final String NON_BLANK_PATTERN =
      "[\\s\\S]*[^\\u0009-\\u000D\\u0020\\u0085\\u00A0\\u1680"
          + "\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000][\\s\\S]*";

  private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper();
  private static final int MAX_PAYLOAD_BYTES = 1018;

  /**
   * Creates an event without a history reference.
   *
   * @param userId recipient identifier
   * @param eventType supported event type
   * @param correlationId event correlation identifier
   */
  public NotificationEventRequest(
      String userId, NotificationEventType eventType, String correlationId) {
    this(userId, eventType, correlationId, null, null, null);
  }

  /**
   * Creates an event with a history reference and default display text.
   *
   * @param userId recipient identifier
   * @param eventType supported event type
   * @param correlationId event correlation identifier
   * @param reference optional history reference
   */
  public NotificationEventRequest(
      String userId, NotificationEventType eventType, String correlationId, String reference) {
    this(userId, eventType, correlationId, reference, null, null);
  }

  /**
   * Builds the payload forwarded to the Notification Service, including display defaults.
   *
   * @return plain-text display fields and event metadata
   */
  public Map<String, String> payload() {
    String defaultMessage = switch (eventType) {
      case REGISTERED_CLIENT_ACTIVITY -> "Activity detected for a registered client.";
      case NEW_CLIENT_REGISTRATION -> "A new client has been registered.";
    };
    return Map.of(
        "event_type", eventType.value(),
        "correlation_id", correlationId,
        "title", title == null ? "ZETA Notification" : title,
        "message", message == null ? defaultMessage : message);
  }

  /**
   * Validates the complete UTF-8 JSON payload against the encrypted push transport limit.
   *
   * @return whether the payload fits, or required fields still need separate validation
   */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "notification payload must not exceed 1018 UTF-8 JSON bytes")
  public boolean isPayloadWithinLimit() {
    if (eventType == null || correlationId == null) {
      return true;
    }
    try {
      return PAYLOAD_MAPPER.writeValueAsBytes(payload()).length <= MAX_PAYLOAD_BYTES;
    } catch (JacksonException exception) {
      throw new IllegalStateException("Cannot serialize notification payload", exception);
    }
  }
}
