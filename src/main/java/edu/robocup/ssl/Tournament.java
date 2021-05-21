package edu.robocup.ssl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.iterators.PermutationIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class Tournament {
    private final Scheduler scheduler;
    private final List<Team> teams;

    public static Tournament of(Scheduler scheduler, List<Team> teams) {
        return new Tournament(scheduler, teams);
    }

    @SuppressWarnings("java:S5413")
    public Map<Match, List<Team>> calcTeamAssignment(List<Integer> teamIds) {
        var teamDemand = scheduler.getTeamDemand();

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


    public List<Map<Match, List<Team>>> calcTeamAssignmentCombinations(List<Integer> teamIds) {
        var teamDemand = scheduler.getTeamDemand();

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


    public List<Schedule> run(Map<Match, List<Team>> teamMapping) {
        return scheduler.findAllSchedules(teamMapping);
    }
}
