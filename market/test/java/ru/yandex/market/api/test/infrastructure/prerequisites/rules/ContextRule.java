package ru.yandex.market.api.test.infrastructure.prerequisites.rules;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites.ContextPrerequisite;

public class ContextRule implements MethodRule {

    private final ContextPrerequisite contextPrerequisite = new ContextPrerequisite();

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        contextPrerequisite.setUp(target);
        try {
            return base;
        } finally {
            contextPrerequisite.tearDown();
        }
    }
}
