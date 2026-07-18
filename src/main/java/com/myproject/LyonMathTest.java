package com.myproject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.*;

/**
 * Implements the load-balancing LP from Eq. 1 of:
 * "Scalable and Balanced Policy Enforcement Through Hybrid SDN-Label
 * Switching" (Odegbile, Chen, Zhang), using the same evaluation setup
 * described in Section IV of the paper:
 *
 *  - Middlebox types & counts: FW=8, IDS=8, WP=4, TM=4 (Sec. IV-A)
 *  - Middlebox capacities: FW=1M, IDS=1.5M, WP=1M, TM=1M packets
 *    (Table II caption: "the capacities of the four types of middleboxes
 *    are 1M, 1.5M, 1M and 1M respectively", listed in FW, IDS, WP, TM order)
 *  - Three policy types (Sec. IV-A):
 *      Type 1: FW -> IDS -> WP,  32 alt. paths (= half FW x half IDS x half WP = 4x4x2)
 *      Type 2: FW -> IDS,        16 alt. paths (= half FW x half IDS = 4x4)
 *      Type 3: IDS -> TM,         8 alt. paths (= half IDS x half TM = 4x2)
 *  - Total traffic: 1M - 10M packets, split evenly across the 3 policy
 *    types, then distributed across edge switches (Sec. IV-A/B).
 *
 * Variables:   t(h_e,p)  -- continuous traffic portion routed on path h
 *                           for policy p at edge switch e
 *              lambda    -- max middlebox load factor, 0 <= lambda <= 1
 *
 * min  lambda
 * s.t. sum_{h in H_e,p} t(h) = T_e,p                 for all e in E, p in P_e
 *      sum_{e,p,h : m in h} t(h) <= lambda * c(m)     for all m in M
 *      t(h) >= 0
 *      0 <= lambda <= 1
 */
public class LyonMathTest {

    static final Random rand = new Random();

    //-----------------------------
    // Middlebox types (Sec. IV-A)
    //-----------------------------

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

    // Global middlebox index layout: [FW 0-7][IDS 8-15][WP 16-19][TM 20-23]
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

    // A single alternative enforcement path: the specific middleboxes it
    // traverses (mirrors He,p built from half-pools of the required types).
    static class Path {
        Set<Integer> usesMB = new LinkedHashSet<>();
        MPVariable t; // t(h_e,p)
    }

    // A policy p as it applies to one edge switch e: holds He,p and Te,p.
    static class EdgePolicy {
        int edge;
        int policyType; // 0, 1, or 2 (see PolicyType below)
        double traffic; // T_e,p
        List<Path> paths = new ArrayList<>(); // H_e,p
    }

    // The 3 policy types from Sec. IV-A, each with its required function
    // sequence and the number of alternative paths it is assigned.
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

    // Pick `half` distinct global indices at random from the pool of `type`.
    static List<Integer> randomHalfPool(MBType type) {
        int start = startIndex(type);
        int half = type.count / 2;
        List<Integer> pool = new ArrayList<>();
        for (int i = start; i < start + type.count; i++) pool.add(i);
        java.util.Collections.shuffle(pool, rand);
        return pool.subList(0, half);
    }

    // Build He,p: the cross product of half-pools for each function in the
    // policy's sequence, per the paper's "half middleboxes of each type"
    // path-count derivation (e.g. 4x4x2=32).
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

        //-----------------------------
        // Simulation Parameters
        //-----------------------------

        // The paper uses ~160 edge routers (16 core routers x 10 edge
        // routers each); we scale down to a manageable demo size while
        // keeping the same per-edge traffic-generation logic.
        int numEdgeSwitches = 20;

        int middleboxes = totalMiddleboxes();

        //-----------------------------
        // Middlebox Capacities  c(m), per Sec. IV-A/Table II
        //-----------------------------

        int[] capacity = new int[middleboxes];
        for (MBType type : MBType.values()) {
            int start = startIndex(type);
            for (int i = start; i < start + type.count; i++) {
                capacity[i] = type.capacityPerBox;
            }
        }

        //-----------------------------
        // Traffic generation (Sec. IV-A):
        // total packets in [1M, 10M], split evenly across the 3 policy
        // types, then distributed across edge switches with random
        // (Dirichlet-like) proportions.
        //
        // Since these are the paper's own realistic parameters (not
        // artificially padded), a rare unlucky random draw of half-pools
        // and edge weights can still concentrate too much traffic on one
        // middlebox even at lambda=1. We retry with a fresh random draw
        // a few times rather than silently giving up.
        //-----------------------------

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

