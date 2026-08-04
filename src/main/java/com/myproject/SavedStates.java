package com.myproject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.graphstream.graph.Graph;

public class SavedStates {

    private static final String FOLDER = "saved_states";
    private static final String FOLDER2 = "successfulLP";

    private static final String SAVE_FOLDER =
            FOLDER + File.separator + FOLDER2;


   public static void saveState(
        Graph graph,
        List<Flow> flows,
        HashMap<String, EdgeRouter> fakeEdgeRouters) {

    try {
        // Make sure folder exists
        File folder = new File(SAVE_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Create the state
        SimulationState state = new SimulationState(graph, flows, fakeEdgeRouters);
        
        // serializable form before writing
        state.prepareForSave();

        // Create timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date());
        
        // Create filename
        String fileName = "network_state_" + timestamp + ".ser";
        File saveFile = new File(folder, fileName);

        // Save state
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            out.writeObject(state);
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
}

    public static SimulationState loadState(String name, Graph graph) {

        File file =
                new File(
                        SAVE_FOLDER +
                        File.separator +
                        name + ".ser"
                );

        try (ObjectInputStream in =
                     new ObjectInputStream(
                             new FileInputStream(file))) {

            SimulationState state =
                    (SimulationState) in.readObject();

            state.attachGraph(graph);

            System.out.println(
                    "Network state loaded: " +
                    file.getName()
            );

            return state;

        } catch (IOException | ClassNotFoundException e) {

            System.err.println(
                    "Failed to load state:"
            );

            e.printStackTrace();

            return null;
        }
    }
    public static SimulationState loadRandState(Graph graph) {

        File folder = new File(SAVE_FOLDER);


        // Check folder
        if (!folder.exists() || !folder.isDirectory()) {

            System.err.println(
                    "Save folder does not exist: " +
                    folder.getPath()
            );

            return null;
        }


        // Get all .ser files
        File[] files =
                folder.listFiles((dir, name) ->
                        name.endsWith(".ser")
                );


        // Check if there are saves
        if (files == null || files.length == 0) {

            System.err.println(
                    "No saved states found."
            );

            return null;
        }


        // Pick random file
        Random random = new Random();

        int randomIndex =
                random.nextInt(files.length);

        File selectedFile =
                files[randomIndex];


        System.out.println(
                "Number of saved states: " +
                files.length
        );

        System.out.println(
                "Loading random state: " +
                selectedFile.getName()
        );


        // Load state
        try (ObjectInputStream in =
                     new ObjectInputStream(
                             new FileInputStream(selectedFile))) {

            SimulationState state =
                    (SimulationState) in.readObject();

            state.attachGraph(graph);

            System.out.println(
                    "Network state loaded."
            );

            return state;

        } catch (IOException | ClassNotFoundException e) {

            System.err.println(
                    "Failed to load state:"
            );

            e.printStackTrace();

            return null;
        }
    }
public static void listSavedStates() {
    File folder = new File(SAVE_FOLDER);
    if (!folder.exists() || !folder.isDirectory()) {
        System.out.println("No saved states folder found.");
        return;
    }
    
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".ser"));
    if (files == null || files.length == 0) {
        System.out.println("No saved states found in " + SAVE_FOLDER);
        return;
    }
    
    System.out.println("\n=== SAVED STATES ===");
    for (int i = 0; i < files.length; i++) {
        System.out.println((i+1) + ". " + files[i].getName() + " (" + files[i].length() + " bytes)");
    }
    System.out.println("Total: " + files.length + " saved states\n");
}
}