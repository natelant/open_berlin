package org.matsim.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

public class RunHomeworkHandler {

    // defining input and output paths
    static String inputBeforeChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-50-iters/berlin-v5.5-1pct.output_events.xml.gz";
    static String inputAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-50-iters-reduced-lanes/berlin-v5.5-1pct.output_events.xml.gz";

    static String agentsBeforeChanges = "./scenarios/equil/handler-analysis-before_50.txt";
    static String agentsAfterChanges = "./scenarios/equil/handler-analysis-after_50.txt";

    static String outputAllAffectedAgents = "./scenarios/equil/handler-all-affected-agents_50.txt";
    static String outputOnlyBefore = "./scenarios/equil/handler-only-before_50.txt";
    static String outputBeforeAndAfter = "./scenarios/equil/handler-before-and-after_50.txt";

    public static void main(String[] args) {

        // run handler for both "before" and "after" cases
        runHomeworkHandler(inputBeforeChanges, agentsBeforeChanges);
        runHomeworkHandler(inputAfterChanges, agentsAfterChanges);

        // creating different sets to store agent ids
        Set<String> agentsBefore = new LinkedHashSet<>();
        Set<String> agentsAfter = new LinkedHashSet<>();
        Set<String> agentsOnlyBefore = new LinkedHashSet<>();
        Set<String> agentsBeforeAndAfter = new LinkedHashSet<>();

        // creating sets of agents for "before" and "after" cases from .txt files
        readIDs(agentsBeforeChanges, agentsBefore);
        readIDs(agentsAfterChanges, agentsAfter);

        // counting those who used Bundesallee before, after or before&after, printing to the console
        for (String agent : agentsBefore) {
            if (agentsAfter.contains(agent)) {
                agentsBeforeAndAfter.add(agent);
            } else {
                agentsOnlyBefore.add(agent);
            }
        }
        // printing .txt with those who used Bundesallee (only before) AND (before and after)
        printToFile(agentsOnlyBefore, outputOnlyBefore);
        printToFile(agentsBeforeAndAfter, outputBeforeAndAfter);

        // printing important numbers to the console
        System.out.println(agentsOnlyBefore.size() + " agents used Bundesallee only before the changes");
        System.out.println(agentsAfter.size()-agentsBeforeAndAfter.size() + " agents used Bundesallee only after the changes");
        System.out.println(agentsBeforeAndAfter.size() + " agents used Bundesallee both before and after the changes");

        // unite sets -> we now have one set for all affected agents, printing to file
        agentsBefore.addAll(agentsAfter);
        printToFile(agentsBefore, outputAllAffectedAgents);
    }

    // just normal matsim runHandler thing
    public static void runHomeworkHandler(String input, String output) {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        HomeworkHandler homeworkHandler = new HomeworkHandler(output);
        eventsManager.addHandler(homeworkHandler);
        eventsReader.readFile(input);
        homeworkHandler.printToFile();
    }

    public static void readIDs(String output, Set<String> affectedAgents) {
        try {
            Scanner scanner = new Scanner(new File(output));
            scanner.nextLine();
            while (scanner.hasNext()) {
                affectedAgents.add(scanner.nextLine());
            }
        } catch (FileNotFoundException ee) {
            System.out.println("File not found!");
        }
    }

    public static void printToFile(Set<String> agents,  String output) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            for (String agent : agents) {
                writer.write(agent + "\n");
            }
            writer.close();
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
    }
}
