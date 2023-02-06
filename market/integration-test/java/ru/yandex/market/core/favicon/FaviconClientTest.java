package ru.yandex.market.core.favicon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.config.DevIntegrationTest;
import ru.yandex.market.core.favicon.client.FaviconClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для {@link FaviconClient}.
 */
class FaviconClientTest extends DevIntegrationTest {

    @Autowired
    private FaviconClient faviconClient;

    @Test
    @DisplayName("Проверить возможность получения favicon")
    void testGetFavicon() {
        var url = "https://ya.ru";
        var favicon = faviconClient.findFavicon(url).orElseThrow();

        assertThat(favicon).isNotEmpty();
    }

    @Test
    @DisplayName("Проверить отсутствие favicon")
    void testGetNoFavicon() {
        var badUrl = "i-am-not-a-link";
        var favicon = faviconClient.findFavicon(badUrl).orElse(null);

        assertThat(favicon).isNull();
    }

}
