package com.myproject;

import java.util.List;

import org.graphstream.graph.Node;


public class EnforcementPath {
    private List<Node> middleboxes;
    private org.graphstream.graph.Path path;

    public EnforcementPath(List<Node> middleboxes,
                           org.graphstream.graph.Path path) {
        this.middleboxes = middleboxes;
        this.path = path;
    }

    public List<Node> getMiddleboxes() {
        return middleboxes;
    }

    public org.graphstream.graph.Path getPath() {
        return path;
    }
}