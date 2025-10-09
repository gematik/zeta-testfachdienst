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
package de.gematik.zeta.testfachdienst.repository;

import de.gematik.zeta.testfachdienst.model.Erezept;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository abstraction for persisting and querying {@link Erezept} entities.
 */
public interface ErezeptRepository extends JpaRepository<Erezept, Long> {

  /**
   * Locate a prescription by its external identifier.
   *
   * @param prescriptionId unique business identifier of the prescription
   * @return optional containing the entity when found
   */
  Optional<Erezept> findByPrescriptionId(String prescriptionId);

  /**
   * Determine whether a prescription with the given external identifier exists.
   *
   * @param prescriptionId unique business identifier of the prescription
   * @return {@code true} if a matching record exists, {@code false} otherwise
   */
  boolean existsByPrescriptionId(String prescriptionId);
}

