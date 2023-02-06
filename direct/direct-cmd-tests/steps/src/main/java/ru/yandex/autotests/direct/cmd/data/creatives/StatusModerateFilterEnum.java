package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

public enum StatusModerateFilterEnum {

    @SerializedName("New")
    NEW("New"),
    @SerializedName("Wait")
    WAIT("Wait"),
    @SerializedName("Yes")
    YES("Yes"),
    @SerializedName("No")
    NO("No");

    private String value;

    StatusModerateFilterEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
