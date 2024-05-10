package com.github.cao.awa.catheter.function;

import java.io.Serializable;

public interface ExceptingRunnable extends Serializable {
    void apply() throws Exception;
}
