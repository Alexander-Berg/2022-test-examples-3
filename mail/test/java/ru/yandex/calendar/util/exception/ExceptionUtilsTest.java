package ru.yandex.calendar.util.exception;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author Daniel Brylev <dbrylev@yandex-team.ru>
 */
public class ExceptionUtilsTest {

    @Test
    public void unwrapReflection() {
        ru.yandex.bolts.internal.ReflectionException wrappedException =
                new ru.yandex.bolts.internal.ReflectionException(
                        new java.lang.reflect.InvocationTargetException(
                                new IllegalArgumentException()));

        ru.yandex.misc.reflection.InvocationTargetReflectionException wrappedError =
                new ru.yandex.misc.reflection.InvocationTargetReflectionException(
                        new AssertionError());

        ru.yandex.misc.reflection.ReflectionException emptyWrapper =
                new ru.yandex.misc.reflection.ReflectionException();

        NumberFormatException notWrapped =
                new NumberFormatException();

        Assert.isTrue(ExceptionUtils.unwrapReflection(wrappedException) instanceof IllegalArgumentException);
        Assert.isTrue(ExceptionUtils.unwrapReflection(wrappedError) instanceof AssertionError);
        Assert.isTrue(ExceptionUtils.unwrapReflection(notWrapped) instanceof NumberFormatException);
        Assert.isTrue(ExceptionUtils.unwrapReflection(emptyWrapper) instanceof ru.yandex.misc.reflection.ReflectionException);
    }
}
