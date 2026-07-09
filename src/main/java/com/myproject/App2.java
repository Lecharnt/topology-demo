package com.myproject;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;
import java.util.Arrays;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.bouncycastle.jce.provider.JDKDSASigner.stdDSA;
import org.graphstream.algorithm.Dijkstra;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static Node findClosestMB(PolicyType mbType, Node source, int maxHops) {
        if (source == null) {
            return null;
        }

        Dijkstra dijkstra = dijkstraCache.get(source);

        Node closestNode = null;
        double shortestDistance = Double.POSITIVE_INFINITY;

        for (Node candidate : getListForType(mbType)) {

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
            } else if (distance == shortestDistance && ThreadLocalRandom.current().nextBoolean()) {
                closestNode = candidate;
            }
        }

        return closestNode;
    }
    private static HashMap<Node, Dijkstra> dijkstraCache = new HashMap<>(); 

    private static HashMap<String, Double> findClosestMBList(
            PolicyType mbType, Node source, int maxHops) {

        if (source == null)
            return null;

        HashMap<String, Double> distances = new HashMap<>();

        Dijkstra dijkstra = dijkstraCache.get(source);

        for (Node candidate : getListForType(mbType)) {
            double distance = dijkstra.getPathLength(candidate);

            if (Double.isInfinite(distance))
                continue;

            if (distance > maxHops)
                continue;

            distances.put(candidate.getId(), distance);
        }

        return distances;
    }
    private static org.graphstream.graph.Path findClosestMBPath(PolicyType mbType, Node source, int maxHops) {

        if (source == null) {
            return null;
        }

        Dijkstra dijkstra = dijkstraCache.get(source);

        Node closestNode = null;
        double shortestDistance = Double.POSITIVE_INFINITY;

        for (Node candidate : getListForType(mbType)) {

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
            } else if (distance == shortestDistance && ThreadLocalRandom.current().nextBoolean()) {
                closestNode = candidate;
            }
        }

        if (closestNode == null)
            return null;

        return dijkstra.getPath(closestNode);
    }
    private static org.graphstream.graph.Path findClosestPathToNode(Node source, Node destination, Graph graph){
        return dijkstraCache.get(source).getPath(destination);
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
            Node nearest = findClosestMB(type, currentNode, maxHops);
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
                HashMap<String, Double> distances = findClosestMBList(type, from, maxHops);
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
    private static Integer getRandomElemantInList(List cool){
        Random rand = new Random();
        return rand.nextInt(cool.size());

    }
    private static Integer getRandomElemant(){
        Random rand = new Random();
        int cool =  rand.nextInt(12);
        if (cool >= 9){
            return 3;
        }
        if (cool >= 6){
            return 2;
        }
        else{
            return 1;
        }

    }
    private static List<Node> ERList = new ArrayList<>();
    private static List<Node> CRList = new ArrayList<>();
    private static List<Node> MList = new ArrayList<>();

    private static List<Node> FWList = new ArrayList<>();
    private static List<Node> IDSList = new ArrayList<>();
    private static List<Node> WPList = new ArrayList<>();
    private static List<Node> TMList = new ArrayList<>();


    public static void main(String[] args) throws IOException {
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
        List<EdgeRouter> FakeEdgeRouters = new ArrayList<EdgeRouter>();
        

        for (Node node : ERList) {
            FakeEdgeRouters.add(new EdgeRouter(node));
        }
        List<Flow> flows = new ArrayList<Flow>();

        routers = setRouters(graph);
        List<String> lines = Files.readAllLines(Paths.get("src/main/java/com/myproject/flowSpread1.txt"));
        int count3333 = 0;
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            
            String ip = parts[0];
            String count = parts[1];
            Integer cool = Integer.parseInt(count);
            int element = getRandomElemantInList(FakeEdgeRouters);
            FakeEdgeRouters.get(element).addFlow(ip, cool);
            FakeEdgeRouters.get(element).addTotFlow(cool);
            int temp = getRandomElemant();
            Flow currentFlow = new Flow(ip, Integer.parseInt(count), FakeEdgeRouters.get(element).getNode());

            String policy = "none";
            List<PolicyType> flowPolicy = new ArrayList<PolicyType>();
            switch (temp) {
                case 1:
                    policy = "POLICY1";
                    flowPolicy.add(PolicyType.FW);
                    flowPolicy.add(PolicyType.IDS);
                    flowPolicy.add(PolicyType.WP);
                    break;
                case 2:
                    policy = "POLICY2";
                    flowPolicy.add(PolicyType.FW);
                    flowPolicy.add(PolicyType.IDS);
                    break;
                case 3:
                    policy = "POLICY3";
                    flowPolicy.add(PolicyType.IDS);
                    flowPolicy.add(PolicyType.TM);
                    break;
                default:
                    System.err.println("there was a out of bounce element");
                    break;
            }
            currentFlow.setFlowPolicy(flowPolicy);
            FakeEdgeRouters.get(element).addPolics(ip,policy);
            flows.add(currentFlow);
            if(count3333 >= 50){
                break;
            }
        }
        int total = 0;
        int totPackest = 0;
        int temp1x = 0;
        int temp2x = 0;
        int temp3x = 0;
        for (EdgeRouter edgeRouter : FakeEdgeRouters) {
            int temp1 = 0;
            int temp2 = 0;
            int temp3 = 0;
            System.out.println("id node element "+ edgeRouter.getNode().getId());
            System.out.print("id of ip sent first element " + edgeRouter.getFlows().keySet().iterator().next());
            System.out.println(" | first element amount of flows  " +  edgeRouter.getFlows().values().iterator().next());
            System.out.println("total pakets in this edge router. "+ edgeRouter.getTotFlow());
            System.out.println("total flows in this edge router. "+ edgeRouter.getFlows().size());


            for (Map.Entry<String, String> entry : edgeRouter.getPolics().entrySet()) {
                switch (entry.getValue()) {
                    case "POLICY1":
                        temp1++;
                        temp1x++;

                        break;
                    case "POLICY2":
                        temp2++;
                        temp2x++;

                        break;
                    case "POLICY3":
                        temp3++; 
                        temp3x++; 

                        break;
                    default:
                        break;
                }
            }
            totPackest += edgeRouter.getTotFlow();
            total += edgeRouter.getFlows().size();

            // System.out.println("total policy 1: " +temp1 + " | total polcy type2: " +temp2+  " | total policy type 3: "+ temp3);
            // System.out.println("tot policy: " + edgeRouter.getPolics().size());

            // System.out.println();
            // System.out.println();
        }
        // System.out.println("total flows in network: "+total);
        // System.out.println("total packets in network: "+totPackest);
        // System.out.println("avrage packets in network: "+totPackest/total);
        // System.out.println("total policy type1 in netwok: " +temp1x + " | total polcy type2: " +temp2x+  " | total policy type 3: "+ temp3x);



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
        for (Node node : graph) {
            Dijkstra d = new Dijkstra(Dijkstra.Element.EDGE, null, null);
            d.init(graph);
            d.setSource(node);
            d.compute();
            dijkstraCache.put(node, d);
        }
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
        // for (Map.Entry<String, List<Node>> entry : allLists.entrySet()) {
        //     System.out.print(entry.getKey() + ": [");
        //     for (Node n : entry.getValue()) {
        //         System.out.print(n.getId() + " ");
        //     }
        //     System.out.println("]");
        // }
        for (Node node : graph) {
            String name = node.getId();
            if (name.startsWith(PolicyType.FW.name()) || name.startsWith(PolicyType.IDS.name())
                    || name.startsWith(PolicyType.TM.name()) || name.startsWith(PolicyType.WP.name()))
                continue;
            Node fw  = findClosestMB(PolicyType.FW,  graph.getNode(name), 100);
            Node ids = findClosestMB(PolicyType.IDS, graph.getNode(name), 100);
            Node tm  = findClosestMB(PolicyType.TM,  graph.getNode(name), 100);
            Node wp  = findClosestMB(PolicyType.WP,  graph.getNode(name), 100);
            routingTable.put(graph.getNode(name), null);

            // System.out.println(name + ": "
            //     + (fw  != null ? fw.getId()  : "none")
            //     + " " + (ids != null ? ids.getId() : "none")
            //     + " " + (tm  != null ? tm.getId()  : "none")
            //     + " " + (wp  != null ? wp.getId()  : "none"));
            HashMap<String, Double> fwL  = findClosestMBList(PolicyType.FW,  graph.getNode(name), 100);
            HashMap<String, Double> idsL = findClosestMBList(PolicyType.IDS, graph.getNode(name), 100);
            HashMap<String, Double> tmL  = findClosestMBList(PolicyType.TM,  graph.getNode(name), 100);
            HashMap<String, Double> wpL  = findClosestMBList(PolicyType.WP,  graph.getNode(name), 100);

            
            // fwL.forEach((key, value) -> {
            //     System.out.print("| "+key + " Hops: " + value);
            // });
            //             System.err.println();
            // idsL.forEach((key, value) -> {
            //     System.out.print("| " + key + " Hops: " + value);
            // });
            //             System.err.println();
            // tmL.forEach((key, value) -> {
            //     System.out.print("| " + key + " Hops: " + value);
            // });
            //             System.err.println();
            // wpL.forEach((key, value) -> {
            //     System.out.print("| " + key + " Hops: " + value);
            // });
            // System.err.println();
            // System.err.println();

            // System.err.println();
            // System.err.println();

        }
        // for (Node node : graph) {
        //     System.err.print("Rand name"+": "+ node.getId()+ " " + findClosestMBRandom(PolicyType.FW)+ " "+ findClosestMBRandom(PolicyType.IDS)+ " "+findClosestMBRandom(PolicyType.TM)+ " "+findClosestMBRandom(PolicyType.WP)+ "\n");
        // }
    // System.out.println("path through middle boxes");

    List<PolicyType> allTypes = new ArrayList<>();
    allTypes.add(PolicyType.FW);
    allTypes.add(PolicyType.IDS);
    allTypes.add(PolicyType.WP);
    allTypes.add(PolicyType.TM);

    Random testRand = new Random();
    List<Simulation> Simulations = new ArrayList<>();
int numSimulations = 5;
int maxFlows = 1000000;

for (int simRun = 0; simRun < numSimulations; simRun++) {

    // fresh packet counters for this run
    HashMap<String, Integer> FWpackest = new HashMap<>();
    for (Node n : FWList) FWpackest.put(n.getId(), 0);

    HashMap<String, Integer> IDSpackest = new HashMap<>();
    for (Node n : IDSList) IDSpackest.put(n.getId(), 0);

    HashMap<String, Integer> TMpackest = new HashMap<>();
    for (Node n : TMList) TMpackest.put(n.getId(), 0);

    HashMap<String, Integer> WPpackest = new HashMap<>();
    for (Node n : WPList) WPpackest.put(n.getId(), 0);

    List<PolicyType> mbOrder = new ArrayList<>(allTypes);
    Collections.shuffle(mbOrder, testRand);

    int processedFlows = 0;

    for (Flow flow : flows) {

        org.graphstream.graph.Path greedyPath =
                findGreedyPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);

        for (Node node : greedyPath.getNodePath()) {
            String nodeId = node.getId();

            if (nodeId.startsWith(PolicyType.FW.name())) {
                FWpackest.replace(nodeId, FWpackest.get(nodeId) + flow.getPakets());
            } else if (nodeId.startsWith(PolicyType.IDS.name())) {
                IDSpackest.replace(nodeId, IDSpackest.get(nodeId) + flow.getPakets());
            } else if (nodeId.startsWith(PolicyType.TM.name())) {
                TMpackest.replace(nodeId, TMpackest.get(nodeId) + flow.getPakets());
            } else if (nodeId.startsWith(PolicyType.WP.name())) {
                WPpackest.replace(nodeId, WPpackest.get(nodeId) + flow.getPakets());
            }
        }

        processedFlows++;
        if (processedFlows >= maxFlows) {
            break;
        }
    }

    // store this run's results
    Simulation simResult = new Simulation(FWpackest, IDSpackest, TMpackest, WPpackest);
    Simulations.add(simResult);

    System.out.println("Simulation run " + (simRun + 1) + " of " + numSimulations);

    FWpackest.forEach((key, value) -> System.out.print(" | " + key + " packets enter: " + value));
    System.out.println();

    IDSpackest.forEach((key, value) -> System.out.print(" | " + key + " packets enter: " + value));
    System.out.println();

    TMpackest.forEach((key, value) -> System.out.print(" | " + key + " packets enter: " + value));
    System.out.println();

    WPpackest.forEach((key, value) -> System.out.print(" | " + key + " packets enter: " + value));
    System.out.println();
    System.out.println();
    int fwMin = Collections.min(FWpackest.values());
    int fwMax = Collections.max(FWpackest.values());
    int idsMin = Collections.min(IDSpackest.values());
    int idsMax = Collections.max(IDSpackest.values());
    int tmMin = Collections.min(TMpackest.values());
    int tmMax = Collections.max(TMpackest.values());
    int wpMin = Collections.min(WPpackest.values());
    int wpMax = Collections.max(WPpackest.values());

    System.out.println("FW  Min: " + fwMin + " Max: " + fwMax);
    System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
    System.out.println("TM  Min: " + tmMin + " Max: " + tmMax);
    System.out.println("WP  Min: " + wpMin + " Max: " + wpMax);

    System.out.println("Overall Min: " + Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
    System.out.println("Overall Max: " + Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));
}
        graph.display().enableAutoLayout();
    }

}
