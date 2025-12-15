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

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Logs incoming WebSocket handshake attempts with key headers to ease debugging of ingress issues.
 */
@Slf4j
public class LoggingHandshakeInterceptor implements HandshakeInterceptor {

  /**
   * Log key headers of the incoming handshake request to aid debugging.
   *
   * @param request    inbound HTTP request starting the WebSocket upgrade
   * @param response   outbound HTTP response for the handshake
   * @param wsHandler  target WebSocket handler
   * @param attributes attributes that may be populated for the resulting session
   * @return {@code true} to allow the handshake to proceed
   */
  @Override
  public boolean beforeHandshake(
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response,
      @NonNull WebSocketHandler wsHandler,
      @NonNull Map<String, Object> attributes) {

    log.info(
        "WS handshake start uri={} remote={} host={} x-forwarded-for={} proto={} origin={} subprotocol={} extensions={} version={}",
        request.getURI(),
        request.getRemoteAddress(),
        request.getHeaders().getFirst("Host"),
        request.getHeaders().getFirst("X-Forwarded-For"),
        request.getHeaders().getFirst("X-Forwarded-Proto"),
        request.getHeaders().getFirst("Origin"),
        request.getHeaders().getFirst("Sec-WebSocket-Protocol"),
        request.getHeaders().getFirst("Sec-WebSocket-Extensions"),
        request.getHeaders().getFirst("Sec-WebSocket-Version"));
    return true;
  }

  /**
   * Log the outcome of a completed handshake, including HTTP status and errors if present.
   *
   * @param request   inbound HTTP request starting the WebSocket upgrade
   * @param response  HTTP response produced by the handshake
   * @param wsHandler target WebSocket handler
   * @param exception optional exception thrown during the handshake
   */
  @Override
  public void afterHandshake(
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response,
      @NonNull WebSocketHandler wsHandler,
      @Nullable Exception exception) {
    // getStatusCode is only on ServletServerHttpResponse; fall back gracefully
    var status =
        response instanceof org.springframework.http.server.ServletServerHttpResponse servletResp
            ? servletResp.getServletResponse().getStatus()
            : -1;
    if (exception != null) {
      log.warn("WS handshake failed uri={} status={} error={}", request.getURI(), status,
          exception.getMessage(), exception);
      return;
    }
    log.info("WS handshake success uri={} status={}", request.getURI(), status);
  }
}
