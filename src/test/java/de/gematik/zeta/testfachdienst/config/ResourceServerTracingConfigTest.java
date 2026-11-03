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

package de.gematik.zeta.testfachdienst.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

/** Regression tests for tracing controller aliases without tracing infrastructure routes. */
class ResourceServerTracingConfigTest {

  @ParameterizedTest
  @ValueSource(strings = {"", "/achelos_testfachdienst"})
  void tracesOriginalAndConfiguredControllerAliases(String contextPath) {
    var interceptor = registeredInterceptor("/no-popp,/custom/v2");

    for (String prefix : new String[]{"", "/no-popp", "/custom/v2"}) {
      for (String path : new String[]{"/hellozeta", "/api/erezept", "/jobs/info",
          "/push/v1/pushers", "/test-support/notifications"}) {
        assertThat(interceptor.matches(request(contextPath, prefix + path)))
            .as("tracing %s%s%s", contextPath, prefix, path).isTrue();
      }
    }
    for (String path : new String[]{"/actuator/health", "/v3/api-docs", "/swagger-ui/index.html",
        "/h2-console", "/no-popp/v3/api-docs"}) {
      assertThat(interceptor.matches(request(contextPath, path))).as(path).isFalse();
    }
  }

  @Test
  void disablingAliasesTracesOnlyOriginalControllerRoutes() {
    var interceptor = registeredInterceptor("");

    assertThat(interceptor.matches(request("", "/api/erezept"))).isTrue();
    assertThat(interceptor.matches(request("", "/no-popp/api/erezept"))).isFalse();
  }

  private MappedInterceptor registeredInterceptor(String prefixes) {
    var registry = new InterceptorRegistry();
    new ResourceServerTracingConfig(OpenTelemetry.noop(), new RouteAliases(prefixes))
        .addInterceptors(registry);
    List<Object> interceptors = ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
    assertThat(interceptors).singleElement().isInstanceOf(MappedInterceptor.class);
    return (MappedInterceptor) interceptors.getFirst();
  }

  private MockHttpServletRequest request(String contextPath, String path) {
    var request = new MockHttpServletRequest("GET", contextPath + path);
    request.setContextPath(contextPath);
    ServletRequestPathUtils.parseAndCache(request);
    return request;
  }
}
