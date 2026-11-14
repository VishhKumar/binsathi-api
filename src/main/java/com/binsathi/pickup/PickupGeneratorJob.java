package com.binsathi.pickup;

import com.binsathi.schedule.Schedule;
import com.binsathi.schedule.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Every night at 00:30 India time, creates the next 7 days of pickups for all active schedules. */
@Component
public class PickupGeneratorJob {

    private static final Logger log = LoggerFactory.getLogger(PickupGeneratorJob.class);

    private final ScheduleRepository schedules;
    private final PickupGenerator generator;

    public PickupGeneratorJob(ScheduleRepository schedules, PickupGenerator generator) {
        this.schedules = schedules;
        this.generator = generator;
    }

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kolkata")
    public void run() {
        LocalDate today = PickupService.todayIst();
        int total = 0;
        for (Schedule s : schedules.findByActiveTrue()) {
            try {
                total += generator.generateForSchedule(s.getId(), today, 7);
            } catch (Exception e) {
                log.error("Pickup generation failed for schedule {}", s.getId(), e);
            }
        }
        log.info("Nightly pickup generation created {} pickups", total);
    }
}