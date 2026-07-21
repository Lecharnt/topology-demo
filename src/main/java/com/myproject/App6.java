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
import org.graphstream.graph.Path;


public class App6 {
    private static int feasibleOptRuns = 0;

    private static int totmaxFWOpt;
    private static int totmaxIDSOpt;
    private static int totmaxTMOpt;
    private static int totmaxWPOpt;

    private static int totMinFWOpt;
    private static int totMinIDSOpt;
    private static int totMinTMOpt;
    private static int totMinWPOpt;

    private static int totOverallMaxOpt;
    private static int totOverallMinOpt;

    private static double totLambda;
    // shared state used across the section methods below
    private static Graph graph;
    private static Random rand;
    private static List<Integer> argsList = new ArrayList<Integer>();
    private static HashMap<String, EdgeRouter> FakeEdgeRouters = new HashMap<>();  
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


    private static int totRuns = 3;

    private static int totmaxFWSingle;
    private static int totmaxIDSSingle;
    private static int totmaxTMSingle;
    private static int totmaxWPSingle;

    private static int totmaxFWRand;
    private static int totmaxIDSRand;
    private static int totmaxTMRand;
    private static int totmaxWPRand;



    private static int totMinFWSingle;
    private static int totMinIDSSingle;
    private static int totMinTMSingle;
    private static int totMinWPSingle;

    private static int totMinFWRand;
    private static int totMinIDSRand;
    private static int totMinTMRand;
    private static int totMinWPRand;

    private static int totOverallMaxSingle;
    private static int totOverallMinSingle;

    private static int totOverallMaxRand;
    private static int totOverallMinRand;



