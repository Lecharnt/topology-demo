package com.myproject;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathConverter {

    public static List<String> pathToIds(Path path) {
        List<String> ids = new ArrayList<>();
        if (path == null) {
            return ids;
        }
        for (Node n : path.getNodePath()) {
            ids.add(n.getId());
        }
        return ids;
    }
    public static Path idsToPath(List<String> ids, Graph graph) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        Node root = graph.getNode(ids.get(0));
        if (root == null) {
            return null;
        }

        Path path = new Path();
        path.setRoot(root);

        Node current = root;
        for (int i = 1; i < ids.size(); i++) {
            Node next = graph.getNode(ids.get(i));
            if (next == null) {
                return null;
            }
            Edge edge = current.getEdgeBetween(next);
            if (edge == null) {
                return null;
            }
            path.add(current, edge);
            current = next;
        }

        return path;
    }
    public static List<List<String>> pathsToIds(List<Path> paths) {
        List<List<String>> saved = new ArrayList<>();
        if (paths == null) {
            return saved;
        }
        for (Path p : paths) {
            saved.add(pathToIds(p));
        }
        return saved;
    }

    public static List<Path> idsToPaths(List<List<String>> saved, Graph graph) {
        List<Path> paths = new ArrayList<>();
        if (saved == null) {
            return paths;
        }
        for (List<String> ids : saved) {
            Path p = idsToPath(ids, graph);
            if (p != null) {
                paths.add(p);
            }
        }
        return paths;
    }
    public static HashMap<List<String>, Integer> trafficToIds(HashMap<Path, Integer> traffic) {
        HashMap<List<String>, Integer> saved = new HashMap<>();
        if (traffic == null) {
            return saved;
        }
        for (Map.Entry<Path, Integer> entry : traffic.entrySet()) {
            saved.put(pathToIds(entry.getKey()), entry.getValue());
        }
        return saved;
    }

    public static HashMap<Path, Integer> idsToTraffic(HashMap<List<String>, Integer> saved, Graph graph) {
        HashMap<Path, Integer> traffic = new HashMap<>();
        if (saved == null) {
            return traffic;
        }
        for (Map.Entry<List<String>, Integer> entry : saved.entrySet()) {
            Path p = idsToPath(entry.getKey(), graph);
            if (p != null) {
                traffic.put(p, entry.getValue());
            }
        }
        return traffic;
    }
}