package com.myproject;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Solves the load-balancing LP from Eq. (1) in the HSLS paper:
 *   minimize lambda
 *   s.t. sum_{h in H_{e,p}} t(h) = T_{e,p}
 *        sum_{e,p,h: m in h} t(h) <= lambda * c(m)
 *        t(h) >= 0
 * Lambda is allowed above 1 so the LP stays feasible when capacity is tight;
 * values > 1 mean at least one middlebox is overloaded relative to c(m).
 */
public class LoadBalancerOptimizer {

    public static final double DEFAULT_CAPACITY = 1_010_000;

    public static class Result {
        public final double lambda;
        public final Map<String, Map<PathType, double[]>> weightsByEdge;
        public final Map<String, Map<PathType, double[]>> trafficByEdge;
        public final boolean optimal;

        public Result(double lambda,
                      Map<String, Map<PathType, double[]>> weightsByEdge,
                      Map<String, Map<PathType, double[]>> trafficByEdge,
                      boolean optimal) {
            this.lambda = lambda;
            this.weightsByEdge = weightsByEdge;
            this.trafficByEdge = trafficByEdge;
            this.optimal = optimal;
        }
    }

    private static class PathVar {
        final String edgeId;
        final PathType policy;
        final List<String> middleboxes;
        final MPVariable variable;

        PathVar(String edgeId, PathType policy,
                List<String> middleboxes, MPVariable variable) {
            this.edgeId = edgeId;
            this.policy = policy;
            this.middleboxes = middleboxes;
            this.variable = variable;
        }
    }

    public static Result solve(Map<String, EdgeRouter> edgeRouters, double middleboxCapacity) {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");
        if (solver == null) {
            throw new RuntimeException("GLOP solver could not be initialized");
        }

        MPVariable lambda = solver.makeNumVar(0.0, Double.POSITIVE_INFINITY, "lambda");
        List<PathVar> pathVars = new ArrayList<>();

        for (Map.Entry<String, EdgeRouter> entry : edgeRouters.entrySet()) {
            String edgeId = entry.getKey();
            EdgeRouter er = entry.getValue();
            registerPaths(solver, pathVars, edgeId, PathType.FW_IDS_WP, er.getFWIdsWpPaths());
            registerPaths(solver, pathVars, edgeId, PathType.FW_IDS, er.getFwIdsPaths());
            registerPaths(solver, pathVars, edgeId, PathType.IDS_TM, er.getIdsTmPaths());
        }

        // Constraint 1: traffic conservation per (edge, policy)
        for (String edgeId : edgeRouters.keySet()) {
            EdgeRouter er = edgeRouters.get(edgeId);
            for (PathType policy : PathType.values()) {
                List<PathVar> group = filter(pathVars, edgeId, policy);
                if (group.isEmpty()) {
                    continue;
                }
                double totalTraffic = getTrafficForPolicy(er, policy);
                MPConstraint constraint = solver.makeConstraint(
                        totalTraffic, totalTraffic, "traffic_" + edgeId + "_" + policy);
                for (PathVar pathVar : group) {
                    constraint.setCoefficient(pathVar.variable, 1.0);
                }
            }
        }

        // Constraint 2: middlebox capacity
        Set<String> allMiddleboxes = new HashSet<>();
        for (PathVar pathVar : pathVars) {
            allMiddleboxes.addAll(pathVar.middleboxes);
        }

        double infinity = Double.POSITIVE_INFINITY;
        for (String middleboxId : allMiddleboxes) {
            MPConstraint constraint = solver.makeConstraint(-infinity, 0.0, "capacity_" + middleboxId);
            for (PathVar pathVar : pathVars) {
                if (pathVar.middleboxes.contains(middleboxId)) {
                    constraint.setCoefficient(pathVar.variable, 1.0);
                }
            }
            constraint.setCoefficient(lambda, -middleboxCapacity);
        }

        MPObjective objective = solver.objective();
        objective.setCoefficient(lambda, 1.0);
        objective.setMinimization();

        MPSolver.ResultStatus status = solver.solve();
        boolean optimal = status == MPSolver.ResultStatus.OPTIMAL;

        Map<String, Map<PathType, double[]>> trafficByEdge = new HashMap<>();
        Map<String, Map<PathType, double[]>> weightsByEdge = new HashMap<>();

        for (String edgeId : edgeRouters.keySet()) {
            for (PathType policy : PathType.values()) {
                List<PathVar> group = filter(pathVars, edgeId, policy);
                if (group.isEmpty()) {
                    continue;
                }

                double[] traffic = new double[group.size()];
                double total = 0.0;
                for (int i = 0; i < group.size(); i++) {
                    traffic[i] = optimal ? group.get(i).variable.solutionValue() : 0.0;
                    total += traffic[i];
                }

                double[] weights = new double[group.size()];
                if (total > 0.0) {
                    for (int i = 0; i < group.size(); i++) {
                        weights[i] = traffic[i] / total;
                    }
                } else {
                    double uniform = 1.0 / group.size();
                    for (int i = 0; i < group.size(); i++) {
                        weights[i] = uniform;
                    }
                }

                trafficByEdge
                        .computeIfAbsent(edgeId, ignored -> new EnumMap<>(PathType.class))
                        .put(policy, traffic);
                weightsByEdge
                        .computeIfAbsent(edgeId, ignored -> new EnumMap<>(PathType.class))
                        .put(policy, weights);
            }
        }

        double lambdaValue = optimal ? lambda.solutionValue() : 1.0;
        return new Result(lambdaValue, weightsByEdge, trafficByEdge, optimal);
    }

