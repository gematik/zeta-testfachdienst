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

package de.gematik.zeta.testfachdienst.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HelloZetaResource}.
 */
class HelloZetaResourceTest {

  /**
   * Verifies the default constructor allows setting the message via setter.
   */
  @Test
  void defaultConstructor_allowsSettingMessage() {
    var resource = new HelloZetaResource();
    resource.setMessage("Hello");

    assertThat(resource.getMessage()).isEqualTo("Hello");
  }

  /**
   * Ensures the parameterized constructor initializes the message field.
   */
  @Test
  void parameterConstructor_initializesMessage() {
    var resource = new HelloZetaResource("Hi there");

    assertThat(resource.getMessage()).isEqualTo("Hi there");
  }
}
