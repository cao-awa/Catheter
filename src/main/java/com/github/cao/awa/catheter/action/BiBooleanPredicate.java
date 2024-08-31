package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiBooleanPredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param b1 the first input argument
     * @param b2 the second input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(boolean b1, boolean b2);
}
