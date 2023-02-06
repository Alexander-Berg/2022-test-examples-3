package ru.yandex.market.checkout.pushapi.client.json;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.json.order.AddressJsonSerializer;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class DeliveryWithRegionJsonSerializerTest {
    
    private Address address = mock(Address.class);
    private DeliveryWithRegionJsonSerializer serializer = new DeliveryWithRegionJsonSerializer();

    @Before
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

    }

    @Test
    public void testSerializeAcceptRequest() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new DeliveryWithRegion() {{
                setId("12345");
                setType(DeliveryType.DELIVERY);
                setPrice(new BigDecimal(1234l));
                setServiceName("pochta");
                setDeliveryDates(new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")));
//                setRegion(new Region(123, "Москва", new Region(234, "Россия", new Region(345, "Земля", ))));
                setAddress(address);
                setOutletId(2345l);
            }},
            "{" +
                "   'id': '12345'," +
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
                "   'outlet': {'id': 2345}" +
                "}"
        );

    }

    @Test
    public void testSerializeCartRequestWithAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new DeliveryWithRegion() {{
//                setRegion(new Region(123, "Москва", new Region(234, "Россия", new Region(345, "Земля"))));
                setAddress(address);
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
    public void testSerializeCartRequestWithoutAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new DeliveryWithRegion() {{
//                setRegion(new Region(123, "Москва", new Region(234, "Россия", new Region(345, "Земля"))));
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
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new DeliveryWithRegion(),
            "{}"
        );
    }
}
