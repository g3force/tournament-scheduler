package edu.robocup.ssl;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Evaluator {
    private final long start = System.nanoTime();
    private long lastStart = start;

    @Getter
    private int numTournaments;
    @Getter
    private int overallMaxEndTime;
    @Getter
    private int overallMaxNumFields;

    @Setter
    private int printStatsEvery = 10;
    @Setter
    private boolean validate = false;
    @Setter
    private boolean findMaxEndTime = true;
    @Setter
    private boolean findMaxNumFields = false;


    public void process(List<Schedule> allSchedules) {
        numTournaments++;

        if (findMaxNumFields) {
            updateMaxNumFields(allSchedules);
        }

        if (findMaxEndTime) {
            updateMaxEndTime(allSchedules);
        }

        if (validate) {
            allSchedules.forEach(this::validateSchedule);
        }

        printStatistics();
    }

    private void printStatistics() {
        if (numTournaments % printStatsEvery == 0) {
            long now = System.nanoTime();
            long totalElapsed = (long) ((now - start) / 1e6);
            long elapsed = (long) ((now - lastStart) / 1e6);
            double avgSpeed = totalElapsed / (double) numTournaments;
            double speed = elapsed / (double) printStatsEvery;
            System.out.printf("%6d: %d (%.1f|%.1f)%n", totalElapsed, numTournaments, avgSpeed, speed);
            lastStart = now;
        }
    }

    private void updateMaxNumFields(List<Schedule> allSchedules) {
        var maxNumFields = allSchedules.stream()
                .mapToInt(Schedule::findMaxNumFields)
                .max()
                .orElse(0);
        if (maxNumFields > this.overallMaxNumFields) {
            overallMaxNumFields = maxNumFields;
        }
    }

    private void updateMaxEndTime(List<Schedule> allSchedules) {
        var maxEndTime = allSchedules.stream()
                .mapToInt(Schedule::findMaxEndTime).max()
                .orElse(0);
        if (overallMaxEndTime > maxEndTime) {
            overallMaxEndTime = maxEndTime;
            System.out.println("maxEnd: " + maxEndTime);
        }
    }

    private void validateSchedule(Schedule schedule) {
        Map<Integer, Set<Team>> foo = new HashMap<>();
        schedule.getAssignmentMap().forEach((k, v) -> {
            var bar = foo.computeIfAbsent(v.getStartTime(), a -> new HashSet<>());
            v.getTeams().forEach(t -> {
                if (!bar.add(t)) {
                    throw new IllegalStateException("Duplicate team: " + t + "(" + schedule + ")");
                }
            });
        });
    }

    public void summary() {
        System.out.println("Num tournaments: " + numTournaments);
        System.out.println("Max end time: " + overallMaxEndTime);
        System.out.println("Max fields: " + overallMaxNumFields);
    }
}
