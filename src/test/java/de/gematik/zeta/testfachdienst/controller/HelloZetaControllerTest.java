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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.model.HelloZetaResource;
import de.gematik.zeta.testfachdienst.service.HelloZetaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link HelloZetaController}.
 */
@ExtendWith(MockitoExtension.class)
class HelloZetaControllerTest {

  @Mock
  private HelloZetaService service;

  @InjectMocks
  private HelloZetaController controller;

  /**
   * Verifies that the controller delegates to the service and returns the greeting payload.
   */
  @Test
  void getHelloZetaResponse_returnsServicePayload() {
    var resource = new HelloZetaResource("Hello");
    when(service.getHelloZetaResource()).thenReturn(resource);

    var response = controller.getHelloZetaResponse(null);

    assertThat(response.getBody()).isSameAs(resource);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(service).getHelloZetaResource();
  }

  /**
   * Verifies that the proxy error endpoint sets the ZETA-Cause header.
   */
  @Test
  void getHelloZetaProxyErrorResponse_setsProxyHeader() {
    var resource = new HelloZetaResource("Hello");
    when(service.getHelloZetaResource()).thenReturn(resource);

    var response = controller.getHelloZetaProxyErrorResponse();

    assertThat(response.getBody()).isSameAs(resource);
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getHeaders().getFirst("ZETA-Cause")).isEqualTo("Proxy");
    verify(service).getHelloZetaResource();
  }

  /**
   * Verifies that the optional responseDelay query parameter delays the response.
   */
  @Test
  void getHelloZetaResponse_withResponseDelay_waitsBeforeReturning() {
    var resource = new HelloZetaResource("Hello");
    when(service.getHelloZetaResource()).thenReturn(resource);

    long startTimeNanos = System.nanoTime();
    var response = controller.getHelloZetaResponse(1);
    long durationMillis = (System.nanoTime() - startTimeNanos) / 1_000_000;

    assertThat(response.getBody()).isSameAs(resource);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(durationMillis).isGreaterThanOrEqualTo(900);
    verify(service).getHelloZetaResource();
  }

  /**
   * Verifies that the path-based delay endpoint waits before returning.
   */
  @Test
  void getHelloZetaResponseWithPathDelay_waitsBeforeReturning() {
    var resource = new HelloZetaResource("Hello");
    when(service.getHelloZetaResource()).thenReturn(resource);

    long startTimeNanos = System.nanoTime();
    var response = controller.getHelloZetaResponseWithPathDelay(1);
    long durationMillis = (System.nanoTime() - startTimeNanos) / 1_000_000;

    assertThat(response.getBody()).isSameAs(resource);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(durationMillis).isGreaterThanOrEqualTo(900);
    verify(service).getHelloZetaResource();
  }

  /**
   * Verifies that negative path-based delays are rejected with HTTP 400.
   */
  @Test
  void getHelloZetaResponseWithPathDelay_withNegativeDelay_throwsBadRequest() {
    assertThatThrownBy(() -> controller.getHelloZetaResponseWithPathDelay(-1))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
        .isEqualTo(400);

    verifyNoInteractions(service);
  }
}
