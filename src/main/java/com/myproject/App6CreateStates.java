package com.myproject;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.Comparator;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.bouncycastle.jce.provider.JDKDSASigner.stdDSA;
import org.graphstream.algorithm.Dijkstra;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.graphstream.graph.Path;

import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.graph.Edge;

public class App6CreateStates {

    private static int totPackets;
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

    private static HashMap<String, Integer> FWpackestLp = new HashMap<>();
    private static HashMap<String, Integer> IDSpackestLp = new HashMap<>();
    private static HashMap<String, Integer> TMpackestLp = new HashMap<>();
    private static HashMap<String, Integer> WPpackestLp = new HashMap<>();

    private static HashMap<String, Integer> FWpackestGreed = new HashMap<>();
    private static HashMap<String, Integer> IDSpackestGreed = new HashMap<>();
    private static HashMap<String, Integer> TMpackestGreed = new HashMap<>();
    private static HashMap<String, Integer> WPpackestGreed = new HashMap<>();

    private static HashMap<String, Integer> FWpackestRand = new HashMap<>();
    private static HashMap<String, Integer> IDSpackestRand = new HashMap<>();
    private static HashMap<String, Integer> TMpackestRand = new HashMap<>();
    private static HashMap<String, Integer> WPpackestRand = new HashMap<>();


    private static int totRuns = 100;

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

    private static OptimalLP.Result resultLP = null;

    // private static SpriteManager spriteManager;
    // private static int packetId = 0;
    // private static Random animationRandom = new Random();

