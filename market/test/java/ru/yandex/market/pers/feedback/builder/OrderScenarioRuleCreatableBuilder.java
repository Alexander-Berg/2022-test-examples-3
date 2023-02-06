package ru.yandex.market.pers.feedback.builder;

import ru.yandex.market.pers.feedback.order.api.OrderScenarioRuleCreateable;
import ru.yandex.market.pers.feedback.order.api.ScenarioType;

public class OrderScenarioRuleCreatableBuilder {
    private ScenarioType scenarioType;
    private int grade;
    private Long questionId;
    private int priority;

    public static OrderScenarioRuleCreatableBuilder builder() {
        return new OrderScenarioRuleCreatableBuilder();
    }

    public OrderScenarioRuleCreatableBuilder withScenarioType(ScenarioType scenarioType) {
        this.scenarioType = scenarioType;
        return this;
    }

    public OrderScenarioRuleCreatableBuilder withGrade(int grade) {
        this.grade = grade;
        return this;
    }

    public OrderScenarioRuleCreatableBuilder withQuestionId(Long questionId) {
        this.questionId = questionId;
        return this;
    }

    public OrderScenarioRuleCreatableBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public OrderScenarioRuleCreateable build() {
        return new OrderScenarioRuleCreateable(scenarioType, grade, questionId, priority);
    }
}
