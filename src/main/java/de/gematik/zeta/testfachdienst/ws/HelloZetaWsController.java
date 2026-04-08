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

import de.gematik.zeta.testfachdienst.model.HelloZetaResource;
import de.gematik.zeta.testfachdienst.service.HelloZetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * STOMP WebSocket controller for the HelloZeta endpoint.
 *
 * <p>Clients send an empty message to {@code /app/hellozeta} and receive
 * the greeting on their private {@code /user/queue/hellozeta} destination.</p>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused") // invoked via STOMP @MessageMapping endpoints
public class HelloZetaWsController {

  private final HelloZetaService helloZetaService;

  /**
   * Return the HelloZeta greeting to the requesting user.
   *
   * <p>Reply destination: /user/queue/hellozeta</p>
   *
   * @return greeting resource
   */
  @MessageMapping("hellozeta")
  @SendToUser("/queue/hellozeta")
  public HelloZetaResource helloZeta() {
    log.info("STOMP hellozeta request received");
    return helloZetaService.getHelloZetaResource();
  }
}
