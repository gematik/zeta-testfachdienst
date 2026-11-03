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

import de.gematik.zeta.testfachdienst.model.NotificationChannelsResponse;
import de.gematik.zeta.testfachdienst.model.NotificationEventRequest;
import de.gematik.zeta.testfachdienst.model.NotificationEventResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Publishes notification events and queries user channels through the Notification Service. */
@Service
public class NotificationEventPublisher {

  private final RestClient restClient;
  private final String channel;

  /**
   * Creates the publisher for the Notification Service resource-server API.
   *
   * @param builder auto-configured REST client builder
   * @param baseUrl Notification Service resource-server base URL
   * @param channel notification channel used by this resource server
   */
  @Autowired
  public NotificationEventPublisher(
      RestClient.Builder builder,
      @Value("${notification-service.rs.base-url}") String baseUrl,
      @Value("${notification-service.rs.channel}") String channel) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.channel = channel;
  }

  /**
   * Publishes one event and returns the Notification Service acceptance result.
   *
   * @param request deterministic test event
   * @return Notification Service response
   */
  public NotificationEventResponse publish(NotificationEventRequest request) {
    return publish(request, null);
  }

  /**
   * Publishes an event with an optional idempotency key and reference.
   *
   * @param request event and optional reference
   * @param idempotencyKey optional submission key forwarded to the Notification Service
   * @return acceptance result from the Notification Service
   */
  public NotificationEventResponse publish(
      NotificationEventRequest request, String idempotencyKey) {
    var notification = new LinkedHashMap<String, Object>(Map.of(
        "user", Map.of("id_type", "kvnr", "value", request.userId()),
        "channel", channel,
        "payload", request.payload()));
    if (request.reference() != null) {
      notification.put("reference", request.reference());
    }
    var submission = restClient.post()
        .uri(uriBuilder -> uriBuilder.pathSegment("notifications").build())
        .body(notification);
    if (idempotencyKey != null) {
      submission.header("Idempotency-Key", idempotencyKey);
    }
    return submission.retrieve().body(NotificationEventResponse.class);
  }

  /**
   * Queries the user's channels and device registration information from the Notification Service.
   *
   * @param userId user identifier value
   * @param idType identifier type, such as kvnr or telematik-id
   * @param includeInactive whether to include inactive channels; null retains the service default
   * @return channel response from the Notification Service
   * @throws RestClientException if the downstream request fails or returns an empty response
   */
  public NotificationChannelsResponse getChannels(
      String userId, String idType, Boolean includeInactive) {
    var response = restClient.get()
        .uri(uriBuilder -> uriBuilder.pathSegment("users", "{userId}", "channels")
            .queryParam("id_type", "{idType}")
            .queryParamIfPresent("include_inactive", Optional.ofNullable(includeInactive))
            .build(Map.of("userId", userId, "idType", idType)))
        .retrieve()
        .body(NotificationChannelsResponse.class);
    if (response == null) {
      throw new RestClientException("Empty Notification Service channel response");
    }
    return response;
  }
}
