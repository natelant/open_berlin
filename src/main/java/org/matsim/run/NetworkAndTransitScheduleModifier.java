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
import org.matsim.pt.utils.TransitScheduleValidator;

import java.util.*;

// make it prepareTransitSchedule or prepareScenario in RunMatsim
public class NetworkAndTransitScheduleModifier {

    static String inputTransitSchedule = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
    static String inputNetwork = "scenarios/equil/network-reduced-lanes-HW2.xml.gz";
    static String outputNetwork = "./scenarios/equil/network-modified-HW2.xml.gz";
    static String outputTransitSchedule = "./scenarios/equil/transit-schedule-modified-HW2.xml.gz";

    // Logger
    private static final Logger LOGGER = Logger.getLogger(NetworkAndTransitScheduleModifier.class);

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(inputNetwork);
        config.transit().setTransitScheduleFile(inputTransitSchedule);

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

        ArrayList<Node> nodesToEast = new ArrayList<>();
        ArrayList<Node> nodesToWest = new ArrayList<>();
        ArrayList<TransitStopFacility> facilitiesToEast = new ArrayList<>();
        ArrayList<TransitStopFacility> facilitiesToWest = new ArrayList<>();

        for (int i=0; i<stopNames.length; i++) {

            String stop = stopNames[i];
            double[] coords = stopCoords[i];
            Coord coord = new Coord(coords[0], coords[1]);

            // creating pt_Nodes (in both directions, toEast/toWest), adding to Network
            Id<Node> nodeIdToEast = Id.createNodeId(String.format("pt_%s_toEast", stop));
            Node nodeToEast = nf.createNode(nodeIdToEast, coord);
            network.addNode(nodeToEast);
            nodesToEast.add(nodeToEast);

            Id<Node> nodeIdToWest = Id.createNodeId(String.format("pt_%s_toWest", stop));
            Node nodeToWest = nf.createNode(nodeIdToWest, coord);
            network.addNode(nodeToWest);
            nodesToWest.add(nodeToWest);

            // creating TransitStops (in both directions, toEast/toWest), adding to TransitSchedule
            Id<TransitStopFacility> idToEast = Id.create(stop + "_toEast", TransitStopFacility.class);
            TransitStopFacility transitStopFacilityToEast = tsf.createTransitStopFacility(idToEast, coord, false);
            transitStopFacilityToEast.setName(String.format("Berlin, %s", stop));
            facilitiesToEast.add(transitStopFacilityToEast);
            transitSchedule.addStopFacility(transitStopFacilityToEast);

            Id<TransitStopFacility> idToWest = Id.create(stop + "_toWest", TransitStopFacility.class);
            TransitStopFacility transitStopFacilityToWest = tsf.createTransitStopFacility(idToWest, coord, false);
            transitStopFacilityToWest.setName(String.format("Berlin, %s", stop));
            facilitiesToWest.add(transitStopFacilityToWest);
            transitSchedule.addStopFacility(transitStopFacilityToWest);
        }

        Collections.reverse(nodesToEast);

        ArrayList<Id<Link>> linkIdsToEast = new ArrayList<>();
        ArrayList<Id<Link>> linkIdsToWest = new ArrayList<>();

        // TODO: link attributes (freespeed)
        double[][] linksAttributes = {
                {696.39, 100000.0, 3.0}, {404.35, 100000.0, 3.0}, {389.41, 100000.0, 3.0},
                {404.3, 100000.0, 3.0}, {404.216, 100000.0, 3.0}, {387.951, 100000.0, 3.0},
                {413.064, 100000.0, 3.0}, {680.237, 100000.0, 3.0}};

        // create and add pt_Links
        for (int i=0; i<8; i++) {
            if (i!=0) {
                Node toNodeToEast = nodesToEast.get(i);
                Node fromNodeToEast = nodesToEast.get(i-1);
                Id<Link> linkIdToEast = Id.createLinkId(String.format("pt_%d_toEast", i));
                Link linkToEast = nf.createLink(linkIdToEast, fromNodeToEast, toNodeToEast);
                facilitiesToEast.get(i).setLinkId(linkIdToEast);
                linkIdsToEast.add(linkIdToEast);
                network.addLink(linkToEast);

                Node toNodeToWest = nodesToWest.get(i);
                Node fromNodeToWest = nodesToWest.get(i-1);
                Id<Link> linkIdToWest = Id.createLinkId(String.format("pt_%d_toWest", 8-i));
                Link linkToWest = nf.createLink(linkIdToWest, fromNodeToWest, toNodeToWest);
                facilitiesToWest.get(i).setLinkId(linkIdToWest);
                linkIdsToWest.add(linkIdToWest);
                network.addLink(linkToWest);
            }
        }

