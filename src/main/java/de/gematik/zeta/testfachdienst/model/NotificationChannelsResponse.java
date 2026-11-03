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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** User channels and device registration information returned by the Notification Service. */
public record NotificationChannelsResponse(
    UserIdentifier user,
    @JsonProperty("has_registered_devices") Boolean hasRegisteredDevices,
    @JsonProperty("registered_device_count")
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer registeredDeviceCount,
    List<PushChannel> channels) {

  /** Identifier and identifier type returned by the Notification Service. */
  public record UserIdentifier(@JsonProperty("id_type") String idType, String value) {}
}
