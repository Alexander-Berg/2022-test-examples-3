package ru.yandex.market.test.scenario;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public abstract class AbstractTestScenarioStep extends TestScenarioStep {
    public AbstractTestScenarioStep() {
    }

    public AbstractTestScenarioStep(String name) {
        super(name);
    }

    public AbstractTestScenarioStep(String name, String description) {
        super(name, description);
    }

    public abstract boolean make(TestScenarioContext context);
}
