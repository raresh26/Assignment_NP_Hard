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
        int numMaterialTypes = C.length;
        int numProductTypes = P.length;
        int[] solution = new int[numProductTypes];
        if (R <= 0) {
            return solution;
        }

        int[] ub = new int[numProductTypes];
        for (int j = 0; j < numProductTypes; j++) {
            int bound = P[j];
            if (r[j] <= 0) {
                ub[j] = 0;
                continue;
            }
            for (int i = 0; i < numMaterialTypes; i++) {
                if (c[i][j] > 0) {
                    bound = Math.min(bound, C[i] / c[i][j]);
                }
            }

            long revenueCap = ((long) R + r[j] - 1L) / r[j];
            if (revenueCap < bound) {
                bound = (int) revenueCap;
            }
            ub[j] = Math.max(0, bound);
        }

        List<Integer> activeProducts = new ArrayList<>();
        long optimisticRevenue = 0L;
        for (int j = 0; j < numProductTypes; j++) {
            if (ub[j] > 0 && r[j] > 0) {
                activeProducts.add(j);
                optimisticRevenue += (long) ub[j] * r[j];
            }
        }
        if (optimisticRevenue < R) {
            return null;
        }

        int[] order = sortedActiveProductOrder(activeProducts, C, c, ub, r);
        int activeCount = order.length;

        int[] greedySolution = greedyConstructSolution(order, C, c, ub, r, R, numProductTypes);
        if (greedySolution != null) {
            return greedySolution;
        }

        List<Solver.Variable> variables = new ArrayList<>(activeCount);
        for (int k = 0; k < activeCount; k++) {
            int j = order[k];
            List<Integer> domain = new ArrayList<>(ub[j] + 1);
            for (int x = ub[j]; x >= 0; x--) {
                domain.add(x);
            }
            variables.add(new Solver.Variable(domain));
        }

        List<Solver.Constraint> constraints = new ArrayList<>();
        Solver.Variable[] varsArr = variables.toArray(new Solver.Variable[0]);

        List<int[]> materialRows = new ArrayList<>();
        List<Integer> materialCaps = new ArrayList<>();
        for (int i = 0; i < numMaterialTypes; i++) {
            int[] row = new int[activeCount];
            boolean nonZero = false;
            for (int k = 0; k < activeCount; k++) {
                int j = order[k];
                row[k] = c[i][j];
                if (row[k] != 0) nonZero = true;
            }
            if (nonZero) {
                materialRows.add(row);
                materialCaps.add(C[i]);
            }
        }

        int materialConstraintCount = materialRows.size();
        boolean[] keepMaterial = new boolean[materialConstraintCount];
        Arrays.fill(keepMaterial, true);
        for (int i = 0; i < materialConstraintCount; i++) {
            if (!keepMaterial[i]) continue;
            for (int j = 0; j < materialConstraintCount; j++) {
                if (i == j || !keepMaterial[j]) continue;
                if (dominates(materialRows.get(i), materialCaps.get(i), materialRows.get(j), materialCaps.get(j))) {
                    keepMaterial[j] = false;
                }
            }
        }

        for (int i = 0; i < materialConstraintCount; i++) {
            if (!keepMaterial[i]) continue;
            int[] row = materialRows.get(i);
            int[] ws = new int[row.length];
            for (int k = 0; k < row.length; k++) {
                ws[k] = -row[k];
            }
            constraints.add(new Solver.IneqConstraint(varsArr, ws, -materialCaps.get(i)));
        }

        int[] revWs = new int[activeCount];
        for (int k = 0; k < activeCount; k++) {
            revWs[k] = r[order[k]];
        }
        constraints.add(new Solver.IneqConstraint(varsArr, revWs, R));

        Solver solver = new Solver(
            varsArr,
            constraints.toArray(new Solver.Constraint[0])
        );
        int[] result = solver.findOneSolution();

        if (result == null) {
            return null;
        }

        for (int k = 0; k < activeCount; k++) {
            int originalProduct = order[k];
            solution[originalProduct] = result[k];
        }
        return solution;
    }

    private static int[] sortedActiveProductOrder(
            List<Integer> activeProducts,
            int[] C,
            int[][] c,
            int[] ub,
            int[] r
    ) {
        Integer[] orderObj = activeProducts.toArray(new Integer[0]);
        double[] pressure = new double[r.length];
        for (int j : activeProducts) {
            double p = 0.0;
            for (int i = 0; i < C.length; i++) {
                p += ((double) c[i][j]) / (C[i] + 1.0);
            }
            pressure[j] = p;
        }

        Arrays.sort(orderObj, (a, b) -> {
            double scoreA = r[a] * (1.0 + pressure[a]);
            double scoreB = r[b] * (1.0 + pressure[b]);
            int cmpScore = Double.compare(scoreB, scoreA);
            if (cmpScore != 0) return cmpScore;

            int cmpPressure = Double.compare(pressure[b], pressure[a]);
            if (cmpPressure != 0) return cmpPressure;

            int cmpUb = Integer.compare(ub[a], ub[b]);
            if (cmpUb != 0) return cmpUb;

            return Integer.compare(a, b);
        });

        int[] order = new int[orderObj.length];
        for (int i = 0; i < orderObj.length; i++) {
            order[i] = orderObj[i];
        }
        return order;
    }

    private static int[] greedyConstructSolution(
            int[] order,
            int[] C,
            int[][] c,
            int[] ub,
            int[] r,
            int R,
            int numProductTypes
    ) {
        for (int pass = 0; pass < 2; pass++) {
            int[] capacities = Arrays.copyOf(C, C.length);
            int[] solution = new int[numProductTypes];
            long revenue = 0;

            for (int product : order) {
                int maxUnits = ub[product];
                for (int i = 0; i < C.length; i++) {
                    int need = c[i][product];
                    if (need > 0) {
                        maxUnits = Math.min(maxUnits, capacities[i] / need);
                    }
                }

                int take;
                if (pass == 0) {
                    long remaining = R - revenue;
                    if (remaining <= 0) {
                        break;
                    }
                    long needed = (remaining + r[product] - 1L) / r[product];
                    take = (int) Math.min(maxUnits, needed);
                } else {
                    take = maxUnits;
                }

                if (take <= 0) {
                    continue;
                }

                solution[product] = take;
                revenue += (long) take * r[product];
                for (int i = 0; i < C.length; i++) {
                    int need = c[i][product];
                    if (need > 0) {
                        capacities[i] -= take * need;
                    }
                }
            }

            if (revenue >= R) {
                return solution;
            }
        }
        return null;
    }

    private static boolean dominates(int[] rowA, int capA, int[] rowB, int capB) {
        if (capA > capB) {
            return false;
        }
        for (int k = 0; k < rowA.length; k++) {
            if (rowA[k] < rowB[k]) {
                return false;
            }
        }
        return true;
    }
}
