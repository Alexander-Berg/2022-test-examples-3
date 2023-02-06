package ru.yandex.market.checkout.pushapi.client.json;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.json.JsonWriter;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.json.order.OrderItemJsonSerializer;
import ru.yandex.market.checkout.pushapi.client.util.test.JsonTestUtil;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createOrderItem;

public class ExternalCartJsonSerializerTest {
    
    private DeliveryWithRegion deliveryWithRegion = mock(DeliveryWithRegion.class);
    private OrderItem item1 = createOrderItem("item1", 1l);
    private OrderItem item2 = createOrderItem("item2", 1l);
    
    private ExternalCartJsonSerializer serializer = new ExternalCartJsonSerializer();

    @Before
    public void setUp() throws Exception {
        serializer.setDeliveryWithRegionJsonSerializer(
            new DeliveryWithRegionJsonSerializer() {
                @Override
                public void serialize(DeliveryWithRegion value, JsonWriter writer) throws IOException {
                    assertEquals(deliveryWithRegion, value);
                    writer.setValue("delivery");
                }
            }
        );
        serializer.setItemJsonSerializer(
            new OrderItemJsonSerializer() {
                @Override
                public void serialize(OrderItem value, JsonWriter generator) throws IOException {
                    if(item1 == value) {
                        generator.setValue("item1");
                    } else if(item2 == value) {
                        generator.setValue("item2");
                    } else {
                        fail();
                    }
                }
            }
        );

    }

    @Test
    public void testSerializeWithAddress() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ExternalCart(
                deliveryWithRegion,
                Currency.RUR,
                Arrays.asList(item1, item2)
            ),
            "{'cart': {'currency': 'RUR'," +
                "'items': ['item1', 'item2']," +
                "'delivery': 'delivery'}}"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        JsonTestUtil.assertJsonSerialize(
            serializer,
            new ExternalCart(),
            "{'cart': {}}"
        );
    }
}