        // manually adding pt_Links between Alex and Berliner Rathaus, because there are
        // two different pt_Nodes of Alexanderplatz/Dircksenstr.
        List<Link> alexLinks = new ArrayList<>();
        Node alex16 = network.getNodes().get(Id.createNodeId("pt_070301008816"));
        Node alex17 = network.getNodes().get(Id.createNodeId("pt_070301008817"));
        Node rathausToEast = nodesToEast.get(7);
        Node rathausToWest = nodesToWest.get(0);

        Id<Link> linkIdAlex16toEast = Id.createLinkId("pt_8_toEast_to16");
        Link rathausToAlex16 = nf.createLink(linkIdAlex16toEast, rathausToEast, alex16);
        alexLinks.add(rathausToAlex16);
        network.addLink(rathausToAlex16);

        Id<Link> linkIdAlex17toEast = Id.createLinkId("pt_8_toEast_to17");
        Link rathausToAlex17 = nf.createLink(linkIdAlex17toEast, rathausToEast, alex17);
        alexLinks.add(rathausToAlex17);
        network.addLink(rathausToAlex17);

        Id<Link> linkIdAlex16toWest = Id.createLinkId("pt_8_toWest_from16");
        Link fromAlex16 = nf.createLink(linkIdAlex16toWest, alex16, rathausToWest);
        alexLinks.add(fromAlex16);
        network.addLink(fromAlex16);

        Id<Link> linkIdAlex17toWest = Id.createLinkId("pt_8_toWest_from17");
        Link fromAlex17 = nf.createLink(linkIdAlex17toWest, alex17, rathausToWest);
        alexLinks.add(fromAlex17);
        network.addLink(fromAlex17);

        // setting link attributes
        for (int i=0; i<linkIdsToEast.size(); i++) {
            Link linkToEast = network.getLinks().get(linkIdsToEast.get(i));
            Link linkToWest = network.getLinks().get(linkIdsToWest.get(i));

            linkToEast.setLength(linksAttributes[i][0]);
            linkToWest.setLength(linksAttributes[i][0]);
            linkToEast.setCapacity(linksAttributes[i][1]);
            linkToWest.setCapacity(linksAttributes[i][1]);
            linkToEast.setFreespeed(linksAttributes[i][2]);
            linkToWest.setFreespeed(linksAttributes[i][2]);
            linkToEast.setAllowedModes(Set.of("pt"));
            linkToWest.setAllowedModes(Set.of("pt"));
        }

        for (Link link: alexLinks) {
            link.setLength(linksAttributes[7][0]);
            link.setCapacity(linksAttributes[7][1]);
            link.setFreespeed(linksAttributes[7][2]);
            link.setAllowedModes(Set.of("pt"));
        }


        // .........
        // .........
        // .........
        // starting to work on transit lines and routes
        // .........
        // .........
        // .........


        // TODO: check and update travel times!
        ArrayList<Double> timeOffsetToEast = new ArrayList<>(
                List.of(0.0, 60.0, 120.0, 180.0, 240.0, 300.0, 360.0, 420.0, 480.0));
        ArrayList<Double> timeOffsetToWest = new ArrayList<>(
                List.of(120.0, 240.0, 360.0, 480.0, 600.0, 720.0, 840.0, 960.0));
        Collections.reverse(timeOffsetToEast);

        // route numbers, manually collected from transit-schedule.xml
        String[] routesToEast = {"0", "1", "2", "3", "4", "5", "8", "9", "10", "13", "14", "15", "16"};
        String[] routesToWest = {"17", "18", "19", "22", "23", "25", "26", "27", "28", "29", "30"};

        List<TransitRouteStop> stopsToEast = new ArrayList<>();
        List<TransitRouteStop> stopsToWest = new ArrayList<>();

        TransitLine m2 = transitSchedule.getTransitLines().get(Id.create("M2---17446_900", TransitLine.class));

        for (int i=0; i<8; i++) {
            TransitStopFacility facilityToEast = facilitiesToEast.get(i);
            double offsetToEast = timeOffsetToEast.get(i+1);
            TransitRouteStop stopToEast = tsf.createTransitRouteStop(facilityToEast, offsetToEast, offsetToEast);
            stopToEast.setAwaitDepartureTime(true);
            stopsToEast.add(stopToEast);

            TransitStopFacility facilityToWest = facilitiesToWest.get(i);
            double offsetToWest= timeOffsetToWest.get(i);
            TransitRouteStop stopToWest = tsf.createTransitRouteStop(facilityToWest, offsetToWest, offsetToWest);
            stopToWest.setAwaitDepartureTime(true);
            stopsToWest.add(stopToWest);
        }

        Collections.reverse(stopsToEast);

