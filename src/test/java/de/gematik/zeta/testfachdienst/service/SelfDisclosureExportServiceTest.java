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
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/**
 * Tests for {@link SelfDisclosureExportService} structured log emission.
 */
@ExtendWith(MockitoExtension.class)
class SelfDisclosureExportServiceTest {

  @Mock
  private SelfDisclosureService selfDisclosureService;

  /**
   * Verifies that the service logs self-disclosure data as key-value pairs.
   */
  @Test
  void logsStructuredSelfDisclosureRecord() {
    when(selfDisclosureService.generateSelfDisclosureRecord())
        .thenReturn(new SelfDisclosureRecord(
            "Selbstauskunft",
            Map.of("product_name", "Testfachdienst", "product_version", "test-version")));

    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    var logger = (Logger) LoggerFactory.getLogger(SelfDisclosureExportService.class);
    logger.addAppender(appender);

    try {
      var service = new SelfDisclosureExportService(selfDisclosureService);
      service.exportSelfDisclosure();
    } finally {
      logger.detachAppender(appender);
    }

    assertThat(appender.list).hasSize(1);
    var event = appender.list.getFirst();
    assertThat(event.getFormattedMessage()).isEqualTo("Selbstauskunft");
    assertThat(event.getKeyValuePairs())
        .containsExactlyInAnyOrder(
            new KeyValuePair("product_name", "Testfachdienst"),
            new KeyValuePair("product_version", "test-version"));
  }

}
