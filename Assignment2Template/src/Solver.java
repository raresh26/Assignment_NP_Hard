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
            this.xs = xs;
            this.ws = ws;
            this.c = c;
        }
    }

    private static final int TYPE_NOTEQ = 1;
    private static final int TYPE_ALLDIFF = 2;
    private static final int TYPE_INEQ = 3;

    private static class CompiledConstraint {
        int type;

        // NotEq
        int x1;
        int x2;
        int c;

        // AllDiff / Ineq
        int[] vars;

        // AllDiff state
        int offset;
        int[] countsArray;
        HashMap<Integer, Integer> countsMap;
        int duplicateValueCount;

        // Ineq state
        int[] ws;
        long rhs;
        long currentSum;
        long maxRemaining;
        long[] bestContrib;

        void onAssign(int pos, int value) {
            if (type == TYPE_ALLDIFF) {
                if (countsArray != null) {
                    int idx = value - offset;
                    int count = ++countsArray[idx];
                    if (count == 2) {
                        duplicateValueCount++;
                    }
                } else {
                    int count = countsMap.getOrDefault(value, 0) + 1;
                    countsMap.put(value, count);
                    if (count == 2) {
                        duplicateValueCount++;
                    }
                }
                return;
            }

            if (type == TYPE_INEQ) {
                currentSum += (long) ws[pos] * value;
                maxRemaining -= bestContrib[pos];
            }
        }

        void onUnassign(int pos, int value) {
            if (type == TYPE_ALLDIFF) {
                if (countsArray != null) {
                    int idx = value - offset;
                    int count = --countsArray[idx];
                    if (count == 1) {
                        duplicateValueCount--;
                    }
                } else {
                    int old = countsMap.get(value);
                    int count = old - 1;
                    if (count == 1) {
                        duplicateValueCount--;
                    }
                    if (count == 0) {
                        countsMap.remove(value);
                    } else {
                        countsMap.put(value, count);
                    }
                }
                return;
            }

            if (type == TYPE_INEQ) {
                currentSum -= (long) ws[pos] * value;
                maxRemaining += bestContrib[pos];
            }
        }

        boolean isViolated(int[] assignment, boolean[] assigned) {
            if (type == TYPE_NOTEQ) {
                return assigned[x1] && assigned[x2] && assignment[x1] == assignment[x2] + c;
            }
            if (type == TYPE_ALLDIFF) {
                return duplicateValueCount > 0;
            }
            if (type == TYPE_INEQ) {
                return currentSum + maxRemaining < rhs;
            }
            return false;
        }
    }

    private static class IncidentRef {
        CompiledConstraint constraint;
        int position;

        IncidentRef(CompiledConstraint constraint, int position) {
            this.constraint = constraint;
            this.position = position;
        }
    }

    private Constraint[] constraints;
    private Variable[] variables;
    private List<int[]> foundSolutions;

    private int[] assignment; //assignment[i] stores the value currently assigned to i-th variable
    private boolean[] assigned; //assigned[i] is true if i-th variable is assigned, false otherwise
    private IdentityHashMap<Variable, Integer> variableToIndex;
    private List<IncidentRef>[] incidents; //incident[i] contains all constraints involving i-th variable
    private IdentityHashMap<Constraint, CompiledConstraint> basicToCompiledConstr;
    private boolean findAllMode;
    private int maxInitialDomainSize;
    private boolean hasAllDiffConstraint;

    private int[] domainMin; //array storing the min value of each variable
    private int[] domainMax; //array storing the max value of each variable

    /**
     * Constructs a Solver using a list of variables and constraints.
     * DO NOT REMOVE THIS METHOD OR CHANGE ITS SIGNATURE.
     *     However, you are allowed to change its behavior in any way you want.
     */
    @SuppressWarnings("unchecked")
    public Solver(Variable[] variables, Constraint[] constraints) {
        this.variables = variables;
        this.constraints = constraints;
        this.foundSolutions = new ArrayList<>();
        this.hasAllDiffConstraint = false;
        for (Constraint c : constraints) {
            if (c instanceof AllDiffConstraint) {
                hasAllDiffConstraint = true;
                break;
            }
        }

        int n = variables.length;
        this.assignment = new int[n];
        this.assigned = new boolean[n];
        this.variableToIndex = new IdentityHashMap<>();
        for (int i = 0; i < n; i++) {
            variableToIndex.put(variables[i], i);
        }

        this.domainMin = new int[n];
        this.domainMax = new int[n];
        this.maxInitialDomainSize = 0;
        for (int i = 0; i < n; i++) {
            domainMin[i] = domainMin(variables[i]);
            domainMax[i] = domainMax(variables[i]);
            if (variables[i].domain.size() > maxInitialDomainSize) {
                maxInitialDomainSize = variables[i].domain.size();
            }
        }

        this.incidents = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            incidents[i] = new ArrayList<>();
        }

        this.basicToCompiledConstr = new IdentityHashMap<>();
        buildCompiledConstraints();
    }

    /**
     * Compiles the constraints into an optimized internal representation suitable for use
     * during the solving process. Constraints are transformed into `CompiledConstraint`
     * objects, which store the necessary information for efficiently managing and
     * propagating constraints.
     *
     * The method processes different types of constraints as follows:
     *
     * 1. **NotEqConstraint**:
     *    - Converts inequality constraints (x1 != x2 + c) into a `CompiledConstraint` of type `TYPE_NOTEQ`.
     *    - Maps involved variables to their internal indices.
     *    - Updates incident information for participating variables.
     *
     * 2. **AllDiffConstraint**:
     *    - Processes "all different" constraints and converts them into a `CompiledConstraint` of type `TYPE_ALLDIFF`.
     *    - Maps all participating variables to their internal indices and computes the value range.
     *    - Allocates an efficient storage (array or hashmap) based on the computed range.
     *    - Updates incident information for all involved variables.
     *
     * 3. **IneqConstraint**:
     *    - Handles inequality constraints and converts them into a `CompiledConstraint` of type `TYPE_INEQ`.
     *    - Maps all participating variables to their internal indices and records associated weights.
     *    - Computes additional attributes such as the best contributions, current sum, and maximum remaining value.
     *    - Updates incident information for all involved variables.
     *
     * These compiled constraints are stored in the `basicToCompiledConstr` map for later use
     * during the solving process. The method ensures efficient representation and quick access
     * to constraint data, optimizing subsequent computations.
     */
    private void buildCompiledConstraints() {
        for (Constraint c : constraints) {
            if (c instanceof NotEqConstraint) {
                NotEqConstraint nc = (NotEqConstraint) c;
                CompiledConstraint cc = new CompiledConstraint();
                cc.type = TYPE_NOTEQ;
                cc.x1 = variableIndex(nc.x1);
                cc.x2 = variableIndex(nc.x2);
                cc.c = nc.c;
                basicToCompiledConstr.put(c, cc);
                addIncident(cc.x1, cc, -1);
                addIncident(cc.x2, cc, -1);
            }else if (c instanceof AllDiffConstraint) {
                AllDiffConstraint ac = (AllDiffConstraint) c;
                CompiledConstraint cc = new CompiledConstraint();
                cc.type = TYPE_ALLDIFF;
                cc.vars = new int[ac.xs.length];

                int minValue = Integer.MAX_VALUE;
                int maxValue = Integer.MIN_VALUE;
                for (int i = 0; i < ac.xs.length; i++) {
                    int varIndex = variableIndex(ac.xs[i]);
                    cc.vars[i] = varIndex;
                    if (domainMin[varIndex] < minValue) {
                        minValue = domainMin[varIndex];
                    }
                    if (domainMax[varIndex] > maxValue) {
                        maxValue = domainMax[varIndex];
                    }
                }

                long range = (long) maxValue - (long) minValue + 1L;
                if (range > 0 && range <= 4096) {
                    cc.offset = minValue;
                    cc.countsArray = new int[(int) range];
                } else {
                    cc.countsMap = new HashMap<>();
                }

                basicToCompiledConstr.put(c, cc);
                for (int i = 0; i < cc.vars.length; i++) {
                    addIncident(cc.vars[i], cc, i);
                }
            }else if (c instanceof IneqConstraint) {
                IneqConstraint ic = (IneqConstraint) c;
                CompiledConstraint cc = new CompiledConstraint();
                cc.type = TYPE_INEQ;
                cc.vars = new int[ic.xs.length];
                cc.ws = Arrays.copyOf(ic.ws, ic.ws.length);
                cc.rhs = ic.c;
                cc.bestContrib = new long[ic.xs.length];
                cc.currentSum = 0;
                cc.maxRemaining = 0;

                for (int i = 0; i < ic.xs.length; i++) {
                    int varIndex = variableIndex(ic.xs[i]);
                    cc.vars[i] = varIndex;
                    int w = cc.ws[i];
                    long best = 0;
                    if (w > 0) {
                        best = (long) w * domainMax[varIndex];
                    } else if (w < 0) {
                        best = (long) w * domainMin[varIndex];
                    }
                    cc.bestContrib[i] = best;
                    cc.maxRemaining += best;
                }

                basicToCompiledConstr.put(c, cc);
                for (int i = 0; i < cc.vars.length; i++) {
                    addIncident(cc.vars[i], cc, i);
                }
            }
        }
    }

    private void addIncident(int varIndex, CompiledConstraint constraint, int position) {
        incidents[varIndex].add(new IncidentRef(constraint, position));
    }

    private int variableIndex(Variable v) {
        Integer idx = variableToIndex.get(v);
        if (idx == null) {
            throw new IllegalArgumentException("Constraint references unknown variable.");
        }
        return idx;
    }

    private int domainMin(Variable v) {
        if (v.domain.isEmpty()) {
            throw new IllegalArgumentException("Variable domain cannot be empty.");
        }
        int min = v.domain.get(0);
        for (int value : v.domain) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private int domainMax(Variable v) {
        if (v.domain.isEmpty()) {
            throw new IllegalArgumentException("Variable domain cannot be empty.");
        }
        int max = v.domain.get(0);
        for (int value : v.domain) {
            if (value > max) {
                max = value;
            }
        }
        return max;
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
        foundSolutions.clear();
        solve(false);
        if (foundSolutions.isEmpty()) {
            return null;
        }
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
        foundSolutions.clear();
        solve(true);
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
        findAllMode = findAll;
        Arrays.fill(assigned, false);
        backtrack(findAll);
    }

    /**
     * Performs a backtracking search to find solutions for the given constraint satisfaction problem.
     * The method recursively assigns values to variables, checks for constraint violations, and backtracks if necessary.
     *
     * @param findAll Specifies whether to find all solutions (`true`) or stop after finding the first valid solution (`false`).
     * @return `true` if at least one solution is found, `false` otherwise.
     */
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
            if (!assignAndCheck(varIndex, value)) {
                continue;
            }

            boolean found = backtrack(findAll);
            unassign(varIndex, value);

            if (found && !findAll) {
                return true;
            }
        }

        return false;
    }

    /**
     * Selects the next unassigned variable to process during a constraint satisfaction problem-solving process.
     * The method uses heuristics to prioritize variables based on their current state and constraints,
     * aiming to optimize the search process. Depending on the problem's setup, it utilizes either
     * "legal lookahead" or simpler selection strategies.
     *
     * The "legal lookahead" heuristic selects the variable with the fewest legal values remaining
     * (also known as the Minimum Remaining Values or MRV heuristic). In case of a tie, it selects
     * the variable with the highest degree (number of constraints incident to the variable) to further
     * reduce the branching factor in subsequent steps.
     *
     * For non-"legal lookahead" selections, the method may prioritize unassigned variables in order
     * of appearance or based on domain size and degree, depending on whether all solutions need to
     * be found or only one is required.
     *
     * @return The index of the selected unassigned variable, or -1 if no variables remain unassigned.
     */
    private int selectUnassignedVariable() {
        boolean useLegalLookahead =
            hasAllDiffConstraint
                && (variables.length >= 40
                || (!findAllMode && maxInitialDomainSize <= 20));

        if (useLegalLookahead) {
            int best = -1;
            int bestLegalCount = Integer.MAX_VALUE;
            int bestDegree = -1;

            for (int i = 0; i < assignment.length; i++) {
                if (assigned[i]) {
                    continue;
                }

                int degree = incidents[i].size();
                int legalCount = countLegalValues(i, bestLegalCount);

                if (legalCount == 0) {
                    return i;
                }
                if (legalCount < bestLegalCount || (legalCount == bestLegalCount && degree > bestDegree)) {
                    best = i;
                    bestLegalCount = legalCount;
                    bestDegree = degree;
                    if (bestLegalCount == 1) {
                        break;
                    }
                }
            }

            return best;
        }

        if (!findAllMode) {
            for (int i = 0; i < assignment.length; i++) {
                if (!assigned[i]) {
                    return i;
                }
            }
            return -1;
        }

        int best = -1;
        int bestDomainSize = Integer.MAX_VALUE;
        int bestDegree = -1;

        for (int i = 0; i < assignment.length; i++) {
            if (assigned[i]) {
                continue;
            }

            int domainSize = variables[i].domain.size();
            int degree = incidents[i].size();

            if (domainSize < bestDomainSize || (domainSize == bestDomainSize && degree > bestDegree)) {
                best = i;
                bestDomainSize = domainSize;
                bestDegree = degree;
                if (bestDomainSize == 1) {
                    break;
                }
            }
        }

        return best;
    }

    /**
     * Counts the number of legal values from the domain of the specified variable that can be assigned
     * without violating any constraints. The counting process stops early if the number of legal values
     * reaches or exceeds the specified cutoff.
     *
     * @param varIndex The index of the variable whose domain is checked for legal values.
     * @param cutoff The threshold for the number of legal values to count. If this threshold is met,
     *               the counting will terminate early.
     * @return The number of legal values in the variable's domain, up to the cutoff limit.
     */
    private int countLegalValues(int varIndex, int cutoff) {
        int legal = 0;
        for (int value : variables[varIndex].domain) {
            if (assignAndCheck(varIndex, value)) {
                legal++;
                unassign(varIndex, value);
                if (legal >= cutoff) {
                    break;
                }
            }
        }
        return legal;
    }

    /**
     * Assigns a value to a variable and checks if the assignment violates any constraints.
     * If a violation is detected, the assignment is undone, and the method returns false.
     * The method ensures constraints are updated during the assignment and rollback phases.
     *
     * @param varIndex The index of the variable to assign the value to.
     * @param value The value to assign to the variable.
     * @return {@code true} if the assignment does not violate any constraints;
     *         {@code false} otherwise.
     */
    private boolean assignAndCheck(int varIndex, int value) {
        assignment[varIndex] = value;
        assigned[varIndex] = true;

        List<IncidentRef> refs = incidents[varIndex];
        for (int i = 0; i < refs.size(); i++) {
            IncidentRef ref = refs.get(i);
            ref.constraint.onAssign(ref.position, value);
            if (ref.constraint.isViolated(assignment, assigned)) {
                for (int j = 0; j <= i; j++) {
                    IncidentRef undoRef = refs.get(j);
                    undoRef.constraint.onUnassign(undoRef.position, value);
                }
                assigned[varIndex] = false;
                return false;
            }
        }

        return true;
    }

    private void unassign(int varIndex, int value) {
        List<IncidentRef> refs = incidents[varIndex];
        for (IncidentRef ref : refs) {
            ref.constraint.onUnassign(ref.position, value);
        }
        assigned[varIndex] = false;
    }

}
