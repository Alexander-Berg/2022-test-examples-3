package ru.yandex.market.yql_test.test_listener;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class YqlAbstractTestListener extends AbstractTestExecutionListener {

    protected static class CtxWrapper {
        private final TestContext ctx;

        public CtxWrapper(TestContext ctx) {
            this.ctx = ctx;
        }

        @NotNull
        <T> T getSpringBean(Class<T> clazz) {
            return ctx.getApplicationContext().getBean(clazz);
        }

        protected <T> T setToContext(T object) {
            ctx.setAttribute(object.getClass().getName(), object);
            return object;
        }

        protected <T> Optional<T> getFromContext(Class<T> clazz) {
            return YqlAbstractTestListener.getFromContext(ctx, clazz);
        }
    }

    protected static String getRequiredProperty(TestContext testContext, String s) {
        return testContext.getApplicationContext()
                .getEnvironment().getRequiredProperty(s);
    }

    protected static <T> T setToContext(TestContext testContext, T object) {
        testContext.setAttribute(object.getClass().getName(), object);
        return object;
    }

    @SuppressWarnings("unchecked")
    protected static <T> Optional<T> getFromContext(TestContext testContext, Class<T> clazz) {
        return Optional.ofNullable((T) testContext.getAttribute(clazz.getName()));
    }

    protected String read(TestContext testContext, String name) {
        InputStream stream = testContext.getTestClass()
                .getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalStateException("Unable to read " + name);
        }
        return new Scanner(stream, StandardCharsets.UTF_8)
                .useDelimiter("\\A")
                .next();
    }
}
