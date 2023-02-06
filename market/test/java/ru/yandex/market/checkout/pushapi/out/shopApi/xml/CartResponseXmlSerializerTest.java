package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.xml.AbstractXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.in.xml.CartResponseXmlSerializer;
import ru.yandex.market.checkout.pushapi.in.xml.DeliveryResponseXmlSerializer;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.mock;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class CartResponseXmlSerializerTest {
    private static EnhancedRandom enhancedRandom = EnhancedRandomHelper.createEnhancedRandom();

    private OrderItemXmlSerializer orderItemXmlSerializer = mock(OrderItemXmlSerializer.class);
    private DeliveryResponseXmlSerializer deliveryXmlSerializer = mock(DeliveryResponseXmlSerializer.class);
    private CartResponseXmlSerializer serializer = new CartResponseXmlSerializer();


    @BeforeEach
    public void setUp() throws Exception {
        serializer.setOrderItemXmlSerializer(orderItemXmlSerializer);
        serializer.setDeliveryResponseXmlSerializer(deliveryXmlSerializer);
    }

    @RepeatedTest(10)
    public void testSerialize() throws Exception {
        final OrderItem item1 = enhancedRandom.nextObject(OrderItem.class);
        final OrderItem item2 = enhancedRandom.nextObject(OrderItem.class);
        final DeliveryResponse delivery1 = enhancedRandom.nextObject(DeliveryResponse.class, "parcels.route", "shipments.route", "shipment.route");
        final DeliveryResponse delivery2 = enhancedRandom.nextObject(DeliveryResponse.class, "parcels.route", "shipments.route", "shipment.route");

        XmlTestUtil.initMockSerializer(orderItemXmlSerializer, item1, new XmlTestUtil.MockSerializer() {
            @Override
            public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                writer.addNode("item", "item1");
            }
        });
        XmlTestUtil.initMockSerializer(orderItemXmlSerializer, item2, new XmlTestUtil.MockSerializer() {
            @Override
            public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                writer.addNode("item", "item2");
            }
        });

        XmlTestUtil.initMockSerializer(deliveryXmlSerializer, delivery1, new XmlTestUtil.MockSerializer() {
            @Override
            public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                writer.addNode("delivery", "delivery1");
            }
        });
        XmlTestUtil.initMockSerializer(deliveryXmlSerializer, delivery2, new XmlTestUtil.MockSerializer() {
            @Override
            public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                writer.addNode("delivery", "delivery2");
            }
        });

        CartResponse cartResponse = enhancedRandom.nextObject(CartResponse.class, "shopAdmin", "deliveryCurrency", "taxSystem",
                "deliveryOptions.parcels.route", "deliveryOptions.shipments.route", "deliveryOptions.shipment.route");
        cartResponse.setItems(Arrays.asList(item1, item2));
        cartResponse.setDeliveryOptions(Arrays.asList(delivery1, delivery2));
        cartResponse.setPaymentMethods(Arrays.asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.YANDEX_MONEY));

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cartResponse,
                "<cart shop-admin=\"false\">" +
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
    }

    @RepeatedTest(10)
    public void testSerializeDeliveryCurrency() throws Exception {
        CartResponse cartResponse = new CartResponse();
        cartResponse.setDeliveryCurrency(Currency.USD);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cartResponse,
                "<cart shop-admin=\"false\" delivery-currency=\"USD\"/>"
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new CartResponse(),
                "<cart shop-admin=\"false\"/>"
        );
    }
}
