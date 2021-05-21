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

    public static Scheduler load(Path tournamentDiagramPath, int initialStart) throws IOException {
        var diagram = UmlComponentDiagram.parse(tournamentDiagramPath);

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

    private Schedule assignTeamsToMatches(Map<Match, List<Team>> matchToTeamMap) {
        var matchAssignmentMap = new HashMap<>(emptyAssignmentMap);
        for (var entry : matchToTeamMap.entrySet()) {
            var matchName = entry.getKey().getName();
            var teams = entry.getValue();
            var assignment = matchAssignmentMap.get(matchName);
            matchAssignmentMap.put(matchName, assignment.assign(teams));
        }
        return new Schedule(matchAssignmentMap);
    }

    public List<Schedule> findAllSchedules(Map<Match, List<Team>> teamMapping) {
        List<Schedule> sets = new ArrayList<>();
        sets.add(assignTeamsToMatches(teamMapping));
        List<Schedule> allScheduled = new ArrayList<>();

        while (!sets.isEmpty()) {
            List<Schedule> newSets = new ArrayList<>();
            for (var set : sets) {
                var scheduleAssignment = scheduleNextMatch(set);
                scheduleAssignment.ifPresentOrElse(
                        s -> {
                            var newSet = new HashMap<>(set.getAssignmentMap());
                            newSet.put(s.getMatch().getName(), s);
                            var newSchedule = new Schedule(Collections.unmodifiableMap(newSet));
                            newSets.addAll(assignTeams(newSchedule, s));
                        },
                        () -> allScheduled.add(set));
            }
            sets.clear();
            sets.addAll(newSets);
        }
        return allScheduled;
    }


    private Optional<MatchAssignment> scheduleNextMatch(Schedule schedule) {
        return schedule.getAssignmentMap().values().stream()
                .filter(MatchAssignment::isNotScheduled)
                .filter(MatchAssignment::isTeamsAssigned)
                .findAny()
                .map(m -> scheduleMatch(schedule, m));
    }

    private MatchAssignment scheduleMatch(Schedule schedule, MatchAssignment assignment) {
        int startTime = assignment.getMatch().getPredecessors().stream()
                .mapToInt(m -> schedule.getAssignmentMap().get(m.getName()).getStartTime()).max().orElse(initialStart);
        return assignment.schedule(startTime + 1);
    }

    private static List<Schedule> assignTeams(Schedule schedule, MatchAssignment assignment) {
        var assignments = schedule.getAssignmentMap();
        List<Match> followUps = assignment.getMatch().getFollowUps();
        if (followUps.size() == 1) {
            var followUpAssignment = assignments.get(followUps.get(0).getName());
            var teamAWin = followUpAssignment.assign(assignment.getTeams().get(0));
            var teamBWin = followUpAssignment.assign(assignment.getTeams().get(1));
            return List.of(
                    schedule.withNewAssignment(teamAWin),
                    schedule.withNewAssignment(teamBWin)
            );
        } else if (followUps.size() == 2) {
            var followUpAssignmentA = assignments.get(followUps.get(0).getName());
            var followUpAssignmentB = assignments.get(followUps.get(1).getName());
            var teamAWin = followUpAssignmentA.assign(assignment.getTeams().get(0));
            var teamBLoose = followUpAssignmentB.assign(assignment.getTeams().get(1));
            var teamALoose = followUpAssignmentA.assign(assignment.getTeams().get(1));
            var teamBWin = followUpAssignmentB.assign(assignment.getTeams().get(0));
            return List.of(
                    schedule.withNewAssignment(teamBLoose).withNewAssignment(teamAWin),
                    schedule.withNewAssignment(teamBWin).withNewAssignment(teamALoose)
            );
        }
        return List.of(schedule);
    }
}
