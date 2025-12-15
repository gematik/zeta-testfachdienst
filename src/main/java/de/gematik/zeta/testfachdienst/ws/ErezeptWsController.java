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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
@SuppressWarnings("unused") // invoked via STOMP @MessageMapping endpoints
public class ErezeptWsController {

  private static final String EREZEPT_TOPIC_SUFFIX = "/erezept";

  private final ErezeptService service;
  private final SimpMessagingTemplate broker;
  @Value("${server.servlet.context-path:}")
  private String contextPath;

  /**
   * Create a new prescription and broadcast it to all subscribers on /topic/erezept.
   *
   * <p>Validation:</p>
   * <ul>
   *   <li>If an {@code id} is supplied and already exists → 409 CONFLICT</li>
   *   <li>If a {@code prescriptionId} already exists → 409 CONFLICT</li>
   * </ul>
   *
   * <p>On success, sets {@code status=CREATED}, persists with the provided values,
   * then broadcasts the created entity.</p>
   *
   * @param request new prescription payload
   */
  @MessageMapping("erezept.create")
  @SendToUser("/queue/erezept")
  public Erezept create(@Payload @Valid Erezept request) {
    log.info("STOMP erezept.create request received for prescriptionId={}",
        request.getPrescriptionId());
    if (request.getId() != null && service.existsById(request.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "ERezept with id=%d already exists".formatted(request.getId()));
    }
    if (request.getPrescriptionId() != null && service.existsByPrescriptionId(
        request.getPrescriptionId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "ERezept with prescriptionId=%s already exists".formatted(request.getPrescriptionId()));
    }

    var toSave = Erezept.builder()
        .id(null) // let the DB generate it
        .medicationName(request.getMedicationName())
        .dosage(request.getDosage())
        .issuedAt(request.getIssuedAt())
        .expiresAt(request.getExpiresAt())
        .patientId(request.getPatientId())
        .practitionerId(request.getPractitionerId())
        .prescriptionId(request.getPrescriptionId())
        .status(ErezeptStatus.CREATED)
        .build();

    var created = service.save(toSave);

    var broadcastDestination = brokerTopic();
    log.info("STOMP erezept.create persisted id={}, broadcasting to {}", created.getId(),
        broadcastDestination);
    broker.convertAndSend(broadcastDestination, created);
    return created;
  }

  /**
   * Return all prescriptions to the requesting user.
   *
   * <p>Reply destination: /user/queue/erezept</p>
   *
   * @return list of prescriptions
   */
  @MessageMapping("erezept.list")
  @SendToUser("/queue/erezept")
  public List<Erezept> list() {
    log.info("STOMP erezept.list request received");
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
  @MessageMapping("erezept.read.{id}")
  @SendToUser("/queue/erezept")
  public Erezept read(@DestinationVariable Long id) {
    log.info("STOMP erezept.read request received for id={}", id);
    return service.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "ERezept with id=%d not found".formatted(id)));
  }

  /**
   * Update an existing prescription and broadcast the updated entity to /topic/erezept.
   *
   * @param id      identifier of the prescription to update
   * @param request new values (validated)
   */
  @MessageMapping("erezept.update.{id}")
  @SendToUser("/queue/erezept")
  public Erezept update(@DestinationVariable Long id, @Payload @Valid Erezept request) {
    log.info("STOMP erezept.update request received for id={}", id);
    var existing = service.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "ERezept with id=%d not found".formatted(id)));

    if (request.getPrescriptionId() != null
        && !request.getPrescriptionId().equals(existing.getPrescriptionId())
        && service.existsByPrescriptionId(request.getPrescriptionId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "ERezept with prescriptionId=%s already exists".formatted(request.getPrescriptionId()));
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

    var saved = service.save(updated);

    var broadcastDestination = brokerTopic();
    log.info("STOMP erezept.update persisted id={}, broadcasting to {}", updated.getId(),
        broadcastDestination);
    broker.convertAndSend(broadcastDestination, saved);
    return saved;
  }

  /**
   * Delete a prescription by id and return a confirmation response to the user.
   *
   * <p>Reply destination: /user/queue/erezept</p>
   *
   * @param id identifier of the prescription to delete
   * @return confirmation object with id and status
   * @throws ResponseStatusException 404 if not found
   */
  @MessageMapping("erezept.delete.{id}")
  @SendToUser("/queue/erezept")
  public java.util.Map<String, Object> delete(@DestinationVariable Long id) {
    log.info("STOMP erezept.delete request received for id={}", id);
    if (!service.existsById(id)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "ERezept with id=%d not found".formatted(id));
    }
    service.deleteById(id);
    log.info("STOMP erezept.delete removed id={}", id);
    return java.util.Map.of("id", id, "status", "deleted");
  }

  /**
   * Build the broker topic destination, respecting an optional servlet context path.
   *
   * @return topic destination string such as {@code /topic/erezept} or with context prefix
   */
  private String brokerTopic() {
    var destination = "/topic" + EREZEPT_TOPIC_SUFFIX;
    if (contextPath == null || contextPath.isBlank()) {
      return destination;
    }

    String normalizedContext = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    if (normalizedContext.endsWith("/")) {
      normalizedContext = normalizedContext.substring(0, normalizedContext.length() - 1);
    }
    return normalizedContext + destination;
  }
}
