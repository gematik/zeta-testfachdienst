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
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

/** Tests normalization of configured aliases in STOMP destinations. */
class StompDestinationAliasInterceptorTest {

  private final MessageChannel channel = mock(MessageChannel.class);

  @Test
  void normalizesApplicationBrokerAndUserDestinationsWithinTheContextPath() {
    var interceptor = new StompDestinationAliasInterceptor(
        new RouteAliases("/no-popp"), "/achelos_testfachdienst/");

    assertNormalized(interceptor,
        "/achelos_testfachdienst/no-popp/app/erezept.create",
        "/achelos_testfachdienst/app/erezept.create");
    assertNormalized(interceptor,
        "/achelos_testfachdienst/no-popp/topic/erezept",
        "/achelos_testfachdienst/topic/erezept");
    assertNormalized(interceptor,
        "/achelos_testfachdienst/no-popp/queue/erezept",
        "/achelos_testfachdienst/queue/erezept");
    assertNormalized(interceptor,
        "/achelos_testfachdienst/no-popp/user/queue/erezept",
        "/achelos_testfachdienst/user/queue/erezept");
  }

  @Test
  void normalizesAliasesWithoutContextPath() {
    var interceptor = new StompDestinationAliasInterceptor(new RouteAliases("/no-popp"), "");

    assertNormalized(interceptor, "/no-popp/app/hellozeta", "/app/hellozeta");
  }

  @Test
  void preservesCanonicalAndUnrelatedDestinations() {
    var interceptor = new StompDestinationAliasInterceptor(
        new RouteAliases("/no-popp"), "/achelos_testfachdienst");
    var canonical = message("/achelos_testfachdienst/app/erezept.list");
    var unrelated = message("/other/no-popp/app/erezept.list");

    assertThat(interceptor.preSend(canonical, channel)).isSameAs(canonical);
    assertThat(interceptor.preSend(unrelated, channel)).isSameAs(unrelated);
  }

  private void assertNormalized(
      StompDestinationAliasInterceptor interceptor, String alias, String canonical) {
    var result = interceptor.preSend(message(alias), channel);
    var accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

    assertThat(accessor).isNotNull();
    assertThat(accessor.getDestination()).isEqualTo(canonical);
    assertThat(accessor.getFirstNativeHeader(StompHeaderAccessor.STOMP_DESTINATION_HEADER))
        .isEqualTo(canonical);
  }

  private Message<byte[]> message(String destination) {
    var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionId("session-1");
    accessor.setSubscriptionId("subscription-1");
    accessor.setDestination(destination);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
