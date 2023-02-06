package ru.yandex.market.api.internal.report.parsers.json;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by apershukov on 01.09.16.
 */
@WithContext
public class OfferV2JsonParserSingleVersionTest extends BaseTest {

    @Inject
    private ReportParserFactory parserFactory;

    private ReportRequestContext context;

    private OfferV2JsonParserSingleVersion parser;


    @Before
    public void setUp() {
        context = new ReportRequestContext();
        context.setFields(
            ParametersV2
                .MULTI_MODEL_FIELDS
                .getItems()
                .stream()
                .flatMap(x -> x.getValues().stream())
                .collect(Collectors.toList())
        );

        parser = new OfferV2JsonParserSingleVersion(context, parserFactory);
    }

    @Test
    public void testParseOffer() {
        OfferV2 offer = parser.parse(ResourceHelpers.getResource("default_offer.json"));

        assertTrue(offer.getAdult());

        assertEquals("Смартфон Samsung Galaxy S7 Edge 32Gb silver", offer.getName());
        assertTrue(offer.isCpa());
        assertEquals(offer.getVendorId(), 153061);

        assertEquals("Samsung Galaxy S7 Edge 32Gb silver", offer.getDescription());

        assertEquals(true, offer.getIsFulfillment());
        assertArrayEquals(new String[]{"SHOP", "YANDEX_MARKET"}, ((DeliveryV2) offer.getDelivery()).getDeliveryPartnerTypes().toArray());

        assertEquals(13485518, offer.getModel().getId());

        assertEquals(1, offer.getPhotos().size());
        assertEquals("http://0.cs-ellpic01gt.yandex.ru/market_ybbFikYWqEId4adcH5AfJg_300x400.jpg",
            offer.getPhotos().get(0).getUrl());

        assertEquals("http://0.cs-ellpic01gt.yandex.ru/market_ybbFikYWqEId4adcH5AfJg_300x400.jpg",
            offer.getPhoto().getUrl());
    }
}
