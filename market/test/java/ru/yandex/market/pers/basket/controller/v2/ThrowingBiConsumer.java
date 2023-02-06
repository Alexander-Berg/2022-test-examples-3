package ru.yandex.market.pers.basket.controller.v2;

/**
 * @author ifilippov5
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws Exception;
}
