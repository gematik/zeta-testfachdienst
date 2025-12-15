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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.testfachdienst.ws.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response model for WebSocket operations.
 *
 * <p>This response is sent when a {@code @MessageMapping} method throws an exception.
 * Clients receive this on the same destination where they expect successful responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "WebSocketErrorResponse", description = "Error response for WebSocket operations")
public class WebSocketErrorResponse {

  @Schema(description = "HTTP status code", example = "404")
  @NotBlank
  private int status;

  @Schema(description = "Human-readable error message",
      example = "ERezept not found for id=123")
  @NotBlank
  private String message;

  @Schema(description = "Timestamp when error occurred",
      example = "2025-10-21T14:30:00Z",
      format = "date-time")
  @NotBlank
  private OffsetDateTime timestamp;

  @Schema(description = "Additional error details (optional)",
      example = "{\"errors\": {\"medicationName\": \"must not be blank\"}}")
  private Map<String, Object> details;
}
