package ru.yandex.autotests.direct.cmd.data.stepzero;

public enum StepZeroProcessClientTypeEnum {

    CLIENT("client"),
    SUBCLIENT("subclient");

    private String type;

    StepZeroProcessClientTypeEnum(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
