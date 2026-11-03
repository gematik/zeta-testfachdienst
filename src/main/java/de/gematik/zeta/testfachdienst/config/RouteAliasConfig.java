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

import java.lang.reflect.Method;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Registers prefix aliases while preserving all controller mapping conditions. */
@Configuration
public class RouteAliasConfig implements WebMvcRegistrations {

  private final RouteAliases aliases;

  /** Creates the MVC registration with the shared prefix configuration. */
  public RouteAliasConfig(RouteAliases aliases) {
    this.aliases = aliases;
  }

  @Override
  public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
    return new RequestMappingHandlerMapping() {
      @Override
      protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo mapping = super.getMappingForMethod(method, handlerType);
        if (mapping == null || !handlerType.getPackageName()
            .startsWith("de.gematik.zeta.testfachdienst.controller")) {
          return mapping;
        }
        return mapping.mutate()
            .paths(aliases.expand(mapping.getPatternValues().toArray(String[]::new)))
            .build();
      }
    };
  }
}
