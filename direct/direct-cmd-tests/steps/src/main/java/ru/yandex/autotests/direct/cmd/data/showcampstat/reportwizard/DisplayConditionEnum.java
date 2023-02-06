package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum DisplayConditionEnum {
    PHRASES("phrases"),
    DYNAMIC("dynamic"),
    SYNONYM("synonym"),
    RETARGETING("retargeting"),
    AUTO_ADDED_PHRASES("auto-added-phrases"),
    PERFORMANCE("performance");

    private String value;

    DisplayConditionEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
