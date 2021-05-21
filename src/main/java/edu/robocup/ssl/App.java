package edu.robocup.ssl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class App {
    private final List<Team> teams;
    private final Scheduler schedulerUpper;
    private final Scheduler schedulerLower;
    private final long start = System.nanoTime();
    private long lastStart = start;
    private boolean validate = false;
    private int numSchedules;
    private int maxEndTime;
    private int overallMaxNumFields;

    public App(int initialStart) throws IOException {
        teams = loadTeams(Paths.get("input.csv"));
        schedulerUpper = Scheduler.load(Path.of("DD8.puml"), initialStart);
        schedulerLower = Scheduler.load(Path.of("DD8.puml"), initialStart);
    }

    private static List<Team> loadTeams(Path csvFile) throws IOException {
        return Files.readAllLines(csvFile).stream()
                .filter(l -> !l.startsWith("#"))
                .map(l -> l.split(","))
                .map(f -> new Team(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2])))
                .collect(Collectors.toList());
    }


    private Map<Match, List<Team>> calcTeamAssignment(List<Integer> teamIds) {
        var teamDemand = schedulerUpper.getTeamDemand();

        List<Integer> remainingIds = new ArrayList<>(teamIds);
        Map<Match, List<Team>> assignment = new HashMap<>();
        for (var entry : teamDemand.entrySet()) {
            var mappedTeams = assignment.computeIfAbsent(entry.getKey(), m -> new ArrayList<>());
            for (int i = 0; i < entry.getValue(); i++) {
                mappedTeams.add(teams.get(remainingIds.remove(0)));
            }
        }
        return assignment;
    }


    private List<Map<Match, List<Team>>> calcTeamAssignmentCombinations(List<Integer> teamIds) {
        var teamDemand = schedulerUpper.getTeamDemand();

        List<Integer> remainingIds = new ArrayList<>(teamIds);
        Map<Match, List<Team>> fixedAssignment = new HashMap<>();
        for (var entry : teamDemand.entrySet()) {
            if (entry.getValue() == 2) {
                var mappedTeams = fixedAssignment.computeIfAbsent(entry.getKey(), m -> new ArrayList<>());
                mappedTeams.add(teams.get(remainingIds.remove(0)));
            }
        }

        List<Map<Match, List<Team>>> result = new ArrayList<>();
        PermutationIterator<Integer> iterator = new PermutationIterator<>(remainingIds);
        while (iterator.hasNext()) {
            Map<Match, List<Team>> assignment = new HashMap<>();
            fixedAssignment.forEach((match, teamList) -> assignment.put(match, new ArrayList<>(teamList)));
            var idsRemaining = new ArrayList<>(iterator.next());
            for (var entry : teamDemand.entrySet()) {
                var mappedTeams = assignment.computeIfAbsent(entry.getKey(), m -> new ArrayList<>());
                mappedTeams.add(teams.get(idsRemaining.remove(0)));
            }
            result.add(assignment);
        }

        return result;
    }

    private void runAllTournamentCombinations(List<Integer> teamIds) {
        var combinations = calcTeamAssignmentCombinations(teamIds);

        for (var combination : combinations) {
            runTournament(combination);
        }
    }

    private void runTournament(Map<Match, List<Team>> teamMapping) {
        var allScheduled = schedulerUpper.run(teamMapping);

        var maxDuration = allScheduled.stream()
                .mapToInt(m -> m.values().stream().mapToInt(MatchAssignment::getStartTime).max().orElseThrow()).max()
                .orElseThrow();

        var maxNumFields = allScheduled.stream()
                .mapToInt(m -> m.values().stream()
                        .collect(Collectors.groupingBy(MatchAssignment::getStartTime))
                        .values()
                        .stream()
                        .mapToInt(Collection::size)
                        .max()
                        .orElseThrow())
                .max()
                .orElseThrow();

        if (maxDuration > maxEndTime) {
            maxEndTime = maxDuration;
            System.out.println("maxEnd: " + maxEndTime);
        }

        if (validate) {
            if (maxNumFields > this.overallMaxNumFields) {
                overallMaxNumFields = maxNumFields;
                System.out.println("Max fields: " + overallMaxNumFields);
            }

            allScheduled.forEach(this::validateSchedule);
        }

        numSchedules++;
        int m = 10;
        if (numSchedules % m == 0) {
            long now = System.nanoTime();
            long totalElapsed = (long) ((now - start) / 1e6);
            long elapsed = (long) ((now - lastStart) / 1e6);
            double avgSpeed = totalElapsed / (double) numSchedules;
            double speed = elapsed / (double) m;
            System.out.printf("%6d: %d (%.1f|%.1f)%n", totalElapsed, numSchedules, avgSpeed, speed);
            lastStart = now;
        }
    }

    public void summary() {
        System.out.println("Num schedules: " + numSchedules);
        System.out.println("max end time: " + maxEndTime);
        if (validate) {
            System.out.println("Max fields: " + overallMaxNumFields);
        }
    }

    private void validateSchedule(Map<String, MatchAssignment> ass) {
        Map<Integer, Set<Team>> foo = new HashMap<>();
        ass.forEach((k, v) -> {
            var bar = foo.computeIfAbsent(v.getStartTime(), a -> new HashSet<>());
            v.getTeams().forEach(t -> {
                if (!bar.add(t)) {
                    System.out.println("Duplicate team");
                }
            });
        });
    }

    private void printSchedule(Map<Match, List<Team>> teamAssignment, int numSchedulesToPrint) {
        var allScheduled = schedulerUpper.run(teamAssignment);

        var maxDuration = allScheduled.stream()
                .mapToInt(m -> m.values().stream().mapToInt(MatchAssignment::getStartTime).max().orElseThrow()).max()
                .orElseThrow();
        System.out.println("Max End time: " + maxDuration);

        for (var i = 0; i < Math.min(numSchedulesToPrint, allScheduled.size()); i++) {
            var match = allScheduled.get(i);
            System.out.println("####");
            match.values().stream()
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

    public static void main(String[] args) throws IOException {
        int initialStart = 35;
        var app = new App(initialStart);

        if (args.length > 0) {
            app.validate = true;
            var teamAssignment = app.calcTeamAssignment(List.of(0, 1, 2, 3, 4, 5, 6, 7));
            app.printSchedule(teamAssignment, 10);
        } else {
            Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(16, 8);
            while (iterator.hasNext()) {
                int[] nextSet = iterator.next();
                List<Integer> teamCombinations = new ArrayList<>(nextSet.length);
                for (int id : nextSet) {
                    teamCombinations.add(id);
                }
                app.runAllTournamentCombinations(teamCombinations);
            }
            app.summary();
        }
    }
}
