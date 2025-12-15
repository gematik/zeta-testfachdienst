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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Spring configuration for enabling STOMP over WebSocket messaging.
 *
 * <p>This class activates Spring's built-in WebSocket/STOMP support and
 * defines the application's messaging topology:
 *
 * <ul>
 *   <li>Registers the STOMP handshake endpoint at {@code /ws}.
 *       <ul>
 *         <li>Clients must connect to {@code ws://.../ws} or {@code wss://.../ws}.</li>
 *         <li>Uses native WebSocket protocol (no SockJS fallback).</li>
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
@SuppressWarnings("unused") // managed by Spring component scanning
@Slf4j
public class StompConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${server.servlet.context-path:}")
  private String contextPath;

  /**
   * Register the STOMP endpoint used for client connections.
   *
   * <p>Clients initiate a connection to this endpoint to establish the
   * native WebSocket session.
   *
   * @param registry registry used to configure STOMP endpoints
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .addInterceptors(new LoggingHandshakeInterceptor());
  }

  /**
   * Register interceptors for inbound client frames to aid troubleshooting.
   *
   * @param registration inbound channel registration
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new StompFrameLoggingInterceptor());
  }

  /**
   * Decorate WebSocket handlers with lifecycle logging for connection open/close events.
   *
   * @param registration transport registration
   */
  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(new WebSocketLifecycleLoggingDecoratorFactory());
  }

  /**
   * Configure the message broker with prefixes for application and broker destinations.
   *
   * @param registry registry used to configure the message broker
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker(resolveBrokerPrefixes());
    registry.setApplicationDestinationPrefixes(resolveApplicationPrefixes());
    registry.setUserDestinationPrefix(resolveUserPrefix());
  }

  /**
   * Determine the broker destination prefixes, optionally prepending the servlet context path.
   *
   * @return array of broker prefixes (topic/queue variants)
   */
  private String[] resolveBrokerPrefixes() {
    if (isContextPathBlank()) {
      return new String[]{"/topic", "/queue"};
    }
    return new String[]{withContextPath("/topic"), "/queue", withContextPath("/queue")};
  }

  /**
   * Determine application destination prefixes for incoming client SEND frames.
   *
   * @return array containing the application prefix, context-aware when needed
   */
  private String[] resolveApplicationPrefixes() {
    return isContextPathBlank()
        ? new String[]{"/app"}
        : new String[]{withContextPath("/app")};
  }

  /**
   * Build the user destination prefix used for private replies.
   *
   * @return user prefix with optional context path
   */
  private String resolveUserPrefix() {
    return isContextPathBlank() ? "/user" : withContextPath("/user");
  }

  /**
   * Check whether a servlet context path is configured.
   *
   * @return {@code true} when the context path is null or blank
   */
  private boolean isContextPathBlank() {
    return contextPath == null || contextPath.isBlank();
  }

  /**
   * Prefix the given destination with the servlet context path, normalizing slashes.
   *
   * @param destination broker or application destination
   * @return destination prefixed with context path when present
   */
  private String withContextPath(String destination) {
    if (contextPath == null || contextPath.isBlank()) {
      return destination;
    }

    String normalizedContext = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    if (normalizedContext.endsWith("/")) {
      normalizedContext = normalizedContext.substring(0, normalizedContext.length() - 1);
    }
    return normalizedContext + destination;
  }
}
