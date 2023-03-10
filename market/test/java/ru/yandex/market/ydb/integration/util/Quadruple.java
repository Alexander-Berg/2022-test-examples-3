package ru.yandex.market.ydb.integration.util;

public class Quadruple<A, B, C, D> {
    private final A a;
    private final B b;
    private final C c;
    private final D d;

    public Quadruple(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    public D getD() {
        return d;
    }
}
