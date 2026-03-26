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

package de.gematik.zeta.testfachdienst.ws.model;

import de.gematik.zeta.testfachdienst.model.Erezept;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Response payload for websocket requests that return multiple E-Rezepte. */
@Schema(
    name = "ErezeptListResponse",
    description = "WebSocket response containing all matching E-Rezepte")
public record ErezeptListResponse(
    @ArraySchema(
        arraySchema = @Schema(description = "List of E-Rezepte"),
        schema = @Schema(implementation = Erezept.class))
    List<Erezept> items) {
}
