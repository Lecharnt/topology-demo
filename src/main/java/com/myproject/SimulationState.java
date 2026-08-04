package com.myproject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class SimulationState implements Serializable {

    private static final long serialVersionUID = 1L;

    List<Flow> flows;
    HashMap<String, EdgeRouter> edgeRouters;

    List<String> nodeIds;

    List<List<String>> edgeNodeIds;

    private transient Graph graph;


    public SimulationState(Graph graph_, List<Flow> flows_, HashMap<String, EdgeRouter> edgeRouters_) {
        flows = flows_;
        edgeRouters = edgeRouters_;
        graph = graph_;

        nodeIds = new ArrayList<>();
        edgeNodeIds = new ArrayList<>();

        if (graph_ != null) {
            for (Node n : graph_) {
                nodeIds.add(n.getId());
            }
            graph_.edges().forEach(e -> {
                List<String> pair = new ArrayList<>();
                pair.add(e.getSourceNode().getId());
                pair.add(e.getTargetNode().getId());
                edgeNodeIds.add(pair);
            });
        }
    }

    public void prepareForSave() {
        if (flows != null) {
            for (Flow flow : flows) {
                flow.toSaved();
            }
        }
        if (edgeRouters == null) {
            return;
        }
        for (EdgeRouter er : edgeRouters.values()) {
            er.toSaved();
        }
    }

    public void rebuildTopology(Graph graph_) {
        if (graph_ == null || edgeNodeIds == null) {
            return;
        }
        int skipped = 0;
        for (List<String> pair : edgeNodeIds) {
            if (pair == null || pair.size() < 2) {
                continue;
            }
            String sourceId = pair.get(0);
            String targetId = pair.get(1);

            Node source = graph_.getNode(sourceId);
            Node target = graph_.getNode(targetId);
            if (source == null || target == null) {
                skipped++;
                continue;
            }
            if (source.getEdgeBetween(target) != null) {
                continue;
            }
            try {
                graph_.addEdge(sourceId + "--" + targetId, sourceId, targetId);
            } catch (Exception e) {
                skipped++;
                System.err.println(
                        "Failed to rebuild edge " + sourceId + " - " + targetId + ": " + e.getMessage()
                );
            }
        }
        if (skipped > 0) {
            System.err.println(
                    "rebuildTopology: " + skipped + " / " + edgeNodeIds.size() +
                    " saved edges could not be rebuilt (missing nodes on the target graph, " +
                    "or a duplicate/rejected edge). This can leave the graph disconnected " +
                    "compared to the one the state was saved from."
            );
        }
    }
    public void restoreAgainstGraph(Graph graph_) {
        this.graph = graph_;
        if (flows != null) {
            for (Flow flow : flows) {
                flow.fromSaved(graph_);
            }
        }
        if (edgeRouters == null) {
            return;
        }
        for (EdgeRouter er : edgeRouters.values()) {
            er.fromSaved(graph_);
        }
    }
    public void attachGraph(Graph graph_) {
        rebuildTopology(graph_);
        restoreAgainstGraph(graph_);
    }

    public Graph getGraph() {
        return graph;
    }
}