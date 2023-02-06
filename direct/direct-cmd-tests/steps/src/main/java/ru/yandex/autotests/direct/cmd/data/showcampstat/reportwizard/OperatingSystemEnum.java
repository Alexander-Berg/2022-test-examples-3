package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum OperatingSystemEnum {
    ANDROID("android"),
    IOS("ios"),
    OTHER("other");

    private String value;

    OperatingSystemEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
