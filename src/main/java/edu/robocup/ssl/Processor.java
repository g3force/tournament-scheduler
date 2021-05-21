package edu.robocup.ssl;

import lombok.SneakyThrows;
import org.apache.commons.math3.util.CombinatoricsUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "process")
public class Processor implements Runnable {

    @CommandLine.Option(names = {"-t", "--teamsInputFile"})
    private Path teamsInputFile = Paths.get("input.csv");

    @CommandLine.Option(names = {"-d", "--tournamentDiagramFile"})
    private Path tournamentDiagramFile = Paths.get("DD8.puml");

    @CommandLine.Option(names = {"-s", "--initialStart"})
    private int initialStart = 35;

    @CommandLine.Option(names = {"-n", "--numThreads"})
    private int numThreads = Runtime.getRuntime().availableProcessors();

    private BlockingDeque<List<Map<Match, List<Team>>>> workerQueue;
    private Evaluator evaluator;
    private Scheduler scheduler;


    @SneakyThrows
    @Override
    public void run() {
        var teams = Team.loadTeams(teamsInputFile);
        workerQueue = new LinkedBlockingDeque<>(numThreads * 2);
        scheduler = Scheduler.load(tournamentDiagramFile, initialStart);
        evaluator = new Evaluator();

        var executorService = Executors.newScheduledThreadPool(numThreads + 1);
        for (int i = 0; i < numThreads; i++) {
            executorService.submit(new WorkerThread());
        }
        executorService.scheduleAtFixedRate(new StatsThread(), 1, 1, TimeUnit.SECONDS);

        Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(16, 8);
        while (iterator.hasNext()) {
            List<Integer> teamCombinations = toList(iterator.next());
            var tournament = new Tournament(scheduler, teams);
            var teamAssignmentCombinations = tournament.calcTeamAssignmentCombinations(teamCombinations);
            workerQueue.putLast(teamAssignmentCombinations);
        }
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
        if (!terminated) {
            System.out.println("Executor service did not terminate in time");
        }
        evaluator.summary();
    }

    private void process(Map<Match, List<Team>> combination) {
        var allSchedules = scheduler.findAllSchedules(combination);
        evaluator.process(allSchedules);
    }

    private void process(List<Map<Match, List<Team>>> teamAssignmentCombinations) {
        for (var combination : teamAssignmentCombinations) {
            process(combination);
        }
    }

    private List<Integer> toList(int[] nextSet) {
        List<Integer> teamCombinations = new ArrayList<>(nextSet.length);
        for (int id : nextSet) {
            teamCombinations.add(id);
        }
        return teamCombinations;
    }


    private class WorkerThread implements Runnable {
        @Override
        public void run() {
            do {
                try {
                    Optional.ofNullable(workerQueue.pollFirst(1, TimeUnit.SECONDS))
                            .ifPresent(Processor.this::process);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } while (!workerQueue.isEmpty());
        }
    }

    private class StatsThread implements Runnable {
        @Override
        public void run() {
            evaluator.printStatistics();
        }
    }
}
