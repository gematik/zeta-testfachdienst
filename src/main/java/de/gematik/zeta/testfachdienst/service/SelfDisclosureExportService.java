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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Emits the self-disclosure event through the application logging pipeline.
 */
@Service
@Slf4j
public class SelfDisclosureExportService {

  private final SelfDisclosureService selfDisclosureService;

  /**
   * Constructor for the self-disclosure export service.
   *
   * @param service service instance to generate self-disclosure data
   */
  @Autowired
  @SuppressWarnings("unused")
  public SelfDisclosureExportService(SelfDisclosureService service) {
    this.selfDisclosureService = service;
  }

  /**
   * Writes the self-disclosure record as a structured log event.
   *
   * <p>The Spring Boot OpenTelemetry logging exporter picks this event up from the normal logging
   * pipeline when OTLP log export is enabled.
   */
  public void exportSelfDisclosure() {
    SelfDisclosureRecord record = selfDisclosureService.generateSelfDisclosureRecord();
    var event = log.atInfo();
    record.attributes().forEach(event::addKeyValue);
    event.log(record.body());
  }
}
