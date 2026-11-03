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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.zeta.testfachdienst.model.NotificationChannelsResponse;
import de.gematik.zeta.testfachdienst.model.NotificationChannelsResponse.UserIdentifier;
import de.gematik.zeta.testfachdienst.model.NotificationEventRequest;
import de.gematik.zeta.testfachdienst.model.NotificationEventResponse;
import de.gematik.zeta.testfachdienst.model.NotificationEventType;
import de.gematik.zeta.testfachdienst.model.PushChannel;
import de.gematik.zeta.testfachdienst.model.PushChannelStatus;
import de.gematik.zeta.testfachdienst.service.NotificationEventPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/** HTTP-boundary tests for {@link NotificationEventController}. */
class NotificationEventControllerTest {

  /**
   * Verifies test support publishes the event and returns its acceptance result.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void testSupportPublishesEvent() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.publish(any())).thenReturn(new NotificationEventResponse(
        "notification-1", "accepted", "zeta", "2026-08-23T12:00:00Z"));
    MockMvc mvc = mockMvc(publisher);

    mvc.perform(post("/test-support/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.notification_id").value("notification-1"))
        .andExpect(jsonPath("$.status").value("accepted"))
        .andExpect(jsonPath("$.channel").value("zeta"));

    verify(publisher).publish(new NotificationEventRequest(
        "X123456789", NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "correlation-1"));
  }

  /**
   * Verifies unsupported event types are rejected before publishing.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void rejectsUnsupportedEventType() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    MockMvc mvc = mockMvc(publisher);

    mvc.perform(post("/test-support/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "user_id": "X123456789",
                  "event_type": "arbitrary_event",
                  "correlation_id": "correlation-1"
                }
                """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(publisher);
  }

  /**
   * Verifies connection failures and timeouts return a retryable service-unavailable response.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void reportsUnavailableNotificationService() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.publish(any())).thenThrow(new ResourceAccessException("Connection refused"));

    mockMvc(publisher)
        .perform(post("/test-support/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type")
            .value("urn:problem:notification-service-unavailable"))
        .andExpect(jsonPath("$.title").value("Notification Service unavailable"))
        .andExpect(jsonPath("$.status").value(503));
  }

  /**
   * Verifies downstream client-error statuses are preserved with a sanitized problem response.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void preservesDownstreamClientErrorStatus() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.publish(any()))
        .thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_CONTENT));

    mockMvc(publisher)
        .perform(post("/test-support/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest()))
        .andExpect(status().isUnprocessableContent())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type")
            .value("urn:problem:notification-service-request-rejected"))
        .andExpect(jsonPath("$.title").value("Notification request rejected"))
        .andExpect(jsonPath("$.status").value(422));
  }

  /**
   * Verifies downstream server errors are represented as bad gateway responses.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void mapsDownstreamServerErrorToBadGateway() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.publish(any()))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    assertBadGateway(mockMvc(publisher));
  }

  /**
   * Verifies unreadable or otherwise invalid downstream responses become bad gateway responses.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void mapsOtherRestClientFailuresToBadGateway() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.publish(any())).thenThrow(new RestClientException("Unreadable response"));

    assertBadGateway(mockMvc(publisher));
  }

  private static void assertBadGateway(MockMvc mvc) throws Exception {
    mvc.perform(post("/test-support/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest()))
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type")
            .value("urn:problem:notification-service-invalid-response"))
        .andExpect(jsonPath("$.title").value("Invalid Notification Service response"))
        .andExpect(jsonPath("$.status").value(502));
  }

  private static MockMvc mockMvc(NotificationEventPublisher publisher) {
    return MockMvcBuilders
        .standaloneSetup(new NotificationEventController(publisher))
        .setControllerAdvice(new NotificationEventExceptionHandler())
        .build();
  }

  private static String validRequest() {
    return """
        {
          "user_id": "X123456789",
          "event_type": "registered_client_activity",
          "correlation_id": "correlation-1"
        }
        """;
  }

  /**
   * Verifies the endpoint binds the default selection and preserves the NS response fields.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void queriesChannelsWithoutOverridingDefaultSelection() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.getChannels("X110411675", "kvnr", null))
        .thenReturn(new NotificationChannelsResponse(
            new UserIdentifier("kvnr", "X110411675"), true, 2,
            List.of(new PushChannel("zeta", PushChannelStatus.ENABLED))));

    mockMvc(publisher)
        .perform(get("/test-support/notifications/users/X110411675/channels")
            .param("id_type", "kvnr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id_type").value("kvnr"))
        .andExpect(jsonPath("$.user.value").value("X110411675"))
        .andExpect(jsonPath("$.has_registered_devices").value(true))
        .andExpect(jsonPath("$.registered_device_count").value(2))
        .andExpect(jsonPath("$.channels[0].id").value("zeta"))
        .andExpect(jsonPath("$.channels[0].status").value("enabled"));

    verify(publisher).getChannels("X110411675", "kvnr", null);
  }

  /**
   * Verifies the optional device count is omitted when the NS does not provide it.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void omitsUnspecifiedDeviceCount() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.getChannels("X110411675", "kvnr", null))
        .thenReturn(new NotificationChannelsResponse(
            new UserIdentifier("kvnr", "X110411675"), true, null, List.of()));

    mockMvc(publisher)
        .perform(get("/test-support/notifications/users/X110411675/channels")
            .param("id_type", "kvnr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registered_device_count").doesNotExist());
  }

  /**
   * Verifies Telematik identifiers and explicit inactive-channel selection are passed downstream.
   *
   * @param includeInactive explicit inactive-channel selection
   * @throws Exception when HTTP request handling fails
   */
  @ParameterizedTest
  @CsvSource({"true", "false"})
  void queriesTelematikChannels(boolean includeInactive) throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    var userId = "5-2-KH-AKA-1234567890";
    when(publisher.getChannels(userId, "telematik-id", includeInactive))
        .thenReturn(new NotificationChannelsResponse(
            new UserIdentifier("telematik-id", userId), true, 1, List.of()));

