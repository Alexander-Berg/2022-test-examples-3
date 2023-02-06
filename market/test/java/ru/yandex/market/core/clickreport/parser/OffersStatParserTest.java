package ru.yandex.market.core.clickreport.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.clickreport.model.ClickReportOfferStats;
import ru.yandex.market.core.clickreport.model.OffersStatsContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author dimkarp93
 */
class OffersStatParserTest {

    @Test
    void testUrlParsing() throws IOException {
        try (InputStream in = OffersStatParserTest.class.getResourceAsStream("OffersStatParserTest_OK-result.xml")) {
            OffersStatParser parser = new OffersStatParser();
            parser.parse(in);
            OffersStatsContainer stats = parser.getOffersStatsContainer();
            String[] expectedUrls = new String[]{
                    "http://www.mvideo.ru/products/smartfon-apple-iphone-5s-16gb-silver-me433ru-a-30018902",
                    "",
                    null,
                    "http://www.mvideo.ru/products/smartfon-apple-iphone-6-16gb-space-gray-mg472ru-a-30020953",
                    "",
                    null,
                    null,
                    "",
                    "",
                    null};
            assertEquals(expectedUrls.length, stats.getClickReportOfferStats().size());
            List<String> actualUrls = stats.getClickReportOfferStats().stream()
                    .map(ClickReportOfferStats::getUrl)
                    .collect(Collectors.toList());

            for (int i = 0; i < expectedUrls.length; ++i) {
                assertEquals(expectedUrls[i], actualUrls.get(i));
            }
        }
    }

}
