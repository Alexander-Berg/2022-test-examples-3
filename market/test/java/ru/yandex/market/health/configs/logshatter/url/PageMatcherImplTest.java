package ru.yandex.market.health.configs.logshatter.url;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author kukabara
 */
@Disabled
public class PageMatcherImplTest {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-logshatter/src/conf.d/";

    PageMatcherImpl pageMatcher;

    @Test
    public void testReloadPages() throws Exception {
        String config = ru.yandex.devtools.test.Paths.getSourcePath(CONF_PATH + "page-matcher.properties");
        PageMatcherFileConfigLoader configLoader = new PageMatcherFileConfigLoader(config);
        pageMatcher = new PageMatcherImpl(
            configLoader,
            null,
            false,
            null,
            10080
        );

        pageMatcher.readConfig();
        pageMatcher.createHttpClient();
        pageMatcher.reload();
    }
}
