package ru.yandex.market.api.integration;

import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.client.ClientVersionInfoResolver;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.common.currency.CurrencyRegionalResolver;
import ru.yandex.market.api.controller.v2.OffersControllerV2;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.domain.v2.OfferResult;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.server.RegionalInfoResolver;
import ru.yandex.market.api.server.RegionalInfoResolverRegistry;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class CurrencyRepresentationTest extends BaseTest {

    @Inject
    private OffersControllerV2 offersControllerV2;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private RegionalInfoResolverRegistry regionalInfoResolverRegistry;

    @Inject
    private CurrencyRegionalResolver currencyRegionalResolver;

    private static final OfferId TEST_OFFER = new OfferId("test-offer-id", null);
    private static final Client SOVETNIK_CLIENT = new Client() {{setType(Type.INTERNAL);}};
    private static final Client MOBILE_CLIENT = new Client() {{setType(Type.MOBILE);}};

    @Test
    public void shouldReturnAlternatePriceForCrimea() throws Exception {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("geo_id", GeoUtils.Region.CRIMEA)
            .build();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        reportTestClient.getOfferInfo(TEST_OFFER, "offer_for_url.json");

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);

        Futures.wait(resolver.initRegionInfo(request, response, context));
        currencyRegionalResolver.extractCurrencyInfo(request, context);

        OfferResult offerResult = offersControllerV2.getOffer(
            TEST_OFFER,
            false,
            Collections.emptySet(),
            GenericParams.DEFAULT
        ).waitResult();

        OfferV2 offer = (OfferV2) offerResult.getOffer();
        OfferPriceV2 price = (OfferPriceV2) offer.getPrice();
        OfferPriceV2 alternatePrice = (OfferPriceV2) offer.getAlternatePrice();

        assertEquals(new OfferPriceV2("500", null, null) , price);
        assertEquals(Currency.RUR, context.getCurrency());

        assertEquals(new OfferPriceV2("231.61", null, null) , alternatePrice);
        assertEquals(Currency.UAH, context.getAlternateCurrency());
    }

    @Test
    public void shouldReturnByrForBelarus() throws Exception {
        context.setCurrency(Currency.BYR);
        HttpServletRequest request = MockRequestBuilder.start()
            .param("geo_id", GeoUtils.Country.BELARUS)
            .build();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        reportTestClient.getOfferInfo(TEST_OFFER, "offer_for_url.json");

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        currencyRegionalResolver.extractCurrencyInfo(request, context);

        Futures.wait(resolver.initRegionInfo(request, response, context));
        OfferResult offerResult = offersControllerV2.getOffer(
            TEST_OFFER,
            false,
            Collections.emptySet(),
            GenericParams.DEFAULT
        ).waitResult();

        OfferV2 offer = (OfferV2) offerResult.getOffer();
        OfferPriceV2 price = (OfferPriceV2) offer.getPrice();

        assertEquals(new OfferPriceV2("500", null, null) , price);
        assertEquals(Currency.BYR, context.getCurrency());

        assertEquals(null, offer.getAlternatePrice());
    }

    @Test
    public void shouldReturnBynForSovetnik() throws Exception {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("geo_id", GeoUtils.Country.BELARUS)
            .build();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        context.setClient(SOVETNIK_CLIENT);
        context.setClientVersionInfo(ClientVersionInfoResolver.resolve(SOVETNIK_CLIENT, request));

        reportTestClient.getOfferInfo(TEST_OFFER, "offer_for_url.json");

        RegionalInfoResolver resolver = regionalInfoResolverRegistry.get(context);
        Futures.wait(resolver.initRegionInfo(request, response, context));
        currencyRegionalResolver.extractCurrencyInfo(request, context);

        OfferResult offerResult = offersControllerV2.getOffer(
            TEST_OFFER,
            false,
            Collections.emptySet(),
            GenericParams.DEFAULT
        ).waitResult();

        OfferV2 offer = (OfferV2) offerResult.getOffer();
        OfferPriceV2 price = (OfferPriceV2) offer.getPrice();

        assertEquals(new OfferPriceV2("500", null, null) , price);
        assertEquals(Currency.BYN, context.getCurrency());

        assertEquals(null, offer.getAlternatePrice());
    }

}
