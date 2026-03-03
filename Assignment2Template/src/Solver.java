import java.util.*;

// Copy the below class to each of the Part 2 WebLab exercises
//    when submitting your solution.
// Make sure you copy the EXACT same version of your Solver in each
//    of the five exercises before the deadline, you'll be
//    automatically flagged for review if you don't!

// Write your solver below here.
// A template is already provided. You are allowed to deviate from this
//     template, as long as all classes and methods mentioned in the
//     description exist.

class Solver {
    static class Variable {
        public List<Integer> domain;

        /**
         * Constructs a Variable with a specified domain.
         * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
         *     However, you are allowed to change its behavior in any way you want.
         *
         * @param domain A list of integers, representing the domain of the variable.
         */
        public Variable(List<Integer> domain) {
            // Variable initialization
            this.domain = new ArrayList<>(domain);
        }
    }

    static abstract class Constraint {
    }

    static class NotEqConstraint extends Constraint {
        private Variable x1;
        private Variable x2;
        private int c;

        /**
         * Constructs a NotEqConstraint:
         *    x1 != x2 + c
         * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
         *     However, you are allowed to change its behavior in any way you want.
         *
         * @param x1 The first variable.
         * @param x2 The second variable.
         * @param c An integer constant.
         */
        public NotEqConstraint(Variable x1, Variable x2, int c) {
            // Variable initialization
            this.x1 = x1;
            this.x2 = x2;
            this.c = c;
        }
    }

    static class AllDiffConstraint extends Constraint {
        private Variable[] xs;

        /**
         * Constructs an AllDiffConstraint:
         *    AllDifferent(x1, ..., xn)
         * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
         *     However, you are allowed to change its behavior in any way you want.
         *
         * @param xs An array of a variables that should be different.
         */
        public AllDiffConstraint(Variable[] xs) {
            // Variable initialization
            this.xs = xs;
        }
    }

    static class IneqConstraint extends Constraint {
        private Variable[] xs;
        private int[] ws;
        private int c;

        /**
         * Constructs an IneqConstraint:
         *    w1 * x1 + ... + wn * xn >= c
         * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
         *     However, you are allowed to change its behavior in any way you want.
         *
         * @param xs An array of all variables involved.
         * @param ws An array of respective integer weights.
         * @param c An integer constant.
         */
        public IneqConstraint(Variable[] xs, int[] ws, int c) {
            // Variable initialization
            this.xs = xs;
            this.ws = ws;
            this.c = c;
        }
    }

    private Constraint[] constraints;
    private Variable[] variables;
    private List<int[]> foundSolutions;
    private Integer[] assignment;
    private IdentityHashMap<Variable, Integer> variableToIndex;
    private List<Constraint>[] incident;
    private int[] domainMin;
    private int[] domainMax;

    /**
     * Constructs a Solver using a list of variables and constraints.
     * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
     *     However, you are allowed to change its behavior in any way you want.
     */
    public Solver(Variable[] variables, Constraint[] constraints) {
        // Initialize variables
        this.variables = variables;
        this.constraints = constraints;
        this.foundSolutions = new ArrayList<>();
        this.assignment = new Integer[variables.length];
        this.variableToIndex = new IdentityHashMap<>();

        for (int i = 0; i < variables.length; i++) {
            variableToIndex.put(variables[i], i);
        }

        domainMin = new int[variables.length];
        domainMax = new int[variables.length];
        for (int i = 0; i < variables.length; i++) {
            domainMin[i] = domainMin(variables[i]);
            domainMax[i] = domainMax(variables[i]);
        }

        incident = new ArrayList[variables.length];
        for (int i = 0; i < variables.length; i++) {
            incident[i] = new ArrayList<>();
        }
        buildIncidentConstraints();
    }

    /**
     * Attempts to find one solution that satisfies the constraints.
     *     Should terminate immediately when a solution is found.
     *     Should return null if no solution exists.
     * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
     *     However, you are allowed to change its behavior in any way you want.
     *
     * @return An integer array of values to assign to the variables,
     *             in the order they are provided.
     */
    public int[] findOneSolution() {
        // Find a solution
        foundSolutions.clear();
        solve(false);

        // Return the found solution (or null if it doesn't exist)
        if (foundSolutions.isEmpty())
            return null;
        return foundSolutions.get(0);
    }

