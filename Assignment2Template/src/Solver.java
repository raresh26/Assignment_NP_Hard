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
        int rhs;
        int currentSum;
        int maxRemaining;
        int[] bestContrib;

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
                currentSum += ws[pos] * value;
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
                currentSum -= ws[pos] * value;
                maxRemaining += bestContrib[pos];
            }
        }

        boolean isViolated(Integer[] assignment) {
            if (type == TYPE_NOTEQ) {
                Integer v1 = assignment[x1];
                Integer v2 = assignment[x2];
                return v1 != null && v2 != null && v1 == v2 + c;
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

    private Integer[] assignment;
    private IdentityHashMap<Variable, Integer> variableToIndex;
    private List<IncidentRef>[] incident;
    private IdentityHashMap<Constraint, CompiledConstraint> compiledByOriginal;
    private boolean findAllMode;

    private int[] domainMin;
    private int[] domainMax;

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

        int n = variables.length;
        this.assignment = new Integer[n];
        this.variableToIndex = new IdentityHashMap<>();
        for (int i = 0; i < n; i++) {
            variableToIndex.put(variables[i], i);
        }

        this.domainMin = new int[n];
        this.domainMax = new int[n];
        for (int i = 0; i < n; i++) {
            domainMin[i] = domainMin(variables[i]);
            domainMax[i] = domainMax(variables[i]);
        }

        this.incident = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            incident[i] = new ArrayList<>();
        }

        this.compiledByOriginal = new IdentityHashMap<>();
        buildCompiledConstraints();
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

    private int selectUnassignedVariable() {
        if (!findAllMode || variables.length >= 40) {
            int best = -1;
            int bestLegalCount = Integer.MAX_VALUE;
            int bestDegree = -1;

            for (int i = 0; i < assignment.length; i++) {
                if (assignment[i] != null) {
                    continue;
                }

                int degree = incident[i].size();
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

        int best = -1;
        int bestDomainSize = Integer.MAX_VALUE;
        int bestDegree = -1;

        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] != null) {
                continue;
            }

            int domainSize = variables[i].domain.size();
            int degree = incident[i].size();

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

    private boolean assignAndCheck(int varIndex, int value) {
        assignment[varIndex] = value;

        List<IncidentRef> refs = incident[varIndex];
        for (int i = 0; i < refs.size(); i++) {
            IncidentRef ref = refs.get(i);
            ref.constraint.onAssign(ref.position, value);
            if (ref.constraint.isViolated(assignment)) {
                for (int j = 0; j <= i; j++) {
                    IncidentRef undoRef = refs.get(j);
                    undoRef.constraint.onUnassign(undoRef.position, value);
                }
                assignment[varIndex] = null;
                return false;
            }
        }

        return true;
    }

    private void unassign(int varIndex, int value) {
        List<IncidentRef> refs = incident[varIndex];
        for (IncidentRef ref : refs) {
            ref.constraint.onUnassign(ref.position, value);
        }
        assignment[varIndex] = null;
    }

    private boolean isConsistentAfterAssign(int varIndex) {
        for (IncidentRef ref : incident[varIndex]) {
            if (ref.constraint.isViolated(assignment)) {
                return false;
            }
        }
        return true;
    }

    private boolean violates(Constraint c) {
        CompiledConstraint compiled = compiledByOriginal.get(c);
        if (compiled == null) {
            return false;
        }
        return compiled.isViolated(assignment);
    }

    private void buildCompiledConstraints() {
        for (Constraint c : constraints) {
            if (c instanceof NotEqConstraint) {
                NotEqConstraint nc = (NotEqConstraint) c;
                CompiledConstraint cc = new CompiledConstraint();
                cc.type = TYPE_NOTEQ;
                cc.x1 = variableIndex(nc.x1);
                cc.x2 = variableIndex(nc.x2);
                cc.c = nc.c;
                compiledByOriginal.put(c, cc);
                addIncident(cc.x1, cc, -1);
                addIncident(cc.x2, cc, -1);
                continue;
            }

            if (c instanceof AllDiffConstraint) {
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

                compiledByOriginal.put(c, cc);
                for (int i = 0; i < cc.vars.length; i++) {
                    addIncident(cc.vars[i], cc, i);
                }
                continue;
            }

            if (c instanceof IneqConstraint) {
                IneqConstraint ic = (IneqConstraint) c;
                CompiledConstraint cc = new CompiledConstraint();
                cc.type = TYPE_INEQ;
                cc.vars = new int[ic.xs.length];
                cc.ws = Arrays.copyOf(ic.ws, ic.ws.length);
                cc.rhs = ic.c;
                cc.bestContrib = new int[ic.xs.length];
                cc.currentSum = 0;
                cc.maxRemaining = 0;

                for (int i = 0; i < ic.xs.length; i++) {
                    int varIndex = variableIndex(ic.xs[i]);
                    cc.vars[i] = varIndex;
                    int w = cc.ws[i];
                    int best = 0;
                    if (w > 0) {
                        best = w * domainMax[varIndex];
                    } else if (w < 0) {
                        best = w * domainMin[varIndex];
                    }
                    cc.bestContrib[i] = best;
                    cc.maxRemaining += best;
                }

                compiledByOriginal.put(c, cc);
                for (int i = 0; i < cc.vars.length; i++) {
                    addIncident(cc.vars[i], cc, i);
                }
            }
        }
    }

    private void addIncident(int varIndex, CompiledConstraint constraint, int position) {
        incident[varIndex].add(new IncidentRef(constraint, position));
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

}
