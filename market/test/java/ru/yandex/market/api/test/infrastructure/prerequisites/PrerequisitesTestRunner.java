package ru.yandex.market.api.test.infrastructure.prerequisites;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class PrerequisitesTestRunner extends BlockJUnit4ClassRunner {

    public static final String TARGET_CLASS_FIELD_NAME = "target";
    private final PrerequisiteStatementDecorator decorator = new PrerequisiteStatementDecorator();

    public PrerequisitesTestRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        final Description description = describeChild(method);
        if (isIgnored(method)) {
            notifier.fireTestIgnored(description);
        } else {
            final Statement statement = methodBlock(method);
            final Object testClass = ReflexUtils.getFieldValue(statement, TARGET_CLASS_FIELD_NAME);

            runLeaf(decorator.decorate(statement, testClass, method),
                description, notifier);
        }
    }
}
