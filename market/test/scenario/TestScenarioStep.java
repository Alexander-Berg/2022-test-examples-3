package ru.yandex.market.test.scenario;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Required;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public class TestScenarioStep {
    private String name;
    private String description;
    private Function<TestScenarioContext, Boolean> action;

    public TestScenarioStep() {
    }

    public TestScenarioStep(String name) {
        this.name = name;
    }

    public TestScenarioStep(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public TestScenarioStep(String name, Function<TestScenarioContext, Boolean> action) {
        this.name = name;
        this.action = action;
    }

    public TestScenarioStep(String name, String description, Function<TestScenarioContext, Boolean> action) {
        this.name = name;
        this.description = description;
        this.action = action;
    }

    public boolean make(TestScenarioContext context) {
        return action.apply(context);
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

    public Function<TestScenarioContext, Boolean> getAction() {
        return action;
    }

    public void setAction(Function<TestScenarioContext, Boolean> action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestScenarioStep that = (TestScenarioStep) o;

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
            return "step{" + "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        } else {
            return "step{name='" + name + "'}";
        }
    }
}
