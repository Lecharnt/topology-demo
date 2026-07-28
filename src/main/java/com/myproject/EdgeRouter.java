package com.myproject;

import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


import javax.print.DocFlavor.STRING;

import org.graphstream.graph.Path;
public class EdgeRouter {

    private Node node;
    private HashMap<String, Flow> flows;

    private int polcy1;
    private int polcy2;
    private int polcy3;

    private List<Path> FWIdsWpPaths = new ArrayList<>();
    private List<Path> FwIdsPaths = new ArrayList<>();
    private List<Path> IdsTmPaths = new ArrayList<>();

    private HashMap<Path, Integer> FWIdsWpPathsTraffic = new HashMap<>();
    private HashMap<Path, Integer> FwIdsPathsTraffic = new HashMap<>();
    private HashMap<Path, Integer> IdsTmPathsTraffic = new HashMap<>();

    private final Random random = new Random();

    public int getAmountFWIdsWp(){
        return FWIdsWpPaths.size();
    }
    public int getAmountFwIds(){
        return FwIdsPaths.size();
    }
    public int getAmountIdsTm(){
        return IdsTmPaths.size();
    }

    public Path getPacketsOnPoliciy(List<PolicyType> policies) {
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
    public int getTotPackets() {
        return polcy1 + polcy2 + polcy3;
    }
    public void setFlows(HashMap<String, Flow> flows) {
        this.flows = flows;
        polcy1 = 0;
        polcy2 = 0;
        polcy3 = 0;

        for (Map.Entry<String, Flow> entry : flows.entrySet()){
            if (entry.getValue().getFlowPolicy().equals(List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP))) {
                polcy1+= entry.getValue().getPakets();
            }

            else if (entry.getValue().equals(List.of(PolicyType.FW, PolicyType.IDS))) {
                polcy2+= entry.getValue().getPakets();
            }

            else if (entry.getValue().equals(List.of(PolicyType.IDS, PolicyType.TM))) {
                polcy3+= entry.getValue().getPakets();
            }
            else{
                System.err.println("aaasafadadfs");
            }
        }
    }

    public void addFlow(String ip, Flow flow) {
        flows.put(ip, flow);
        if (flow.getFlowPolicy().equals(List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP))) {
                polcy1 += flow.getPakets();
        }

        else if (flow.getFlowPolicy().equals(List.of(PolicyType.FW, PolicyType.IDS))) {
            polcy2 += flow.getPakets();
        }

        else if (flow.getFlowPolicy().equals(List.of(PolicyType.IDS, PolicyType.TM))) {
            polcy3 += flow.getPakets();
        }
        else{
            System.err.println("aaasafadadfs");
        }
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
    public Path addTrafficToRandomFWIdsWpPath(int trafficAmount) {
        if (FWIdsWpPaths.isEmpty()) {
            return null;
        }
        Path path = FWIdsWpPaths.get(random.nextInt(FWIdsWpPaths.size()));
        FWIdsWpPathsTraffic.merge(path, trafficAmount, Integer::sum);
        return path;
    }

    public Path addTrafficToRandomFwIdsPath(int trafficAmount) {
        if (FwIdsPaths.isEmpty()) {
            return null;
        }
        Path path = FwIdsPaths.get(random.nextInt(FwIdsPaths.size()));
        FwIdsPathsTraffic.merge(path, trafficAmount, Integer::sum);
        return path;
    }

    public Path addTrafficToRandomIdsTmPath(int trafficAmount) {
        if (IdsTmPaths.isEmpty()) {
            return null;
        }
        Path path = IdsTmPaths.get(random.nextInt(IdsTmPaths.size()));
        IdsTmPathsTraffic.merge(path, trafficAmount, Integer::sum);
        return path;
    }
    public Path addTrafficToRandomPath(List<PolicyType> policies, int trafficAmount) {
        if (policies.equals(List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP))) {
            return addTrafficToRandomFWIdsWpPath(trafficAmount);
        }
        if (policies.equals(List.of(PolicyType.FW, PolicyType.IDS))) {
            return addTrafficToRandomFwIdsPath(trafficAmount);
        }
        if (policies.equals(List.of(PolicyType.IDS, PolicyType.TM))) {
            return addTrafficToRandomIdsTmPath(trafficAmount);
        }
        return null;
    }
    public HashMap<Path, Integer> getFWIdsWpPathsTraffic() {
        return FWIdsWpPathsTraffic;
    }

    public HashMap<Path, Integer> getFwIdsPathsTraffic() {
        return FwIdsPathsTraffic;
    }

    public HashMap<Path, Integer> getIdsTmPathsTraffic() {
        return IdsTmPathsTraffic;
    }
}