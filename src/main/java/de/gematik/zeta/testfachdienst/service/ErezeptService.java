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

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.repository.ErezeptRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service exposing CRUD-style operations for {@link Erezept} aggregates.
 */
@Service
@RequiredArgsConstructor
public class ErezeptService {

  private final ErezeptRepository repository;

  /**
   * Retrieve all prescriptions.
   *
   * @return list of prescriptions, possibly empty
   */
  public List<Erezept> findAll() {
    return repository.findAll();
  }

  /**
   * Find a prescription by database identifier.
   *
   * @param id primary key of the prescription
   * @return optional containing the entity when found
   */
  public Optional<Erezept> findById(Long id) {
    return repository.findById(id);
  }

  /**
   * Find a prescription by its domain-specific identifier.
   *
   * @param prescriptionId business identifier of the prescription
   * @return optional containing the entity when found
   */
  public Optional<Erezept> findByPrescriptionId(String prescriptionId) {
    return repository.findByPrescriptionId(prescriptionId);
  }

  /**
   * Store a prescription when the business identifier is unique.
   *
   * @param prescription prescription to persist
   * @return saved entity when persisted, empty optional when duplicate
   */
  public Optional<Erezept> create(Erezept prescription) {
    if (prescription.getPrescriptionId() != null
        && existsByPrescriptionId(prescription.getPrescriptionId())) {
      return Optional.empty();
    }
    return Optional.of(repository.save(prescription));
  }

  /**
   * Update an existing prescription by applying the provided changes.
   *
   * @param id          identifier of the prescription to update
   * @param updateData  new field values
   * @return updated entity when present, empty optional when missing
   */
  public Optional<Erezept> update(Long id, Erezept updateData) {
    return repository.findById(id).map(existing -> {
      existing.setMedicationName(updateData.getMedicationName());
      existing.setDosage(updateData.getDosage());
      existing.setExpiresAt(updateData.getExpiresAt());
      existing.setStatus(updateData.getStatus());
      return repository.save(existing);
    });
  }

  /**
   * Delete a prescription if a matching entity exists.
   *
   * @param id identifier of the prescription to delete
   * @return {@code true} when deleted, {@code false} otherwise
   */
  public boolean deleteIfExists(Long id) {
    if (!repository.existsById(id)) {
      return false;
    }
    repository.deleteById(id);
    return true;
  }

  /**
   * Persist the given prescription.
   *
   * @param prescription entity to store
   * @return saved entity
   */
  public Erezept save(Erezept prescription) {
    return repository.save(prescription);
  }

  /**
   * Remove a prescription by its identifier.
   *
   * @param id identifier to delete
   */
  public void deleteById(Long id) {
    repository.deleteById(id);
  }

  /**
   * Check whether a prescription exists for the given identifier.
   *
   * @param id database identifier
   * @return {@code true} if present
   */
  public boolean existsById(Long id) {
    return repository.existsById(id);
  }

  /**
   * Check for the presence of a prescription by business identifier.
   *
   * @param prescriptionId business identifier
   * @return {@code true} if a matching record exists
   */
  public boolean existsByPrescriptionId(String prescriptionId) {
    if (prescriptionId == null) {
      return false;
    }
    return repository.existsByPrescriptionId(prescriptionId);
  }
}
