package ru.yandex.market.javaframework.internal.environment.test;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnvironmentExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(EnvironmentExtension.class);

    private static final String IS_ANNOTATION_PRESENT = "annotationPresent";
    private static final String ORIG_ENV = "origEnv";
    private static final String ENV_PROP_NAME = "environment";

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        final boolean annotationPresent = context.getRequiredTestMethod().isAnnotationPresent(TestEnvironment.class);
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put(IS_ANNOTATION_PRESENT, annotationPresent);
        if (annotationPresent) {
            store.put(ORIG_ENV, System.getProperty(ENV_PROP_NAME));
            final TestEnvironment testEnvironment =
                context.getRequiredTestMethod().getAnnotation(TestEnvironment.class);
            System.setProperty(ENV_PROP_NAME, testEnvironment.value());
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        if (!store.get(IS_ANNOTATION_PRESENT, Boolean.class)) {
            return;
        }
        final String origEnv = store.get(ORIG_ENV, String.class);
        if (origEnv == null) {
            System.clearProperty(ENV_PROP_NAME);
        } else {
            System.setProperty(ENV_PROP_NAME, origEnv);
        }
    }
}
