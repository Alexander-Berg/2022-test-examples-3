package ru.yandex.market.api.geo.parser;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by tesseract on 22.02.17.
 */
public class PostcodeParserTest extends UnitTestBase {

    /**
     * Проверяем правильность получения почтового индекса из ответа
     */
    @Test
    public void simple() {
        PostcodeParser parser = new PostcodeParser();
        String postcode = parser.parse(ResourceHelpers.getResource("postcode.json"));

        Assert.assertEquals("620137", postcode);
    }
}
