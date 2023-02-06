package ru.yandex.autotests.direct.cmd.data.campaigns;

public enum ShowCampsTabsEnum {
    ALL("all"),
    ARCH("arch"),
    ACTIVE("active"),
    PLANNED("planned");

    ShowCampsTabsEnum(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}
