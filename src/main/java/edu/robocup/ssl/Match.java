package edu.robocup.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class Match {
    String name;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<Match> followUps = new ArrayList<>();
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<Match> predecessors = new ArrayList<>();

    public Match(String name) {
        this.name = name;
    }

    public void followUp(Match match) {
        followUps.add(match);
    }

    public void predecessor(Match match) {
        predecessors.add(match);
    }
}
