package ru.yandex.autotests.direct.cmd.data;

public class CSRFToken {
    public static final String KEY = "csrf_token";
    public static final CSRFToken EMPTY = new CSRFToken("");
    private final String value;


    public CSRFToken(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
