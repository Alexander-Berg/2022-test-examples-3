package ru.yandex.market.logshatter.url;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author kukabara
 */
@Ignore
public class PageMatcherImplTest {

    PageMatcherImpl pageMatcher;

    @Test
    public void testReloadPages() throws Exception {
        String config = "../config-cs-logshatter/src/configs/page-matcher.properties";
        pageMatcher = new PageMatcherImpl();
        pageMatcher.setConfigFile(config);

        pageMatcher.readConfig();
        pageMatcher.createHttpClient();
        pageMatcher.reload();
    }
}
