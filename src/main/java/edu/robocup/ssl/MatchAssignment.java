package edu.robocup.ssl;

import lombok.Value;
import lombok.With;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
@With
public class MatchAssignment {
    Match match;
    List<Team> teams;
    Integer startTime;


    public static MatchAssignment of(Match match) {
        return new MatchAssignment(match, Collections.emptyList(), null);
    }

    public MatchAssignment assign(Team team) {
        if (teams.size() >= 2) {
            throw new IllegalStateException("Both teams assigned already for match " + this);
        }
        List<Team> newTeams = new ArrayList<>(teams.size() + 1);
        newTeams.addAll(teams);
        newTeams.add(team);
        return withTeams(newTeams);
    }

    public boolean isTeamsAssigned() {
        return teams.size() == 2;
    }

    public boolean isNotScheduled() {
        return startTime == null;
    }

    public boolean isScheduled() {
        return startTime != null;
    }

    public int nextAvailableStart(int baseTime) {
        return Math.max(
                teams.get(0).nextAvailableStart(baseTime),
                teams.get(1).nextAvailableStart(baseTime)
        );
    }

    public MatchAssignment schedule(int baseTime) {
        return withStartTime(nextAvailableStart(baseTime));
    }
}
