package edu.robocup.ssl;

import lombok.Value;

@Value
public class Team {
    String name;
    int availStart;
    int availEnd;

    public int nextAvailableStart(int start) {
        int dayStart = start % 24;
        int days = start / 24;
        if (dayStart >= availStart) {
            // after or at start
            if (dayStart < availEnd || availStart > availEnd) {
                // before end
                return start;
            }
            // after or at end
            if (availStart < availEnd) {
                return (days + 1) * 24 + availStart;
            }
            return days * 24 + availStart;
        }
        // before start
        if (dayStart < availEnd && availStart > availEnd) {
            return start;
        }
        return days * 24 + availStart;
    }
}
