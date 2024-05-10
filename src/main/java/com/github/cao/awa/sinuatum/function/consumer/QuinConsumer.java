package com.github.cao.awa.sinuatum.function.consumer;

import java.util.Objects;

@FunctionalInterface
public interface QuinConsumer<A, B, C, D, E> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @param d the quad input argument
     * @param e the quin input argument
     */
    void accept(A a, B b, C c, D d, E e);

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
    default QuinConsumer<A, B, C, D, E> andThen(QuinConsumer<A, B, C, D, E> after) {
        Objects.requireNonNull(after);
        return (a, b, c, d, e) -> {
            accept(a, b, c, d, e);
            after.accept(a, b, c, d, e);
        };
    }
}
