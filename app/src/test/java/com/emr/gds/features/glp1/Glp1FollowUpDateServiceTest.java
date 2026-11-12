package com.emr.gds.features.glp1;

import com.emr.gds.features.glp1.application.Glp1FollowUpDateService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Glp1FollowUpDateServiceTest {

    @Test
    void calculatesTheSelectedWeeklyInterval() {
        assertEquals(
                LocalDate.of(2026, 3, 27),
                Glp1FollowUpDateService.calculateNextVisit(
                        LocalDate.of(2026, 2, 27), "Every 4 weeks"));
    }

    @Test
    void movesWeekendFollowUpsToMonday() {
        assertEquals(
                LocalDate.of(2026, 8, 31),
                Glp1FollowUpDateService.calculateNextVisit(
                        LocalDate.of(2026, 8, 1), "Every 4 weeks"));
    }

    @Test
    void keepsTheCurrentDateForPrnFollowUp() {
        LocalDate currentDate = LocalDate.of(2026, 2, 27);

        assertEquals(
                currentDate,
                Glp1FollowUpDateService.calculateNextVisit(currentDate, "PRN / as needed"));
    }
}
