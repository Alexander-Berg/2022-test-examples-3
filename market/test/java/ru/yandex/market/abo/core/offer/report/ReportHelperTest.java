package ru.yandex.market.abo.core.offer.report;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.common.report.model.MarketSearchRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 03/07/19.
 *
 * Parse url from https://abo.market.yandex-team.ru/check/url
 */
class ReportHelperTest {
    @Test
    void urlWithModelId() {
        String urlOffers = "http://market.yandex.ru/offers.xml?modelid=7151875&hid=90586&hyperid=7151875&" +
                "hideduplicate=0&mcpriceto=2570&fesh=23542";
        MarketSearchRequest request = ReportHelper.createRequestFromSearchUrl(urlOffers);
        assertEquals("23542", request.getParams().get("fesh").iterator().next());
        assertEquals("7151875", request.getParams().get("modelid").iterator().next());
        assertEquals("2570", request.getParams().get("mcpriceto").iterator().next());
    }

    @Test
    void urlWithSearch() {
        String urlSearch = "http://market.yandex.ru/search.xml?hid=90668&how=aprice&np=1&text=&mcpricefrom=900&" +
                "mcpriceto=900&fesh=34845&glfilter=6126378:6126748&glfilter=6126560:6126727,6126644";
        MarketSearchRequest request = ReportHelper.createRequestFromSearchUrl(urlSearch);
        assertEquals("34845", request.getParams().get("fesh").iterator().next());
        assertEquals("90668", request.getParams().get("hid").iterator().next());
        assertEquals("aprice", request.getParams().get("how").iterator().next());
    }

    @Test
    void humanReadableUrl() {
        String urlOld = "https://market.yandex.ru/product/1877564240/offers?local-offers-first=0&how=aprice&grhow=shop&fesh=561218";
        String urlNew = "https://market.yandex.ru/product--mysh-genius-scorpion-m8-610-black-orange-usb/1877564240/offers?local-offers-first=0&how=aprice&grhow=shop&fesh=561218";

        MarketSearchRequest reqFromOld = ReportHelper.createRequestFromSearchUrl(urlOld);
        MarketSearchRequest reqFromNew = ReportHelper.createRequestFromSearchUrl(urlNew);

        Stream.of(reqFromOld, reqFromNew).forEach(req -> {
            assertEquals("561218", req.getParams().get("fesh").iterator().next());
            assertEquals("1877564240", req.getParams().get("hyperid").iterator().next());
        });
    }
}
