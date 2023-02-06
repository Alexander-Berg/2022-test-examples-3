package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum PhraseStatusEnum {
    ADDED("added"),
    NONE("none");

    private String value;

    PhraseStatusEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
