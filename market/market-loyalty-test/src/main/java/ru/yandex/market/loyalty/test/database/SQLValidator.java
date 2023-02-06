package ru.yandex.market.loyalty.test.database;

import java.util.List;

public interface SQLValidator {
    void validate(String sql);

    void startTest();

    List<String> finishTest();
}
