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

package de.gematik.zeta.testfachdienst.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response body for the PushNotification pusher list route.
 *
 * <p>Used by GET /pushers from A_27104.
 */
@Schema(name = "PushPushersResponse", description = "List of registered pushers for A_27104")
public record PushPushersResponse(
    @ArraySchema(schema = @Schema(
        implementation = PushPusher.class,
        description = "Registered pushers returned by A_27104"))
    List<PushPusher> pushers) {

  /**
   * Creates a response containing registered pushers.
   *
   * @param pushers registered pushers
   */
  public PushPushersResponse {
    pushers = List.copyOf(pushers);
  }
}
