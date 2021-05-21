package edu.robocup.ssl;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class Match {
    String name;
    @ToString.Exclude
    List<Match> followUps = new ArrayList<>();
    @ToString.Exclude
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
