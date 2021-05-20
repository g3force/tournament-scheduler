package edu.robocup.ssl;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class UmlComponentDiagram {

    @Singular
    Set<String> actors;
    @Singular
    Set<String> components;
    @Singular
    List<ConnectionPair> connections;

    public static UmlComponentDiagram parse(Path umlFile) throws IOException {
        var builder = builder();
        Files.readAllLines(umlFile).stream()
                .filter(l -> !l.startsWith("'"))
                .filter(l -> !l.isBlank())
                .forEach(l -> parseLine(l, builder));
        return builder.build();
    }

    private static void parseLine(String line, UmlComponentDiagramBuilder builder) {
        if (line.startsWith("actor")) {
            builder.actor(line.split(" ")[1]);
        } else if (line.startsWith("component")) {
            builder.component(line.split(" ")[1]);
        } else if (line.contains("-->")) {
            var connectionPart = line.split(":");
            var parts = connectionPart[0].split(" ");
            builder.connection(new ConnectionPair(parts[0], parts[2]));
        }
    }
    
    @Value
    public static class ConnectionPair {
        String from;
        String to;
    }
}
