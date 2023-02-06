package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum DeviceTypeEnum {
    MOBILE("mobile"),
    TABLET("tablet"),
    DESKTOP("desktop");

    private String value;

    DeviceTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
