package com.myproject;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.Graph;

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

    public static  int FW_CAP = 900672;
    public static  int IDS_CAP = 1350009;
    public static  int WP_CAP = 900672;
    public static  int TM_CAP = 900672;

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
    public double findMinCapacity(Map<String, EdgeRouter> ers, int totPackets, Graph graph,
                            java.util.function.Consumer<Integer> setCapacity) {
        double lo = 0, hi = 2_000_000; // upper bound guess
        double best = hi;
        for (int i = 0; i < 40; i++) { // ~40 iterations gives plenty of precision
            double mid = (lo + hi) / 2;
            setCapacity.accept((int) mid);          // e.g. v -> FW_CAP = v (needs to be non-final/injectable)
            Result r = OptimalLP.solve(ers, totPackets, graph);
            if (r.feasible && r.lambda <= 1.0 + 1e-9) {
                best = mid;
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return best;
    }
    public static Result solve(Map<String, EdgeRouter> edgeRouters, int totPackets, Graph graph) {

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

        // sole capacity constraint per middlebox: sum(t) - lambda * capacity <= 0
        Map<Integer, MPConstraint> capConstraints = new HashMap<>();
        for (int m = 0; m < capacity.length; m++) {
            MPConstraint greaterThen0 = solver.makeConstraint(-Double.POSITIVE_INFINITY, 0, "cap_m" + m);
            greaterThen0.setCoefficient(lambda, -capacity[m]);
            capConstraints.put(m, greaterThen0);
        }

        int totalTraficP1 = 0;
        int totalTraficP2 = 0;
        int totalTraficP3 = 0;

        MPVariable[] varArrayER = new MPVariable[160 * (32 + 16 + 8)];

        List<MPConstraint> constraintsER = new ArrayList<MPConstraint>();

        for (int index = 0; index < varArrayER.length; index++) {
            varArrayER[index] = solver.makeNumVar(0.0, Double.POSITIVE_INFINITY, "x_" + index);
        }
        // int erIndex = 0;
        int constIndex = 0;
        int varIndex = 0;

        for (EdgeRouter er : edgeRouters.values()) {
            for (Map.Entry<Path, Integer> entry : er.getFWIdsWpPathsTraffic().entrySet()) {
                totalTraficP1 += entry.getValue();

            }
            constraintsER.add(constIndex, solver.makeConstraint(totalTraficP1, totalTraficP1));

            for (int index = varIndex; index < varIndex + 32; index++) {
                constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
            }

            varIndex += 32;
            constIndex++;

            for (Map.Entry<Path, Integer> entry : er.getFwIdsPathsTraffic().entrySet()) {
                totalTraficP2 += entry.getValue();
            }

            constraintsER.add(constIndex, solver.makeConstraint(totalTraficP2, totalTraficP2));

            for (int index = varIndex; index < varIndex + 16; index++) {
                constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
            }
            varIndex += 16;
            constIndex++;

            for (Map.Entry<Path, Integer> entry : er.getIdsTmPathsTraffic().entrySet()) {
                totalTraficP3 += entry.getValue();
            }

            constraintsER.add(constIndex, solver.makeConstraint(totalTraficP3, totalTraficP3));

            for (int index = varIndex; index < varIndex + 8; index++) {
                constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
            }
            varIndex += 8;
            constIndex++;
            // erIndex ++;

            totalTraficP1 = 0;
            totalTraficP2 = 0;
            totalTraficP3 = 0;
        }
        int index = 0;
        for (int indexFW = 0; indexFW < 8; indexFW++) {
            varIndex = 0;
            index = 0;
            constraintsER.add(constIndex, solver.makeConstraint(-Double.POSITIVE_INFINITY, 0));
            for (EdgeRouter er : edgeRouters.values()) {
                index = varIndex;

                for (Map.Entry<Path, Integer> entry : er.getFWIdsWpPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("FW"+indexFW))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }

                varIndex += 32;

                for (Map.Entry<Path, Integer> entry : er.getFwIdsPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("FW"+indexFW))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }

                varIndex += 16;
                varIndex += 8;
            }
            constraintsER.get(constIndex).setCoefficient(lambda, FW_CAP * -1);
            constIndex++;
        }

        index = 0;
        for (int indexIDS = 0; indexIDS < 8; indexIDS++) {
            varIndex = 0;
            index = 0;
            constraintsER.add(constIndex, solver.makeConstraint(-Double.POSITIVE_INFINITY, 0));
            for (EdgeRouter er : edgeRouters.values()) {
                index = varIndex;

                for (Map.Entry<Path, Integer> entry : er.getFWIdsWpPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("IDS"+indexIDS))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }

                varIndex += 32;

                for (Map.Entry<Path, Integer> entry : er.getFwIdsPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("IDS"+indexIDS))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }

                varIndex += 16;
                for (Map.Entry<Path, Integer> entry : er.getIdsTmPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("IDS"+indexIDS))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }
                varIndex += 8;
            }
            constraintsER.get(constIndex).setCoefficient(lambda, IDS_CAP * -1);
            constIndex++;
        }
        index = 0;
        for (int indexWP = 0; indexWP < 4; indexWP++) {
            varIndex = 0;
            index = 0;
            constraintsER.add(constIndex, solver.makeConstraint(-Double.POSITIVE_INFINITY, 0));
            for (EdgeRouter er : edgeRouters.values()) {
                index = varIndex;

                for (Map.Entry<Path, Integer> entry : er.getFWIdsWpPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("WP" + indexWP))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }

                varIndex += 32;
                varIndex += 16;
                varIndex += 8;
            }
            constraintsER.get(constIndex).setCoefficient(lambda, WP_CAP * -1);
            constIndex++;
        }

        index = 0;
        for (int indexTM = 0; indexTM < 4; indexTM++) {
            varIndex = 0;
            index = 0;
            constraintsER.add(constIndex, solver.makeConstraint(-Double.POSITIVE_INFINITY, 0));
            for (EdgeRouter er : edgeRouters.values()) {

                varIndex += 32;
                varIndex += 16;
                index = varIndex;

                for (Map.Entry<Path, Integer> entry : er.getIdsTmPathsTraffic().entrySet()) {
                    if (entry.getKey().contains(graph.getNode("TM" + indexTM))) {
                        constraintsER.get(constIndex).setCoefficient(varArrayER[index], 1.0);
                    }
                    index++;
                }

                varIndex += 8;
            }
            constraintsER.get(constIndex).setCoefficient(lambda, TM_CAP * -1);
            constIndex++;
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
            if (tVal <= 0)
                continue;
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

    private static void addPolicyPool(MPSolver solver, EdgeRouter er, List<Path> pool, List<PolicyType> chain,
            Map<String, Integer> mbIndex, Map<Integer, MPConstraint> capConstraints, List<LPPath> allPaths) {

        if (pool == null || pool.isEmpty())
            return;

        double demand = 0;
        for (Flow flow : er.getFlows().values()) {
            if (flow.getFlowPolicy().equals(chain)) {
                demand += flow.getPakets();
            }
        }

        MPConstraint conserve = solver.makeConstraint(demand, demand,
                "conserve_" + er.getNode().getId() + "_" + chain.hashCode());

        int h = 0;
        for (Path path : pool) {
            if (path == null)
                continue;

            LPPath lp = new LPPath();
            for (Node node : path.getNodePath()) {
                Integer idx = mbIndex.get(node.getId());
                if (idx != null) {
                    lp.usesMB.add(idx);
                }
            }

            lp.t = solver.makeNumVar(0, Double.POSITIVE_INFINITY,
                    "t_" + er.getNode().getId() + "_" + chain.hashCode() + "_h" + (h++));

            conserve.setCoefficient(lp.t, 1);

            for (int m : lp.usesMB) {
                capConstraints.get(m).setCoefficient(lp.t, 1);
            }

            allPaths.add(lp);
        }
    }
}