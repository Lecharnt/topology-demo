package com.myproject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.*;
public class LyonMathTest {

    static final Random rand = new Random();

    // Middlebox types

    enum MBType {
        FW(8, 1_000_000),
        IDS(8, 1_500_000),
        WP(4, 1_000_000),
        TM(4, 1_000_000);

        final int count;
        final int capacityPerBox;

        MBType(int count, int capacityPerBox) {
            this.count = count;
            this.capacityPerBox = capacityPerBox;
        }
    }

    // Global middlebox index layout:
    static int startIndex(MBType type) {
        int idx = 0;
        for (MBType t : MBType.values()) {
            if (t == type) return idx;
            idx += t.count;
        }
        throw new IllegalArgumentException();
    }

    static int totalMiddleboxes() {
        int total = 0;
        for (MBType t : MBType.values()) total += t.count;
        return total;
    }

 
    static class Path {
        Set<Integer> usesMB = new LinkedHashSet<>();
        MPVariable t; // t(h_e,p)
    }

    // A policy p that applies to one edge switch
    static class EdgePolicy {
        int edge;
        int policyType; // 0 or 1 or 2
        double traffic;
        List<Path> paths = new ArrayList<>();
    }

    // sequence and the number of alternative paths it is assigned
    enum PolicyType {
        WEB_TRAFFIC(new MBType[]{MBType.FW, MBType.IDS, MBType.WP}, 32, "FW -> IDS -> WP"),
        SECURITY_WATCH(new MBType[]{MBType.FW, MBType.IDS}, 16, "FW -> IDS"),
        BANDWIDTH_WATCH(new MBType[]{MBType.IDS, MBType.TM}, 8, "IDS -> TM");

        final MBType[] sequence;
        final int altPathCount;
        final String description;

        PolicyType(MBType[] sequence, int altPathCount, String description) {
            this.sequence = sequence;
            this.altPathCount = altPathCount;
            this.description = description;
        }
    }

    // pick half distinct global indices at random from the pool of type
    static List<Integer> randomHalfPool(MBType type) {
        int start = startIndex(type);
        int half = type.count / 2;
        List<Integer> pool = new ArrayList<>();
        for (int i = start; i < start + type.count; i++) pool.add(i);
        java.util.Collections.shuffle(pool, rand);
        return pool.subList(0, half);
    }

    // the cross product of half pools for each function
    static List<Path> buildAlternativePaths(PolicyType policyType) {

        List<List<Integer>> halfPools = new ArrayList<>();
        for (MBType type : policyType.sequence) {
            halfPools.add(randomHalfPool(type));
        }

        List<Path> paths = new ArrayList<>();
        cartesianBuild(halfPools, 0, new ArrayList<>(), paths);
        return paths;
    }

