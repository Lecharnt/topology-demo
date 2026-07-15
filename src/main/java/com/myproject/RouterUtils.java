package com.myproject;

import java.util.HashMap;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

class RouterUtils {
    static HashMap<String, RoutingTable> setRouters(Graph graph){
        HashMap<String, RoutingTable> tables = new HashMap<String, RoutingTable>();
        for (Node node : graph) {
            RoutingTable routingTable = new RoutingTable(node);
            routingTable.setNode(node);
            tables.put(node.getId(), routingTable);
        }
        return tables;
    }
    static HashMap<String, RoutingTable> setRouterEdges(Graph graph, HashMap<String, RoutingTable> tables){
        for (RoutingTable table : tables.values()) {
            for (PolicyType policy : PolicyType.values()) {
                System.out.println(policy);
            }
        }
        return tables;
    }
    static RouterType getRouterType(String id) {
        for (RouterType type : RouterType.values()) {
            if (id.startsWith(type.name())) {
                return type;
            }
        }
        return null;
    }
}