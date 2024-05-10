package com.github.cao.awa.sinuatum.manipulate;

import com.github.cao.awa.sinuatum.function.ecception.function.ExceptingFunction;
import com.github.cao.awa.sinuatum.function.ecception.runnable.ExceptingRunnable;
import com.github.cao.awa.sinuatum.function.ecception.supplier.ExceptingSupplier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Manipulate<I, T> {
    private ExceptingFunction<I, T, Throwable> action;
    private Map<Class<? extends Throwable>, Consumer<? extends Throwable>> exceptionHandlers = new HashMap<>();

    public Manipulate(ExceptingFunction<I, T, Throwable> action) {
        this.action = action;
    }

    public static Manipulate<?, ?> trys(ExceptingRunnable<Throwable> action) {
        return new Manipulate<>((i) -> {
            action.run();
            return null;
        });
    }

    public static <X> Manipulate<?, X> get(ExceptingSupplier<X, Throwable> supplier) {
        return new Manipulate<>((i) -> supplier.get());
    }

    public static <X, Y> Manipulate<X, Y> get(ExceptingFunction<X, Y, Throwable> supplier) {
        return new Manipulate<>(supplier);
    }

    public <E extends Throwable> Manipulate<I, T> catching(Class<E> target, Consumer<E> handler) {
        this.exceptionHandlers.put(target, handler);
        return this;
    }

    public static <E extends Throwable, R extends Throwable> void reThrow(ExceptingRunnable<E> runnable, Class<E> specifiedType, Function<E, R> makeException, Function<Throwable, R> whenOther) throws R {
        new Manipulate<>((i) -> {
            runnable.run();
            return null;
        }).reThrow(specifiedType, makeException, whenOther);
    }

    public static <E extends Throwable, R extends Throwable> void reThrow(ExceptingRunnable<E> runnable, Class<E> specifiedType, Function<E, R> makeException, Consumer<Throwable> whenOther) throws R {
        new Manipulate<>((i) -> {
            runnable.run();
            return null;
        }).reThrow(specifiedType, makeException, whenOther);
    }

    public static <E extends Throwable, R extends Throwable> void reThrow(ExceptingRunnable<E> runnable, Class<E> specifiedType, Function<E, R> makeException) throws R {
        new Manipulate<>((i) -> {
            runnable.run();
            return null;
        }).reThrow(specifiedType, makeException);
    }

    public static <E extends Throwable, R extends Throwable, X> X reThrow(ExceptingSupplier<X, E> runnable, Class<E> specifiedType, Function<E, R> makeException, Function<Throwable, R> whenOther) throws R {
        return new Manipulate<>((i) -> runnable.get()).reThrow(specifiedType, makeException, whenOther);
    }

    public static <E extends Throwable, R extends Throwable, X> X reThrow(ExceptingSupplier<X, E> runnable, Class<E> specifiedType, Function<E, R> makeException, Consumer<Throwable> whenOther) throws R {
        return new Manipulate<>((i) -> runnable.get()).reThrow(specifiedType, makeException, whenOther);
    }

    public static <E extends Throwable, R extends Throwable, X> X reThrow(ExceptingSupplier<X, E> runnable, Class<E> specifiedType, Function<E, R> makeException) throws R {
        return new Manipulate<>((i) -> runnable.get()).reThrow(specifiedType, makeException);
    }

    public <E extends Throwable, R extends Throwable> T reThrow(Class<E> specifiedType, Function<E, R> makeException, Function<Throwable, R> whenOther) throws R {
        try {
            return this.action.apply(null);
        } catch (Throwable e) {
            E specifiedEx;
            try {
                specifiedEx = specifiedType.cast(e);
            } catch (Exception ignored) {
                throw whenOther.apply(e);
            }
            throw makeException.apply(specifiedEx);
        }
    }

    public <E extends Throwable, R extends Throwable> T reThrow(Class<E> specifiedType, Function<E, R> makeException, Consumer<Throwable> whenOther) throws R {
        try {
            return this.action.apply(null);
        } catch (Throwable e) {
            E specifiedEx;
            try {
                specifiedEx = specifiedType.cast(e);
            } catch (Exception ignored) {
                whenOther.accept(e);
                return null;
            }
            throw makeException.apply(specifiedEx);
        }
    }

    public <E extends Throwable, R extends Throwable> T reThrow(Class<E> specifiedType, Function<E, R> makeException) throws R {
        try {
            return this.action.apply(null);
        } catch (Throwable e) {
            E specifiedEx;
            try {
                specifiedEx = specifiedType.cast(e);
            } catch (Exception ignored) {
                return null;
            }
            throw makeException.apply(specifiedEx);
        }
    }

    public void execute() {
        try {
            this.action.apply(null);
        } catch (Throwable throwable) {
            Consumer<? extends Throwable> handler = this.exceptionHandlers.get(throwable.getClass());
            if (handler != null) {
                handler.accept(cast(throwable));
            }
        }
    }

    public T get() {
        try {
            return this.action.apply(null);
        } catch (Throwable throwable) {
            Consumer<? extends Throwable> handler = this.exceptionHandlers.get(throwable.getClass());
            if (handler != null) {
                handler.accept(cast(throwable));
            }
        }

        return null;
    }

    public T operate(I input) {
        try {
            return this.action.apply(input);
        } catch (Throwable throwable) {
            Consumer<? extends Throwable> handler = this.exceptionHandlers.get(throwable.getClass());
            if (handler != null) {
                handler.accept(cast(throwable));
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <R> R cast(Object object) {
        return (R) object;
    }
}
