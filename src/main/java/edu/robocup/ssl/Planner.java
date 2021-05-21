package edu.robocup.ssl;

import lombok.SneakyThrows;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command
public class Planner implements Runnable {

    @CommandLine.Option(names = {"-t", "--teamsInputFile"})
    private Path teamsInputFile = Paths.get("input.csv");

    @CommandLine.Option(names = {"-d", "--tournamentDiagramFile"})
    private Path tournamentDiagramFile = Paths.get("DD8.puml");

    @CommandLine.Option(names = {"-s", "--initialStart"})
    private int initialStart = 35;

    @CommandLine.Option(names = {"-n", "--numSchedulesToPrint"})
    private int numSchedulesToPrint = 10;

    @CommandLine.Option(names = {"-a", "--teamIds"})
    private List<Integer> teamIds = List.of(0, 1, 2, 3, 4, 5, 6, 7);


    @SneakyThrows
    @Override
    public void run() {
        var teams = Team.loadTeams(teamsInputFile);
        var scheduler = Scheduler.load(tournamentDiagramFile, initialStart);

        var selectedTeams = teamIds.stream().map(teams::get).collect(Collectors.toList());
        var tournament = new Tournament(scheduler, selectedTeams);
        var teamAssignment = tournament.calcTeamAssignment(teamIds);
        var allSchedules = scheduler.findAllSchedules(teamAssignment);

        var maxEndTime = allSchedules.stream().mapToInt(Schedule::findMaxEndTime).max().orElseThrow();
        System.out.println("Max End time: " + maxEndTime);

        for (var i = 0; i < Math.min(numSchedulesToPrint, allSchedules.size()); i++) {
            System.out.println("####");
            allSchedules.get(i).print();
            System.out.println("####");
        }
    }
}
