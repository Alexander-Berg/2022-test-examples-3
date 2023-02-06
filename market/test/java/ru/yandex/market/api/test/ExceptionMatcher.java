package ru.yandex.market.api.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.lang.reflect.ParameterizedType;

/**
 * Матчер для проверки исключений
 *
 * Created by apershukov on 28.09.16.
 */
public abstract class ExceptionMatcher<T extends Throwable> extends BaseMatcher {

    private final Class<T> clazz;

    protected ExceptionMatcher() {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        this.clazz = (Class<T>)type.getActualTypeArguments()[0];
    }

    @Override
    public boolean matches(Object o) {
        return o != null && clazz.isInstance(o) && match(clazz.cast(o));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Корректное исключение");
    }

    protected abstract boolean match(T e);
}
