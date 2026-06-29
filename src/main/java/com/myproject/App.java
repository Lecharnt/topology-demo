package com.myproject;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
    private static void addStyledNodes(Graph graph, String prefix, int count, String hexColor) {
        for (int i = 0; i < count; i++) {
            graph.addNode(prefix + i).setAttribute("ui.style","fill-color: " + hexColor + "; size: 30px;");
        }
    }
    private static List<Integer> rangeList(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(i);
        return list;
    }
    private static void connectMiddleboxes(Graph graph, Random rand,int coreCount,String mbPrefix, int mbCount, String edgeLabelTag) {
        List<Integer> available = rangeList(coreCount);
        for (int mb = 0; mb < mbCount; mb++) {
            int z = rand.nextInt(available.size());
            int cr = available.get(z);
            available.remove(z);
            graph.addEdge("CR" + cr + "|" + edgeLabelTag + mb, "CR" + cr, mbPrefix + mb);
        }
    }
    public static class RoutingTable {
        private Node node;


        public RoutingTable(Node _node){
            this.node = _node;
        }
        public Node getNode(){return node;}
        public void setNode(Node _node){_node = node;}

        private Map<String, String> table = new HashMap<>();

        public void addRoute(String destination, String nextHop) {
            table.put(destination, nextHop);
        }

        public String getNextHop(String destination) {
            return table.getOrDefault(destination, null);
        }
    }
    private static HashMap<String, RoutingTable> setRouters(Graph graph){
        HashMap<String, RoutingTable> tables = new HashMap<String, RoutingTable>();
        for (Node node : graph) {
            RoutingTable routingTable = new RoutingTable(node);
            routingTable.setNode(node);
            tables.put(node.getId(), routingTable);
        }
        return tables;
    }
    private static HashMap<String, RoutingTable> setRouterEdges(Graph graph, HashMap<String, RoutingTable> tables){
        for (RoutingTable table : tables.values()) {
            System.out.println(table.getNode());
        }
        return tables;
    }

    private static ArrayList<Integer> initList( int amount){
        ArrayList<Integer> list = new ArrayList<>();

        for (int cool = 0; cool < amount; cool++){
            list.add(cool);
        }
        return list;
    }
    enum PolicyType {
        FW,
        IDS,
        WP,
        TM
    }
    enum RouterType{
        ER,
        CR,
        M
    }
    private static RouterType getRouterType(String id) {
        for (RouterType type : RouterType.values()) {
            if (id.startsWith(type.name())) {
                return type;
            }
        }
        return null;
    }
    private static Node findNextClosestMB(Node coreNode, PolicyType mbName) {
        if (coreNode == null)
            return null;

        for (Node x : coreNode.neighborNodes().toList()) {
            if (x.getId().startsWith(mbName.name())) {
                return x;
            }
        }

        return null;
    }
    private static Node findNextClosestRouter(Node coreNode, RouterType RName) {
        if (coreNode == null)
            return null;

        for (Node x : coreNode.neighborNodes().toList()) {
            if (x.getId().startsWith(RName.name())) {
                return x;
            }
        }

        return null;
    }
    private static Node findClosestMB(PolicyType MBName, Node node, Graph graph, int MaxHops){
        Node currentNode = node;
        Set<Node> exploredCoreRouters = new HashSet<>();

        while (MaxHops > 0 && currentNode != null && !currentNode.getId().startsWith(MBName.name())) {
            RouterType routerType = getRouterType(currentNode.getId());

            switch (routerType) {
                case ER://check for edge routers
                    currentNode = findNextClosestRouter(currentNode, RouterType.CR);
                    MaxHops--;
                    break;

                case CR://check for core routers
                    Node foundMB = findNextClosestMB(currentNode, MBName);
                    if (foundMB != null) {
                        return foundMB;
                    }
                    currentNode = findNextClosestRouter(currentNode, RouterType.M);
                    MaxHops--;
                    break;

                case M://check for middle routers
                    Node foundMBFromM = null;
                    for (Node cr : currentNode.neighborNodes().toList()) {
                        if (!cr.getId().startsWith(RouterType.CR.name())) continue;
                        if (exploredCoreRouters.contains(cr)) continue;

                        exploredCoreRouters.add(cr);

                        foundMBFromM = findNextClosestMB(cr, MBName);
                        if (foundMBFromM != null) {
                            return foundMBFromM;
                        }
                    }
                    currentNode = null;
                    MaxHops--;
                    break;

                default:
                    return null;
            }
        }
        return currentNode;
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

        HashMap<String, RoutingTable> routers = new HashMap<String, RoutingTable>();
        
        String fwColor  = "#ffaabb";
        String idsColor = "#aa9a1e";
        String wpColor  = "#a11f94";
        String tmColor  = "#27da3f";


        // add nodes core router cr1 er1
        graph = addNodes(graph, "ER", amount_of_edge_routers);
        graph = addNodes(graph, "CR", amount_of_core_routers);
        graph = addNodes(graph, "M", amount_of_main_core_routers);
        
        addStyledNodes(graph, "FW", amount_of_firewalling, fwColor);
        addStyledNodes(graph, "IDS", amount_of_intrusion_detection, idsColor);
        addStyledNodes(graph, "WP", amount_of_web_proxing, wpColor);
        addStyledNodes(graph, "TM", amount_of_traffic_measurement, tmColor);
        // connect core and main routers
        for (int m = 0; m < amount_of_main_core_routers; m++){
            for (int c = 0; c < amount_of_core_routers; c++){
                graph.addEdge("M" + m + " CR" + c, "M" + m, "CR" + c);
            }
        }
        Random rand = new Random();

        List<Integer> list = new ArrayList<>();
        list = initList(amount_of_edge_routers);

        for (int indexCore = 0; indexCore < amount_of_core_routers; indexCore++) {
            
            for (int indexEdge = 0; indexEdge < 10; indexEdge++){
                // System.err.println(rand.nextInt(list.size()));
                int z = rand.nextInt(list.size());
                graph.addEdge("Core Router Connection " + indexCore + "| Edge Router Connection " + indexEdge, "CR"+indexCore, "ER"+ list.get(z));
                list.remove(z);
            }
        }

        connectMiddleboxes(graph, rand, amount_of_core_routers, "FW", amount_of_firewalling, "FW");
        connectMiddleboxes(graph, rand, amount_of_core_routers, "IDS", amount_of_intrusion_detection, "IDS");
        connectMiddleboxes(graph, rand, amount_of_core_routers, "TM", amount_of_traffic_measurement, "TM");
        connectMiddleboxes(graph, rand, amount_of_core_routers, "WP", amount_of_web_proxing, "WP");

        routers = setRouters(graph);

        // add the labes to the graph nodes

        for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
        }

        graph.setAttribute("ui.stylesheet",
            "node { fill-color: #4A90D9; size: 30px; text-size: 13; text-color: Black; text-style: bold; }" +
            "edge { fill-color: #888; size: 2px; }"
        );
        

        System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("ER1"),graph,100).getId());
        
        graph.display();
    }

}