    /**
     * Attempts to find all solutions that satisfy the constraints.
     * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
     *     However, you are allowed to change its behavior in any way you want.
     *
     * @return A list of integer arrays, each representing a different
    solution to the problem (same format as findOneSolution).
     */
    public List<int[]> findAllSolutions() {
        // Find a solution
        foundSolutions.clear();
        solve(true);

        // Return all found solutions
        return foundSolutions;
    }

    /**
     * Applies search and inference to find value assignments to each variable,
     *    such that all constraints are satisfied. Any found solution is added
     *    to the list `foundSolutions`.
     * You are allowed to change or even remove this method.
     *
     * @param findAll True if all solutions must be found, false if only
     *                    only one needs to be found.
     */
    private void solve(boolean findAll) {
        Arrays.fill(assignment, null);
        backtrack(findAll);
    }

    private boolean backtrack(boolean findAll) {
        int varIndex = selectUnassignedVariable();
        if (varIndex == -1) {
            int[] solution = new int[variables.length];
            for (int i = 0; i < variables.length; i++) {
                solution[i] = assignment[i];
            }
            foundSolutions.add(solution);
            return true;
        }

        for (int value : variables[varIndex].domain) {
            assignment[varIndex] = value;
            if (isConsistentAfterAssign(varIndex)) {
                boolean found = backtrack(findAll);
                if (found && !findAll) {
                    assignment[varIndex] = null;
                    return true;
                }
            }
            assignment[varIndex] = null;
        }
        return false;
    }

    private int selectUnassignedVariable() {
        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private boolean isConsistentAfterAssign(int varIndex) {
        for (Constraint c : incident[varIndex]) {
            if (violates(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean violates(Constraint c) {
        if (c instanceof NotEqConstraint) {
            NotEqConstraint nc = (NotEqConstraint) c;
            Integer i1 = variableToIndex.get(nc.x1);
            Integer i2 = variableToIndex.get(nc.x2);
            Integer v1 = assignment[i1];
            Integer v2 = assignment[i2];
            if (v1 == null || v2 == null) {
                return false;
            }
            return v1 == v2 + nc.c;
        }

        if (c instanceof AllDiffConstraint) {
            AllDiffConstraint ac = (AllDiffConstraint) c;
            HashSet<Integer> seen = new HashSet<>();
            for (Variable x : ac.xs) {
                int idx = variableToIndex.get(x);
                Integer value = assignment[idx];
                if (value == null) {
                    continue;
                }
                if (!seen.add(value)) {
                    return true;
                }
            }
            return false;
        }

        if (c instanceof IneqConstraint) {
            IneqConstraint ic = (IneqConstraint) c;
            int currentSum = 0;
            int maxRemaining = 0;

            for (int i = 0; i < ic.xs.length; i++) {
                int idx = variableToIndex.get(ic.xs[i]);
                int w = ic.ws[i];
                Integer value = assignment[idx];
                if (value != null) {
                    currentSum += w * value;
                } else if (w > 0) {
                    maxRemaining += w * domainMax[idx];
                } else if (w < 0) {
                    maxRemaining += w * domainMin[idx];
                }
            }

            return currentSum + maxRemaining < ic.c;
        }

        return false;
    }

    private void buildIncidentConstraints() {
        for (Constraint c : constraints) {
            if (c instanceof NotEqConstraint) {
                NotEqConstraint nc = (NotEqConstraint) c;
                addIncident(nc.x1, c);
                addIncident(nc.x2, c);
            } else if (c instanceof AllDiffConstraint) {
                AllDiffConstraint ac = (AllDiffConstraint) c;
                for (Variable x : ac.xs) {
                    addIncident(x, c);
                }
            } else if (c instanceof IneqConstraint) {
                IneqConstraint ic = (IneqConstraint) c;
                for (Variable x : ic.xs) {
                    addIncident(x, c);
                }
            }
        }
    }

    private void addIncident(Variable v, Constraint c) {
        Integer idx = variableToIndex.get(v);
        if (idx == null) {
            throw new IllegalArgumentException("Constraint references unknown variable.");
        }
        incident[idx].add(c);
    }

    private int domainMin(Variable v) {
        if (v.domain.isEmpty()) {
            throw new IllegalArgumentException("Variable domain cannot be empty.");
        }
        int min = v.domain.get(0);
        for (int x : v.domain) {
            if (x < min) {
                min = x;
            }
        }
        return min;
    }

    private int domainMax(Variable v) {
        if (v.domain.isEmpty()) {
            throw new IllegalArgumentException("Variable domain cannot be empty.");
        }
        int max = v.domain.get(0);
        for (int x : v.domain) {
            if (x > max) {
                max = x;
            }
        }
        return max;
    }

}
