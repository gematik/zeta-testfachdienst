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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.HttpAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Unit tests for {@link ResourceServerTraceInterceptor}.
 */
class ResourceServerTraceInterceptorTest {

  /**
   * Verifies the interceptor creates a named resource server span with stable attributes.
   */
  @Test
  void requestLifecycleEmitsResourceServerSpan() throws Exception {
    CapturingSpanExporter exporter = new CapturingSpanExporter();
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build();
    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build();
    ResourceServerTraceInterceptor interceptor = new ResourceServerTraceInterceptor(openTelemetry);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hellozeta");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/hellozeta");
    MockHttpServletResponse response = new MockHttpServletResponse();
    Tracer testTracer = openTelemetry.getTracer("test");

    try {
      interceptor.preHandle(request, response, new Object());
      final Span resourceServerSpan = Span.current();
      Span childSpan = testTracer.spanBuilder("child").startSpan();
      childSpan.end();
      response.setStatus(200);
      interceptor.afterCompletion(request, response, new Object(), null);

      assertThat(Span.current().getSpanContext()).isNotEqualTo(resourceServerSpan.getSpanContext());
    } finally {
      tracerProvider.close();
    }

    assertThat(exporter.spans).hasSize(2);
    SpanData child = span(exporter.spans, "child");
    SpanData span = span(exporter.spans, ResourceServerTraceInterceptor.CONTEXTUAL_NAME);
    assertThat(span.getName()).isEqualTo(ResourceServerTraceInterceptor.CONTEXTUAL_NAME);
    assertThat(span.getKind()).isEqualTo(SpanKind.INTERNAL);
    assertThat(child.getParentSpanContext().getSpanId()).isEqualTo(span.getSpanId());
    assertThat(span.getAttributes().get(AttributeKey.stringKey("zeta.component")))
        .isEqualTo("resource server");
    assertThat(span.getAttributes().get(AttributeKey.stringKey("zeta.rs.name")))
        .isEqualTo("testfachdienst");
    assertThat(span.getAttributes().get(HttpAttributes.HTTP_REQUEST_METHOD))
        .isEqualTo("GET");
    assertThat(span.getAttributes().get(HttpAttributes.HTTP_ROUTE))
        .isEqualTo("/hellozeta");
    assertThat(span.getAttributes().get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE))
        .isEqualTo(200L);
  }

  /**
   * Find a captured span by name.
   *
   * @param spans exported spans
   * @param name  span name to find
   * @return matching span data
   */
  private static SpanData span(List<SpanData> spans, String name) {
    return spans.stream()
        .filter(span -> span.getName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Span exporter that records exported spans for assertions.
   */
  private static class CapturingSpanExporter implements SpanExporter {

    private final List<SpanData> spans = new ArrayList<>();

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      this.spans.addAll(spans);
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
      return CompletableResultCode.ofSuccess();
    }
  }
}
