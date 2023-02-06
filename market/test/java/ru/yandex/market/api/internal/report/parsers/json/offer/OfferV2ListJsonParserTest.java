package ru.yandex.market.api.internal.report.parsers.json.offer;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.json.OfferV2ListJsonParser;
import ru.yandex.market.api.matchers.DeliveryMatcher;
import ru.yandex.market.api.matchers.OfferMatcher;
import ru.yandex.market.api.matchers.OfferPriceMatcher;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

@WithMocks
@WithContext
public class OfferV2ListJsonParserTest extends BaseTest {
    @Mock
    CurrencyService currencyService;

    @Mock
    GeoRegionService geoRegionService;

    @Mock
    DeliveryService deliveryService;

    ReportParserFactory factory;

    OfferV2ListJsonParser parser;

    @Inject
    private MarketUrls marketUrls;

    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Mock
    private ClientHelper clientHelper;

    @Mock
    BlueMobileApplicationRule blueMobileApplicationRule;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        factory = new ReportParserFactory(
                currencyService,
                deliveryService,
                geoRegionService,
                null,
                null,
                marketUrls,
                urlParamsFactoryImpl,
                clientHelper,
                null,
                blueMobileApplicationRule
        );

        ReportRequestContext context = new ReportRequestContext();
        context.setFields(Collections.singletonList(OfferFieldV2.DELIVERY));
        parser = factory.getOfferV2ListJsonParser(context, PageInfo.DEFAULT);
    }

    @Test
    public void trouble_with_yandex_plus_marketapi_4716() {
        List<OfferV2> offers = doParse("offers_with_yandex_plus.json");

        Assert.assertThat(
            offers.get(5),
            OfferMatcher.offer(
                OfferMatcher.offerId(
                    OfferMatcher.wareMd5("js8enBvYsAhin3A5dD43jA"),
                    OfferMatcher.feeShow("8-qH2tqoDtL4bVvNHR6iJeJqg2rxAOedUfzvygYYKx4bBJjy2vt3m6Y47msnzP6N6iBwrcVKmqebDKCVcI3onMpTK0a9lovN3kf-pRfUBq4vDk46uEgWqX_wdzo9thoipAaan4LbOC_Ktd193JWGlA,,")
                ),
                OfferMatcher.price(
                    OfferPriceMatcher.value("139")
                ),
                OfferMatcher.delivery(
                    DeliveryMatcher.price(
                        OfferPriceMatcher.price(
                            OfferPriceMatcher.value("249"),
                            OfferPriceMatcher.discountType(null)
                        )
                    )
                )
            )
        );
    }

    private List<OfferV2> doParse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename)).getElements();
    }
}
