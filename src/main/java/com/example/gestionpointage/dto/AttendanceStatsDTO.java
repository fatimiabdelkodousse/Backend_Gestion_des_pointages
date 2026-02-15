package com.example.gestionpointage.dto;

public class AttendanceStatsDTO {

    private long total;
    private long present;
    private long early;
    private long onTime;
    private long late;
    private long absent;

    public AttendanceStatsDTO(
            long total,
            long present,
            long early,
            long onTime,
            long late,
            long absent
    ) {
        this.total = total;
        this.present = present;
        this.early = early;
        this.onTime = onTime;
        this.late = late;
        this.absent = absent;
    }

    public long getTotal() { return total; }
    public long getPresent() { return present; }
    public long getEarly() { return early; }
    public long getOnTime() { return onTime; }
    public long getLate() { return late; }
    public long getAbsent() { return absent; }
}
