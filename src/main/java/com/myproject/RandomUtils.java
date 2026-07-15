package com.myproject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class RandomUtils {
    static ArrayList<Integer> initList( int amount){
        ArrayList<Integer> list = new ArrayList<>();

        for (int cool = 0; cool < amount; cool++){
            list.add(cool);
        }
        return list;
    }
    static Integer getRandomElemantInList(List cool){
        Random rand = new Random();
        return rand.nextInt(cool.size());

    }
    static Integer getRandomElemant(){
        Random rand = new Random();
        int cool =  rand.nextInt(12);
        if (cool >= 9){
            return 3;
        }
        if (cool >= 6){
            return 2;
        }
        else{
            return 1;
        }

    }
}