    static void cartesianBuild(List<List<Integer>> pools, int depth,
                                List<Integer> current, List<Path> out) {
        if (depth == pools.size()) {
            Path p = new Path();
            p.usesMB.addAll(current);
            out.add(p);
            return;
        }
        for (int mb : pools.get(depth)) {
            current.add(mb);
            cartesianBuild(pools, depth + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    public static void main(String[] args) {

        Loader.loadNativeLibraries();

        // parameters
        int numEdgeSwitches = 20;

        int middleboxes = totalMiddleboxes();

        // middlebox capacities

        int[] capacity = new int[middleboxes];
        for (MBType type : MBType.values()) {
            int start = startIndex(type);
            for (int i = start; i < start + type.count; i++) {
                capacity[i] = type.capacityPerBox;
            }
        }

        // traffic generation

        MPSolver solver = null;
        MPVariable lambda = null;
        List<EdgePolicy> edgePolicies = null;
        long totalPackets = 0;
        int[] finalCapacity = capacity;
        int attempts = 0;
        final int maxAttempts = 8;

        while (attempts < maxAttempts) {
            attempts++;

            totalPackets = 1_000_000 + rand.nextInt(9_000_001); // [1M, 10M]
            double perTypeTotal = totalPackets / 3.0;

            edgePolicies = new ArrayList<>();

            for (PolicyType pt : PolicyType.values()) {

                // random weights per edge switch
                double[] weights = new double[numEdgeSwitches];
                double weightSum = 0;
                for (int e = 0; e < numEdgeSwitches; e++) {
                    weights[e] = 0.2 + rand.nextDouble();
                    weightSum += weights[e];
                }

                for (int e = 0; e < numEdgeSwitches; e++) {

                    EdgePolicy ep = new EdgePolicy();
                    ep.edge = e;
                    ep.policyType = pt.ordinal();
                    ep.traffic = perTypeTotal * (weights[e] / weightSum);
                    ep.paths = buildAlternativePaths(pt);

                    edgePolicies.add(ep);
                }
            }



            solver = MPSolver.createSolver("GLOP");

            if (solver == null) {
                System.out.println("GLOP not found.");
                return;
            }

            // t(h) for every path of every (e,p) and lambda

            for (EdgePolicy ep : edgePolicies) {
                for (int h = 0; h < ep.paths.size(); h++) {
                    ep.paths.get(h).t = solver.makeNumVar(0, Double.POSITIVE_INFINITY, "t_e" + ep.edge + "_p" + ep.policyType + "_h" + h);
                }
            }

            // 0 <= lambda <= 1 "lambda <= 1"
            lambda = solver.makeNumVar(0, 1, "lambda");

            // sum_{h in He,p} t(h) = Te,p   for every (e,p)

            for (EdgePolicy ep : edgePolicies) {

                MPConstraint c = solver.makeConstraint(ep.traffic, ep.traffic, "conserve_e" + ep.edge + "_p" + ep.policyType);

                for (Path path : ep.paths) {
                    c.setCoefficient(path.t, 1);
                }
            }

            // sum_{e,p,h : m in h} t(h) <= lambda * c(m)   for every m

            for (int m = 0; m < middleboxes; m++) {

                MPConstraint c = solver.makeConstraint(-Double.POSITIVE_INFINITY, 0,
                        "cap_m" + m);

                for (EdgePolicy ep : edgePolicies) {
                    for (Path path : ep.paths) {
                        if (path.usesMB.contains(m)) {
                            c.setCoefficient(path.t, 1);
                        }
                    }
                }

                c.setCoefficient(lambda, -finalCapacity[m]);
            }

            // min lambda

            MPObjective objective = solver.objective();
            objective.setCoefficient(lambda, 1);
            objective.setMinimization();

            // Solve

            MPSolver.ResultStatus status = solver.solve();

            if (status == MPSolver.ResultStatus.OPTIMAL) {
                break;
            }

            System.out.println("Attempt " + attempts + ": no optimal solution "
                    + "(random draw overloaded a middlebox type at lambda=1); resampling...");
            solver = null;
        }

        if (solver == null) {
            System.out.println("Could not find a feasible solution after " + maxAttempts + " attempts. Try increasing numEdgeSwitches " + "or lowering the traffic range.");
            return;
        }

        // compute middlebox load

        double[] load = new double[middleboxes];

        for (EdgePolicy ep : edgePolicies) {
            for (Path path : ep.paths) {
                double tVal = path.t.solutionValue();
                if (tVal <= 0) continue;
                for (int m : path.usesMB) {
                    load[m] += tVal;
                }
            }
        }
        // print result

        System.out.println();
        System.out.println("simulation setup");

        System.out.println("total traffic: " + totalPackets + " packets");
        System.out.println("edge switches: " + numEdgeSwitches);
        System.out.println();

        for (PolicyType pt : PolicyType.values()) {
            System.out.println("policy: " + pt.name());
            System.out.println("path: " + pt.description);
            System.out.println("alternative paths: " + pt.altPathCount);
            System.out.println();
        }

        // load distribution by middlebox type
        System.out.println();
        System.out.println("load distribution by middlebox type");

        for (MBType type : MBType.values()) {
            int start = startIndex(type);
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (int i = start; i < start + type.count; i++) {
                min = Math.min(min, load[i]);
                max = Math.max(max, load[i]);
            }

            System.out.println(type.name());
            // System.out.println("min load: " + (int) min);
            System.out.println("max load: " + (int) max);
            System.out.println("capacity: " + type.capacityPerBox);
            System.out.println();
        }

        // print lambda

        System.out.println("minimum max utilization lambda: " + lambda.solutionValue());

        // check capacity limits

        System.out.println();
        System.out.println("checking middlebox capacities");

        boolean allOk = true;

        for (int m = 0; m < middleboxes; m++) {
            double lhs = load[m];
            double rhs = lambda.solutionValue() * capacity[m];

            if (lhs > rhs + 1e-6) {
                allOk = false;
                System.out.println("middlebox " + m + " failed");
                System.out.println("load: " + lhs);
                System.out.println("limit: " + rhs);
                System.out.println();
            }
        }

        if (allOk) {
            System.out.println("all middleboxes passed");
        } else {
            System.out.println("some middleboxes failed");
        }
    }
}