package com.myproject;

/**
 * Standalone entry point documenting the LP formulation from the HSLS paper.
 * The full solver runs inside App4 via {@link LoadBalancerOptimizer}.
 */
public class LyonMathTest {

    public static void main(String[] args) {
        System.out.println("HSLS load-balancing LP (Eq. 1)");
        System.out.println("  minimize lambda");
        System.out.println("  s.t. sum t(h_e,p) = T_e,p  for each edge e and policy p");
        System.out.println("       sum t(h) <= lambda * c(m)  for each middlebox m");
        System.out.println("       t(h) >= 0, lambda <= 1");
        System.out.println();
        System.out.println("Policies:");
        for (PathType pathType : PathType.values()) {
            System.out.println("  " + pathType + ": "
                    + pathType.getPathCount() + " paths, middleboxes=" + pathType.getMiddleboxes());
        }
        System.out.println();
        System.out.println("Run App4 to solve the LP on the full topology and compare");
        System.out.println("Single (greedy) vs Random vs Optimized (LP-weighted) path selection.");
    }
}
