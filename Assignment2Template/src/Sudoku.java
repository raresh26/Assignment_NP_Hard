import java.util.*;

// Do not copy the entire class to WebLab, only the contents of the "solveProblem"-method
// On WebLab, your class should always just be called "Solution"
class Sudoku {
    /**
     * Returns the filled in sudoku grid.
     *
     * @param sudoku The partially filled in sudoku grid. Unfilled positions are marked with -1. Always either of size 9x9, 16x16 or 25x25.
     * @return The fully filled in sudoku grid.
     */
    public static int[][] solveProblem(int[][] sudoku) {
        // TODO: Copy your code from "Sudoku (Part 1)" here
        int n = sudoku.length;
        int sqrt_n = 1;
        while (sqrt_n * sqrt_n < n) sqrt_n++;
        Solver.Variable[][] cellVar = new Solver.Variable[n][n];
        List<Solver.Variable> variables = new ArrayList<>(n * n);
        List<Integer> fullDomain = new ArrayList<>(n);
        for (int v = 1; v <= n; v++) fullDomain.add(v);
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int given = sudoku[r][c];
                List<Integer> domain;
                if (given != -1) {
                    domain = Collections.singletonList(given);
                } else {
                    domain = fullDomain;
                }

                Solver.Variable v = new Solver.Variable(domain);
                cellVar[r][c] = v;
                variables.add(v);
            }
        }

        List<Solver.Constraint> constraints = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            Solver.Variable[] row = new Solver.Variable[n];
            for (int c = 0; c < n; c++) row[c] = cellVar[r][c];
            constraints.add(new Solver.AllDiffConstraint(row));
        }


        for (int c = 0; c < n; c++) {
            Solver.Variable[] col = new Solver.Variable[n];
            for (int r = 0; r < n; r++) col[r] = cellVar[r][c];
            constraints.add(new Solver.AllDiffConstraint(col));
        }


        for (int br = 0; br < sqrt_n; br++) {
            for (int bc = 0; bc < sqrt_n; bc++) {
                Solver.Variable[] block = new Solver.Variable[n];
                int idx = 0;
                int r0 = br * sqrt_n;
                int c0 = bc * sqrt_n;
                for (int dr = 0; dr < sqrt_n; dr++) {
                    for (int dc = 0; dc < sqrt_n; dc++) {
                        block[idx++] = cellVar[r0 + dr][c0 + dc];
                    }
                }
                constraints.add(new Solver.AllDiffConstraint(block));
            }
        }


        Solver solver = new Solver(
            variables.toArray(new Solver.Variable[0]),
            constraints.toArray(new Solver.Constraint[0])
        );

        int[] result = solver.findOneSolution();
        if (result == null) {

            return null;
        }


        int k = 0;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                sudoku[r][c] = result[k++];
            }
        }

        return sudoku;
    }
}
