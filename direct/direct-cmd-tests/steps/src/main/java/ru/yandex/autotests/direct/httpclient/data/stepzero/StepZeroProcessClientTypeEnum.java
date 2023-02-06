package ru.yandex.autotests.direct.httpclient.data.stepzero;

/**
 * Created by shmykov on 07.04.15.
 */
public enum StepZeroProcessClientTypeEnum {

    CLIENT("client"),
    SUBCLIENT("subclient");

    private String type;

    StepZeroProcessClientTypeEnum(String type) {
         this.type = type;
    }

    public String toSring() {
        return this.type;
    }
}