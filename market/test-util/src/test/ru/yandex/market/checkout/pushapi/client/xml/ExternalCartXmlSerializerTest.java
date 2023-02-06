package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createOrderItem;

public class ExternalCartXmlSerializerTest {

    private DeliveryWithRegion deliveryWithRegion = mock(DeliveryWithRegion.class);
    private OrderItem item1 = createOrderItem("item1", 1l);
    private OrderItem item2 = createOrderItem("item2", 1l);
    
    private ExternalCartXmlSerializer serializer = new ExternalCartXmlSerializer();

    @Before
    public void setUp() throws Exception {
        serializer.setDeliveryWithRegionXmlSerializer(
            new DeliveryWithRegionXmlSerializer() {
                @Override
                public void serializeXml(DeliveryWithRegion value, PrimitiveXmlWriter writer) throws IOException {
                    assertEquals(deliveryWithRegion, value);
                    writer.addNode("delivery", "delivery");
                }
            }
        );
        serializer.setItemXmlSerializer(new OrderItemXmlSerializer() {
            @Override
            public void serializeXml(OrderItem value, PrimitiveXmlWriter writer) throws IOException {
                if(value == item1) {
                    writer.addNode("item", "item1");
                } else if(value == item2) {
                    writer.addNode("item", "item2");
                } else {
                    fail();
                }
            }
        });

    }

    @Test
    public void testSerialize() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ExternalCart() {{
                setDeliveryWithRegion(deliveryWithRegion);
                setItems(Arrays.asList(item1, item2));
                setCurrency(Currency.RUR);
            }},
            "<cart currency='RUR'>" +
                "        <delivery>delivery</delivery>" +
                "        <items>" +
                "            <item>item1</item>" +
                "            <item>item2</item>" +
                "        </items>" +
                "    </cart>"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ExternalCart(),
            "<cart />"
        );

    }
}
