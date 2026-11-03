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

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

/** Tests display defaults and the serialized notification payload boundary. */
class NotificationEventRequestTest {

  /**
   * Verifies representative Unicode and multiline cases in schema and server validation.
   *
   * @param field display field to validate
   */
  @ParameterizedTest
  @ValueSource(strings = {"title", "message"})
  @SuppressWarnings("checkstyle:IllegalTokenText") // Explicit Unicode whitespace test fixtures.
  void publishesDisplayTextPatternMatchingValidation(String field) {
    Schema<?> requestSchema = new ModelConverters()
        .readAllAsResolvedSchema(NotificationEventRequest.class).schema;
    assertThat(requestSchema).isNotNull();
    assertThat(requestSchema.getProperties()).containsKey(field);
    Schema<?> schema = requestSchema.getProperties().get(field);
    String pattern = schema.getPattern();
    assertThat(pattern).doesNotContain("(?", "\\p{");
    var schemaPattern = java.util.regex.Pattern.compile(pattern);
    var originalPattern = java.util.regex.Pattern.compile("(?sU).*\\S.*");

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      for (String value : new String[] {"", " \t\r\n", "\u0085\u00A0\u2003",
          "\u2028\u2029", "\uFEFF", "\nHello\r\n", "\u00A0界😀\u2003"}) {
        var request = new NotificationEventRequest("X110411675",
            NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "event-1", null,
            field.equals("title") ? value : null, field.equals("message") ? value : null);
        boolean valid = originalPattern.matcher(value).matches();
        assertThat(schemaPattern.matcher(value).find()).as("schema: %s", value).isEqualTo(valid);
        assertThat(factory.getValidator().validate(request).isEmpty())
            .as("server: %s", value).isEqualTo(valid);
      }
    }
  }

  /**
   * Verifies each optional display field defaults independently.
   *
   * @param title supplied title or null
   * @param message supplied message or null
   * @param expectedTitle resulting display title
   * @param expectedMessage resulting display message
   */
  @ParameterizedTest
  @CsvSource({
      "Custom title,,Custom title,Activity detected for a registered client.",
      ",Custom message,ZETA Notification,Custom message",
      ",,ZETA Notification,Activity detected for a registered client."
  })
  void defaultsEachDisplayFieldIndependently(
      String title, String message, String expectedTitle, String expectedMessage) {
    var request = new NotificationEventRequest("X110411675",
        NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "event-1", null, title, message);

    assertThat(request.payload()).containsEntry("title", expectedTitle)
        .containsEntry("message", expectedMessage);
  }

  /**
   * Verifies exactly 1018 UTF-8 bytes are accepted and one additional byte is rejected.
   *
   * @param content text including ASCII, multibyte characters, or JSON escapes
   * @throws Exception when serialization fails
   */
  @ParameterizedTest
  @ValueSource(strings = {
      "ASCII", "界", "😀", "\\",
      "\uD800", "\uDC00" // Unpaired surrogates have no printable representation.
  })
  void enforcesExactSerializedByteBoundary(String content) throws Exception {
    var mapper = new ObjectMapper();
    var initial = new NotificationEventRequest("X110411675",
        NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "x", null, "Title", content.repeat(20));
    int initialBytes = mapper.writeValueAsBytes(initial.payload()).length;
    String correlationId = "x".repeat(1 + 1018 - initialBytes);
    var atLimit = new NotificationEventRequest(initial.userId(), initial.eventType(), correlationId,
        null, initial.title(), initial.message());
    var tooLarge = new NotificationEventRequest(initial.userId(), initial.eventType(),
        correlationId + "x", null, initial.title(), initial.message());

    assertThat(mapper.writeValueAsBytes(atLimit.payload()))
        .hasSize(1018);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(atLimit)).isEmpty();
      assertThat(factory.getValidator().validate(tooLarge))
          .anyMatch(v -> v.getPropertyPath().toString().equals("payloadWithinLimit"));
    }
  }

  /** Verifies incomplete requests fail validation without serialization errors. */
  @Test
  void rejectsMissingRequiredFieldsWithoutFailingPayloadValidation() {
    var request = new NotificationEventRequest(null, null, null);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(request)).hasSize(3);
    }
  }
}
