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

package de.gematik.zeta.testfachdienst.controller;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.service.ErezeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller that exposes CRUD operations for {@link Erezept} resources.
 */
@RestController
@RequestMapping("/api/erezept")
@Slf4j
@Tag(
    name = "E-Rezept",
    description = "CRUD API for managing electronic prescriptions stored by the test service")
public class ErezeptController {

  private static final String EREZEPT_EXAMPLE = """
      {
        "id": 1,
        "medicationName": "Ibuprofen 400 mg",
        "dosage": "1 tablet, 3x daily after meals",
        "issuedAt": "2026-03-09T10:15:30Z",
        "expiresAt": "2026-06-09T10:15:30Z",
        "status": "CREATED",
        "patientId": "PAT-123456",
        "practitionerId": "PRAC-98765",
        "prescriptionId": "RX-2026-000123"
      }
      """;

  private static final String EREZEPT_CREATE_EXAMPLE = """
      {
        "medicationName": "Ibuprofen 400 mg",
        "dosage": "1 tablet, 3x daily after meals",
        "issuedAt": "2026-03-09T10:15:30Z",
        "expiresAt": "2026-06-09T10:15:30Z",
        "status": "CREATED",
        "patientId": "PAT-123456",
        "practitionerId": "PRAC-98765",
        "prescriptionId": "RX-2026-000123"
      }
      """;

  private final ErezeptService service;

  private final String servletContextPath;

  /**
   * Creates the E-Rezept controller with injected dependencies.
   *
   * @param service service layer for prescription handling
   * @param servletContextPath optional servlet context path prefix
   */
  public ErezeptController(
      ErezeptService service,
      @Value("${server.servlet.context-path:}") String servletContextPath) {
    this.service = service;
    this.servletContextPath = servletContextPath;
  }

