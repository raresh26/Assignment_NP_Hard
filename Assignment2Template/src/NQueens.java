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
        if (n <= 0) return 0;
        if (n == 1) return 1;
        if (n <= 3) return 0;

        List<Integer> firstHalfRows = new ArrayList<>();
        for (int row = 0; row < n / 2; row++) {
            firstHalfRows.add(row);
        }

        int mirroredCount = countSolutionsWithFirstColumnDomain(n, firstHalfRows);
        int total = mirroredCount * 2;

        if (n % 2 == 1) {
            total += countSolutionsWithFirstColumnDomain(n, Collections.singletonList(n / 2));
        }

        return total;
    }

    private static int countSolutionsWithFirstColumnDomain(int n, List<Integer> firstColumnRows) {
        List<Solver.Variable> variables = new ArrayList<>(n);
        for (int col = 0; col < n; col++) {
            List<Integer> domain = new ArrayList<>();
            if (col == 0) {
                domain.addAll(firstColumnRows);
            } else {
                for (int row = 0; row < n; row++) {
                    domain.add(row);
                }
            }
            variables.add(new Solver.Variable(domain));
        }

        List<Solver.Constraint> constraints = new ArrayList<>();
        Solver.Variable[] vars = variables.toArray(new Solver.Variable[0]);
        constraints.add(new Solver.AllDiffConstraint(vars));

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                constraints.add(new Solver.NotEqConstraint(variables.get(i), variables.get(j), i - j));
                constraints.add(new Solver.NotEqConstraint(variables.get(i), variables.get(j), j - i));
            }
        }

        Solver solver = new Solver(vars, constraints.toArray(new Solver.Constraint[0]));
        return solver.findAllSolutions().size();
    }
}
