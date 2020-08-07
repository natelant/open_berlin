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

        // network, transitSchedule, factories initialization
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(inputNetwork);
        config.transit().setTransitScheduleFile(inputTransitSchedule);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network network = scenario.getNetwork();
        network.getAttributes().removeAttribute("coordinateReferenceSystem");
        NetworkFactory nf = network.getFactory();

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        transitSchedule.getAttributes().removeAttribute("coordinateReferenceSystem");
        TransitScheduleFactory tsf = transitSchedule.getFactory();

        // input data for Nodes/TransitStopFacilities/TransitRouteStops
        String[] stopNames = {
                "Memhardstr.", "S+U_Alexanderplatz/Dircksenstr.", "Berliner_Rathaus", "Fischerinsel",
                "U_Spittelmarkt", "Jerusalemer_Str.", "U_Stadtmitte/Leipziger_Str.",
                "Leipziger_Str./Wilhelmstr.", "S+U_Potsdamer_Platz", "Kulturforum"};
        double[][] stopCoords = {
                {4595951.256039302, 5822171.279169948}, {4595891.300747029, 5821904.293893884},
                {4595741.283812712, 5821391.332894235}, {4595543.276787662, 5821032.276349911},
                {4595311.298656522, 5820721.322711342}, {4594868.306563006, 5820661.307287242},
                {4594469.274947998, 5820596.279395453}, {4594082.239244875, 5820553.33117294},
                {4593681.244368908, 5820501.353559958}, {4593018.0, 5820324.0}};

        // lists to store Nodes, Links and TSFacilities in both directions
        ArrayList<Node> nodesToEast = new ArrayList<>();
        ArrayList<Node> nodesToWest = new ArrayList<>();
        ArrayList<Id<Link>> linkIdsToEast = new ArrayList<>();
        ArrayList<Id<Link>> linkIdsToWest = new ArrayList<>();
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

            // creating TransitStopFacilities (in both directions, toEast/toWest), adding to TransitSchedule
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
        Collections.reverse(facilitiesToEast);

        // create and add first pt_Link
        Id<Link> firstLinkId = Id.createLinkId("pt_0_toEast");
        Link firstLink = nf.createLink(firstLinkId, nodesToEast.get(0), nodesToEast.get(0));
        firstLink.setLength(50);
        firstLink.setCapacity(100000.0);
        firstLink.setFreespeed(0.1);
        firstLink.setAllowedModes(Set.of("pt"));
        network.addLink(firstLink);

        // create and add all other pt_Links
        for (int i=0; i<nodesToEast.size(); i++) {
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
                Id<Link> linkIdToWest = Id.createLinkId(String.format("pt_%d_toWest", nodesToWest.size()-i));
                Link linkToWest = nf.createLink(linkIdToWest, fromNodeToWest, toNodeToWest);
                linkIdsToWest.add(linkIdToWest);
                facilitiesToWest.get(i).setLinkId(linkIdToWest);
                network.addLink(linkToWest);
            }
        }

        // setting ref link ids for first/last stop
        facilitiesToEast.get(0).setLinkId(firstLinkId);
        facilitiesToWest.get(0).setLinkId(Id.createLinkId("pt_38662"));
        Collections.reverse(facilitiesToEast);

        // connecting Memhardstr. stop with existing links
        {
            network.getLinks().get(Id.createLinkId("pt_38639")).setFromNode(nodesToEast.get(9));
            network.removeLink(Id.createLinkId("pt_38638"));
            network.removeLink(Id.createLinkId("pt_38653"));

            network.getLinks().get(Id.createLinkId("pt_38662")).setToNode(nodesToWest.get(0));
            network.removeLink(Id.createLinkId("pt_38663"));
            network.removeLink(Id.createLinkId("pt_38671"));
        }

        // data for link length and travel times between stops
        double[] linksLength = {696.39, 404.35, 389.41, 404.3, 404.216, 387.951, 413.064, 680.237, 273.63};
        ArrayList<Double> travelTimesToEast = new ArrayList<>(
                List.of(120.0, 60.0, 60.0, 60.0, 60.0, 60.0, 60.0, 120.0, 120.0));
        ArrayList<Double> travelTimesToWest = new ArrayList<>(travelTimesToEast);
        Collections.reverse(travelTimesToWest);

        // setting link attributes
        for (int i=0; i<linkIdsToEast.size(); i++) {
            Link linkToEast = network.getLinks().get(linkIdsToEast.get(i));
            linkToEast.setLength(linksLength[0]);
            linkToEast.setCapacity(100000.0);
            linkToEast.setFreespeed( linkToEast.getLength() / (travelTimesToEast.get(i) - 30.0));
            linkToEast.setAllowedModes(Set.of("pt"));

            Link linkToWest = network.getLinks().get(linkIdsToWest.get(i));
            linkToWest.setLength(linksLength[0]);
            linkToWest.setCapacity(100000.0);
            linkToWest.setFreespeed( linkToWest.getLength() / (travelTimesToWest.get(i) - 30.0));
            linkToWest.setAllowedModes(Set.of("pt"));
        }


        // .........
        // .........
        // .........
        // starting to work on transit lines and routes
        // .........
        // .........
        // .........


        // data for dep/arr offsets, manually calculated using travel times
        ArrayList<Double> timeOffsetToEast = new ArrayList<>(
                List.of(0.0, 120.0, 180.0, 240.0, 300.0, 360.0, 420.0, 480.0, 600.0, 720.0));
        ArrayList<Double> timeOffsetToWest = new ArrayList<>(
                List.of(120.0, 180.0, 300.0, 360.0, 420.0, 480.0, 540.0, 600.0, 660.0, 780.0));
        Collections.reverse(timeOffsetToEast);

        // route numbers, manually collected from transit-schedule.xml
        String[] routesToEast = {"0", "1", "2", "3", "4", "5", "8", "9", "10", "13", "14", "15", "16"};
        String[] routesToWest = {"17", "18", "19", "22", "23", "25", "26", "27", "28", "29", "30"};

        List<TransitRouteStop> stopsToEast = new ArrayList<>();
        List<TransitRouteStop> stopsToWest = new ArrayList<>();

        TransitLine m2 = transitSchedule.getTransitLines().get(Id.create("M2---17446_900", TransitLine.class));

        // creating TransitRouteStops using TSFacilities and calculated offsets
        for (int i=0; i<facilitiesToEast.size(); i++) {
            double offsetToEast = timeOffsetToEast.get(i);
            TransitRouteStop stopToEast = tsf.createTransitRouteStop(facilitiesToEast.get(i), offsetToEast, offsetToEast);
            stopToEast.setAwaitDepartureTime(true);
            stopsToEast.add(stopToEast);

            double offsetToWest= timeOffsetToWest.get(i);
            TransitRouteStop stopToWest = tsf.createTransitRouteStop(facilitiesToWest.get(i), offsetToWest, offsetToWest);
            stopToWest.setAwaitDepartureTime(true);
            stopsToWest.add(stopToWest);
        }

        Collections.reverse(stopsToEast);

        // modifying routes heading east
        for (String routeNumber : routesToEast) {

            // getting transit route with particular number
            Id<TransitRoute> transitRouteId = Id.create(String.format("M2---17446_900_%s", routeNumber), TransitRoute.class);
            TransitRoute transitRoute = m2.getRoutes().get(transitRouteId);

            // adding new links, saving them to network route
            NetworkRoute networkRoute = transitRoute.getRoute();
            {
                List<Id<Link>> routeLinkIds = new ArrayList<>(networkRoute.getLinkIds());
                routeLinkIds.remove(0);

                List<Id<Link>> newRouteLinkIds = new ArrayList<>(linkIdsToEast);
                newRouteLinkIds.addAll(routeLinkIds);

                networkRoute.setLinkIds(firstLinkId, newRouteLinkIds, networkRoute.getEndLinkId());
            }

            // adding new stops, saving them to "stops" list
            List<TransitRouteStop> stops = new ArrayList<>(stopsToEast);
            {
                for (TransitRouteStop currStop : transitRoute.getStops()) {
                    double offset = currStop.getDepartureOffset().seconds() + timeOffsetToEast.get(1);
                    TransitStopFacility facility = currStop.getStopFacility();
                    TransitRouteStop stop = tsf.createTransitRouteStop(facility, offset, offset);
                    stop.setAwaitDepartureTime(true);
                    stops.add(stop);
                }
                stops.remove(stopsToEast.size());
                stops.remove(stopsToEast.size());
            }

            // creating modified TransitRoute
            TransitRoute newTransitRoute = tsf.createTransitRoute(transitRouteId, networkRoute, stops, "tram");

            // shifting departures 10 min earlier, so that existing part of the route keeps its schedule
            for (Id<Departure> departureId: transitRoute.getDepartures().keySet()) {
                double departureTime = transitRoute.getDepartures().get(departureId).getDepartureTime();
                double newDepartureTime = departureTime - timeOffsetToEast.get(1);
                // make sure no negative time appears in the schedule (applies only to 1 Departure of 1 TransitRoute)
                if (newDepartureTime > 0.0) {
                    Departure newDeparture = tsf.createDeparture(departureId, newDepartureTime);
                    newDeparture.setVehicleId(transitRoute.getDepartures().get(departureId).getVehicleId());
                    newTransitRoute.addDeparture(newDeparture);
                }
            }

            // changing transit route with modified one
            m2.removeRoute(transitRoute);
            m2.addRoute(newTransitRoute);
        }

        // modifying routes heading east
        for (String routeNumber : routesToWest) {

            // getting transit route with particular number
            Id<TransitRoute> transitRouteId = Id.create(String.format("M2---17446_900_%s", routeNumber), TransitRoute.class);
            TransitRoute transitRoute = m2.getRoutes().get(transitRouteId);

            // adding new links, saving them to network route
            NetworkRoute networkRoute = transitRoute.getRoute();
            {
                List<Id<Link>> newRouteLinkIds = new ArrayList<>(networkRoute.getLinkIds());
                newRouteLinkIds.addAll(linkIdsToWest);
                newRouteLinkIds.remove(Id.createLinkId("pt_1_toWest"));
                networkRoute.setLinkIds(networkRoute.getStartLinkId(), newRouteLinkIds, Id.createLinkId("pt_1_toWest"));
            }

            // adding new stops, saving them to "stops" list
            List<TransitRouteStop> stops = new ArrayList<>(transitRoute.getStops());
            {
                stops.remove(stops.size() - 1);
                stops.remove(stops.size() - 1);
                double newOffset = stops.get(stops.size() - 1).getDepartureOffset().seconds();

                for (TransitRouteStop stopToWest : stopsToWest) {
                    double offset = stopToWest.getDepartureOffset().seconds() + newOffset;
                    TransitStopFacility facility = stopToWest.getStopFacility();
                    TransitRouteStop stop = tsf.createTransitRouteStop(facility, offset, offset);
                    stop.setAwaitDepartureTime(true);
                    stops.add(stop);
                }
            }

            // creating modified TransitRoute
            TransitRoute newTransitRoute = tsf.createTransitRoute(transitRouteId, networkRoute, stops, "tram");

            // re-creating all Departures
            for (Id<Departure> departureId: transitRoute.getDepartures().keySet()) {
                double departureTime = transitRoute.getDepartures().get(departureId).getDepartureTime();
                Departure newDeparture = tsf.createDeparture(departureId, departureTime);
                newDeparture.setVehicleId(transitRoute.getDepartures().get(departureId).getVehicleId());
                newTransitRoute.addDeparture(newDeparture);
            }

            // changing transit route with modified one
            m2.removeRoute(transitRoute);
            m2.addRoute(newTransitRoute);
        }

        // using TransitScheduleValidator
        TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(transitSchedule, network);
        for (String error : result.getWarnings()) {
            LOGGER.warn(error);
        }
        for (String warning : result.getWarnings()) {
            LOGGER.warn(warning);
        }
        for (var issue : result.getIssues()) {
            LOGGER.warn(issue.getMessage());
        }

        // save to output files
        NetworkWriter nw = new NetworkWriter(network);
        nw.write(outputNetwork);

        TransitScheduleWriter tsw = new TransitScheduleWriter(transitSchedule);
        tsw.writeFile(outputTransitSchedule);
    }
}
