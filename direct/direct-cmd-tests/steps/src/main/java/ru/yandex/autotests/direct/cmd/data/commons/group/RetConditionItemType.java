package ru.yandex.autotests.direct.cmd.data.commons.group;

public enum RetConditionItemType {
    OR("or"),
    NOT("not"),
    ALL("all");

    private String value;

    RetConditionItemType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
