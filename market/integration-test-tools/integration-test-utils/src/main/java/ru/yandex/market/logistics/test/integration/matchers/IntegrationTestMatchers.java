package ru.yandex.market.logistics.test.integration.matchers;

import org.assertj.core.api.Condition;

import ru.yandex.market.logistics.test.integration.utils.ComparsionUtils;

public final class IntegrationTestMatchers {

    private IntegrationTestMatchers() {
        throw new UnsupportedOperationException();
    }

    public static Condition<? super String> jsonMatch(String expectedJson) {
        return new Condition<String>() {
            @Override
            public boolean matches(String actualJson) {
                return ComparsionUtils.isJsonEquals(expectedJson, actualJson);
            }
        };
    }

    public static Condition<? super String> xmlMatch(String expectedJson) {
        return new Condition<String>() {
            @Override
            public boolean matches(String actualJson) {
                return ComparsionUtils.isXmlEquals(expectedJson, actualJson);
            }
        };

    }
}
