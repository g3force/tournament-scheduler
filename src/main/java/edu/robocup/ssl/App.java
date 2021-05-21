package edu.robocup.ssl;

import picocli.CommandLine;

public class App {
    public static void main(String[] args) {
        new CommandLine(new Planner())
                .addSubcommand(new Processor())
                .execute(args);
    }
}
