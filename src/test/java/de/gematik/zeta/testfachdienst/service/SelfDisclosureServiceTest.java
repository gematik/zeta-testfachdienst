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

package de.gematik.zeta.testfachdienst.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zeta.testfachdienst.config.SelfDisclosureProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SelfDisclosureService}.
 */
class SelfDisclosureServiceTest {

  private static final String VERSION_UNDER_TEST = "test-version";

  /**
   * Verifies that generated self-disclosure records contain configured attributes.
   */
  @Test
  void generateSelfDisclosureRecordUsesConfiguredAttributes() {
    var properties = new SelfDisclosureProperties();
    properties.setResourceAttributes(
        Map.of("product_name", "Testfachdienst", "product_version", VERSION_UNDER_TEST));
    var service = new SelfDisclosureService(properties);

    var record = service.generateSelfDisclosureRecord();

    assertThat(record.body()).isEqualTo("Selbstauskunft");
    assertThat(record.attributes())
        .containsEntry("product_name", "Testfachdienst")
        .containsEntry("product_version", VERSION_UNDER_TEST);
  }
}
