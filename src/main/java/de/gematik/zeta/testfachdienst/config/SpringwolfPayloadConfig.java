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

package de.gematik.zeta.testfachdienst.config;

import io.github.springwolf.core.asyncapi.scanners.common.payload.PayloadMethodReturnService;
import io.github.springwolf.core.asyncapi.scanners.common.payload.PayloadSchemaObject;
import io.github.springwolf.core.asyncapi.scanners.common.payload.internal.PayloadService;
import java.lang.reflect.Method;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration that replaces Springwolf's default payload method return service so that
 * schema generation uses the generic return type of controller methods.
 */
@Configuration
@SuppressWarnings("unused") // instantiated by Spring's component scan
public class SpringwolfPayloadConfig {

  /**
   * Creates a {@link PayloadMethodReturnService} that builds schemas using the method's generic
   * return type.
   *
   * @param payloadService delegate used to build payload schemas
   * @return payload return service aware of generic method return types
   */
  @Bean
  @Primary
  public PayloadMethodReturnService genericAwarePayloadMethodReturnService(
      PayloadService payloadService) {
    return new PayloadMethodReturnService(payloadService) {
      /**
       * Build a payload schema using the method's generic return type instead of the raw class.
       *
       * @param method controller method being inspected
       * @return schema object derived from the generic return type
       */
      @Override
      public PayloadSchemaObject extractSchema(Method method) {
        return payloadService.buildSchema(method.getGenericReturnType());
      }
    };
  }
}
