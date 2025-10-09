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
package de.gematik.zeta.testfachdienst.service;

import de.gematik.zeta.testfachdienst.model.HelloZetaResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simple service that constructs {@link HelloZetaResource} instances.
 */
@Service
@Slf4j
public class HelloZetaService {

  /**
   * Create a new greeting resource for the consumer-facing API.
   *
   * @return a greeting wrapper with the default message
   */
  public HelloZetaResource getHelloZetaResource() {
    log.debug("Asking for hello zeta resource.");
    return new HelloZetaResource("Hello ZETA!");
  }
}
