package ru.yandex.market.common.test.matcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

/**
 * Матчер принимает на вход функцию с двумя аргументами, которая производит проверку матчинга самостоятельно.
 *
 * @author avetokhin 22.08.18.
 */
public class FunctionalMatcher<T> extends BaseMatcher<T> {

    private T expectedValue;
    private BiFunction<T, T, Boolean> matcher;

    private FunctionalMatcher(final T expectedValue, @Nonnull final BiFunction<T, T, Boolean> matcher) {
        this.expectedValue = expectedValue;
        this.matcher = matcher;
    }

    @Override
    public boolean matches(final Object item) {
        return matcher.apply(expectedValue, (T) item);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendValue(expectedValue);
    }

    /**
     * Создать матчер с указанными параметрами.
     *
     * @param expectedValue ожидаемое значение
     * @param matcher       функция матчинга, первым аргументом является ожидаемое значение, вторым актуальное
     * @param <T>           тип сравниваемых значений
     */
    public static <T> FunctionalMatcher<T> functionalMatcher(final T expectedValue,
                                                             @Nonnull final BiFunction<T, T, Boolean> matcher) {
        return new FunctionalMatcher<>(expectedValue, matcher);
    }
}
