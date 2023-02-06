package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;

import java.util.stream.Stream;

public enum GoalType {
    @SerializedName("goal")
    GOAL("goal"),
    @SerializedName("segment")
    SEGMENT("segment"),
    @SerializedName("audience")
    AUDIENCE("audience");

    private String value;

    GoalType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GoalType getEnumByValue(String value) {
        return Stream.of(values())
                .filter(t -> t.getValue().equals(value))
                .findFirst().orElse(null);
    }
}
