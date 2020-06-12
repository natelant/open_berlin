package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class RunDistanceTimeModeHandler {

    static String networkBeforeChanges = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
    static String networkAfterChanges = "./scenarios/equil/network-reduced-lanes.xml.gz";
    static String eventsBeforeChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-50-iters/berlin-v5.5-1pct.output_events.xml.gz";
    static String eventsAfterChanges = "./scenarios/berlin-v5.5-1pct/output-berlin-v5.5-1pct-50-iters-reduced-lanes/berlin-v5.5-1pct.output_events.xml.gz";

    static String agentsOnlyBefore = "./scenarios/equil/handler-only-before_50.txt";
    // static String agentsBeforeAndAfter = "./scenarios/equil/handler-before-and-after_50.txt";

    static HashMap<Id<Person>, ArrayList<String>> modesBefore;
    static HashMap<Id<Person>, ArrayList<String>> modesAfter;

    public static void main(String[] args) {

        // getting average distance and time travelling before and after for all affected agents
        runDistanceTimeAnalyzer(networkBeforeChanges, eventsBeforeChanges);
        runDistanceTimeAnalyzer(networkAfterChanges, eventsAfterChanges);

        // check if there was mode change for those who used Bundesallee only before the changes
        checkModeChange(agentsOnlyBefore);

    }

    // normal matsim runHandler thing
    public static void runDistanceTimeAnalyzer(String network, String events) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(network);

        DistanceTimeModeHandler distanceTimeModeHandler = new DistanceTimeModeHandler(scenario.getNetwork());
        eventsManager.addHandler(distanceTimeModeHandler);
        new MatsimEventsReader(eventsManager).readFile(events);

        distanceTimeModeHandler.printAverageDistance();
        distanceTimeModeHandler.printAverageTravelTime();

        if (network.equals(networkBeforeChanges)) {
            modesBefore = distanceTimeModeHandler.modes;
        } else {
            modesAfter = distanceTimeModeHandler.modes;
        }
    }

    public static void checkModeChange(String agentsGroup) {

        // reading specified agents' IDs and saving to arraylist
        ArrayList<String> agents = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(agentsGroup));
            while (scanner.hasNext()) {
                agents.add(scanner.nextLine());
            }
        } catch (FileNotFoundException ee) {
            System.out.println("File not found!");
        }

        // setting counters to 0
        int counter = 0;
        int lessCarCounter = 0;
        int morePtCounter = 0;
        int moreBicycleCounter = 0;
        int moreWalkCounter = 0;

        // iterating over agents: if modes "after" are not the same as "before", do dome analysis
        for (String strPersonId : agents) {
            Id<Person> personId = Id.createPersonId(strPersonId);
            if (!modesBefore.get(personId).equals(modesAfter.get(personId))) {

                // see this function further down
                if (moreOrLessMode(personId, "car").equals("less")) lessCarCounter++;
                if (moreOrLessMode(personId, "pt").equals("more")) morePtCounter++;
                if (moreOrLessMode(personId, "bicycle").equals("more")) moreBicycleCounter++;
                if (moreOrLessMode(personId, "walk").equals("more")) moreWalkCounter++;

                counter++;
            }
        }

        // printing results to the console
        System.out.println("total agents " + agents.size());
        System.out.println("new routes: " + (agents.size()-counter) + " agents");
        System.out.println("new modes: " + counter + " agents. especially:");

        System.out.println("less car " + lessCarCounter);
        System.out.println("more pt " + morePtCounter);
        System.out.println("more bicycle " + moreBicycleCounter);
        System.out.println("more walk " + moreWalkCounter);
    }

    // checking if person makes more, less or equal number of legs with this mode ("before" vs. "after")
    static String moreOrLessMode(Id<Person> personId, String mode) {
        if (Collections.frequency(modesBefore.get(personId), mode)
                > Collections.frequency(modesAfter.get(personId), mode)) {
            return "less";
        } else if (Collections.frequency(modesBefore.get(personId), mode)
                == Collections.frequency(modesAfter.get(personId), mode)){
            return "equal";
        } else {
            return "more";
        }
    }
}