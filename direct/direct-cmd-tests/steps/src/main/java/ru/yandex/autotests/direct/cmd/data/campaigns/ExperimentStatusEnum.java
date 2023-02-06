package ru.yandex.autotests.direct.cmd.data.campaigns;

public enum ExperimentStatusEnum {
    NEW("New"),
    STARTED("Started"),
    STOPPED("Stopped");

    private String value;

    ExperimentStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