    public static void applyWeights(Map<String, EdgeRouter> edgeRouters, Result result) {
        for (Map.Entry<String, Map<PathType, double[]>> edgeEntry : result.weightsByEdge.entrySet()) {
            EdgeRouter edgeRouter = edgeRouters.get(edgeEntry.getKey());
            if (edgeRouter == null) {
                continue;
            }
            for (Map.Entry<PathType, double[]> policyEntry : edgeEntry.getValue().entrySet()) {
                edgeRouter.setPathWeights(policyEntry.getKey(), policyEntry.getValue());
            }
        }
    }

    private static void registerPaths(MPSolver solver, List<PathVar> pathVars,
                                      String edgeId, PathType policy, List<Path> paths) {
        for (int i = 0; i < paths.size(); i++) {
            Path path = paths.get(i);
            if (path == null) {
                continue;
            }
            String varName = "t_" + edgeId + "_" + policy + "_" + i;
            MPVariable variable = solver.makeNumVar(0.0, infinity(solver), varName);
            pathVars.add(new PathVar(edgeId, policy, getMiddleboxesOnPath(path), variable));
        }
    }

    private static double infinity(MPSolver solver) {
        return Double.POSITIVE_INFINITY;
    }

    private static List<PathVar> filter(List<PathVar> pathVars, String edgeId, PathType policy) {
        List<PathVar> filtered = new ArrayList<>();
        for (PathVar pathVar : pathVars) {
            if (pathVar.edgeId.equals(edgeId) && pathVar.policy == policy) {
                filtered.add(pathVar);
            }
        }
        return filtered;
    }

    private static double getTrafficForPolicy(EdgeRouter edgeRouter, PathType policy) {
        List<PolicyType> target = policy.getMiddleboxes();
        double total = 0.0;
        for (Flow flow : edgeRouter.getFlows().values()) {
            if (flow.getFlowPolicy().equals(target)) {
                total += flow.getPakets();
            }
        }
        return total;
    }

    static List<String> getMiddleboxesOnPath(Path path) {
        List<String> middleboxes = new ArrayList<>();
        for (Node node : path.getNodePath()) {
            String nodeId = node.getId();
            if (nodeId.startsWith(PolicyType.FW.name())
                    || nodeId.startsWith(PolicyType.IDS.name())
                    || nodeId.startsWith(PolicyType.WP.name())
                    || nodeId.startsWith(PolicyType.TM.name())) {
                middleboxes.add(nodeId);
            }
        }
        return middleboxes;
    }
}
