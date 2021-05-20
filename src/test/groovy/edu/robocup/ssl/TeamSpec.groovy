package edu.robocup.ssl

import spock.lang.Specification

class TeamSpec extends Specification {

    def "Test available start"() {
        given:
        def team = new Team("", availStart, availEnd)

        when:
        def actualNextStart = team.nextAvailableStart(start)

        then:
        actualNextStart == nextStart

        where:
        availStart | availEnd | start | nextStart
        0          | 15       | 0     | 0
        0          | 15       | 3     | 3
        3          | 15       | 0     | 3
        0          | 15       | 14    | 14
        0          | 15       | 15    | 24
        0          | 15       | 17    | 24
        0          | 23       | 23    | 24
        22         | 5        | 0     | 0
        22         | 5        | 20    | 22
        22         | 5        | 3     | 3
        22         | 5        | 7     | 22
        22         | 5        | 25    | 25
        22         | 5        | 29    | 46
        22         | 5        | 43    | 46
        22         | 5        | 48    | 48
        22         | 5        | 53    | 70
        0          | 2        | 0     | 0
        0          | 2        | 1     | 1
        0          | 2        | 2     | 24
        1          | 2        | 2     | 25
        23         | 1        | 22    | 23
        23         | 1        | 23    | 23
        23         | 1        | 24    | 24
        23         | 1        | 25    | 47
        22         | 23       | 22    | 22
        22         | 23       | 21    | 22
        22         | 23       | 23    | 46
        12         | 3        | 22    | 22
        11         | 2        | 22    | 22
    }
}
