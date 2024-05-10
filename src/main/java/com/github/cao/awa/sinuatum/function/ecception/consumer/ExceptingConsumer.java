package com.github.cao.awa.sinuatum.function.ecception.consumer;

import java.io.Serializable;
import java.util.Objects;

@FunctionalInterface
public interface ExceptingConsumer<A, EX extends Throwable> extends Serializable {
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the input argument
     */
    void accept(A a) throws EX;

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
    default ExceptingConsumer<A, EX> andThen(ExceptingConsumer<A, EX> after) {
        Objects.requireNonNull(after);
        return (a) -> {
            accept(a);
            after.accept(a);
        };
    }
}
