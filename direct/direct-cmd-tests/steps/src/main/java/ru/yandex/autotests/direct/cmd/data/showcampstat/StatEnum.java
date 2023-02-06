package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum StatEnum {
    STAT_ON("1"),
    STAT_OFF("0");

    String value;

    StatEnum(String value) {
        this.value = value;
    }
    public String toString() {
        return value;
    }
}
