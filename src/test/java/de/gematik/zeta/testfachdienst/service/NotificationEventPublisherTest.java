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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.gematik.zeta.testfachdienst.model.NotificationEventRequest;
import de.gematik.zeta.testfachdienst.model.NotificationEventType;
import de.gematik.zeta.testfachdienst.model.PushChannel;
import de.gematik.zeta.testfachdienst.model.PushChannelStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
                "correlation_id": "correlation-1",
                "title": "ZETA Notification",
                "message": "%s"
              }
            }
            """.formatted(wireValue, eventType == NotificationEventType.REGISTERED_CLIENT_ACTIVITY
                ? "Activity detected for a registered client."
                : "A new client has been registered.")))
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

  /**
   * Verifies identifier types, optional filtering, and all returned channel states.
   *
   * @param userId user identifier
   * @param idType identifier type
   * @param includeInactive optional inactive-channel selection
   * @param querySuffix expected optional query parameter
   */
  @ParameterizedTest
  @CsvSource(value = {
      "X110411675, kvnr, default, ''",
      "5-2-KH-AKA-1234567890, telematik-id, true, &include_inactive=true",
      "X110411675, kvnr, false, &include_inactive=false"
  }, nullValues = "default")
  void queriesUserChannels(
      String userId, String idType, Boolean includeInactive, String querySuffix) {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(
        builder, "http://tiger-proxy/notification-service-rs", "zeta");
    server.expect(once(), requestTo("http://tiger-proxy/notification-service-rs/users/"
            + userId + "/channels?id_type=" + idType + querySuffix))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("""
            {
              "user": {"id_type": "%s", "value": "%s"},
              "has_registered_devices": true,
              "registered_device_count": 2,
              "channels": [
                {"id": "zeta", "status": "enabled"},
                {"id": "inactive", "status": "disabled"},
                {"id": "default", "status": "not_set"}
              ]
            }
            """.formatted(idType, userId), MediaType.APPLICATION_JSON));

    var response = publisher.getChannels(userId, idType, includeInactive);

    assertThat(response.user().idType()).isEqualTo(idType);
    assertThat(response.user().value()).isEqualTo(userId);
    assertThat(response.hasRegisteredDevices()).isTrue();
    assertThat(response.registeredDeviceCount()).isEqualTo(2);
    assertThat(response.channels()).extracting(PushChannel::status)
        .containsExactly(PushChannelStatus.ENABLED, PushChannelStatus.DISABLED,
            PushChannelStatus.NOT_SET);
    server.verify();
  }

  /** Verifies user identifiers and identifier types cannot inject path or query components. */
  @Test
  void encodesChannelQueryValues() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(
        builder, "http://notification-service-rs", "zeta");
    server.expect(once(), requestTo("http://notification-service-rs/users/"
            + "user%2Fother%3Fx%3Dy/channels?id_type=kvnr%26include_inactive%3Dtrue"))
        .andRespond(withSuccess("""
            {"user":{"id_type":"kvnr","value":"X110411675"},
             "has_registered_devices":true,"channels":[]}
            """, MediaType.APPLICATION_JSON));

    var response = publisher.getChannels("user/other?x=y", "kvnr&include_inactive=true", null);

    assertThat(response.channels()).isEmpty();
    assertThat(response.registeredDeviceCount()).isNull();
    server.verify();
  }

  /**
   * Verifies downstream error responses are not replaced with successful channel responses.
   *
   * @param downstreamStatus error status returned by the Notification Service
   */
  @ParameterizedTest
  @CsvSource({"400", "404", "429", "500"})
  void propagatesChannelQueryFailures(int downstreamStatus) {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(
        builder, "http://notification-service-rs", "zeta");
    server.expect(once(), requestTo(
            "http://notification-service-rs/users/X110411675/channels?id_type=kvnr"))
        .andRespond(withStatus(HttpStatus.valueOf(downstreamStatus)));

    assertThatThrownBy(() -> publisher.getChannels("X110411675", "kvnr", null))
        .isInstanceOfSatisfying(RestClientResponseException.class,
            failure -> assertThat(failure.getStatusCode().value()).isEqualTo(downstreamStatus));
    server.verify();
  }

  /**
   * Verifies empty and malformed successful responses are treated as downstream failures.
   *
   * @param body invalid downstream response body
   */
  @ParameterizedTest
  @CsvSource({"''", "not-json"})
  void rejectsInvalidChannelResponses(String body) {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(
        builder, "http://notification-service-rs", "zeta");
    server.expect(once(), requestTo(
            "http://notification-service-rs/users/X110411675/channels?id_type=kvnr"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> publisher.getChannels("X110411675", "kvnr", null))
        .isInstanceOf(RestClientException.class);
    server.verify();
  }

  /** Verifies the real publisher forwards idempotency and reference values unchanged. */
  @Test
  void forwardsIdempotencyAndReference() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(builder, "http://notification-service-rs", "zeta");
    server.expect(once(), requestTo("http://notification-service-rs/notifications"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", "key-1"))
        .andExpect(content().json("""
            {"user":{"id_type":"kvnr","value":"X110411675"},"channel":"zeta",
             "payload":{"event_type":"registered_client_activity","correlation_id":"event-1",
             "title":"ZETA Notification","message":"Activity detected for a registered client."},
             "reference":"reference-1"}
            """))
        .andRespond(withSuccess("""
            {"notification_id":"notification-1","status":"accepted","channel":"zeta"}
            """, MediaType.APPLICATION_JSON));

    var response = publisher.publish(new NotificationEventRequest("X110411675",
        NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "event-1", "reference-1"), "key-1");

    assertThat(response.notificationId()).isEqualTo("notification-1");
    server.verify();
  }

  /** Verifies custom display fields survive JSON escaping and preserve event metadata. */
  @Test
  void forwardsCustomDisplayText() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var publisher = new NotificationEventPublisher(builder, "http://notification-service-rs", "zeta");
    server.expect(once(), requestTo("http://notification-service-rs/notifications"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("""
            {"user":{"id_type":"kvnr","value":"X110411675"},"channel":"zeta",
             "payload":{"event_type":"registered_client_activity","correlation_id":"event-2",
             "title":"ZETA Test","message":"Hello \\\"Android\\\"!\\nSecond line."}}
            """))
        .andRespond(withSuccess("""
            {"notification_id":"notification-2","status":"accepted","channel":"zeta"}
            """, MediaType.APPLICATION_JSON));

    publisher.publish(new NotificationEventRequest("X110411675",
        NotificationEventType.REGISTERED_CLIENT_ACTIVITY, "event-2", null,
        "ZETA Test", "Hello \"Android\"!\nSecond line."));

    server.verify();
  }
}
