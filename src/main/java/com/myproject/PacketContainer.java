package com.myproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.graphstream.graph.Node;

public class PacketContainer {
    public List<Node> FWListR = new ArrayList<>();
    public List<Node> IDSListR = new ArrayList<>();
    public List<Node> WPListR = new ArrayList<>();
    public List<Node> TMListR = new ArrayList<>();
    
    public PacketContainer(List<Node> map1_, List<Node> map2_, List<Node> map3_, List<Node> map4_){
        FWListR = map1_;
        IDSListR = map2_;
        WPListR = map3_;
        TMListR = map4_;

    }
    public List<Node> GetMap1(){
        return FWListR;
    }
    public List<Node> GetMap2(){
        return IDSListR;
    }
    public List<Node> GetMap3(){
        return WPListR;
    }
    public List<Node> GetMap4(){
        return TMListR;
    }
}