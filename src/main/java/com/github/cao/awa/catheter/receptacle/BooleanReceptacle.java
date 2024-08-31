package com.github.cao.awa.catheter.receptacle;

public final class BooleanReceptacle {
    private boolean target;

    public BooleanReceptacle(boolean target) {
        this.target = target;
    }

    public static BooleanReceptacle of() {
        return of(false);
    }

    public static BooleanReceptacle of(boolean target) {
        return new BooleanReceptacle(target);
    }

    public boolean get() {
        return this.target;
    }

    public BooleanReceptacle set(boolean target) {
        this.target = target;
        return this;
    }
}
