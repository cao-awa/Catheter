package com.github.cao.awa.sinuatum.function.consumer;

import java.util.Objects;

@FunctionalInterface
public interface QuadConsumer<A, B, C, D> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @param d the quad input argument
     */
    void accept(A a, B b, C c, D d);

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after
     *         the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     *
     */
    default QuadConsumer<A, B, C, D> andThen(QuadConsumer<A, B, C, D> after) {
        Objects.requireNonNull(after);
        return (a, b, c, d) -> {
            accept(a, b, c, d);
            after.accept(a, b, c, d);
        };
    }
}
