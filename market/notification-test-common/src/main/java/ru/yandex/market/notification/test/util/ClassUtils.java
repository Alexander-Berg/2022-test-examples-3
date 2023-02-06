package ru.yandex.market.notification.test.util;

import java.lang.reflect.Modifier;

import javax.annotation.Nonnull;

import com.pushtorefresh.private_constructor_checker.PrivateConstructorChecker;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Базовый класс для unit-тестов утилитных классов.
 *
 * @author Vladislav Bauer
 */
public final class ClassUtils {

    private ClassUtils() {
        throw new UnsupportedOperationException();
    }


    public static void checkConstructor(@Nonnull final Class<?> utilsClass) {
        PrivateConstructorChecker
            .forClass(utilsClass)
            .expectedTypeOfException(UnsupportedOperationException.class)
            .check();

        assertThat(Modifier.isFinal(utilsClass.getModifiers()), equalTo(true));
    }

}
