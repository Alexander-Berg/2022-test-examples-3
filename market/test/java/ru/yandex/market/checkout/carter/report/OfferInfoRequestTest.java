package ru.yandex.market.checkout.carter.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.report.builders.OfferInfoRequestBuilder;
import ru.yandex.market.checkout.carter.report.parsers.OfferInfoPromoParser;
import ru.yandex.market.checkout.common.report.ColoredGenericMarketReportService;
import ru.yandex.market.checkout.common.report.ColoredRequest;
import ru.yandex.market.common.report.model.SearchRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

@ExtendWith(SpringExtension.class)
public class OfferInfoRequestTest {

    private static final String REPORT_URL_EXAMPLE = "localhost?place=offerinfo&rids=0&client=carter_for_morda&co" +
            "-from=test&rgb" +
            "=white&adult=1&pp=18&show-urls=DECRYPTED&%s=%s&numdoc=1&regset=1&new-picture-format=1&offerid=%s" +
            "&compact-offer-output=price_and_promo";

    private final OfferInfoRequestBuilder requestBuilder = new OfferInfoRequestBuilder("localhost", "test", "test");

    @Test
    void urlBuilderTest() throws IOException, InterruptedException {
        ColoredGenericMarketReportService reportService = spy(ColoredGenericMarketReportService.class);
        OfferInfoReportService offerInfoReportService = new OfferInfoReportService(reportService,
                new OfferInfoPromoParser(), 100, 100);

        ItemOffer item = generateItem(RandomStringUtils.randomAlphabetic(10));

        ArgumentCaptor<ColoredRequest<SearchRequest>> requestCaptor = ArgumentCaptor.forClass(ColoredRequest.class);
        doReturn(null).when(reportService).executeSearchAndParse(requestCaptor.capture(), Mockito.any());

        offerInfoReportService.getRawOfferInfo(List.of(item), CartItem::getObjId, UserIdType.UID,
                "9995873481632323703");

        String url = requestBuilder.build(requestCaptor.getValue().getRequest());

        Map<String, String> actualParams = parseUrlParams(url);
        Map<String, String> expectedParams =
                parseUrlParams(String.format(REPORT_URL_EXAMPLE, "puid", "9995873481632323703", item.getObjId()));

        assertEquals(actualParams, expectedParams);

        offerInfoReportService.getRawOfferInfo(List.of(item), CartItem::getObjId, UserIdType.YANDEXUID,
                "9995873481632323703");

        url = requestBuilder.build(requestCaptor.getValue().getRequest());

        actualParams = parseUrlParams(url);
        expectedParams =
                parseUrlParams(String.format(REPORT_URL_EXAMPLE, "yandexuid", "9995873481632323703", item.getObjId()));

        assertEquals(actualParams, expectedParams);

    }

    private Map<String, String> parseUrlParams(String url) {
        String[] params = url.substring(url.indexOf('?') + 1).split("&");
        Map<String, String> result = new HashMap<>(params.length);
        for (String param : params) {
            String[] keyValue = param.split("=");
            result.put(keyValue[0], keyValue[1]);
        }

        return result;
    }
}
