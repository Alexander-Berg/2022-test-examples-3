package ru.yandex.market.checkout.pushapi.out.shopApi.json;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderResponseJsonDeserializerTest {

    private OrderResponseJsonDeserializer deserializer = new OrderResponseJsonDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        deserializer.setCheckoutDateFormat(new CheckoutDateFormat());
    }

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
                "{'order': {'accepted': true, shipmentDate: '23-02-2021'}}"
        );

        assertTrue(actual.isAccepted());
        assertEquals(Date.from(LocalDate.of(2021, 2, 23)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()), actual.getShipmentDate());
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
