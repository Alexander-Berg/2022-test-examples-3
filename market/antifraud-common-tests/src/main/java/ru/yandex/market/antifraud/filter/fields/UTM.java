package ru.yandex.market.antifraud.filter.fields;

import ru.yandex.market.antifraud.filter.TestClick;

import java.util.List;

/**
 * Created by kateleb on 21.05.15.
 */
public enum UTM {
    TERM("test_utm_term"),
    SOURCE("test_utm_source"),
    MEDIUM("test_utm_medium"),
    CAMPAIGN("test_utm_campaign");

    private String value;

    UTM(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static List<TestClick> setUtmTerm(List<TestClick> clicks, String utmTerm) {
        return setUtmTerm(clicks, utmTerm, true);
    }

    public static List<TestClick> setUtmTerm(List<TestClick> clicks, String utmTerm, boolean markRowid) {
        for (TestClick click : clicks) {
            click.set("utm_term", utmTerm);
            if(markRowid) {
                click.set("rowid", utmTerm + "_" + click.get("rowid"));
            }
        }
        return clicks;
    }
}
