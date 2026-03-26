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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.model.ErezeptStatus;
import de.gematik.zeta.testfachdienst.repository.ErezeptRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link ErezeptService}. */
@ExtendWith(MockitoExtension.class)
class ErezeptServiceTest {

  @Mock private ErezeptRepository repository;

  @InjectMocks private ErezeptService service;

  private Erezept sample;

  @BeforeEach
  void setUp() {
    sample =
        Erezept.builder()
            .id(42L)
            .medicationName("Ibuprofen")
            .dosage("1x daily")
            .issuedAt(OffsetDateTime.parse("2026-03-09T10:15:30Z"))
            .expiresAt(OffsetDateTime.parse("2026-06-09T10:15:30Z"))
            .patientId("PAT-123")
            .practitionerId("PRAC-456")
            .prescriptionId("RX-123")
            .status(ErezeptStatus.SIGNED)
            .build();
  }

  @Test
  void findAllDelegatesToRepository() {
    when(repository.findAll()).thenReturn(List.of(sample));

    assertThat(service.findAll()).containsExactly(sample);
  }

  @Test
  void findByIdDelegatesToRepository() {
    when(repository.findById(42L)).thenReturn(Optional.of(sample));

    assertThat(service.findById(42L)).contains(sample);
  }

  @Test
  void findByPrescriptionIdDelegatesToRepository() {
    when(repository.findByPrescriptionId("RX-123")).thenReturn(Optional.of(sample));

    assertThat(service.findByPrescriptionId("RX-123")).contains(sample);
  }

  @Test
  void createRejectsDuplicatePrescriptionId() {
    Erezept request = Erezept.builder().prescriptionId("RX-123").build();
    when(repository.existsByPrescriptionId("RX-123")).thenReturn(true);

    assertThat(service.create(request)).isEmpty();
    verify(repository, never()).save(request);
  }

  @Test
  void createPreservesMissingStatus() {
    Erezept request =
        Erezept.builder()
            .prescriptionId("RX-NEW")
            .medicationName("Paracetamol")
            .dosage("2x daily")
            .patientId("PAT-1")
            .practitionerId("PRAC-1")
            .issuedAt(OffsetDateTime.parse("2026-03-09T10:15:30Z"))
            .expiresAt(OffsetDateTime.parse("2026-06-09T10:15:30Z"))
            .status(null)
            .build();
    when(repository.save(request)).thenReturn(request);

    Optional<Erezept> created = service.create(request);

    assertThat(created).contains(request);
    assertThat(request.getStatus()).isNull();
    verify(repository).save(request);
  }

  @Test
  void updateReturnsUpdatedEntityWhenPresent() {
    Erezept update =
        Erezept.builder()
            .medicationName("Updated med")
            .dosage("Updated dosage")
            .expiresAt(OffsetDateTime.parse("2026-07-09T10:15:30Z"))
            .status(ErezeptStatus.DISPENSED)
            .build();
    when(repository.findById(42L)).thenReturn(Optional.of(sample));
    when(repository.save(sample)).thenReturn(sample);

    Optional<Erezept> updated = service.update(42L, update);

    assertThat(updated).contains(sample);
    assertThat(sample.getMedicationName()).isEqualTo("Updated med");
    assertThat(sample.getDosage()).isEqualTo("Updated dosage");
    assertThat(sample.getExpiresAt()).isEqualTo(OffsetDateTime.parse("2026-07-09T10:15:30Z"));
    assertThat(sample.getStatus()).isEqualTo(ErezeptStatus.DISPENSED);
    verify(repository).save(sample);
  }

  @Test
  void updateReturnsEmptyWhenMissing() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThat(service.update(99L, sample)).isEmpty();
    verify(repository, never()).save(sample);
  }

  @Test
  void deleteIfExistsDeletesKnownEntity() {
    when(repository.existsById(42L)).thenReturn(true);

    assertThat(service.deleteIfExists(42L)).isTrue();
    verify(repository).deleteById(42L);
  }

  @Test
  void deleteIfExistsReturnsFalseForUnknownEntity() {
    when(repository.existsById(99L)).thenReturn(false);

    assertThat(service.deleteIfExists(99L)).isFalse();
    verify(repository, never()).deleteById(99L);
  }

  @Test
  void saveDelegatesToRepository() {
    when(repository.save(sample)).thenReturn(sample);

    assertThat(service.save(sample)).isSameAs(sample);
  }

  @Test
  void deleteByIdDelegatesToRepository() {
    service.deleteById(42L);

    verify(repository).deleteById(42L);
  }

  @Test
  void existsByIdDelegatesToRepository() {
    when(repository.existsById(42L)).thenReturn(true);

    assertThat(service.existsById(42L)).isTrue();
  }

  @Test
  void existsByPrescriptionIdReturnsFalseForNullInput() {
    assertThat(service.existsByPrescriptionId(null)).isFalse();
    verify(repository, never()).existsByPrescriptionId(null);
  }

  @Test
  void existsByPrescriptionIdDelegatesForNonNullInput() {
    when(repository.existsByPrescriptionId("RX-123")).thenReturn(true);

    assertThat(service.existsByPrescriptionId("RX-123")).isTrue();
  }
}
