package ru.yandex.market.test.scenario.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.test.scenario.AbstractTestScenarioStep;
import ru.yandex.market.test.scenario.TestScenarioContext;
import ru.yandex.market.test.scenario.TestScenarioStep;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 23.05.16
 */
public class MultiStep extends AbstractTestScenarioStep {
    private static final Logger log = LoggerFactory.getLogger(MultiStep.class);

    private List<TestScenarioStep> steps;

    @Override
    public boolean make(TestScenarioContext context) {
        for (TestScenarioStep step : steps) {
            if (!step.make(context)) {
                log.debug("Error on step " + step);
                return false;
            }
        }
        return true;
    }

    public void setSteps(List<TestScenarioStep> steps) {
        this.steps = steps;
    }
}
