package com.myproject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

class GraphBuilder {
    static Graph addNodes(Graph graph, String name, int amount)
    {
        for (int index = 0; index < amount; index++) {
            graph.addNode(name + index);
        }
        return graph;
    }
    static void addStyledNodes(Graph graph, String prefix, int count, String hexColor) {
        for (int i = 0; i < count; i++) {
            graph.addNode(prefix + i).setAttribute("ui.style","fill-color: " + hexColor + "; size: 15px;");
        }
    }
    static List<Integer> rangeList(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(i);
        return list;
    }
    static void connectMiddleboxes(Graph graph, Random rand,int coreCount,String mbPrefix, int mbCount, String edgeLabelTag) {
        List<Integer> available = rangeList(coreCount);
        for (int mb = 0; mb < mbCount; mb++) {
            int z = rand.nextInt(available.size());
            int cr = available.get(z);
            available.remove(z);
            graph.addEdge("CR" + cr + "|" + edgeLabelTag + mb, "CR" + cr, mbPrefix + mb);
        }
    }
    static Graph createEdge(Node node1, Node node2, Graph graph){
        if (graph.getEdge(getEdgeName(node1, node2)) == null){
            graph.addEdge(getEdgeName(node1, node2), node1, node2);
        }
        return graph;
    }
    static String getEdgeName(Node node1, Node node2){
        String name1 = node1.getId();
        String name2 = node2.getId();

            if (name1.startsWith(name2.replaceAll("\\d", "")) && name2.startsWith(name1.replaceAll("\\d", ""))){
                if (Integer.parseInt(name1.replaceAll("\\D", ""))>= Integer.parseInt(name2.replaceAll("\\D", "")));{
                    return name2 + name1;
                }
            }
            else if (name1.startsWith(PolicyType.FW.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.IDS.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(PolicyType.TM.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.WP.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(RouterType.ER.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(RouterType.CR.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(RouterType.M.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.FW.name())){
                return name2 + name1;
            }


            else if (name1.startsWith(PolicyType.IDS.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(PolicyType.TM.name())){
                return name2 + name1;

            }


            else if (name1.startsWith(PolicyType.WP.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(RouterType.ER.name())){
                return name2 + name1;

            }


            else if (name1.startsWith(RouterType.CR.name())){
                return name1 + name2;
            }
            else if (name2.startsWith(RouterType.M.name())){
                return name2 + name1;
            }
            else
                System.err.println("created bad edge");
                return name1 + name2;
    }
}