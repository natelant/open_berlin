package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class HomeworkHandler implements LinkEnterEventHandler {

    private final BufferedWriter writer;
    ArrayList<Integer> linkIds = new ArrayList<>();
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
                int linkId = Integer.parseInt(line);
                if (!linkIds.contains(linkId)) {
                    linkIds.add(linkId);
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
        for (int id : linkIds) {
            Id<Link> linkId = Id.createLinkId(id);
            if (event.getLinkId().equals(linkId) && !vehicleIds.contains(event.getVehicleId())) {
                vehicleIds.add(event.getVehicleId());
            }
        }
    }

    // print unique vehicles that use at least one link of Bundesallee
    public void printToFile() {
        try {
            writer.write("Vehicles using Bundesallee (total of " + vehicleIds.size() + ")\n\n");
            writer.write("Vehicle IDs:\n");
            for (Id<Vehicle> vehId : vehicleIds) {
                writer.write(vehId.toString() + "\n");
            }
            writer.close();
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
    }
}