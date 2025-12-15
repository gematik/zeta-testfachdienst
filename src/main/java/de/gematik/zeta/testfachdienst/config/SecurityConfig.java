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

package de.gematik.zeta.testfachdienst.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig configures the Spring Security filter chain and supporting beans.
 *
 * <p>The configuration keeps the H2 console usable in development and leaves all endpoints
 * publicly accessible.
 */
@Configuration
public class SecurityConfig {

  /**
   * Build the security filter chain for the application.
   *
   * @param http mutable security builder supplied by Spring Boot
   * @return configured security filter chain
   * @throws Exception when the security configuration cannot be built
   */
  @Bean
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        // H2 console support (optional; safe enough for local dev)
        .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .securityContext(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
    ;

    return http.build();
  }
}
