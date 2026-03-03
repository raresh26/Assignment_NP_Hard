import java.util.*;

// Do not copy the entire class to WebLab, only the contents of the "solveProblem"-method
// On WebLab, your class should always just be called "Solution"
class FamilyDinner {
    /**
     * Attempts to find a solution to the Family Dinner problem.
     *
     * @param n The number of seats available at your table.
     * @param names A complete list of names of every person attending the dinner.
     * @param hates A list of pairs of people who should not be seated next to each other.
     * @return An array indicating who sits where around the table. null if there is no way to allocate everyone without breaking a constraint.
     */
    public static String[] solveProblem(int n, String[] names, List<String[]> hates) {
        // TODO: Copy your code from "Family Dinner (Part 1)" here
        int k = names.length;

        Map<String, Integer> id = new HashMap<>();
        for (int i = 0; i < k; i++) id.put(names[i], i);

        List<Integer> allSeats = new ArrayList<>(n);
        for (int s = 0; s < n; s++) allSeats.add(s);

        Solver.Variable[] personSeat = new Solver.Variable[k];
        List<Solver.Variable> variables = new ArrayList<>(k);

        personSeat[0] = new Solver.Variable(Collections.singletonList(0));
        variables.add(personSeat[0]);

        for (int i = 1; i < k; i++) {
            personSeat[i] = new Solver.Variable(allSeats);
            variables.add(personSeat[i]);
        }

        List<Solver.Constraint> constraints = new ArrayList<>();

        constraints.add(new Solver.AllDiffConstraint(personSeat));

        for (String[] pair : hates) {
            Integer a = id.get(pair[0]);
            Integer b = id.get(pair[1]);
            if (a == null || b == null) continue;

            Solver.Variable xa = personSeat[a];
            Solver.Variable xb = personSeat[b];

            constraints.add(new Solver.NotEqConstraint(xa, xb, 1));
            constraints.add(new Solver.NotEqConstraint(xa, xb, -1));
            constraints.add(new Solver.NotEqConstraint(xa, xb, 1 - n));
            constraints.add(new Solver.NotEqConstraint(xa, xb, -1 + n));
        }

        Solver solver = new Solver(
            variables.toArray(new Solver.Variable[0]),
            constraints.toArray(new Solver.Constraint[0])
        );
        int[] result = solver.findOneSolution();

        if (result == null) return null;

        String[] solution = new String[n];
        for (int i = 0; i < k; i++) {
            int seat = result[i];
            solution[seat] = names[i];
        }
        return solution;
    }
}
