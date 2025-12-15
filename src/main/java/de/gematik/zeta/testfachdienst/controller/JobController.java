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

import de.gematik.zeta.testfachdienst.service.SelfDisclosureExportService;
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
public class JobController {

  private JobScheduler jobScheduler;
  private SelfDisclosureExportService selfDisclosureExportService;

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
   * @return Constant string
   */
  @GetMapping(value = "/info")
  public String info() {
    return "{\"status\": \"fantastic!\"}";
  }
}
