package com.myproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

class PathFinder {

    static List<Node> ERList = new ArrayList<>();
    static List<Node> CRList = new ArrayList<>();
    static List<Node> MList = new ArrayList<>();

    static List<Node> FWList = new ArrayList<>();
    static List<Node> IDSList = new ArrayList<>();
    static List<Node> WPList = new ArrayList<>();
    static List<Node> TMList = new ArrayList<>();

    static List<Node> FWListR = new ArrayList<>();
    static List<Node> IDSListR = new ArrayList<>();
    static List<Node> WPListR = new ArrayList<>();
    static List<Node> TMListR = new ArrayList<>();

    static HashMap<Node, Dijkstra> dijkstraCache = new HashMap<>();

    static Node findNextClosestMB(Node coreNode, PolicyType mbName) {
        if (coreNode == null)
            return null;

        for (Node x : coreNode.neighborNodes().toList()) {
            if (x.getId().startsWith(mbName.name())) {
                return x;
            }
        }

        return null;
    }
    static Node findNextClosestRouter(Node coreNode, RouterType RName) {
        if (coreNode == null)
            return null;

        for (Node x : coreNode.neighborNodes().toList()) {
            if (x.getId().startsWith(RName.name())) {
                return x;
            }
        }

        return null;
    }
    static Node findClosestMB(PolicyType mbType, Node source, int maxHops) {
        if (source == null) {
            return null;
        }

        Dijkstra dijkstra = dijkstraCache.get(source);

        Node closestNode = null;
        double shortestDistance = Double.POSITIVE_INFINITY;

        for (Node candidate : getListForType(mbType)) {

            if (candidate.equals(source))
                continue;

            double distance = dijkstra.getPathLength(candidate);

            if (Double.isInfinite(distance))
                continue;

            if (distance > maxHops)
                continue;

            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestNode = candidate;
            } else if (distance == shortestDistance && ThreadLocalRandom.current().nextBoolean()) {
                closestNode = candidate;
            }
        }

