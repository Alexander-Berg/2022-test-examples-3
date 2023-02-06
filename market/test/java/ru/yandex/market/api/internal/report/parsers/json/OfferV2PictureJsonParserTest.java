package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

/**
 *
 * Created by apershukov on 11.01.17.
 */
public class OfferV2PictureJsonParserTest extends BaseTest {

    private OfferV2PictureJsonParser parser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        parser = new OfferV2PictureJsonParser();
        context.setUrlSchema(null);
    }

    @Test
    public void testParsePicture() {
        Pair<Image, Image> pair = parser.parse(ResourceHelpers.getResource("picture1.json"));

        assertEquals("https://0.cs-ellpic01gt.yandex.ru/market_B5WECUzs-Z-7rt6E4J4Ucg_1x1.jpg", pair.getFirst().getUrl());
        assertEquals(285, pair.getFirst().getWidth());
        assertEquals(600, pair.getFirst().getHeight());

        assertEquals("https://0.cs-ellpic01gt.yandex.ru/market_B5WECUzs-Z-7rt6E4J4Ucg_190x250.jpg", pair.getSecond().getUrl());
        assertEquals(118, pair.getSecond().getWidth());
        assertEquals(250, pair.getSecond().getHeight());
    }

    /**
     * Тестирование того что в случае если с оригинальной картинкой будет что-то не так вместо нее будет
     * взят самый большой thumbnail
     */
    @Test
    public void testParseBackupWithThumbnail() {
        Pair<Image, Image> pair = parser.parse(ResourceHelpers.getResource("picture2.json"));

        assertEquals("https://0.cs-ellpic01gt.yandex.ru/market_B5WECUzs-Z-7rt6E4J4Ucg_600x600.jpg", pair.getFirst().getUrl());
        assertEquals(285, pair.getFirst().getWidth());
        assertEquals(600, pair.getFirst().getHeight());
    }
}
