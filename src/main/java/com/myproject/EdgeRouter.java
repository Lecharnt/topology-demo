package com.myproject;

import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.graphstream.graph.Path;
public class EdgeRouter {

    private Node node;
    private HashMap<String, Flow> flows;

    private List<Path> FWIdsWpPaths = new ArrayList<>();
    private List<Path> FwIdsPaths = new ArrayList<>();
    private List<Path> IdsTmPaths = new ArrayList<>();
    private final Map<PathType, double[]> pathWeights = new EnumMap<>(PathType.class);

    private final Random random = new Random();

    public List<Path> getFWIdsWpPaths() {
        return FWIdsWpPaths;
    }

    public void setFWIdsWpPaths(List<Path> FWIdsWpPaths) {
        this.FWIdsWpPaths = FWIdsWpPaths;
    }

    public void addFWIdsWpPath(Path path) {
        FWIdsWpPaths.add(path);
    }

    public Path getRandomFWIdsWpPath() {
        if (FWIdsWpPaths.isEmpty()) {
            return null;
        }
        return FWIdsWpPaths.get(random.nextInt(FWIdsWpPaths.size()));
    }

    public List<Path> getFwIdsPaths() {
        return FwIdsPaths;
    }

    public void setFwIdsPaths(List<Path> FwIdsPaths) {
        this.FwIdsPaths = FwIdsPaths;
    }

    public void addFwIdsPath(Path path) {
        FwIdsPaths.add(path);
    }

    public Path getRandomFwIdsPath() {
        if (FwIdsPaths.isEmpty()) {
            return null;
        }
        return FwIdsPaths.get(random.nextInt(FwIdsPaths.size()));
    }

    public List<Path> getIdsTmPaths() {
        return IdsTmPaths;
    }

    public void setIdsTmPaths(List<Path> IdsTmPaths) {
        this.IdsTmPaths = IdsTmPaths;
    }

    public void addIdsTmPath(Path path) {
        IdsTmPaths.add(path);
    }

    public void addPath(PathType policy, Path path) {
        if (path == null) {
            return;
        }
        switch (policy) {
            case FW_IDS_WP -> FWIdsWpPaths.add(path);
            case FW_IDS -> FwIdsPaths.add(path);
            case IDS_TM -> IdsTmPaths.add(path);
        }
    }

    public Path getRandomIdsTmPath() {
        if (IdsTmPaths.isEmpty()) {
            return null;
        }
        return IdsTmPaths.get(random.nextInt(IdsTmPaths.size()));
    }

    public void setPathWeights(PathType policy, double[] weights) {
        pathWeights.put(policy, weights);
    }

    public double[] getPathWeights(PathType policy) {
        return pathWeights.get(policy);
    }

    public List<Path> getPathsForPolicy(PathType policy) {
        return switch (policy) {
            case FW_IDS_WP -> FWIdsWpPaths;
            case FW_IDS -> FwIdsPaths;
            case IDS_TM -> IdsTmPaths;
        };
    }

    public Path getRandomPath(List<PolicyType> policies) {
        PathType policyType = PathType.fromFlowPolicy(policies);
        if (policyType == null) {
            return null;
        }
        List<Path> paths = getPathsForPolicy(policyType);
        if (paths.isEmpty()) {
            return null;
        }
        return paths.get(random.nextInt(paths.size()));
    }

    public Path getWeightedPath(List<PolicyType> policies, String flowId) {
        PathType policyType = PathType.fromFlowPolicy(policies);
        if (policyType == null) {
            return null;
        }

        List<Path> paths = getPathsForPolicy(policyType);
        if (paths.isEmpty()) {
            return null;
        }

        double[] weights = pathWeights.get(policyType);
        if (weights == null || weights.length != paths.size()) {
            return getRandomPath(policies);
        }

        double hash = (flowId.hashCode() & 0xFFFFFFFFL) / (double) 0x100000000L;
        double cumulative = 0.0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (hash <= cumulative) {
                return paths.get(i);
            }
        }
        return paths.get(paths.size() - 1);
    }
    
    public EdgeRouter(Node node) {
        this.node = node;
        this.flows = new HashMap<>();
    }


    public Integer getTotFlow() {
        return flows.size();
    }

    public HashMap<String, Flow> getFlows() {
        return flows;
    }

    public Flow getFlow(String ip) {
        return flows.get(ip);
    }

    public void setFlows(HashMap<String, Flow> flows) {
        this.flows = flows;
    }

    public void addFlow(String ip, Flow flow) {
        flows.put(ip, flow);
    }

    public void removeFlow(String ip) {
        flows.remove(ip);
    }

    public void clearFlows() {
        flows.clear();
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}