    mockMvc(publisher)
        .perform(get("/test-support/notifications/users/{userId}/channels", userId)
            .param("id_type", "telematik-id")
            .param("include_inactive", Boolean.toString(includeInactive)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id_type").value("telematik-id"))
        .andExpect(jsonPath("$.user.value").value(userId));

    verify(publisher).getChannels(userId, "telematik-id", includeInactive);
  }

  /**
   * Verifies malformed trigger parameters are rejected before any downstream request.
   *
   * @param queryParameters invalid query parameters
   * @throws Exception when HTTP request handling fails
   */
  @ParameterizedTest
  @CsvSource({"''", "id_type=kvnr&include_inactive=invalid"})
  void rejectsInvalidChannelQueryParameters(String queryParameters) throws Exception {
    var publisher = mock(NotificationEventPublisher.class);

    mockMvc(publisher)
        .perform(get("/test-support/notifications/users/X110411675/channels?" + queryParameters))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(publisher);
  }

  /**
   * Verifies the NS handles identifier-type validation and its error status is preserved.
   *
   * @param downstreamStatus downstream client error status
   * @throws Exception when HTTP request handling fails
   */
  @ParameterizedTest
  @CsvSource({"400", "404", "429"})
  void preservesChannelQueryClientErrors(int downstreamStatus) throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.getChannels("X110411675", "invalid", null))
        .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(downstreamStatus),
            "Downstream private error"));

    mockMvc(publisher)
        .perform(get("/test-support/notifications/users/X110411675/channels")
            .param("id_type", "invalid"))
        .andExpect(status().is(downstreamStatus))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(downstreamStatus))
        .andExpect(jsonPath("$.detail")
            .value("The Notification Service rejected the notification request."));

    verify(publisher).getChannels("X110411675", "invalid", null);
  }

  /**
   * Verifies channel queries share the existing transport and response error handling.
   *
   * @param failureType downstream failure category
   * @param expectedStatus expected public response status
   * @throws Exception when HTTP request handling fails
   */
  @ParameterizedTest
  @CsvSource({"transport, 503", "server, 502", "response, 502"})
  void mapsChannelQueryFailures(String failureType, int expectedStatus) throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    RestClientException failure = switch (failureType) {
      case "transport" -> new ResourceAccessException("Connection refused");
      case "server" -> new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
      default -> new RestClientException("Invalid response");
    };
    when(publisher.getChannels("X110411675", "kvnr", null)).thenThrow(failure);

    mockMvc(publisher)
        .perform(get("/test-support/notifications/users/X110411675/channels")
            .param("id_type", "kvnr"))
        .andExpect(status().is(expectedStatus))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(expectedStatus));
  }

  /**
   * Verifies optional event submission values reach the publisher through the HTTP API.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void forwardsIdempotencyAndReference() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    var request = new NotificationEventRequest("X110411675",
        NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "event-1", "reference-1");
    when(publisher.publish(request, "key-1")).thenReturn(new NotificationEventResponse(
        "notification-1", "accepted", "zeta", "2026-10-14T12:00:00Z"));

    mockMvc(publisher).perform(post("/test-support/notifications")
            .header("Idempotency-Key", "key-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"user_id":"X110411675","event_type":"registered_client_activity",
                 "correlation_id":"event-1","reference":"reference-1"}
                """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.notification_id").value("notification-1"));

    verify(publisher).publish(request, "key-1");
  }

  /**
   * Verifies JSON parameters invoke the same RS query operation as the GET endpoint.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void jsonParametersTriggerTheResourceServerQuery() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    when(publisher.getChannels("X123456789", "kvnr", true))
        .thenReturn(new NotificationChannelsResponse(
            new UserIdentifier("kvnr", "X123456789"), true, 1,
            List.of(new PushChannel("zeta", PushChannelStatus.ENABLED))));
    mockMvc(publisher).perform(post("/test-support/notifications/users/X123456789/channels/query")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id_type\":\"kvnr\",\"include_inactive\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.value").value("X123456789"))
        .andExpect(jsonPath("$.channels[0].status").value("enabled"));
    verify(publisher).getChannels("X123456789", "kvnr", true);
  }

  /**
   * Verifies a missing identifier type never reaches the outbound query.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void jsonQueryRequiresAnIdentifierType() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    mockMvc(publisher).perform(post("/test-support/notifications/users/X123456789/channels/query")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(publisher);
  }

  /**
   * Verifies custom display fields bind together with existing idempotency and reference fields.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void acceptsCustomNotificationText() throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    var request = new NotificationEventRequest("X110411675",
        NotificationEventType.NEW_CLIENT_REGISTRATION, "event-2", "reference-2",
        "ZETA Test", "Hello from Testfachdienst!");
    when(publisher.publish(request, "key-2")).thenReturn(new NotificationEventResponse(
        "notification-2", "accepted", "zeta", "2026-10-19T10:00:00Z"));

    mockMvc(publisher).perform(post("/test-support/notifications")
            .header("Idempotency-Key", "key-2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(request)))
        .andExpect(status().isAccepted());

    verify(publisher).publish(request, "key-2");
  }

  /**
   * Verifies blank and overly long display fields are rejected before publishing.
   *
   * @param field display field to validate
   * @throws Exception when HTTP request handling fails
   */
  @ParameterizedTest
  @ValueSource(strings = {"title", "message"})
  void rejectsInvalidDisplayText(String field) throws Exception {
    for (String value : List.of("", " \n\t", "\u2003", "\u2002", "\u00A0",
        "x".repeat(field.equals("title") ? 81 : 501))) {
      var publisher = mock(NotificationEventPublisher.class);
      var request = new NotificationEventRequest("X110411675",
          NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "event-2", null,
          field.equals("title") ? value : null, field.equals("message") ? value : null);

      mockMvc(publisher).perform(post("/test-support/notifications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(new ObjectMapper().writeValueAsString(request)))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(publisher);
    }
  }

  /**
   * Verifies the byte limit includes metadata, multibyte text, and JSON escaping.
   *
   * @param oversizedPart part that exceeds the payload budget
   * @throws Exception when HTTP request handling fails
   */
  @ParameterizedTest
  @ValueSource(strings = {"unicode", "escaping", "metadata",
      "unpaired-high-surrogate", "unpaired-low-surrogate"})
  void rejectsOversizedPayload(String oversizedPart) throws Exception {
    var publisher = mock(NotificationEventPublisher.class);
    String message = switch (oversizedPart) {
      case "unicode" -> "界".repeat(400);
      case "escaping" -> "\\".repeat(500);
      case "unpaired-high-surrogate" -> "\uD800".repeat(500); // Unpaired high surrogate.
      case "unpaired-low-surrogate" -> "\uDC00".repeat(500); // Unpaired low surrogate.
      default -> "Hello";
    };
    var request = new NotificationEventRequest("X110411675",
        NotificationEventType.REGISTERED_CLIENT_ACTIVITY,
        oversizedPart.equals("metadata") ? "x".repeat(1018) : "event-2", null, "ZETA", message);

    mockMvc(publisher).perform(post("/test-support/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new tools.jackson.databind.ObjectMapper().writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(publisher);
  }
}
