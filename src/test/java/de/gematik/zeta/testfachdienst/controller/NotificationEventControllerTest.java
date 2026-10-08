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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.zeta.testfachdienst.model.NotificationEventRequest;
import de.gematik.zeta.testfachdienst.model.NotificationEventResponse;
import de.gematik.zeta.testfachdienst.model.NotificationEventType;
import de.gematik.zeta.testfachdienst.service.NotificationEventPublisher;
import org.junit.jupiter.api.Test;
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
}
