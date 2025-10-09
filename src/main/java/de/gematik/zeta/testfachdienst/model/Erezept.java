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
package de.gematik.zeta.testfachdienst.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity that represents an electronic prescription with lifecycle metadata.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "erezept")
@Schema(name = "ERezept", description = "A prescription (ERezept)")
public class Erezept {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "Unique identifier", example = "123", accessMode = AccessMode.READ_ONLY)
  private Long id;

  @NotBlank
  @Column(nullable = false, length = 128)
  @Schema(description = "Medication name", example = "Ibuprofen 400 mg")
  private String medicationName;

  @NotBlank
  @Column(nullable = false, length = 256)
  @Schema(description = "Dosage instructions", example = "1 tablet, 3× daily after meals")
  private String dosage;

  @PastOrPresent
  @Column(nullable = false)
  @Schema(description = "When it was issued (ISO-8601)", example = "2025-09-22T10:30:00Z",
      format = "date-time")
  private OffsetDateTime issuedAt;

  @FutureOrPresent
  @Schema(description = "When it expires (ISO-8601)", example = "2025-12-31T23:59:59Z",
      format = "date-time")
  private OffsetDateTime expiresAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  @Builder.Default
  @Schema(description = "Current status")
  private ErezeptStatus status = ErezeptStatus.CREATED;

  @NotBlank
  @Column(nullable = false, length = 64)
  @Schema(description = "FHIR/PKV patient identifier", example = "PAT-123456")
  private String patientId;

  @NotBlank
  @Column(nullable = false, length = 64)
  @Schema(description = "Identifier of prescribing practitioner", example = "PRAC-98765")
  private String practitionerId;

  @NotBlank
  @Column(nullable = false, length = 64, unique = true)
  @Schema(description = "Prescription identifier", example = "RX-2025-000123")
  private String prescriptionId;
}
