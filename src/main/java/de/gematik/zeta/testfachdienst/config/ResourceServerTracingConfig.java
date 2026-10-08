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

import de.gematik.zeta.testfachdienst.observability.ResourceServerTraceInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers resource server tracing for externally visible application endpoints.
 */
@Configuration
public class ResourceServerTracingConfig implements WebMvcConfigurer {

  private final OpenTelemetry openTelemetry;

  /**
   * Create tracing configuration with the shared OpenTelemetry instance.
   *
   * @param openTelemetry OpenTelemetry instance used to create resource server spans
   */
  public ResourceServerTracingConfig(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Register the resource server tracing interceptor for public application endpoints.
   *
   * @param registry MVC interceptor registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ResourceServerTraceInterceptor(openTelemetry))
        .addPathPatterns(
            "/hellozeta/**", "/api/**", "/jobs/**", "/push/v1/**", "/test-support/**");
  }
}
