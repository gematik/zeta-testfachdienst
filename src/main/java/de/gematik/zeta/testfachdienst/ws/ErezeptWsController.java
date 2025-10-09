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
package de.gematik.zeta.testfachdienst.ws;

import de.gematik.zeta.testfachdienst.model.Erezept;
import de.gematik.zeta.testfachdienst.model.ErezeptStatus;
import de.gematik.zeta.testfachdienst.service.ErezeptService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

/**
 * <h2>STOMP WebSocket API for ERezept.</h2>
 *
 * <p>This controller exposes CRUD-style messaging endpoints over STOMP/WebSocket.
 * Clients send to application destinations (prefix <code>/app</code>) and receive:</p>
 * <ul>
 *   <li>broadcast updates on <b>/topic/erezept</b> (create/update)</li>
 *   <li>personal replies on <b>/user/queue/erezept</b> (list/read/delete)</li>
 * </ul>
 *
 * <h3>Client → Server destinations</h3>
 * <ul>
 *   <li><b>create</b> — payload: {@link Erezept}</li>
 *   <li><b>list</b> — no payload</li>
 *   <li><b>read.{id}</b> — no payload</li>
 *   <li><b>update.{id}</b> — payload: {@link Erezept}</li>
 *   <li><b>delete.{id}</b> — no payload</li>
 * </ul>
 *
 * <p>Server broadcasts created/updated entities to <b>/topic/erezept</b>.
 * For list/read/delete, the server replies directly to the caller at
 * <b>/user/queue/erezept</b>.</p>
 */
@Controller
@RequiredArgsConstructor
public class ErezeptWsController {

  private final ErezeptService service;
  private final SimpMessagingTemplate broker;

  /**
   * Create a new prescription and broadcast it to all subscribers on /topic/erezept.
   *
   * <p>Validation:</p>
   * <ul>
   *   <li>If an {@code id} is supplied and already exists → 409 CONFLICT</li>
   *   <li>If a {@code prescriptionId} already exists → 409 CONFLICT</li>
   * </ul>
   *
   * <p>On success, sets {@code status=CREATED} and {@code issuedAt=now}, persists,
   * then broadcasts the created entity.</p>
   *
   * @param request new prescription payload
   */
  @MessageMapping("create")
  public void create(@Payload @Valid Erezept request) {
    if (request.getId() != null && service.existsById(request.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "ERezept with id=%d already exists".formatted(request.getId()));
    }
    if (request.getPrescriptionId() != null && service.existsByPrescriptionId(
        request.getPrescriptionId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "ERezept already exists for prescriptionId=%s".formatted(request.getPrescriptionId()));
    }

    var toSave = Erezept.builder()
        .id(null) // let the DB generate it
        .medicationName(request.getMedicationName())
        .dosage(request.getDosage())
        .issuedAt(OffsetDateTime.now())
        .expiresAt(request.getExpiresAt())
        .patientId(request.getPatientId())
        .practitionerId(request.getPractitionerId())
        .prescriptionId(request.getPrescriptionId())
        .status(ErezeptStatus.CREATED)
        .build();

    var created = service.save(toSave);

    broker.convertAndSend("/topic/erezept", created);
  }

  /**
   * Return all prescriptions to the requesting user.
   *
   * <p>Reply destination: /user/queue/erezept</p>
   *
   * @return list of prescriptions
   */
  @MessageMapping("list")
  @SendToUser("/queue/erezept")
  public List<Erezept> list() {
    return service.findAll();
  }

  /**
   * Read a single prescription by id and return it to the requesting user.
   *
   * <p>Reply destination: /user/queue/erezept</p>
   *
   * @param id identifier of the prescription
   * @return the found prescription
   * @throws ResponseStatusException 404 if not found
   */
  @MessageMapping("read.{id}")
  @SendToUser("/queue/erezept")
  public Erezept read(@DestinationVariable Long id) {
    return service.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "ERezept not found for id=" + id));
  }

  /**
   * Update an existing prescription and broadcast the updated entity to /topic/erezept.
   *
   * @param id      identifier of the prescription to update
   * @param request new values (validated)
   */
  @MessageMapping("update.{id}")
  public void update(@DestinationVariable Long id, @Payload @Valid Erezept request) {
    var existing = service.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "ERezept not found for id=" + id));

    if (request.getPrescriptionId() != null
        && !request.getPrescriptionId().equals(existing.getPrescriptionId())
        && service.existsByPrescriptionId(request.getPrescriptionId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "ERezept already exists for prescriptionId=%s".formatted(request.getPrescriptionId()));
    }

    var updated = Erezept.builder()
        .id(existing.getId())
        .medicationName(request.getMedicationName())
        .dosage(request.getDosage())
        .issuedAt(existing.getIssuedAt())        // preserve original issue date
        .expiresAt(request.getExpiresAt())
        .patientId(request.getPatientId())
        .practitionerId(request.getPractitionerId())
        .prescriptionId(request.getPrescriptionId())
        .status(request.getStatus() != null ? request.getStatus() : existing.getStatus())
        .build();

    service.save(updated);

    broker.convertAndSend("/topic/erezept", updated);
  }

  /**
   * Delete a prescription by id and return a simple acknowledgement to the user.
   *
   * <p>Reply destination: /user/queue/erezept</p>
   *
   * @param id identifier of the prescription to delete
   * @return textual acknowledgement: "deleted" or "not_found"
   */
  @MessageMapping("delete.{id}")
  @SendToUser("/queue/erezept")
  public String delete(@DestinationVariable Long id) {
    if (service.existsById(id)) {
      service.deleteById(id);
      return "deleted";
    }
    return "not_found";
  }
}
