# Assignment 2 Template
***CSE3300 Algorithms for NP-Hard Problems G***

This is a template project for Assignment 2, which you can use for local development for *Part 2: Solving*.

The project mimics the setup on WebLab; you can decide to keep all of your work on WebLab and use its group collaboration feature to work together, but we provide you with this template if you prefer to work in IntelliJ. You can create a ***private*** GitHub repo to collaborate with your partner. If we find a *public* GitHub repo with your implementation of the assignment, you will be marked for fraud.

### Setup

You might need to manually mark the `src`-directory as *Sources Root* and the `test`-directory as *Test Sources Root*. You can do so by right-clicking the directories in the Project view on the left side of your screen, and navigating to *Mark Directory As*.

### Contents

The project contains:
- *Five problem classes (`ToyExample`, `Production`, `Sudoku`, `FamilyDinner`, `NQueens`) to write your models in.* \
  You should copy the models that you wrote for Part 1 here. Those you should still develop on WebLab, as that is the only place where you will have access to our hidden `Solver`-implementation.
- *A `Solver`-class to write your solver in.* \
  Here you should develop your own solver, which all five classes above should make use of. Keep in mind the rules your `Solver`-implementation should adhere to, as listed on WebLab!
- *Five test suite classes (`ToyExampleTest`, `ProductionTest`, `SudokuTest`, `FamilyDinnerTest`, `NQueensTest`)* \
  These are test suites you can use to test the correctness and performance of your `Solver`. These tests are set up in the exact same as on WebLab, you do not have to touch or change them. Keep in mind that your computer might be faster or slower than the WebLab servers, so the runtime here might not be representative of the runtime on WebLab. Your code should pass all tests on WebLab; you cannot receive points for passing these tests on your own computer.

### Submissions

To submit your work:
- Copy over the contents of all `solveProblem`-methods to their respective WebLab exercises for both *Part 1: Modelling* and *Part 2: Solving*. Make sure the `solveProblem`-code for each problem is ***exactly*** the same in both parts; we have automatic tests that assert this, and if you have any differences, this could lead to a deduction in points. If you defined any helper methods in the `Solution`-class, copy those over too.
- Copy over your full `Solver`-implementation and paste them at the bottom of each of your WebLab submissions in *Part 2: Solving*. Make sure the implementation of your `Solver` is exactly the same in each of the five exercises; again, we have automatic tests that assert this.

---

Have fun! :)

### Creating your Solver
Your Solver should be able to solve any problem that can be defined according to the modelling specifications in Part 1. As a reminder, these specifications are repeated below (Section The Solver framework).

We recommend you to follow these steps to build up your Solver:

Let your Solver recursively or iteratively brute-force all possible solutions, and only return the ones that do not break any of the constraints.
Use inference to reduce the search space. Specifically, write propagation algorithms for each of the different constraint types, and use those to reduce the domains of the variables.
Further optimize your code through different techniques and your own creativity. Think about things like variable selection, pruning, and whether your propagation(s) can be done more efficiently.
You have a lot of freedom over how to implement your Solver, but we do have a few ground rules:

All documented methods must exist.
All classes and methods documented below must exist, with the exact same signature (name, arguments and return type). All of these are already provided in the template.
Use one solver for all problems.
The implementation of Solver (and of Variable and all Constraints) must be the same over all exercises in Part 2. You are not allowed to use different implementations for different problems, e.g. because a particular implementation is more optimized for a given problem. The only code that can be different in the exercise is the code in the Solution-class, where the model is created.
If you make a change to your Solver in an exercise, make sure to copy it over to all other exercises in Part 2 as well, or else you might get flagged for review.
Reuse your Part 1 models.
In each exercise, you must use the exact same solveProblem-code in Part 2 as you did in the corresponding exercise in Part 1; if your code wouldn’t compile in Part 1, it should not be used in Part 2.
If you change your model in Part 2, make sure to copy it over to Part 1, or else you might get flagged for review.
Do not rely on problem-specific logic.
It is not allowed to optimize your Solver specifically for certain problems. For example, your Solver should not solve Sudoku problems by reconstructing the original Sudoku from the constraints it is provided, and using a specialized algorithm for it. All your code should be applicable for any kind of problem that can be modeled with the constraints.
We will manually go over your code to make sure you adhere to these rules.
Breaking any of these rules could lead to a deduction in points, possibly all points.

We have provided a template in Toy Example (Part 2) to get you started. You are free to deviate from this template or even start from scratch, as long as you adhere to the above rules.

### The Solver framework
When constructing a Solver, you provide an array of variables and an array of constraints on these variables (see details further below). You can then call one of two methods:

#### int[] findOneSolution()

Returns the first solution the solver can find that satisfies the given constraints. The solution is given as an array of integers, representing the values assigned to each of the variables, in respective order. Once the solver finds a solution, it immediately stops searching for more.
Returns null if it concludes no feasible solution exists.

#### List<int[]> findAllSolutions()

Returns a list of all solutions the solver can find that satisfy the given constraints. It only stops searching after concluding no more solutions can be found.

    class Solver {
      public Solver(
      Variable[] variables,
      Constraint[] constraints
      );
      
          public int[] findOneSolution();
          public List<int[]> findAllSolutions();
      
          // ...
    }
### Variables
Variables are represented by Variable objects, of which the class is defined inside of the Solver class. To construct these, you will need to provide a List<Integer> representing the domain of the variable, i.e. all values that it could possibly be assigned.

    class Solver {
    // ...
    
        static class Variable {
            public Variable(
                List<Integer> domain
            );
        }
    
        // ...
    }
### Constraints
The provided solver only supports three different types of constraints, listed below. Each of these constraints extends the abstract Constraint class:

    class Solver {
    // ...
    
        static abstract class Constraint {}
    
        // ...
    }

The first type of constraint is the Not-Equals constraint. It takes two variables (x1
and x2
) and a constant (c
), and ensures that the following (non-)equation holds:
x1≠x2+c

    class Solver {
    // ...
    
        static class NotEqConstraint extends Constraint {
            public NotEqConstraint(
                Variable x1,
                Variable x2,
                int c
            );
        }
    
        // ...
    }

The second type of constraint is the All-Different constraint. It takes a list of any number of variables (xi
) and ensures that no pair of two variables will be equal in value:
AllDifferent(x1,…,xn)

    class Solver {
    // ...

      static class AllDiffConstraint extends Constraint {
          public AllDiffConstraint(
              Variable[] xs
          );
      }

    // ...
    }

The third type of constraint is the Inequality constraint. It takes a list of variables (xi
), a list of respective integer weights (wi
), and a constant c
, and ensures that the following inequality holds:
w1x1+…+wnxn≥c

    class Solver {
    // ...
    
        static class IneqConstraint extends Constraint {
            public IneqConstraint(
                Variable[] xs,
                int[] ws,
                int c
            );
        }
    }