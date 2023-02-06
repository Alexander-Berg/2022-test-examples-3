package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.common.xml.AbstractXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createOrderItem;

public class ShopOrderXmlSerializerTest {

    private ShopOrderXmlSerializer serializer = new ShopOrderXmlSerializer();
    private DeliveryWithRegionXmlSerializer deliveryWithRegionXmlSerializer
        = mock(DeliveryWithRegionXmlSerializer.class);
    private OrderItemXmlSerializer orderItemXmlSerializer = mock(OrderItemXmlSerializer.class);
    private DeliveryWithRegion deliveryWithRegion = mock(DeliveryWithRegion.class);
    private OrderItem offerItem1 = createOrderItem("item1", 1l);
    private OrderItem offerItem2 = createOrderItem("item2", 1l);

    @Before
    public void setUp() throws Exception {
        XmlTestUtil.initMockSerializer(
            deliveryWithRegionXmlSerializer,
            deliveryWithRegion,
            new XmlTestUtil.MockSerializer() {
                @Override
                public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                    writer.addNode("delivery", "delivery");
                }
            }
        );
        XmlTestUtil.initMockSerializer(
            orderItemXmlSerializer,
            offerItem1,
            new XmlTestUtil.MockSerializer() {
                @Override
                public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                    writer.addNode("item", "item1");
                }
            }
        );
        XmlTestUtil.initMockSerializer(
            orderItemXmlSerializer,
            offerItem2,
            new XmlTestUtil.MockSerializer() {
                @Override
                public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
                    writer.addNode("item", "item2");
                }
            }
        );

        serializer.setDeliveryWithRegionXmlSerializer(deliveryWithRegionXmlSerializer);
        serializer.setOrderItemXmlSerializer(orderItemXmlSerializer);
    }

    @Test
    public void testPostpaid() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setCurrency(Currency.RUR);
                setPaymentType(PaymentType.POSTPAID);
                setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
                setItems(
                    Arrays.asList(
                    offerItem1,
                    offerItem2
                    )
                );
                setDeliveryWithRegion(deliveryWithRegion);
                setNotes("notes-notes-notes");
                setFake(true);
            }},
            "<order id='1234' currency='RUR' payment-type='POSTPAID' payment-method='CASH_ON_DELIVERY'" +
                "   fake='true'>" +
                "    <items>" +
                "        <item>item1</item>" +
                "        <item>item2</item>" +
                "    </items>" +
                "    <delivery>delivery</delivery>" +
                "    <notes>notes-notes-notes</notes>" +
                "</order>"
        );
    }

    @Test
    public void testPrepaid() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setCurrency(Currency.RUR);
                setPaymentType(PaymentType.PREPAID);
                setItems(
                    Arrays.asList(
                        offerItem1,
                        offerItem2
                    )
                );
                setDeliveryWithRegion(deliveryWithRegion);
                setNotes("notes-notes-notes");
            }},
            "<order id='1234' currency='RUR' payment-type='PREPAID'>" +
                "    <items>" +
                "        <item>item1</item>" +
                "        <item>item2</item>" +
                "    </items>" +
                "    <delivery>delivery</delivery>" +
                "    <notes>notes-notes-notes</notes>" +
                "</order>"
        );
    }

    @Test
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ShopOrder(),
            "<order />"
        );
    }

    @Test
    public void testSerializeCancelled() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setStatus(OrderStatus.CANCELLED);
                setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
                setCreationDate(createLongDate("2013-07-06 15:30:40"));
                setCurrency(Currency.RUR);
                setItemsTotal(new BigDecimal("10.75"));
                setTotal(new BigDecimal("11.43"));
                setPaymentType(PaymentType.PREPAID);
                setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
                setFake(true);
                setItems(Arrays.asList(offerItem1, offerItem2));
                setDeliveryWithRegion(deliveryWithRegion);
                setBuyer(new Buyer() {{
                    setId("blah");
                    setLastName("Иванов");
                    setFirstName("Иван");
                    setMiddleName("Иваныч");
                    setPhone("123456789");
                    setEmail("vanya@localhost");
                }});
            }},
            "<order id='1234' status='CANCELLED'" +
                "   substatus='USER_CHANGED_MIND'" +
                "   creation-date='06-07-2013 15:30:40'" +
                "   currency='RUR'" +
                "   items-total='10.75'" +
                "   total='11.43'" +
                "   payment-type='PREPAID'" +
                "   payment-method='CARD_ON_DELIVERY'" +
                "   fake='true'>" +
                "       <items>" +
                "           <item>item1</item>" +
                "           <item>item2</item>" +
                "       </items>" +
                "       <delivery>delivery</delivery>" +
                "       <buyer id='blah' last-name='Иванов' first-name='Иван' middle-name='Иваныч' phone='123456789' email='vanya@localhost' />" +
                "</order>"
        );
    }

    @Test
    public void testSerializeNotCancelled() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
            serializer,
            new ShopOrder() {{
                setId(1234l);
                setStatus(OrderStatus.DELIVERED);
            }},
            "<order id='1234' status='DELIVERED'/>"
        );
    }
}
