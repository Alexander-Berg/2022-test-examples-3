package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum StatGroupEnum {
    WEEK("week"),
    DAY("day"),
    MONTH("month"),
    YEAR("year"),
    PERIOD("none");

    private String value;

    StatGroupEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
