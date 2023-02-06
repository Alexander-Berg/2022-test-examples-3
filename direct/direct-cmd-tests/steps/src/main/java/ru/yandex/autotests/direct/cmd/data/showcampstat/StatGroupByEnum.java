package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum StatGroupByEnum {
    BANNER("banner"),
    ADGROUP("adgroup"),
    PHRASE("phrase"),
    IMAGE("image"),
    DEVICE_TYPE("device_type"),
    DATE("date"),
    ALL("adgroup, banner, phrase, image, device_type, date");

    private String value;

    StatGroupByEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
