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

package de.gematik.zeta.testfachdienst.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zeta.testfachdienst.controller.ErezeptController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/** Tests configurable aliases and preservation of controller mapping conditions. */
class RouteAliasesTest {

  @Test
  void normalizesMultiplePrefixesWithoutStackingOrDuplicates() {
    assertThat(new RouteAliases(" no-popp/, /other/v2/, /no-popp ")
        .expand("/api/erezept", "/api/erezept/{id}"))
        .containsExactly("/api/erezept", "/api/erezept/{id}",
            "/no-popp/api/erezept", "/no-popp/api/erezept/{id}",
            "/other/v2/api/erezept", "/other/v2/api/erezept/{id}");
  }

  @Test
  void emptyConfigurationDisablesAliases() {
    assertThat(new RouteAliases(" , ").expand("/hellozeta"))
        .containsExactly("/hellozeta");
  }

  @Test
  void canonicalizesLongestMatchingPrefixAtPathBoundary() {
    var aliases = new RouteAliases("/no-popp,/no-popp/v2");

    assertThat(aliases.canonicalize("/no-popp/app/erezept.create"))
        .isEqualTo("/app/erezept.create");
    assertThat(aliases.canonicalize("/no-popp/v2/user/queue/erezept"))
        .isEqualTo("/user/queue/erezept");
    assertThat(aliases.canonicalize("/no-popp-other/app/erezept.create"))
        .isEqualTo("/no-popp-other/app/erezept.create");
  }

  @Test
  void rejectsWildcardsAndNonLiteralPrefixes() {
    for (String prefix : new String[]{"/", "/**", "/{prefix}", "/../other", "/a?b"}) {
      assertThatThrownBy(() -> new RouteAliases(prefix))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/topic", "/queue", "/user", " app/ ", "/topic/nested"})
  void rejectsPrefixesThatRewriteCanonicalStompDestinations(String prefix) {
    assertThatThrownBy(() -> new RouteAliases(prefix))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("conflicts with canonical STOMP destinations");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/no-popp,/no-popp/app", "/no-popp/topic,/no-popp", "/no-popp,/no-popp/queue",
      "/no-popp/user/nested,/no-popp", " no-popp/, /no-popp/app/ "
  })
  void rejectsPrefixesThatMakeStompAliasesAmbiguous(String prefixes) {
    assertThatThrownBy(() -> new RouteAliases(prefixes))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ambiguous STOMP destinations");
  }

  @Test
  void allowsPrefixesThatOnlyShareCharactersWithStompNamespaces() {
    var aliases = new RouteAliases(
        "/application,/topics,/queues,/users,/no-popp,/no-popp/application");

    assertThat(aliases.canonicalize("/application/app/hellozeta")).isEqualTo("/app/hellozeta");
    assertThat(aliases.canonicalize("/no-popp/application/topic/erezept"))
        .isEqualTo("/topic/erezept");
    assertThat(aliases.canonicalize("/app/hellozeta")).isEqualTo("/app/hellozeta");
  }

  @Test
  void preservesEveryPrescriptionMappingConditionForCustomAndDisabledPrefixes() {
    var original = new RouteAliasConfig(new RouteAliases("")).getRequestMappingHandlerMapping();
    var aliased = new RouteAliasConfig(new RouteAliases("/custom,/other"))
        .getRequestMappingHandlerMapping();
    for (var method : ErezeptController.class.getDeclaredMethods()) {
      RequestMappingInfo mapping = ReflectionTestUtils.invokeMethod(
          original, "getMappingForMethod", method, ErezeptController.class);
      if (mapping == null) {
        continue;
      }
      RequestMappingInfo expanded = ReflectionTestUtils.invokeMethod(
          aliased, "getMappingForMethod", method, ErezeptController.class);
      assertThat(expanded.getPatternValues()).containsExactlyInAnyOrder(
          new RouteAliases("/custom,/other")
              .expand(mapping.getPatternValues().toArray(String[]::new)));
      assertThat(expanded.mutate().paths(mapping.getPatternValues().toArray(String[]::new)).build())
          .isEqualTo(mapping);
    }
  }
}
