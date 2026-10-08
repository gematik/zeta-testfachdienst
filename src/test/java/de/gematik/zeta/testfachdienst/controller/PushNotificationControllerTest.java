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

package de.gematik.zeta.testfachdienst.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.zeta.testfachdienst.model.PushChannel;
import de.gematik.zeta.testfachdienst.model.PushChannelStatus;
import de.gematik.zeta.testfachdienst.model.PushChannelsResponse;
import de.gematik.zeta.testfachdienst.model.PushErrorResponse;
import de.gematik.zeta.testfachdienst.model.PushPusher;
import de.gematik.zeta.testfachdienst.model.PushPushersResponse;
import de.gematik.zeta.testfachdienst.service.PushNotificationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link PushNotificationController}.
 */
class PushNotificationControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Verifies pusher registration and listing.
   */
  @Test
  void registersAndListsPusher() {
    var controller = new PushNotificationController(new PushNotificationService());

    var response = controller.setPusher(pusher());
    PushPushersResponse pushers = controller.getPushers();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(pushers.pushers()).hasSize(1);
    assertThat(pushers.pushers().getFirst().appId()).isEqualTo("com.example.app.ios");
    assertThat(pushers.pushers().getFirst().pushkey()).isEqualTo("pushkey-1");
  }

  /**
   * Verifies pusher deletion with a null kind.
   */
  @Test
  void deletesPusherWhenKindIsNull() {
    var controller = new PushNotificationController(new PushNotificationService());
    controller.setPusher(pusher());

    var response = controller.setPusher(new PushPusher(
        "pushkey-1",
        null,
        "com.example.app.ios",
        null,
        null,
        null,
        null,
        null,
        null,
        null));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.getPushers().pushers()).isEmpty();
  }

  /**
   * Verifies missing registration fields return the PushNotification error body.
   */
  @Test
  void rejectsMissingPusherParameters() {
    var controller = new PushNotificationController(new PushNotificationService());

    var response = controller.setPusher(new PushPusher(
        "pushkey-1",
        "http",
        "com.example.app.ios",
        null,
        null,
        null,
        null,
        null,
        null,
        null));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isInstanceOf(PushErrorResponse.class);
    assertThat(((PushErrorResponse) response.getBody()).errcode()).isEqualTo("M_MISSING_PARAM");
  }

  /**
   * Verifies channel updates for registered devices.
   */
  @Test
  void updatesRegisteredDeviceChannels() {
    var controller = new PushNotificationController(new PushNotificationService());
    controller.setPusher(pusher());

    var response = controller.setChannelsOfDevice(
        "pushkey-1",
        new PushChannelsResponse(List.of(new PushChannel("erezept", PushChannelStatus.ENABLED))));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.getChannelsOfDevice("pushkey-1").getBody())
        .isEqualTo(new PushChannelsResponse(
            List.of(new PushChannel("erezept", PushChannelStatus.ENABLED))));
  }

  /**
   * Verifies unknown device channel requests return invalid parameter responses.
   */
  @Test
  void rejectsUnknownDeviceChannels() {
    var controller = new PushNotificationController(new PushNotificationService());

    var response = controller.getChannelsOfDevice("missing");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isInstanceOf(PushErrorResponse.class);
    assertThat(((PushErrorResponse) response.getBody()).errcode()).isEqualTo("M_INVALID_PARAM");
  }

  /**
   * Verifies missing channel fields can reach the controller's PushNotification error response.
   *
   * @throws Exception when HTTP request handling fails
   */
  @Test
  void rejectsMissingChannelParametersAtHttpBoundary() throws Exception {
    MockMvc mvc = MockMvcBuilders
        .standaloneSetup(new PushNotificationController(new PushNotificationService()))
        .build();

    mvc.perform(post("/push/v1/channels/pushkey-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errcode").value("M_MISSING_PARAM"));

    mvc.perform(post("/push/v1/channels/pushkey-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"channels\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errcode").value("M_MISSING_PARAM"));
  }

  /**
   * Verifies PushNotification models use the spec-defined JSON field names and enum values.
   *
   * @throws JsonProcessingException when JSON serialization fails
   */
  @Test
  void usesSpecJsonFieldNames() throws JsonProcessingException {
    String pusherJson = objectMapper.writeValueAsString(pusher());

    assertThat(pusherJson).contains("\"app_id\":\"com.example.app.ios\"");
    assertThat(pusherJson).contains("\"device_display_name\":\"Test Device\"");

    PushChannelsResponse response = objectMapper.readValue(
        "{\"channels\":[{\"id\":\"erezept\",\"status\":\"enabled\"}]}",
        PushChannelsResponse.class);

    assertThat(response.channels())
        .containsExactly(new PushChannel("erezept", PushChannelStatus.ENABLED));
  }

  /**
   * Creates a valid test pusher.
   *
   * @return test pusher
   */
  private PushPusher pusher() {
    return new PushPusher(
        "pushkey-1",
        "http",
        "com.example.app.ios",
        "Test App",
        "Test Device",
        null,
        "de-DE",
        Map.of("url", "https://push.example/push/v1/"),
        null,
        false);
  }
}
