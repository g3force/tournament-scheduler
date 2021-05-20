package edu.robocup.ssl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.iterators.PermutationIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class App {
    private final List<Team> teams;
    private final Scheduler schedulerUpper;
    private final Scheduler schedulerLower;
    private final long start = System.nanoTime();
    private int count;
    private int maxEnd;

    public App() throws IOException {
        teams = loadTeams(Paths.get("input.csv"));
        schedulerUpper = Scheduler.load(Path.of("DD8.puml"));
        schedulerLower = Scheduler.load(Path.of("DD8.puml"));
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

        if (maxDuration > maxEnd) {
            maxEnd = maxDuration;
        }

        count++;
        if (count % 10 == 0) {
            long elapsed = (long) ((System.nanoTime() - start) / 1e6);
            double speed = elapsed / (double) count;
            System.out.printf("%6d: %d (%.1f)\n", elapsed, count, speed);
        }
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
                    .forEach(m -> System.out.printf("%6s: %3d %20s vs. %s\n",
                            m.getMatch().getName(),
                            m.getStartTime(),
                            m.getTeams().get(0).getName(),
                            m.getTeams().get(1).getName()));
        }
    }

    public static void main(String[] args) throws IOException {
        var app = new App();

        if (args.length > 0) {
            app.printSchedule(app.teams.subList(0, 8), 2);
        } else {
            PermutationIterator<Integer> teamPermutationIterator =
                    new PermutationIterator<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));

            teamPermutationIterator.forEachRemaining(app::run);
        }
    }
}
