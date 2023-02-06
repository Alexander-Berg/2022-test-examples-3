package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.out.shopApi.json.order.AddressJsonSerializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class DeliveryWithRegionJsonSerializerTest {

    private final Address address = mock(Address.class);
    private final Parcel orderShipment1 = mock(Parcel.class);
    private final Parcel orderShipment2 = mock(Parcel.class);
    private Courier courier = mock(Courier.class);
    private final DeliveryWithRegionJsonSerializer serializer = new DeliveryWithRegionJsonSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
        serializer.setAddressJsonSerializer(
                new AddressJsonSerializer() {
                    @Override
                    public void serialize(Address value, JsonWriter generator) throws IOException {
                        assertEquals(address, value);
                        generator.setValue("address");
                    }
                }
        );
        serializer.setParcelJsonSerializer(
                new ParcelJsonSerializer() {
                    @Override
                    public void serialize(Parcel parcel, JsonWriter writer) throws IOException {
                        assertThat(parcel, is(anyOf(equalTo(orderShipment1), equalTo(orderShipment2))));
                        writer.setValue("shipment-" + (parcel == orderShipment1 ? 1 : 2));
                    }
                }
        );

        serializer.setCourierJsonSerializer(
                new CourierJsonSerializer() {
                    @Override
                    public void serialize(Courier value, JsonWriter writer) throws IOException {
                        assertEquals(courier, value);
                        writer.setValue("courier");
                    }
                }
        );
    }

    @Test
    public void testSerializeAcceptRequest() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(1234L));
                    setServiceName("pochta");
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setRegion(new Region(123, "Москва", null,
                            new Region(234, "Россия", null,
                                    new Region(345, "Земля", null))));
                    setShopAddress(address);
                    setOutletId(2345L);
                    setCourier(courier);
                }},
                "{" +
                        "   'shopDeliveryId': '12345'," +
                        "   'type': 'DELIVERY'," +
                        "   'price': 1234," +
                        "   'dates': {" +
                        "       'toDate': '23-05-2013'," +
                        "       'fromDate': '20-05-2013'" +
                        "   }," +
                        "   'serviceName': 'pochta'," +
                        "   'region': {" +
                        "       'id': 123, 'name': 'Москва', 'parent': {" +
                        "           'id': 234, 'name': 'Россия', 'parent': {" +
                        "               'id': 345, 'name': 'Земля'" +
                        "           }" +
                        "       }" +
                        "   }," +
                        "   'address': 'address'," +
                        "   'outlet': {'id': 2345}," +
                        "   'courier': 'courier'" +
                        "}"
        );

    }

    @Test
    public void testSerializeAcceptRequestOutletCode() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(1234L));
                    setServiceName("pochta");
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setRegion(new Region(123, "Москва", null,
                            new Region(234, "Россия", null,
                                    new Region(345, "Земля", null))));
                    setShopAddress(address);
                    setOutletCode("str2345l");
                }},
                "{" +
                        "   'shopDeliveryId': '12345'," +
                        "   'type': 'DELIVERY'," +
                        "   'price': 1234," +
                        "   'dates': {" +
                        "       'toDate': '23-05-2013'," +
                        "       'fromDate': '20-05-2013'" +
                        "   }," +
                        "   'serviceName': 'pochta'," +
                        "   'region': {" +
                        "       'id': 123, 'name': 'Москва', 'parent': {" +
                        "           'id': 234, 'name': 'Россия', 'parent': {" +
                        "               'id': 345, 'name': 'Земля'" +
                        "           }" +
                        "       }" +
                        "   }," +
                        "   'address': 'address'," +
                        "   'outlet': {'code': 'str2345l'}" +
                        "}"
        );

    }

    @Test
    public void testSerializeAcceptRequestOutletIdAndCode() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(1234L));
                    setServiceName("pochta");
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setRegion(new Region(123, "Москва", null,
                            new Region(234, "Россия", null,
                                    new Region(345, "Земля", null))));
                    setShopAddress(address);
                    setOutletId(1234L);
                    setOutletCode("str2345l");
                }},
                "{" +
                        "   'shopDeliveryId': '12345'," +
                        "   'type': 'DELIVERY'," +
                        "   'price': 1234," +
                        "   'dates': {" +
                        "       'toDate': '23-05-2013'," +
                        "       'fromDate': '20-05-2013'" +
                        "   }," +
                        "   'serviceName': 'pochta'," +
                        "   'region': {" +
                        "       'id': 123, 'name': 'Москва', 'parent': {" +
                        "           'id': 234, 'name': 'Россия', 'parent': {" +
                        "               'id': 345, 'name': 'Земля'" +
                        "           }" +
                        "       }" +
                        "   }," +
                        "   'address': 'address'," +
                        "   'outlet': {'id': 1234, 'code': 'str2345l'}" +
                        "}"
        );

    }

    @Test
    public void testSerializeCartRequestWithAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setRegion(new Region(123, "Москва", null,
                            new Region(234, "Россия", null,
                                    new Region(345, "Земля", null))));
                    setShopAddress(address);
                }},
                "{" +
                        "   'region': {" +
                        "       'id': 123, 'name': 'Москва', 'parent': {" +
                        "           'id': 234, 'name': 'Россия', 'parent': {" +
                        "               'id': 345, 'name': 'Земля'" +
                        "           }" +
                        "       }" +
                        "   }," +
                        "   'address': 'address'" +
                        "}"
        );
    }

    @Test
    public void testSerializeDeliveryWithRegionWithSubsidy() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setPrices(new ItemPrices() {{
                        setSubsidy(BigDecimal.TEN);
                    }});
                }},
                "{" +
                        "   'subsidy': 10" +
                        "}"
        );
    }

    @Test
    public void testSerializeCartRequestWithoutAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setRegion(new Region(123, "Москва", null,
                            new Region(234, "Россия", null,
                                    new Region(345, "Земля", null))));
                }},
                "{" +
                        "   'region': {" +
                        "       'id': 123, 'name': 'Москва', 'parent': {" +
                        "           'id': 234, 'name': 'Россия', 'parent': {" +
                        "               'id': 345, 'name': 'Земля'" +
                        "           }" +
                        "       }" +
                        "   }" +
                        "}"
        );
    }

    @Test
    public void testSerializeStatusRequest() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setShopDeliveryId("12345");
                    setType(DeliveryType.DELIVERY);
                    setPrice(new BigDecimal(1234L));
                    setServiceName("pochta");
                    setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
                    setRegion(new Region(123, "Москва", null,
                            new Region(234, "Россия", null,
                                    new Region(345, "Земля", null))));
                    setShopAddress(address);
                    setOutletId(2345L);
                    setParcels(Arrays.asList(orderShipment1, orderShipment2));
                }},
                "{" +
                        "   'shopDeliveryId': '12345'," +
                        "   'type': 'DELIVERY'," +
                        "   'price': 1234," +
                        "   'dates': {" +
                        "       'toDate': '23-05-2013'," +
                        "       'fromDate': '20-05-2013'" +
                        "   }," +
                        "   'serviceName': 'pochta'," +
                        "   'region': {" +
                        "       'id': 123, 'name': 'Москва', 'parent': {" +
                        "           'id': 234, 'name': 'Россия', 'parent': {" +
                        "               'id': 345, 'name': 'Земля'" +
                        "           }" +
                        "       }" +
                        "   }," +
                        "   'address': 'address'," +
                        "   'outlet': {'id': 2345}," +
                        "   'shipments': ['shipment-1', 'shipment-2']" +
                        "}"
        );

    }

    @Test
    public void testSerializeDeliveryWithLiftingOptionsFreeOrUnknown() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setLiftType(LiftType.FREE);
                    setLiftPrice(BigDecimal.TEN);
                }},
                "{" +
                        "   'liftType': 'FREE'," +
                        "   'liftPrice': 10" +
                        "}"
        );

        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setLiftType(LiftType.FREE);
                }},
                "{" +
                        "   'liftType': 'FREE'," +
                        "}"
        );

        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setLiftType(LiftType.UNKNOWN);
                }},
                "{}"
        );

        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setLiftType(null);
                    setLiftPrice(BigDecimal.valueOf(150L));
                }},
                "{}"
        );
    }

    @Test
    public void testSerializeDeliveryWithLiftingOptionsWithoutFloor() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setLiftType(LiftType.NOT_NEEDED);
                    setLiftPrice(BigDecimal.ZERO);
                }},
                "{" +
                        "   'liftType': 'NOT_NEEDED'," +
                        "   'liftPrice': 0" +
                        "}"
        );
    }

    @Test
    public void testSerializeDeliveryWithLiftingOptions() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion() {{
                    setLiftType(LiftType.CARGO_ELEVATOR);
                    setLiftPrice(BigDecimal.valueOf(150));
                    setShopAddress(address);
                }},
                "{" +
                        "   'liftType': 'CARGO_ELEVATOR'," +
                        "   'liftPrice': 150," +
                        "   'address': 'address'" +
                        "}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
                serializer,
                new DeliveryWithRegion(),
                "{}"
        );
    }
}
