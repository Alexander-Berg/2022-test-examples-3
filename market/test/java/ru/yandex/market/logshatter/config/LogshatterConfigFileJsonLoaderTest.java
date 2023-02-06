package ru.yandex.market.logshatter.config;

import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.logshatter.config.storage.json.LogshatterConfigFileJsonLoader;

public class LogshatterConfigFileJsonLoaderTest {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-logshatter/src/conf.d";

    /**
     * Проверяем что LogshatterConfigFileJsonLoader не падает на маркетных конфигах.
     * LogshatterConfigFileJsonLoader падает если в JSON'е есть неизвестные поля.
     */
    @Test
    public void test() {
        new LogshatterConfigFileJsonLoader(Paths.getSourcePath(CONF_PATH))
            .load();
    }
}
