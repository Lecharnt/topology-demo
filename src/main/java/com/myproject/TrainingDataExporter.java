package com.myproject;

import com.google.gson.Gson;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TrainingDataExporter {

    private static final String SYSTEM_PROMPT =
            "You are a network load-balancing assistant for a hybrid SDN-label switching system. " +
            "Given a policy and the current state of the network, output the enforcement path selection " +
            "and load distribution in the exact JSON format shown in examples. Only use middleboxes and " +
            "paths listed in the input. Respect all capacity constraints.";

    private static final Gson GSON = new Gson();

    private static final List<List<PolicyType>> CHAINS = List.of(
            List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP),
            List.of(PolicyType.FW, PolicyType.IDS),
            List.of(PolicyType.IDS, PolicyType.TM)
    );

    public static void export(Map<String, EdgeRouter> edgeRouters, Graph graph,
                               OptimalLP.Result result, String outputPath) {

        if (result == null || !result.feasible || result.lambda > 1.0) {
            return;
        }

        try (FileWriter fw = new FileWriter(outputPath, true); // append mode
             BufferedWriter writer = new BufferedWriter(fw)) {

            for (EdgeRouter er : edgeRouters.values()) {
                for (List<PolicyType> chain : CHAINS) {
                    String example = buildExample(er, chain, result);
                    if (example != null) {
                        writer.write(example);
                        writer.newLine();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String buildExample(EdgeRouter er, List<PolicyType> chain, OptimalLP.Result result) {

        String poolKey = OptimalLP.poolKey(er.getNode().getId(), chain);
        Map<Path, Double> pathTraffic = result.pathTraffic.get(poolKey);

        if (pathTraffic == null || pathTraffic.isEmpty()) {
            return null;
        }

        double demand = 0;
        for (double v : pathTraffic.values()) {
            demand += v;
        }
        if (demand <= 0) {
            return null;
        }

        List<Map<String, Object>> altPaths = new ArrayList<>();
        List<Map<String, Object>> assignments = new ArrayList<>();
        Map<String, Double> mbLoadFromThisPool = new HashMap<>();
        Set<String> mbIdsUsed = new LinkedHashSet<>();

        int h = 1;
        for (Map.Entry<Path, Double> entry : pathTraffic.entrySet()) {
            Path path = entry.getKey();
            double tVal = entry.getValue();
            String pathId = "h" + h++;

            List<String> mbIds = new ArrayList<>();
            for (Node node : path.getNodePath()) {
                String nid = node.getId();
                if (isMiddlebox(nid)) {
                    mbIds.add(nid);
                    mbIdsUsed.add(nid);
                    mbLoadFromThisPool.merge(nid, tVal, Double::sum);
                }
            }

            Map<String, Object> altPath = new LinkedHashMap<>();
            altPath.put("id", pathId);
            altPath.put("middleboxes", mbIds);
            altPaths.add(altPath);

            if (tVal > 0) {
                Map<String, Object> assignment = new LinkedHashMap<>();
                assignment.put("path_id", pathId);
                assignment.put("weight", round4(tVal / demand));
                assignments.add(assignment);
            }
        }

        if (assignments.isEmpty()) {
            return null;
        }

        Map<String, Integer> capMap = new LinkedHashMap<>();
        Map<String, Double> loadMap = new LinkedHashMap<>();
        for (String mbId : mbIdsUsed) {
            capMap.put(mbId, capacityFor(mbId));
            double totalLoad = result.load.getOrDefault(mbId, 0.0);
            double backgroundLoad = totalLoad - mbLoadFromThisPool.getOrDefault(mbId, 0.0);
            loadMap.put(mbId, Math.max(0.0, round1(backgroundLoad)));
        }

        Map<String, Object> userObj = new LinkedHashMap<>();
        userObj.put("edge_switch", er.getNode().getId());

        Map<String, Object> policyObj = new LinkedHashMap<>();
        policyObj.put("traffic_descriptor", descriptorFor(chain));
        List<String> actionList = new ArrayList<>();
        for (PolicyType pt : chain) {
            actionList.add(pt.name());
        }
        policyObj.put("action_list", actionList);
        userObj.put("policy", policyObj);

        userObj.put("alternative_paths", altPaths);
        userObj.put("middlebox_capacity", capMap);
        userObj.put("current_load", loadMap);
        userObj.put("traffic_volume", (int) Math.round(demand));

        Map<String, Object> assistantObj = new LinkedHashMap<>();
        assistantObj.put("assignments", assignments);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", GSON.toJson(userObj));
        messages.add(userMsg);

        Map<String, String> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", GSON.toJson(assistantObj));
        messages.add(assistantMsg);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("messages", messages);

        return GSON.toJson(root);
    }

    private static boolean isMiddlebox(String nodeId) {
        return nodeId.startsWith("FW") || nodeId.startsWith("IDS")
                || nodeId.startsWith("WP") || nodeId.startsWith("TM");
    }

    private static int capacityFor(String mbId) {
        if (mbId.startsWith("FW")) return OptimalLP.FW_CAP;
        if (mbId.startsWith("IDS")) return OptimalLP.IDS_CAP;
        if (mbId.startsWith("WP")) return OptimalLP.WP_CAP;
        if (mbId.startsWith("TM")) return OptimalLP.TM_CAP;
        return 0;
    }

    private static String descriptorFor(List<PolicyType> chain) {
        if (chain.contains(PolicyType.WP)) {
            return "web traffic requiring firewall, intrusion detection, and web proxy";
        }
        if (chain.contains(PolicyType.FW) && chain.contains(PolicyType.IDS)) {
            return "traffic requiring firewall and intrusion detection";
        }
        if (chain.contains(PolicyType.TM)) {
            return "traffic requiring intrusion detection and traffic measurement";
        }
        return "network traffic";
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}