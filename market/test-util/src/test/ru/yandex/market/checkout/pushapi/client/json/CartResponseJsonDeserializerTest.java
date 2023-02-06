package ru.yandex.market.checkout.pushapi.client.json;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.json.JsonReader;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.json.order.OrderItemJsonDeserializer;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX_MONEY;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createOrderItem;

public class CartResponseJsonDeserializerTest {
    
    private Delivery delivery1 = mock(Delivery.class);
    private Delivery delivery2 = mock(Delivery.class);
    private OrderItem item1 = createOrderItem("item1", 1l);
    private OrderItem item2 = createOrderItem("item2", 1l);
    
    private CartResponseJsonDeserializer deserializer = new CartResponseJsonDeserializer();

    @Before
    public void setUp() throws Exception {
        deserializer.setDeliveryJsonDeserializer(
            new DeliveryResponseJsonDeserializer() {
                @Override
                public Delivery deserialize(JsonReader jsonParser) throws IOException {
                    switch(jsonParser.getString("val")) {
                        case "delivery1":
                            return delivery1;
                        case "delivery2":
                            return delivery2;
                        default:
                            fail();
                            return null;
                    }
                }
            }
        );
        deserializer.setOrderItemJsonDeserializer(
            new OrderItemJsonDeserializer() {
                @Override
                public OrderItem deserialize(JsonReader jsonParser) throws IOException {
                    switch(jsonParser.getString("val")) {
                        case "item1":
                            return item1;
                        case "item2":
                            return item2;
                        default:
                            fail();
                            return null;
                    }
                }
            }
        );

    }

    @Test
    public void testDeserialize() throws Exception {
        final CartResponse actual = JsonTestUtil.deserialize(
            deserializer,
            "{'cart': {'items': [{'val': 'item1'}, {'val': 'item2'}], 'deliveryOptions': [{'val': 'delivery1'}, {'val': 'delivery2'}]," +
                "'paymentMethods': ['CASH_ON_DELIVERY', 'YANDEX_MONEY']}}"
        );

        assertEquals(Arrays.asList(item1, item2), actual.getItems());
        assertEquals(Arrays.asList(delivery1, delivery2), actual.getDeliveryOptions());
        assertEquals(Arrays.asList(CASH_ON_DELIVERY, YANDEX_MONEY), actual.getPaymentMethods());
    }

    @Test
    public void testDeserializeEmpty() throws Exception {
        JsonTestUtil.deserialize(
            deserializer,
            "{'cart': {}}"
        );

    }
}
