package edu.robocup.ssl;

import lombok.SneakyThrows;
import org.apache.commons.math3.util.CombinatoricsUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@CommandLine.Command(name = "process")
public class Processor implements Runnable {

    @CommandLine.Option(names = {"-t", "--teamsInputFile"})
    private Path teamsInputFile = Paths.get("input.csv");

    @CommandLine.Option(names = {"-d", "--tournamentDiagramFile"})
    private Path tournamentDiagramFile = Paths.get("DD8.puml");

    @CommandLine.Option(names = {"-s", "--initialStart"})
    private int initialStart = 35;

    @SneakyThrows
    @Override
    public void run() {
        var teams = Team.loadTeams(teamsInputFile);
        var scheduler = Scheduler.load(tournamentDiagramFile, initialStart);

        Evaluator evaluator = new Evaluator();

        Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(16, 8);
        while (iterator.hasNext()) {
            List<Integer> teamCombinations = toList(iterator.next());
            var tournament = new Tournament(scheduler, teams);
            var teamAssignmentCombinations = tournament.calcTeamAssignmentCombinations(teamCombinations);

            for (var combination : teamAssignmentCombinations) {
                var allSchedules = scheduler.findAllSchedules(combination);
                evaluator.process(allSchedules);
            }
        }
        evaluator.summary();
    }

    private List<Integer> toList(int[] nextSet) {
        List<Integer> teamCombinations = new ArrayList<>(nextSet.length);
        for (int id : nextSet) {
            teamCombinations.add(id);
        }
        return teamCombinations;
    }
}
