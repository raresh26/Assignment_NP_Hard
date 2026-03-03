import java.util.*;

// Do not copy the entire class to WebLab, only the contents of the "solveProblem"-method
// On WebLab, your class should always just be called "Solution"
class Production {
    /**
     * Attempts to find a solution to the Production problem.
     *
     * @param C The number of units available for each type of material.
     * @param c A 2D-array describing how many units of a type of material are needed to produce on instance of a particular product-type.
     * @param P The maximum number of instances that can be made of a product-type.
     * @param r How much revenue one instance of a product-type will yield.
     * @param R The desired minimum revenue.
     * @return An array describing how many instances of each product-type should be produced to obtained enough revenue. null if this is not possible.
     */
    public static int[] solveProblem(int[] C, int[][] c, int[] P, int[] r, int R) {
        // TODO: Copy your code from "Production (Part 1)" here
        int numMaterialTypes = C.length;
        int numProductTypes = P.length;

        List<Solver.Variable> variables = new ArrayList<>();
        for(int i=0;i<numProductTypes;i++){
            List<Integer> possible_values = new ArrayList<>();
            for(int j=0;j<=P[i];j++)
                possible_values.add(j);
            variables.add(new Solver.Variable(possible_values));
        }

        List<Solver.Constraint> constraints = new ArrayList<>();
        for(int i=0;i<numMaterialTypes;i++){
            int[] ws = new int[numProductTypes];
            for(int j=0;j<numProductTypes;j++){
                ws[j] = -1 * c[i][j];
                Solver.Constraint material = new Solver.IneqConstraint(
                    variables.toArray(new Solver.Variable[0]),
                    ws,
                    -1*C[i]
                );
                constraints.add(material);
            }
        }

        Solver.Constraint returns = new Solver.IneqConstraint(
            variables.toArray(new Solver.Variable[0]),
            r,
            R
        );

        constraints.add(returns);


        // Use solver
        Solver solver = new Solver(
            variables.toArray(new Solver.Variable[0]),
            constraints.toArray(new Solver.Constraint[0])
        );
        int[] result = solver.findOneSolution();

        if(result==null) return null;

        int[] solution = new int[numProductTypes];
        // TODO: construct solution using result
        for(int i=0;i<solution.length;i++)
            solution[i] = result[i];
        return solution;
    }
}
