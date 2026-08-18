package com.myproject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.algorithm.Dijkstra;

/**
 * Builds fresh random topologies (same generation logic as App6CreateStates)
 * and keeps going until it has collected exactly TARGET_INFEASIBLE
 * infeasible ones and TARGET_FEASIBLE feasible ones, saving each to its
 * own labeled subfolder:
 *
 *   saved_states/successfulLP/  <- feasible (lambda <= 1.0), same folder
 *                                  App6RunSim already loads from
 *   saved_states/infeasibleLP/  <- infeasible OR lambda > 1.0
 *
 * Requires the SavedStates.java patch (SavedStates_PATCH.md) applied first.
 */
public class App7GenerateTestStates {

    private static final int TARGET_INFEASIBLE = 3;
    private static final int TARGET_FEASIBLE = 2;
    private static final int MAX_ATTEMPTS = 500; // safety cap

    private static final String INFEASIBLE_FOLDER = "infeasibleLP";

    private static Graph graph;
    private static Random rand;
    private static List<Integer> argsList = new ArrayList<>();
    private static HashMap<String, EdgeRouter> FakeEdgeRouters = new HashMap<>();
    private static List<Flow> flows = new ArrayList<>();

    private static final List<String[]> summary = new ArrayList<>(); // [status, lambda, filenameFolder]

    public static void main(String[] args) throws IOException {
        int savedFeasible = 0;
        int savedInfeasible = 0;
        int attempt = 0;

        // Build the topology ONCE -- same graph structure for every attempt.
        // Only flow assignment + path pools get re-rolled per attempt below,
        // since those are what actually swing feasibility.
        clearPublicVars();
        PathFinder.clearPublicVars();
        setupConfig();
        graph = buildGraph(50, 16, 4, 8, 8, 4, 4);
        sortNodesIntoLists();
        wireTopologyEdges();
        styleGraphAndBuildDijkstraCache();
        System.out.println("Base topology built once. Re-rolling flows/paths per attempt.\n");

        while ((savedFeasible < TARGET_FEASIBLE || savedInfeasible < TARGET_INFEASIBLE)
                && attempt < MAX_ATTEMPTS) {
            attempt++;

            // Re-roll only flows + path pools -- SAME graph/topology every time.
            FakeEdgeRouters.clear();
            flows.clear();
            resetPathPools();
            buildFlowsAndEdgeRouters();
            populatePathPools();

            OptimalLP.Result result = OptimalLP.solve(FakeEdgeRouters, 0, graph);
            boolean feasible = result.feasible && result.lambda <= 1.0;

            if (feasible && savedFeasible < TARGET_FEASIBLE) {
                SavedStates.saveState(graph, flows, FakeEdgeRouters); // goes to successfulLP
                savedFeasible++;
                summary.add(new String[]{"FEASIBLE", String.format("%.4f", result.lambda), "successfulLP"});
                System.out.println("[attempt " + attempt + "] FEASIBLE  lambda=" + result.lambda
                        + "  -> saved (" + savedFeasible + "/" + TARGET_FEASIBLE + " feasible)");

            } else if (!feasible && savedInfeasible < TARGET_INFEASIBLE) {
                SavedStates.saveStateToFolder(graph, flows, FakeEdgeRouters, INFEASIBLE_FOLDER);
                savedInfeasible++;
                String lambdaStr = result.feasible ? String.format("%.4f", result.lambda) : "N/A (infeasible)";
                summary.add(new String[]{"INFEASIBLE", lambdaStr, INFEASIBLE_FOLDER});
                System.out.println("[attempt " + attempt + "] INFEASIBLE  lambda=" + lambdaStr
                        + "  -> saved (" + savedInfeasible + "/" + TARGET_INFEASIBLE + " infeasible)");

            } else {
                String status = feasible ? "FEASIBLE (skipped, quota met)" : "INFEASIBLE (skipped, quota met)";
                System.out.println("[attempt " + attempt + "] " + status);
            }
        }

        System.out.println("\n=== SUMMARY ===");
        System.out.println("Total attempts: " + attempt);
        System.out.println("Feasible saved: " + savedFeasible + "/" + TARGET_FEASIBLE);
        System.out.println("Infeasible saved: " + savedInfeasible + "/" + TARGET_INFEASIBLE);
        for (int i = 0; i < summary.size(); i++) {
            String[] row = summary.get(i);
            System.out.println("  " + (i + 1) + ". " + row[0] + "  lambda=" + row[1] + "  folder=" + row[2]);
        }

        if (savedFeasible < TARGET_FEASIBLE || savedInfeasible < TARGET_INFEASIBLE) {
            System.out.println("\nWARNING: hit MAX_ATTEMPTS (" + MAX_ATTEMPTS + ") before reaching both targets. "
                    + "Increase MAX_ATTEMPTS or adjust capacities/flow counts if this keeps happening -- "
                    + "it likely means one outcome (feasible or infeasible) is much rarer than the other "
                    + "with the current topology parameters.");
        }
    }

    private static void clearPublicVars() {
        graph = null;
        rand = null;
        argsList.clear();
        FakeEdgeRouters.clear();
        flows.clear();
    }

    private static void setupConfig() {
        System.setProperty("org.graphstream.ui", "swing");
        int[] values = {2, 2, 6, 6};
        for (int v : values) argsList.add(v);
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
        for (Node node : graph) {
            Dijkstra d = new Dijkstra(Dijkstra.Element.EDGE, null, null);
            d.init(graph);
            d.setSource(node);
            d.compute();
            PathFinder.dijkstraCache.put(node, d);
        }
    }

    // Same random path pools App6RunSim/App6CreateStates build before solving the LP.
    private static void populatePathPools() {
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
    }

    // NOTE: EdgeRouter objects get fully recreated in buildFlowsAndEdgeRouters()
    // each attempt (FakeEdgeRouters.clear() + new EdgeRouter(node) per ER), so
    // their internal path-pool lists start empty automatically -- nothing to
    // reset by hand here. Kept as an explicit no-op for clarity/readability.
    private static void resetPathPools() {
        // intentionally empty -- see note above
    }
}