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

import de.gematik.zeta.testfachdienst.model.PushChannel;
import de.gematik.zeta.testfachdienst.model.PushChannelStatus;
import de.gematik.zeta.testfachdienst.model.PushPusher;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PushNotificationService}.
 */
class PushNotificationServiceTest {

  /**
   * Verifies pusher creation and deletion by app id plus push key.
   */
  @Test
  void storesAndDeletesPusher() {
    var service = new PushNotificationService();
    var pusher = pusher("com.example.app.ios", "pushkey-1");

    service.setPusher(pusher);

    assertThat(service.listPushers()).containsExactly(pusher);

    service.setPusher(new PushPusher(
        "pushkey-1",
        null,
        "com.example.app.ios",
        null,
        null,
        null,
        null,
        null,
        null,
        null));

    assertThat(service.listPushers()).isEmpty();
  }

  /**
   * Verifies a new registration replaces an existing pusher for the same push key.
   */
  @Test
  void replacesExistingPusherForSamePushkey() {
    var service = new PushNotificationService();
    var replacementPusher = pusher("com.example.second", "pushkey-1");
    var enabledChannel = new PushChannel("erezept", PushChannelStatus.ENABLED);

    service.setPusher(pusher("com.example.first", "pushkey-1"));
    service.updateDeviceChannels("pushkey-1", List.of(enabledChannel));
    service.setPusher(replacementPusher);

    assertThat(service.listPushers()).containsExactly(replacementPusher);
    assertThat(service.listDeviceChannels("pushkey-1")).hasValue(List.of(enabledChannel));
  }

  /**
   * Verifies deregistering a pusher removes the pushkey channel state.
   */
  @Test
  void deletesDeviceChannelsWhenPusherIsDeregistered() {
    var service = new PushNotificationService();
    var enabledChannel = new PushChannel("erezept", PushChannelStatus.ENABLED);

    service.setPusher(pusher("com.example.app.ios", "pushkey-1"));
    service.updateDeviceChannels("pushkey-1", List.of(enabledChannel));

    service.setPusher(deletePusher("com.example.app.ios", "pushkey-1"));

    assertThat(service.listPushers()).isEmpty();
    assertThat(service.listDeviceChannels("pushkey-1")).isEmpty();
  }

  /**
   * Verifies device channel updates for a registered pusher.
   */
  @Test
  void updatesChannelsForRegisteredDevice() {
    var service = new PushNotificationService();
    service.setPusher(pusher("com.example.app.ios", "pushkey-1"));

    boolean updated = service.updateDeviceChannels(
        "pushkey-1",
        List.of(new PushChannel("erezept", PushChannelStatus.ENABLED)));

    assertThat(updated).isTrue();
    assertThat(service.listDeviceChannels("pushkey-1"))
        .hasValue(List.of(new PushChannel("erezept", PushChannelStatus.ENABLED)));
  }

  /**
   * Verifies unknown devices cannot be configured.
   */
  @Test
  void rejectsUnknownDeviceChannelUpdate() {
    var service = new PushNotificationService();

    boolean updated = service.updateDeviceChannels(
        "missing",
        List.of(new PushChannel("erezept", PushChannelStatus.ENABLED)));

    assertThat(updated).isFalse();
  }

  /**
   * Creates a valid test pusher.
   *
   * @param appId pusher app id
   * @param pushkey device push key
   * @return test pusher
   */
  private PushPusher pusher(String appId, String pushkey) {
    return new PushPusher(
        pushkey,
        "http",
        appId,
        "Test App",
        "Test Device",
        null,
        "de-DE",
        Map.of("url", "https://push.example/push/v1/"),
        null,
        false);
  }

  /**
   * Creates a pusher deletion request.
   *
   * @param appId pusher app id
   * @param pushkey device push key
   * @return pusher deletion request
   */
  private PushPusher deletePusher(String appId, String pushkey) {
    return new PushPusher(
        pushkey,
        null,
        appId,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
