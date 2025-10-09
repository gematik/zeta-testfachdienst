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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.zeta.testfachdienst.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.model.HelloZetaResource;
import de.gematik.zeta.testfachdienst.service.HelloZetaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HelloZetaControllerTest {

  @Mock
  private HelloZetaService service;

  @InjectMocks
  private HelloZetaController controller;

  @Test
  void getHelloZetaResponse_returnsServicePayload() {
    var resource = new HelloZetaResource("Hello");
    when(service.getHelloZetaResource()).thenReturn(resource);

    var response = controller.getHelloZetaResponse();

    assertThat(response.getBody()).isSameAs(resource);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(service).getHelloZetaResource();
  }
}
