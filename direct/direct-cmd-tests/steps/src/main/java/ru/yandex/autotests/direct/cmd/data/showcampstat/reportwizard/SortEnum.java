package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum SortEnum {
    CAMP_NAME("camp_name"),
    DATE("date"),
    CAMPAIGN("campaign"),
    SHOWS("shows"),
    CLICKS("clicks"),
    CTR("ctr"),
    SUM("sum"),
    SEARCH_QUERY("search_query"),
    SEARCH_QUERY_STATUS("search_query_status"),
    PHRASE_STATUS("ext_phrase_status");
    private String value;

    SortEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
