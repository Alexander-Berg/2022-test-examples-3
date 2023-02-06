package ru.yandex.market.api.parsers;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.YandexUidMatcher;
import ru.yandex.market.api.server.sec.YandexUid;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @author dimkarp93
 */
public class YandexUidParserTest extends UnitTestBase {
    private Parameters.YandexuidParser parser;

    @Override
    public void setUp() throws Exception {
        parser = new Parameters.YandexuidParser();
        super.setUp();
    }

    @Test
    public void fromHeader() {
        String yandexUid = "head_rand";
        doTest(
            YandexUidMatcher.yandexUid(yandexUid),
            MockRequestBuilder.start()
                .header("X-YANDEXUID", yandexUid)
                .build()
        );
    }

    @Test
    public void fromCookie() {
        String yandexUid = "cookie_rand";
        doTest(
            YandexUidMatcher.yandexUid(yandexUid),
            MockRequestBuilder.start()
                .cookie(new Cookie("yandexuid", yandexUid))
                .build()
        );
    }

    @Test
    public void empty() {
        doTest(
            Matchers.nullValue(YandexUid.class),
            MockRequestBuilder.start().build()
        );
    }

    private void doTest(Matcher<YandexUid> yandexuid, HttpServletRequest request) {
        Assert.assertThat(parser.get(request).getValue(), yandexuid);
    }
}
