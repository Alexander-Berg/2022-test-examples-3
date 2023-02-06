package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum QueryStatusEnum {
    ADDED("added"),
    NONE("none");

    private String value;

    QueryStatusEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
