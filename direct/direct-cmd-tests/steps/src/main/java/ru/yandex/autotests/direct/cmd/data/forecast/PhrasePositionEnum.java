package ru.yandex.autotests.direct.cmd.data.forecast;

/**
 * Created by aleran on 29.09.2015.
 */
public enum PhrasePositionEnum {
    STD("std", "24"),
    FISRT_PLACE("first_place", "21"),
    FIRST_PREMIUM("first_premium", "11"),
    PREMIUM("premium","13"),
    SECOND_PREMIUM("second_premium","12");

    private String position;

    private String positionIndex;

    PhrasePositionEnum(String position, String positionIndex) {
        this.position = position;
        this.positionIndex = positionIndex;
    }

    public String getPosition() {
        return position;
    }

    public String getPositionIndex() {
        return positionIndex;
    }
}
