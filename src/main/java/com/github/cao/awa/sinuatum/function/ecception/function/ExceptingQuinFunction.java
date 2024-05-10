package com.github.cao.awa.sinuatum.function.ecception.function;

import java.util.Objects;

@FunctionalInterface
public interface ExceptingQuinFunction<A, B, C, D, E, R, EX extends Throwable> {
    /**
     * Applies this function to the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the quad function argument
     * @param e the quin function argument
     * @return the function result
     */
    R apply(A a, B b, C c, D d, E e) throws EX;

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ExceptingQuinFunction<A, B, C, D, E, V, EX> andThen(ExceptingFunction<? super R, ? extends V, EX> after) {
        Objects.requireNonNull(after);
        return (a, b, c, d, e) -> after.apply(apply(a, b, c, d, e));
    }
}
