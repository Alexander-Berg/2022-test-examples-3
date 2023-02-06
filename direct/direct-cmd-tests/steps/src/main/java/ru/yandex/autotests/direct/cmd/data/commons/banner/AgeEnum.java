package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

import java.util.stream.Stream;

/**
 * Created by aleran on 17.09.2015.
 */
public enum AgeEnum {
    @SerializedName("0+")
    ZERO_PLUS("0+"),
    @SerializedName("6+")
    SIX_PLUS("6+"),
    @SerializedName("12+")
    TWELVE_PLUS("12+"),
    @SerializedName("16+")
    SIXTEEN_PLUS("16+"),
    @SerializedName("18+")
    EIGHTEEN_PLUS("18+");

    private String value;

    AgeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AgeEnum getAgeEnumByValue(String value) {
       return Stream.of(AgeEnum.values()).filter(t -> t.getValue().equals(value)).findFirst().get();
    }
}