                // Random weights per edge switch, normalized to sum to 1,
                // used to split this policy type's total traffic across edges.
                double[] weights = new double[numEdgeSwitches];
                double weightSum = 0;
                for (int e = 0; e < numEdgeSwitches; e++) {
                    weights[e] = 0.2 + rand.nextDouble(); // avoid near-zero shares
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

            //-----------------------------
            // LP Solver (continuous, so GLOP -- t(h) and lambda are both
            // continuous variables per the paper, not booleans)
            //-----------------------------

            solver = MPSolver.createSolver("GLOP");

            if (solver == null) {
                System.out.println("GLOP not found.");
                return;
            }

            //-----------------------------
            // Variables: t(h) for every path of every (e,p), and lambda
            //-----------------------------

            for (EdgePolicy ep : edgePolicies) {
                for (int h = 0; h < ep.paths.size(); h++) {
                    ep.paths.get(h).t = solver.makeNumVar(0, Double.POSITIVE_INFINITY,
                            "t_e" + ep.edge + "_p" + ep.policyType + "_h" + h);
                }
            }

            // 0 <= lambda <= 1, per the paper's constraint "lambda <= 1"
            lambda = solver.makeNumVar(0, 1, "lambda");

            //-----------------------------
            // Constraint 1 (flow conservation):
            // sum_{h in He,p} t(h) = Te,p   for every (e,p)
            //-----------------------------

            for (EdgePolicy ep : edgePolicies) {

                MPConstraint c = solver.makeConstraint(ep.traffic, ep.traffic,
                        "conserve_e" + ep.edge + "_p" + ep.policyType);

                for (Path path : ep.paths) {
                    c.setCoefficient(path.t, 1);
                }
            }

            //-----------------------------
            // Constraint 2 (capacity):
            // sum_{e,p,h : m in h} t(h) <= lambda * c(m)   for every m
            //-----------------------------

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

            //-----------------------------
            // Objective: min lambda
            //-----------------------------

            MPObjective objective = solver.objective();
            objective.setCoefficient(lambda, 1);
            objective.setMinimization();

            //-----------------------------
            // Solve
            //-----------------------------

            MPSolver.ResultStatus status = solver.solve();

            if (status == MPSolver.ResultStatus.OPTIMAL) {
                break;
            }

            System.out.println("Attempt " + attempts + ": no optimal solution "
                    + "(random draw overloaded a middlebox type at lambda=1); resampling...");
            solver = null;
        }

        if (solver == null) {
            System.out.println("Could not find a feasible solution after "
                    + maxAttempts + " attempts. Try increasing numEdgeSwitches "
                    + "or lowering the traffic range.");
            return;
        }

        //-----------------------------
        // Compute per-middlebox load
        //-----------------------------

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

        //-----------------------------
        // Report: overview
        //-----------------------------

        System.out.println();
        System.out.println("Simulation Setup (mirrors paper Sec. IV-A)");
        System.out.println("-------------------------------------------");
        System.out.printf("Total traffic volume        : %,d packets%n", totalPackets);
        System.out.printf("Edge switches                : %d%n", numEdgeSwitches);
        for (PolicyType pt : PolicyType.values()) {
            System.out.printf("Policy type %-16s: %-14s  (%d alt. paths/edge)%n",
                    pt.name(), pt.description, pt.altPathCount);
        }

        //-----------------------------
        // Report: load distribution by middlebox type (mirrors Table II)
        //-----------------------------

        System.out.println();
        System.out.println("Load Distribution by Middlebox Type (Load-Balanced / LB)");
        System.out.println("---------------------------------------------------------");
        System.out.printf("%-6s %12s %12s %14s%n", "Type", "Min Load", "Max Load", "Capacity/box");

        for (MBType type : MBType.values()) {
            int start = startIndex(type);
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = start; i < start + type.count; i++) {
                min = Math.min(min, load[i]);
                max = Math.max(max, load[i]);
            }
            System.out.printf("%-6s %,12.0f %,12.0f %,14d%n",
                    type.name(), min, max, type.capacityPerBox);
        }

        //-----------------------------
        // Objective
        //-----------------------------

        System.out.println();
        System.out.printf("Minimum Max Utilization (Lambda) = %.4f%n",
                lambda.solutionValue());

        //-----------------------------
        // Verify Constraint 2
        //-----------------------------

        System.out.println();
        System.out.println("Verification (per middlebox)");
        boolean allOk = true;
        for (int m = 0; m < middleboxes; m++) {
            double lhs = load[m];
            double rhs = lambda.solutionValue() * capacity[m];
            boolean ok = lhs <= rhs + 1e-6;
            allOk &= ok;
            if (!ok) {
                System.out.printf("MB %d : %.2f <= %.2f  FAILED%n", m, lhs, rhs);
            }
        }
        System.out.println(allOk ? "All middlebox capacity constraints satisfied." :
                "Some constraints failed (see above).");
    }
}