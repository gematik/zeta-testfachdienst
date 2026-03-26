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

package de.gematik.zeta.testfachdienst.controller;

import de.gematik.zeta.testfachdienst.service.SelfDisclosureExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Job controller to initiate job scheduling with jobrunr.
 */
@RestController
@RequestMapping("/jobs")
@Tag(
    name = "Jobs",
    description = "Operational endpoints for background job scheduling and status inspection")
public class JobController {

  private final JobScheduler jobScheduler;
  private final SelfDisclosureExportService selfDisclosureExportService;

  /**
   * Constructor for controller that also initiates job scheduling.
   *
   * @param scheduler Scheduler service from jobrunr
   * @param service Export service that provides method to be run in a job
   */
  public JobController(JobScheduler scheduler, SelfDisclosureExportService service) {
    this.jobScheduler = scheduler;
    this.selfDisclosureExportService = service;
    scheduleInitial(jobScheduler, service);
  }

  /**
   * Register the recurring self-disclosure export job during controller initialization.
   *
   * @param scheduler JobRunr scheduler used to create the recurring job
   * @param service export service invoked by the scheduled job
   */
  private void scheduleInitial(JobScheduler scheduler, SelfDisclosureExportService service) {
    scheduler.createRecurrently(
        RecurringJobBuilder.aRecurringJob()
            .withId("self-disclosure-export")
            .withInterval(Duration.of(service.getExportIntervalInSeconds(), ChronoUnit.SECONDS))
            .withDetails(service::exportSelfDisclosure)
    );
  }

  /**
   * Dummy endpoint to activate controller.
   *
   * @return status string describing the job wiring
   */
  @GetMapping(value = "/info")
  @Operation(
      summary = "Return background job status information",
      description = "Provides a lightweight status payload for the recurring self-disclosure "
          + "export job scheduling setup.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Job status returned successfully",
          content = @Content(
              mediaType = "text/plain",
              schema = @Schema(type = "string"),
              examples = @ExampleObject(value = "{\"status\": \"fantastic!\"}")))
  })
  public String info() {
    return "{\"status\": \"fantastic!\"}";
  }
}
