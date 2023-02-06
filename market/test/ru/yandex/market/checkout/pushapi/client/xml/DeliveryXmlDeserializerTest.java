package ru.yandex.market.checkout.pushapi.client.xml;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutTimeFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.ShopAddressXmlDeserializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class DeliveryXmlDeserializerTest {

    private final Address address = mock(Address.class);
    private final Parcel parcel = mock(Parcel.class);

    private final DeliveryXmlDeserializer deserializer = new DeliveryXmlDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        CheckoutDateFormat formatter = new CheckoutDateFormat();
        RawDeliveryInterval interval = new RawDeliveryInterval(formatter.parse("15-09-2018", false), LocalTime.parse("08:00"), LocalTime.parse("12:00"));
        RawDeliveryIntervalXmlDeserializer intervalDeserializer = XmlTestUtil.createDeserializerMock(
                RawDeliveryIntervalXmlDeserializer.class,
                new HashMap<>() {{
                    put("<interval date='15-09-2018' from-time='08:00' to-time='12:00'/>", interval);
                }}
        );

        deserializer.setCheckoutDateFormat(new CheckoutDateFormat());
        deserializer.setCheckoutTimeFormat(new CheckoutTimeFormat());
        deserializer.setRawDeliveryIntervalXmlDeserializer(intervalDeserializer);
        deserializer.setShopAddressXmlDeserializer(
                XmlTestUtil.createDeserializerMock(
                        ShopAddressXmlDeserializer.class,
                        new HashMap<>() {{
                            put("<shop-address>address</shop-address>", address);
                        }}
                )
        );
        deserializer.setParcelXmlDeserializer(
                XmlTestUtil.createDeserializerMock(
                        ParcelXmlDeserializer.class,
                        new HashMap<>() {{
                            put("<shipment>shipment</shipment>", parcel);
                        }}
                )
        );
    }

    @Test
    public void testDeserializeCartRequest() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery region-id='213'>" +
                        "    <shop-address>address</shop-address>" +
                        "</delivery>"
        );

        assertEquals(213L, actual.getRegionId().longValue());
        assertEquals(address, actual.getShopAddress());
        assertNull(actual.getOutletIdsSet());
        assertNull(actual.getOutletCodes());
    }

    @Test
    public void testDeserializeCartResponse() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta'" +
                        "      delivery-service-id='99'" +
                        "      delivery-partner-type='SHOP'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <outlets>" +
                        "        <outlet id='3456' />" +
                        "        <outlet id='4567' />" +
                        "        <outlet id='4567' />" +
                        "    </outlets>" +
                        "</delivery>"
        );

        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals("123", actual.getDeliveryOptionId());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals(new BigDecimal(2345L), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );
        assertEquals(Set.of(3456L, 4567L), actual.getOutletIdsSet());
        assertEquals("pochta", actual.getServiceName());
        assertEquals(Long.valueOf(99L), actual.getDeliveryServiceId());
        assertEquals(DeliveryPartnerType.SHOP, actual.getDeliveryPartnerType());
    }

    @Test
    public void testDeserializeCartResponseWithOutletCode() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <outlets>" +
                        "        <outlet code='3456str' />" +
                        "        <outlet code='4567str' />" +
                        "        <outlet code='4567str' />" +
                        "    </outlets>" +
                        "</delivery>"
        );

        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals("123", actual.getDeliveryOptionId());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals(new BigDecimal(2345L), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );
        assertEquals(Set.of("3456str", "4567str"), actual.getOutletCodes());
        assertEquals("pochta", actual.getServiceName());
    }


    @Test
    public void testDeserializeDeliveryIntervals() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'>" +
                        "    <dates>" +
                        "       <interval date='15-09-2018' from-time='08:00' to-time='12:00'/> " +
                        "    </dates>" +
                        "</delivery>"
        );


        CheckoutDateFormat formatter = new CheckoutDateFormat();
        Date date = formatter.parse("15-09-2018", false);

        assertNotNull(actual.getRawDeliveryIntervals(),"Raw intervals collection is null");
        assertFalse(actual.getRawDeliveryIntervals().isEmpty(),"Intervals collection is empty.");

        Map<Date, Set<RawDeliveryInterval>> actualIntervals = actual.getRawDeliveryIntervals().getCollection();

        assertTrue(actualIntervals.containsKey(date),"Date not found.");

        RawDeliveryIntervalsCollection expected = new RawDeliveryIntervalsCollection();
        expected.add(new RawDeliveryInterval(date, LocalTime.parse("08:00"), LocalTime.parse("12:00")));

        assertEquals(expected, actual.getRawDeliveryIntervals(),"Raw interval collections aren't euqals");
    }

    @Test
    public void testDeserializeAcceptRequest() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet id='3456' " +
                        "            schedule-string='schedule-string' " +
                        "            postcode='postcode' " +
                        "            city='city' " +
                        "            street='street' " +
                        "            house='house' " +
                        "            block='block'/>" +
                        "    <shipment>shipment</shipment>" +
                        "</delivery>"
        );

        assertEquals(3456L, actual.getOutletId().longValue());

        ShopOutlet outlet = actual.getOutlet();
        assertNotNull(outlet);
        assertEquals("schedule-string", outlet.getScheduleString());
        assertEquals("postcode", outlet.getPostcode());
        assertEquals("city", outlet.getCity());
        assertEquals("street", outlet.getStreet());
        assertEquals("house", outlet.getHouse());
        assertEquals("block", outlet.getBlock());
    }


    @Test
    public void testDeserializeAcceptRequestWithOutletCode() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet code='Я3456' />" +
                        "</delivery>"
        );

        assertEquals("Я3456", actual.getOutletCode());
    }


    @Test
    public void testDeserializeAcceptRequestWithDeliverySubsidy() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <prices subsidy='13' />" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet code='Я3456' />" +
                        "</delivery>"
        );

        assertEquals(BigDecimal.valueOf(13), actual.getPrices().getSubsidy());
    }

    @Test
    public void testDeserializeAcceptRequestWithMarketBranded() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'" +
                        "      market-branded='true'>" +
                        "    <prices subsidy='13' />" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet code='Я3456' />" +
                        "</delivery>"
        );

        assertTrue(actual.isMarketBranded());
    }

    private void checkDeserializeAcceptRequest(Delivery actual) throws Exception {
        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals("123", actual.getDeliveryOptionId());
        assertEquals(new BigDecimal(2345L), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );

        assertEquals(213L, actual.getRegionId().longValue());
        assertEquals(address, actual.getShopAddress());
        assertEquals("pochta", actual.getServiceName());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery />"
        );

        assertNotNull(actual);
    }

    @Test
    public void shouldDeserializePaymentOptions() throws Exception {
        Delivery delivery = XmlTestUtil.deserialize(
                deserializer,
                "<delivery><payment-methods><payment-method>SHOP_PREPAID</payment-method></payment-methods></delivery>");
        assertEquals(delivery.getPaymentOptions(),new HashSet<>(List.of(PaymentMethod.SHOP_PREPAID)),"Payment options");
    }

    @Test
    public void testDeserializeLiftingOptions() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      lift-type='ELEVATOR'" +
                        "      lift-price='150'" +
                        "      service-name='pochta'>" +
                        "    <prices subsidy='13' />" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <shop-address>address</shop-address>" +
                        "    <outlet code='Я3456' />" +
                        "    <outlet code='Я3456' />" +
                        "    <address>address</address>" +
                        "</delivery>"
        );

        assertEquals(LiftType.ELEVATOR, actual.getLiftType());
        assertEquals(BigDecimal.valueOf(150L), actual.getLiftPrice());
        assertEquals(address, actual.getShopAddress());
    }

    @Test
    public void testDeserializeWithoutLiftingOptions() throws Exception {
        final Delivery actual = XmlTestUtil.deserialize(
                deserializer,
                "<delivery />"
        );

        assertNotNull(actual);
        assertNull(actual.getLiftType());
        assertNull(actual.getLiftPrice());
        assertNull(actual.getBuyerAddress());
    }
}