    public static void main(String[] args) throws IOException {
        for (int index = 0; index < totRuns; index++) {
            clearPublicVars();
            PathFinder.clearPublicVars();
            setupConfig();
            buildGraph();
            sortNodesIntoLists();
            buildFlowsAndEdgeRouters();
            reportEdgeRouterStats();
            wireTopologyEdges();
            styleGraphAndBuildDijkstraCache();
            computeRoutingTables();
            simulateFlowsAndTallyPackets();
            runOptimalLP();
            printResultsAndDisplay();

        }
        System.out.println();
        System.out.println("--------");
        System.out.println("FINAL AVERAGES ACROSS " + totRuns + " RUNS");
        System.out.println("-------");

        System.out.println();
        System.out.println("----SINGLE");
        System.out.println("Total Max Single FW: " + totmaxFWSingle / totRuns);
        System.out.println("Total Min Single FW: " + totMinFWSingle / totRuns);
        System.out.println("Total Max Single IDS: " + totmaxIDSSingle / totRuns);
        System.out.println("Total Min Single IDS: " + totMinIDSSingle / totRuns);
        System.out.println("Total Max Single TM: " + totmaxTMSingle / totRuns);
        System.out.println("Total Min Single TM: " + totMinTMSingle / totRuns);
        System.out.println("Total Max Single WP: " + totmaxWPSingle / totRuns);
        System.out.println("Total Min Single WP: " + totMinWPSingle / totRuns);
        System.out.println("Overall Max Single: " + totOverallMaxSingle / totRuns);
        System.out.println("Overall Min Single: " + totOverallMinSingle / totRuns);

        System.out.println();
        System.out.println("------RANDOM");
        System.out.println("Total Max Random FW: " + totmaxFWRand / totRuns);
        System.out.println("Total Min Random FW: " + totMinFWRand / totRuns);
        System.out.println("Total Max Random IDS: " + totmaxIDSRand / totRuns);
        System.out.println("Total Min Random IDS: " + totMinIDSRand / totRuns);
        System.out.println("Total Max Random TM: " + totmaxTMRand / totRuns);
        System.out.println("Total Min Random TM: " + totMinTMRand / totRuns);
        System.out.println("Total Max Random WP: " + totmaxWPRand / totRuns);
        System.out.println("Total Min Random WP: " + totMinWPRand / totRuns);
        System.out.println("Overall Max Random: " + totOverallMaxRand / totRuns);
        System.out.println("Overall Min Random: " + totOverallMinRand / totRuns);

        System.out.println();
        System.out.println("----OPTIMAL (LP)");
        if (feasibleOptRuns == 0) {
            System.out.println("No feasible LP solution was found in any run.");
        } else {
            System.out.println("Feasible runs: " + feasibleOptRuns + " / " + totRuns);
            System.out.println("Total Max Optimal FW: " + totmaxFWOpt / feasibleOptRuns);
            System.out.println("Total Min Optimal FW: " + totMinFWOpt / feasibleOptRuns);
            System.out.println("Total Max Optimal IDS: " + totmaxIDSOpt / feasibleOptRuns);
            System.out.println("Total Min Optimal IDS: " + totMinIDSOpt / feasibleOptRuns);
            System.out.println("Total Max Optimal TM: " + totmaxTMOpt / feasibleOptRuns);
            System.out.println("Total Min Optimal TM: " + totMinTMOpt / feasibleOptRuns);
            System.out.println("Total Max Optimal WP: " + totmaxWPOpt / feasibleOptRuns);
            System.out.println("Total Min Optimal WP: " + totMinWPOpt / feasibleOptRuns);
            System.out.println("Overall Max Optimal: " + totOverallMaxOpt / feasibleOptRuns);
            System.out.println("Overall Min Optimal: " + totOverallMinOpt / feasibleOptRuns);
            System.out.println("Average lambda: " + (totLambda / feasibleOptRuns));
        }
    }
    private static void runOptimalLP() {
        OptimalLP.Result result = OptimalLP.solve(FakeEdgeRouters);

        if (!result.feasible) {
            System.out.println();
            System.out.println("OPTIMAL LP INFEASIBLE THIS RUN SKIPPED");
            return;
        }

        feasibleOptRuns++;

        int fwMin = Integer.MAX_VALUE, fwMax = Integer.MIN_VALUE;
        int idsMin = Integer.MAX_VALUE, idsMax = Integer.MIN_VALUE;
        int tmMin = Integer.MAX_VALUE, tmMax = Integer.MIN_VALUE;
        int wpMin = Integer.MAX_VALUE, wpMax = Integer.MIN_VALUE;

        for (Map.Entry<String, Double> e : result.load.entrySet()) {
            String id = e.getKey();
            int val = (int) Math.round(e.getValue());

            if (id.startsWith(PolicyType.FW.name())) {
                fwMin = Math.min(fwMin, val); fwMax = Math.max(fwMax, val);
            } else if (id.startsWith(PolicyType.IDS.name())) {
                idsMin = Math.min(idsMin, val); idsMax = Math.max(idsMax, val);
            } else if (id.startsWith(PolicyType.TM.name())) {
                tmMin = Math.min(tmMin, val); tmMax = Math.max(tmMax, val);
            } else if (id.startsWith(PolicyType.WP.name())) {
                wpMin = Math.min(wpMin, val); wpMax = Math.max(wpMax, val);
            }
        }

        System.out.println();
        System.out.println("-----OPTIMAL LP THIS RUN");
        System.out.println("lambda: " + result.lambda);
        System.out.println("FW Min: " + fwMin + " Max: " + fwMax);
        System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
        System.out.println("TM Min: " + tmMin + " Max: " + tmMax);
        System.out.println("WP Min: " + wpMin + " Max: " + wpMax);

        totMinFWOpt += fwMin;   totmaxFWOpt += fwMax;
        totMinIDSOpt += idsMin; totmaxIDSOpt += idsMax;
        totMinTMOpt += tmMin;   totmaxTMOpt += tmMax;
        totMinWPOpt += wpMin;   totmaxWPOpt += wpMax;

        totOverallMaxOpt += Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax));
        totOverallMinOpt += Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin));
        totLambda += result.lambda;
    }
    private static void clearPublicVars() {
        graph = null;
        rand = null;
        argsList.clear();
        FakeEdgeRouters.clear();
        flows.clear();

        FWpackest.clear();
        IDSpackest.clear();
        TMpackest.clear();
        WPpackest.clear();

        FWpackestGreed.clear();
        IDSpackestGreed.clear();
        TMpackestGreed.clear();
        WPpackestGreed.clear();

        FWpackestRand.clear();
        IDSpackestRand.clear();
        TMpackestRand.clear();
        WPpackestRand.clear();
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


        for (Node node : graph) {
            List<Node> FWListR = new ArrayList<>();
            List<Node> IDSListR = new ArrayList<>();
            List<Node> WPListR = new ArrayList<>();
            List<Node> TMListR = new ArrayList<>();

            int FWListRandAmount = 4;
            List<Node> tempFW = new ArrayList<>(PathFinder.FWList);
            Collections.shuffle(tempFW);
            FWListR = tempFW.subList(0, FWListRandAmount);

            int IDSListRandAmount = 4;
            List<Node> tempIDS = new ArrayList<>(PathFinder.IDSList);
            Collections.shuffle(tempIDS);
            IDSListR = tempIDS.subList(0, IDSListRandAmount);

            int WPListRandAmount = 2;
            List<Node> tempWP = new ArrayList<>(PathFinder.WPList);
            Collections.shuffle(tempWP);
            WPListR = tempWP.subList(0, WPListRandAmount);

            int TMListRandAmount = 2;
            List<Node> tempTM = new ArrayList<>(PathFinder.TMList);
            Collections.shuffle(tempTM);
            TMListR = tempTM.subList(0, TMListRandAmount);

            PathFinder.NodePackestRand.put(node.getId(), new PacketContainer(FWListR, IDSListR, WPListR,TMListR));

        }

    }

    // Build fake edge routers, load flows from file, assign policies
    private static void buildFlowsAndEdgeRouters() throws IOException {
        for (Node node : PathFinder.ERList) {
            FakeEdgeRouters.put(node.getId(), new EdgeRouter(node));
        }

        HashMap<String, RoutingTable> routers = RouterUtils.setRouters(graph);

        List<String> lines = Files.readAllLines(Paths.get("src/main/java/com/myproject/flowSpread1.txt"));
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");

            String ip = parts[0];
            String count = parts[1];
            Integer cool = Integer.parseInt(count);
            List<EdgeRouter> routers1 = new ArrayList<>(FakeEdgeRouters.values());
            EdgeRouter edgeRouter = routers1.get(RandomUtils.getRandomElemantInList(routers1));

            Flow currentFlow = new Flow(ip, Integer.parseInt(count), edgeRouter.getNode());

            
            int temp = RandomUtils.getRandomElemant();

            String policy = "none";
            List<PolicyType> flowPolicy = new ArrayList<PolicyType>();
            switch (temp) {
                case 1:
                    flowPolicy.add(PolicyType.FW);
                    flowPolicy.add(PolicyType.IDS);
                    flowPolicy.add(PolicyType.WP);
                    break;
                case 2:
                    flowPolicy.add(PolicyType.FW);
                    flowPolicy.add(PolicyType.IDS);
                    break;
                case 3:
                    flowPolicy.add(PolicyType.IDS);
                    flowPolicy.add(PolicyType.TM);
                    break;
                default:
                    System.err.println("there was a out of bounce element");
                    break;
            }
            currentFlow.setFlowPolicy(flowPolicy);
            edgeRouter.addFlow(ip, currentFlow);
            flows.add(currentFlow);
        }
    }

    // policy
    private static void reportEdgeRouterStats() {
        int totalFlows = 0;
        int totalPackets = 0;

        int totalFW = 0;
        int totalIDS = 0;
        int totalTM = 0;
        int totalWP = 0;

        for (EdgeRouter edgeRouter : FakeEdgeRouters.values()) {

            int fw = 0;
            int ids = 0;
            int tm = 0;
            int wp = 0;

            for (Flow flow : edgeRouter.getFlows().values()) {

                if (flow.getFlowPolicy().contains(PolicyType.FW)) {
                    fw++;
                    totalFW++;
                }

                if (flow.getFlowPolicy().contains(PolicyType.IDS)) {
                    ids++;
                    totalIDS++;
                }

                if (flow.getFlowPolicy().contains(PolicyType.TM)) {
                    tm++;
                    totalTM++;
                }

                if (flow.getFlowPolicy().contains(PolicyType.WP)) {
                    wp++;
                    totalWP++;
                }

                totalPackets += flow.getPakets();
            }

            totalFlows += edgeRouter.getFlows().size();

            /*
            System.out.println(
                edgeRouter.getNode().getId() +
                " | Flows: " + edgeRouter.getFlows().size() +
                " | FW: " + fw +
                " | IDS: " + ids +
                " | TM: " + tm +
                " | WP: " + wp
            );
            */
        }

        // System.out.println("Total flows: " + totalFlows);
        // System.out.println("Total packets: " + totalPackets);

        // if (totalFlows > 0) {
        //     System.out.println("Average packets per flow: " + (double) totalPackets / totalFlows);
        // }

        // System.out.println("FW flows : " + totalFW);
        // System.out.println("IDS flows: " + totalIDS);
        // System.out.println("TM flows : " + totalTM);
        // System.out.println("WP flows : " + totalWP);
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

        int howManyFWIDSWP = 32;
        int howManyFWIDS = 16;
        int howManyIDSTM = 8;

        for (EdgeRouter edgeRouter : FakeEdgeRouters.values()) {
            for (int index = 0; index < howManyFWIDSWP; index++) {
                edgeRouter.addFWIdsWpPath(PathFinder.findRandomPathThroughMBs(edgeRouter.getNode(), List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP), graph));
            }
            for (int index = 0; index < howManyFWIDS; index++) {
                edgeRouter.addFwIdsPath(PathFinder.findRandomPathThroughMBs(edgeRouter.getNode(), List.of(PolicyType.FW, PolicyType.IDS), graph));
            }
            for (int index = 0; index < howManyIDSTM; index++) {
                edgeRouter.addIdsTmPath(PathFinder.findRandomPathThroughMBs(edgeRouter.getNode(), List.of(PolicyType.IDS, PolicyType.TM), graph));
            }
        }

        for (Flow flow : flows) {

            Path greedyPath = PathFinder.findGreedyPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);

            Path randomPath = FakeEdgeRouters.get(flow.getNode().getId()).getRandomPath(flow.getFlowPolicy());

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
        //graph.display().enableAutoLayout();
        System.out.println();

        // System.err.println("Single middle boxes");

        // printMap(FWpackestGreed);
        // printMap(IDSpackestGreed);
        // printMap(TMpackestGreed);
        // printMap(WPpackestGreed);

        // System.err.println();
        // System.err.println("Rand middle boxes");

        // printMap(FWpackestRand);
        // printMap(IDSpackestRand);
        // printMap(TMpackestRand);
        // printMap(WPpackestRand);

    int fwMin = Collections.min(FWpackestGreed.values());
    int fwMax = Collections.max(FWpackestGreed.values());
    int idsMin = Collections.min(IDSpackestGreed.values());
    int idsMax = Collections.max(IDSpackestGreed.values());
    int tmMin = Collections.min(TMpackestGreed.values());
    int tmMax = Collections.max(TMpackestGreed.values());
    int wpMin = Collections.min(WPpackestGreed.values());
    int wpMax = Collections.max(WPpackestGreed.values());
    System.out.println();

    System.out.println("Single");
    System.out.println("FW  Min: " + fwMin + " Max: " + fwMax);
    System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
    System.out.println("TM  Min: " + tmMin + " Max: " + tmMax);
    System.out.println("WP  Min: " + wpMin + " Max: " + wpMax);

    System.out.println("Overall Min: " + Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
    System.out.println("Overall Max: " + Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));

    totOverallMaxSingle += Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax));
    totOverallMinSingle += Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin));

    totMinFWSingle += fwMin;
    totMinIDSSingle += idsMin;
    totMinTMSingle += tmMin;
    totMinWPSingle += wpMin;

    totmaxFWSingle += fwMax;
    totmaxIDSSingle += idsMax;
    totmaxTMSingle += tmMax;
    totmaxWPSingle += wpMax;


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

    System.out.println("Overall Min: " + Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
    System.out.println("Overall Max: " + Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));

    totMinFWRand += fwMin;
    totMinIDSRand += idsMin;
    totMinTMRand += tmMin;
    totMinWPRand += wpMin;

    totmaxFWRand += fwMax;
    totmaxIDSRand += idsMax;
    totmaxTMRand += tmMax;
    totmaxWPRand += wpMax;

    totOverallMaxRand += Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax));
    totOverallMinRand += Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin));
}
    private static void printMap(HashMap<String, Integer> map) {
        map.entrySet().stream()
            .sorted(Comparator.comparingInt(e ->
                Integer.parseInt(e.getKey().replaceAll("\\D+", ""))))
            .forEach(e ->
                System.out.print(e.getKey() + ": " + e.getValue() + " | "));
        System.out.println();
    }
    private static final int DEFAULT_MIDDLEBOX_CAPACITY = 1010000;

    private static void MathStuff(Map<String, Integer> FWpackest_,Map<String, Integer> IDSpackest_,Map<String, Integer> TMpackest_,Map<String, Integer> WPpackest_) {

        // minimize λ
        double lambda = 0.0;

        // (h_e,p)
        Map<String, Integer> allMbLoads = new HashMap<>();
        allMbLoads.putAll(FWpackest_);
        allMbLoads.putAll(IDSpackest_);
        allMbLoads.putAll(TMpackest_);
        allMbLoads.putAll(WPpackest_);

        System.out.println("Total middleboxes being checked: " + allMbLoads.size());

        // t(h_e,p) ≥ 0
        for (Map.Entry<String, Integer> entry : allMbLoads.entrySet()) {
            if (entry.getValue() < 0) {
                System.out.println(entry.getKey() + " has negative traffic: " + entry.getValue());
            }
        }
        // Σ t(h_e,p) = T_e,p
        for (EdgeRouter er : FakeEdgeRouters.values()) {
            int totalAssigned = 0;
            for (Flow flow : er.getFlows().values()) {
                totalAssigned += flow.getPakets();
            }
            int totalAdvertised = er.getFlows().values().stream().mapToInt(Flow::getPakets).sum();

            System.out.println("Edge router " + er.getNode().getId() + " | flows: " + er.getFlows().size() + " | total assigned packets: " + totalAssigned + " | total advertised packets: " + totalAdvertised);

            assert totalAssigned == totalAdvertised;
        }
        // Σ Σ t(h_e,p) ≤ λ * c(m)
        for (Map.Entry<String, Integer> entry : allMbLoads.entrySet()) {
            String mbName = entry.getKey();
            double load = entry.getValue();
            double utilization = load / (double) DEFAULT_MIDDLEBOX_CAPACITY;

            System.out.println("Middlebox " + mbName + " | load: " + load + " | capacity: " + DEFAULT_MIDDLEBOX_CAPACITY + " | utilization: " + utilization);

            if (utilization > lambda) {
                System.out.println(mbName + " is the most used");
                lambda = utilization;
            }
        }

        System.out.println("Final lambda: " + lambda);

        // λ ≤ 1
        if (lambda > 1.0) {
            System.out.println("Middlebox capacity exceeded");
        } else {
            System.out.println("Middleboxes are within capacity");
        }
    }
}