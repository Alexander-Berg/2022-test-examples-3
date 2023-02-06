package config.classmapping;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DeliveryClassMappingsTest extends BaseClassMappingsTest {
    private static final String SERIALIZED_ADDRESS = "<shop-address country=\"Русь\" postcode=\"131488\" city=\"Питер\" subway=\"Петровско-Разумовская\" street=\"Победы\" house=\"13\" block=\"666\" floor=\"8\"/>";
    private static final String SERIALIZED_ADDRESS_OUTPUT = "<address country=\"Русь\" postcode=\"131488\" city=\"Питер\" subway=\"Петровско-Разумовская\" street=\"Победы\" house=\"13\" block=\"666\" floor=\"8\"/>";
    private Address address;

    @BeforeEach
    public void setUp() throws Exception {
        AddressImpl address = new AddressImpl();
        address.setCountry("Русь");
        address.setPostcode("131488");
        address.setCity("Питер");
        address.setSubway("Петровско-Разумовская");
        address.setStreet("Победы");
        address.setHouse("13");
        address.setBlock("666");
        address.setFloor("8");
        this.address = address;

    }

    @Test
    public void testDeserializeCartRequest() throws Exception {
        final Delivery actual = deserialize(Delivery.class,
            "<delivery region-id='213'>" +
                    "    " + SERIALIZED_ADDRESS +
                    "</delivery>"
        );

        assertEquals(213L, actual.getRegionId().longValue());
        assertEquals(address, actual.getShopAddress());
        assertNull(actual.getOutletIdsSet());
        assertNull(actual.getOutletCodes());
    }

    @Test
    public void testDeserializeCartResponse() throws Exception {
        final Delivery actual = deserialize(Delivery.class,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta'>" +
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
        assertEquals(new BigDecimal(2345l), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );
        assertEquals(Set.of(3456l, 4567l), actual.getOutletIdsSet());
        assertEquals("pochta", actual.getServiceName());
    }

    @Test
    public void testDeserializeCartResponseWithOutletCode() throws Exception {
        final Delivery actual = deserialize(Delivery.class,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <outlets>" +
                        "        <outlet code='str3456' />" +
                        "        <outlet code='str4567' />" +
                        "        <outlet code='str4567' />" +
                        "    </outlets>" +
                        "</delivery>"
        );

        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals("123", actual.getDeliveryOptionId());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals(new BigDecimal(2345l), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );
        assertEquals(Set.of("str3456", "str4567"), actual.getOutletCodes());
        assertEquals("pochta", actual.getServiceName());
    }

    @Test
    public void testDeserializeAcceptRequest() throws Exception {
        final Delivery actual = deserialize(Delivery.class,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    " + SERIALIZED_ADDRESS +
                        "    <outlet id='3456' />" +
                        "</delivery>"
        );

        assertEquals(3456l, actual.getOutletId().longValue());
    }

    @Test
    public void testDeserializeAcceptRequestWithCodeOutlet() throws Exception {
        final Delivery actual = deserialize(Delivery.class,
                "<delivery id='12345'" +
                        "      shop-delivery-id='54321'" +
                        "      hash='vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='" +
                        "      delivery-option-id='123'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    " + SERIALIZED_ADDRESS +
                        "    <outlet code='3456_hello' />" +
                        "</delivery>"
        );

        assertEquals("3456_hello", actual.getOutletCode());
    }

    private void checkDeserializeAcceptRequest(Delivery actual) throws Exception {

        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals("123", actual.getDeliveryOptionId());
        assertEquals(new BigDecimal(2345l), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );

        assertEquals(213l, actual.getRegionId().longValue());
        assertEquals(address, actual.getShopAddress());
        assertEquals("pochta", actual.getServiceName());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final Delivery actual = deserialize(Delivery.class,
                "<delivery />"
        );

        assertNotNull(actual);
    }

    @Test
    public void shouldDeserializePaymentOptions() throws Exception {
        Delivery delivery = deserialize(Delivery.class,
                "<delivery><payment-methods><payment-method>SHOP_PREPAID</payment-method></payment-methods></delivery>");
        assertEquals(delivery.getPaymentOptions(),new HashSet<>(Arrays.asList(PaymentMethod.SHOP_PREPAID)),"Payment options");
    }


    // SERIALIZERS:

    @Test
    public void testSerializeInternalCartRequest() throws Exception {
        serializeAndCompare(
                new Delivery() {{
                    setRegionId(213L);
                    setShopAddress(address);
                }},
                "<delivery region-id='213'>" +
                        "   " + SERIALIZED_ADDRESS_OUTPUT +
                        "</delivery>"
        );
    }

    @Test
    public void testSerializeInternalCartResponse() throws Exception {
        serializeAndCompare(
                new Delivery() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(2345));
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setOutletIds(Set.of(3456l, 4567l));
                    setOutletCodes(Set.of("Xo-xo"));
                    setServiceName("pochta");
                }},
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    <outlets>" +
                        "        <outlet id='3456' />" +
                        "        <outlet id='4567' />" +
                        "        <outlet code='Xo-xo' />"+
                        "    </outlets>" +
                        "</delivery>"
        );
    }

    @Test
    public void testSerializeInternalAcceptRequest() throws Exception {
        serializeAndCompare(
                new Delivery() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(2345));
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setOutletId(3456l);
                    setRegionId(213l);
                    setServiceName("pochta");
                    setShopAddress(address);
                }},
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    " + SERIALIZED_ADDRESS_OUTPUT  +
                        "    <outlet id='3456' />" +
                        "</delivery>"
        );
    }

    @Test
    public void testSerializeInternalAcceptRequestWithCodeOutlet() throws Exception {
        serializeAndCompare(
                new Delivery() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(2345));
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setOutletCode("str3456");
                    setRegionId(213l);
                    setServiceName("pochta");
                    setShopAddress(address);
                }},
                "<delivery shop-delivery-id='12345'" +
                        "      type='DELIVERY'" +
                        "      price='2345'" +
                        "      region-id='213'" +
                        "      service-name='pochta'>" +
                        "    <dates from-date='20-05-2013' to-date='23-05-2013' />" +
                        "    " + SERIALIZED_ADDRESS_OUTPUT  +
                        "    <outlet code='str3456' />" +
                        "</delivery>"
        );
    }


    @Test
    public void testSerializeEmpty() throws Exception {
        serializeAndCompare(
                new Delivery(),
                "<delivery />"
        );
    }
}
