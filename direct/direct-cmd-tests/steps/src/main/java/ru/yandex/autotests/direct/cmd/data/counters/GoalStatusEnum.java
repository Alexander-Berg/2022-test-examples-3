package ru.yandex.autotests.direct.cmd.data.counters;

public enum GoalStatusEnum {
    ACTIVE("Active"),
    DELETED("Deleted");

    private String text;

    GoalStatusEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
