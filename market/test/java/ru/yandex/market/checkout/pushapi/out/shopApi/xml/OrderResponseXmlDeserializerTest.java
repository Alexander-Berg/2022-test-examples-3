package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.OrderResponseXmlDeserializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OrderResponseXmlDeserializerTest {

    private OrderResponseXmlDeserializer deserializer = new OrderResponseXmlDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        deserializer.setCheckoutDateFormat(new CheckoutDateFormat());
    }

    @Test
    public void testParseAccepted() throws Exception {
        final OrderResponse actual = XmlTestUtil.deserialize(
                deserializer,
                "<order accepted='true' shipment-date='20-02-2021'/>"
        );

        assertEquals(true, actual.isAccepted());
        assertNull(actual.getReason());
        assertEquals(createExpectedDate(), actual.getShipmentDate());
    }

    @Test
    public void testParseDeclined() throws Exception {
        final OrderResponse actual = XmlTestUtil.deserialize(
                deserializer,
                "<order id='1234' accepted='false' reason='OTHER' />"
        );

        assertEquals(false, actual.isAccepted());
        assertEquals(DeclineReason.OTHER, actual.getReason());
        assertEquals("1234", actual.getId());
    }

    @Test
    public void testParseEmpty() throws Exception {
        final OrderResponse actual = XmlTestUtil.deserialize(
                deserializer,
                "<order />"
        );

        assertNotNull(actual);
    }

    @NotNull
    private Date createExpectedDate() {
        return Date.from(LocalDate.of(2021, 2, 20)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
    }
}
