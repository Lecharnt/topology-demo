package com.myproject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App6RunSim {

    private static final boolean USE_SAVED_STATE = true;
    private static final String SAVED_STATE_NAME = null;

    private static final int TOT_RUNS = 1;

    private static int totPackets;
    private static int feasibleOptRuns = 0;
    private static boolean anyFreshRun = false;

    private static int totmaxFWOpt, totmaxIDSOpt, totmaxTMOpt, totmaxWPOpt;
    private static int totMinFWOpt, totMinIDSOpt, totMinTMOpt, totMinWPOpt;
    private static int totOverallMaxOpt, totOverallMinOpt;
    private static double totLambda;

    private static int totmaxFWSingle, totmaxIDSSingle, totmaxTMSingle, totmaxWPSingle;
    private static int totMinFWSingle, totMinIDSSingle, totMinTMSingle, totMinWPSingle;
    private static int totOverallMaxSingle, totOverallMinSingle;

    private static int totmaxFWRand, totmaxIDSRand, totmaxTMRand, totmaxWPRand;
    private static int totMinFWRand, totMinIDSRand, totMinTMRand, totMinWPRand;
    private static int totOverallMaxRand, totOverallMinRand;

    private static int totmaxFWLP, totmaxIDSLP, totmaxTMLP, totmaxWPLP;
    private static int totMinFWLP, totMinIDSLP, totMinTMLP, totMinWPLP;
    private static int totOverallMaxLP, totOverallMinLP;
    
    private static int freshRunsCount = 0;
    private static int lpRunsCount = 0;
    private static int routingDataCount = 0; 

    private static Graph graph;
    private static Random rand;
    private static List<Integer> argsList = new ArrayList<>();
    private static HashMap<String, EdgeRouter> FakeEdgeRouters = new HashMap<>();
    private static List<Flow> flows = new ArrayList<>();

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

    public static void main(String[] args) throws IOException {
        SavedStates.listSavedStates();

        for (int index = 0; index < TOT_RUNS; index++) {
            clearPublicVars();
            PathFinder.clearPublicVars();
            setupConfig();

            boolean loaded = USE_SAVED_STATE && loadSavedSimulation();

            if (!loaded) {
                if (USE_SAVED_STATE) {
                    System.err.println("No saved simulation found — generating a fresh one instead.");
                }
                PathFinder.clearPublicVars();
                graph = buildGraph(50, 16, 4, 8, 8, 4, 4);
                sortNodesIntoLists();
                buildFlowsAndEdgeRouters();
                wireTopologyEdges();
                styleGraphAndBuildDijkstraCache();

                simulateFlowsAndTallyPackets();
                runOptimalLP();
                printResultsAndDisplay();
                anyFreshRun = true;
                freshRunsCount++;

            } else {
    if (graph == null) {
        System.err.println("Error: Graph is null after loading state!");
        return;
    }
    rebuildDijkstraCache();
    totPackets = 0;
    for (EdgeRouter er : FakeEdgeRouters.values()) {
        totPackets += er.getTotPackets();
    }

    tallyLoadedTraffic();
    runOptimalLP();
    computeGreedyAndRandomForLoaded();
    printLoadedResults();
}
        }

        printFinalSummary();
    }

    private static boolean loadSavedSimulation() {
        Graph freshGraph = buildGraph(50, 16, 4, 8, 8, 4, 4);
        graph = freshGraph;
        sortNodesIntoLists();

        SimulationState state = (SAVED_STATE_NAME != null)
                ? SavedStates.loadState(SAVED_STATE_NAME, freshGraph)
                : SavedStates.loadRandState(freshGraph);

        if (state == null) {
            PathFinder.clearPublicVars();
            graph = null;
            return false;
        }

        graph = state.getGraph();
        FakeEdgeRouters = state.edgeRouters;
        flows = state.flows;

        styleGraphAndBuildDijkstraCache();
        return true;
    }

    private static void rebuildDijkstraCache() {
        for (Node node : graph) {
            Dijkstra d = new Dijkstra(Dijkstra.Element.EDGE, null, null);
            d.init(graph);
            d.setSource(node);
            d.compute();
            PathFinder.dijkstraCache.put(node, d);
        }
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

        FWpackest.clear();  IDSpackest.clear();  TMpackest.clear();  WPpackest.clear();
        FWpackestGreed.clear(); IDSpackestGreed.clear(); TMpackestGreed.clear(); WPpackestGreed.clear();
        FWpackestRand.clear();  IDSpackestRand.clear();  TMpackestRand.clear();  WPpackestRand.clear();
    }

    private static void setupConfig() {
        System.setProperty("org.graphstream.ui", "swing");

        int[] values = {2, 2, 6, 6};
        for (int v : values) {
            argsList.add(v);
        }

        rand = new Random();
    }

    private static Graph buildGraph(int erCount, int crCount, int mCount,
            int fwCount, int idsCount, int wpCount, int tmCount) {

        String red = "#e74d3c74", blue = "#8e8a06", green = "#2ecc3175", orange = "#037d73";
        String purple = "#1100ff6b", cyan = "#ff00fb74", yellow = "#73090969";

        Graph graphTemp = new SingleGraph("Topology");

        addStyledNodes(graphTemp, "ER", erCount, blue, PathFinder.ERList);
        addStyledNodes(graphTemp, "CR", crCount, green, PathFinder.CRList);
        addStyledNodes(graphTemp, "M", mCount, red, PathFinder.MList);
        addStyledNodes(graphTemp, "FW", fwCount, orange, PathFinder.FWList);
        addStyledNodes(graphTemp, "IDS", idsCount, purple, PathFinder.IDSList);
        addStyledNodes(graphTemp, "WP", wpCount, cyan, PathFinder.WPList);
        addStyledNodes(graphTemp, "TM", tmCount, yellow, PathFinder.TMList);

        return graphTemp;
    }

    private static void addStyledNodes(Graph graphTemp, String prefix, int amount, String color, List<Node> listOfNodes) {
        for (int index = 0; index < amount; index++) {
            listOfNodes.add(GraphBuilder.addStyledNode(graphTemp, prefix + index, color));
        }
    }

    private static void sortNodesIntoLists() {
        for (Node node : graph) {
            List<Node> tempFW = new ArrayList<>(PathFinder.FWList);
            Collections.shuffle(tempFW);
            List<Node> FWListR = tempFW.subList(0, 4);

            List<Node> tempIDS = new ArrayList<>(PathFinder.IDSList);
            Collections.shuffle(tempIDS);
            List<Node> IDSListR = tempIDS.subList(0, 4);

            List<Node> tempWP = new ArrayList<>(PathFinder.WPList);
            Collections.shuffle(tempWP);
            List<Node> WPListR = tempWP.subList(0, 2);

            List<Node> tempTM = new ArrayList<>(PathFinder.TMList);
            Collections.shuffle(tempTM);
            List<Node> TMListR = tempTM.subList(0, 2);

            PathFinder.NodePackestRand.put(node.getId(), new PacketContainer(FWListR, IDSListR, WPListR, TMListR));
        }
    }

    private static void buildFlowsAndEdgeRouters() throws IOException {
        for (Node node : PathFinder.ERList) {
            FakeEdgeRouters.put(node.getId(), new EdgeRouter(node));
        }

        List<String> lines = Files.readAllLines(Paths.get("src/main/java/com/myproject/flowSpread1.txt"));
        Random random = new Random();

        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            String ip = parts[0];
            int count = Integer.parseInt(parts[1]);

            List<EdgeRouter> routers1 = new ArrayList<>(FakeEdgeRouters.values());
            EdgeRouter edgeRouter = routers1.get(RandomUtils.getRandomElemantInList(routers1));

            Flow currentFlow = new Flow(ip, count, edgeRouter.getNode());

            List<PolicyType> flowPolicy = new ArrayList<>();
            switch (random.nextInt(3) + 1) {
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
            }
            currentFlow.setFlowPolicy(flowPolicy);
            edgeRouter.addFlow(ip, currentFlow);
            flows.add(currentFlow);
        }
    }

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

    private static void styleGraphAndBuildDijkstraCache() {
        for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
        }
        graph.setAttribute("ui.stylesheet",
            "node { fill-color: #4A90D9; size: 15px; text-size: 13; text-color: Black; text-style: bold; }" +
            "edge { fill-color: #000000; size: 2px; }"
        );
        rebuildDijkstraCache();
    }

    private static void simulateFlowsAndTallyPackets() {
        for (Node n : PathFinder.FWList)  FWpackest.put(n.getId(), 0);
        for (Node n : PathFinder.IDSList) IDSpackest.put(n.getId(), 0);
        for (Node n : PathFinder.TMList)  TMpackest.put(n.getId(), 0);
        for (Node n : PathFinder.WPList)  WPpackest.put(n.getId(), 0);

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

        int howManyFWIDSWP = 32, howManyFWIDS = 16, howManyIDSTM = 8;

        for (EdgeRouter edgeRouter : FakeEdgeRouters.values()) {
            for (int i = 0; i < howManyFWIDSWP; i++) {
                edgeRouter.addFWIdsWpPath(PathFinder.findRandomPathThroughMBs(
                        edgeRouter.getNode(), List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP), graph));
            }
            for (int i = 0; i < howManyFWIDS; i++) {
                edgeRouter.addFwIdsPath(PathFinder.findRandomPathThroughMBs(
                        edgeRouter.getNode(), List.of(PolicyType.FW, PolicyType.IDS), graph));
            }
            for (int i = 0; i < howManyIDSTM; i++) {
                edgeRouter.addIdsTmPath(PathFinder.findRandomPathThroughMBs(
                        edgeRouter.getNode(), List.of(PolicyType.IDS, PolicyType.TM), graph));
            }
        }

        totPackets = 0;
        OptimalLP.Result lpResult = OptimalLP.solve(FakeEdgeRouters, totPackets, graph);

        int maxFlows = 1_000_000;
        int processedFlows = 0;

        for (Flow flow : flows) {
            EdgeRouter er = FakeEdgeRouters.get(flow.getNode().getId());
            Path chosenPath = OptimalLP.sendPacketViaOptimalLP(er, flow, flow.getPakets(), lpResult);
            Path greedyPath = PathFinder.findGreedyPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);
            Path randomPath = er.addTrafficToRandomPath(flow.getFlowPolicy(), flow.getPakets());

            if (chosenPath == null || greedyPath == null || randomPath == null) {
                processedFlows++;
                if (processedFlows >= maxFlows) break;
                continue;
            }

            totPackets += flow.getPakets();

            tallyPath(chosenPath, flow.getPakets(), FWpackestLp, IDSpackestLp, TMpackestLp, WPpackestLp);
            tallyPath(greedyPath, flow.getPakets(), FWpackestGreed, IDSpackestGreed, TMpackestGreed, WPpackestGreed);
            tallyPath(randomPath, flow.getPakets(), FWpackestRand, IDSpackestRand, TMpackestRand, WPpackestRand);

            processedFlows++;
            if (processedFlows >= maxFlows) break;
        }
    }

    private static void tallyPath(Path path, int packets,
            HashMap<String, Integer> fw, HashMap<String, Integer> ids,
            HashMap<String, Integer> tm, HashMap<String, Integer> wp) {
        for (Node node : path.getNodePath()) {
            String nodeId = node.getId();
            if (nodeId.startsWith(PolicyType.FW.name())) {
                fw.merge(nodeId, packets, Integer::sum);
            } else if (nodeId.startsWith(PolicyType.IDS.name())) {
                ids.merge(nodeId, packets, Integer::sum);
            } else if (nodeId.startsWith(PolicyType.TM.name())) {
                tm.merge(nodeId, packets, Integer::sum);
            } else if (nodeId.startsWith(PolicyType.WP.name())) {
                wp.merge(nodeId, packets, Integer::sum);
            }
        }
    }

    private static void tallyLoadedTraffic() {
        FWpackestLp.clear();
        IDSpackestLp.clear();
        TMpackestLp.clear();
        WPpackestLp.clear();

        for (Node n : PathFinder.FWList)  FWpackestLp.put(n.getId(), 0);
        for (Node n : PathFinder.IDSList) IDSpackestLp.put(n.getId(), 0);
        for (Node n : PathFinder.TMList)  TMpackestLp.put(n.getId(), 0);
        for (Node n : PathFinder.WPList)  WPpackestLp.put(n.getId(), 0);

        for (EdgeRouter er : FakeEdgeRouters.values()) {
            tallyTrafficMap(er.getFwIdsWpTraffic());
            tallyTrafficMap(er.getFwIdsTraffic());
            tallyTrafficMap(er.getIdsTmTraffic());
        }
    }

    private static void tallyTrafficMap(HashMap<Path, Integer> trafficMap) {
        for (Map.Entry<Path, Integer> entry : trafficMap.entrySet()) {
            tallyPath(entry.getKey(), entry.getValue(), FWpackestLp, IDSpackestLp, TMpackestLp, WPpackestLp);
        }
    }

    private static void computeGreedyAndRandomForLoaded() {
        for (Node n : PathFinder.FWList)  FWpackest.put(n.getId(), 0);
        for (Node n : PathFinder.IDSList) IDSpackest.put(n.getId(), 0);
        for (Node n : PathFinder.TMList)  TMpackest.put(n.getId(), 0);
        for (Node n : PathFinder.WPList)  WPpackest.put(n.getId(), 0);

        FWpackestGreed = new HashMap<>(FWpackest);
        IDSpackestGreed = new HashMap<>(IDSpackest);
        TMpackestGreed = new HashMap<>(TMpackest);
        WPpackestGreed = new HashMap<>(WPpackest);

        FWpackestRand = new HashMap<>(FWpackest);
        IDSpackestRand = new HashMap<>(IDSpackest);
        TMpackestRand = new HashMap<>(TMpackest);
        WPpackestRand = new HashMap<>(WPpackest);

        for (Flow flow : flows) {
            EdgeRouter er = FakeEdgeRouters.get(flow.getNode().getId());
            if (er == null) continue;

            Path greedyPath = PathFinder.findGreedyPathThroughMBs(flow.getNode(), flow.getFlowPolicy(), graph, 1000);
            Path randomPath = er.addTrafficToRandomPath(flow.getFlowPolicy(), flow.getPakets());

            if (greedyPath != null) {
                tallyPath(greedyPath, flow.getPakets(), FWpackestGreed, IDSpackestGreed, TMpackestGreed, WPpackestGreed);
            }
            if (randomPath != null) {
                tallyPath(randomPath, flow.getPakets(), FWpackestRand, IDSpackestRand, TMpackestRand, WPpackestRand);
            }
        }
    }

    private static void printLoadedResults() {
        System.out.println();

        int fwMin = Collections.min(FWpackestLp.values());
        int fwMax = Collections.max(FWpackestLp.values());
        int idsMin = Collections.min(IDSpackestLp.values());
        int idsMax = Collections.max(IDSpackestLp.values());
        int tmMin = Collections.min(TMpackestLp.values());
        int tmMax = Collections.max(TMpackestLp.values());
        int wpMin = Collections.min(WPpackestLp.values());
        int wpMax = Collections.max(WPpackestLp.values());

        System.out.println("FW  Min: " + fwMin + " Max: " + fwMax);
        System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
        System.out.println("TM  Min: " + tmMin + " Max: " + tmMax);
        System.out.println("WP  Min: " + wpMin + " Max: " + wpMax);
        System.out.println("Overall Min: " + Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
        System.out.println("Overall Max: " + Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));
        System.out.println("total packets in Network: " + totPackets);

        totMinFWLP += fwMin;   totmaxFWLP += fwMax;
        totMinIDSLP += idsMin; totmaxIDSLP += idsMax;
        totMinTMLP += tmMin;   totmaxTMLP += tmMax;
        totMinWPLP += wpMin;   totmaxWPLP += wpMax;
        totOverallMaxLP += Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax));
        totOverallMinLP += Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin));
        lpRunsCount++;

        // Single (greedy) — now populated by computeGreedyAndRandomForLoaded()
        int gFwMin = Collections.min(FWpackestGreed.values());
        int gFwMax = Collections.max(FWpackestGreed.values());
        int gIdsMin = Collections.min(IDSpackestGreed.values());
        int gIdsMax = Collections.max(IDSpackestGreed.values());
        int gTmMin = Collections.min(TMpackestGreed.values());
        int gTmMax = Collections.max(TMpackestGreed.values());
        int gWpMin = Collections.min(WPpackestGreed.values());
        int gWpMax = Collections.max(WPpackestGreed.values());

        System.out.println("\nSingle (recomputed on loaded flows)");
        System.out.println("FW  Min: " + gFwMin + " Max: " + gFwMax);
        System.out.println("IDS Min: " + gIdsMin + " Max: " + gIdsMax);
        System.out.println("TM  Min: " + gTmMin + " Max: " + gTmMax);
        System.out.println("WP  Min: " + gWpMin + " Max: " + gWpMax);
        System.out.println("Overall Min: " + Collections.min(Arrays.asList(gFwMin, gIdsMin, gTmMin, gWpMin)));
        System.out.println("Overall Max: " + Collections.max(Arrays.asList(gFwMax, gIdsMax, gTmMax, gWpMax)));

        totOverallMaxSingle += Collections.max(Arrays.asList(gFwMax, gIdsMax, gTmMax, gWpMax));
        totOverallMinSingle += Collections.min(Arrays.asList(gFwMin, gIdsMin, gTmMin, gWpMin));
        totMinFWSingle += gFwMin; totMinIDSSingle += gIdsMin; totMinTMSingle += gTmMin; totMinWPSingle += gWpMin;
        totmaxFWSingle += gFwMax; totmaxIDSSingle += gIdsMax; totmaxTMSingle += gTmMax; totmaxWPSingle += gWpMax;

        // Random — now populated by computeGreedyAndRandomForLoaded()
        int rFwMin = Collections.min(FWpackestRand.values());
        int rFwMax = Collections.max(FWpackestRand.values());
        int rIdsMin = Collections.min(IDSpackestRand.values());
        int rIdsMax = Collections.max(IDSpackestRand.values());
        int rTmMin = Collections.min(TMpackestRand.values());
        int rTmMax = Collections.max(TMpackestRand.values());
        int rWpMin = Collections.min(WPpackestRand.values());
        int rWpMax = Collections.max(WPpackestRand.values());

        System.out.println("\nRandom (recomputed on loaded flows)");
        System.out.println("FW  Min: " + rFwMin + " Max: " + rFwMax);
        System.out.println("IDS Min: " + rIdsMin + " Max: " + rIdsMax);
        System.out.println("TM  Min: " + rTmMin + " Max: " + rTmMax);
        System.out.println("WP  Min: " + rWpMin + " Max: " + rWpMax);
        System.out.println("Overall Min: " + Collections.min(Arrays.asList(rFwMin, rIdsMin, rTmMin, rWpMin)));
        System.out.println("Overall Max: " + Collections.max(Arrays.asList(rFwMax, rIdsMax, rTmMax, rWpMax)));

        totMinFWRand += rFwMin; totMinIDSRand += rIdsMin; totMinTMRand += rTmMin; totMinWPRand += rWpMin;
        totmaxFWRand += rFwMax; totmaxIDSRand += rIdsMax; totmaxTMRand += rTmMax; totmaxWPRand += rWpMax;
        totOverallMaxRand += Collections.max(Arrays.asList(rFwMax, rIdsMax, rTmMax, rWpMax));
        totOverallMinRand += Collections.min(Arrays.asList(rFwMin, rIdsMin, rTmMin, rWpMin));

        routingDataCount++;
    }

    private static void printResultsAndDisplay() {
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
        totMinFWSingle += fwMin; totMinIDSSingle += idsMin; totMinTMSingle += tmMin; totMinWPSingle += wpMin;
        totmaxFWSingle += fwMax; totmaxIDSSingle += idsMax; totmaxTMSingle += tmMax; totmaxWPSingle += wpMax;

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

        totMinFWRand += fwMin; totMinIDSRand += idsMin; totMinTMRand += tmMin; totMinWPRand += wpMin;
        totmaxFWRand += fwMax; totmaxIDSRand += idsMax; totmaxTMRand += tmMax; totmaxWPRand += wpMax;
        totOverallMaxRand += Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax));
        totOverallMinRand += Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin));

        fwMin = Collections.min(FWpackestLp.values());
        fwMax = Collections.max(FWpackestLp.values());
        idsMin = Collections.min(IDSpackestLp.values());
        idsMax = Collections.max(IDSpackestLp.values());
        tmMin = Collections.min(TMpackestLp.values());
        tmMax = Collections.max(TMpackestLp.values());
        wpMin = Collections.min(WPpackestLp.values());
        wpMax = Collections.max(WPpackestLp.values());

        System.out.println("\nLP");
        System.out.println("FW  Min: " + fwMin + " Max: " + fwMax);
        System.out.println("IDS Min: " + idsMin + " Max: " + idsMax);
        System.out.println("TM  Min: " + tmMin + " Max: " + tmMax);
        System.out.println("WP  Min: " + wpMin + " Max: " + wpMax);
        System.out.println("Overall Min: " + Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin)));
        System.out.println("Overall Max: " + Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax)));

        totMinFWLP += fwMin; totMinIDSLP += idsMin; totMinTMLP += tmMin; totMinWPLP += wpMin;
        totmaxFWLP += fwMax; totmaxIDSLP += idsMax; totmaxTMLP += tmMax; totmaxWPLP += wpMax;
        totOverallMaxLP += Collections.max(Arrays.asList(fwMax, idsMax, tmMax, wpMax));
        totOverallMinLP += Collections.min(Arrays.asList(fwMin, idsMin, tmMin, wpMin));
        lpRunsCount++;
        System.out.println("total packets in Network: " + totPackets);

        routingDataCount++;
    }

    private static void printFinalSummary() {
        System.out.println();
        System.out.println("--------");
        System.out.println("FINAL AVERAGES ACROSS " + TOT_RUNS + " RUNS");
        System.out.println("-------");

        if (routingDataCount > 0) {
            System.out.println();
            System.out.println("----SINGLE");
            System.out.println("Total Max Single FW: " + totmaxFWSingle / routingDataCount);
            System.out.println("Total Min Single FW: " + totMinFWSingle / routingDataCount);
            System.out.println("Total Max Single IDS: " + totmaxIDSSingle / routingDataCount);
            System.out.println("Total Min Single IDS: " + totMinIDSSingle / routingDataCount);
            System.out.println("Total Max Single TM: " + totmaxTMSingle / routingDataCount);
            System.out.println("Total Min Single TM: " + totMinTMSingle / routingDataCount);
            System.out.println("Total Max Single WP: " + totmaxWPSingle / routingDataCount);
            System.out.println("Total Min Single WP: " + totMinWPSingle / routingDataCount);
            System.out.println("Overall Max Single: " + totOverallMaxSingle / routingDataCount);
            System.out.println("Overall Min Single: " + totOverallMinSingle / routingDataCount);

            System.out.println();
            System.out.println("------RANDOM");
            System.out.println("Total Max Random FW: " + totmaxFWRand / routingDataCount);
            System.out.println("Total Min Random FW: " + totMinFWRand / routingDataCount);
            System.out.println("Total Max Random IDS: " + totmaxIDSRand / routingDataCount);
            System.out.println("Total Min Random IDS: " + totMinIDSRand / routingDataCount);
            System.out.println("Total Max Random TM: " + totmaxTMRand / routingDataCount);
            System.out.println("Total Min Random TM: " + totMinTMRand / routingDataCount);
            System.out.println("Total Max Random WP: " + totmaxWPRand / routingDataCount);
            System.out.println("Total Min Random WP: " + totMinWPRand / routingDataCount);
            System.out.println("Overall Max Random: " + totOverallMaxRand / routingDataCount);
            System.out.println("Overall Min Random: " + totOverallMinRand / routingDataCount);
        } else {
            System.out.println();
            System.out.println("----SINGLE / ------RANDOM");
            System.out.println("No greedy or random path data was generated in any run. N/A.");
        }

        if (lpRunsCount > 0) {
            System.out.println();
            System.out.println("------LP");
            System.out.println("Total Max LP FW: " + totmaxFWLP / lpRunsCount);
            System.out.println("Total Min LP FW: " + totMinFWLP / lpRunsCount);
            System.out.println("Total Max LP IDS: " + totmaxIDSLP / lpRunsCount);
            System.out.println("Total Min LP IDS: " + totMinIDSLP / lpRunsCount);
            System.out.println("Total Max LP TM: " + totmaxTMLP / lpRunsCount);
            System.out.println("Total Min LP TM: " + totMinTMLP / lpRunsCount);
            System.out.println("Total Max LP WP: " + totmaxWPLP / lpRunsCount);
            System.out.println("Total Min LP WP: " + totMinWPLP / lpRunsCount);
            System.out.println("Overall Max LP: " + totOverallMaxLP / lpRunsCount);
            System.out.println("Overall Min LP: " + totOverallMinLP / lpRunsCount);
        } else {
            System.out.println();
            System.out.println("------LP");
            System.out.println("No LP-routed traffic data was available in any run. N/A.");
        }

        System.out.println();
        System.out.println("----OPTIMAL (LP)");
        if (feasibleOptRuns == 0) {
            System.out.println("No feasible LP solution was found in any run.");
        } else {
            System.out.println("Feasible runs: " + feasibleOptRuns + " / " + TOT_RUNS);
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
        for (EdgeRouter er : FakeEdgeRouters.values()) {
            totalPaketsInEdgeRouters += er.getTotPackets();
            for (Map.Entry<Path, Integer> entry : er.getIdsTmPathsTraffic().entrySet()) {
                totTm += entry.getValue();
            }
        }

        System.out.println("total pakets in all edge routers: " + totalPaketsInEdgeRouters);
        System.out.println("total tm traffic " + totTm);
    }
}