package ru.yandex.direct.operation.testing.entity;

import java.util.List;

import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.model.ModelWithId;

public class RetargetingCondition implements ModelWithId {
    public static final ModelProperty<RetargetingCondition, String> NAME =
            ModelProperty.create(RetargetingCondition.class, "name", RetargetingCondition::getName, RetargetingCondition::setName);

    public static final ModelProperty<RetargetingCondition, String> DESCRIPTION =
            ModelProperty.create(RetargetingCondition.class, "description", RetargetingCondition::getDescription, RetargetingCondition::setDescription);

    private Long id;
    private String name;
    private String description;
    private List<Rule> rules;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public RetargetingCondition withId(Long id) {
        this.id = id;
        return this;
    }

    public RetargetingCondition withName(String name) {
        this.name = name;
        return this;
    }

    public RetargetingCondition withDescription(String description) {
        this.description = description;
        return this;
    }

    public RetargetingCondition withRules(List<Rule> rules) {
        this.rules = rules;
        return this;
    }
}
