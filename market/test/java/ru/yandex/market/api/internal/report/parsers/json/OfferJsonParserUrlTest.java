package ru.yandex.market.api.internal.report.parsers.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.offer.Offer;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

/**
 * @author dimkarp93
 */
@WithMocks
@WithContext
public class OfferJsonParserUrlTest extends BaseTest {
    private static final int REGION_ID = 54;

    private ReportParserFactory reportParserFactory;

    @Mock
    private CurrencyService currencyService;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private GeoRegionService geoRegionService;

    @Inject
    private MarketUrls marketUrls;

    @Mock
    private BlueRule blueRule;

    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Mock
    private ClientHelper clientHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.setUrlSchema(UrlSchema.HTTP);

        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(false);
        Mockito
            .when(
                geoRegionService.getRegion(
                    Mockito.anyInt(),
                    Mockito.anyCollection(),
                    Mockito.any(RegionVersion.class)
                )
            )
            .thenReturn(
                new GeoRegion(1, null, null, null, null)
            );

        reportParserFactory = new ReportParserFactory(
            currencyService,
            deliveryService,
            geoRegionService,
            null,
            null,
            marketUrls,
                urlParamsFactoryImpl,
            clientHelper,
            null,
            null
        );

    }

    private void setVendorWithoutCpc() {
        ContextHolder.update(ctx -> {
            Client vendor = new Client();
            vendor.setTariff(TestTariffs.VENDOR);
            vendor.setShowShopUrl(false);
            ctx.setClient(vendor);
        });
    }

    @Test
    public void testParseUrlCpc() {
        Offer offer = parse("cpc-offer.json");
        assertEquals("http://market-click2.yandex.ru/redir", offer.getUrl());
        OfferV2 offerV2 = parseV2("cpc-offer.json");
        assertEquals("http://market-click2.yandex.ru/redir", offerV2.getUrl());
    }

    @Test
    public void testParseUrlCpc_forVendor() {
        setVendorWithoutCpc();
        Offer offer = parse("cpc-offer.json");
        assertEquals("http://market-click2.yandex.ru/redir", offer.getUrl());
        OfferV2 offerV2 = parseV2("cpc-offer.json");
        assertEquals("http://market-click2.yandex.ru/redir", offerV2.getUrl());
    }

    @Test
    public void testParseUrlCpa() {
        Offer offer = parse("cpa-offer.json");
        assertEquals("http://market-click2.yandex.ru/redir", offer.getUrl());
        OfferV2 offerV2 = parseV2("cpa-offer.json");
        assertEquals("http://market-click2.yandex.ru/redir", offerV2.getUrl());
    }

    @Test
    public void testParseUrlCpa_forVendor() {
        setVendorWithoutCpc();
        Offer offer = parse("cpa-offer.json");
        assertEquals("http://market-click2.yandex.ru/fast/checkout", offer.getUrl());
        OfferV2 offerV2 = parseV2("cpa-offer.json");
        assertEquals("http://market-click2.yandex.ru/fast/checkout", offerV2.getUrl());
    }


    private OfferV2 parseV2(String filename, Consumer<ReportRequestContext> func) {
        ReportRequestContext context = new ReportRequestContext();
        context.setFields(
            ParametersV2
                .MULTI_MODEL_FIELDS
                .getItems()
                .stream()
                .flatMap(x -> x.getValues().stream())
                .collect(Collectors.toList())
        );
        context.setUserRegionId(REGION_ID);
        func.accept(context);

        OfferV2JsonParser parser = new OfferV2JsonParser(
                context,
                currencyService,
                deliveryService,
                geoRegionService,
                new FilterFactory(),
                marketUrls,
                clientHelper);
        return parser.parse(ResourceHelpers.getResource(filename));


    }

    private OfferV2 parseV2(String filename) {
        return parseV2(filename, x -> {});
    }

    private Offer parse(String filename) {
        ReportRequestContext reportContext = new ReportRequestContext();
        Collection<Field> fields = new ArrayList<>();
        reportContext.setFields(fields);
        reportContext.setUserRegionId(GeoUtils.DEFAULT_GEO_ID);
        return reportParserFactory.getOfferV1JsonParser(reportContext).parse(ResourceHelpers.getResource(filename));
    }

}
