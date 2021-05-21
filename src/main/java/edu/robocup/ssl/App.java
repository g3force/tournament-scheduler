package edu.robocup.ssl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.iterators.PermutationIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
    private int count;
    private int maxEnd;
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

    private void run(List<Integer> mapping) {

        List<Team> mappedTeams = new ArrayList<>(mapping.size());
        mapping.forEach(i -> mappedTeams.add(teams.get(i)));

        var allScheduled = schedulerUpper.run(mappedTeams);

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

        if (maxDuration > maxEnd) {
            maxEnd = maxDuration;
            System.out.println("maxEnd: " + maxEnd);
        }

        if (validate) {
            if (maxNumFields > this.overallMaxNumFields) {
                overallMaxNumFields = maxNumFields;
                System.out.println("Max fields: " + overallMaxNumFields);
            }

            allScheduled.forEach(this::validateSchedule);
        }

        count++;
        int m = 10;
        if (count % m == 0) {
            long now = System.nanoTime();
            long totalElapsed = (long) ((now - start) / 1e6);
            long elapsed = (long) ((now - lastStart) / 1e6);
            double avgSpeed = totalElapsed / (double) count;
            double speed = elapsed / (double) m;
            System.out.printf("%6d: %d (%.1f|%.1f)%n", totalElapsed, count, avgSpeed, speed);
            lastStart = now;
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

    private void printSchedule(List<Team> teams, int numSchedulesToPrint) {
        var allScheduled = schedulerUpper.run(teams);

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
                    .forEach(m -> System.out.printf("%6s: %3d %20s vs. %s%n",
                            m.getMatch().getName(),
                            m.getStartTime(),
                            m.getTeams().get(0).getName(),
                            m.getTeams().get(1).getName()));
        }
    }

    public static void main(String[] args) throws IOException {
        int initialStart = 0;
        var app = new App(initialStart);

        if (args.length > 0) {
            app.printSchedule(app.teams.subList(0, 8), 10);
        } else {
            PermutationIterator<Integer> teamPermutationIterator =
                    new PermutationIterator<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));

            teamPermutationIterator.forEachRemaining(app::run);
        }
    }
}
