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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Logs lifecycle events (open/close) of WebSocket sessions so we can see if STOMP connects at all.
 */
@Slf4j
public class WebSocketLifecycleLoggingDecoratorFactory implements WebSocketHandlerDecoratorFactory {

  /**
   * Wrap the given handler with a decorator that logs connection open/close events.
   *
   * @param handler delegate WebSocket handler
   * @return handler that emits lifecycle logs while delegating to the original handler
   */
  @Override
  public @NonNull WebSocketHandler decorate(@NonNull WebSocketHandler handler) {
    return new WebSocketHandlerDecorator(handler) {
      /**
       * Log session establishment details before delegating to the underlying handler.
       *
       * @param session established WebSocket session
       * @throws Exception if the delegate handler raises an error
       */
      @Override
      public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("WS session established id={} principal={} protocol={}", session.getId(), session.getPrincipal(), session.getAcceptedProtocol());
        super.afterConnectionEstablished(session);
      }

      /**
       * Log session closure details before delegating to the underlying handler.
       *
       * @param session     closing WebSocket session
       * @param closeStatus close status code and reason
       * @throws Exception if the delegate handler raises an error
       */
      @Override
      public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus) throws Exception {
        log.info("WS session closed id={} code={} reason={}", session.getId(), closeStatus.getCode(), closeStatus.getReason());
        super.afterConnectionClosed(session, closeStatus);
      }
    };
  }
}
