package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum StatTypeEnum {
    CUSTOM("custom"),
    GEO("geo"),
    PAGES("pages"),
    MOL("mol"),
    MOC("moc"),
    CAMP_DATE("campdate"),
    SEARCH_QUERIES("search_queries");
    private String value;

    StatTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
