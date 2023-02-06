package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum TargetTypeEnum {
    SEARCH("search"),
    CONTEXT("context");

    private String value;

    TargetTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
