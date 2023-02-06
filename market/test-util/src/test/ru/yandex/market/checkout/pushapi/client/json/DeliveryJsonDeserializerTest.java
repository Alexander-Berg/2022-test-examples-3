package ru.yandex.market.checkout.pushapi.client.json;

import org.junit.Test;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeliveryJsonDeserializerTest {
    
    private DeliveryResponseJsonDeserializer deserializer = new DeliveryResponseJsonDeserializer();

    @Test
    public void testDeserialize() throws Exception {
        final Delivery actual = JsonTestUtil.deserialize(
            deserializer,
            "{'type':'DELIVERY'," +
                " 'id': '12345'," +
                " 'price': 2345," +
                " 'dates': {" +
                "   'toDate': '23-05-2013'," +
                "   'fromDate': '20-05-2013'" +
                "  }," +
                " 'outlets':[{'id': 3456}, {'id': 4567}, {'id': 5678}]," +
                " 'serviceName': '234'}"
        );

        assertEquals("12345", actual.getId());
        assertEquals(DeliveryType.DELIVERY, actual.getType());
        assertEquals(new BigDecimal(2345l), actual.getPrice());
        assertEquals(
            new DeliveryDates(XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")),
            actual.getDeliveryDates()
        );
        assertEquals(Arrays.asList(3456l, 4567l, 5678l), actual.getOutletIds());
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
}
