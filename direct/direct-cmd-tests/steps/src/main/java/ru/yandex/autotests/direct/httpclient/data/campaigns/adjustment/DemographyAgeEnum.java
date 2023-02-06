package ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment;

/**
 * Created by aleran on 07.08.2015.
 */
public enum DemographyAgeEnum {

    ALL("all"),
    BETWEEN_0_AND_17("0-17"),
    BETWEEN_18_AND_24("18-24"),
    BETWEEN_25_AND_34("25-34"),
    BETWEEN_35_AND_44("35-44"),
    BETWEEN_45_AND_54("45-54"),
    MORE_THAN_55("55-"),
    UNDEFINED("undefined");

    private String key;

    DemographyAgeEnum(String key) {
        this.key = key;
    }

    public String getKey(){
        return key;
    }
}
