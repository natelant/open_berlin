package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;

import java.util.*;

// make it prepareTransitSchedule or prepareScenario in RunMatsim
public class TransitScheduleModifier {

    static String input = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
    static String output = "./scenarios/equil/transit-schedule-modified.xml.gz";

    // to do: change to RunMatsim.class
    private static final Logger LOGGER = Logger.getLogger(TransitScheduleModifier.class);

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig("./scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        TransitScheduleFactory tsf = transitSchedule.getFactory();

        HashMap<String, Double[]> stops = new HashMap<>(Map.of(
                "Berliner Rathaus", new Double[]{4595741.283812712, 5821391.332894235},
                "Fischerinsel", new Double[]{4595584.307828543, 5820979.312737804},
                "U Spittelmarkt", new Double[]{4595311.298656522, 5820721.322711342},
                "Jerusalemer Str.", new Double[]{4594868.306563006, 5820661.307287242},
                "U Stadtmitte/Leipziger Str.", new Double[]{4594469.274947998, 5820596.279395453},
                "Leipziger Str./Wilhelmstr.", new Double[]{4594082.239244875, 5820553.33117294},
                "S+U Potsdamer Platz", new Double[]{4593681.244368908, 5820501.353559958},
                "Kulturforum", new Double[]{4593018.0, 5820324.0}));

        // check and update travel times!
        List<Double> travelTimesWestToEast = List.of(100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0);

        // some of these lists are not necessary, remove them later
        ArrayList<Id<Node>> nodeIdsToEast = new ArrayList<>(9);
        ArrayList<Id<Node>> nodeIdsToWest = new ArrayList<>(9);
        ArrayList<Node> nodesToEast = new ArrayList<>(9);
        ArrayList<Node> nodesToWest = new ArrayList<>(9);
        ArrayList<Link> linksToEast = new ArrayList<>(9);
        ArrayList<Link> linksToWest = new ArrayList<>(9);
        ArrayList<Id<TransitStopFacility>> transitStopsToEast = new ArrayList<>(9);
        ArrayList<Id<TransitStopFacility>> transitStopsToWest = new ArrayList<>(9);

        for (String stop: stops.keySet()) {

            Coord coord = new Coord(stops.get(stop)[0], stops.get(stop)[1]);

            // creating pt_Nodes (in both directions, toEast/toWest), adding to Network
            Id<Node> nodeIdToEast = Id.createNodeId(String.format("pt_%s", stop));
            nodeIdsToEast.add(nodeIdToEast);
            Node nodeToEast = nf.createNode(nodeIdToEast, coord);
            network.addNode(nodeToEast);
            nodesToEast.add(nodeToEast);

            Id<Node> nodeIdToWest = Id.createNodeId(String.format("pt_%s", stop));
            nodeIdsToWest.add(nodeIdToWest);
            Node nodeToWest = nf.createNode(nodeIdToWest, coord);
            network.addNode(nodeToWest);
            nodesToWest.add(nodeToWest);

            // creating TransitStops (in both directions, toEast/toWest), adding to TransitSchedule
            Id<TransitStopFacility> idToEast = Id.create(stop, TransitStopFacility.class);
            transitStopsToEast.add(idToEast);
            TransitStopFacility transitStopFacilityToEast = tsf.createTransitStopFacility(idToEast, coord, false);
            transitStopFacilityToEast.setName(String.format("Berlin, %s", stop));
            transitSchedule.addStopFacility(transitStopFacilityToEast);

            Id<TransitStopFacility> idToWest = Id.create(stop, TransitStopFacility.class);
            transitStopsToWest.add(idToWest);
            TransitStopFacility transitStopFacilityToWest = tsf.createTransitStopFacility(idToWest, coord, false);
            transitStopFacilityToWest.setName(String.format("Berlin, %s", stop));
            transitSchedule.addStopFacility(transitStopFacilityToWest);
        }

        // create and add pt_Links
        {
            int counter = 0;
            for (Node toNode : nodesToEast) {
                if (counter != 0) {
                    Node fromNode = nodesToEast.get(nodesToEast.indexOf(toNode) - 1);
                    Id<Link> linkId = Id.createLinkId(String.format("pt_%d_toEast", counter));
                    Link link = nf.createLink(linkId, fromNode, toNode);
                    network.addLink(link);
                    linksToEast.add(link);
                }
                counter++;
            }

            for (Node toNode : nodesToWest) {
                if (counter != 8) {
                    Node fromNode = nodesToWest.get(nodesToEast.indexOf(toNode) - 1);
                    Id<Link> linkId = Id.createLinkId(String.format("pt_%d_toWest", counter));
                    Link link = nf.createLink(linkId, fromNode, toNode);
                    network.addLink(link);
                    linksToWest.add(link);
                }
                counter--;
            }
        }

        // manually adding pt_Links between Alex and Berliner Rathaus, because there are
        // two different pt_Nodes of Alexanderplatz/Dircksenstr.
        {
            Node alex16 = network.getNodes().get(Id.createNodeId("pt_070301008816"));
            Node alex17 = network.getNodes().get(Id.createNodeId("pt_070301008817"));
            Node rathausToEast = nodesToEast.get(7);
            Node rathausToWest = nodesToWest.get(7);

            Id<Link> linkIdToAlex16 = Id.createLinkId(String.format("pt_%d_toEast_to16", 8));
            Link rathausToAlex16 = nf.createLink(linkIdToAlex16, rathausToEast, alex16);
            network.addLink(rathausToAlex16);
            linksToEast.add(rathausToAlex16);

            Id<Link> linkIdToAlex17 = Id.createLinkId(String.format("pt_%d_toEast_to17", 8));
            Link rathausToAlex17 = nf.createLink(linkIdToAlex17, rathausToEast, alex17);
            network.addLink(rathausToAlex17);
            linksToEast.add(rathausToAlex17);

            Id<Link> linkIdFromAlex16 = Id.createLinkId(String.format("pt_%d_toWest_from16", 8));
            Link fromAlex16 = nf.createLink(linkIdFromAlex16, alex17, rathausToWest);
            network.addLink(fromAlex16);
            linksToWest.add(fromAlex16);

            Id<Link> linkIdFromAlex17 = Id.createLinkId(String.format("pt_%d_toWest_from17", 8));
            Link fromAlex17 = nf.createLink(linkIdFromAlex17, alex17, rathausToWest);
            network.addLink(fromAlex17);
            linksToWest.add(fromAlex17);
        }

        // TODO: link attributes (length, capacity, freespeed)
        double[][] linksAttributes = {
                {1.0, 2.0, 3.0}, {1.0, 2.0, 3.0}, {1.0, 2.0, 3.0},
                {1.0, 2.0, 3.0}, {1.0, 2.0, 3.0}, {1.0, 2.0, 3.0},
                {1.0, 2.0, 3.0}, {1.0, 2.0, 3.0}, {1.0, 2.0, 3.0}};

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


        Id<TransitStopFacility> idAlex16 = Id.create("070301008816", TransitStopFacility.class);
        Id<TransitStopFacility> idAlex16_1 = Id.create("070301008816.1", TransitStopFacility.class);
        Id<TransitStopFacility> idAlex17 = Id.create("070301008817", TransitStopFacility.class);
        Id<TransitStopFacility> idAlex17_1 = Id.create("070301008817.1", TransitStopFacility.class);







/*


        // HEADING EAST
        // for bla bla routes (keys):
        TransitRoute transitRoute = m2.getRoutes().get(Id.create("M2---17446_900_17", TransitRoute.class));

        // inner loop: for stops from the list
        TransitRouteStop transitRouteStop = tsf.createTransitRouteStop(transitStopFacility, arrOffset, depOffset);
        transitRoute.getStops().add(index, transitRouteStop);

        // for all following stops: increase offset times



        //HEADING WEST
        TransitRoute transitRoute = m2.getRoutes().get(Id.create("M2---17446_900_17", TransitRoute.class));

        TransitRouteStop transitRouteStop = tsf.createTransitRouteStop(transitStopFacility, arrOffset, depOffset);
        transitRoute.getStops().add(transitRouteStop);
*/













        // use TransitScheduleValidator

        TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(transitSchedule, scenario.getNetwork());
        for (String error : result.getWarnings()) {
            LOGGER.warn(error);
        }
        for (String warning : result.getWarnings()) {
            LOGGER.warn(warning);
        }
        for (TransitScheduleValidator.ValidationResult.ValidationIssue issue : result.getIssues()) {
            LOGGER.warn(issue.getMessage());
        }

        // save to file
        TransitScheduleWriter tsw = new TransitScheduleWriter(transitSchedule);
        tsw.writeFile(output);
    }
}
