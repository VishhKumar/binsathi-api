package com.binsathi.dashboard;

public record DashboardResponse(long activeHouseholds, long residents, long collectors,
                                long todayScheduled, long todayCompleted, long todaySkipped,
                                long weekCompleted, long weekSkipped, long weekPending,
                                Integer completionPercent, long openComplaints) {
}