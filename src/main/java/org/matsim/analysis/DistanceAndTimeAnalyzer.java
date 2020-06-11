package org.matsim.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class DistanceAndTimeAnalyzer implements LinkEnterEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

    Network network;
    ConcurrentHashMap<Id<Vehicle>, Double> distanceTravelled = new ConcurrentHashMap<>();
    ConcurrentHashMap<Id<Person>, Double> timeTravelled = new ConcurrentHashMap<>();

    public DistanceAndTimeAnalyzer(Network network) {
        this.network = network;

        // reading all-affected-agents' IDs
        try {
            Scanner scanner = new Scanner(new File("./scenarios/equil/handler-all-affected-agents_50.txt"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                distanceTravelled.put(Id.createVehicleId(line), 0.0);
                timeTravelled.put(Id.createPersonId(line), 0.0);
            }
        } catch (FileNotFoundException ee) {
            System.out.println("File not found!");
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (distanceTravelled.containsKey(event.getVehicleId())) {
            double currDistance = distanceTravelled.get(event.getVehicleId());
            double length = network.getLinks().get(event.getLinkId()).getLength();
            distanceTravelled.put(event.getVehicleId(), currDistance + length);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Id<Person> personId = event.getPersonId();
        if (timeTravelled.containsKey(personId)) {
            timeTravelled.put(personId, timeTravelled.get(personId) + event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Person> personId = event.getPersonId();
        if (timeTravelled.containsKey(event.getPersonId())) {
            timeTravelled.put(personId, timeTravelled.get(personId) - event.getTime());
        }
    }

    public void printAverageDistance() {
        for (Id<Vehicle> vehId : distanceTravelled.keySet()) {
            // removing those who used pt/bike
            if (distanceTravelled.get(vehId) == 0.0) {
                distanceTravelled.remove(vehId);
            }
        }
        double averageDistance = distanceTravelled.values().stream().mapToDouble(i -> i).sum() / distanceTravelled.size();
        System.out.println(averageDistance);
    }

    public void printAverageTravelTime() {
        for (Id<Person> personId : timeTravelled.keySet()) {
            if (timeTravelled.get(personId) == 0.0) {
                timeTravelled.remove(personId);
            }
        }
        double averageTime = timeTravelled.values().stream().mapToDouble(i -> i).sum() / timeTravelled.size();
        System.out.println(clockTime(averageTime));
    }

    private String clockTime(double seconds) {
        LocalTime timeOfDay = LocalTime.ofSecondOfDay((long)seconds);
        return timeOfDay.toString();
    }
}
