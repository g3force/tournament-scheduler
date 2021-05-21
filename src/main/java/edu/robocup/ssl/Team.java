package edu.robocup.ssl;

import lombok.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class Team {
    String name;
    int availStart;
    int availEnd;

    public int nextAvailableStart(int start) {
        int dayStart = start % 24;
        int days = start / 24;
        if (dayStart >= availStart) {
            // after or at start
            if (dayStart < availEnd || availStart > availEnd) {
                // before end
                return start;
            }
            // after or at end
            if (availStart < availEnd) {
                return (days + 1) * 24 + availStart;
            }
            return days * 24 + availStart;
        }
        // before start
        if (dayStart < availEnd && availStart > availEnd) {
            return start;
        }
        return days * 24 + availStart;
    }

    public static List<Team> loadTeams(Path csvFile) throws IOException {
        return Files.readAllLines(csvFile).stream()
                .filter(l -> !l.startsWith("#"))
                .map(l -> l.split(","))
                .map(f -> new Team(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2])))
                .collect(Collectors.toList());
    }
}
