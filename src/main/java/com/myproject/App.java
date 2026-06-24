package com.myproject;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class App {
    private static Graph addNodes(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index);
        }
        return graph;
    }

    private static Graph addNodesCool(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index).setAttribute("ui.style", "fill-color: #ffaabb; size: 30px;");
        }
        return graph;
    }
        private static Graph addNodesCool1(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index).setAttribute("ui.style", "fill-color: #aa9a1e; size: 30px;");
        }
        return graph;
    }
        private static Graph addNodesCool2(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index).setAttribute("ui.style", "fill-color: #a11f94; size: 30px;");
        }
        return graph;
    }
        private static Graph addNodesCool3(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index).setAttribute("ui.style", "fill-color: #27da3f; size: 30px;");
        }
        return graph;
    }
    private static ArrayList<Integer> initList( int amount){
        ArrayList<Integer> list = new ArrayList<>();

        for (int cool = 0; cool < amount; cool++){
            list.add(cool);
        }
        return list;
    }

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("Topology");
        int amount_of_edge_routers = 160;
        int amount_of_core_routers = 16;
        int amount_of_main_core_routers = 2;


        int amount_of_firewalling = 8;
        int amount_of_intrusion_detection = 8;
        int amount_of_web_proxing = 4;
        int amount_of_traffic_measurement = 4;





        // add nodes core router cr1 er1
        graph = addNodes(graph, "ER", amount_of_edge_routers);
        graph = addNodes(graph, "CR", amount_of_core_routers);
        graph = addNodes(graph, "M", amount_of_main_core_routers);
        
        graph = addNodesCool(graph, "FW", amount_of_firewalling);
        graph = addNodesCool1(graph, "IDS", amount_of_intrusion_detection);
        graph = addNodesCool2(graph, "WP", amount_of_web_proxing);
        graph = addNodesCool3(graph, "TM", amount_of_traffic_measurement);

        // connect core and main routers
        for (int indexMainCore = 0; indexMainCore < amount_of_main_core_routers; indexMainCore++) {
            for (int indexCore = 0; indexCore < amount_of_core_routers; indexCore++) {
                graph.addEdge("Main Core Router "+ indexMainCore + "| Core Router Connection " + indexCore,
                 "M" + indexMainCore, "CR" + indexCore);
            }
        }

        Random rand = new Random();

        List<Integer> list = new ArrayList<>();


        // for (int cool = 0; cool < amount_of_edge_routers; cool++){
        //     list.add(cool);
        // }
        list = initList(amount_of_edge_routers);


        // for (int cool = 0; cool < amount_of_firewalling; cool++){
        //     list1.add(cool);
        // }
        // for (int cool = 0; cool < amount_of_intrusion_detection; cool++){
        //     list2.add(cool);
        // }
        // for (int cool = 0; cool < amount_of_web_proxing; cool++){
        //     list3.add(cool);
        // }
        // for (int cool = 0; cool < amount_of_traffic_measurement; cool++){
        //     list4.add(cool);
        // }
        System.err.println("the list size: " + list.size());
        // // connect core routers and edge routers
        // // amount_of_core_routers = 5;
        for (int indexCore = 0; indexCore < amount_of_core_routers; indexCore++) {
            
            for (int indexEdge = 0; indexEdge < 10; indexEdge++){
                // System.err.println(rand.nextInt(list.size()));
                int z = rand.nextInt(list.size());
                graph.addEdge("Core Router Connection " + indexCore + "| Edge Router Connection " + indexEdge, "CR"+indexCore, "ER"+ list.get(z));
                System.err.println(list.get(z));
                list.remove(z);
            }
        }
        
        list = initList(amount_of_core_routers);
        for (int indexEdge = 0; indexEdge < amount_of_firewalling; indexEdge++){
            int z = rand.nextInt(list.size());
            graph.addEdge("Core Router Connection " + list.get(z) + "| fire wall Connection " + indexEdge, "CR"+list.get(z), "FW"+ indexEdge);
            System.err.println();
            list.remove(z);
        }
                list = initList(amount_of_core_routers);
        for (int indexEdge = 0; indexEdge < amount_of_intrusion_detection; indexEdge++){
            int z = rand.nextInt(list.size());
            graph.addEdge("Core Router Connection " + list.get(z) + "| amount_of_intrusion_detection Connection " + indexEdge, "CR"+list.get(z), "IDS"+ indexEdge);
            System.err.println();
            list.remove(z);
        }
                list = initList(amount_of_core_routers);
        for (int indexEdge = 0; indexEdge < amount_of_traffic_measurement; indexEdge++){
            int z = rand.nextInt(list.size());
            graph.addEdge("Core Router Connection " + list.get(z) + "| amount_of_traffic_measurement Connection " + indexEdge, "CR"+list.get(z), "TM"+ indexEdge);
            System.err.println();
            list.remove(z);
        }
                list = initList(amount_of_core_routers);
        for (int indexEdge = 0; indexEdge < amount_of_web_proxing; indexEdge++){
            int z = rand.nextInt(list.size());
            graph.addEdge("Core Router Connection " + list.get(z) + "| amount_of_web_proxing Connection " + indexEdge, "CR"+list.get(z), "WP"+ indexEdge);
            System.err.println();
            list.remove(z);
        }
        // // add the labes to the graph nodes

        for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
        }

        graph.setAttribute("ui.stylesheet",
            "node { fill-color: #4A90D9; size: 30px; text-size: 13; text-color: Black; text-style: bold; }" +
            "edge { fill-color: #888; size: 2px; }"
        );
        // Cross shape, pink color, 30px size for one specific node
        


        graph.display();
    }

}