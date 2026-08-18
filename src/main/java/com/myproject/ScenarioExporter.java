package com.myproject;

import org.graphstream.graph.Path;
import org.graphstream.graph.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Dumps each edge-router/policy "pool" (the set of alternative paths OptimalLP
 * chose between) as a scenario JSON, including the LP's ACTUAL optimal
 * per-path weight split as ground truth.
 *
 * One JSON file per pool -> one JSON file = one test case for the AI model.
 *
 * Call this right after OptimalLP.solve() returns a feasible result with
 * lambda <= 1.0 (same condition your code already checks before trusting
 * the LP output).
 */
public class ScenarioExporter {

    private static final List<PolicyType> CHAIN_FW_IDS_WP = List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP);
    private static final List<PolicyType> CHAIN_FW_IDS = List.of(PolicyType.FW, PolicyType.IDS);
    private static final List<PolicyType> CHAIN_IDS_TM = List.of(PolicyType.IDS, PolicyType.TM);

    public static void exportAll(OptimalLP.Result result, Map<String, EdgeRouter> edgeRouters, String outputDir) {
        if (result == null || !result.feasible || result.lambda > 1.0) {
            System.err.println("ScenarioExporter: skipping export, LP result not usable for export");
            return;
        }

        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            System.err.println("ScenarioExporter: could not create output dir: " + e.getMessage());
            return;
        }

        int fileIndex = 0;
        for (EdgeRouter er : edgeRouters.values()) {
            fileIndex = exportPool(result, er, er.getFwIdsWpPaths(), CHAIN_FW_IDS_WP, "FW_IDS_WP", outputDir, fileIndex);
            fileIndex = exportPool(result, er, er.getFwIdsPaths(), CHAIN_FW_IDS, "FW_IDS", outputDir, fileIndex);
            fileIndex = exportPool(result, er, er.getIdsTmPaths(), CHAIN_IDS_TM, "IDS_TM", outputDir, fileIndex);
        }

        System.out.println("ScenarioExporter: wrote " + fileIndex + " scenario files to " + outputDir);
    }

    /**
     * For INFEASIBLE results: there's no LP ground truth to compare against
     * (result.pathTraffic will be empty), so this exports raw scenarios --
     * alternative paths + demand computed directly from each edge router's
     * flows -- with no ground_truth_optimal_weights field. Use this to test
     * whether the AI still returns a structurally valid, sane response when
     * there's no "correct" answer, rather than to grade its accuracy.
     */
    public static void exportAllNoGroundTruth(Map<String, EdgeRouter> edgeRouters, String outputDir) {
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            System.err.println("ScenarioExporter: could not create output dir: " + e.getMessage());
            return;
        }

        int fileIndex = 0;
        for (EdgeRouter er : edgeRouters.values()) {
            fileIndex = exportPoolRaw(er, er.getFwIdsWpPaths(), CHAIN_FW_IDS_WP, "FW_IDS_WP", outputDir, fileIndex);
            fileIndex = exportPoolRaw(er, er.getFwIdsPaths(), CHAIN_FW_IDS, "FW_IDS", outputDir, fileIndex);
            fileIndex = exportPoolRaw(er, er.getIdsTmPaths(), CHAIN_IDS_TM, "IDS_TM", outputDir, fileIndex);
        }

        System.out.println("ScenarioExporter: wrote " + fileIndex + " NO-GROUND-TRUTH scenario files to " + outputDir);
    }

    private static int exportPoolRaw(EdgeRouter er, List<Path> pool, List<PolicyType> chain,
                                      String policyLabel, String outputDir, int fileIndex) {
        if (pool == null || pool.isEmpty()) {
            return fileIndex;
        }

        // Demand computed directly from flows -- doesn't depend on the LP
        // having found a solution.
        double demand = 0;
        for (Flow flow : er.getFlows().values()) {
            if (flow.getFlowPolicy().equals(chain)) {
                demand += flow.getPakets();
            }
        }
        if (demand <= 0) {
            return fileIndex;
        }

        StringBuilder pathsJson = new StringBuilder();
        int pathIdx = 0;
        boolean firstPath = true;

        for (Path path : pool) {
            if (path == null) continue;
            String pathId = "h" + pathIdx;
            pathIdx++;

            StringBuilder mbList = new StringBuilder();
            boolean firstMb = true;
            for (Node node : path.getNodePath()) {
                String id = node.getId();
                if (id.startsWith("FW") || id.startsWith("IDS") || id.startsWith("TM") || id.startsWith("WP")) {
                    if (!firstMb) mbList.append(", ");
                    mbList.append("\"").append(id).append("\"");
                    firstMb = false;
                }
            }

            if (!firstPath) pathsJson.append(",\n");
            pathsJson.append("    {\"id\": \"").append(pathId)
                      .append("\", \"middleboxes\": [").append(mbList).append("]}");
            firstPath = false;
        }

        if (pathIdx == 0) {
            return fileIndex;
        }

        String scenarioName = er.getNode().getId() + "_" + policyLabel;
        String fileName = outputDir + "/scenario_" + String.format("%03d", fileIndex) + "_" + scenarioName + ".json";

        String json = "{\n" +
                "  \"edge_switch\": \"" + er.getNode().getId() + "\",\n" +
                "  \"policy\": \"" + policyLabel + "\",\n" +
                "  \"alternative_paths\": [\n" + pathsJson + "\n  ],\n" +
                "  \"total_traffic\": " + Math.round(demand) + "\n" +
                "}\n";

        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        } catch (IOException e) {
            System.err.println("ScenarioExporter: failed to write " + fileName + ": " + e.getMessage());
            return fileIndex;
        }

        return fileIndex + 1;
    }


    private static int exportPool(OptimalLP.Result result, EdgeRouter er, List<Path> pool,
                                   List<PolicyType> chain, String policyLabel,
                                   String outputDir, int fileIndex) {
        if (pool == null || pool.isEmpty()) {
            return fileIndex;
        }

        String key = OptimalLP.poolKey(er.getNode().getId(), chain);
        Map<Path, Double> optimalSplit = result.pathTraffic.get(key);

        // Skip pools the LP assigned no traffic to at all — nothing to compare.
        if (optimalSplit == null || optimalSplit.isEmpty()) {
            return fileIndex;
        }

        double demand = 0;
        for (double v : optimalSplit.values()) {
            demand += v;
        }
        if (demand <= 0) {
            return fileIndex;
        }

        StringBuilder pathsJson = new StringBuilder();
        StringBuilder groundTruthJson = new StringBuilder();
        int pathIdx = 0;
        boolean firstPath = true;
        boolean firstGT = true;

        for (Path path : pool) {
            if (path == null) continue;
            String pathId = "h" + pathIdx;
            pathIdx++;

            StringBuilder mbList = new StringBuilder();
            boolean firstMb = true;
            for (Node node : path.getNodePath()) {
                String id = node.getId();
                if (id.startsWith("FW") || id.startsWith("IDS") || id.startsWith("TM") || id.startsWith("WP")) {
                    if (!firstMb) mbList.append(", ");
                    mbList.append("\"").append(id).append("\"");
                    firstMb = false;
                }
            }

            if (!firstPath) pathsJson.append(",\n");
            pathsJson.append("    {\"id\": \"").append(pathId)
                      .append("\", \"middleboxes\": [").append(mbList).append("]}");
            firstPath = false;

            Double traffic = optimalSplit.get(path);
            double weight = (traffic == null) ? 0.0 : (traffic / demand);
            if (!firstGT) groundTruthJson.append(", ");
            groundTruthJson.append("\"").append(pathId).append("\": ").append(String.format("%.6f", weight));
            firstGT = false;
        }

        if (pathIdx == 0) {
            return fileIndex; // no valid paths after filtering
        }

        String scenarioName = er.getNode().getId() + "_" + policyLabel;
        String fileName = outputDir + "/scenario_" + String.format("%03d", fileIndex) + "_" + scenarioName + ".json";

        String json = "{\n" +
                "  \"edge_switch\": \"" + er.getNode().getId() + "\",\n" +
                "  \"policy\": \"" + policyLabel + "\",\n" +
                "  \"alternative_paths\": [\n" + pathsJson + "\n  ],\n" +
                "  \"total_traffic\": " + Math.round(demand) + ",\n" +
                "  \"ground_truth_optimal_weights\": {" + groundTruthJson + "}\n" +
                "}\n";

        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(json);
        } catch (IOException e) {
            System.err.println("ScenarioExporter: failed to write " + fileName + ": " + e.getMessage());
            return fileIndex;
        }

        return fileIndex + 1;
    }
}