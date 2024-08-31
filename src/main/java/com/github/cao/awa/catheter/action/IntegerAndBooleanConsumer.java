package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBooleanConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param i the first input argument
     * @param b the second input argument
     */
    void accept(int i, boolean b);
}