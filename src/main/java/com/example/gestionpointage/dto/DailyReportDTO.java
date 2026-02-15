package com.example.gestionpointage.dto;

import java.time.LocalDate;

public class DailyReportDTO {

    private LocalDate date;
    private long totalMinutes;
    private long totalHours;
    private long remainingMinutes;
    private String formattedDuration;

    public DailyReportDTO(LocalDate date, long totalMinutes) {
        this.date = date;
        this.totalMinutes = totalMinutes;

        this.totalHours = totalMinutes / 60;
        this.remainingMinutes = totalMinutes % 60;

        this.formattedDuration =
                totalHours + "h " + remainingMinutes + "min";
    }

    public LocalDate getDate() { return date; }
    public long getTotalMinutes() { return totalMinutes; }
    public long getTotalHours() { return totalHours; }
    public long getRemainingMinutes() { return remainingMinutes; }
    public String getFormattedDuration() { return formattedDuration; }
}
