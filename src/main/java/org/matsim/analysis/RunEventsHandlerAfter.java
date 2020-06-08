package org.matsim.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class RunEventsHandlerAfter {

    public static void main(String[] args) {

        String inputAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-10-iters-reduced-lanes/berlin-v5.5-1pct.output_events.xml.gz";
        String outputFile = "./scenarios/equil/handler-analysis-after.txt";

        // this manager is infrastructure for matsim... super fundamental
        EventsManager eventsManager = EventsUtils.createEventsManager();

        HomeworkHandler homeworkHandler = new HomeworkHandler(outputFile);
        eventsManager.addHandler(homeworkHandler); // add more events

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager); // reads events
        eventsReader.readFile(inputAfterChanges);

        homeworkHandler.printToFile();
    }
}
