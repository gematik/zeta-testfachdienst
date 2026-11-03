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

import de.gematik.zeta.testfachdienst.config.RouteAliases;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

/** Normalizes configured route aliases in inbound STOMP destinations. */
public class StompDestinationAliasInterceptor implements ChannelInterceptor {

  private final RouteAliases routeAliases;
  private final String contextPath;
  private final StompSubscriptionDestinations subscriptionDestinations;

  /** Creates an interceptor using the shared route aliases and servlet context path. */
  public StompDestinationAliasInterceptor(RouteAliases routeAliases, String contextPath) {
    this(routeAliases, contextPath, new StompSubscriptionDestinations());
  }

  /** Creates an interceptor sharing subscription destinations with the outbound channel. */
  public StompDestinationAliasInterceptor(RouteAliases routeAliases, String contextPath,
      StompSubscriptionDestinations subscriptionDestinations) {
    this.subscriptionDestinations = subscriptionDestinations;
    this.routeAliases = routeAliases;
    this.contextPath = normalizeContextPath(contextPath);
  }

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }
    String destination = accessor.getDestination();
    String canonicalDestination = destination == null ? null : canonicalize(destination);
    subscriptionDestinations.track(accessor, canonicalDestination);
    if (destination == null || destination.equals(canonicalDestination)) {
      return message;
    }

    var canonicalAccessor = StompHeaderAccessor.wrap(message);
    canonicalAccessor.setDestination(canonicalDestination);
    return MessageBuilder.createMessage(
        message.getPayload(), canonicalAccessor.getMessageHeaders());
  }

  private String canonicalize(String destination) {
    if (contextPath.isEmpty()) {
      return routeAliases.canonicalize(destination);
    }
    if (!destination.startsWith(contextPath + "/")) {
      return destination;
    }
    return contextPath + routeAliases.canonicalize(destination.substring(contextPath.length()));
  }

  private static String normalizeContextPath(String contextPath) {
    if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath.trim())) {
      return "";
    }
    String normalized = contextPath.trim();
    normalized = normalized.startsWith("/") ? normalized : "/" + normalized;
    return normalized.endsWith("/")
        ? normalized.substring(0, normalized.length() - 1)
        : normalized;
  }
}
