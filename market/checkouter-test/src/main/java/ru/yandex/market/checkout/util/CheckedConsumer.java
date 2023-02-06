package ru.yandex.market.checkout.util;

/**
 * @author Nikolai Iusiumbeli
 * date: 20/04/2018
 */
public interface CheckedConsumer<T> {

    void consume(T t) throws Exception;
}
