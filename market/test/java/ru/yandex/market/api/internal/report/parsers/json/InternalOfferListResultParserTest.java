package ru.yandex.market.api.internal.report.parsers.json;

import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.InternalOfferListResult;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @author Ural Yulmukhametov <a href="mailto:ural@yandex-team.ru"></a>
 * @date 16.09.2019
 */
@WithContext
@WithMocks
public class InternalOfferListResultParserTest extends BaseTest {
    @Mock
    private CurrencyService currencyService;

    @Mock
    GeoRegionService geoRegionService;

    @Mock
    DeliveryService deliveryService;

    private FilterFactory filterFactory;

    @Mock
    private ClientHelper clientHelper;

    @Mock
    private BlueMobileApplicationRule blueMobileApplicationRule;

    @Inject
    private MarketUrls marketUrls;

    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;


    @Before
    public void setUp() {
        filterFactory = new FilterFactory();
    }

    @Test
    public void shouldParseOffers() {
        InternalOfferListResult result = parse("report__offerinfo_with_promooffers.json");

        List<OfferV2> offers = result.getOffers();
        Assert.assertNotNull(offers);
        Assert.assertFalse(offers.isEmpty());

        List<OfferV2> promoOffers = result.getPromoOffers();
        Assert.assertNotNull(promoOffers);
        Assert.assertFalse(promoOffers.isEmpty());

    }

    private InternalOfferListResult parse(String filename) {
        ReportParserFactory factory = new ReportParserFactory(
                currencyService,
                deliveryService,
                geoRegionService,
                null,
                filterFactory,
                marketUrls,
                urlParamsFactoryImpl,
                clientHelper,
                null,
                blueMobileApplicationRule
        );
        ReportRequestContext context = new ReportRequestContext();

        InternalOfferListResultParser parser = new InternalOfferListResultParser(
            factory, context, PageInfo.DEFAULT
        );

        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