  /**
   * Return all stored electronic prescriptions.
   *
   * @return list of persisted prescriptions, possibly empty
   */
  @GetMapping
  @Operation(
      summary = "List all E-Rezepte",
      description = "Returns every prescription currently stored in the service.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Prescription list returned",
          content = @Content(
              mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = Erezept.class)),
              examples = @ExampleObject(value = "[" + EREZEPT_EXAMPLE + "]")))
  })
  public List<Erezept> list() {
    log.debug("List all E-Rezepte");
    return service.findAll();
  }

  /**
   * Retrieve a prescription by its primary key.
   *
   * @param id database identifier of the prescription
   * @return HTTP 200 with the prescription or 404 if none exists
   */
  @GetMapping("/{id}")
  @Operation(
      summary = "Get an E-Rezept by database id",
      description = "Looks up a single prescription by its generated numeric identifier.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Prescription found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Erezept.class),
              examples = @ExampleObject(value = EREZEPT_EXAMPLE))),
      @ApiResponse(responseCode = "404", description = "Prescription not found")
  })
  public ResponseEntity<Erezept> get(
      @Parameter(description = "Database identifier of the prescription", example = "1")
      @PathVariable Long id) {
    log.debug("Fetch E-Rezept by id={}", id);
    return service.findById(id).map(ResponseEntity::ok)
        .orElseGet(() -> {
          log.info("E-Rezept not found: id={}", id);
          return ResponseEntity.notFound().build();
        });
  }

  /**
   * Look up a prescription by its domain-specific identifier.
   *
   * @param prescriptionId external identifier that uniquely identifies the prescription
   * @return HTTP 200 with the matching prescription or 404 when absent
   */
  @GetMapping("/by-prescription/{prescriptionId}")
  @Operation(
      summary = "Get an E-Rezept by prescription id",
      description = "Looks up a single prescription using its business identifier.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Prescription found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Erezept.class),
              examples = @ExampleObject(value = EREZEPT_EXAMPLE))),
      @ApiResponse(responseCode = "404", description = "Prescription not found")
  })
  public ResponseEntity<Erezept> byPrescriptionId(
      @Parameter(
          description = "External prescription identifier",
          example = "RX-2026-000123")
      @PathVariable String prescriptionId) {
    log.debug("Fetch by prescriptionId={}", prescriptionId);
    return service.findByPrescriptionId(prescriptionId).map(ResponseEntity::ok)
        .orElseGet(() -> {
          log.info("E-Rezept not found: prescriptionId={}", prescriptionId);
          return ResponseEntity.notFound().build();
        });
  }

  /**
   * Persist a new electronic prescription.
   *
   * @param req request payload representing the prescription to save
   * @return HTTP 201 with location header when created, 409 on duplicate identifiers
   */
  @PostMapping
  @Operation(
      summary = "Create a new E-Rezept",
      description = "Stores a new prescription and returns the persisted resource together with "
          + "a Location header pointing to the created entity.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Prescription created",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Erezept.class),
              examples = @ExampleObject(value = EREZEPT_EXAMPLE))),
      @ApiResponse(
          responseCode = "409",
          description = "A prescription with the same prescriptionId already exists")
  })
  public ResponseEntity<?> create(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Prescription payload to create",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Erezept.class),
              examples = @ExampleObject(value = EREZEPT_CREATE_EXAMPLE)))
      @Valid @RequestBody Erezept req) {
    log.info("Create E-Rezept prescriptionId={}", req.getPrescriptionId());
    var created = service.create(req);
    if (created.isEmpty()) {
      log.warn("Duplicate prescriptionId={}", req.getPrescriptionId());
      return ResponseEntity.status(HttpStatus.CONFLICT).body("PrescriptionId already exists");
    }
    Erezept saved = created.get();
    String contextPath = normalizeContextPath(servletContextPath);
    var location = UriComponentsBuilder.fromPath(contextPath)
        .path("/api/erezept/{id}")
        .buildAndExpand(saved.getId())
        .toUri();
    return ResponseEntity.created(location).body(saved);
  }

  /**
   * Normalize the configured servlet context path so it can be prefixed to generated locations.
   *
   * @param contextPath raw context path from configuration (may be null or blank)
   * @return normalized context path starting with {@code /} and without trailing slash
   */
  private String normalizeContextPath(String contextPath) {
    if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
      return "";
    }
    if (!contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    return contextPath.endsWith("/")
        ? contextPath.substring(0, contextPath.length() - 1)
        : contextPath;
  }

  /**
   * Update an existing prescription in place.
   *
   * @param id  identifier of the prescription to update
   * @param req new state to apply to the existing entity
   * @return HTTP 200 on success or 404 if the entity is missing
   */
  @PutMapping("/{id}")
  @Operation(
      summary = "Update an existing E-Rezept",
      description = "Replaces the mutable state of an existing prescription.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Prescription updated",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Erezept.class),
              examples = @ExampleObject(value = EREZEPT_EXAMPLE))),
      @ApiResponse(responseCode = "404", description = "Prescription not found")
  })
  public ResponseEntity<?> update(
      @Parameter(description = "Database identifier of the prescription", example = "1")
      @PathVariable Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "New prescription state",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Erezept.class),
              examples = @ExampleObject(value = EREZEPT_CREATE_EXAMPLE)))
      @Valid @RequestBody Erezept req) {
    log.info("Update E-Rezept id={}", id);
    return service.update(id, req).map(ResponseEntity::ok).orElseGet(() -> {
      log.warn("Update failed; E-Rezept not found id={}", id);
      return ResponseEntity.notFound().build();
    });
  }

  /**
   * Remove a prescription from the persistence layer.
   *
   * @param id identifier of the prescription to delete
   * @return HTTP 204 when deleted or 404 if the record did not exist
   */
  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete an E-Rezept",
      description = "Removes a prescription from the service by its database id.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Prescription deleted"),
      @ApiResponse(responseCode = "404", description = "Prescription not found")
  })
  public ResponseEntity<Void> delete(
      @Parameter(description = "Database identifier of the prescription", example = "1")
      @PathVariable Long id) {
    log.info("Delete E-Rezept id={}", id);
    if (!service.deleteIfExists(id)) {
      log.warn("Delete failed; E-Rezept not found id={}", id);
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