        for (String routeNumber : routesToEast) {

            Id<TransitRoute> transitRouteId = Id.create(String.format("M2---17446_900_%s", routeNumber), TransitRoute.class);
            TransitRoute transitRoute = m2.getRoutes().get(transitRouteId);

            NetworkRoute networkRoute = transitRoute.getRoute();
            List<Id<Link>> routeLinkIds = networkRoute.getLinkIds();
            List<Id<Link>> newRouteLinkIds = new ArrayList<>(linkIdsToEast);
            newRouteLinkIds.remove(0);

            if (Set.of("3", "4", "9", "10", "16").contains(routeNumber)) {
                newRouteLinkIds.add(linkIdAlex16toEast);
            } else {
                newRouteLinkIds.add(linkIdAlex17toEast);
            }

            newRouteLinkIds.add(networkRoute.getStartLinkId());
            newRouteLinkIds.addAll(routeLinkIds);
            networkRoute.setLinkIds(Id.createLinkId("pt_1_toEast"), newRouteLinkIds, networkRoute.getEndLinkId());

            List<TransitRouteStop> stops = new ArrayList<>(stopsToEast);

            for (TransitRouteStop currStop: transitRoute.getStops()) {
                double offset = currStop.getDepartureOffset().seconds() + timeOffsetToEast.get(0);
                TransitStopFacility facility = currStop.getStopFacility();
                TransitRouteStop stop = tsf.createTransitRouteStop(facility, offset, offset);
                stop.setAwaitDepartureTime(true);
                stops.add(stop);
            }

            TransitRoute newTransitRoute = tsf.createTransitRoute(transitRouteId, networkRoute, stops, "tram");

            for (Id<Departure> departureId: transitRoute.getDepartures().keySet()) {
                double departureTime = transitRoute.getDepartures().get(departureId).getDepartureTime();
                Departure newDeparture = tsf.createDeparture(departureId, departureTime - timeOffsetToEast.get(0));
                newDeparture.setVehicleId(transitRoute.getDepartures().get(departureId).getVehicleId());
                newTransitRoute.addDeparture(newDeparture);
            }

            m2.removeRoute(transitRoute);
            m2.addRoute(newTransitRoute);
        }

        for (String routeNumber : routesToWest) {

            Id<TransitRoute> transitRouteId = Id.create(String.format("M2---17446_900_%s", routeNumber), TransitRoute.class);
            TransitRoute transitRoute = m2.getRoutes().get(transitRouteId);

            NetworkRoute networkRoute = transitRoute.getRoute();
            List<Id<Link>> routeLinkIds = networkRoute.getLinkIds();
            List<Id<Link>> newRouteLinkIds = new ArrayList<>(routeLinkIds);
            newRouteLinkIds.add(0, networkRoute.getStartLinkId());
            newRouteLinkIds.add(networkRoute.getEndLinkId());

            if (Set.of("17", "18", "19", "22").contains(routeNumber)) {
                newRouteLinkIds.add(linkIdAlex16toWest);
            } else {
                newRouteLinkIds.add(linkIdAlex17toWest);
            }

            newRouteLinkIds.addAll(linkIdsToWest);
            newRouteLinkIds.remove(Id.createLinkId("pt_1_toWest"));
            networkRoute.setLinkIds(networkRoute.getStartLinkId(), newRouteLinkIds, Id.createLinkId("pt_1_toWest"));

            List<TransitRouteStop> stops = new ArrayList<>(transitRoute.getStops());
            double newOffset = stops.get(stops.size()-1).getDepartureOffset().seconds();

            for (TransitRouteStop stopToWest: stopsToWest) {
                double offset = stopToWest.getDepartureOffset().seconds() + newOffset;
                TransitStopFacility facility = stopToWest.getStopFacility();
                TransitRouteStop stop = tsf.createTransitRouteStop(facility, offset, offset);
                stop.setAwaitDepartureTime(true);
                stops.add(stop);
            }

            TransitRoute newTransitRoute = tsf.createTransitRoute(transitRouteId, networkRoute, stops, "tram");
            m2.removeRoute(transitRoute);
            m2.addRoute(newTransitRoute);
        }

        // use TransitScheduleValidator
        TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(transitSchedule, network);
        for (String error : result.getWarnings()) {
            LOGGER.warn(error);
        }
        for (String warning : result.getWarnings()) {
            LOGGER.warn(warning);
        }
        for (TransitScheduleValidator.ValidationResult.ValidationIssue issue : result.getIssues()) {
            LOGGER.warn(issue.getMessage());
        }

        // save to files
        NetworkWriter nw = new NetworkWriter(network);
        nw.write(outputNetwork);

        TransitScheduleWriter tsw = new TransitScheduleWriter(transitSchedule);
        tsw.writeFile(outputTransitSchedule);
    }
}
