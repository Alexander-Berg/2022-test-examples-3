package ru.yandex.market.core.abo._public.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.abo._public.RatingCalculationDetails;

/**
 * @author zoom
 */
public class RatingCalculationDetailsParserTest extends AbstractParserTest {

    @Test
    public void shouldParseWell() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-result.xml")) {
            RatingCalculationDetails actual = new RatingCalculationDetailsParser().parseStream(in);
            RatingCalculationDetails expected = new RatingCalculationDetails();
            expected.setShopId(211);
            expected.setCheckCount(25);
            expected.setGradeCount(25587);
            expected.setMarketRating(0.74f);
            expected.setQualityBonus(1);
            expected.setErrorCount(1);
            expected.setQualityServiceRating(0.58f);
            expected.setAgeBonus(0.5f);
            expected.setModificationTimestamp(LocalDateTime.of(2016, 1, 25, 13, 16, 59));
            assertEquals(expected, actual);
        }
    }
}