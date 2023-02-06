package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.IgnoreCondition;
import ru.yandex.autotests.innerpochta.ignores.ConditionalIgnoreStatement;
import ru.yandex.autotests.innerpochta.ignores.IgnoreStatement;

import java.lang.reflect.Modifier;

/**
 * @author pavponn
 */
public class ConditionalIgnoreRule implements TestRule {

    private static boolean hasConditionalIgnoreAnnotation(Description description) {
        return description.getAnnotation(ConditionalIgnore.class) != null;
    }

    private static IgnoreCondition getIgnoreCondition(Description description) {
        ConditionalIgnore annotation = description.getAnnotation(ConditionalIgnore.class);
        return new IgnoreConditionCreator(annotation).create();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (base instanceof IgnoreStatement) {
            return base;
        }
        Statement result = base;
        if (hasConditionalIgnoreAnnotation(description)) {
            IgnoreCondition condition = getIgnoreCondition(description);
            condition.setFields(description);
            if (condition.isSatisfied()) {
                result = new ConditionalIgnoreStatement(condition);
            }
        }
        return result;
    }

    private static class IgnoreConditionCreator {

        private final Class<? extends IgnoreCondition> conditionType;

        IgnoreConditionCreator(ConditionalIgnore annotation) {
            this.conditionType = annotation.condition();
        }

        IgnoreCondition create() {
            checkConditionType();
            try {
                return createCondition();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private IgnoreCondition createCondition() throws Exception {
            return conditionType.newInstance();
        }

        private void checkConditionType() {
            if (!isConditionTypeStandalone()) {
                String msg =
                    "Conditional class '%s' is a member class "
                        + "but was not declared inside the test case using it.\n"
                        + "Make this class a static class, "
                        + "standalone class (by declaring it in it's own file) ";
                throw new IllegalArgumentException(String.format(msg, conditionType.getName()));
            }
        }

        private boolean isConditionTypeStandalone() {
            return !conditionType.isMemberClass() || Modifier.isStatic(conditionType.getModifiers());
        }
    }
}
