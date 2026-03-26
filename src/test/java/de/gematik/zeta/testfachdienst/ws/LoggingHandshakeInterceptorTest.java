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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

/** Unit tests for {@link LoggingHandshakeInterceptor}. */
class LoggingHandshakeInterceptorTest {

  private final LoggingHandshakeInterceptor interceptor = new LoggingHandshakeInterceptor();

  @Test
  void beforeHandshakeAlwaysAllowsUpgrade() throws Exception {
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Host", "example.test");
    headers.add("Origin", "https://origin.test");
    org.mockito.Mockito.when(request.getURI()).thenReturn(URI.create("ws://localhost/ws"));
    org.mockito.Mockito.when(request.getRemoteAddress())
        .thenReturn(new InetSocketAddress("127.0.0.1", 8080));
    org.mockito.Mockito.when(request.getHeaders()).thenReturn(headers);

    boolean allowed =
        interceptor.beforeHandshake(
            request,
            mock(ServerHttpResponse.class),
            mock(WebSocketHandler.class),
            new HashMap<>());

    assertThat(allowed).isTrue();
  }

  @Test
  void afterHandshakeHandlesServletResponsesAndExceptions() {
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    org.mockito.Mockito.when(request.getURI()).thenReturn(URI.create("ws://localhost/ws"));
    MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    servletResponse.setStatus(101);

    interceptor.afterHandshake(
        request,
        new ServletServerHttpResponse(servletResponse),
        mock(WebSocketHandler.class),
        null);

    interceptor.afterHandshake(
        request,
        new ServletServerHttpResponse(servletResponse),
        mock(WebSocketHandler.class),
        new IllegalStateException("boom"));
  }

  @Test
  void afterHandshakeAlsoAcceptsNonServletResponses() {
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    org.mockito.Mockito.when(request.getURI()).thenReturn(URI.create("ws://localhost/ws"));
    ServerHttpResponse response = mock(ServerHttpResponse.class);

    interceptor.afterHandshake(request, response, mock(WebSocketHandler.class), null);
  }
}
