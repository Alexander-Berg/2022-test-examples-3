package ru.yandex.autotests.direct.cmd.data.showcampstat;

public enum StatSortEnum {
    TEXT("text"),
    CLICKS("clicks"),
    SHOWS("shows"),
    SORTING("sorting"),
    DATE("date");

    private String value;

    StatSortEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
