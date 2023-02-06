package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum TargetTypeEnum {
    CONTEXT("context"),
    SEARCH("search");

    private String value;

    TargetTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
