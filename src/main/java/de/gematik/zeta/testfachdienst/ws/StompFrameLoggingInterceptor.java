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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.testfachdienst.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Minimal STOMP frame logger to see CONNECT/SUBSCRIBE/SEND flow when diagnosing failed handshakes.
 */
@Slf4j
public class StompFrameLoggingInterceptor implements ChannelInterceptor {

  /**
   * Log the STOMP command of inbound frames before they are handled by the broker.
   *
   * @param message inbound STOMP frame
   * @param channel channel receiving the frame
   * @return original message to continue processing
   */
  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null && accessor.getCommand() != null) {
      logCommand(accessor);
    }
    return message;
  }

  /**
   * Emit a concise log entry for the given STOMP command and headers.
   *
   * @param accessor header accessor containing command and routing details
   */
  private void logCommand(@NonNull StompHeaderAccessor accessor) {
    StompCommand command = accessor.getCommand();
    if (command == null) {
      return;
    }
    switch (command) {
      case CONNECT, STOMP -> log.info("STOMP {} session={} host={} accept-version={}",
          command, accessor.getSessionId(), accessor.getHost(), accessor.getAcceptVersion());
      case SUBSCRIBE -> log.info("STOMP SUBSCRIBE session={} destination={} id={}",
          accessor.getSessionId(), accessor.getDestination(), accessor.getSubscriptionId());
      case SEND -> log.info("STOMP SEND session={} destination={}", accessor.getSessionId(), accessor.getDestination());
      default -> log.info("STOMP {} session={} destination={}", command, accessor.getSessionId(), accessor.getDestination());
    }
  }
}
