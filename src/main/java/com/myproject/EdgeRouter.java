package com.myproject;

import org.graphstream.graph.Node;
import java.util.HashMap;

public class EdgeRouter {

    private Node node;
    private HashMap<String, Integer> flows;
    private HashMap<String, String> flowPolicy;

    private Integer totFlows = 0;

    public EdgeRouter(Node node) {
        this.node = node;
        this.flows = new HashMap<>();
        this.flowPolicy = new HashMap<>();
    }


    public Integer getTotFlow() {
        return totFlows;
    }
    public void addTotFlow(int amount) {
        totFlows += amount;
    }
    public void setTotFlow(int amount) {
        totFlows = amount;
    }


    public HashMap<String, String> getPolics() {
        return flowPolicy;
    }

    public String getPolics(String ip) {
        return flowPolicy.get(ip);
    }

    public void setPolics(HashMap<String, String> policy) {
        this.flowPolicy = policy;
    }

    public void addPolics(String policy, String ip) {
        flowPolicy.put(policy, ip);
    }

    public void removePolics(String ip) {
        flowPolicy.remove(ip);
    }


    public HashMap<String, Integer> getFlows() {
        return flows;
    }

    public Integer getFlow(String ip) {
        return flows.get(ip);
    }

    public void setFlows(HashMap<String, Integer> flows) {
        this.flows = flows;
    }

    public void addFlow(String ip, Integer num) {
        flows.put(ip, num);
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