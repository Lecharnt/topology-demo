package com.myproject;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
public class LyonMathTest {
        public static void main(String[] args) {

        UnivariateFunction function = new UnivariateFunction() {
            @Override
            public double value(double x) {
                return Math.pow(x - 2, 2) + 3; // find local min of f(x) = (x - 2)^2 + 3
            }
        };

        BrentOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);

        UnivariatePointValuePair result = optimizer.optimize(new MaxEval(200), new UnivariateObjectiveFunction(function),GoalType.MINIMIZE, new SearchInterval(0, 5));

        System.out.println("Optimal X: " + result.getPoint());
        System.out.println("Optimal Y: " + result.getValue());
    }
}
