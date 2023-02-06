package ru.yandex.market.api.common.url;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author dimkarp93
 */
public class FrontendTypeTest extends UnitTestBase {
    private static final List<Pair<String, FrontendType>> URI_FOR_TYPE = Lists.newArrayList(
            Pair.of("https://market.yandex.ru", FrontendType.DESKTOP),
            Pair.of("http://m.market.yandex.kz", FrontendType.TOUCH),
            Pair.of("market.yandex.by", FrontendType.DESKTOP),
            Pair.of("yandexmarket://catalog/root", FrontendType.MOBILE),
            Pair.of("m.market.yandex.", null),
            Pair.of("market.yandex.sh", null),
            Pair.of("pokupki.yandex.ru", FrontendType.BLUE_DESKTOP),
            Pair.of("beru.ru", FrontendType.BLUE_DESKTOP),
            Pair.of("pokupki.market.yandex.ru", FrontendType.BLUE_DESKTOP),
            Pair.of("m.pokupki.yandex.ru", FrontendType.BLUE_TOUCH),
            Pair.of("m.beru.ru", FrontendType.BLUE_TOUCH),
            Pair.of("m.pokupki.market.yandex.ru", FrontendType.BLUE_TOUCH)
    );


    private static final List<Pair<String, UrlGeoDomain>> URI_FOR_GEO = Lists.newArrayList(
        Pair.of("yandexmarket://search",  UrlGeoDomain.DEFAULT),
        Pair.of("market.yandex.ru", UrlGeoDomain.RU),
        Pair.of("m.market.yandex.by", UrlGeoDomain.BY),
        Pair.of("https://market.yandex.kz", UrlGeoDomain.KZ),
        Pair.of("http://m.market.yandex.ua", UrlGeoDomain.UA)
    );

    @Test
    public void testValidFrontendType() {
        for (Pair<String, FrontendType> pair: URI_FOR_TYPE) {
            FrontendType type = FrontendType.valueOf(MarketUrl.of(pair.getKey()));
            Assert.assertEquals(pair.getValue(), type);
        }
    }

    @Test
    public void testValidGeoDomain() {
        for (Pair<String, UrlGeoDomain> pair: URI_FOR_GEO) {
            MarketUrl uri = MarketUrl.of(pair.getKey());
            FrontendType frontendType = FrontendType.valueOf(uri);
            Assert.assertNotNull(frontendType);
            UrlGeoDomain geoDomain = frontendType.extractGeoDomain(uri);
            Assert.assertEquals(pair.getValue(), geoDomain);
        }
    }
}
