package edu.robocup.ssl;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Scheduler {

    private final Map<String, MatchAssignment> emptyAssignmentMap;
    private final int initialStart;

    public static Scheduler load(Path tournamentDiagram, int initialStart) throws IOException {
        var diagram = UmlComponentDiagram.parse(tournamentDiagram);

        Map<String, MatchAssignment> emptyAssignmentMap = new HashMap<>();
        Map<String, Match> matches = loadMatches(diagram);
        matches.forEach((name, match) -> emptyAssignmentMap.put(name, MatchAssignment.of(match)));

        return new Scheduler(emptyAssignmentMap, initialStart);
    }

    private static Map<String, Match> loadMatches(UmlComponentDiagram diagram) {
        var matches = diagram.getComponents().stream().collect(Collectors.toMap(s -> s, Match::new));
        diagram.getConnections().stream()
                .filter(e -> matches.containsKey(e.getFrom()))
                .forEach(e -> matches.get(e.getFrom()).followUp(matches.get(e.getTo())));
        diagram.getConnections().stream()
                .filter(e -> matches.containsKey(e.getFrom()))
                .forEach(e -> matches.get(e.getTo()).predecessor(matches.get(e.getFrom())));
        return matches;
    }

    public Map<Match, Integer> getTeamDemand() {
        return emptyAssignmentMap.values().stream()
                .map(MatchAssignment::getMatch)
                .filter(m -> m.getPredecessors().size() != 2)
                .collect(Collectors.toMap(m -> m, m -> 2 - m.getPredecessors().size()));
    }

    private Map<String, MatchAssignment> assignTeamsToInitialMatches(Map<Match, List<Team>> teams) {
        var matchAssignmentMap = new HashMap<>(emptyAssignmentMap);
        for (var entry : teams.entrySet()) {
            String matchName = entry.getKey().getName();
            var assignment = matchAssignmentMap.get(matchName);
            for (var team : entry.getValue()) {
                assignment = assignment.assign(team);
            }
            matchAssignmentMap.put(matchName, assignment);
        }
        return matchAssignmentMap;
    }

    public List<Map<String, MatchAssignment>> run(Map<Match, List<Team>> teamMapping) {
        List<Map<String, MatchAssignment>> sets = new ArrayList<>();
        sets.add(assignTeamsToInitialMatches(teamMapping));
        List<Map<String, MatchAssignment>> allScheduled = new ArrayList<>();

        while (!sets.isEmpty()) {
            List<Map<String, MatchAssignment>> newSets = new ArrayList<>();
            for (var set : sets) {
                var scheduleAssignment = scheduleMatches(set);
                scheduleAssignment.ifPresentOrElse(
                        s -> {
                            var newSet = new HashMap<>(set);
                            newSet.put(s.getMatch().getName(), s);
                            newSets.addAll(assignTeams(Collections.unmodifiableMap(newSet), s));
                        },
                        () -> allScheduled.add(set));
            }
            sets.clear();
            sets.addAll(newSets);
        }
        return allScheduled;
    }


    private Optional<MatchAssignment> scheduleMatches(Map<String, MatchAssignment> assignments) {

        return assignments.values().stream()
                .filter(MatchAssignment::isNotScheduled)
                .filter(MatchAssignment::isTeamsAssigned)
                .findAny()
                .map(m -> scheduleMatch(assignments, m));
    }

    private MatchAssignment scheduleMatch(Map<String, MatchAssignment> assignments, MatchAssignment assignment) {
        int startTime = assignment.getMatch().getPredecessors().stream()
                .mapToInt(m -> assignments.get(m.getName()).getStartTime()).max().orElse(initialStart);
        return assignment.schedule(startTime + 1);
    }

    private static List<Map<String, MatchAssignment>> assignTeams(
            Map<String, MatchAssignment> assignments,
            MatchAssignment assignment) {
        List<Map<String, MatchAssignment>> result = new ArrayList<>();

        List<Match> followUps = assignment.getMatch().getFollowUps();
        if (followUps.size() == 1) {
            var followUpAssignment = assignments.get(followUps.get(0).getName());
            var teamAWin = followUpAssignment.assign(assignment.getTeams().get(0));
            result.add(assign(assignments, teamAWin));
            var teamBWin = followUpAssignment.assign(assignment.getTeams().get(1));
            result.add(assign(assignments, teamBWin));
        } else if (followUps.size() == 2) {
            var followUpAssignmentA = assignments.get(followUps.get(0).getName());
            var followUpAssignmentB = assignments.get(followUps.get(1).getName());
            var teamAWin = followUpAssignmentA.assign(assignment.getTeams().get(0));
            var teamBLoose = followUpAssignmentB.assign(assignment.getTeams().get(1));
            result.add(assign(assign(assignments, teamBLoose), teamAWin));
            var teamALoose = followUpAssignmentA.assign(assignment.getTeams().get(1));
            var teamBWin = followUpAssignmentB.assign(assignment.getTeams().get(0));
            result.add(assign(assign(assignments, teamBWin), teamALoose));
        } else {
            result.add(assignments);
        }

        return result;
    }

    private static Map<String, MatchAssignment> assign(Map<String, MatchAssignment> assignments,
                                                       MatchAssignment followUpAssignment) {
        Map<String, MatchAssignment> newAssignment = new HashMap<>(assignments);
        newAssignment.put(followUpAssignment.getMatch().getName(), followUpAssignment);
        return Collections.unmodifiableMap(newAssignment);
    }
}
