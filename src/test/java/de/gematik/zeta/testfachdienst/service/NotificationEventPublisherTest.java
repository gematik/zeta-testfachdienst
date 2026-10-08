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

package de.gematik.zeta.testfachdienst.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.gematik.zeta.testfachdienst.model.NotificationEventRequest;
import de.gematik.zeta.testfachdienst.model.NotificationEventType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Unit tests for {@link NotificationEventPublisher}. */
class NotificationEventPublisherTest {

  /**
   * Verifies the exact resource-server API request and response mapping for both event types.
   *
   * @param eventType supported event type
   * @param wireValue expected JSON wire value
   */
  @ParameterizedTest
  @CsvSource({
      "REGISTERED_CLIENT_ACTIVITY, registered_client_activity",
      "NEW_CLIENT_REGISTRATION, new_client_registration"
  })
  void publishesCorrelatedEventToNotificationService(
      NotificationEventType eventType, String wireValue) {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(
        builder, "http://notification-service-rs/resource-server", "zeta");
    server.expect(once(),
            requestTo("http://notification-service-rs/resource-server/notifications"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("""
            {
              "user": {"id_type": "kvnr", "value": "X123456789"},
              "channel": "zeta",
              "payload": {
                "event_type": "%s",
                "correlation_id": "correlation-1"
              }
            }
            """.formatted(wireValue)))
        .andRespond(withSuccess("""
            {
              "notification_id": "notification-1",
              "status": "accepted",
              "channel": "zeta",
              "accepted_at": "2026-08-23T12:00:00Z"
            }
            """, MediaType.APPLICATION_JSON));

    var response = publisher.publish(new NotificationEventRequest(
        "X123456789",
        eventType,
        "correlation-1"));

    assertThat(response.notificationId()).isEqualTo("notification-1");
    assertThat(response.status()).isEqualTo("accepted");
    assertThat(response.channel()).isEqualTo("zeta");
    server.verify();
  }
}
