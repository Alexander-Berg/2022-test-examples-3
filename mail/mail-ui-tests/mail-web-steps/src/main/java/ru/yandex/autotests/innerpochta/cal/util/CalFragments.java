package ru.yandex.autotests.innerpochta.cal.util;

/**
 * @author cosmopanda
 */
public enum CalFragments {

    MONTH("month?%s"),
    DAY("day?%s"),
    WEEK("week?%s"),
    EVENT("event/%s"),
    SETTINGS_CAL("sidebar=editLayer"),
    SETTINGS_TZ("settings#setting-tz"),
    WIZARD("/?modal=welcomeWizard"),
    TZ_POPUP("/?modal=confirmTimezone"),
    LAYER_SETTING("?sidebar=editLayer&sidebarLayerId=%s"),
    SETTINGS_SIDEBAR("?sidebar=settings");

    private String fragment;

    CalFragments(String fragment) {
        this.fragment = fragment;
    }

    public String fragment(String... addToTheEnd) {
        return String.format(this.fragment, (Object[]) addToTheEnd);
    }

    public String makeUrlPart(String... addToTheEnd) {
        String ConstructedURL = fragment(addToTheEnd);
        return String.format("/%s", ConstructedURL);
    }
}