package ru.yandex.market.sqb.test;

import java.lang.reflect.Modifier;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.pushtorefresh.private_constructor_checker.PrivateConstructorChecker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Утилитный класс для unit-тестов.
 *
 * @author Vladislav Bauer
 */
public final class TestUtils {

    private TestUtils() {
        throw new UnsupportedOperationException();
    }


    public static void checkConstructor(@Nonnull final Class<?> utilsClass) {
        PrivateConstructorChecker
                .forClass(utilsClass)
                .expectedTypeOfException(UnsupportedOperationException.class)
                .check();

        assertThat(Modifier.isFinal(utilsClass.getModifiers()), equalTo(true));
    }

    @SuppressWarnings("all")
    public static <T> void checkOptional(final Optional<T> optional, final T value) {
        assertThat(optional.orElse(null), equalTo(value));
    }

}
