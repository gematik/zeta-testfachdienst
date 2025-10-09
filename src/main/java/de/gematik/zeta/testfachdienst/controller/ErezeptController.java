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
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes CRUD operations for {@link Erezept} resources.
 */
@RestController
@RequestMapping("/api/erezept")
@Slf4j
@RequiredArgsConstructor
public class ErezeptController {

  private final ErezeptService service;

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
   * @return HTTP 201 with location header when created, 400 on duplicate identifiers
   */
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody Erezept req) {
    log.info("Create E-Rezept prescriptionId={}", req.getPrescriptionId());
    var created = service.create(req);
    if (created.isEmpty()) {
      log.warn("Duplicate prescriptionId={}", req.getPrescriptionId());
      return ResponseEntity.badRequest().body("PrescriptionId already exists");
    }
    Erezept saved = created.get();
    return ResponseEntity.created(URI.create("/api/erezept/" + saved.getId())).body(saved);
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
