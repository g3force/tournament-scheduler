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
    private final List<UmlComponentDiagram.ConnectionPair> connections;
    private final int numTeams;

    public static Scheduler load(Path tournamentDiagram) throws IOException {
        var diagram = UmlComponentDiagram.parse(tournamentDiagram);

        Map<String, MatchAssignment> emptyAssignmentMap = new HashMap<>();
        Map<String, Match> matches = loadMatches(diagram);
        matches.forEach((name, match) -> emptyAssignmentMap.put(name, MatchAssignment.of(match)));

        var numTeams = diagram.getActors().size();
        return new Scheduler(emptyAssignmentMap, diagram.getConnections(), numTeams);
    }

    private static Map<String, Match> loadMatches(UmlComponentDiagram diagram) {
        var matches = diagram.getComponents().stream().collect(Collectors.toMap(s -> s, Match::new));
        diagram.getConnections().stream()
                .filter(e -> matches.containsKey(e.getFrom()))
                .forEach(e -> matches.get(e.getFrom()).followUp(matches.get(e.getTo())));
        return matches;
    }

    private Map<String, MatchAssignment> assignTeamsToInitialMatches(List<Team> teams) {
        Map<String, Team> teamAssignment = new HashMap<>();
        for (var i = 0; i < teams.size(); i++) {
            teamAssignment.put("T" + (i+1), teams.get(i));
        }

        var matchAssignmentMap = new HashMap<>(emptyAssignmentMap);
//        List<MatchAssignment> assignments = new ArrayList<>(emptyAssignmentMap.values());
//        for (var assignment : assignments) {
//            
//        }

        connections.stream()
                .filter(e -> teamAssignment.containsKey(e.getFrom()))
                .forEach(e -> matchAssignmentMap.put(
                        e.getTo(),
                        matchAssignmentMap.get(e.getTo()).assign(teamAssignment.get(e.getFrom()))
                        )
                );
        return matchAssignmentMap;
    }

    public List<Map<String, MatchAssignment>> run(List<Team> teamMapping) {
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


    private static Optional<MatchAssignment> scheduleMatches(Map<String, MatchAssignment> assignments) {

        return assignments.values().stream()
                .filter(MatchAssignment::isNotScheduled)
                .filter(MatchAssignment::isTeamsAssigned)
                .findAny()
                .map(m -> scheduleMatch(assignments, m));
    }

    private static MatchAssignment scheduleMatch(Map<String, MatchAssignment> assignments, MatchAssignment assignment) {
        int startTime = assignments.values().stream()
                .filter(m -> m.getStartTime() != null)
                .mapToInt(MatchAssignment::getStartTime)
                .max()
                .orElse(0);
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
