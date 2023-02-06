package ru.yandex.market.test.scenario.impl;

import java.util.Map;

import ru.yandex.market.test.scenario.AbstractTestScenarioStep;
import ru.yandex.market.test.scenario.TestScenarioContext;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 19.05.16
 */
public class ContextInitializerStep extends AbstractTestScenarioStep {
    private Map<String, Object> data;

    @Override
    public boolean make(TestScenarioContext context) {
        context.setData(data);
        return true;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
