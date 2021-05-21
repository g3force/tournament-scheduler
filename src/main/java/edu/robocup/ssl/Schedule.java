package edu.robocup.ssl;

import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class Schedule {
    Map<String, MatchAssignment> assignmentMap;

    public Schedule(Map<String, MatchAssignment> assignmentMap) {
        this.assignmentMap = Collections.unmodifiableMap(assignmentMap);
    }

    public Schedule withNewAssignment(MatchAssignment matchAssignment) {
        Map<String, MatchAssignment> newAssignment = new HashMap<>(assignmentMap);
        newAssignment.put(matchAssignment.getMatch().getName(), matchAssignment);
        return new Schedule(newAssignment);
    }

    public int findMaxEndTime() {
        return assignmentMap.values().stream()
                .mapToInt(MatchAssignment::getStartTime)
                .max()
                .orElseThrow();
    }

    public int findMaxNumFields() {
        return assignmentMap.values().stream()
                .collect(Collectors.groupingBy(MatchAssignment::getStartTime))
                .values()
                .stream()
                .mapToInt(Collection::size)
                .max()
                .orElseThrow();
    }

    public void print() {
        assignmentMap.values().stream()
                .filter(m -> m.getStartTime() != null)
                .sorted(Comparator.comparing(MatchAssignment::getStartTime))
                .forEach(m -> System.out.printf("%6s: %3d|%2d|%2d %20s vs. %-20s %02d-%02d | %02d-%02d%n",
                        m.getMatch().getName(),
                        m.getStartTime(),
                        m.getStartTime() / 24,
                        m.getStartTime() % 24,
                        m.getTeams().get(0).getName(),
                        m.getTeams().get(1).getName(),
                        m.getTeams().get(0).getAvailStart(),
                        m.getTeams().get(0).getAvailEnd(),
                        m.getTeams().get(1).getAvailStart(),
                        m.getTeams().get(1).getAvailEnd()));
    }
}
