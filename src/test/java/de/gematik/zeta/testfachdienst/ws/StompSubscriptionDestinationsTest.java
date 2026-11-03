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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.gematik.zeta.testfachdienst.config.RouteAliases;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Verifies that canonical broker routing preserves each client's subscription destination. */
class StompSubscriptionDestinationsTest {

  private static final String CANONICAL = "/achelos_testfachdienst/user/queue/erezept";
  private static final String ALIAS = "/achelos_testfachdienst/no-popp/user/queue/erezept";
  private final MessageChannel channel = mock(MessageChannel.class);
  private final StompSubscriptionDestinations destinations = new StompSubscriptionDestinations();
  private final StompDestinationAliasInterceptor inbound = new StompDestinationAliasInterceptor(
      new RouteAliases("/no-popp"), "/achelos_testfachdienst", destinations);

  @Test
  void restoresAliasAndSpringOriginalDestinationForPrivateReply() {
    var subscribe = frame(StompCommand.SUBSCRIBE, "session-1", "sub-1", ALIAS);
    var normalized = inbound.preSend(subscribe, channel);
    assertThat(headers(normalized).getDestination()).isEqualTo(CANONICAL);

    var reply = reply("session-1", "sub-1");
    var restored = destinations.preSend(reply, channel);
    assertThat(headers(restored).getDestination()).isEqualTo(ALIAS);
    assertThat(headers(restored).getFirstNativeHeader(
        SimpMessageHeaderAccessor.ORIGINAL_DESTINATION)).isEqualTo(ALIAS);
  }

  @Test
  void isolatesSessionsAndSubscriptionsIncludingCanonicalSubscribers() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "same", ALIAS), channel);
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "b", "same", CANONICAL), channel);
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "other", CANONICAL), channel);

    assertThat(headers(destinations.preSend(reply("a", "same"), channel)).getDestination())
        .isEqualTo(ALIAS);
    var canonicalReply = reply("b", "same");
    var otherReply = reply("a", "other");
    assertThat(destinations.preSend(canonicalReply, channel)).isSameAs(canonicalReply);
    assertThat(destinations.preSend(otherReply, channel)).isSameAs(otherReply);
  }

  @Test
  void unsubscribeRemovesOnlyTheSelectedSubscription() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one", ALIAS), channel);
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "two", ALIAS), channel);
    inbound.preSend(frame(StompCommand.UNSUBSCRIBE, "a", "one", null), channel);
    var reply = reply("a", "one");

    assertThat(destinations.preSend(reply, channel)).isSameAs(reply);
    assertThat(headers(destinations.preSend(reply("a", "two"), channel)).getDestination())
        .isEqualTo(ALIAS);
  }

  @Test
  void disconnectFrameAndUnexpectedTransportClosureReleaseSessionMappings() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one", ALIAS), channel);
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "b", "one", ALIAS), channel);
    inbound.preSend(frame(StompCommand.DISCONNECT, "a", null, null), channel);
    var firstReply = reply("a", "one");
    assertThat(destinations.preSend(firstReply, channel)).isSameAs(firstReply);

    destinations.onApplicationEvent(new SessionDisconnectEvent(
        this, frame(StompCommand.DISCONNECT, "b", null, null), "b", CloseStatus.NORMAL));
    var secondReply = reply("b", "one");
    assertThat(destinations.preSend(secondReply, channel)).isSameAs(secondReply);
  }

  @Test
  void replacingSubscriptionDoesNotRetainPreviousAlias() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one", ALIAS), channel);
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one", CANONICAL), channel);
    var reply = reply("a", "one");
    assertThat(destinations.preSend(reply, channel)).isSameAs(reply);
  }

  @Test
  void canonicalWildcardSubscriptionPreservesConcreteBroadcastDestination() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one",
        "/achelos_testfachdienst/topic/*"), channel);
    var broadcast = broadcast("a", "one", "/achelos_testfachdienst/topic/erezept");

    assertThat(destinations.preSend(broadcast, channel)).isSameAs(broadcast);
    assertThat(headers(broadcast).getDestination())
        .isEqualTo("/achelos_testfachdienst/topic/erezept");
    assertThat(headers(broadcast).getFirstNativeHeader(
        SimpMessageHeaderAccessor.ORIGINAL_DESTINATION)).isNull();
  }

  @Test
  void aliasedBroadcastRestoresPrefixWithoutAddingUserDestinationHeader() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one",
        "/achelos_testfachdienst/no-popp/topic/erezept"), channel);
    var restored = destinations.preSend(
        broadcast("a", "one", "/achelos_testfachdienst/topic/erezept"), channel);

    assertThat(headers(restored).getDestination())
        .isEqualTo("/achelos_testfachdienst/no-popp/topic/erezept");
    assertThat(headers(restored).getFirstNativeHeader(
        SimpMessageHeaderAccessor.ORIGINAL_DESTINATION)).isNull();
  }

  @Test
  void aliasedWildcardSubscriptionPreservesConcreteBroadcastDestination() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one",
        "/achelos_testfachdienst/no-popp/topic/**"), channel);
    var restored = destinations.preSend(
        broadcast("a", "one", "/achelos_testfachdienst/topic/erezept/created"), channel);

    assertThat(headers(restored).getDestination())
        .isEqualTo("/achelos_testfachdienst/no-popp/topic/erezept/created");
    assertThat(headers(restored).getFirstNativeHeader(
        SimpMessageHeaderAccessor.ORIGINAL_DESTINATION)).isNull();
  }

  @Test
  void canonicalBroadcastSubscriptionPreservesExistingMessageHeaders() {
    inbound.preSend(frame(StompCommand.SUBSCRIBE, "a", "one",
        "/achelos_testfachdienst/topic/erezept"), channel);
    var broadcast = broadcast("a", "one", "/achelos_testfachdienst/topic/erezept");

    assertThat(destinations.preSend(broadcast, channel)).isSameAs(broadcast);
  }

  private Message<byte[]> broadcast(String session, String subscription, String destination) {
    var accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setSessionId(session);
    accessor.setSubscriptionId(subscription);
    accessor.setDestination(destination);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> frame(StompCommand command, String session, String subscription,
      String destination) {
    var accessor = StompHeaderAccessor.create(command);
    accessor.setSessionId(session);
    accessor.setSubscriptionId(subscription);
    accessor.setDestination(destination);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> reply(String session, String subscription) {
    var accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setSessionId(session);
    accessor.setSubscriptionId(subscription);
    accessor.setDestination("/queue/erezept-user" + session);
    accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, CANONICAL);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private SimpMessageHeaderAccessor headers(Message<?> message) {
    return MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
  }
}
