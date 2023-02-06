package ru.yandex.market.antifraud.filter.fields;

/**
 * Created by oroboros on 14.08.17.
 */
public enum StateConstants {
    STATE_0,
    STATE_1,
    STATE_2,
    STATE_3,
    STATE_4;

    public int id() {
        return Integer.parseInt(this.name().split("_")[1]);
    }
}
