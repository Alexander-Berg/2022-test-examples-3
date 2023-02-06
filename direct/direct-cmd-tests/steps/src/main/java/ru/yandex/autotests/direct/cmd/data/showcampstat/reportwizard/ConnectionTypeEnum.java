package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum ConnectionTypeEnum {
    MOBILE("mobile"),
    STATIONARY("stationary");

    private String value;

    ConnectionTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
