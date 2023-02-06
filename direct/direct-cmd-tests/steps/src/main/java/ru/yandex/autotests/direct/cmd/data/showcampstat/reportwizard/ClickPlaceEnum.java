package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum ClickPlaceEnum {
    TITLE("title"),
    VCARD("vcard"),
    UNDEFINED("undefined"),
    DISPLAY_HREF("display_href"),
    BUTTON("button"),
    SITELINK1("sitelink1"),
    SITELINK2("sitelink2"),
    SITELINK3("sitelink3"),
    SITELINK4("sitelink4");

    private String value;

    ClickPlaceEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
