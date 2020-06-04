package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class HomeworkHandler implements LinkEnterEventHandler {

    private final BufferedWriter writer ;
    ArrayList<Integer> ids = new ArrayList<>();
    ArrayList<Id<Vehicle>> vehicleIds = new ArrayList<>();

    public HomeworkHandler(String outputFile) {

        File file = new File("./scenarios/equil/links-to-be-reduced.txt");

        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                // reading lines, parsing integers (link IDs) and creating corresponding Id<Link>
                String line = scanner.nextLine()
                        .replace("Link", "")
                        .replace(" ", "");
                int id = Integer.parseInt(line);
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
        }

        try {
            FileWriter fileWriter = new FileWriter(outputFile);
            writer = new BufferedWriter(fileWriter);
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }

    }



    @Override
    public void handleEvent(LinkEnterEvent event) {

        for (int id : ids) {
            Id<Link> linkId = Id.createLinkId(id);
            if (event.getLinkId().equals(linkId)) {
                vehicleIds.add(event.getVehicleId());

            }
        }
    }
}