    // private static PacketAnimator animator;
    public static void main(String[] args) throws IOException {
        for (int index = 0; index < totRuns; index++) {
            clearPublicVars();
            PathFinder.clearPublicVars();
            setupConfig();
            graph = buildGraph(
            50,
            16,
            4,
            8,
            8,
            4,
            4);
            sortNodesIntoLists();
            buildFlowsAndEdgeRouters();
            wireTopologyEdges();
            styleGraphAndBuildDijkstraCache();
            simulateFlowsAndTallyPackets();
            runOptimalLP();
            printResultsAndDisplay();
            CreateState();
            
            //startAnimatedSim();
            
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

        System.out.println("total packets in Network: " + totPackets);

        int totalPaketsInEdgeRouters = 0;
        int totTm = 0;
        for (Map.Entry<String, EdgeRouter> entry : FakeEdgeRouters.entrySet()) {
            totalPaketsInEdgeRouters += entry.getValue().getTotPackets();
            for(Map.Entry<Path, Integer> entrydd : entry.getValue().getIdsTmPathsTraffic().entrySet()){
                totTm =  totTm + entrydd.getValue();
            }
        }
        
        System.out.println("total pakets in all edge routers: " + totalPaketsInEdgeRouters);
        System.out.println("total tm traffic " + totTm);
    }

    private static void runOptimalLP() {
        OptimalLP.Result result = OptimalLP.solve(FakeEdgeRouters, totPackets, graph);

        if (!result.feasible) {
            System.out.println("lambda: " + result.lambda);
            System.out.println("OPTIMAL LP INFEASIBLE THIS RUN SKIPPED");
            return;
        }

        if (result.lambda > 1.0) {
            System.out.println("lambda: " + result.lambda);
            System.out.println("Capacity exceeded this run (lambda > 1) — skipped from stats");
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

        if(result.lambda > 1){
            System.out.println("there is no solution");
            return;
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

        resultLP = result;

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
        resultLP = null;
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
    private static Graph buildGraph(int amount_of_edge_routers, int amount_of_core_routers, int amount_of_main_core_routers, 
        int amount_of_firewalling, int amount_of_intrusion_detection, int amount_of_web_proxing, int amount_of_traffic_measurement) {

        Graph graphTemp = null;
        String red     = "#e74d3c74";
        String blue    = "#8e8a06";
        String green   = "#2ecc3175";
        String orange  = "#037d73";
        String purple  = "#1100ff6b";
        String cyan    = "#ff00fb74";
        String yellow  = "#73090969";

        graphTemp = new SingleGraph("Topology");

        // add nodes core router cr1 er1
        addStyledNodes(graphTemp, "ER", amount_of_edge_routers, blue,PathFinder.ERList);
        addStyledNodes(graphTemp, "CR", amount_of_core_routers, green, PathFinder.CRList);
        addStyledNodes(graphTemp, "M", amount_of_main_core_routers, red, PathFinder.MList);

        addStyledNodes(graphTemp, "FW", amount_of_firewalling, orange, PathFinder.FWList);
        addStyledNodes(graphTemp, "IDS", amount_of_intrusion_detection, purple, PathFinder.IDSList);
        addStyledNodes(graphTemp, "WP", amount_of_web_proxing, cyan, PathFinder.WPList);
        addStyledNodes(graphTemp, "TM", amount_of_traffic_measurement, yellow, PathFinder.TMList);
        return graphTemp;
    }
    private static void addStyledNodes(Graph graphTemp, String prefix, Integer amount, String color, List<Node> listOfNodes) {
        for (int index = 0; index < amount; index++) {
            listOfNodes.add(GraphBuilder.addStyledNode(graphTemp, prefix + index, color));
        }
    }

    // Sort nodes into their type lists
    private static void sortNodesIntoLists() {
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

        // HashMap<String, RoutingTable> routers = RouterUtils.setRouters(graph);

        List<String> lines = Files.readAllLines(Paths.get("src/main/java/com/myproject/flowSpread1.txt"));
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");

            String ip = parts[0];
            String count = parts[1];
            // Integer cool = Integer.parseInt(count);
            List<EdgeRouter> routers1 = new ArrayList<>(FakeEdgeRouters.values());
            EdgeRouter edgeRouter = routers1.get(RandomUtils.getRandomElemantInList(routers1));

            Flow currentFlow = new Flow(ip, Integer.parseInt(count), edgeRouter.getNode());

            
            int temp = RandomUtils.getRandomElemant();

            Random random = new Random();
            temp = random.nextInt(3) + 1;

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

        FWpackestLp = new HashMap<>(FWpackest);
        IDSpackestLp = new HashMap<>(IDSpackest);
        TMpackestLp = new HashMap<>(TMpackest);
        WPpackestLp = new HashMap<>(WPpackest);

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
        totPackets = 0;

        OptimalLP.Result lpResult = OptimalLP.solve(FakeEdgeRouters, totPackets, graph);

        for (Flow flow : flows) {
            EdgeRouter er = FakeEdgeRouters.get(flow.getNode().getId());
            Path chosenPath = OptimalLP.sendPacketViaOptimalLP(er, flow, flow.getPakets(), lpResult);
            Path greedyPath = PathFinder.findGreedyPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);
            Path randomPath = FakeEdgeRouters.get(flow.getNode().getId()).addTrafficToRandomPath(flow.getFlowPolicy(), flow.getPakets());

            totPackets += flow.getPakets();
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


            for (Node node : chosenPath.getNodePath()) {
                String nodeId = node.getId();

                if (nodeId.startsWith(PolicyType.FW.name())) {
                    FWpackestLp.replace(nodeId, FWpackestLp.get(nodeId) + flow.getPakets());
                } else if (nodeId.startsWith(PolicyType.IDS.name())) {
                    IDSpackestLp.replace(nodeId, IDSpackestLp.get(nodeId) + flow.getPakets());

                } else if (nodeId.startsWith(PolicyType.TM.name())) {
                    TMpackestLp.replace(nodeId, TMpackestLp.get(nodeId) + flow.getPakets());

                } else if (nodeId.startsWith(PolicyType.WP.name())) {
                    WPpackestLp.replace(nodeId, WPpackestLp.get(nodeId) + flow.getPakets());

                }
            }

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
        //spriteManager = new SpriteManager(graph);
        // graph.display().enableAutoLayout();
        // try {
        //     Thread.sleep(10000); // let it settle for 3 seconds
        // } catch (InterruptedException e) {
        //     Thread.currentThread().interrupt();
        // }

        // animator = new PacketAnimator(graph);
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



    int fwMinLp = Collections.min(FWpackestLp.values());
    int fwMaxLp = Collections.max(FWpackestLp.values());
    int idsMinLp = Collections.min(IDSpackestLp.values());
    int idsMaxLp = Collections.max(IDSpackestLp.values());
    int tmMinLp = Collections.min(TMpackestLp.values());
    int tmMaxLp = Collections.max(TMpackestLp.values());
    int wpMinLp = Collections.min(WPpackestLp.values());
    int wpMaxLp = Collections.max(WPpackestLp.values());

    System.out.println("\nLP");
    System.out.println("FW  Min: " + fwMinLp + " Max: " + fwMaxLp);
    System.out.println("IDS Min: " + idsMinLp + " Max: " + idsMaxLp);
    System.out.println("TM  Min: " + tmMinLp + " Max: " + tmMaxLp);
    System.out.println("WP  Min: " + wpMinLp + " Max: " + wpMaxLp);

    System.out.println("Overall Min: " + Collections.min(Arrays.asList(fwMinLp, idsMinLp, tmMinLp, wpMinLp)));
    System.out.println("Overall Max: " + Collections.max(Arrays.asList(fwMaxLp, idsMaxLp, tmMaxLp, wpMaxLp)));
}
    private static void CreateState(){
        if (resultLP != null) {
            if(resultLP.lambda <= 1){
                System.out.println("\n=== SAVING STATE ===");
                SavedStates.saveState(graph, flows, FakeEdgeRouters);
                System.out.println("=== STATE SAVED ===\n");
            } else {
                System.out.println("LP lambda > 1 - not saving state");
            }
        }
    }
}