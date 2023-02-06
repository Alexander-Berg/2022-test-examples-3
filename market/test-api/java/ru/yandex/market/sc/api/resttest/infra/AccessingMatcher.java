package ru.yandex.market.sc.api.resttest.infra;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author valter
 */
public abstract class AccessingMatcher<T> extends BaseMatcher<T> {

    protected abstract void accessValue(T value);

    @Override
    public boolean matches(Object actual) {
        //noinspection unchecked
        accessValue((T) actual);
        return true;
    }

    @Override
    public void describeTo(Description description) {
    }

}
