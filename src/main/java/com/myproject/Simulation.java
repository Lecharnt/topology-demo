package com.myproject;

import java.util.HashMap;

public class Simulation {
    HashMap<String, Integer> FWpackest = new HashMap<>();
    HashMap<String, Integer> IDSpackest = new HashMap<>();
    HashMap<String, Integer> TMpackest = new HashMap<>();
    HashMap<String, Integer> WPpackest = new HashMap<>();
    public Simulation(HashMap<String, Integer> FWpackest, HashMap<String, Integer> IDSpackest, HashMap<String, Integer> TMpackest, HashMap<String, Integer> WPpackest) {
        this.FWpackest = FWpackest;
        this.IDSpackest = IDSpackest;
        this.TMpackest = TMpackest;
        this.WPpackest = WPpackest;
    }
}
