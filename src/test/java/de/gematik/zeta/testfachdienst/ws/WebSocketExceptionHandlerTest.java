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

package de.gematik.zeta.testfachdienst.ws;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zeta.testfachdienst.model.Erezept;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for {@link WebSocketExceptionHandler}. */
class WebSocketExceptionHandlerTest {

  private final WebSocketExceptionHandler handler = new WebSocketExceptionHandler();

  @Test
  void handlesResponseStatusException() {
    var response = handler.handleException(
        new ResponseStatusException(HttpStatus.CONFLICT, "duplicate"));

    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(response.getMessage()).isEqualTo("duplicate");
    assertThat(response.getTimestamp()).isNotNull();
  }

  @Test
  void handlesValidationErrors() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Erezept(), "erezept");
    bindingResult.addError(new FieldError("erezept", "medicationName", "must not be blank"));
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(message(), validatedParameter(), bindingResult);

    var response = handler.handleException(exception);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getMessage()).isEqualTo("Validation failed");
    assertThat(response.getDetails())
        .containsKey("errors")
        .extractingByKey("errors")
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
        .containsEntry("medicationName", "must not be blank");
  }

  @Test
  void handlesMessageConversionErrors() {
    var response = handler.handleException(new MessageConversionException("invalid payload"));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getMessage())
        .isEqualTo("Invalid message format or missing required fields");
    assertThat(response.getDetails()).containsEntry("error", "invalid payload");
  }

  @Test
  void handlesDestinationVariableTypeMismatches() throws Exception {
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(message(), idParameter(), "not-a-number");

    var response = handler.handleException(exception);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getMessage()).isEqualTo("Invalid STOMP destination variable");
    assertThat(response.getDetails())
        .containsKey("error")
        .extractingByKey("error")
        .asString()
        .contains("not-a-number");
  }

  @Test
  void handlesUnexpectedExceptions() {
    var response = handler.handleException(new IllegalStateException("boom"));

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getMessage()).isEqualTo("An unexpected error occurred");
    assertThat(response.getTimestamp()).isNotNull();
  }

  /**
   * Create a minimal websocket message for exception handler tests.
   *
   * @return message with a simple payload
   */
  private Message<String> message() {
    return MessageBuilder.withPayload("payload").build();
  }

  /**
   * Resolve the validated method parameter used to simulate bean validation failures.
   *
   * @return method parameter representing the validated {@code Erezept} argument
   * @throws NoSuchMethodException if the test helper method cannot be found
   */
  private MethodParameter validatedParameter() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("validatedMethod", Erezept.class);
    return new MethodParameter(method, 0);
  }

  /**
   * Resolve a numeric method parameter used to simulate destination-variable binding failures.
   *
   * @return method parameter representing a numeric id argument
   * @throws NoSuchMethodException if the test helper method cannot be found
   */
  private MethodParameter idParameter() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("idMethod", Long.class);
    return new MethodParameter(method, 0);
  }

  /**
   * Helper signature used to obtain a {@link MethodParameter} with validation metadata.
   *
   * @param erezept payload argument used purely for reflective lookup
   */
  @SuppressWarnings("unused")
  private void validatedMethod(Erezept erezept) {}

  @SuppressWarnings("unused")
  private void idMethod(Long id) {}
}