        return closestNode;
    }

    static HashMap<String, Double> findClosestMBList(
            PolicyType mbType, Node source, int maxHops) {

        if (source == null)
            return null;

        HashMap<String, Double> distances = new HashMap<>();

        Dijkstra dijkstra = dijkstraCache.get(source);

        for (Node candidate : getListForType(mbType)) {
            double distance = dijkstra.getPathLength(candidate);

            if (Double.isInfinite(distance))
                continue;

            if (distance > maxHops)
                continue;

            distances.put(candidate.getId(), distance);
        }

        return distances;
    }
    static org.graphstream.graph.Path findClosestMBPath(PolicyType mbType, Node source, int maxHops) {

        if (source == null) {
            return null;
        }

        Dijkstra dijkstra = dijkstraCache.get(source);

        Node closestNode = null;
        double shortestDistance = Double.POSITIVE_INFINITY;

        for (Node candidate : getListForType(mbType)) {

            if (candidate.equals(source))
                continue;

            double distance = dijkstra.getPathLength(candidate);

            if (Double.isInfinite(distance))
                continue;

            if (distance > maxHops)
                continue;

            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestNode = candidate;
            } else if (distance == shortestDistance && ThreadLocalRandom.current().nextBoolean()) {
                closestNode = candidate;
            }
        }

        if (closestNode == null)
            return null;

        return dijkstra.getPath(closestNode);
    }
    static org.graphstream.graph.Path findClosestPathToNode(Node source, Node destination, Graph graph){
        return dijkstraCache.get(source).getPath(destination);
    }
    static Node findClosestMBRandom(PolicyType MBName){
        List<Node> list;
        switch (MBName) {
            case FW:  list = FWListR;  break;
            case IDS: list = IDSListR; break;
            case WP:  list = WPListR;  break;
            case TM:  list = TMListR;  break;
            default:  return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(randomIndex);
    }
    static List<Node> getListForType(PolicyType type) {
        switch (type) {
            case FW:  return FWList;
            case IDS: return IDSList;
            case WP:  return WPList;
            case TM:  return TMList;
            default:  return Collections.emptyList();
        }
    }
    static org.graphstream.graph.Path findGreedyPathThroughMBs(Node startNode, List<PolicyType> middleBoxTypes, Graph graph, int maxHops) {
        List<org.graphstream.graph.Path> segments = new ArrayList<>();
        Node currentNode = startNode;

        for (PolicyType type : middleBoxTypes) {
            Node nearest = findClosestMB(type, currentNode, maxHops);
            if (nearest == null) {
                System.err.println("No reachable " + type + " from " + currentNode.getId());
                return null;
            }
            org.graphstream.graph.Path segment = findClosestPathToNode(currentNode, nearest, graph);
            segments.add(segment);
            currentNode = nearest;
        }

        return mergeSegments(segments);
    }
    static org.graphstream.graph.Path findOptimalPathThroughMBs(Node startNode, List<PolicyType> middleBoxTypes, Graph graph, int maxHops) {
        Map<Node, Double> bestDistance = new HashMap<>();
        Map<Node, Node> predecessor = new HashMap<>();
        bestDistance.put(startNode, 0.0);
        List<Node> currentLayer = new ArrayList<>();
        currentLayer.add(startNode);

        for (PolicyType type : middleBoxTypes) {
            List<Node> candidates = getListForType(type);
            Map<Node, Double> nextBest = new HashMap<>();

            for (Node from : currentLayer) {
                double baseDist = bestDistance.get(from);
                HashMap<String, Double> distances = findClosestMBList(type, from, maxHops);
                if (distances == null) continue;

                for (Node candidate : candidates) {
                    Double d = distances.get(candidate.getId());
                    if (d == null) continue; // unreachable within maxHops

                    double total = baseDist + d;
                    if (!nextBest.containsKey(candidate) || total < nextBest.get(candidate)) {
                        nextBest.put(candidate, total);
                        predecessor.put(candidate, from);
                    }
                }
            }

            if (nextBest.isEmpty()) {
                System.err.println("No reachable " + type + " from previous layer");
                return null;
            }

            bestDistance = nextBest;
            currentLayer = new ArrayList<>(nextBest.keySet());
        }

        // pick the best node in the final layer
        Node bestFinal = null;
        double bestTotal = Double.MAX_VALUE;
        for (Node n : currentLayer) {
            if (bestDistance.get(n) < bestTotal) {
                bestTotal = bestDistance.get(n);
                bestFinal = n;
            }
        }

        // walk predecessors back to build the winning sequence of nodes
        List<Node> sequence = new ArrayList<>();
        Node cur = bestFinal;
        sequence.add(0, cur);
        while (predecessor.containsKey(cur)) {
            cur = predecessor.get(cur);
            sequence.add(0, cur);
        }

        // stitch the Dijkstra segments between each consecutive pair into one path
        List<org.graphstream.graph.Path> segments = new ArrayList<>();
        for (int i = 0; i < sequence.size() - 1; i++) {
            segments.add(findClosestPathToNode(sequence.get(i), sequence.get(i + 1), graph));
        }

        return mergeSegments(segments);
    }
    static org.graphstream.graph.Path mergeSegments(List<org.graphstream.graph.Path> segments) {
        org.graphstream.graph.Path merged = new org.graphstream.graph.Path();
        for (org.graphstream.graph.Path segment : segments) {
            List<Node> nodes = segment.getNodePath();
            List<org.graphstream.graph.Edge> edges = segment.getEdgePath();
            for (int i = 0; i < edges.size(); i++) {
                merged.add(nodes.get(i), edges.get(i));
            }
        }
        return merged;
    }
    static org.graphstream.graph.Path findRandomPathThroughMBs(Node startNode, List<PolicyType> middleBoxTypes, Graph graph) {

        List<org.graphstream.graph.Path> segments = new ArrayList<>();
        Node currentNode = startNode;

        for (PolicyType type : middleBoxTypes) {
            Node randomMB = findClosestMBRandom(type);
            if (randomMB == null) {
                System.err.println("No " + type + " available");
                return null;
            }
            org.graphstream.graph.Path segment = findClosestPathToNode(currentNode, randomMB, graph);
            segments.add(segment);
            currentNode = randomMB;
        }

        return mergeSegments(segments);
    }
}