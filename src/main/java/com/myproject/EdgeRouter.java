package com.myproject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

public class EdgeRouter implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private transient Node node;
    private String nodeId;
    private HashMap<String, Flow> flows = new HashMap<>();
    
    // Live path lists 
    private transient List<Path> fwIdsWpPaths = new ArrayList<>();
    private transient List<Path> fwIdsPaths = new ArrayList<>();
    private transient List<Path> idsTmPaths = new ArrayList<>();
    
    // Live traffic maps 
    private transient HashMap<Path, Integer> fwIdsWpTraffic = new HashMap<>();
    private transient HashMap<Path, Integer> fwIdsTraffic = new HashMap<>();
    private transient HashMap<Path, Integer> idsTmTraffic = new HashMap<>();
    
    // Serializable versions of the paths
    private List<List<String>> savedFwIdsWpPaths = new ArrayList<>();
    private List<List<String>> savedFwIdsPaths = new ArrayList<>();
    private List<List<String>> savedIdsTmPaths = new ArrayList<>();
    
    // Serializable versions of traffic 
    private HashMap<List<String>, Integer> savedFwIdsWpTraffic = new HashMap<>();
    private HashMap<List<String>, Integer> savedFwIdsTraffic = new HashMap<>();
    private HashMap<List<String>, Integer> savedIdsTmTraffic = new HashMap<>();
    
    private int totPackets = 0;
    
    public EdgeRouter(Node node) {
        this.node = node;
        this.nodeId = node.getId();
    }
    
    // ===== Getters =====
    public Node getNode() { return node; }
    public String getNodeId() { return nodeId; }
    public int getTotPackets() { return totPackets; }
    public void setTotPackets(int totPackets) { this.totPackets = totPackets; }
    
    public HashMap<String, Flow> getFlows() { return flows; }
    public void setFlows(HashMap<String, Flow> flows) { this.flows = flows; }
    
    // Path getters
    public List<Path> getFwIdsWpPaths() { return fwIdsWpPaths; }
    public List<Path> getFWIdsWpPaths() { return fwIdsWpPaths; } // For compatibility
    
    public List<Path> getFwIdsPaths() { return fwIdsPaths; }
    public List<Path> getFWIdsPaths() { return fwIdsPaths; } // For compatibility
    
    public List<Path> getIdsTmPaths() { return idsTmPaths; }
    public List<Path> getIDSTmPaths() { return idsTmPaths; } // For compatibility
    
    // Traffic getters
    public HashMap<Path, Integer> getFwIdsWpTraffic() { return fwIdsWpTraffic; }
    public HashMap<Path, Integer> getFWIdsWpPathsTraffic() { return fwIdsWpTraffic; } // For compatibility
    
    public HashMap<Path, Integer> getFwIdsTraffic() { return fwIdsTraffic; }
    public HashMap<Path, Integer> getFwIdsPathsTraffic() { return fwIdsTraffic; } // For compatibility
    
    public HashMap<Path, Integer> getIdsTmTraffic() { return idsTmTraffic; }
    public HashMap<Path, Integer> getIdsTmPathsTraffic() { return idsTmTraffic; } // For compatibility
    
    // Saved getters
    public HashMap<List<String>, Integer> getSavedFwIdsWpTraffic() { return savedFwIdsWpTraffic; }
    public HashMap<List<String>, Integer> getSavedFwIdsTraffic() { return savedFwIdsTraffic; }
    public HashMap<List<String>, Integer> getSavedIdsTmTraffic() { return savedIdsTmTraffic; }
    
    public List<List<String>> getSavedFwIdsWpPaths() { return savedFwIdsWpPaths; }
    public List<List<String>> getSavedFwIdsPaths() { return savedFwIdsPaths; }
    public List<List<String>> getSavedIdsTmPaths() { return savedIdsTmPaths; }
    
    // Count methods
    public int getAmountFWIdsWp() {
        return fwIdsWpPaths != null ? fwIdsWpPaths.size() : 0;
    }
    
    public int getAmountFWIds() {
        return fwIdsPaths != null ? fwIdsPaths.size() : 0;
    }
    
    public int getAmountIDSTm() {
        return idsTmPaths != null ? idsTmPaths.size() : 0;
    }
    
    // method
    public void addFlow(String ip, Flow flow) {
        flows.put(ip, flow);
    }
    
    public void addFWIdsWpPath(Path path) {
        if (path != null) {
            fwIdsWpPaths.add(path);
        }
    }
    
    public void addFwIdsPath(Path path) {
        if (path != null) {
            fwIdsPaths.add(path);
        }
    }
    
    public void addIdsTmPath(Path path) {
        if (path != null) {
            idsTmPaths.add(path);
        }
    }
    
    public Path addTrafficToRandomPath(List<PolicyType> policyTypes, int packets) {
        List<Path> availablePaths = new ArrayList<>();
        HashMap<Path, Integer> trafficMap = null;
        
        // Determine which path list to use based on policy types
        if (policyTypes != null && policyTypes.size() >= 2) {
            if (policyTypes.contains(PolicyType.FW) && 
                policyTypes.contains(PolicyType.IDS) && 
                policyTypes.contains(PolicyType.WP)) {
                availablePaths = fwIdsWpPaths;
                trafficMap = fwIdsWpTraffic;
            } else if (policyTypes.contains(PolicyType.FW) && 
                       policyTypes.contains(PolicyType.IDS)) {
                availablePaths = fwIdsPaths;
                trafficMap = fwIdsTraffic;
            } else if (policyTypes.contains(PolicyType.IDS) && 
                       policyTypes.contains(PolicyType.TM)) {
                availablePaths = idsTmPaths;
                trafficMap = idsTmTraffic;
            }
        }
        
        if (availablePaths == null || availablePaths.isEmpty()) {
            return null;
        }
        
        Random rand = new Random();
        Path selectedPath = availablePaths.get(rand.nextInt(availablePaths.size()));
        
        // Update traffic
        if (trafficMap != null) {
            trafficMap.put(selectedPath, trafficMap.getOrDefault(selectedPath, 0) + packets);
            totPackets += packets;
        }
        
        return selectedPath;
    }
    
    public Path getTotRandomPath(Random rand) {
        List<Path> allPaths = new ArrayList<>();
        allPaths.addAll(fwIdsWpPaths);
        allPaths.addAll(fwIdsPaths);
        allPaths.addAll(idsTmPaths);
        
        if (allPaths.isEmpty()) {
            return null;
        }
        
        return allPaths.get(rand.nextInt(allPaths.size()));
    }
    

    //Converts live Path Node to serializable form.
    public void toSaved() {
        // Convert FW-IDS-WP paths
        savedFwIdsWpPaths = new ArrayList<>();
        for (Path p : fwIdsWpPaths) {
            savedFwIdsWpPaths.add(PathConverter.pathToIds(p));
        }
        
        // Convert FW-IDS paths
        savedFwIdsPaths = new ArrayList<>();
        for (Path p : fwIdsPaths) {
            savedFwIdsPaths.add(PathConverter.pathToIds(p));
        }
        
        // Convert IDS-TM paths
        savedIdsTmPaths = new ArrayList<>();
        for (Path p : idsTmPaths) {
            savedIdsTmPaths.add(PathConverter.pathToIds(p));
        }
        
        // Convert traffic maps
        savedFwIdsWpTraffic = PathConverter.trafficToIds(fwIdsWpTraffic);
        savedFwIdsTraffic = PathConverter.trafficToIds(fwIdsTraffic);
        savedIdsTmTraffic = PathConverter.trafficToIds(idsTmTraffic);
    }
    
    //Restores live Path Node references from saved lists

    public void fromSaved(Graph graph) {
        this.node = graph.getNode(nodeId);
        if (this.node == null) {
            System.err.println("Warning: Could not resolve node " + nodeId + " when restoring EdgeRouter");
        }

        // Restore FW-IDS-WP paths
        fwIdsWpPaths = new ArrayList<>();
        for (List<String> pathIds : savedFwIdsWpPaths) {
            Path p = PathConverter.idsToPath(pathIds, graph);
            if (p != null) {
                fwIdsWpPaths.add(p);
            } else {
                System.err.println("Warning: Could not restore FW-IDS-WP path for " + nodeId);
            }
        }
        
        // Restore FW-IDS paths
        fwIdsPaths = new ArrayList<>();
        for (List<String> pathIds : savedFwIdsPaths) {
            Path p = PathConverter.idsToPath(pathIds, graph);
            if (p != null) {
                fwIdsPaths.add(p);
            } else {
                System.err.println("Warning: Could not restore FW-IDS path for " + nodeId);
            }
        }
        
        // Restore IDS-TM paths
        idsTmPaths = new ArrayList<>();
        for (List<String> pathIds : savedIdsTmPaths) {
            Path p = PathConverter.idsToPath(pathIds, graph);
            if (p != null) {
                idsTmPaths.add(p);
            } else {
                System.err.println("Warning: Could not restore IDS-TM path for " + nodeId);
            }
        }
        
        // Restore traffic maps
        fwIdsWpTraffic = PathConverter.idsToTraffic(savedFwIdsWpTraffic, graph);
        fwIdsTraffic = PathConverter.idsToTraffic(savedFwIdsTraffic, graph);
        idsTmTraffic = PathConverter.idsToTraffic(savedIdsTmTraffic, graph);
        
        // Recalculate total packets from traffic
        totPackets = 0;
        for (int traffic : fwIdsWpTraffic.values()) {
            totPackets += traffic;
        }
        for (int traffic : fwIdsTraffic.values()) {
            totPackets += traffic;
        }
        for (int traffic : idsTmTraffic.values()) {
            totPackets += traffic;
        }
    }
}