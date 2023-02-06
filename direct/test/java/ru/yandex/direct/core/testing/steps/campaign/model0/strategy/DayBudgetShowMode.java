package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

public enum DayBudgetShowMode {

    DEFAULT("default"),

    STRETCHED("stretched");

    private final String literal;

    DayBudgetShowMode(String literal) {
        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }
}
