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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.config.SelfDisclosureExportConfig;
import de.gematik.zeta.testfachdienst.service.SelfDisclosureExportService.OtlpLogExporterFactory;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link SelfDisclosureExportService} exporter selection and endpoint handling.
 */
@ExtendWith(MockitoExtension.class)
class SelfDisclosureExportServiceTest {

  @Mock
  private SelfDisclosureService selfDisclosureService;
  @Mock
  private SelfDisclosureExportConfig config;
  @Mock
  private OtlpLogExporterFactory exporterFactory;
  @Mock
  private LogRecordExporter logRecordExporter;

  /**
   * Ensures the service initializes and uses the gRPC exporter when gRPC is enabled.
   */
  @Test
  void usesGrpcExporterWhenEnabled() {
    when(config.isGrpcExportEnabled()).thenReturn(true);
    when(config.isHttpExportEnabled()).thenReturn(false);
    when(config.getGrpcHost()).thenReturn("telemetry:4317");

    LogRecordData record =
        TestLogRecordData.builder()
            .setTimestamp(Instant.now())
            .setBody("test")
            .build();
    when(selfDisclosureService.generateSelfDisclosureRecord()).thenReturn(record);

    assertDoesNotThrow(() -> {
      try (LogRecordExporter exporter = logRecordExporter) {
        when(exporterFactory.createGrpcExporter("http://telemetry:4317")).thenReturn(exporter);
        when(exporter.export(anyList())).thenReturn(CompletableResultCode.ofSuccess());

        SelfDisclosureExportService service =
            new SelfDisclosureExportService(selfDisclosureService, config, exporterFactory);
        service.exportSelfDisclosure();

        verify(exporterFactory).createGrpcExporter("http://telemetry:4317");
        verify(exporter).export(Collections.singletonList(record));
        verify(exporterFactory, never()).createHttpExporter(anyString());
      }
    });
  }

  /**
   * Verifies the HTTP exporter is used when gRPC is disabled but HTTP is enabled.
   */
  @Test
  void fallsBackToHttpExporterWhenGrpcDisabled() {
    when(config.isGrpcExportEnabled()).thenReturn(false);
    when(config.isHttpExportEnabled()).thenReturn(true);
    when(config.getHttpHost()).thenReturn("telemetry:4318");

    LogRecordData record =
        TestLogRecordData.builder()
            .setTimestamp(Instant.now())
            .setBody("test")
            .build();
    when(selfDisclosureService.generateSelfDisclosureRecord()).thenReturn(record);

    assertDoesNotThrow(() -> {
      try (LogRecordExporter exporter = logRecordExporter) {
        when(exporterFactory.createHttpExporter("http://telemetry:4318"))
            .thenReturn(exporter);
        when(exporter.export(anyList())).thenReturn(CompletableResultCode.ofSuccess());

        SelfDisclosureExportService service =
            new SelfDisclosureExportService(selfDisclosureService, config, exporterFactory);
        service.exportSelfDisclosure();

        verify(exporterFactory).createHttpExporter("http://telemetry:4318");
        verify(exporter).export(Collections.singletonList(record));
        verify(exporterFactory, never()).createGrpcExporter(anyString());
      }
    });
  }

  /**
   * Confirms export is skipped entirely when neither HTTP nor gRPC export is enabled.
   */
  @Test
  void skipsExportWhenNoExporterEnabled() {
    when(config.isGrpcExportEnabled()).thenReturn(false);
    when(config.isHttpExportEnabled()).thenReturn(false);

    SelfDisclosureExportService service =
        new SelfDisclosureExportService(selfDisclosureService, config, exporterFactory);
    service.exportSelfDisclosure();

    verifyNoInteractions(exporterFactory, logRecordExporter, selfDisclosureService);
  }
}
