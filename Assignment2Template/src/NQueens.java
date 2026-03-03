import java.util.*;

// Do not copy the entire class to WebLab, only the contents of the "solveProblem"-method
// On WebLab, your class should always just be called "Solution"
class NQueens {
    /**
     * Returns the number of solutions to the N-Queens problem.
     *
     * @param n The number of queens and the size of the board.
     * @return The number of valid ways to arrange the queens.
     */
    public static int solveProblem(int n) {
        // TODO: Copy your code from "N-Queens (Part 1)" here
        if(n==1) return 1;
        List<Solver.Variable> variables = new ArrayList<>();
        // TODO: add your variables
        //assume each queen is on a different column
        //so each queen can be considered to take only a "row-position"
        //each variable represents therow index of a queen
        for(int i=0;i<n;i++){
            List<Integer> queen = new ArrayList<>();
            for(int j=0;j<n;j++){
                queen.add(j);
            }
            variables.add(new Solver.Variable(queen));
        }

        List<Solver.Constraint> constraints = new ArrayList<>();
        // TODO: add your constraints
        //constraint that each queen is on a different row
        Solver.Constraint all_diff_rows = new Solver.AllDiffConstraint(
            variables.toArray(new Solver.Variable[0])
        );
        constraints.add(all_diff_rows);

        for(int i=0;i<n-1;i++){
            for(int j=i+1;j<n;j++){
                Solver.Constraint positive_diag = new Solver.NotEqConstraint(
                    variables.get(i),
                    variables.get(j),
                    i-j
                );
                Solver.Constraint  negative_diag = new Solver.NotEqConstraint(
                    variables.get(i),
                    variables.get(j),
                    j-i
                );

                constraints.add(positive_diag);
                constraints.add(negative_diag);
            }
        }

        // Use solver
        Solver solver = new Solver(
            variables.toArray(new Solver.Variable[0]),
            constraints.toArray(new Solver.Constraint[0])
        );
        List<int[]> result = solver.findAllSolutions();
        // TODO: construct solution using result
        return result.size();
    }
}
