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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error response format used by the PushNotification setup API.
 *
 * <p>Matches OpenApi_Notification_Fachdienst error responses used by A_27104 and A_27190 routes.
 */
@Schema(
    name = "PushErrorResponse",
    description = "PushNotification error response from OpenApi_Notification_Fachdienst")
public record PushErrorResponse(
    @Schema(description = "Human readable error message", example = "Missing parameters: lang")
    String error,
    @Schema(description = "Matrix-style error code", example = "M_MISSING_PARAM")
    String errcode) {}
