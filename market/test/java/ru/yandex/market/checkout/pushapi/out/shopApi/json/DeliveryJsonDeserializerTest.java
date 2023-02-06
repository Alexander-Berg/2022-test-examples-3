package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeliveryJsonDeserializerTest {

    private DeliveryResponseJsonDeserializer deserializer = new DeliveryResponseJsonDeserializer();

    @Test
    public void testDeserialize() throws Exception {
        final Delivery actual = JsonTestUtil.deserialize(
                deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[{'id': 3456}, {'id': 4567}, {'id': 5678}, {'id': 5678}]," +
                        " 'serviceName': '234'}"
        );

        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals(new BigDecimal(2345l), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );
        assertEquals(Set.of(3456l, 4567l, 5678l), actual.getOutletIdsSet());
        assertEquals("234", actual.getServiceName());
    }

    @Test
    public void testDeserializeWithOutletCode() throws Exception {
        final Delivery actual = JsonTestUtil.deserialize(
                deserializer,
                "{'type':'DELIVERY'," +
                        " 'id': '12345'," +
                        " 'shopDeliveryId': '54321'," +
                        " 'hash': 'vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc='," +
                        " 'price': 2345," +
                        " 'dates': {" +
                        "   'toDate': '23-05-2013'," +
                        "   'fromDate': '20-05-2013'" +
                        "  }," +
                        " 'outlets':[{'code': 'str3456'}, {'code': 'str4567'}, {'code': 'str5678'}, {'code': 'str5678'}]," +
                        " 'serviceName': '234'}"
        );

        assertEquals("12345", actual.getId());
        assertEquals("54321", actual.getShopDeliveryId());
        assertEquals("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=", actual.getHash());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals(new BigDecimal(2345l), actual.getPrice());
        assertEquals(
                new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
                actual.getDeliveryDates()
        );
        assertEquals(Set.of("str3456", "str4567", "str5678"), actual.getOutletCodes());
        assertEquals("234", actual.getServiceName());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final Delivery actual = JsonTestUtil.deserialize(
                deserializer,
                "{}"
        );

        assertNotNull(actual);
    }

    @Test
    public void testDeserializeWithIntervals() throws Exception {
        final Delivery actual = JsonTestUtil.deserialize(
                deserializer,
                "{" +
                        "  'type': 'DELIVERY'," +
                        "  'id': '12345'," +
                        "  'dates': {" +
                        "    'intervals': [" +
                        "      {" +
                        "      'date': '15-09-2018'," +
                        "      'fromTime': '08:00'," +
                        "      'toTime': '12:00'" +
                        "      }," +
                        "      {" +
                        "      'date': '15-09-2018'," +
                        "      'fromTime': '11:00'," +
                        "      'toTime': '15:00'" +
                        "      }," +
                        "      {" +
                        "      'date': '17-09-2018'" +
                        "      }" +
                        "    ]" +
                        "  }" +
                        "}"
        );

        assertEquals(2, actual.getRawDeliveryIntervals().getDates().size());
    }
}
