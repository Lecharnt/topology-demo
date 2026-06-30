package com.myproject;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.algorithm.Dijkstra;

public class App2 {
    
    private static Graph addNodes(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index);
        }
        return graph;
    }
    private static void addStyledNodes(Graph graph, String prefix, int count, String hexColor) {
        for (int i = 0; i < count; i++) {
            graph.addNode(prefix + i).setAttribute("ui.style","fill-color: " + hexColor + "; size: 15px;");
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
            for (PolicyType policy : PolicyType.values()) {
                System.out.println(policy);
            }
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
private static Node findClosestMB(PolicyType MBName, Node node, Graph graph, int MaxHops) {
    if (node == null) {
        return null;
    }

    Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);
    dijkstra.init(graph);
    dijkstra.setSource(node);
    dijkstra.compute();

    Node closest = null;
    double closestDistance = Double.POSITIVE_INFINITY;

    for (Node candidate : graph) {
        if (!candidate.getId().startsWith(MBName.name())) {
            continue;
        }

        double distance = dijkstra.getPathLength(candidate);

        if (distance <= MaxHops && distance < closestDistance) {
            closestDistance = distance;
            closest = candidate;
        }
    }

    dijkstra.clear();
    return closest;
}

    private static Node findClosestMBRandom(PolicyType MBName){
        List<Node> list;
        switch (MBName) {
            case FW:  list = FWList;  break;
            case IDS: list = IDSList; break;
            case WP:  list = WPList;  break;
            case TM:  list = TMList;  break;
            default:  return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(randomIndex);
    }

    private static List<Node> ERList = new ArrayList<>();
    private static List<Node> CRList = new ArrayList<>();
    private static List<Node> MList = new ArrayList<>();

    private static List<Node> FWList = new ArrayList<>();
    private static List<Node> IDSList = new ArrayList<>();
    private static List<Node> WPList = new ArrayList<>();
    private static List<Node> TMList = new ArrayList<>();


    public static void main(String[] args) {
        System.out.println("THIS IS APP2 RUNNING");
        System.setProperty("org.graphstream.ui", "swing");
        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("Topology");
        int amount_of_edge_routers = 160;
        int amount_of_core_routers = 16;
        int amount_of_main_core_routers = 4;


        int amount_of_firewalling = 2;
        int amount_of_intrusion_detection = 3;
        int amount_of_web_proxing = 4;
        int amount_of_traffic_measurement = 6;

        int[] values = {2, 2, 6, 6};
        List<Integer> argsList = new ArrayList<Integer>();
        for (int v : values) {
            argsList.add(v);
        }

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

        Random rand = new Random();

        for (Node node : graph) {
            String name = node.getId();
            if (name.startsWith(PolicyType.FW.name())){
                FWList.add(node);
            }
            else if (name.startsWith(PolicyType.IDS.name())){
                IDSList.add(node);
                
            }
            else if (name.startsWith(PolicyType.TM.name())){
                WPList.add(node);

            }
            else if (name.startsWith(PolicyType.WP.name())){
                TMList.add(node);

            }
            else if (name.startsWith(RouterType.ER.name())){
                ERList.add(node);

            }
            else if (name.startsWith(RouterType.CR.name())){
                CRList.add(node);

            }
            else if (name.startsWith(RouterType.M.name())){
                MList.add(node);
            }
            else
                System.err.println("something whent wrong" + node.getId());
        }

        routers = setRouters(graph);


        Collections.shuffle(MList);
        Node temp = null;
        for (Node node : MList) {
            if (temp != null) {
                graph.addEdge(temp.getId() + " " + node.getId(), temp, node);
                
            }
            temp = node;
        }
        Collections.shuffle(CRList);
        int crIndex = 0;
        for (int i = 0; i < MList.size(); i++) {
            Node mNode = MList.get(i);
            int connectionsForThisM = argsList.get(i % argsList.size());

            for (int y = 0; y < connectionsForThisM; y++) {
                Node crNode = CRList.get(crIndex % CRList.size());
                graph.addEdge(mNode.getId() + " " + crNode.getId(), mNode, crNode);
                crIndex++;
            }
        }
        Collections.shuffle(CRList);
        List<Node> middleBoxes = new ArrayList<>();
        middleBoxes.addAll(FWList);
        middleBoxes.addAll(IDSList);
        middleBoxes.addAll(WPList);
        middleBoxes.addAll(TMList);
        middleBoxes.addAll(ERList);
        
        Collections.shuffle(middleBoxes);

        crIndex = 0;
        for (Node node : middleBoxes) {
            Node crNode = CRList.get(crIndex % CRList.size());
            graph.addEdge(node.getId() + " " + crNode.getId(), node, crNode);
            crIndex++;
        }

        // add the labes to the graph nodes

        for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
        }
        graph.setAttribute("ui.stylesheet",
            "node { fill-color: #4A90D9; size: 15px; text-size: 13; text-color: Black; text-style: bold; }" +
            "edge { fill-color: #888; size: 2px; }"
        );
        // System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("FW0"),graph,100).getId());
        // System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("IDS0"),graph,100).getId());

        // System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("TM0"),graph,100).getId());

        // System.out.println("found:"+findClosestMB(PolicyType.IDS,graph.getNode("WP0"),graph,100).getId());
        
Map<String, List<Node>> allLists = new HashMap<>();
        allLists.put("FWList", FWList);
        allLists.put("IDSList", IDSList);
        allLists.put("WPList", WPList);
        allLists.put("TMList", TMList);
        allLists.put("ERList", ERList);
        allLists.put("CRList", CRList);
        allLists.put("MList", MList);

        for (Map.Entry<String, List<Node>> entry : allLists.entrySet()) {
            System.out.print(entry.getKey() + ": [");
            for (Node n : entry.getValue()) {
                System.out.print(n.getId() + " ");
            }
            System.out.println("]");
        }
        for (Node node : graph) {
            String name = node.getId();
            if (name.startsWith(PolicyType.FW.name()) || name.startsWith(PolicyType.IDS.name())
                    || name.startsWith(PolicyType.TM.name()) || name.startsWith(PolicyType.WP.name()))
                continue;

            Node fw  = findClosestMB(PolicyType.FW,  graph.getNode(name), graph, 100);
            Node ids = findClosestMB(PolicyType.IDS, graph.getNode(name), graph, 100);
            Node tm  = findClosestMB(PolicyType.TM,  graph.getNode(name), graph, 100);
            Node wp  = findClosestMB(PolicyType.WP,  graph.getNode(name), graph, 100);

            System.out.println(name + ": "
                + (fw  != null ? fw.getId()  : "none")
                + " " + (ids != null ? ids.getId() : "none")
                + " " + (tm  != null ? tm.getId()  : "none")
                + " " + (wp  != null ? wp.getId()  : "none"));
        }
        for (Node node : graph) {
            System.err.print("Rand name"+": "+ node.getId()+ " " + findClosestMBRandom(PolicyType.FW)+ " "+ findClosestMBRandom(PolicyType.IDS)+ " "+findClosestMBRandom(PolicyType.TM)+ " "+findClosestMBRandom(PolicyType.WP)+ "\n");
        }
        graph.display();
    }

}
