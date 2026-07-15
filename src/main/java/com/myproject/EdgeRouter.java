package com.myproject;

import org.graphstream.graph.Node;
import java.util.HashMap;
import org.graphstream.graph.Path;
public class EdgeRouter {

    private Node node;
    private HashMap<String, Flow> flows;


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