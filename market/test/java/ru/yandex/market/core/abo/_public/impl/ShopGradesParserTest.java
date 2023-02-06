package ru.yandex.market.core.abo._public.impl;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.abo._public.ShopGrades;

public class ShopGradesParserTest extends AbstractParserTest {
    @Test
    public void test() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-result.xml")) {
            ShopGradesParser parser = new ShopGradesParser();
            ShopGrades shopGradesActual = parser.parseStream(in);
            ShopGrades shopGradesExcepted = new ShopGrades();
            shopGradesExcepted.setClusterGradeCount(30560);
            shopGradesExcepted.setShopGradeCount(30469);
            assertEquals(shopGradesExcepted, shopGradesActual);
        }
    }
}
