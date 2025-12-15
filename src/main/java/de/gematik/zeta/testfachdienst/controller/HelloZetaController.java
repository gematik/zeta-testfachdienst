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

package de.gematik.zeta.testfachdienst.controller;

import de.gematik.zeta.testfachdienst.model.HelloZetaResource;
import de.gematik.zeta.testfachdienst.service.HelloZetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the public {@code /hellozeta} endpoint.
 *
 * <p>The controller delegates retrieval of the greeting resource to
 * {@link HelloZetaService} and augments the interaction with trace logs.</p>
 */
@RestController
@RequestMapping("/hellozeta")
@Slf4j
public class HelloZetaController {

  private final HelloZetaService service;

  /**
   * Create a new controller instance with the required service dependency.
   *
   * @param service business service that provides the greeting resource
   */
  public HelloZetaController(HelloZetaService service) {
    this.service = service;
    log.debug(this.getClass().getSimpleName() + " initialized!");
  }

  /**
   * Retrieve the greeting resource and return it as the response body.
   *
   * @return HTTP 200 response containing the greeting payload
   */
  @GetMapping
  public ResponseEntity<HelloZetaResource> getHelloZetaResponse() {
    log.debug("Trying to get resource from HalloZetaService.");
    HelloZetaResource resource = service.getHelloZetaResource();
    log.debug("Got resource: '" + resource + "' from service, returning resource with 200.");
    return ResponseEntity.ok(resource);
  }
}
