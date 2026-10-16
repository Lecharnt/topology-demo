package com.myproject;

import com.google.gson.Gson;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrainingDataExportercopy {

    private static final String SYSTEM_PROMPT =
        "You are a network load-balancing assistant for a hybrid SDN-label " +
        "switching system. Minimize the maximum load factor lambda across all " +
        "middleboxes subject to: (1) the path weights sum to 1, " +
        "(2) for each middlebox m, current_load[m] + sum(weight_p * traffic) " +
        "must not exceed capacity[m] * lambda, (3) all weights are >= 0. " +
        "If no assignment satisfies (2) for any lambda <= 1, set " +
        "\"feasible\": false and output the best-effort assignment that " +
        "minimizes the maximum overflow ratio max_m (post_load[m] / capacity[m]). " +
        "Output only the JSON object. Only use middleboxes and paths listed in " +
        "the input.";

    private static final Gson GSON = new Gson();

    private static final List<List<PolicyType>> CHAINS = List.of(
            List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP),
            List.of(PolicyType.FW, PolicyType.IDS),
            List.of(PolicyType.IDS, PolicyType.TM)
    );

    /**
     * Writes a training JSONL containing (system, user, assistant) triples.
     */
    public static void export(
            Map<String, EdgeRouter> edgeRouters,
            Graph graph,
            OptimalLP.Result result,
            String stateName,
            boolean append) {

        String folderPath = "training_data" + File.separator + stateName;
        File folder = new File(folderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            System.err.println("Could not create folder: " + folderPath);
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
                .format(new Date());
        File outputFile = new File(folder, "training_data_" + timestamp + ".jsonl");

        int written = 0;
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(outputFile, false))) {

            for (EdgeRouter er : edgeRouters.values()) {
                for (List<PolicyType> chain : CHAINS) {
                    String example = buildExample(er, chain, result);
                    if (example == null) continue;

                    writer.write(example);
                    writer.newLine();
                    written++;
                }
            }
            System.out.println("Training data saved to: " + outputFile.getPath()
                    + " (" + written + " examples)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // Per-example construction
    // ------------------------------------------------------------------
    private static String buildExample(EdgeRouter er, List<PolicyType> chain,
                                       OptimalLP.Result result) {

        String poolKey = OptimalLP.poolKey(er.getNode().getId(), chain);
        Map<Path, Double> pathTraffic = (result == null)
                ? null : result.pathTraffic.get(poolKey);
        if (pathTraffic == null || pathTraffic.isEmpty()) return null;

        double demand = 0.0;
        for (double v : pathTraffic.values()) demand += v;
        if (demand <= 0) return null;

        boolean feasible = result != null && result.feasible && result.lambda <= 1.0;

        // ---- alternative_paths with stable IDs ----------------------
        List<Map<String, Object>> altPaths = new ArrayList<>();
        List<Path> orderedPaths = new ArrayList<>(pathTraffic.keySet());
        Map<Path, String> pathToId = new LinkedHashMap<>();
        Map<String, List<String>> pathToMbs = new LinkedHashMap<>();
        Set<String> mbIdsUsed = new LinkedHashSet<>();

        int h = 1;
        for (Path path : orderedPaths) {
            String pathId = "h" + h++;
            pathToId.put(path, pathId);
            List<String> mbIds = new ArrayList<>();
            Set<String> seenMb = new HashSet<>();
            for (Node node : path.getNodePath()) {
                String nid = node.getId();
                if (isMiddlebox(nid)) {
                    if (!seenMb.add(nid)) {
                        System.err.println("WARNING: path " + pathId
                                + " for pool " + poolKey
                                + " contains duplicate middlebox " + nid);
                        continue;
                    }
                    mbIds.add(nid);
                    mbIdsUsed.add(nid);
                }
            }
            pathToMbs.put(pathId, mbIds);
            Map<String, Object> altPath = new LinkedHashMap<>();
            altPath.put("id", pathId);
            altPath.put("middleboxes", mbIds);
            altPaths.add(altPath);
        }

        // ---- pool load per middlebox --------------------------------
        Map<String, Double> poolLoadOnMb = new HashMap<>();
        for (Map.Entry<Path, Double> e : pathTraffic.entrySet()) {
            String pid = pathToId.get(e.getKey());
            for (String mb : pathToMbs.get(pid)) {
                poolLoadOnMb.merge(mb, e.getValue(), Double::sum);
            }
        }

        // ---- capacity / current_load --------------------------------
        Map<String, Integer> capMap = new LinkedHashMap<>();
        Map<String, Double> loadMap = new LinkedHashMap<>();
        Map<String, Double> remainingMap = new LinkedHashMap<>();
        Map<String, Double> headroomRatioMap = new LinkedHashMap<>();

        for (String mbId : mbIdsUsed) {
            int cap = capacityFor(mbId);
            capMap.put(mbId, cap);

            double totalLoad = (result != null)
                    ? result.load.getOrDefault(mbId, 0.0) : 0.0;
            double background = Math.max(0.0,
                    totalLoad - poolLoadOnMb.getOrDefault(mbId, 0.0));
            loadMap.put(mbId, background);

            double remaining = Math.max(0.0, cap - background);
            remainingMap.put(mbId, remaining);
            headroomRatioMap.put(mbId, cap > 0 ? remaining / cap : 0.0);
        }

        // ---- derived features ---------------------------------------
        Map<String, Object> pathBottleneck = new LinkedHashMap<>();
        double minRatio = Double.POSITIVE_INFINITY;
        double maxRatio = Double.NEGATIVE_INFINITY;
        double totalBottleneckHeadroom = 0.0;

        for (String pathId : pathToMbs.keySet()) {
            List<String> mbs = pathToMbs.get(pathId);
            if (mbs.isEmpty()) continue;

            String worstMb = null;
            double worstHeadroom = Double.POSITIVE_INFINITY;
            double worstRatio = Double.POSITIVE_INFINITY;

            for (String mb : mbs) {
                Double remObj = remainingMap.get(mb);
                Double ratioObj = headroomRatioMap.get(mb);
                if (remObj == null || ratioObj == null) continue;
                double rem = remObj;
                double ratio = ratioObj;
                if (!Double.isFinite(rem) || !Double.isFinite(ratio)) continue;
                if (rem < worstHeadroom) {
                    worstHeadroom = rem;
                    worstRatio = ratio;
                    worstMb = mb;
                }
            }

            if (worstMb == null) continue;

            Map<String, Object> bn = new LinkedHashMap<>();
            bn.put("middlebox", worstMb);
            bn.put("headroom", worstHeadroom);
            bn.put("headroom_ratio", worstRatio);
            pathBottleneck.put(pathId, bn);

            minRatio = Math.min(minRatio, worstRatio);
            maxRatio = Math.max(maxRatio, worstRatio);
            totalBottleneckHeadroom += worstHeadroom;
        }

        Map<String, Object> derived = new LinkedHashMap<>();
        derived.put("remaining_capacity", remainingMap);
        derived.put("headroom_ratio", headroomRatioMap);
        derived.put("path_bottleneck", pathBottleneck);
        derived.put("bottleneck_headroom_ratio_min",
                Double.isFinite(minRatio) ? minRatio : null);
        derived.put("bottleneck_headroom_ratio_max",
                Double.isFinite(maxRatio) ? maxRatio : null);
        derived.put("total_bottleneck_headroom",
                Double.isFinite(totalBottleneckHeadroom)
                        ? totalBottleneckHeadroom : null);
        derived.put("traffic_to_bottleneck_ratio",
                (Double.isFinite(totalBottleneckHeadroom)
                        && totalBottleneckHeadroom > 0)
                        ? demand / totalBottleneckHeadroom
                        : null);

        // ---- assignments (include zero-weight paths, no rounding) ---
        List<Map<String, Object>> assignments = new ArrayList<>();
        Map<String, Double> postLoad = new LinkedHashMap<>();
        for (String mbId : mbIdsUsed) postLoad.put(mbId, loadMap.get(mbId));

        for (Path path : orderedPaths) {
            String pathId = pathToId.get(path);
            double tVal = pathTraffic.get(path);
            double w = tVal / demand;

            Map<String, Object> assignment = new LinkedHashMap<>();
            assignment.put("path_id", pathId);
            assignment.put("weight", w);
            assignments.add(assignment);

            for (String mb : pathToMbs.get(pathId)) {
                postLoad.merge(mb, tVal, Double::sum);
            }
        }

        // ---- lambda + bottleneck (computed, not copied) -------------
        double computedLambda = 0.0;
        String maxLoadMb = null;
        for (Map.Entry<String, Double> e : postLoad.entrySet()) {
            int cap = capMap.get(e.getKey());
            if (cap <= 0) continue;
            double factor = e.getValue() / cap;
            if (factor > computedLambda) {
                computedLambda = factor;
                maxLoadMb = e.getKey();
            }
        }

        // ---- assemble -----------------------------------------------
        Map<String, Object> userObj = new LinkedHashMap<>();
        userObj.put("edge_switch", er.getNode().getId());

        Map<String, Object> policyObj = new LinkedHashMap<>();
        policyObj.put("traffic_descriptor", descriptorFor(chain));
        List<String> actionList = new ArrayList<>();
        for (PolicyType pt : chain) actionList.add(pt.name());
        policyObj.put("action_list", actionList);
        userObj.put("policy", policyObj);

        userObj.put("alternative_paths", altPaths);
        userObj.put("middlebox_capacity", capMap);
        userObj.put("current_load", loadMap);
        userObj.put("traffic_volume", (int) Math.round(demand));
        userObj.put("derived", derived);

        Map<String, Object> assistantObj = new LinkedHashMap<>();
        assistantObj.put("assignments", assignments);
        assistantObj.put("feasible", feasible);
        assistantObj.put("lambda", computedLambda);
        assistantObj.put("max_load_mb", maxLoadMb);
        assistantObj.put("post_assignment_load", postLoad);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(msg("system", SYSTEM_PROMPT));
        messages.add(msg("user", GSON.toJson(userObj)));
        messages.add(msg("assistant", GSON.toJson(assistantObj)));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("messages", messages);
        return GSON.toJson(root);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private static Map<String, String> msg(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static boolean isMiddlebox(String nodeId) {
        return nodeId.startsWith("FW") || nodeId.startsWith("IDS")
                || nodeId.startsWith("WP") || nodeId.startsWith("TM");
    }

    private static int capacityFor(String mbId) {
        if (mbId.startsWith("FW"))  return OptimalLP.FW_CAP;
        if (mbId.startsWith("IDS")) return OptimalLP.IDS_CAP;
        if (mbId.startsWith("WP"))  return OptimalLP.WP_CAP;
        if (mbId.startsWith("TM"))  return OptimalLP.TM_CAP;
        return 0;
    }

    private static String descriptorFor(List<PolicyType> chain) {
        List<String> names = new ArrayList<>();
        for (PolicyType pt : chain) names.add(humanName(pt));
        return String.join(", ", names) + " enforcement";
    }

    private static String humanName(PolicyType pt) {
        switch (pt) {
            case FW:  return "firewall";
            case IDS: return "intrusion detection";
            case WP:  return "web proxy";
            case TM:  return "traffic measurement";
            default:  return pt.name();
        }
    }
}