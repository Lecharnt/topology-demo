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
private static Node findClosestMB(PolicyType mbType, Node source, Graph graph, int maxHops) {
    if (source == null || graph == null) {
        return null;
    }

    Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);

    try {
        dijkstra.init(graph);
        dijkstra.setSource(source);
        dijkstra.compute();

        Node closestNode = null;
        double shortestDistance = 10000;

        for (Node candidate : graph) {

            if (!candidate.getId().startsWith(mbType.name()))
                continue;

            if (candidate.equals(source))
                continue;

            double distance = dijkstra.getPathLength(candidate);

            if (Double.isInfinite(distance))
                continue;

            if (distance > maxHops)
                continue;

            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestNode = candidate;
            }
        }

        return closestNode;
    }
    finally {
        dijkstra.clear();
    }
}
private static HashMap<String, Double> findClosestMBList(PolicyType mbType, Node source, Graph graph, int maxHops) {
    if (source == null || graph == null) {
        return null;
    }
    HashMap<String, Double> testin = new HashMap<String, Double>();
    Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);

    try {
        dijkstra.init(graph);
        dijkstra.setSource(source);
        dijkstra.compute();

        Node closestNode = null;
        double shortestDistance = 10000;

        for (Node candidate : graph) {

            if (!candidate.getId().startsWith(mbType.name()))
                continue;

            if (candidate.equals(source))
                continue;

            double distance = dijkstra.getPathLength(candidate);

            
            if (Double.isInfinite(distance))
                continue;

            testin.put(candidate.getId(), distance);

            if (distance > maxHops)
                continue;

            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestNode = candidate;
            }
        }

        return testin;
    }
    finally {
        dijkstra.clear();
    }
}
private static org.graphstream.graph.Path findClosestMBPath(PolicyType mbType, Node source, Graph graph, int maxHops) {
    if (source == null || graph == null) {
        return null;
    }
    Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);

    try {
        dijkstra.init(graph);
        dijkstra.setSource(source);
        dijkstra.compute();

        Node closestNode = null;
        double shortestDistance = 10000;

        for (Node candidate : graph) {

            if (!candidate.getId().startsWith(mbType.name()))
                continue;

            if (candidate.equals(source))
                continue;

            double distance = dijkstra.getPathLength(candidate);

            
            if (Double.isInfinite(distance))
                continue;

            if (distance > maxHops)
                continue;

            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestNode = candidate;
            }
        }

        return dijkstra.getPath(closestNode);
    }
    finally {
        dijkstra.clear();
    }
}
    private static org.graphstream.graph.Path findClosestPathToNode(Node source, Node destination, Graph graph){
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, null);

        dijkstra.init(graph);
        dijkstra.setSource(source);
        dijkstra.compute();

        return dijkstra.getPath(destination);
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
    private static List<Node> getListForType(PolicyType type) {
        switch (type) {
            case FW:  return FWList;
            case IDS: return IDSList;
            case WP:  return WPList;
            case TM:  return TMList;
            default:  return Collections.emptyList();
        }
    }
    private static org.graphstream.graph.Path findGreedyPathThroughMBs(Node startNode, List<PolicyType> middleBoxTypes, Graph graph, int maxHops) {
        List<org.graphstream.graph.Path> segments = new ArrayList<>();
        Node currentNode = startNode;

        for (PolicyType type : middleBoxTypes) {
            Node nearest = findClosestMB(type, currentNode, graph, maxHops);
            if (nearest == null) {
                System.err.println("No reachable " + type + " from " + currentNode.getId());
                return null;
            }
            org.graphstream.graph.Path segment = findClosestPathToNode(currentNode, nearest, graph);
            segments.add(segment);
            currentNode = nearest;
        }

        return mergeSegments(segments);
    }
    private static org.graphstream.graph.Path findOptimalPathThroughMBs(Node startNode, List<PolicyType> middleBoxTypes, Graph graph, int maxHops) {
        Map<Node, Double> bestDistance = new HashMap<>();
        Map<Node, Node> predecessor = new HashMap<>();
        bestDistance.put(startNode, 0.0);
        List<Node> currentLayer = new ArrayList<>();
        currentLayer.add(startNode);

        for (PolicyType type : middleBoxTypes) {
            List<Node> candidates = getListForType(type);
            Map<Node, Double> nextBest = new HashMap<>();

            for (Node from : currentLayer) {
                double baseDist = bestDistance.get(from);
                HashMap<String, Double> distances = findClosestMBList(type, from, graph, maxHops);
                if (distances == null) continue;

                for (Node candidate : candidates) {
                    Double d = distances.get(candidate.getId());
                    if (d == null) continue; // unreachable within maxHops

                    double total = baseDist + d;
                    if (!nextBest.containsKey(candidate) || total < nextBest.get(candidate)) {
                        nextBest.put(candidate, total);
                        predecessor.put(candidate, from);
                    }
                }
            }

            if (nextBest.isEmpty()) {
                System.err.println("No reachable " + type + " from previous layer");
                return null;
            }

            bestDistance = nextBest;
            currentLayer = new ArrayList<>(nextBest.keySet());
        }

        // pick the best node in the final layer
        Node bestFinal = null;
        double bestTotal = Double.MAX_VALUE;
        for (Node n : currentLayer) {
            if (bestDistance.get(n) < bestTotal) {
                bestTotal = bestDistance.get(n);
                bestFinal = n;
            }
        }

        // walk predecessors back to build the winning sequence of nodes
        List<Node> sequence = new ArrayList<>();
        Node cur = bestFinal;
        sequence.add(0, cur);
        while (predecessor.containsKey(cur)) {
            cur = predecessor.get(cur);
            sequence.add(0, cur);
        }

        // stitch the Dijkstra segments between each consecutive pair into one path
        List<org.graphstream.graph.Path> segments = new ArrayList<>();
        for (int i = 0; i < sequence.size() - 1; i++) {
            segments.add(findClosestPathToNode(sequence.get(i), sequence.get(i + 1), graph));
        }

        return mergeSegments(segments);
    }
    private static org.graphstream.graph.Path mergeSegments(List<org.graphstream.graph.Path> segments) {
        org.graphstream.graph.Path merged = new org.graphstream.graph.Path();
        for (org.graphstream.graph.Path segment : segments) {
            List<Node> nodes = segment.getNodePath();
            List<org.graphstream.graph.Edge> edges = segment.getEdgePath();
            for (int i = 0; i < edges.size(); i++) {
                merged.add(nodes.get(i), edges.get(i));
            }
        }
        return merged;
    }
    private static Graph createEdge(Node node1, Node node2, Graph graph){
        if (graph.getEdge(getEdgeName(node1, node2)) == null){
            graph.addEdge(getEdgeName(node1, node2), node1, node2);
        }
        return graph;
    }
    private static String getEdgeName(Node node1, Node node2){
        String name1 = node1.getId();
        String name2 = node2.getId();

            if (name1.startsWith(name2.replaceAll("\\d", "")) && name2.startsWith(name1.replaceAll("\\d", ""))){
                if (Integer.parseInt(name1.replaceAll("\\D", ""))>= Integer.parseInt(name2.replaceAll("\\D", "")));{
                    return name2 + name1;
                }
            }
            else if (name1.startsWith(PolicyType.FW.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.IDS.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(PolicyType.TM.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.WP.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(RouterType.ER.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(RouterType.CR.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(RouterType.M.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.FW.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(PolicyType.IDS.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.TM.name())){
                return name2 + name1;

            }


            else if (name1.startsWith(PolicyType.WP.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(RouterType.ER.name())){
                return name2 + name1;

            }


            else if (name1.startsWith(RouterType.CR.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(RouterType.M.name())){
                return name2 + name1;
            }
            else
                System.err.println("created bad edge");
                return name1 + name2;
    }
    private static org.graphstream.graph.Path findRandomPathThroughMBs(Node startNode, List<PolicyType> middleBoxTypes, Graph graph) {

        List<org.graphstream.graph.Path> segments = new ArrayList<>();
        Node currentNode = startNode;

        for (PolicyType type : middleBoxTypes) {
            Node randomMB = findClosestMBRandom(type);
            if (randomMB == null) {
                System.err.println("No " + type + " available");
                return null;
            }
            org.graphstream.graph.Path segment = findClosestPathToNode(currentNode, randomMB, graph);
            segments.add(segment);
            currentNode = randomMB;
        }

        return mergeSegments(segments);
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
        int amount_of_edge_routers = 50;
        int amount_of_core_routers = 16;
        int amount_of_main_core_routers = 4;


        int amount_of_firewalling = 8;
        int amount_of_intrusion_detection = 8;
        int amount_of_web_proxing = 4;
        int amount_of_traffic_measurement = 4;

        int[] values = {2, 2, 6, 6};
        List<Integer> argsList = new ArrayList<Integer>();
        for (int v : values) {
            argsList.add(v);
        }

        HashMap<String, RoutingTable> routers = new HashMap<String, RoutingTable>();
        

        String red     = "#e74d3c74";
        String blue    = "#8e8a06";
        String green   = "#2ecc3175";
        String orange  = "#037d73";
        String purple  = "#1100ff6b";
        String cyan    = "#ff00fb74";
        String yellow  = "#73090969";



        // add nodes core router cr1 er1
        addStyledNodes(graph, "ER", amount_of_edge_routers,blue);
        addStyledNodes(graph, "CR", amount_of_core_routers,green);
        addStyledNodes(graph, "M", amount_of_main_core_routers,red);

        addStyledNodes(graph, "FW", amount_of_firewalling, orange);
        addStyledNodes(graph, "IDS", amount_of_intrusion_detection, purple);
        addStyledNodes(graph, "WP", amount_of_web_proxing, cyan);
        addStyledNodes(graph, "TM", amount_of_traffic_measurement, yellow);

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
                TMList.add(node);

            }
            else if (name.startsWith(PolicyType.WP.name())){
                WPList.add(node);

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
                graph = createEdge(temp, node, graph);
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
                graph = createEdge(mNode, crNode, graph);
                crIndex++;
            }
        }

        Collections.shuffle(ERList);

        for (Node er : ERList) {

            Node cr1 = CRList.get(rand.nextInt(CRList.size()));
            graph = createEdge(er, cr1, graph);

            if (rand.nextBoolean()) {

                Node cr2;

                do {
                    cr2 = CRList.get(rand.nextInt(CRList.size()));
                } while (cr2 == cr1);

                graph = createEdge(er, cr2, graph);
            }
        }
        Collections.shuffle(CRList);
        List<Node> middleBoxes = new ArrayList<>();
        middleBoxes.clear();
        middleBoxes.addAll(FWList);
        middleBoxes.addAll(IDSList);
        middleBoxes.addAll(WPList);
        middleBoxes.addAll(TMList);

        Collections.shuffle(middleBoxes);

        crIndex = 0;
        for (Node mb : middleBoxes) {
            Node cr = CRList.get(crIndex % CRList.size());
            graph = createEdge(mb, cr, graph);
            crIndex++;
        }
        // add the labes to the graph nodes

        for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
        }
        graph.setAttribute("ui.stylesheet",
            "node { fill-color: #4A90D9; size: 15px; text-size: 13; text-color: Black; text-style: bold; }" +
            "edge { fill-color: #000000; size: 2px; }"
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
        Map<Node, List<String>> routingTable = new HashMap<>();
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
            routingTable.put(graph.getNode(name), null);

            System.out.println(name + ": "
                + (fw  != null ? fw.getId()  : "none")
                + " " + (ids != null ? ids.getId() : "none")
                + " " + (tm  != null ? tm.getId()  : "none")
                + " " + (wp  != null ? wp.getId()  : "none"));
            HashMap<String, Double> fwL  = findClosestMBList(PolicyType.FW,  graph.getNode(name), graph, 100);
            HashMap<String, Double> idsL = findClosestMBList(PolicyType.IDS, graph.getNode(name), graph, 100);
            HashMap<String, Double> tmL  = findClosestMBList(PolicyType.TM,  graph.getNode(name), graph, 100);
            HashMap<String, Double> wpL  = findClosestMBList(PolicyType.WP,  graph.getNode(name), graph, 100);

            
            fwL.forEach((key, value) -> {
                System.out.print("| "+key + " Hops: " + value);
            });
                        System.err.println();
            idsL.forEach((key, value) -> {
                System.out.print("| " + key + " Hops: " + value);
            });
                        System.err.println();
            tmL.forEach((key, value) -> {
                System.out.print("| " + key + " Hops: " + value);
            });
                        System.err.println();
            wpL.forEach((key, value) -> {
                System.out.print("| " + key + " Hops: " + value);
            });
            System.err.println();
            System.err.println();

            System.err.println();
            System.err.println();

        }
        // for (Node node : graph) {
        //     System.err.print("Rand name"+": "+ node.getId()+ " " + findClosestMBRandom(PolicyType.FW)+ " "+ findClosestMBRandom(PolicyType.IDS)+ " "+findClosestMBRandom(PolicyType.TM)+ " "+findClosestMBRandom(PolicyType.WP)+ "\n");
        // }
    System.out.println("path through middle boxes");

    List<PolicyType> allTypes = new ArrayList<>();
    allTypes.add(PolicyType.FW);
    allTypes.add(PolicyType.IDS);
    allTypes.add(PolicyType.WP);
    allTypes.add(PolicyType.TM);

    Random testRand = new Random();

    for (int i = 0; i < 10; i++) {
        List<PolicyType> mbOrder = new ArrayList<>(allTypes);
        Collections.shuffle(mbOrder, testRand);

        Node startNode = ERList.get(testRand.nextInt(ERList.size()));

        org.graphstream.graph.Path greedyPath = findGreedyPathThroughMBs(startNode, mbOrder, graph, 1000);
        org.graphstream.graph.Path optimalPath = findOptimalPathThroughMBs(startNode, mbOrder, graph, 1000);
        org.graphstream.graph.Path randomPath = findRandomPathThroughMBs(startNode, mbOrder, graph);

        System.out.println(startNode.getId() + " " + mbOrder  + " optimal: " + optimalPath.getEdgeCount()+ " greedy: " + greedyPath.getEdgeCount() + " random: " + randomPath.getEdgeCount());
        System.out.println("optimal: " + optimalPath.getNodePath());
        System.out.println("greedy: " + greedyPath.getNodePath());
        System.out.println("random: "+  randomPath.getNodePath());
        System.out.println();
    }
        graph.display().enableAutoLayout();
    }

}
