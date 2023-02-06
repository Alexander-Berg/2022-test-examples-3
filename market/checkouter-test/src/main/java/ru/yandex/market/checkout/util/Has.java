package ru.yandex.market.checkout.util;

/**
 * @author mkasumov
 */
public interface Has<T> {

    T get();

    T set(T data);
}
