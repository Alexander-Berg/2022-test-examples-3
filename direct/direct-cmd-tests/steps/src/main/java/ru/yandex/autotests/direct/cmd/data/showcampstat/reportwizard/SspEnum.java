package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum SspEnum {
    YANDEX("Яндекс и РСЯ");

    private String value;

    SspEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
