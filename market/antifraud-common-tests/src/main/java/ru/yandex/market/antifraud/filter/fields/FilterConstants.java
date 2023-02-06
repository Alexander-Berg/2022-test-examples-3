package ru.yandex.market.antifraud.filter.fields;

/**
 * Created by oroboros on 11.08.17.
 */
public enum FilterConstants {
    FILTER_0,
    FILTER_1,
    FILTER_2,
    FILTER_3,
    FILTER_4,
    FILTER_5,
    FILTER_6,
    FILTER_7,
    FILTER_8,
    FILTER_9,
    FILTER_10,
    FILTER_11,
    FILTER_12,
    FILTER_13,
    FILTER_14,
    FILTER_15,
    FILTER_16,
    FILTER_17,
    FILTER_18,
    FILTER_19,
    FILTER_20,
    FILTER_21,
    FILTER_22,
    FILTER_23,
    FILTER_24,
    FILTER_25,
    FILTER_26,
    FILTER_27,
    FILTER_28,
    FILTER_29,
    FILTER_30,
    FILTER_31,
    FILTER_32,
    FILTER_33,
    FILTER_34,
    FILTER_35,
    FILTER_36,
    FILTER_37,
    FILTER_38,
    FILTER_39,
    FILTER_40,
    FILTER_41,
    FILTER_42,
    FILTER_43,
    FILTER_44,
    FILTER_45,
    FILTER_46
    ;

    public int id() {
        return Integer.parseInt(this.name().split("_")[1]);
    }
}
