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

package de.gematik.zeta.testfachdienst.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.model.ErezeptStatus;
import de.gematik.zeta.testfachdienst.service.ErezeptService;
import de.gematik.zeta.testfachdienst.ws.model.ErezeptDeleteResponse;
import de.gematik.zeta.testfachdienst.ws.model.ErezeptListResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for {@link ErezeptWsController}. */
@ExtendWith(MockitoExtension.class)
class ErezeptWsControllerTest {

  @Mock private ErezeptService service;

  @Mock private SimpMessagingTemplate broker;

  @InjectMocks private ErezeptWsController controller;

  private Erezept request;
  private Erezept persisted;

  @BeforeEach
  void setUp() {
    request =
        Erezept.builder()
            .id(99L)
            .medicationName("Ibuprofen")
            .dosage("1x daily")
            .issuedAt(OffsetDateTime.parse("2026-03-09T10:15:30Z"))
            .expiresAt(OffsetDateTime.parse("2026-06-09T10:15:30Z"))
            .patientId("PAT-123")
            .practitionerId("PRAC-456")
            .prescriptionId("RX-123")
            .status(ErezeptStatus.SIGNED)
            .build();
    persisted =
        Erezept.builder()
            .id(42L)
            .medicationName(request.getMedicationName())
            .dosage(request.getDosage())
            .issuedAt(request.getIssuedAt())
            .expiresAt(request.getExpiresAt())
            .patientId(request.getPatientId())
            .practitionerId(request.getPractitionerId())
            .prescriptionId(request.getPrescriptionId())
            .status(ErezeptStatus.CREATED)
            .build();
  }

  @Test
  void createRejectsKnownId() {
    when(service.existsById(99L)).thenReturn(true);

    assertThatThrownBy(() -> controller.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409 CONFLICT");

    verify(service, never()).save(request);
  }

  @Test
  void createRejectsDuplicatePrescriptionId() {
    request.setId(null);
    when(service.existsByPrescriptionId("RX-123")).thenReturn(true);

    assertThatThrownBy(() -> controller.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("prescriptionId=RX-123 already exists");

    verify(service, never()).save(request);
  }

  @Test
  void createPersistsCreatedStatusAndBroadcastsToDefaultTopic() {
    request.setId(null);
    when(service.save(org.mockito.ArgumentMatchers.any(Erezept.class))).thenReturn(persisted);

    Erezept created = controller.create(request);

    assertThat(created).isSameAs(persisted);
    verify(service).save(org.mockito.ArgumentMatchers.argThat(candidate ->
        candidate.getId() == null
            && candidate.getStatus() == ErezeptStatus.CREATED
            && candidate.getIssuedAt().equals(request.getIssuedAt())
            && candidate.getPrescriptionId().equals("RX-123")));
    verify(broker).convertAndSend("/topic/erezept", persisted);
  }

  @Test
  void brokerTopicIncludesNormalizedContextPath() {
    ReflectionTestUtils.setField(controller, "contextPath", "/achelos_testfachdienst");

    String topic = ReflectionTestUtils.invokeMethod(controller, "brokerTopic");

    assertThat(topic).isEqualTo("/achelos_testfachdienst/topic/erezept");
  }

  @Test
  void listDelegatesToService() {
    when(service.findAll()).thenReturn(List.of(persisted));

    ErezeptListResponse response = controller.list();

    assertThat(response.items()).containsExactly(persisted);
  }

  @Test
  void readReturnsKnownPrescription() {
    when(service.findById(42L)).thenReturn(Optional.of(persisted));

    assertThat(controller.read(42L)).isSameAs(persisted);
  }

  @Test
  void readRejectsUnknownPrescription() {
    when(service.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.read(42L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("id=42 not found");
  }

  @Test
  void updateRejectsUnknownPrescription() {
    when(service.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.update(42L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("id=42 not found");
  }

  @Test
  void updateRejectsDuplicateChangedPrescriptionId() {
    Erezept existing = Erezept.builder().id(42L).prescriptionId("RX-OLD").build();
    Erezept update = Erezept.builder().prescriptionId("RX-NEW").build();
    when(service.findById(42L)).thenReturn(Optional.of(existing));
    when(service.existsByPrescriptionId("RX-NEW")).thenReturn(true);

    assertThatThrownBy(() -> controller.update(42L, update))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("prescriptionId=RX-NEW already exists");
  }

  @Test
  void updatePreservesIssuedAtAndExistingStatusWhenMissingInRequest() {
    Erezept existing =
        Erezept.builder()
            .id(42L)
            .issuedAt(OffsetDateTime.parse("2026-01-01T10:15:30Z"))
            .status(ErezeptStatus.DISPENSED)
            .prescriptionId("RX-123")
            .build();
    Erezept update =
        Erezept.builder()
            .medicationName("Updated")
            .dosage("2x daily")
            .expiresAt(OffsetDateTime.parse("2026-07-09T10:15:30Z"))
            .patientId("PAT-999")
            .practitionerId("PRAC-888")
            .prescriptionId("RX-123")
            .build();
    Erezept saved =
        Erezept.builder()
            .id(42L)
            .issuedAt(existing.getIssuedAt())
            .status(ErezeptStatus.DISPENSED)
            .medicationName("Updated")
            .dosage("2x daily")
            .expiresAt(update.getExpiresAt())
            .patientId("PAT-999")
            .practitionerId("PRAC-888")
            .prescriptionId("RX-123")
            .build();
    ReflectionTestUtils.setField(controller, "contextPath", "achelos_testfachdienst/");
    when(service.findById(42L)).thenReturn(Optional.of(existing));
    when(service.save(org.mockito.ArgumentMatchers.any(Erezept.class))).thenReturn(saved);

    Erezept result = controller.update(42L, update);

    assertThat(result).isSameAs(saved);
    ArgumentCaptor<Erezept> savedPrescription = ArgumentCaptor.forClass(Erezept.class);
    verify(service).save(savedPrescription.capture());
    assertThat(savedPrescription.getValue().getId()).isEqualTo(42L);
    assertThat(savedPrescription.getValue().getIssuedAt()).isEqualTo(existing.getIssuedAt());
    assertThat(savedPrescription.getValue().getMedicationName()).isEqualTo("Updated");
    assertThat(savedPrescription.getValue().getPrescriptionId()).isEqualTo("RX-123");
    verify(broker).convertAndSend("/achelos_testfachdienst/topic/erezept", saved);
  }

  @Test
  void deleteRemovesKnownPrescription() {
    when(service.existsById(42L)).thenReturn(true);

    ErezeptDeleteResponse result = controller.delete(42L);

    assertThat(result.id()).isEqualTo(42L);
    assertThat(result.status()).isEqualTo("deleted");
    verify(service).deleteById(42L);
  }

  @Test
  void deleteRejectsUnknownPrescription() {
    when(service.existsById(42L)).thenReturn(false);

    assertThatThrownBy(() -> controller.delete(42L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("id=42 not found");
  }
}
