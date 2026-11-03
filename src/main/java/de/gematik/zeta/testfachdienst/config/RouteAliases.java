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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Configurable, literal prefixes shared by REST, WebSocket, and STOMP routes. */
@Component
public class RouteAliases {

  private static final List<String> STOMP_PREFIXES = List.of("/app", "/topic", "/queue", "/user");

  private final List<String> prefixes;

  /** Creates aliases from a comma-separated list; an empty value disables aliases. */
  public RouteAliases(@Value("${app.routes.prefixes:/no-popp}") String configuredPrefixes) {
    prefixes = Arrays.stream(configuredPrefixes.split(","))
        .map(String::trim)
        .filter(prefix -> !prefix.isEmpty())
        .map(RouteAliases::normalize)
        .distinct()
        .toList();
    validatePrefixes();
  }

  /** Keeps original paths and adds each prefix independently, without stacking prefixes. */
  public String[] expand(String... paths) {
    Set<String> expanded = new LinkedHashSet<>(Arrays.asList(paths));
    for (String prefix : prefixes) {
      for (String path : paths) {
        expanded.add(prefix + (path.startsWith("/") ? path : "/" + path));
      }
    }
    return expanded.toArray(String[]::new);
  }

  /** Removes the longest configured prefix from a path while preserving unaliased paths. */
  public String canonicalize(String path) {
    return prefixes.stream()
        .filter(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"))
        .max((left, right) -> Integer.compare(left.length(), right.length()))
        .map(prefix -> path.substring(prefix.length()))
        .orElse(path);
  }

  private void validatePrefixes() {
    for (String prefix : prefixes) {
      if (hasStompPrefix(prefix)) {
        throw new IllegalArgumentException(
            "Route prefix conflicts with canonical STOMP destinations: " + prefix);
      }
      for (String other : prefixes) {
        if (prefix.startsWith(other + "/") && hasStompPrefix(prefix.substring(other.length()))) {
          throw new IllegalArgumentException(
              "Route prefixes create ambiguous STOMP destinations: " + other + " and " + prefix);
        }
      }
    }
  }

  private static boolean hasStompPrefix(String path) {
    return STOMP_PREFIXES.stream()
        .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
  }

  private static String normalize(String prefix) {
    String normalized = prefix.replaceAll("^/+|/+$", "");
    if (!normalized.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")) {
      throw new IllegalArgumentException(
          "Route prefix must contain literal path segments: " + prefix);
    }
    return "/" + normalized;
  }
}
