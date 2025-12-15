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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.model.ErezeptStatus;
import de.gematik.zeta.testfachdienst.service.ErezeptService;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ErezeptController}.
 */
@ExtendWith(MockitoExtension.class)
class ErezeptControllerTest {

  @Mock
  private ErezeptService service;

  @InjectMocks
  private ErezeptController controller;

  private Erezept sample;

  /**
   * Prepare a reusable sample prescription for test interactions.
   */
  @BeforeEach
  void setUp() {
    sample = Erezept.builder()
        .id(42L)
        .prescriptionId("RX-0042")
        .patientId("PT-17")
        .practitionerId("PR-21")
        .medicationName("Ibuprofen")
        .dosage("200mg")
        .issuedAt(OffsetDateTime.now().minusDays(2))
        .expiresAt(OffsetDateTime.now().plusDays(5))
        .status(ErezeptStatus.SIGNED)
        .build();
  }

  /**
   * Verifies that the list endpoint returns all prescriptions from the repository.
   */
  @Test
  void list_returnsAllPrescriptions() {
    when(service.findAll()).thenReturn(List.of(sample));

    var result = controller.list();

    assertThat(result).containsExactly(sample);
  }

  /**
   * Ensures that fetching by identifier returns an entity when it exists.
   */
  @Test
  void get_returnsEntityWhenPresent() {
    when(service.findById(42L)).thenReturn(Optional.of(sample));

    var response = controller.get(42L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(sample);
  }

  /**
   * Confirms that fetching a non-existent prescription yields a 404 response.
   */
  @Test
  void get_returnsNotFoundWhenMissing() {
    when(service.findById(99L)).thenReturn(Optional.empty());

    var response = controller.get(99L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * Ensures a known prescriptionId is resolved to the corresponding entity.
   */
  @Test
  void byPrescriptionId_returnsEntityWhenPresent() {
    when(service.findByPrescriptionId("RX-0042")).thenReturn(Optional.of(sample));

    var response = controller.byPrescriptionId("RX-0042");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(sample);
  }

  /**
   * Confirms that missing prescriptionIds result in a 404 response.
   */
  @Test
  void byPrescriptionId_returnsNotFoundWhenMissing() {
    when(service.findByPrescriptionId("missing")).thenReturn(Optional.empty());

    var response = controller.byPrescriptionId("missing");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * Verifies creating a prescription succeeds when the identifier is unique.
   */
  @Test
  void create_persistsWhenPrescriptionIdUnique() {
    var toPersist = Erezept.builder().id(null).prescriptionId("RX-NEW").build();
    var created = Erezept.builder().id(1L).prescriptionId("RX-NEW").build();
    when(service.create(toPersist)).thenReturn(Optional.of(created));

    var response = controller.create(toPersist);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/erezept/1"));
    assertThat(response.getBody()).isEqualTo(created);
    verify(service).create(toPersist);
  }

  /**
   * Verifies the Location header includes the servlet context path when configured.
   */
  @Test
  void create_includesContextPathInLocation() {
    var toPersist = Erezept.builder().id(null).prescriptionId("RX-NEW").build();
    var created = Erezept.builder().id(1L).prescriptionId("RX-NEW").build();
    when(service.create(toPersist)).thenReturn(Optional.of(created));
    ReflectionTestUtils.setField(controller, "servletContextPath", "/achelos_testfachdienst");

    var response = controller.create(toPersist);

    assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/achelos_testfachdienst/api/erezept/1"));
    assertThat(response.getBody()).isEqualTo(created);
    verify(service).create(toPersist);
  }

  /**
   * Validates that updating a stored prescription applies the provided changes.
   */
  @Test
  void update_modifiesEntityWhenPresent() {
    var updatePayload = Erezept.builder().medicationName("Updated med").build();
    sample.setMedicationName("Updated med");
    when(service.update(42L, updatePayload)).thenReturn(Optional.of(sample));

    var response = controller.update(42L, updatePayload);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(sample);
    verify(service).update(42L, updatePayload);
  }

  /**
   * Confirms update requests for unknown ids return a 404 response.
   */
  @Test
  void update_returnsNotFoundWhenEntityMissing() {
    var updatePayload = Erezept.builder().medicationName("Updated").build();
    when(service.update(42L, updatePayload)).thenReturn(Optional.empty());

    var response = controller.update(42L, updatePayload);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /**
   * Ensures deleting a known prescription removes it and returns 204.
   */
  @Test
  void delete_removesEntityWhenPresent() {
    when(service.deleteIfExists(42L)).thenReturn(true);

    var response = controller.delete(42L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(service).deleteIfExists(42L);
  }

  /**
   * Checks that deleting an unknown prescription results in a 404 response.
   */
  @Test
  void delete_returnsNotFoundWhenMissing() {
    when(service.deleteIfExists(42L)).thenReturn(false);

    var response = controller.delete(42L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
