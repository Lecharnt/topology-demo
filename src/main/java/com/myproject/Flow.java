package com.myproject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class Flow implements Serializable{

    private transient Node node;
    private String savedNodeId;

    private int pakets;
    private String id;
    private List<PolicyType> flowPolicy;
    private String flowPolicyName;

    private Integer totFlows = 0;

    public Flow(String id_,int pakets_, Node node_) {
        this.id = id_;
        this.pakets = pakets_;
        this.node = node_;
        this.flowPolicy = new ArrayList<PolicyType>();
    }

    //Converts this Flow Node reference into its serializable id
    public void toSaved() {
        savedNodeId = (node != null) ? node.getId() : null;
    }

    // Rebuilds this Flow live Node reference from the saved id
    public void fromSaved(Graph graph) {
        node = (savedNodeId != null) ? graph.getNode(savedNodeId) : null;
    }

    public int getPakets() {
        return pakets;
    }

    public void setPakets(int pakets_) {
        this.pakets = pakets_;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<PolicyType> getFlowPolicy() {
        return flowPolicy;
    }

    public void setFlowPolicy(List<PolicyType> flowPolicy) {
        this.flowPolicy = flowPolicy;
        flowPolicyName = getFlowPolicyName();
    }

    public Integer getTotFlows() {
        return totFlows;
    }

    public void setTotFlows(Integer totFlows) {
        this.totFlows = totFlows;
    }
    public String getFlowPolicyName(){
        String name = "";
        for (PolicyType policyType : flowPolicy) {
            name.concat(policyType.name());
        }
        return name;
    }
}