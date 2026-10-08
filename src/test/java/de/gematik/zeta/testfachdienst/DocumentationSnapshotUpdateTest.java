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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Updates checked-in API documentation snapshots from the generated runtime endpoints.
 */
@EnabledIfSystemProperty(
    named = "testfachdienst.docs.update",
    matches = "swagger|springwolf|all")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "server.ssl.enabled=false",
        "management.server.port=0",
        "jobrunr.dashboard.enabled=false",
        "server.servlet.context-path=/achelos_testfachdienst"
    })
class DocumentationSnapshotUpdateTest {

  private static final String CONTEXT_PATH = "/achelos_testfachdienst";
  private static final Path SWAGGER_DOC = Path.of("docs/swagger-api-docs.json");
  private static final Path SPRINGWOLF_DOC = Path.of("docs/async-api-docs.yml");

  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final YAMLMapper yamlMapper =
      YAMLMapper.builder().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).build();

  @LocalServerPort private int port;

  /**
   * Updates the documentation snapshots selected by {@code testfachdienst.docs.update}.
   *
   * @throws IOException on file or HTTP transport failures
   * @throws InterruptedException if the HTTP request is interrupted
   */
  @Test
  void updateDocumentationSnapshots() throws IOException, InterruptedException {
    String snapshot = System.getProperty("testfachdienst.docs.update");
    if ("swagger".equals(snapshot) || "all".equals(snapshot)) {
      updateSwaggerApiDocs();
    }
    if ("springwolf".equals(snapshot) || "all".equals(snapshot)) {
      updateSpringwolfApiDocs();
    }
  }

  /**
   * Writes the generated OpenAPI JSON snapshot.
   *
   * @throws IOException on file or HTTP transport failures
   * @throws InterruptedException if the HTTP request is interrupted
   */
  private void updateSwaggerApiDocs() throws IOException, InterruptedException {
    HttpResponse<String> response = get(appUri("/v3/api-docs"));

    assertThat(response.statusCode()).isEqualTo(200);

    Object json = objectMapper.readTree(response.body());
    String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    Files.writeString(SWAGGER_DOC, prettyJson + "\n");
  }

  /**
   * Writes the generated AsyncAPI YAML snapshot.
   *
   * @throws IOException on file or HTTP transport failures
   * @throws InterruptedException if the HTTP request is interrupted
   */
  private void updateSpringwolfApiDocs() throws IOException, InterruptedException {
    HttpResponse<String> response = get(appUri("/springwolf/docs"));

    assertThat(response.statusCode()).isEqualTo(200);

    Object json = objectMapper.readTree(response.body());
    String yaml = yamlMapper.writeValueAsString(json).stripTrailing() + "\n";
    Files.writeString(SPRINGWOLF_DOC, yaml);
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
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .header("X-Forwarded-Proto", "https")
            .header("X-Forwarded-Host", "localhost:8080")
            .GET()
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
}
