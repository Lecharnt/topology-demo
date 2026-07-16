package com.myproject;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
public class LyonMathTest {

    public static void main(String[] args) {

        final int Edgerouter = 166;
        final int Lambda = 1;

        final int Policy1 = 32;
        final int Policy2 = 16;
        final int Policy3 = 8;


        // 1. Initialize native libraries
        Loader.loadNativeLibraries();

        // 2. Instantiate GLOP solver
        MPSolver solver = MPSolver.createSolver("GLOP");
        if (solver == null) {
            System.err.println("Error: GLOP solver could not be initialized.");
            return;
        }

        // 3. Define the size and initialize the Variable Array
        int numVars = 5;
        MPVariable[] x = new MPVariable[numVars];

        // Populate the array with bounded variables: 0.0 <= x[i] <= 10.0
        for (int i = 0; i < numVars; i++) {
            x[i] = solver.makeNumVar(0.0, 10.0, "x_" + i);
        }

        // 4. Create a Global Constraint using a loop
        // Constraint: x_0 + x_1 + x_2 + x_3 + x_4 <= 15.0
        double infinity = Double.POSITIVE_INFINITY;
        MPConstraint sumConstraint = solver.makeConstraint(-infinity, 15.0, "sum_constraint");
        
        for (int i = 0; i < numVars; i++) {
            sumConstraint.setCoefficient(x[i], 1.0); // Coefficient 1 for each variable
        }

        // 5. Define Objective Function using a loop
        // Objective: Maximize (0*x_0 + 1*x_1 + 2*x_2 + 3*x_3 + 4*x_4)
        MPObjective objective = solver.objective();
        for (int i = 0; i < numVars; i++) {
            objective.setCoefficient(x[i], (double) i); // Dynamic weight based on index
        }
        objective.setMaximization();

        // 6. Run the solver
        MPSolver.ResultStatus resultStatus = solver.solve();

        // 7. Process output using loops
        if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("--- Optimal Array Solution Found ---");
            System.out.printf("Total Maximized Objective Value: %.2f%n", objective.value());
            
            for (int i = 0; i < numVars; i++) {
                System.out.printf("x[%d] Value = %.2f%n", i, x[i].solutionValue());
            }
        } else {
            System.err.println("An optimal solution could not be computed.");
        }
    }
}
