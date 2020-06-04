package org.matsim.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

public class RunEventsHandler {

    public static void main(String[] args) {

        // TIMUR:
        // check path, for me output is located here:
        // better make sure that yours is located here as well
        String inputBeforeChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/berlin-v5.5-1pct.output_events.xml.gz";
        String inputAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-reduced-lanes/berlin-v5.5-1pct.output_events_reduced_lanes.xml.gz";



        String outputFile = "./scenarios/equil/handler-analysis.txt";

        // this manager is infrastructure for matsim... super fundamental
        EventsManager eventsManager = EventsUtils.createEventsManager();

        HomeworkHandler eventHandler = new HomeworkHandler(outputFile);
        eventsManager.addHandler(eventHandler); // add more events

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager); // reads events
        eventsReader.readFile(inputBeforeChanges);

        eventHandler.printResult();
    }
}
