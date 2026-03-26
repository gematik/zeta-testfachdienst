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

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/** Unit tests for {@link StompFrameLoggingInterceptor}. */
class StompFrameLoggingInterceptorTest {

  private final StompFrameLoggingInterceptor interceptor = new StompFrameLoggingInterceptor();
  private final MessageChannel channel = mock(MessageChannel.class);

  @Test
  void preSendReturnsOriginalMessageWithoutAccessor() {
    Message<String> message = MessageBuilder.withPayload("payload").build();

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
  }

  @Test
  void preSendHandlesConnectFrame() {
    Message<byte[]> message = createMessage(StompCommand.CONNECT);

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
  }

  @Test
  void preSendHandlesSubscribeFrame() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionId("s1");
    accessor.setDestination("/topic/erezept");
    accessor.setSubscriptionId("sub-1");
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
  }

  @Test
  void preSendHandlesSendAndDefaultFrames() {
    Message<byte[]> send = createMessage(StompCommand.SEND);
    Message<byte[]> disconnect = createMessage(StompCommand.DISCONNECT);

    assertThat(interceptor.preSend(send, channel)).isSameAs(send);
    assertThat(interceptor.preSend(disconnect, channel)).isSameAs(disconnect);
  }

  /**
   * Create a simple STOMP message for the supplied command.
   *
   * @param command STOMP frame command to encode
   * @return message carrying the generated STOMP headers
   */
  private Message<byte[]> createMessage(StompCommand command) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setSessionId("s1");
    accessor.setDestination("/app/erezept");
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
