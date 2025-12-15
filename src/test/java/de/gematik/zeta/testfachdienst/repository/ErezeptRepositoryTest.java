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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.model.ErezeptStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Persistence tests for {@link ErezeptRepository}.
 */
@DataJpaTest
class ErezeptRepositoryTest {

  @Autowired
  private ErezeptRepository repository;

  /**
   * Verifies that the repository retrieves a persisted entity by its prescriptionId.
   */
  @Test
  @DisplayName("findByPrescriptionId returns matching entity")
  void findByPrescriptionId_returnsEntity() {
    var saved = repository.save(Erezept.builder()
        .prescriptionId("RX-001")
        .patientId("PT-1")
        .practitionerId("PR-1")
        .medicationName("Ibuprofen")
        .dosage("200mg")
        .issuedAt(OffsetDateTime.now().minusDays(1))
        .expiresAt(OffsetDateTime.now().plusDays(10))
        .status(ErezeptStatus.SIGNED)
        .build());

    assertThat(repository.findByPrescriptionId("RX-001")).contains(saved);
  }

  /**
   * Confirms the repository correctly reports whether a prescriptionId already exists.
   */
  @Test
  @DisplayName("existsByPrescriptionId reflects persistence state")
  void existsByPrescriptionId_detectsPresence() {
    repository.save(Erezept.builder()
        .prescriptionId("RX-002")
        .patientId("PT-2")
        .practitionerId("PR-2")
        .medicationName("Paracetamol")
        .dosage("500mg")
        .issuedAt(OffsetDateTime.now().minusDays(1))
        .expiresAt(OffsetDateTime.now().plusDays(5))
        .status(ErezeptStatus.CREATED)
        .build());

    assertThat(repository.existsByPrescriptionId("RX-002")).isTrue();
    assertThat(repository.existsByPrescriptionId("RX-unknown")).isFalse();
  }
}

