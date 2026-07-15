package com.myproject;

import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.print.DocFlavor.STRING;

import org.graphstream.graph.Path;
public class EdgeRouter {

    private Node node;
    private HashMap<String, Flow> flows;

    private List<Path> FWIdsWpPaths = new ArrayList<>();
    private List<Path> FwIdsPaths = new ArrayList<>();
    private List<Path> IdsTmPaths = new ArrayList<>();

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

    public Path getRandomIdsTmPath() {
        if (IdsTmPaths.isEmpty()) {
            return null;
        }
        return IdsTmPaths.get(random.nextInt(IdsTmPaths.size()));
    }

    public Path getRandomPath(List<PolicyType> policies) {
        if (policies.equals(List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP))) {
            if (FWIdsWpPaths.isEmpty()) return null;
            return FWIdsWpPaths.get(random.nextInt(FWIdsWpPaths.size()));
        }

        if (policies.equals(List.of(PolicyType.FW, PolicyType.IDS))) {
            if (FwIdsPaths.isEmpty()) return null;
            return FwIdsPaths.get(random.nextInt(FwIdsPaths.size()));
        }

        if (policies.equals(List.of(PolicyType.IDS, PolicyType.TM))) {
            if (IdsTmPaths.isEmpty()) return null;
            return IdsTmPaths.get(random.nextInt(IdsTmPaths.size()));
        }

        return null;
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