package ru.yandex.market.loyalty.core.utils;

import java.util.function.Function;

@FunctionalInterface
public interface SequenceCustomizer<T, B extends Builder<T>> {
    void change(long order, B builder);

    static <T, B extends Builder<T>, R> SequenceCustomizer<T, B> compose(
            Function<Long, R> mapper,
            Function<R, BuildCustomizer<T, B>> customizerFunction) {
        return (order, builder) ->
                customizerFunction.apply(mapper.apply(order)).change(builder);
    }
}
