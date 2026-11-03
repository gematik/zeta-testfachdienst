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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.config.RouteAliases;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

/**
 * Unit tests for {@link StompConfig} verifying the handling of servlet context paths.
 */
class StompConfigTest {

  @Test
  void registersOriginalAndConfiguredHandshakeAliases() {
    var config = new StompConfig(new RouteAliases("/no-popp,/other"));
    var registry = mock(StompEndpointRegistry.class);
    var registration = mock(StompWebSocketEndpointRegistration.class);
    when(registry.addEndpoint("/ws", "/ws-no-popp", "/no-popp/ws", "/no-popp/ws-no-popp",
        "/other/ws", "/other/ws-no-popp")).thenReturn(registration);
    when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

    config.registerStompEndpoints(registry);

    verify(registry).addEndpoint("/ws", "/ws-no-popp", "/no-popp/ws", "/no-popp/ws-no-popp",
        "/other/ws", "/other/ws-no-popp");
  }

  @Test
  void registersWebSocketHandshakeEndpoints() {
    var config = new StompConfig(new RouteAliases(""));
    var registry = mock(StompEndpointRegistry.class);
    var registration = mock(StompWebSocketEndpointRegistration.class);
    when(registry.addEndpoint("/ws", "/ws-no-popp"))
        .thenReturn(registration);
    when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

    config.registerStompEndpoints(registry);

    verify(registry).addEndpoint("/ws", "/ws-no-popp");
  }

  @Test
  void usesPlainPrefixesWhenNoContextPathIsSet() {
    var config = new StompConfig(new RouteAliases(""));
    ReflectionTestUtils.setField(config, "contextPath", "");

    var brokerPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveBrokerPrefixes");
    var appPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveApplicationPrefixes");
    var userPrefix = (String) ReflectionTestUtils.invokeMethod(config, "resolveUserPrefix");

    assertThat(brokerPrefixes).containsExactlyInAnyOrder("/topic", "/queue");
    assertThat(appPrefixes).containsExactly("/app");
    assertThat(userPrefix).isEqualTo("/user");
  }

  @Test
  void usesOnlyContextPrefixedDestinationsWhenContextPathIsSet() {
    var config = new StompConfig(new RouteAliases(""));
    ReflectionTestUtils.setField(config, "contextPath", "/achelos_testfachdienst");

    var brokerPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveBrokerPrefixes");
    var appPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveApplicationPrefixes");
    var userPrefix = (String) ReflectionTestUtils.invokeMethod(config, "resolveUserPrefix");

    assertThat(brokerPrefixes)
        .containsExactlyInAnyOrder(
            "/achelos_testfachdienst/topic", "/achelos_testfachdienst/queue", "/queue");
    assertThat(appPrefixes).containsExactly("/achelos_testfachdienst/app");
    assertThat(userPrefix).isEqualTo("/achelos_testfachdienst/user");
  }

  @Test
  void appliesConfiguredAliasesToApplicationAndBrokerPrefixes() {
    var config = new StompConfig(new RouteAliases("/no-popp,/other"));
    ReflectionTestUtils.setField(config, "contextPath", "/achelos_testfachdienst");

    var brokerPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveBrokerPrefixes");
    var appPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveApplicationPrefixes");

    assertThat(brokerPrefixes).containsExactlyInAnyOrder(
        "/queue",
        "/achelos_testfachdienst/topic",
        "/achelos_testfachdienst/queue",
        "/achelos_testfachdienst/no-popp/topic",
        "/achelos_testfachdienst/no-popp/queue",
        "/achelos_testfachdienst/other/topic",
        "/achelos_testfachdienst/other/queue");
    assertThat(appPrefixes).containsExactly(
        "/achelos_testfachdienst/app",
        "/achelos_testfachdienst/no-popp/app",
        "/achelos_testfachdienst/other/app");
  }

  @Test
  void normalizesContextPathWithoutLeadingSlashAndWithTrailingSlash() {
    var config = new StompConfig(new RouteAliases(""));
    ReflectionTestUtils.setField(config, "contextPath", "achelos_testfachdienst/");

    var brokerPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveBrokerPrefixes");
    var appPrefixes =
        (String[]) ReflectionTestUtils.invokeMethod(config, "resolveApplicationPrefixes");
    var userPrefix = (String) ReflectionTestUtils.invokeMethod(config, "resolveUserPrefix");

    assertThat(brokerPrefixes)
        .containsExactlyInAnyOrder(
            "/achelos_testfachdienst/topic", "/achelos_testfachdienst/queue", "/queue");
    assertThat(appPrefixes).containsExactly("/achelos_testfachdienst/app");
    assertThat(userPrefix).isEqualTo("/achelos_testfachdienst/user");
  }

  @Test
  void exposesConfiguredHeartbeatIntervals() {
    var config = new StompConfig(new RouteAliases(""));
    ReflectionTestUtils.setField(config, "serverHeartbeatMs", 2_500L);
    ReflectionTestUtils.setField(config, "clientHeartbeatMs", 4_000L);

    var heartbeatValues =
        (long[]) ReflectionTestUtils.invokeMethod(config, "resolveHeartbeatValues");

    assertThat(heartbeatValues).containsExactly(2_500L, 4_000L);
  }

  @Test
  void keepsTimeToFirstMessageDefaultAtSixtySeconds() throws NoSuchFieldException {
    Field field = StompConfig.class.getDeclaredField("timeToFirstMessageMs");
    Value valueAnnotation = field.getAnnotation(Value.class);

    assertThat(valueAnnotation).isNotNull();
    assertThat(valueAnnotation.value())
        .isEqualTo("${app.websocket.transport.time-to-first-message-ms:60000}");
  }
}
