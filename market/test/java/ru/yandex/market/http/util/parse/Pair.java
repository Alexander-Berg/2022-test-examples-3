package ru.yandex.market.http.util.parse;

/**
 * @author dimkarp93
 */
public class Pair<A, B> {
    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}
