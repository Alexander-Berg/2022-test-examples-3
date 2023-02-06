package ru.yandex.autotests.direct.cmd.data.campaigns;

public enum CampaignTypeEnum {

    TEXT("text"),
    MOBILE("mediaType"),
    DMO("performance");

    private String value;

    CampaignTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
