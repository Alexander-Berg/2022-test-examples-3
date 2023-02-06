package ru.yandex.market.logistics.test.integration;

import org.assertj.core.api.Condition;

import ru.yandex.market.logistics.test.integration.matchers.IntegrationTestMatchers;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

public class BaseIntegrationTest extends SoftAssertionSupport {

    protected String extractFileContent(String relativePath) {
        return IntegrationTestUtils.extractFileContent(relativePath);
    }

    protected Condition<? super String> jsonMatch(String expectedJson) {
        return IntegrationTestMatchers.jsonMatch(expectedJson);
    }

    protected Condition<? super String> xmlMatch(String expectedXml) {
        return IntegrationTestMatchers.xmlMatch(expectedXml);
    }
}
