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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Retains each subscription's client destination while the broker uses canonical routes. */
public class StompSubscriptionDestinations
    implements ChannelInterceptor, ApplicationListener<SessionDisconnectEvent> {

  private final Map<String, Map<String, SubscriptionAlias>> destinations =
      new ConcurrentHashMap<>();

  /** Records only rewritten subscriptions and removes terminated or replaced subscriptions. */
  public void track(StompHeaderAccessor accessor, String canonicalDestination) {
    String sessionId = accessor.getSessionId();
    if (sessionId == null) {
      return;
    }
    String subscriptionId = accessor.getSubscriptionId();
    if (accessor.getCommand() == StompCommand.DISCONNECT) {
      destinations.remove(sessionId);
    } else if (accessor.getCommand() == StompCommand.SUBSCRIBE
        && subscriptionId != null && accessor.getDestination() != null) {
      if (!accessor.getDestination().equals(canonicalDestination)) {
        destinations.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>())
            .put(subscriptionId,
                new SubscriptionAlias(accessor.getDestination(), canonicalDestination));
      } else {
        removeSubscription(sessionId, subscriptionId);
      }
    } else if (accessor.getCommand() == StompCommand.UNSUBSCRIBE && subscriptionId != null) {
      removeSubscription(sessionId, subscriptionId);
    }
  }

  /** Restores the destination selected by the recipient subscription, including user replies. */
  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    var accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
    if (accessor == null || accessor.getMessageType() != SimpMessageType.MESSAGE
        || accessor.getSessionId() == null || accessor.getSubscriptionId() == null) {
      return message;
    }
    var subscriptions = destinations.get(accessor.getSessionId());
    var alias = subscriptions == null ? null : subscriptions.get(accessor.getSubscriptionId());
    if (alias == null) {
      return message;
    }
    String userDestination = accessor.getFirstNativeHeader(
        SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
    String destination = userDestination != null ? userDestination : accessor.getDestination();
    if (destination == null) {
      return message;
    }
    var outgoing = SimpMessageHeaderAccessor.wrap(message);
    String restoredDestination = alias.restore(destination);
    outgoing.setDestination(restoredDestination);
    if (userDestination != null) {
      // Spring restores this header for user destinations after outbound interceptors.
      outgoing.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, restoredDestination);
    }
    return MessageBuilder.createMessage(message.getPayload(), outgoing.getMessageHeaders());
  }

  private void removeSubscription(String sessionId, String subscriptionId) {
    var subscriptions = destinations.get(sessionId);
    if (subscriptions != null) {
      subscriptions.remove(subscriptionId);
    }
  }

  /** Restores alias prefixes while keeping concrete destinations for wildcard subscriptions. */
  private record SubscriptionAlias(String original, String canonical) {

    private String restore(String destination) {
      int offset = 0;
      while (offset < canonical.length() && original.charAt(offset) == canonical.charAt(offset)) {
        offset++;
      }
      if (!destination.startsWith(canonical.substring(0, offset))) {
        return destination;
      }
      String prefix = original.substring(offset, offset + original.length() - canonical.length());
      return destination.substring(0, offset) + prefix + destination.substring(offset);
    }
  }

  /** Releases mappings also when a connection closes without a STOMP DISCONNECT frame. */
  @Override
  public void onApplicationEvent(@NonNull SessionDisconnectEvent event) {
    destinations.remove(event.getSessionId());
  }
}
