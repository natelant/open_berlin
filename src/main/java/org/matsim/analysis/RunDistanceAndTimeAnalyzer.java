package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunDistanceAndTimeAnalyzer {

    public static void main(String[] args) {

        String networkBeforeChanges = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        String networkAfterChanges = "./scenarios/equil/network-reduced-lanes.xml.gz";
        String eventsBeforeChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-50-iters/berlin-v5.5-1pct.output_events.xml.gz";
        String eventsAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-50-iters-reduced-lanes/berlin-v5.5-1pct.output_events.xml.gz";

        runDistanceAnalyzer(networkBeforeChanges, eventsBeforeChanges);
        runDistanceAnalyzer(networkAfterChanges, eventsAfterChanges);

    }

    public static void runDistanceAnalyzer(String network, String events) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(network);

        DistanceAndTimeAnalyzer distanceAndTimeAnalyzer = new DistanceAndTimeAnalyzer(scenario.getNetwork());
        eventsManager.addHandler(distanceAndTimeAnalyzer);
        new MatsimEventsReader(eventsManager).readFile(events);

        distanceAndTimeAnalyzer.printAverageDistance();
        distanceAndTimeAnalyzer.printAverageTravelTime();
    }
}