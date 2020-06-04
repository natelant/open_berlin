package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class NetworkModifierHomework1 {

    public static void main(String[] args) {

        // defining paths to network files
        String input = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
        String output = "scenarios/equil/network-reduced-lanes.xml.gz";

        // creating and reading network from input
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(input);

        // defining path to .txt file with links to be reduced
        File file = new File("./scenarios/equil/links-to-be-reduced.txt");
        ArrayList<Integer> ids = new ArrayList<>();
        int counter = 0;

        // try-catch block to handle FileNotFoundException
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

        // check: for every link ID from .txt file get number of lanes,
        //        then if there are more than 1 lanes, reduce by 1
        for (int id : ids) {
            Id<Link> linkId = Id.createLinkId(id);
            if (network.getLinks().containsKey(linkId)) {
                double currNumLanes = network.getLinks().get(linkId).getNumberOfLanes();
                if (currNumLanes > 1) {
                    network.getLinks().get(linkId).setNumberOfLanes(currNumLanes - 1);
                    counter++;
                }
            }
        }

        System.out.println(counter + " links have been modified");

        // writing resulting network to output file
        new NetworkWriter(network).write(output);
    }
}
