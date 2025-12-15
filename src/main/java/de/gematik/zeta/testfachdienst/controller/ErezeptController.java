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

package de.gematik.zeta.testfachdienst.controller;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.service.ErezeptService;
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
public class ErezeptController {

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
  public ResponseEntity<Erezept> get(@PathVariable Long id) {
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
  public ResponseEntity<Erezept> byPrescriptionId(@PathVariable String prescriptionId) {
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
  public ResponseEntity<?> create(@Valid @RequestBody Erezept req) {
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
    return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
  }

  /**
   * Update an existing prescription in place.
   *
   * @param id  identifier of the prescription to update
   * @param req new state to apply to the existing entity
   * @return HTTP 200 on success or 404 if the entity is missing
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Erezept req) {
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
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    log.info("Delete E-Rezept id={}", id);
    if (!service.deleteIfExists(id)) {
      log.warn("Delete failed; E-Rezept not found id={}", id);
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
