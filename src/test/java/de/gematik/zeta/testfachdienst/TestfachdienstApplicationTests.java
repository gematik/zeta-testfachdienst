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

package de.gematik.zeta.testfachdienst;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Integration tests for externally visible application endpoints and context-path handling. */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "server.ssl.enabled=false",
        "management.server.port=0",
        "jobrunr.dashboard.enabled=false",
        "server.servlet.context-path=/achelos_testfachdienst"
    })
class TestfachdienstApplicationTests {

  private static final String CONTEXT_PATH = "/achelos_testfachdienst";

  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @LocalServerPort private int port;

  @Value("${local.management.port}")
  private int managementPort;

  @Value("${springwolf.docket.info.version}")
  private String asyncApiVersion;

  @Value("${selfdisclosure.resource-attributes.product_version}")
  private String productVersion;

  @Test
  void helloEndpointIsServedUnderConfiguredContextPath()
      throws IOException, InterruptedException {
    HttpResponse<String> response = get(appUri("/hellozeta"));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("Hello ZETA!");
  }

  @Test
  void livenessEndpointIsExposedOnManagementPort()
      throws IOException, InterruptedException {
    HttpResponse<String> response = get(managementUri("/actuator/health/liveness"));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"");
  }

  @Test
  void apiDocsEndpointIsExposed() throws IOException, InterruptedException {
    HttpResponse<String> response = get(appUri("/v3/api-docs"));

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode apiDocs = objectMapper.readTree(response.body());

    assertThat(apiDocs.path("openapi").asText()).isNotBlank();
    assertThat(apiDocs.path("paths").has("/api/erezept")).isTrue();
    assertThat(apiDocs.at("/paths/~1api~1erezept/post/summary").asText())
        .isEqualTo("Create a new E-Rezept");
    assertThat(apiDocs.at("/paths/~1hellozeta/get/summary").asText())
        .isEqualTo("Return the Hello ZETA greeting");
    assertThat(
        apiDocs.at("/paths/~1jobs~1info/get/responses/200/content/text~1plain/schema/type")
            .asText())
        .isEqualTo("string");
    assertThat(
        apiDocs.at("/components/schemas/HelloZetaResource/properties/message/example").asText())
        .isEqualTo("Hello ZETA!");
    assertThat(apiDocs.at("/components/schemas/ERezept/properties/prescriptionId/example").asText())
        .isEqualTo("RX-2025-000123");
  }

  @Test
  void asyncApiDocsExposeWebsocketReplySchemas() throws IOException, InterruptedException {
    HttpResponse<String> response = get(appUri("/springwolf/docs"));

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode asyncApiDocs = objectMapper.readTree(response.body());

    assertThat(asyncApiDocs.path("asyncapi").asText()).isNotBlank();
    assertThat(asyncApiDocs.at("/info/version").asText()).isEqualTo(asyncApiVersion);
    assertThat(asyncApiVersion).doesNotContain("@projectVersion@");
    assertThat(productVersion).isEqualTo(asyncApiVersion);
    assertThat(asyncApiDocs.at("/channels/erezept.list/address").asText())
        .isEqualTo("erezept.list");
    assertThat(asyncApiDocs.path("components").path("messages").has("ErezeptListResponse"))
        .isTrue();
    assertThat(
        asyncApiDocs.at("/components/messages/ErezeptListResponse/payload/schema/$ref").asText())
        .isEqualTo("#/components/schemas/ErezeptListResponse");
    assertThat(
        asyncApiDocs.at(
                "/components/schemas/ErezeptListResponse/properties/items/type")
            .asText())
        .isEqualTo("array");
    assertThat(
        asyncApiDocs.at(
                "/components/schemas/ErezeptDeleteResponse/properties/status/type")
            .asText())
        .isEqualTo("string");
    assertThat(
        asyncApiDocs.at(
            "/operations/erezept.list_receive_list/reply/messages/0/$ref").asText())
        .isEqualTo("#/channels/_queue_erezept/messages/ErezeptListResponse");
  }

  @Test
  void createErezeptReturnsLocationIncludingContextPath()
      throws IOException, InterruptedException {
    HttpResponse<String> createResponse =
        post(
            appUri("/api/erezept"),
            """
            {
              "medicationName": "Ibuprofen 400 mg",
              "dosage": "1 tablet, 3x daily after meals",
              "issuedAt": "2026-03-09T10:15:30Z",
              "expiresAt": "2026-06-09T10:15:30Z",
              "status": "CREATED",
              "patientId": "PAT-123456",
              "practitionerId": "PRAC-98765",
              "prescriptionId": "RX-2026-000123"
            }
            """);

    assertThat(createResponse.statusCode()).isEqualTo(201);
    String location = createResponse.headers().firstValue(HttpHeaders.LOCATION).orElseThrow();
    assertThat(location).startsWith(CONTEXT_PATH + "/api/erezept/");

    JsonNode created = objectMapper.readTree(createResponse.body());
    assertThat(created.path("id").asLong()).isPositive();
    assertThat(created.path("prescriptionId").asText()).isEqualTo("RX-2026-000123");

    HttpResponse<String> fetchResponse = get(appUri(location.substring(CONTEXT_PATH.length())));
    assertThat(fetchResponse.statusCode()).isEqualTo(200);
    assertThat(fetchResponse.body()).contains("\"prescriptionId\":\"RX-2026-000123\"");
  }

  /**
   * Execute an HTTP GET request against the running test application.
   *
   * @param uri target URI to request
   * @return response returned by the embedded server
   * @throws IOException on transport failures
   * @throws InterruptedException if the calling thread is interrupted
   */
  private HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Execute an HTTP POST request with a JSON body against the running test application.
   *
   * @param uri target URI to request
   * @param body JSON payload to send
   * @return response returned by the embedded server
   * @throws IOException on transport failures
   * @throws InterruptedException if the calling thread is interrupted
   */
  private HttpResponse<String> post(URI uri, String body) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Build an application URI rooted at the configured servlet context path.
   *
   * @param path relative path below the application context
   * @return absolute URI pointing at the application server
   */
  private URI appUri(String path) {
    return URI.create("http://localhost:" + port + CONTEXT_PATH + path);
  }

  /**
   * Build an actuator URI on the management port.
   *
   * @param path relative management endpoint path
   * @return absolute URI pointing at the management server
   */
  private URI managementUri(String path) {
    return URI.create("http://localhost:" + managementPort + path);
  }
}
