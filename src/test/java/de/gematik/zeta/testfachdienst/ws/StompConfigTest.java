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

package de.gematik.zeta.testfachdienst.ws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link StompConfig} verifying the handling of servlet context paths.
 */
class StompConfigTest {

  @Test
  void usesPlainPrefixesWhenNoContextPathIsSet() {
    var config = new StompConfig();
    ReflectionTestUtils.setField(config, "contextPath", "");

    var brokerPrefixes = (String[]) ReflectionTestUtils.invokeMethod(config, "resolveBrokerPrefixes");
    var appPrefixes = (String[]) ReflectionTestUtils.invokeMethod(config, "resolveApplicationPrefixes");
    var userPrefix = (String) ReflectionTestUtils.invokeMethod(config, "resolveUserPrefix");

    assertThat(brokerPrefixes).containsExactlyInAnyOrder("/topic", "/queue");
    assertThat(appPrefixes).containsExactly("/app");
    assertThat(userPrefix).isEqualTo("/user");
  }

  @Test
  void usesOnlyContextPrefixedDestinationsWhenContextPathIsSet() {
    var config = new StompConfig();
    ReflectionTestUtils.setField(config, "contextPath", "/achelos_testfachdienst");

    var brokerPrefixes = (String[]) ReflectionTestUtils.invokeMethod(config, "resolveBrokerPrefixes");
    var appPrefixes = (String[]) ReflectionTestUtils.invokeMethod(config, "resolveApplicationPrefixes");
    var userPrefix = (String) ReflectionTestUtils.invokeMethod(config, "resolveUserPrefix");

    assertThat(brokerPrefixes)
        .containsExactlyInAnyOrder("/achelos_testfachdienst/topic", "/achelos_testfachdienst/queue", "/queue");
    assertThat(appPrefixes).containsExactly("/achelos_testfachdienst/app");
    assertThat(userPrefix).isEqualTo("/achelos_testfachdienst/user");
  }
}
