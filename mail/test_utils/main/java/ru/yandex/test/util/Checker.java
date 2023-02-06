package ru.yandex.test.util;

public interface Checker {
    // returns null if string passed the check, otherwise difference
    // description returned
    String check(final String value);
}

