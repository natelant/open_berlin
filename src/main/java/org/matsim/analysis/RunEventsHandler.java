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
        String inputFile = "/scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct/berlin-v5.5-1pct.output_events.xml.gz";

        // does this path have to exist? or am I writing a new folder and new output file?

        // TIMUR:
        // you just write path to a new (yet non existing) file

        String outputFile = "output100/output100.txt";

        // this manager is infrastructure for matsim... super fundamental
        EventsManager eventsManager = EventsUtils.createEventsManager();

        HomeworkHandler eventHandler = new HomeworkHandler();
        eventsManager.addHandler(eventHandler); // add more events

        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager); // reads events
        eventsReader.readFile(inputFile);

        // eventHandler.printResult();
    }
}
