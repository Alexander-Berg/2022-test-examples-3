package ru.yandex.autotests.innerpochta.ignores;

import org.junit.runners.model.Statement;
import ru.yandex.autotests.innerpochta.conditions.IgnoreCondition;

import static org.junit.Assume.assumeTrue;

/**
 * @author pavponn
 */
public class ConditionalIgnoreStatement extends Statement implements IgnoreStatement {

    private final IgnoreCondition condition;

    public ConditionalIgnoreStatement(IgnoreCondition condition) {
        this.condition = condition;
    }

    @Override
    public void evaluate() {
        assumeTrue(
            "Ignored by " + condition.getClass().getSimpleName() + " condition\n" +
                "Reason: " + condition.getMessage(),
            false
        );
    }
}
