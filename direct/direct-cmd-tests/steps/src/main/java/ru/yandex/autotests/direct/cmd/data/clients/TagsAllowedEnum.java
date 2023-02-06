package ru.yandex.autotests.direct.cmd.data.clients;

public enum TagsAllowedEnum {
    CHECKED("CHECKED"),
    UNCHECKED("UNCHECKED"),
    OFF(null),
    ON("on"),
    NUMERIC_ON("1");


    String value;
    TagsAllowedEnum(String value) {
        this.value = value;
    }
    public String toString() {
        return this.value;
    }
}
