package ru.yandex.calendar.test.generic;

import java.lang.reflect.Method;

import ru.yandex.calendar.test.Developer;
import ru.yandex.commune.test.random.TestContextManager2;
import ru.yandex.misc.cache.tl.TlCache;

/**
 * @author Stepan Koltsov
 */
public class CalendarTestContextManager extends TestContextManager2 {
    public CalendarTestContextManager(Class<?> testClass) {
        super(testClass);
    }

    @Override
    public void beforeTestClass() throws Exception {
        Developer.beforeTest();
        super.beforeTestClass();
    }

    @Override
    public void afterTestClass() throws Exception {
        super.afterTestClass();
        Developer.afterTest();
    }

    @Override
    public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
        TlCache.reset();
        TlCache.push();
        super.beforeTestMethod(testInstance, testMethod);
    }

    @Override
    public void afterTestMethod(Object testInstance, Method testMethod, Throwable exception) throws Exception {
        super.afterTestMethod(testInstance, testMethod, exception);
        TlCache.reset();
    }
}
