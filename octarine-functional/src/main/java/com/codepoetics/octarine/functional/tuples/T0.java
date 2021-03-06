package com.codepoetics.octarine.functional.tuples;

import java.util.function.Supplier;

public final class T0 {

    public static final T0 INSTANCE = new T0();

    private T0() {
    }

    public <A> T1<A> push(A a) {
        return T1.of(a);
    }

    public <R> R pack(Supplier<? extends R> supplier) {
        return supplier.get();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof T0;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "_|_";
    }
}
