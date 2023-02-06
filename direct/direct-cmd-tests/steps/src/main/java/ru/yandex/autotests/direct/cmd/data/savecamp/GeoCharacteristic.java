package ru.yandex.autotests.direct.cmd.data.savecamp;

import com.google.gson.annotations.SerializedName;

public class GeoCharacteristic {
    @SerializedName("is_negative")
    private String isNegative;

    public String getIsNegative() {
        return isNegative;
    }

    public GeoCharacteristic withIsNegative(String isNegative) {
        this.isNegative = isNegative;
        return this;
    }
}
