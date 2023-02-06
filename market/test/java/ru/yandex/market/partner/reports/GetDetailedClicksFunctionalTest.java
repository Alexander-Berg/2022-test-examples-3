package ru.yandex.market.partner.reports;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.util.xml.XpathResolver;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link GetDetailedClicksServantlet}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class GetDetailedClicksFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Превышение лимита на количество дней")
    void testDaysLimit() {
        var xpathResolver = new XpathResolver();
        var response = FunctionalTestHelper.get(getUrl(), "2017-01-01 00:00", "2017-03-01 00:00");
        assertThat(xpathResolver.getXPathValue("//errors/simple-error-info/message-code", response.getBody()))
                .isEqualTo("days-interval-limit");
    }

    @Disabled("should be enabled after migrating tests to EmbeddedPostgresConfig")
    @Test
    void testEmpty() {
        var response = FunctionalTestHelper.get(getUrl(), "2017-01-01 00:00", "2017-01-02 00:00");
        assertThat(response.getBody()).isNotBlank();
    }

    private String getUrl() {
        return String.format("%s/getDetailedClicks?from_date={from_date}&to_date={to_date}&id=1", baseUrl);
    }
}
