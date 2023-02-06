package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.ShowUrlsParam;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 * Date: 19/04/2017.
 */
public class MarketReportInfoFetcherExtractItemParametersTest extends AbstractServicesTestBase {

    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private MarketReportSearchService searchService;

    private final long shopId = 100500L;
    private final long regionId = 213L;
    private final long feedId = 200310551L;

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    public void extractItemParametersTest() throws Exception {
        Method method = ReportOrderItemInflater.class.getDeclaredMethod("extractItemParameters", FoundOffer.class);
        method.setAccessible(true);
        List<ItemParameter> itemParameterList = (List<ItemParameter>) method.invoke(null, getFoundOffers().get(0));

        Assertions.assertEquals(5, itemParameterList.size());
    }

    private List<FoundOffer> getFoundOffers() throws Exception {
        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportInfoFetcherExtractItemParametersTest.class.getResource("/files/report/offerInfo" +
                ".json");
        mockReport(resource, Collections.singletonList(feedOfferId));
        return searchService.searchItems(getParameters(), Collections.singleton(feedOfferId));
    }

    private ReportSearchParameters getParameters() {
        return ReportSearchParameters.builder().withRgb(Color.BLUE).withShopId(shopId).withRegionId(regionId).build();
    }

    private void mockReport(URL resource, Collection<FeedOfferId> offers) throws IOException {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.OFFER_INFO.getId()))
                .withQueryParam("rids", equalTo("213"))
                .withQueryParam("fesh", equalTo(String.valueOf(shopId)))
                .withQueryParam("regset", equalTo("1"))
                .withQueryParam("numdoc", equalTo(String.valueOf(offers.size())))
                .withQueryParam("cpa-category-filter", equalTo("0"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("show-urls", equalTo(ShowUrlsParam.DECRYPTED.getId()));

        for (FeedOfferId offer : offers) {
            builder.withQueryParam("feed_shoffer_id", equalTo(offer.getFeedId() + "-" + offer.getId()));
        }

        reportMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(IOUtils.toByteArray(resource))
                )
        );
    }
}
