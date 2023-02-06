package ru.yandex.market.crm.operatorwindow.util;

@FunctionalInterface
public interface AssertMatcher<T> {

    void matches(String prefix, T expected, T actual);

}
