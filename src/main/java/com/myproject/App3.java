package com.myproject;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.algorithm.Dijkstra;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App3 {

    // shared state used across the section methods below
    private static Graph graph;
    private static Random rand;
    private static List<Integer> argsList = new ArrayList<Integer>();
    private static List<EdgeRouter> FakeEdgeRouters = new ArrayList<EdgeRouter>();
    private static List<Flow> flows = new ArrayList<Flow>();
    private static HashMap<String, Integer> FWpackest = new HashMap<>();
    private static HashMap<String, Integer> IDSpackest = new HashMap<>();
    private static HashMap<String, Integer> TMpackest = new HashMap<>();
    private static HashMap<String, Integer> WPpackest = new HashMap<>();

    private static HashMap<String, Integer> FWpackestGreed = new HashMap<>();
    private static HashMap<String, Integer> IDSpackestGreed = new HashMap<>();
    private static HashMap<String, Integer> TMpackestGreed = new HashMap<>();
    private static HashMap<String, Integer> WPpackestGreed = new HashMap<>();

    private static HashMap<String, Integer> FWpackestRand = new HashMap<>();
    private static HashMap<String, Integer> IDSpackestRand = new HashMap<>();
    private static HashMap<String, Integer> TMpackestRand = new HashMap<>();
    private static HashMap<String, Integer> WPpackestRand = new HashMap<>();

    private static HashMap<String, Integer> FWpackestOp = new HashMap<>();
    private static HashMap<String, Integer> IDSpackestOp = new HashMap<>();
    private static HashMap<String, Integer> TMpackestOp = new HashMap<>();
    private static HashMap<String, Integer> WPpackestOp = new HashMap<>();

    public static void main(String[] args) throws IOException {
        setupConfig();
        buildGraph();
        sortNodesIntoLists();
        buildFlowsAndEdgeRouters();
        reportEdgeRouterStats();
        wireTopologyEdges();
        styleGraphAndBuildDijkstraCache();
        computeRoutingTables();
        simulateFlowsAndTallyPackets();
        printResultsAndDisplay();
    }

    // Config
    private static void setupConfig() {
        System.setProperty("org.graphstream.ui", "swing");
        System.setProperty("org.graphstream.ui", "swing");

        int[] values = {2, 2, 6, 6};
        for (int v : values) {
            argsList.add(v);
        }

        rand = new Random();
    }

    // Build the graph and create all node types
    private static void buildGraph() {
        int amount_of_edge_routers = 50;
        int amount_of_core_routers = 16;
        int amount_of_main_core_routers = 4;

        int amount_of_firewalling = 8;
        int amount_of_intrusion_detection = 8;
        int amount_of_web_proxing = 4;
        int amount_of_traffic_measurement = 4;

        String red     = "#e74d3c74";
        String blue    = "#8e8a06";
        String green   = "#2ecc3175";
        String orange  = "#037d73";
        String purple  = "#1100ff6b";
        String cyan    = "#ff00fb74";
        String yellow  = "#73090969";

        graph = new SingleGraph("Topology");

        // add nodes core router cr1 er1
        GraphBuilder.addStyledNodes(graph, "ER", amount_of_edge_routers, blue);
        GraphBuilder.addStyledNodes(graph, "CR", amount_of_core_routers, green);
        GraphBuilder.addStyledNodes(graph, "M", amount_of_main_core_routers, red);

        GraphBuilder.addStyledNodes(graph, "FW", amount_of_firewalling, orange);
        GraphBuilder.addStyledNodes(graph, "IDS", amount_of_intrusion_detection, purple);
        GraphBuilder.addStyledNodes(graph, "WP", amount_of_web_proxing, cyan);
        GraphBuilder.addStyledNodes(graph, "TM", amount_of_traffic_measurement, yellow);
    }

    // Sort nodes into their type lists
    private static void sortNodesIntoLists() {
        for (Node node : graph) {
            String name = node.getId();
            if (name.startsWith(PolicyType.FW.name())){
                PathFinder.FWList.add(node);
            }
            else if (name.startsWith(PolicyType.IDS.name())){
                PathFinder.IDSList.add(node);

            }
            else if (name.startsWith(PolicyType.TM.name())){
                PathFinder.TMList.add(node);

            }
            else if (name.startsWith(PolicyType.WP.name())){
                PathFinder.WPList.add(node);

            }
            else if (name.startsWith(RouterType.ER.name())){
                PathFinder.ERList.add(node);

            }
            else if (name.startsWith(RouterType.CR.name())){
                PathFinder.CRList.add(node);

            }
            else if (name.startsWith(RouterType.M.name())){
                PathFinder.MList.add(node);
            }
            else
                System.err.println("something whent wrong" + node.getId());
        }

        int FWListRandAmount = 4;
        List<Node> tempFW = new ArrayList<>(PathFinder.FWList);
        Collections.shuffle(tempFW);
        PathFinder.FWListR = tempFW.subList(0, FWListRandAmount);

        int IDSListRandAmount = 4;
        List<Node> tempIDS = new ArrayList<>(PathFinder.IDSList);
        Collections.shuffle(tempIDS);
        PathFinder.IDSListR = tempIDS.subList(0, IDSListRandAmount);

        int WPListRandAmount = 2;
        List<Node> tempWP = new ArrayList<>(PathFinder.WPList);
        Collections.shuffle(tempWP);
        PathFinder.WPListR = tempWP.subList(0, WPListRandAmount);

        int TMListRandAmount = 2;
        List<Node> tempTM = new ArrayList<>(PathFinder.TMList);
        Collections.shuffle(tempTM);
        PathFinder.TMListR = tempTM.subList(0, TMListRandAmount);
    }

    // Build fake edge routers, load flows from file, assign policies
    private static void buildFlowsAndEdgeRouters() throws IOException {
        for (Node node : PathFinder.ERList) {
            FakeEdgeRouters.add(new EdgeRouter(node));
        }

        HashMap<String, RoutingTable> routers = RouterUtils.setRouters(graph);

        List<String> lines = Files.readAllLines(Paths.get("src/main/java/com/myproject/flowSpread1.txt"));
        int count3333 = 0;
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");

            String ip = parts[0];
            String count = parts[1];
            Integer cool = Integer.parseInt(count);
            int element = RandomUtils.getRandomElemantInList(FakeEdgeRouters);
            FakeEdgeRouters.get(element).addFlow(ip, cool);
            FakeEdgeRouters.get(element).addTotFlow(cool);
            int temp = RandomUtils.getRandomElemant();
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
            FakeEdgeRouters.get(element).addPolics(ip, policy);
            flows.add(currentFlow);
            if(count3333 >= 50){
                break;
            }
        }
    }

    // policy
    private static void reportEdgeRouterStats() {
        int total = 0;
        int totPackest = 0;
        int temp1x = 0;
        int temp2x = 0;
        int temp3x = 0;
        for (EdgeRouter edgeRouter : FakeEdgeRouters) {
            int temp1 = 0;
            int temp2 = 0;
            int temp3 = 0;
            System.out.print("id: "+ edgeRouter.getNode().getId());
            System.out.print(" | id first element: " + edgeRouter.getFlows().keySet().iterator().next());
            System.out.print(" | first element flows: " +  edgeRouter.getFlows().values().iterator().next());
            System.out.print(" | total pakets: "+ edgeRouter.getTotFlow());
            System.out.println(" | total flows: "+ edgeRouter.getFlows().size());

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
    }

    // Wire up the topology edges
    private static void wireTopologyEdges() {
        List<Node> mList = new ArrayList<>(PathFinder.MList);
        Collections.shuffle(mList);
        Node temp = null;
        for (Node node : mList) {
            if (temp != null) {
                graph = GraphBuilder.createEdge(temp, node, graph);
            }
            temp = node;
        }

        List<Node> crList = new ArrayList<>(PathFinder.CRList);
        Collections.shuffle(crList);
        int crIndex = 0;
        for (int i = 0; i < mList.size(); i++) {
            Node mNode = mList.get(i);
            int connectionsForThisM = argsList.get(i % argsList.size());

            for (int y = 0; y < connectionsForThisM; y++) {
                Node crNode = crList.get(crIndex % crList.size());
                graph = GraphBuilder.createEdge(mNode, crNode, graph);
                crIndex++;
            }
        }

        List<Node> erList = new ArrayList<>(PathFinder.ERList);
        Collections.shuffle(erList);
        for (Node er : erList) {
            Node cr = crList.get(rand.nextInt(crList.size()));
            graph = GraphBuilder.createEdge(er, cr, graph);
        }
        double connectionProb = 0.5;
        for (Node cr : crList) {

            if (rand.nextDouble() < connectionProb) {

                Node other;

                do {
                    other = crList.get(rand.nextInt(crList.size()));
                } while (other == cr || cr.hasEdgeBetween(other));

                graph = GraphBuilder.createEdge(cr, other, graph);
            }
        }

        List<Node> crListForMB = new ArrayList<>(PathFinder.CRList);
        Collections.shuffle(crListForMB);
        List<Node> middleBoxes = new ArrayList<>();
        middleBoxes.clear();
        middleBoxes.addAll(PathFinder.FWList);
        middleBoxes.addAll(PathFinder.IDSList);
        middleBoxes.addAll(PathFinder.WPList);
        middleBoxes.addAll(PathFinder.TMList);

        Collections.shuffle(middleBoxes);

        crIndex = 0;
        for (Node mb : middleBoxes) {
            Node cr = crListForMB.get(crIndex % crListForMB.size());
            graph = GraphBuilder.createEdge(mb, cr, graph);
            crIndex++;
        }
    }

    // label the graph and build the Dijkstra cache
    private static void styleGraphAndBuildDijkstraCache() {
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
            PathFinder.dijkstraCache.put(node, d);
        }
        // System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("FW0"),graph,100).getId());
        // System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("IDS0"),graph,100).getId());

        // System.out.println("found:"+findClosestMB(PolicyType.WP,graph.getNode("TM0"),graph,100).getId());

        // System.out.println("found:"+findClosestMB(PolicyType.IDS,graph.getNode("WP0"),graph,100).getId());
    }

    // compute per-node routing and closest-middlebox tables
    private static void computeRoutingTables() {
        Map<String, List<Node>> allLists = new HashMap<>();
        allLists.put("FWList", PathFinder.FWList);
        allLists.put("IDSList", PathFinder.IDSList);
        allLists.put("WPList", PathFinder.WPList);
        allLists.put("TMList", PathFinder.TMList);
        allLists.put("ERList", PathFinder.ERList);
        allLists.put("CRList", PathFinder.CRList);
        allLists.put("MList", PathFinder.MList);
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
            Node fw  = PathFinder.findClosestMB(PolicyType.FW,  graph.getNode(name), 100);
            Node ids = PathFinder.findClosestMB(PolicyType.IDS, graph.getNode(name), 100);
            Node tm  = PathFinder.findClosestMB(PolicyType.TM,  graph.getNode(name), 100);
            Node wp  = PathFinder.findClosestMB(PolicyType.WP,  graph.getNode(name), 100);
            routingTable.put(graph.getNode(name), null);

            // System.out.println(name + ": "
            //     + (fw  != null ? fw.getId()  : "none")
            //     + " " + (ids != null ? ids.getId() : "none")
            //     + " " + (tm  != null ? tm.getId()  : "none")
            //     + " " + (wp  != null ? wp.getId()  : "none"));
            HashMap<String, Double> fwL  = PathFinder.findClosestMBList(PolicyType.FW,  graph.getNode(name), 100);
            HashMap<String, Double> idsL = PathFinder.findClosestMBList(PolicyType.IDS, graph.getNode(name), 100);
            HashMap<String, Double> tmL  = PathFinder.findClosestMBList(PolicyType.TM,  graph.getNode(name), 100);
            HashMap<String, Double> wpL  = PathFinder.findClosestMBList(PolicyType.WP,  graph.getNode(name), 100);

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
    }

    // Simulate flows through their middlebox chain
    private static void simulateFlowsAndTallyPackets() {
        List<PolicyType> allTypes = new ArrayList<>();
        allTypes.add(PolicyType.FW);
        allTypes.add(PolicyType.IDS);
        allTypes.add(PolicyType.WP);
        allTypes.add(PolicyType.TM);

        Random testRand = new Random();
        for (Node iterable_element : PathFinder.FWList) {
            FWpackest.put(iterable_element.getId(), 0);
        }

        for (Node iterable_element : PathFinder.IDSList) {
            IDSpackest.put(iterable_element.getId(), 0);

        }
        for (Node iterable_element : PathFinder.TMList) {
            TMpackest.put(iterable_element.getId(), 0);

        }
        for (Node iterable_element : PathFinder.WPList) {
            WPpackest.put(iterable_element.getId(), 0);

        }

        // for (int i = 0; i < 10; i++) {
        List<PolicyType> mbOrder = new ArrayList<>(allTypes);
        Collections.shuffle(mbOrder, testRand);

        Node startNode = PathFinder.ERList.get(testRand.nextInt(PathFinder.ERList.size()));

        // org.graphstream.graph.Path greedyPath = findGreedyPathThroughMBs(startNode, mbOrder, graph, 1000);
        // org.graphstream.graph.Path optimalPath = findOptimalPathThroughMBs(startNode, mbOrder, graph, 1000);
        // org.graphstream.graph.Path randomPath = findRandomPathThroughMBs(startNode, mbOrder, graph);

        // System.out.println(startNode.getId() + " " + mbOrder  + " optimal: " + optimalPath.getEdgeCount()+ " greedy: " + greedyPath.getEdgeCount() + " random: " + randomPath.getEdgeCount());
        // System.out.println("optimal: " + optimalPath.getNodePath());
        // System.out.println("greedy: " + greedyPath.getNodePath());
        // System.out.println("random: "+  randomPath.getNodePath());
        // System.out.println();

        int maxFlows = 1000000;
        int processedFlows = 0;
        FWpackestGreed = new HashMap<>(FWpackest);
        IDSpackestGreed = new HashMap<>(IDSpackest);
        TMpackestGreed = new HashMap<>(TMpackest);
        WPpackestGreed = new HashMap<>(WPpackest);

        FWpackestRand = new HashMap<>(FWpackest);
        IDSpackestRand = new HashMap<>(IDSpackest);
        TMpackestRand = new HashMap<>(TMpackest);
        WPpackestRand = new HashMap<>(WPpackest);

        FWpackestOp = new HashMap<>(FWpackest);
        IDSpackestOp = new HashMap<>(IDSpackest);
        TMpackestOp = new HashMap<>(TMpackest);
        WPpackestOp = new HashMap<>(WPpackest);
        for (Flow flow : flows) {

            org.graphstream.graph.Path greedyPath =
                    PathFinder.findGreedyPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);

            org.graphstream.graph.Path randomPath =
                    PathFinder.findRandomPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph);

            // org.graphstream.graph.Path optimalPath =
            //         PathFinder.findOptimalPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);

            // System.out.println("=================================================");
            // System.out.println("Flow: " + flow.getId());
            // System.out.println("Start Node: " + flow.getNode());
            // System.out.println("Packets: " + flow.getPakets());

            // System.out.print("Policies:");
            // for (PolicyType policy : flow.getFlowPolicy()) {
            //     System.out.print(" | " + policy.name());
            // }
            // System.out.println("\n");

            // System.out.println("Greedy Path (" + greedyPath.getEdgeCount() + " hops)");
            // System.out.println(greedyPath.getNodePath());
            // System.out.println();

            // System.out.println("Random Path (" + randomPath.getEdgeCount() + " hops)");
            // System.out.println(randomPath.getNodePath());
            // System.out.println();

            // System.out.println("Optimal Path (" + optimalPath.getEdgeCount() + " hops)");
            // System.out.println(optimalPath.getNodePath());
            // System.out.println();

            // Count packets for the greedy path




            for (Node node : greedyPath.getNodePath()) {
                String nodeId = node.getId();

                if (nodeId.startsWith(PolicyType.FW.name())) {
                    FWpackestGreed.replace(nodeId, FWpackestGreed.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.IDS.name())) {
                    IDSpackestGreed.replace(nodeId, IDSpackestGreed.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.TM.name())) {
                    TMpackestGreed.replace(nodeId, TMpackestGreed.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.WP.name())) {
                    WPpackestGreed.replace(nodeId, WPpackestGreed.get(nodeId) + flow.getPakets());
                }
            }
            // Count packets for the rand path

            for (Node node : randomPath.getNodePath()) {
                String nodeId = node.getId();

                if (nodeId.startsWith(PolicyType.FW.name())) {
                    FWpackestRand.replace(nodeId, FWpackestRand.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.IDS.name())) {
                    IDSpackestRand.replace(nodeId, IDSpackestRand.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.TM.name())) {
                    TMpackestRand.replace(nodeId, TMpackestRand.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.WP.name())) {
                    WPpackestRand.replace(nodeId, WPpackestRand.get(nodeId) + flow.getPakets());
                }
            }
            // Count packets for the optimal path

            // for (Node node : optimalPath.getNodePath()) {
            //     String nodeId = node.getId();

            //     if (nodeId.startsWith(PolicyType.FW.name())) {
            //         FWpackestOp.replace(nodeId, FWpackestOp.get(nodeId) + flow.getPakets());
            //     } else if (nodeId.startsWith(PolicyType.IDS.name())) {
            //         IDSpackestOp.replace(nodeId, IDSpackestOp.get(nodeId) + flow.getPakets());
            //     } else if (nodeId.startsWith(PolicyType.TM.name())) {
            //         TMpackestOp.replace(nodeId, TMpackestOp.get(nodeId) + flow.getPakets());
            //     } else if (nodeId.startsWith(PolicyType.WP.name())) {
            //         WPpackestOp.replace(nodeId, WPpackestOp.get(nodeId) + flow.getPakets());
            //     }
            // }
            processedFlows++;
            if (processedFlows >= maxFlows) {
                break;
            }

        }
    }

    // Print results and display the graph
    private static void printResultsAndDisplay() {
        graph.display().enableAutoLayout();
        System.out.println();

        System.err.println("Greed middle boxes");

        printMap(FWpackestGreed);
        printMap(IDSpackestGreed);
        printMap(TMpackestGreed);
        printMap(WPpackestGreed);

        System.err.println();
        System.err.println("Rand middle boxes");

        printMap(FWpackestRand);
        printMap(IDSpackestRand);
        printMap(TMpackestRand);
        printMap(WPpackestRand);

    int fwMin = Collections.min(FWpackestGreed.values());
    int fwMax = Collections.max(FWpackestGreed.values());
    int idsMin = Collections.min(IDSpackestGreed.values());
    int idsMax = Collections.max(IDSpackestGreed.values());
    int tmMin = Collections.min(TMpackestGreed.values());
    int tmMax = Collections.max(TMpackestGreed.values());
    int wpMin = Collections.min(WPpackestGreed.values());
    int wpMax = Collections.max(WPpackestGreed.values());
    System.out.println();

    System.out.println("Greedy");
    System.out.println("FW  Min: " + fwMin + " Max: " + fwMax);
    System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
    System.out.println("TM  Min: " + tmMin + " Max: " + tmMax);
    System.out.println("WP  Min: " + wpMin + " Max: " + wpMax);

    System.out.println("Overall Min: " +
        Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
    System.out.println("Overall Max: " +
        Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));


    fwMin = Collections.min(FWpackestRand.values());
    fwMax = Collections.max(FWpackestRand.values());
    idsMin = Collections.min(IDSpackestRand.values());
    idsMax = Collections.max(IDSpackestRand.values());
    tmMin = Collections.min(TMpackestRand.values());
    tmMax = Collections.max(TMpackestRand.values());
    wpMin = Collections.min(WPpackestRand.values());
    wpMax = Collections.max(WPpackestRand.values());

    System.out.println("\nRandom");
    System.out.println("FW  Min: " + fwMin + " Max: " + fwMax);
    System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
    System.out.println("TM  Min: " + tmMin + " Max: " + tmMax);
    System.out.println("WP  Min: " + wpMin + " Max: " + wpMax);

    System.out.println("Overall Min: " +
        Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
    System.out.println("Overall Max: " +
        Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));
}
    private static void printMap(HashMap<String, Integer> map) {
        map.entrySet().stream()
            .sorted(Comparator.comparingInt(e ->
                Integer.parseInt(e.getKey().replaceAll("\\D+", ""))))
            .forEach(e ->
                System.out.print(e.getKey() + ": " + e.getValue() + " | "));
        System.out.println();
    }
}