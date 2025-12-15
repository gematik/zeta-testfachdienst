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

package de.gematik.zeta.testfachdienst.service;

import de.gematik.zeta.testfachdienst.config.SelfDisclosureProperties;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to provide self disclosure information about the application.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SelfDisclosureService {

  private final SelfDisclosureProperties props;

  /**
   *  Constructs an OTLP conformant LogRecordData object with the configured
   *  properties.
   *
   * @return OTLP conformant object containing the self disclosure
   */
  public LogRecordData generateSelfDisclosureRecord() {
    AttributesBuilder attributesBuilder = Attributes.builder();
    for (Map.Entry<String, String> e : props.getResourceAttributes().entrySet()) {
      attributesBuilder.put(e.getKey(), e.getValue());
    }
    // read value dynamically
    // env var $HOSTNAME is set to pod name in kubernetes by conventions
    String podName = System.getenv("HOSTNAME");
    if (podName != null && !podName.isBlank()) {
      attributesBuilder.put("pod_name", podName);
    }

    return TestLogRecordData.builder()
        .setBody("Selbstauskunft")
        .setTimestamp(Instant.now())
        .setAttributes(attributesBuilder.build())
        .build();
  }
}
