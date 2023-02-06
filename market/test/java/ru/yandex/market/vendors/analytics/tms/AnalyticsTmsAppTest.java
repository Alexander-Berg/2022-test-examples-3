package ru.yandex.market.vendors.analytics.tms;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@ActiveProfiles("functionalTest")
public class AnalyticsTmsAppTest extends FunctionalTest {

    /**
     * Тест проверяет, что приложение analytics-platform стартует и отвечает на пинг стандартным способом
     */
    @Test
    void shouldLoadSpringContextWithoutAnyErrors() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getBaseUrl() + "/ping", String.class);
        assertThat(response.getBody(), equalTo("0;OK"));
    }
}