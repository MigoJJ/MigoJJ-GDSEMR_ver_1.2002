package com.emr.gds.features.glp1.application;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Calculates follow-up dates and moves weekend dates to the following business day. */
public final class Glp1FollowUpDateService {

    private Glp1FollowUpDateService() {
    }

    public static LocalDate calculateNextVisit(LocalDate fromDate, String interval) {
        if (fromDate == null || interval == null) {
            return fromDate;
        }

        LocalDate nextVisit = switch (interval) {
            case "Every 4 weeks" -> fromDate.plusWeeks(4);
            case "Every 8 weeks" -> fromDate.plusWeeks(8);
            case "Every 12 weeks" -> fromDate.plusWeeks(12);
            default -> fromDate;
        };

        while (nextVisit.getDayOfWeek() == DayOfWeek.SATURDAY
                || nextVisit.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextVisit = nextVisit.plusDays(1);
        }
        return nextVisit;
    }
}
