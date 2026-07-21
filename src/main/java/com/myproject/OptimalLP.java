package com.myproject;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class OptimalLP {

    private static final int FW_COUNT = 8;
    private static final int IDS_COUNT = 8;
    private static final int WP_COUNT = 4;
    private static final int TM_COUNT = 4;

    private static final int FW_CAP = 1_000_000;
    private static final int IDS_CAP = 1_500_000;
    private static final int WP_CAP = 1_000_000;
    private static final int TM_CAP = 1_000_000;

    private static final List<PolicyType> CHAIN_FW_IDS_WP = List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP);
    private static final List<PolicyType> CHAIN_FW_IDS = List.of(PolicyType.FW, PolicyType.IDS);
    private static final List<PolicyType> CHAIN_IDS_TM = List.of(PolicyType.IDS, PolicyType.TM);

    private static class LPPath {
        Set<Integer> usesMB = new LinkedHashSet<>();
        MPVariable t;
    }

    public static class Result {
        public boolean feasible;
        public double lambda;
        public Map<String, Double> load = new HashMap<>();
    }

    public static Result solve(Map<String, EdgeRouter> edgeRouters) {

        Loader.loadNativeLibraries();

        Map<String, Integer> mbIndex = new HashMap<>();
        int[] capacity = buildIndexAndCapacity(mbIndex);

        MPSolver solver = MPSolver.createSolver("GLOP");

        Result result = new Result();

        if (solver == null) {
            System.out.println("GLOP not found.");
            result.feasible = false;
            return result;
        }

        MPVariable lambda = solver.makeNumVar(0, Double.POSITIVE_INFINITY, "lambda");

        // one capacity constraint per middlebox sum(t) - lambda * capacity <= 0
        Map<Integer, MPConstraint> capConstraints = new HashMap<>();
        for (int m = 0; m < capacity.length; m++) {
            MPConstraint c = solver.makeConstraint(-Double.POSITIVE_INFINITY, 0, "cap_m" + m);
            c.setCoefficient(lambda, -capacity[m]);
            capConstraints.put(m, c);
        }

        List<LPPath> allPaths = new ArrayList<>();

        for (EdgeRouter er : edgeRouters.values()) {
            addPolicyPool(solver, er, er.getFWIdsWpPaths(), CHAIN_FW_IDS_WP, mbIndex, capConstraints, allPaths);
            addPolicyPool(solver, er, er.getFwIdsPaths(), CHAIN_FW_IDS, mbIndex, capConstraints, allPaths);
            addPolicyPool(solver, er, er.getIdsTmPaths(), CHAIN_IDS_TM, mbIndex, capConstraints, allPaths);
        }

        MPObjective objective = solver.objective();
        objective.setCoefficient(lambda, 1);
        objective.setMinimization();

        MPSolver.ResultStatus status = solver.solve();

        if (status != MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("OptimalLP: no optimal solution found (status: " + status + ")");
            result.feasible = false;
            return result;
        }

        result.feasible = true;
        result.lambda = lambda.solutionValue();

        double[] loadByIndex = new double[capacity.length];
        for (LPPath p : allPaths) {
            double tVal = p.t.solutionValue();
            if (tVal <= 0) continue;
            for (int m : p.usesMB) {
                loadByIndex[m] += tVal;
            }
        }

        Map<Integer, String> reverseIndex = new HashMap<>();
        for (Map.Entry<String, Integer> e : mbIndex.entrySet()) {
            reverseIndex.put(e.getValue(), e.getKey());
        }
        for (int m = 0; m < loadByIndex.length; m++) {
            result.load.put(reverseIndex.get(m), loadByIndex[m]);
        }

        return result;
    }

    private static int[] buildIndexAndCapacity(Map<String, Integer> mbIndex) {
        int total = FW_COUNT + IDS_COUNT + WP_COUNT + TM_COUNT;
        int[] capacity = new int[total];
        int idx = 0;

        for (Node n : sortedById(PathFinder.FWList)) {
            mbIndex.put(n.getId(), idx);
            capacity[idx] = FW_CAP;
            idx++;
        }
        for (Node n : sortedById(PathFinder.IDSList)) {
            mbIndex.put(n.getId(), idx);
            capacity[idx] = IDS_CAP;
            idx++;
        }
        for (Node n : sortedById(PathFinder.WPList)) {
            mbIndex.put(n.getId(), idx);
            capacity[idx] = WP_CAP;
            idx++;
        }
        for (Node n : sortedById(PathFinder.TMList)) {
            mbIndex.put(n.getId(), idx);
            capacity[idx] = TM_CAP;
            idx++;
        }
        return capacity;
    }

    private static List<Node> sortedById(List<Node> list) {
        List<Node> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingInt(n -> Integer.parseInt(n.getId().replaceAll("\\D+", ""))));
        return copy;
    }

    private static void addPolicyPool(MPSolver solver, EdgeRouter er, List<Path> pool, List<PolicyType> chain, Map<String, Integer> mbIndex, Map<Integer, MPConstraint> capConstraints, List<LPPath> allPaths) {

        if (pool == null || pool.isEmpty()) return;

        double demand = 0;
        for (Flow flow : er.getFlows().values()) {
            if (flow.getFlowPolicy().equals(chain)) {
                demand += flow.getPakets();
            }
        }

        MPConstraint conserve = solver.makeConstraint(demand, demand,"conserve_" + er.getNode().getId() + "_" + chain.hashCode());

        int h = 0;
        for (Path path : pool) {
            if (path == null) continue;

            LPPath lp = new LPPath();
            for (Node node : path.getNodePath()) {
                Integer idx = mbIndex.get(node.getId());
                if (idx != null) {
                    lp.usesMB.add(idx);
                }
            }

            lp.t = solver.makeNumVar(0, Double.POSITIVE_INFINITY,"t_" + er.getNode().getId() + "_" + chain.hashCode() + "_h" + (h++));

            conserve.setCoefficient(lp.t, 1);

            for (int m : lp.usesMB) {
                capConstraints.get(m).setCoefficient(lp.t, 1);
            }

            allPaths.add(lp);
        }
    }
}