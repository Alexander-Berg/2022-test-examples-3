package ru.yandex.autotests.direct.cmd.data.commons.group;

public enum RetargetingType {
    INTERESTS("interests");

    private String value;

    RetargetingType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
