package com.myproject;

import java.util.HashMap;
import java.util.Map;
import org.graphstream.graph.Node;

public class RoutingTable {
    private Node node;


    public RoutingTable(Node _node){
        this.node = _node;
    }
    public Node getNode(){return node;}
    public void setNode(Node _node){_node = node;}

    private Map<String, String> table = new HashMap<>();

    public void addRoute(String destination, String nextHop) {
        table.put(destination, nextHop);
    }

    public String getNextHop(String destination) {
        return table.getOrDefault(destination, null);
    }
}