package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

// make it prepareTransitSchedule or prepareScenario in RunMatsim
public class TransitScheduleModifier {

    static String input = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
    static String outputNetwork = "./scenarios/equil/network-modified-HW2.xml.gz";
    static String outputTransitSchedule = "./scenarios/equil/transit-schedule-modified.xml.gz";

    // to do: change to RunMatsim.class
    private static final Logger LOGGER = Logger.getLogger(TransitScheduleModifier.class);

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig("./scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        TransitScheduleFactory tsf = transitSchedule.getFactory();

        String[] stopNames = {
                "Berliner Rathaus", "Fischerinsel", "U Spittelmarkt", "Jerusalemer Str.",
                "U Stadtmitte/Leipziger Str.", "Leipziger Str./Wilhelmstr.", "S+U Potsdamer Platz", "Kulturforum"};
        double[][] stopCoords = {
                {4595741.283812712, 5821391.332894235}, {4595543.276787662, 5821032.276349911},
                {4595311.298656522, 5820721.322711342}, {4594868.306563006, 5820661.307287242},
                {4594469.274947998, 5820596.279395453}, {4594082.239244875, 5820553.33117294},
                {4593681.244368908, 5820501.353559958}, {4593018.0, 5820324.0}};

        // check and update travel times!
        List<Double> travelTimesWestToEast = List.of(100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0);

        // some of these lists are not necessary, remove them later
        ArrayList<Id<Node>> nodeIdsToEast = new ArrayList<>();
        ArrayList<Id<Node>> nodeIdsToWest = new ArrayList<>();
        ArrayList<Node> nodesToEast = new ArrayList<>();
        ArrayList<Node> nodesToWest = new ArrayList<>();
        ArrayList<Link> linksToEast = new ArrayList<>();
        ArrayList<Link> linksToWest = new ArrayList<>();
        ArrayList<Id<Link>> linkIdsToEast = new ArrayList<>();
        ArrayList<Id<Link>> linkIdsToWest = new ArrayList<>();
        ArrayList<TransitStopFacility> transitStopsToEast = new ArrayList<>();
        ArrayList<TransitStopFacility> transitStopsToWest = new ArrayList<>();

        for (int i=0; i<stopNames.length; i++) {

            String stop = stopNames[i];
            double[] coords = stopCoords[i];
            Coord coord = new Coord(coords[0], coords[1]);

            // creating pt_Nodes (in both directions, toEast/toWest), adding to Network
            Id<Node> nodeIdToEast = Id.createNodeId(String.format("pt_%s_toEast", stop));
            nodeIdsToEast.add(nodeIdToEast);
            Node nodeToEast = nf.createNode(nodeIdToEast, coord);
            network.addNode(nodeToEast);
            nodesToEast.add(nodeToEast);

            Id<Node> nodeIdToWest = Id.createNodeId(String.format("pt_%s_toWest", stop));
            nodeIdsToWest.add(nodeIdToWest);
            Node nodeToWest = nf.createNode(nodeIdToWest, coord);
            network.addNode(nodeToWest);
            nodesToWest.add(nodeToWest);

            // creating TransitStops (in both directions, toEast/toWest), adding to TransitSchedule
            Id<TransitStopFacility> idToEast = Id.create(stop + "_toEast", TransitStopFacility.class);
            TransitStopFacility transitStopFacilityToEast = tsf.createTransitStopFacility(idToEast, coord, false);
            transitStopFacilityToEast.setName(String.format("Berlin, %s", stop));
            transitStopsToEast.add(transitStopFacilityToEast);
            transitSchedule.addStopFacility(transitStopFacilityToEast);

            Id<TransitStopFacility> idToWest = Id.create(stop + "_toWest", TransitStopFacility.class);
            TransitStopFacility transitStopFacilityToWest = tsf.createTransitStopFacility(idToWest, coord, false);
            transitStopFacilityToWest.setName(String.format("Berlin, %s", stop));
            transitStopsToWest.add(transitStopFacilityToWest);
            transitSchedule.addStopFacility(transitStopFacilityToWest);
        }

        Collections.reverse(nodesToEast);

        // create and add pt_Links
        {
            int counter = 0;
            for (Node toNode : nodesToEast) {
                if (counter != 0) {
                    Node fromNode = nodesToEast.get(nodesToEast.indexOf(toNode) - 1);
                    Id<Link> linkId = Id.createLinkId(String.format("pt_%d_toEast", counter));
                    Link link = nf.createLink(linkId, fromNode, toNode);
                    network.addLink(link);
                    // TODO: remove one of them
                    linkIdsToEast.add(linkId);
                    linksToEast.add(link);
                }
                counter++;
            }

            int counter2 = 0;
            for (Node toNode : nodesToWest) {
                if (counter2 != 0) {
                    Node fromNode = nodesToWest.get(nodesToWest.indexOf(toNode) - 1);
                    Id<Link> linkId = Id.createLinkId(String.format("pt_%d_toWest", 8-counter2));
                    Link link = nf.createLink(linkId, fromNode, toNode);
                    network.addLink(link);
                    // TODO: remove one of them
                    linkIdsToWest.add(linkId);
                    linksToWest.add(link);
                }
                counter2++;
            }
        }

        // manually adding pt_Links between Alex and Berliner Rathaus, because there are
        // two different pt_Nodes of Alexanderplatz/Dircksenstr.
        Node alex16 = network.getNodes().get(Id.createNodeId("pt_070301008816"));
        Node alex17 = network.getNodes().get(Id.createNodeId("pt_070301008817"));
        Node rathausToEast = nodesToEast.get(7);
        Node rathausToWest = nodesToWest.get(0);

        Id<Link> linkIdToAlex16 = Id.createLinkId("pt_8_toEast_to16");
        Link rathausToAlex16 = nf.createLink(linkIdToAlex16, rathausToEast, alex16);
        network.addLink(rathausToAlex16);
        //linkIdsToEast.add(linkIdToAlex16);
        linksToEast.add(rathausToAlex16);

        Id<Link> linkIdToAlex17 = Id.createLinkId("pt_8_toEast_to17");
        Link rathausToAlex17 = nf.createLink(linkIdToAlex17, rathausToEast, alex17);
        network.addLink(rathausToAlex17);
        //linkIdsToEast.add(linkIdToAlex17);
        linksToEast.add(rathausToAlex17);

        Id<Link> linkIdFromAlex16 = Id.createLinkId("pt_8_toWest_from16");
        Link fromAlex16 = nf.createLink(linkIdFromAlex16, alex17, rathausToWest);
        network.addLink(fromAlex16);
        //linkIdsToWest.add(linkIdFromAlex16);
        linksToWest.add(fromAlex16);

        Id<Link> linkIdFromAlex17 = Id.createLinkId("pt_8_toWest_from17");
        Link fromAlex17 = nf.createLink(linkIdFromAlex17, alex17, rathausToWest);
        network.addLink(fromAlex17);
        //linkIdsToWest.add(linkIdFromAlex17);
        linksToWest.add(fromAlex17);

        // TODO: link attributes (length, capacity, freespeed)
        double[][] linksAttributes = {
                {696.39, 100000.0, 3.0}, {404.35, 100000.0, 3.0}, {389.41, 100000.0, 3.0},
                {404.3, 100000.0, 3.0}, {404.216, 100000.0, 3.0}, {387.951, 100000.0, 3.0},
                {413.064, 100000.0, 3.0}, {680.237, 100000.0, 3.0}, {1.0, 100000.0, 3.0}};

        // setting link attributes
        for (Link linkToEast : linksToEast) {
            Link linkToWest = linksToWest.get(linksToEast.indexOf(linkToEast));

            linkToEast.setLength(linksAttributes[linksToEast.indexOf(linkToEast)][0]);
            linkToWest.setLength(linksAttributes[linksToEast.indexOf(linkToEast)][0]);
            linkToEast.setCapacity(linksAttributes[linksToEast.indexOf(linkToEast)][1]);
            linkToWest.setCapacity(linksAttributes[linksToEast.indexOf(linkToEast)][1]);
            linkToEast.setFreespeed(linksAttributes[linksToEast.indexOf(linkToEast)][2]);
            linkToWest.setFreespeed(linksAttributes[linksToEast.indexOf(linkToEast)][2]);
            linkToEast.setAllowedModes(Set.of("pt"));
            linkToWest.setAllowedModes(Set.of("pt"));
        }




        // starting to work on transit lines and routes
        TransitLine m2 = transitSchedule.getTransitLines().get(Id.create("M2---17446_900", TransitLine.class));

        // maybe not needed
        Id<TransitStopFacility> idAlex16toEast = Id.create("070301008816", TransitStopFacility.class);
        Id<TransitStopFacility> idAlex16toWest = Id.create("070301008816.1", TransitStopFacility.class);
        Id<TransitStopFacility> idAlex17toEast = Id.create("070301008817", TransitStopFacility.class);
        Id<TransitStopFacility> idAlex17toWest = Id.create("070301008817.1", TransitStopFacility.class);

        // TODO: only toEast and toWest arrays
        String[] routesAlex16toEast = {"0", "1", "2", "5", "8", "13", "14", "15"};
        String[] routesAlex16toWest = {"17", "18", "19", "22"};
        String[] routesAlex17toEast = {"3", "4", "9", "10", "16"};
        String[] routesAlex17toWest = {"23", "25", "26", "27", "28", "29", "30"};



        for (String routeNumber : routesAlex16toWest) {

            Id<TransitRoute> transitRouteId = Id.create(String.format("M2---17446_900_%s", routeNumber), TransitRoute.class);
            TransitRoute transitRoute = m2.getRoutes().get(transitRouteId);

            NetworkRoute networkRoute = transitRoute.getRoute();
            List<Id<Link>> routeLinkIds = networkRoute.getLinkIds();
            List<Id<Link>> newRouteLinkIds = new ArrayList<>(routeLinkIds);
            newRouteLinkIds.add(0, networkRoute.getStartLinkId());
            newRouteLinkIds.add(networkRoute.getEndLinkId());
            newRouteLinkIds.add(linkIdFromAlex16);
            newRouteLinkIds.addAll(linkIdsToWest);
            networkRoute.setLinkIds(networkRoute.getStartLinkId(), newRouteLinkIds, Id.createLinkId("pt_1_toWest"));

            List<TransitRouteStop> routeStopIds = transitRoute.getStops();
            TransitRouteStop stop = tsf.createTransitRouteStop(transitStopsToWest.get(0), 1.0, 1.0);
            // TODO: add stops


            /*if (transitRouteId.equals(Id.create("M2---17446_900_17", TransitRoute.class))) {
                System.out.println("route 17:");
                for (Id<Link> linkId: newRouteLinkIds) {
                    System.out.println(linkId.toString());
                }
            }*/

            // do departures
        }










        // use TransitScheduleValidator

        /*TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(transitSchedule, scenario.getNetwork());
        for (String error : result.getWarnings()) {
            LOGGER.warn(error);
        }
        for (String warning : result.getWarnings()) {
            LOGGER.warn(warning);
        }
        for (TransitScheduleValidator.ValidationResult.ValidationIssue issue : result.getIssues()) {
            LOGGER.warn(issue.getMessage());
        }*/

        // save to files
        NetworkWriter nw = new NetworkWriter(network);
        nw.write(outputNetwork);

        TransitScheduleWriter tsw = new TransitScheduleWriter(transitSchedule);
        tsw.writeFile(outputTransitSchedule);
    }
}
