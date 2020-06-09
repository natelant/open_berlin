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
        String inputBeforeChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-10-iters/berlin-v5.5-1pct.output_events.xml.gz";
        String outputBeforeChanges = "./scenarios/equil/handler-analysis-before.txt";
        String inputAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-10-iters-reduced-lanes/berlin-v5.5-1pct.output_events.xml.gz";
        String outputAfterChanges = "./scenarios/equil/handler-analysis-after.txt";
        String outputAllAffectedAgents = "./scenarios/equil/handler-all-affected-agents.txt";

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

        Set<String> affectedAgents = new LinkedHashSet<>();

        readIDs(outputBefore, affectedAgents);
        readIDs(outputAfter, affectedAgents);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFinal));
            for (String agent : affectedAgents) {
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
}
