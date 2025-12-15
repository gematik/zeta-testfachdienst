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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configures OTLP export related properties.
 */
@Component
@Getter
public class SelfDisclosureExportConfig {
  private final boolean grpcExportEnabled;
  private final String grpcHost;
  private final boolean httpExportEnabled;
  private final String httpHost;
  private final long intervalSeconds;

  /**
   * Creates a config holder for OTLP self disclosure export settings.
   *
   * @param grpcExportEnabled whether gRPC export is enabled
   * @param grpcHost OTLP gRPC endpoint
   * @param httpExportEnabled whether HTTP export is enabled
   * @param httpHost OTLP HTTP endpoint
   * @param intervalSeconds export interval in seconds
   */
  public SelfDisclosureExportConfig(
      @Value("${otlp.export.logs.grpc.enabled}") boolean grpcExportEnabled,
      @Value("${otlp.export.logs.grpc.host}") String grpcHost,
      @Value("${otlp.export.logs.http.enabled}") boolean httpExportEnabled,
      @Value("${otlp.export.logs.http.host}") String httpHost,
      @Value("${otlp.export.logs.intervalSeconds}") long intervalSeconds) {
    this.grpcExportEnabled = grpcExportEnabled;
    this.grpcHost = grpcHost;
    this.httpExportEnabled = httpExportEnabled;
    this.httpHost = httpHost;
    this.intervalSeconds = intervalSeconds;
  }
}
