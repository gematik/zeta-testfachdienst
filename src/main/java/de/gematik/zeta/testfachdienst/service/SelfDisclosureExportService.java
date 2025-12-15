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

import de.gematik.zeta.testfachdienst.config.SelfDisclosureExportConfig;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Export service that handles an OTLP conformant log record export.
 */
@Service
@Slf4j
@Component
public class SelfDisclosureExportService {
  private final SelfDisclosureService selfDisclosureService;
  private final SelfDisclosureExportConfig config;
  private final OtlpLogExporterFactory exporterFactory;
  private LogRecordExporter logExporter;
  private static final String HTTP_SCHEME = "http://";
  private static final String HTTPS_SCHEME = "https://";

  /**
   * Constructor for OTLP export service.
   *
   * @param service Service instance to generate self disclosure object
   * @param config Configuration object for this service
   */
  @Autowired
  @SuppressWarnings("unused")
  public SelfDisclosureExportService(SelfDisclosureService service, SelfDisclosureExportConfig config) {
    this(service, config, new DefaultOtlpLogExporterFactory());
  }

  /**
   * Package-private constructor used for testing to supply a custom exporter factory.
   *
   * @param service self disclosure service that builds the log record
   * @param config export configuration that controls protocol selection
   * @param exporterFactory factory that creates the OTLP exporters
   */
  SelfDisclosureExportService(
      SelfDisclosureService service,
      SelfDisclosureExportConfig config,
      OtlpLogExporterFactory exporterFactory) {
    this.selfDisclosureService = service;
    this.config = config;
    this.exporterFactory = exporterFactory;
    this.logExporter = null;
  }

  /**
   * Creates an OTLP log exporter based on the enabled protocol with a normalized endpoint.
   *
   * @param config export configuration containing the host
   * @return initialized {@link LogRecordExporter}
   */
  private LogRecordExporter setupLogExporter(SelfDisclosureExportConfig config) {
    if (config.isGrpcExportEnabled() && config.isHttpExportEnabled()) {
      log.info("Both OTLP HTTP and gRPC export are enabled; defaulting to gRPC exporter");
    }

    if (config.isGrpcExportEnabled()) {
      String endpoint = normalizeEndpoint(config.getGrpcHost(), "gRPC");
      return exporterFactory.createGrpcExporter(endpoint);
    }
    if (config.isHttpExportEnabled()) {
      String endpoint = normalizeEndpoint(config.getHttpHost(), "HTTP");
      return exporterFactory.createHttpExporter(endpoint);
    }
    throw new IllegalStateException("No OTLP exporter enabled");
  }

  /**
   * Normalizes the OTLP endpoint by ensuring an HTTP/HTTPS scheme is present, defaulting to HTTP.
   *
   * @param endpoint endpoint string from configuration
   * @param exporterType export protocol for logging purposes
   * @return endpoint with an explicit scheme
   * @throws IllegalArgumentException if the endpoint is null or blank
   */
  private String normalizeEndpoint(String endpoint, String exporterType) {
    if (endpoint == null || endpoint.isBlank()) {
      throw new IllegalArgumentException("OTLP " + exporterType + " host must not be empty");
    }
    if (endpoint.startsWith(HTTP_SCHEME) || endpoint.startsWith(HTTPS_SCHEME)) {
      return endpoint;
    }
    String normalizedEndpoint = HTTP_SCHEME + endpoint;
    log.info(
        "OTLP {} host '{}' missing scheme, defaulting to '{}'",
        exporterType,
        endpoint,
        normalizedEndpoint);
    return normalizedEndpoint;
  }

  /**
   * Method to construct and export self disclosure log record via OTLP.
   *
   * <p>Honors the OTLP export flags, lazily initializes the configured exporter once, then emits
   * the generated self disclosure as a single log record.
   */
  public void exportSelfDisclosure() {
    if (!config.isHttpExportEnabled() && !config.isGrpcExportEnabled()) {
      log.debug("OTLP export disabled; skipping self disclosure export");
      return;
    }
    if (logExporter == null) {
      logExporter = setupLogExporter(config);
    }
    LogRecordData logRecord = selfDisclosureService.generateSelfDisclosureRecord();
    logExporter.export(Collections.singletonList(logRecord));
  }

  /**
   * Retrieves the configured export interval in seconds from injected config object.
   *
   * @return configured export interval
   */
  public long getExportIntervalInSeconds() {
    return config.getIntervalSeconds();
  }

  /**
   * Factory abstraction to create OTLP log exporters for different transports.
   */
  interface OtlpLogExporterFactory {
    /**
     * Create an HTTP-based OTLP exporter targeting the given endpoint.
     *
     * @param endpoint OTLP HTTP endpoint URL
     * @return configured HTTP exporter
     */
    LogRecordExporter createHttpExporter(String endpoint);

    /**
     * Create a gRPC-based OTLP exporter targeting the given endpoint.
     *
     * @param endpoint OTLP gRPC endpoint URL
     * @return configured gRPC exporter
     */
    LogRecordExporter createGrpcExporter(String endpoint);
  }

  /**
   * Default factory that builds OTLP exporters using the OpenTelemetry SDK builders.
   */
  private static class DefaultOtlpLogExporterFactory implements OtlpLogExporterFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public LogRecordExporter createHttpExporter(String endpoint) {
      return OtlpHttpLogRecordExporter.builder()
          .setEndpoint(endpoint)
          .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogRecordExporter createGrpcExporter(String endpoint) {
      return OtlpGrpcLogRecordExporter.builder()
          .setEndpoint(endpoint)
          .build();
    }
  }

}
