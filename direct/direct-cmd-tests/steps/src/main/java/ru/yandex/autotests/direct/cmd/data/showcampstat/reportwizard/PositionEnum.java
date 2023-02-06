package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum PositionEnum {
    PRIME("prime"),
    NON_PRIME("non-prime");

    private String value;

    PositionEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
