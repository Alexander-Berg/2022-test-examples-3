package ru.yandex.autotests.market.stat.attribute;

/**
 * Created by kateleb on 21.05.15.
 */
public enum UTM {
    TERM("test_utm_term"),
    SOURCE("test_utm_source"),
    MEDIUM("test_utm_medium"),
    CAMPAIGN("test_utm_campaign");

    private String mask;

    UTM(String mask) {
        this.mask = mask;
    }

    public String mask() {
        return mask;
    }
}
