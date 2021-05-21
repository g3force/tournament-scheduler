package edu.robocup.ssl;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Evaluator {
    private final long start = System.nanoTime();
    private long lastStart = start;
    private int lastNumTournaments;

    @Getter
    private final AtomicInteger numTournaments = new AtomicInteger();
    @Getter
    private final AtomicInteger overallMaxEndTime = new AtomicInteger();
    @Getter
    private final AtomicInteger overallMaxNumFields = new AtomicInteger();

    @Setter
    private boolean validate = false;
    @Setter
    private boolean findMaxEndTime = true;
    @Setter
    private boolean findMaxNumFields = false;


    public void process(List<Schedule> allSchedules) {
        numTournaments.incrementAndGet();

        if (findMaxNumFields) {
            updateMaxNumFields(allSchedules);
        }

        if (findMaxEndTime) {
            updateMaxEndTime(allSchedules);
        }

        if (validate) {
            allSchedules.forEach(this::validateSchedule);
        }
    }

    public void printStatistics() {
        int n = numTournaments.get();
        long now = System.nanoTime();
        long totalElapsed = (long) ((now - start) / 1e6);
        double elapsed = ((now - lastStart) / 1e9);
        int tournamentsProcessed = n - lastNumTournaments;
        double speed = tournamentsProcessed / elapsed;
        System.out.printf("%6d: %d (%4.1f/s) | %d%n",
                totalElapsed,
                n,
                speed,
                overallMaxEndTime.get());
        lastStart = now;
        lastNumTournaments = n;
    }

    private void updateMaxNumFields(List<Schedule> allSchedules) {
        var maxNumFields = allSchedules.stream()
                .mapToInt(Schedule::findMaxNumFields)
                .max()
                .orElse(0);
        overallMaxNumFields.accumulateAndGet(maxNumFields, Math::max);
    }

    private void updateMaxEndTime(List<Schedule> allSchedules) {
        var maxEndTime = allSchedules.stream()
                .mapToInt(Schedule::findMaxEndTime).max()
                .orElse(0);
        overallMaxEndTime.accumulateAndGet(maxEndTime, Math::max);
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
        System.out.println("Num tournaments: " + numTournaments.get());
        System.out.println("Max end time: " + overallMaxEndTime.get());
        System.out.println("Max fields: " + overallMaxNumFields.get());
    }
}
