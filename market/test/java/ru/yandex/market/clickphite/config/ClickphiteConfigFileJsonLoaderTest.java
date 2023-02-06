package ru.yandex.market.clickphite.config;

import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.clickphite.config.storage.json.ClickphiteConfigFileJsonLoader;

public class ClickphiteConfigFileJsonLoaderTest {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-clickphite/src/conf.d";
    /**
     * Проверяем что ClickphiteConfigFileJsonLoader не падает на маркетных конфигах.
     * ClickphiteConfigFileJsonLoader падает если в JSON'е есть неизвестные поля.
     */
    @Test
    public void test() {
        new ClickphiteConfigFileJsonLoader(Paths.getSourcePath(CONF_PATH))
            .load();
    }
}
