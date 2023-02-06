package ru.yandex.market.mbo.utils;

import java.util.Objects;

/**
 * @author anmalysh
 * @since 3/12/2019
 */
public class SameObjectEqualsWrapper<T> {
    private final T wrapped;

    public SameObjectEqualsWrapper(T wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SameObjectEqualsWrapper<?> that = (SameObjectEqualsWrapper<?>) o;
        return wrapped == that.wrapped;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrapped);
    }
}
