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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Pusher registration according to gemF PushNotification.
 *
 * <p>Represents the pusher payload from A_27104, A_27154, A_27155, and A_27156.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "PushPusher",
    description = "PushNotification pusher registration for A_27104, A_27154, A_27155, and A_27156")
public record PushPusher(
    @JsonProperty("pushkey")
    @Schema(description = "Unique device push key required by A_27154, A_27155, and A_27156",
        example = "<APNS/GCM TOKEN>")
    String pushkey,
    @JsonProperty("kind")
    @Schema(description = "Pusher kind; null deletes the pusher according to A_27156",
        example = "http")
    String kind,
    @JsonProperty("app_id")
    @Schema(description = "Reverse-DNS app identifier required by A_27154 and A_27155",
        example = "com.example.app.ios")
    String appId,
    @JsonProperty("app_display_name")
    @Schema(description = "Application display name required for pusher registration",
        example = "Mat Rix")
    String appDisplayName,
    @JsonProperty("device_display_name")
    @Schema(description = "Device display name required for pusher registration",
        example = "iPhone 9")
    String deviceDisplayName,
    @JsonProperty("profile_tag")
    @Schema(description = "Profile tag")
    String profileTag,
    @JsonProperty("lang")
    @Schema(description = "Preferred language required for pusher registration", example = "en-US")
    String lang,
    @JsonProperty("data")
    @Schema(description = "Pusher implementation data required for pusher registration")
    Map<String, Object> data,
    @JsonProperty("encryption")
    @Schema(description = "Pusher encryption data")
    Map<String, Object> encryption,
    @JsonProperty("append")
    @Schema(description = "Whether the pusher should be appended")
    Boolean append) {

  /**
   * Creates a PushNotification pusher registration.
   *
   * @param pushkey unique device push key
   * @param kind pusher kind; {@code null} deletes the pusher
   * @param appId reverse-DNS app identifier
   * @param appDisplayName application display name
   * @param deviceDisplayName device display name
   * @param profileTag profile tag
   * @param lang preferred language
   * @param data pusher implementation data
   * @param encryption pusher encryption data
   * @param append append flag
   */
  public PushPusher {
    data = data == null ? null : Map.copyOf(data);
    encryption = encryption == null ? null : Map.copyOf(encryption);
  }
}
