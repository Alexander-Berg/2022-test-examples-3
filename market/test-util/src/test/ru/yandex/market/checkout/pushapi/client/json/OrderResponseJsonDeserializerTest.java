package ru.yandex.market.checkout.pushapi.client.json;

import org.junit.Test;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;

import static org.junit.Assert.*;

public class OrderResponseJsonDeserializerTest {
    
    private OrderResponseJsonDeserializer deserializer = new OrderResponseJsonDeserializer();

    @Test
    public void testDeserializeDeclined() throws Exception {
        final OrderResponse actual = JsonTestUtil.deserialize(
            deserializer,
            "{'order': {'id': '1234', 'accepted': false, 'reason': 'OUT_OF_DATE'}}"
        );

        assertFalse(actual.isAccepted());
        assertEquals(DeclineReason.OUT_OF_DATE, actual.getReason());
    }

    @Test
    public void testDeserializeAccepted() throws Exception {
        final OrderResponse actual = JsonTestUtil.deserialize(
            deserializer,
            "{'order': {'accepted': true}}"
        );

        assertTrue(actual.isAccepted());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        final OrderResponse actual = JsonTestUtil.deserialize(
            deserializer,
            "{'order': {}}"
        );

        assertNotNull(actual);
    }
}
