package ru.yandex.market.api.test.infrastructure.prerequisites;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class PrerequisitesRule implements MethodRule {

    private final PrerequisiteStatementDecorator decorator = new PrerequisiteStatementDecorator();

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return decorator.decorate(base, target, method);
    }
}
