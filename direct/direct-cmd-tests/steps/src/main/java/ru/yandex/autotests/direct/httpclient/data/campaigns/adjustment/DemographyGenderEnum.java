package ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment;

/**
 * Created by aleran on 07.08.2015.
 */
public enum DemographyGenderEnum {

    ALL("all"),
    MALE("male"),
    FEMALE("female");

    private String key;

    DemographyGenderEnum(String key) {
        this.key = key;
    }

    public String getKey(){
        return key;
    }
}
