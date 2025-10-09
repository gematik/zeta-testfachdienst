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

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Spring configuration for enabling STOMP over WebSocket messaging.
 *
 * <p>This class activates Spring’s built-in WebSocket/STOMP support and
 * defines the application’s messaging topology:
 *
 * <ul>
 *   <li>Registers the STOMP handshake endpoint at {@code /ws}.
 *       <ul>
 *         <li>Clients must connect to {@code ws://.../ws} or {@code wss://.../ws}.</li>
 *         <li>{@link StompEndpointRegistry#withSockJS()} is enabled here to allow
 *             fallback to SockJS when raw WebSocket is unavailable (e.g. in older browsers
 *             or restrictive proxies).</li>
 *       </ul>
 *   </li>
 *   <li>Configures broker destinations:
 *       <ul>
 *         <li>{@code /app} is the <em>application prefix</em>:
 *             clients SEND messages here; they are routed to {@code @MessageMapping}
 *             methods in controllers like {@link ErezeptWsController}.</li>
 *         <li>{@code /topic} and {@code /queue} are the <em>simple broker prefixes</em>:
 *             clients SUBSCRIBE here to receive messages broadcast or directed
 *             by the application.</li>
 *         <li>{@code /user} is configured as the <em>user destination prefix</em>:
 *             messages sent to {@code /user/queue/...} are delivered only to the
 *             authenticated user who subscribed.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>In production, the simple in-memory broker can be replaced with a
 * <em>broker relay</em> (e.g. RabbitMQ, ActiveMQ) for scalability.
 *
 * <p>References:
 * <ul>
 *   <li><a href="https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html">
 *       Spring Framework Reference: STOMP over WebSocket</a></li>
 *   <li><a href="https://spring.io/guides/gs/messaging-stomp-websocket/">
 *       Spring Guide: Messaging with STOMP over WebSocket</a></li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

  /**
   * Register the STOMP endpoint used for client connections.
   *
   * <p>Clients initiate a connection to this endpoint to establish the
   * WebSocket (or SockJS) session.
   *
   * @param registry registry used to configure STOMP endpoints
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").withSockJS();
  }

  /**
   * Configure the message broker with prefixes for application and broker destinations.
   *
   * @param registry registry used to configure the message broker
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
  }
}
