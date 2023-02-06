package ru.yandex.direct.operation.testing.entity;

import java.util.List;

import ru.yandex.direct.model.Model;

public class Rule implements Model {
    private RuleType type;
    private List<Goal> goals;

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public List<Goal> getGoals() {
        return goals;
    }

    public void setGoals(List<Goal> goals) {
        this.goals = goals;
    }

    public Rule withType(RuleType type) {
        this.type = type;
        return this;
    }

    public Rule withGoals(List<Goal> goals) {
        this.goals = goals;
        return this;
    }
}
