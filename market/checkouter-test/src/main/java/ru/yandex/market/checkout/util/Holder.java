package ru.yandex.market.checkout.util;

/**
 * @author mkasumov
 */
public class Holder<T> implements Has<T> {

    private T data;

    public Holder(T data) {
        this.data = data;
    }

    public Holder() {
        data = null;
    }

    public static <T> Holder<T> empty() {
        return new Holder<>();
    }

    @Override
    public T get() {
        return data;
    }

    @Override
    public T set(T data) {
        return this.data = data;
    }
}
