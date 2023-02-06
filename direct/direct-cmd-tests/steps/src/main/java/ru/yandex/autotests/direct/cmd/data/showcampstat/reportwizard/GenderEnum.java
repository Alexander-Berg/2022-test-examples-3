package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum GenderEnum {
    MALE("male"),
    FEMALE("female"),
    UNDEFINED("undefined");

    private String value;

    GenderEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
