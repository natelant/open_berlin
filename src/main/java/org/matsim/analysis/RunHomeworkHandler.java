package org.matsim.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

public class RunHomeworkHandler {

    public static void main(String[] args) {

        // defining input and output paths
        String inputBeforeChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/berlin-v5.5-1pct.output_events.xml.gz";
        String outputBeforeChanges = "./scenarios/equil/handler-analysis-before_50.txt";
        String inputAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-10-iters-reduced-lanes/berlin-v5.5-1pct.output_events.xml.gz";
        String outputAfterChanges = "./scenarios/equil/handler-analysis-after_50.txt";
        String outputAllAffectedAgents = "./scenarios/equil/handler-all-affected-agents_50.txt";

        // run handler for both "before" and "after" cases
        runHomeworkHandler(inputBeforeChanges, outputBeforeChanges);
        runHomeworkHandler(inputAfterChanges, outputAfterChanges);

        // getting all affected agents in one file
        createAllAffectedAgentsFile(outputBeforeChanges, outputAfterChanges, outputAllAffectedAgents);
    }

    public static void runHomeworkHandler(String input, String output) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        HomeworkHandler homeworkHandler = new HomeworkHandler(output);
        eventsManager.addHandler(homeworkHandler);
        eventsReader.readFile(input);
        homeworkHandler.printToFile();
    }

    public static void createAllAffectedAgentsFile(String outputBefore, String outputAfter, String outputFinal) {

        Set<String> agentsBefore = new LinkedHashSet<>();
        Set<String> agentsAfter = new LinkedHashSet<>();

        // creating sets of agents for "before" and "after" cases
        readIDs(outputBefore, agentsBefore);
        readIDs(outputAfter, agentsAfter);

        // counting those who used Bundesallee before, after or before&after
        countAgentsBeforeAfter(agentsBefore, agentsAfter);

        // unite sets -> we now have one set for all affected agents
        agentsBefore.addAll(agentsAfter);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFinal));
            for (String agent : agentsBefore) {
                writer.write(agent + "\n");
            }
            writer.close();
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }

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

    public static void countAgentsBeforeAfter(Set<String> agentsBefore, Set<String> agentsAfter) {
        int onlyBefore = 0;
        int beforeAndAfter = 0;

        for (String agent : agentsBefore) {
            if (agentsAfter.contains(agent)) {
                beforeAndAfter++;
            } else {
                onlyBefore++;
            }
        }
        System.out.println(onlyBefore + " agents used Bundesallee only before the changes");
        System.out.println(agentsAfter.size()-beforeAndAfter + " agents used Bundesallee only after the changes");
        System.out.println(beforeAndAfter + " agents used Bundesallee both before and after the changes");
    }
}
