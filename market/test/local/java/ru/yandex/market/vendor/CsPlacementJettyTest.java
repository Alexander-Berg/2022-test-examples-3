package ru.yandex.market.vendor;

import org.junit.jupiter.api.Test;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class CsPlacementJettyTest extends AbstractCsPlacementTmsFunctionalTest {

    /**
     * Тест проверяет, что модуль cs-placement-tms стартует и отвечает на пинг стандартным способом.
     */
    @Test
    void shouldLoadSpringContextWithoutAnyErrors() {
        String response = FunctionalTestHelper.get(resourceUrl("/ping"));
        assertThat(response, equalTo("0;OK\n"));
    }
}
