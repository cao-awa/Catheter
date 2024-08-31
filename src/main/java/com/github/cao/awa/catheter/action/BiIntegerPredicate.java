package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiIntegerPredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param i1 the first input argument
     * @param i2 the second input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(int i1, int i2);
}