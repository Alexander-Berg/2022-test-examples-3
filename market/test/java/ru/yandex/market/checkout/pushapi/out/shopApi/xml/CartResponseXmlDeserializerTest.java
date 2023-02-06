package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.CartResponseXmlDeserializer;
import ru.yandex.market.checkout.pushapi.client.xml.DeliveryResponseXmlDeserializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlDeserializer;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class CartResponseXmlDeserializerTest {

    final OrderItem item1 = mock(OrderItem.class);
    final OrderItem item2 = mock(OrderItem.class);
    final DeliveryResponse delivery1 = mock(DeliveryResponse.class);
    final DeliveryResponse delivery2 = mock(DeliveryResponse.class);

    private OrderItemXmlDeserializer orderItemXmlDeserializer;
    private DeliveryResponseXmlDeserializer deliveryXmlDeserializer;
    private CartResponseXmlDeserializer deserializer = new CartResponseXmlDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        orderItemXmlDeserializer = XmlTestUtil.createDeserializerMock(
                OrderItemXmlDeserializer.class,
                new HashMap<String, OrderItem>() {{
                    put("<item>item1</item>", item1);
                    put("<item>item2</item>", item2);
                }}
        );
        deliveryXmlDeserializer = XmlTestUtil.createDeserializerMock(
                DeliveryResponseXmlDeserializer.class,
                new HashMap<String, DeliveryResponse>() {{
                    put("<delivery>delivery1</delivery>", delivery1);
                    put("<delivery>delivery2</delivery>", delivery2);
                }}
        );
        deserializer.setOrderItemXmlDeserializer(orderItemXmlDeserializer);
        deserializer.setDeliveryResponseXmlDeserializer(deliveryXmlDeserializer);
    }

    @Test
    public void testParse() throws Exception {
        final CartResponse actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart>" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery-options>" +
                        "            <delivery>delivery1</delivery>" +
                        "            <delivery>delivery2</delivery>" +
                        "        </delivery-options>" +
                        "        <payment-methods>" +
                        "            <payment-method>CASH_ON_DELIVERY</payment-method>" +
                        "            <payment-method>YANDEX_MONEY</payment-method>" +
                        "        </payment-methods>" +
                        "    </cart>"
        );

        assertEquals(Arrays.asList(item1, item2), actual.getItems());
        assertEquals(Arrays.asList(delivery1, delivery2), actual.getDeliveryOptions());
        assertEquals(
                Arrays.asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.YANDEX_MONEY),
                actual.getPaymentMethods()
        );
    }

    @Test
    public void testParseDeliveryCurrency() throws Exception {
        CartResponse deserialize = XmlTestUtil.deserialize(
                deserializer,
                "<cart delivery-currency=\"USD\">\n" +
                        "        <items>\n" +
                        "            <item>item1</item>\n" +
                        "            <item>item2</item>\n" +
                        "        </items>\n" +
                        "        <delivery-options>\n" +
                        "            <delivery>delivery1</delivery>\n" +
                        "            <delivery>delivery2</delivery>\n" +
                        "        </delivery-options>\n" +
                        "        <payment-methods>\n" +
                        "            <payment-method>CASH_ON_DELIVERY</payment-method>\n" +
                        "            <payment-method>YANDEX_MONEY</payment-method>\n" +
                        "        </payment-methods>\n" +
                        "    </cart>"
        );

        assertEquals(Currency.USD, deserialize.getDeliveryCurrency());
    }

    @Test
    public void testParseEmpty() throws Exception {
        final CartResponse actual = XmlTestUtil.deserialize(
                deserializer,
                "<cart />"
        );

        assertNotNull(actual);
        assertNull(actual.getItems());
        assertNull(actual.getDeliveryOptions());
        assertNull(actual.getPaymentMethods());
    }
}
