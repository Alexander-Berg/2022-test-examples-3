package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.domain.Address;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.GeoCoordinates;
import ru.yandex.market.api.domain.v2.*;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.outlet.OutletField;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import static org.junit.Assert.*;

/**
 * Created by apershukov on 12.09.16.
 */
@WithContext
public class OutletWithOfferV2JsonParserTest extends BaseTest {

    private static final double DELTA = 1e-10;
    private ReportRequestContext context;

    @Inject
    private ReportParserFactory reportParserFactory;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context = new ReportRequestContext();
    }

    @Test
    public void testParseOutletWithoutOffer() {
        OutletWithOfferV2JsonParser parser = createParser(Collections.emptyList());
        OutletV2 outletV2 = parser.parse(ResourceHelpers.getResource("offer.json"));

        assertEquals("440721", outletV2.getId());
        assertEquals("Магазин", outletV2.getName());
        assertEquals("mixed", outletV2.getType());
        assertEquals(342553L, outletV2.getShopId());

        assertNull(outletV2.getOffer());
    }

    @Test
    public void testParseOutletWithOffer() {
        OutletWithOfferV2JsonParser parser = createParser(Collections.singleton(OutletField.OFFER));
        OutletV2 outletV2 = parser.parse(ResourceHelpers.getResource("offer.json"));

        assertEquals("440721", outletV2.getId());
        assertEquals("Магазин", outletV2.getName());
        assertEquals("mixed", outletV2.getType());
        assertEquals(342553L, outletV2.getShopId());

        assertNotNull(outletV2.getOffer());

        OfferV2 offer = (OfferV2) outletV2.getOffer();
        assertEquals(10495456L, (long) offer.getModelId());
        assertEquals("Смартфон Apple iPhone 5S 16Гб", offer.getName());
        assertEquals("LP3e5x_dp4TX7pRS0n9x9g", offer.getWareMd5());
        assertEquals(17, offer.getVariationCount().intValue());

        assertNull(offer.getDelivery());

        assertNull(offer.getOutlet());
    }

    @Test
    public void testParseOutletWithOfferWithDelivery() {
        OutletWithOfferV2JsonParser parser = createParser(Arrays.asList(OutletField.OFFER, OfferFieldV2.DELIVERY));
        OutletV2 outlet = parser.parse(ResourceHelpers.getResource("offer.json"));

        assertNotNull(outlet.getOffer());

        OfferV2 offer = (OfferV2) outlet.getOffer();
        assertEquals(10495456L, (long) offer.getModelId());

        assertTrue(offer.getDelivery() instanceof DeliveryV2);
        DeliveryV2 deliveryV2 = (DeliveryV2) offer.getDelivery();

        assertNotNull(deliveryV2);

        assertEquals(54, deliveryV2.getShopRegion().getId());
        assertFalse(deliveryV2.isFree());
    }

    @Test
    public void testNoOutletWithOfferWithField() {
        OutletWithOfferV2JsonParser parser = createParser(Arrays.asList(OutletField.OFFER, OfferFieldV2.OUTLET));
        OutletV2 outlet = parser.parse(ResourceHelpers.getResource("offer.json"));

        assertNotNull(outlet.getOffer());
        OfferV2 offer = (OfferV2) outlet.getOffer();
        assertNull(offer.getOutlet());
    }

    @Test
    public void testParseOutletAddress() {
        OutletWithOfferV2JsonParser parser = createParser(Collections.emptyList());
        OutletV2 outletV2 = parser.parse(ResourceHelpers.getResource("offer.json"));
        Address address = outletV2.getAddress();
        assertNotNull(address);
        assertEquals(AddressV2.class, address.getClass());
        AddressV2 addressV2 = (AddressV2) address;

        assertEquals("Екатеринбург, Первомайская, д. 77", addressV2.getFullAddress());
        assertEquals("Россия", addressV2.getCountry());
        assertEquals("Свердловская область", addressV2.getRegion());
        assertEquals("Екатеринбург", addressV2.getLocality());
        assertEquals("Первомайская", addressV2.getThoroughfare());
        assertEquals("77", addressV2.getPremiseNumber());
        assertEquals("1", addressV2.getBlock());
        assertEquals("2", addressV2.getWing());
        assertEquals("3", addressV2.getEstate());
        assertEquals("4", addressV2.getEntrance());
        assertEquals("5", addressV2.getFloor());
        assertEquals("6", addressV2.getRoom());
        assertEquals("note", addressV2.getNote());

        GeoPointV2 geoPoint = addressV2.getGeoPoint();
        assertNotNull(geoPoint);
        GeoCoordinates coordinates = geoPoint.getCoordinates();
        assertNotNull(coordinates);
        assertEquals(GeoCoordinatesV2.class, coordinates.getClass());
        GeoCoordinatesV2 coordinatesV2 = (GeoCoordinatesV2) coordinates;
        assertEquals(60.64092485, coordinatesV2.getLongitude(), DELTA);
        assertEquals(56.84540286, coordinatesV2.getLatitude(), DELTA);
    }

    private OutletWithOfferV2JsonParser createParser(Collection<? extends Field> fields) {
        context.setFields(fields);

        return reportParserFactory.getOutletWithOfferV2JsonParser(context);
    }
}
