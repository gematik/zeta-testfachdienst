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

package de.gematik.zeta.testfachdienst.ws;

import de.gematik.zeta.testfachdienst.ws.model.WebSocketErrorResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for WebSocket operations.
 *
 * <p>Catches exceptions thrown from {@code @MessageMapping} methods and returns
 * structured error responses to the requesting client on the same destination where they expect
 * successful responses.
 */
@ControllerAdvice
@Slf4j
public class WebSocketExceptionHandler {

  /**
   * Handle all exceptions from WebSocket message handlers.
   *
   * @param ex the exception that was thrown
   * @return structured error response sent to the user's queue
   */
  @MessageExceptionHandler
  @SendToUser("/queue/erezept")
  public WebSocketErrorResponse handleException(Exception ex) {
    if (ex instanceof ResponseStatusException rsEx) {
      log.warn("WebSocket error [{}]: {}", rsEx.getStatusCode(), rsEx.getReason());
      return WebSocketErrorResponse.builder()
          .status(rsEx.getStatusCode().value())
          .message(rsEx.getReason())
          .timestamp(OffsetDateTime.now())
          .build();
    }

    if (ex instanceof MethodArgumentNotValidException validationEx) {
      log.warn("Validation error: {}", ex.getMessage());
      Map<String, String> errors = validationEx.getBindingResult()
          .getFieldErrors()
          .stream()
          .collect(Collectors.toMap(
              FieldError::getField,
              err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value"
          ));
      return WebSocketErrorResponse.builder()
          .status(400)
          .message("Validation failed")
          .timestamp(OffsetDateTime.now())
          .details(Map.of("errors", errors))
          .build();
    }

    if (ex instanceof MessageConversionException conversionEx) {
      log.warn("Message conversion error: {}", ex.getMessage());
      return WebSocketErrorResponse.builder()
          .status(400)
          .message("Invalid message format or missing required fields")
          .timestamp(OffsetDateTime.now())
          .details(Map.of("error", conversionEx.getMessage()))
          .build();
    }

    log.error("Unexpected WebSocket error", ex);
    return WebSocketErrorResponse.builder()
        .status(500)
        .message("An unexpected error occurred")
        .timestamp(OffsetDateTime.now())
        .build();
  }
}
