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

package de.gematik.zeta.testfachdienst.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.HttpAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Emits a dedicated resource server span for Testfachdienst request handling.
 */
public class ResourceServerTraceInterceptor implements HandlerInterceptor {

  public static final String CONTEXTUAL_NAME = "RS Trace Span";

  private static final String SPAN_ATTRIBUTE =
      ResourceServerTraceInterceptor.class.getName() + ".span";
  private static final String SCOPE_ATTRIBUTE =
      ResourceServerTraceInterceptor.class.getName() + ".scope";

  private final Tracer tracer;

  /**
   * Create an interceptor that records resource server spans.
   *
   * @param openTelemetry OpenTelemetry instance used to create spans
   */
  public ResourceServerTraceInterceptor(OpenTelemetry openTelemetry) {
    this.tracer = openTelemetry.getTracer(ResourceServerTraceInterceptor.class.getName());
  }

  /**
   * Start the resource server span before the selected controller handles the request.
   *
   * @param request  current HTTP request
   * @param response current HTTP response
   * @param handler  selected handler
   * @return always {@code true} so request processing continues
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
    Span span = tracer.spanBuilder(CONTEXTUAL_NAME)
        .setSpanKind(SpanKind.INTERNAL)
        .setAttribute("zeta.component", "resource server")
        .setAttribute("zeta.rs.name", "testfachdienst")
        .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, request.getMethod())
        .setAttribute(HttpAttributes.HTTP_ROUTE, route(request))
        .startSpan();
    Scope scope = span.makeCurrent();
    request.setAttribute(SPAN_ATTRIBUTE, span);
    request.setAttribute(SCOPE_ATTRIBUTE, scope);
    return true;
  }

  /**
   * Complete the resource server span after request handling finished.
   *
   * @param request  current HTTP request
   * @param response current HTTP response
   * @param handler  selected handler
   * @param ex       exception raised by request handling, if any
   */
  @Override
  public void afterCompletion(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex) {
    Object attribute = request.getAttribute(SPAN_ATTRIBUTE);
    if (!(attribute instanceof Span span)) {
      return;
    }
    try {
      span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, response.getStatus());
      if (ex != null) {
        span.recordException(ex);
        span.setStatus(StatusCode.ERROR);
      }
    } finally {
      try {
        closeScope(request);
      } finally {
        span.end();
      }
    }
  }

  /**
   * Close and clear the request-bound OpenTelemetry scope.
   *
   * @param request current HTTP request
   */
  private void closeScope(HttpServletRequest request) {
    Object scope = request.getAttribute(SCOPE_ATTRIBUTE);
    request.removeAttribute(SCOPE_ATTRIBUTE);
    request.removeAttribute(SPAN_ATTRIBUTE);
    if (scope instanceof Scope activeScope) {
      activeScope.close();
    }
  }

  /**
   * Resolve the best available HTTP route for observation tagging.
   *
   * @param request current HTTP request
   * @return matched route pattern or request URI when no pattern is available
   */
  private String route(HttpServletRequest request) {
    Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    if (route instanceof String pattern && !pattern.isBlank()) {
      return pattern;
    }
    return request.getRequestURI();
  }
}
