package ru.yandex.market.test.scenario;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public class TestScenario {
    private static final Logger log = LoggerFactory.getLogger(TestScenario.class);

    private String name;
    private String description;
    private List<TestScenarioStep> steps;

    public TestScenario() {
    }

    public TestScenario(String name, List<TestScenarioStep> steps) {
        this.name = name;
        this.steps = steps;
    }

    public TestScenario(String name, String description, List<TestScenarioStep> steps) {
        this.name = name;
        this.description = description;
        this.steps = steps;
    }

    public TestScenarioStep make() {
        TestScenarioContext context = new TestScenarioContext();
        log.debug("Making test scenario: " + this + "\n");
        TestScenarioStep problemStep = null;
        for (TestScenarioStep step : this.getSteps()) {
            log.debug("Making step: " + step);

            boolean success;
            log.debug("Context state before: " + context);
            long start = System.currentTimeMillis();
            try {
                success = step.make(context);
            } catch (Exception e) {
                log.debug("Exception while making step: " + step, e);
                success = false;
            }
            log.debug("Step time: " + (System.currentTimeMillis() - start));
            log.debug("Context state after: " + context);

            if (!success) {
                problemStep = step;
                log.debug("Step finished with problems: " + step + "\n");
                log.debug("Interrupting scenario: " + this);
                break;
            }
            log.debug("Step finished successfully: " + step + "\n");
        }
        log.debug("Scenario finished " + (problemStep == null ? "successfully" : "with problems") + ": " + this);
        return problemStep;
    }

    public String getName() {
        return name;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TestScenarioStep> getSteps() {
        return steps;
    }

    @Required
    public void setSteps(List<TestScenarioStep> steps) {
        this.steps = steps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestScenario that = (TestScenario) o;

        return name != null
                ? name.equals(that.name)
                : that.name == null
                && (description != null ? description.equals(that.description) : that.description == null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (name != null && description != null) {
            return "scenario{" + "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        } else {
            return "scenario{name='" + name + "'}";
        }
    }
}
