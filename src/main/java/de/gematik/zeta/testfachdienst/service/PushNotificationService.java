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

import de.gematik.zeta.testfachdienst.model.PushChannel;
import de.gematik.zeta.testfachdienst.model.PushChannelStatus;
import de.gematik.zeta.testfachdienst.model.PushPusher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * In-memory service for gemF PushNotification pusher and channel state.
 *
 * <p>Implements pusher registration lifecycle behavior from A_27154, A_27155, A_27156,
 * A_27193-02, and A_27197-01 for the Testfachdienst.
 */
@Service
public class PushNotificationService {

  private static final List<String> DEFAULT_CHANNEL_IDS = List.of("erezept");

  private final Map<PusherKey, PushPusher> pushers = new LinkedHashMap<>();
  private final Map<String, Map<String, PushChannelStatus>> deviceChannels = new LinkedHashMap<>();

  /**
   * Lists all currently registered pushers for A_27104.
   *
   * @return registered pushers in insertion order
   */
  public synchronized List<PushPusher> listPushers() {
    return List.copyOf(pushers.values());
  }

  /**
   * Creates, updates, or deletes a pusher according to A_27154, A_27155, A_27156, A_27193-02, and
   * A_27197-01.
   *
   * @param pusher pusher request
   */
  public synchronized void setPusher(PushPusher pusher) {
    var key = new PusherKey(pusher.appId(), pusher.pushkey());
    if (pusher.kind() == null) {
      pushers.remove(key);
      deviceChannels.remove(pusher.pushkey());
      return;
    }
    removePushersWithPushkeyExcept(pusher.pushkey(), key);
    pushers.put(key, pusher);
    deviceChannels.computeIfAbsent(pusher.pushkey(), ignored -> defaultChannelMap());
  }

  /**
   * Lists default channels available to push notification clients according to A_27190.
   *
   * @return default channel states
   */
  public synchronized List<PushChannel> listAvailableChannels() {
    return DEFAULT_CHANNEL_IDS.stream()
        .map(channelId -> new PushChannel(channelId, PushChannelStatus.NOT_SET))
        .toList();
  }

  /**
   * Lists channel states for a registered device according to A_27190.
   *
   * @param pushkey device push key
   * @return channel states when the app registration exists
   */
  public synchronized Optional<List<PushChannel>> listDeviceChannels(String pushkey) {
    return Optional.ofNullable(deviceChannels.get(pushkey)).map(this::toChannels);
  }

  /**
   * Updates channel states for a registered device according to A_27190.
   *
   * @param pushkey device push key
   * @param channels channel states to store
   * @return {@code true} when the app registration exists and channels were updated
   */
  public synchronized boolean updateDeviceChannels(String pushkey, List<PushChannel> channels) {
    Map<String, PushChannelStatus> existing = deviceChannels.get(pushkey);
    if (existing == null) {
      return false;
    }
    for (PushChannel channel : channels) {
      existing.put(channel.id(), channel.status());
    }
    return true;
  }

  /**
   * Creates the default channel map for A_27193-02.
   *
   * @return default channel state keyed by channel id
   */
  private Map<String, PushChannelStatus> defaultChannelMap() {
    var channels = new LinkedHashMap<String, PushChannelStatus>();
    for (String channelId : DEFAULT_CHANNEL_IDS) {
      channels.put(channelId, PushChannelStatus.NOT_SET);
    }
    return channels;
  }

  /**
   * Converts a channel map to response objects.
   *
   * @param channelMap channel status map
   * @return response channel list
   */
  private List<PushChannel> toChannels(Map<String, PushChannelStatus> channelMap) {
    var channels = new ArrayList<PushChannel>();
    for (Map.Entry<String, PushChannelStatus> entry : channelMap.entrySet()) {
      channels.add(new PushChannel(entry.getKey(), entry.getValue()));
    }
    return channels;
  }

  /**
   * Removes stale pusher registrations before storing a new registration for the same push key.
   *
   * @param pushkey device push key
   * @param key pusher key that should remain eligible for update
   */
  private void removePushersWithPushkeyExcept(String pushkey, PusherKey key) {
    pushers.keySet().removeIf(existingKey ->
        existingKey.pushkey().equals(pushkey) && !existingKey.equals(key));
  }

  /**
   * Identity key for pusher registrations in A_27154, A_27155, and A_27156.
   *
   * @param appId pusher app id
   * @param pushkey device push key
   */
  private record PusherKey(String appId, String pushkey) {}
}
