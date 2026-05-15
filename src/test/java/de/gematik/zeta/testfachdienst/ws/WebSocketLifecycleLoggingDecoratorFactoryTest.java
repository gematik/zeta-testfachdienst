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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

/** Unit tests for {@link WebSocketLifecycleLoggingDecoratorFactory}. */
class WebSocketLifecycleLoggingDecoratorFactoryTest {

  private final WebSocketLifecycleLoggingDecoratorFactory factory =
      new WebSocketLifecycleLoggingDecoratorFactory();

  @Test
  void decoratorDelegatesLifecycleCallbacks() throws Exception {
    WebSocketHandler delegate = org.mockito.Mockito.mock(WebSocketHandler.class);
    WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
    when(session.getId()).thenReturn("s1");
    when(session.getAcceptedProtocol()).thenReturn("v12.stomp");

    WebSocketHandler decorated = factory.decorate(delegate);
    decorated.afterConnectionEstablished(session);
    decorated.afterConnectionClosed(session, CloseStatus.NORMAL);

    verify(delegate).afterConnectionEstablished(session);
    verify(delegate).afterConnectionClosed(session, CloseStatus.NORMAL);
  }

  @Test
  void decoratorDelegatesTransportErrors() throws Exception {
    WebSocketHandler delegate = org.mockito.Mockito.mock(WebSocketHandler.class);
    WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
    when(session.getId()).thenReturn("s1");
    Throwable error = new IllegalStateException("boom");

    WebSocketHandler decorated = factory.decorate(delegate);
    decorated.handleTransportError(session, error);

    verify(delegate).handleTransportError(session, error);
  }
}
