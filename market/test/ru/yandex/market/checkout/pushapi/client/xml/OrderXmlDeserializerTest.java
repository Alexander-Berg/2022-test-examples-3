package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlDeserializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;

public class OrderXmlDeserializerTest {

    private Delivery delivery = mock(Delivery.class);
    private OrderItem item1 = (new OrderItemBuilder()).withOfferId("item1").withFeedId(1L).build();
    private OrderItem item2 = (new OrderItemBuilder()).withOfferId("item2").withFeedId(1L).build();

    private DeliveryXmlDeserializer deliveryXmlDeserializer;
    private OrderItemXmlDeserializer orderItemXmlDeserializer;

    private OrderXmlDeserializer orderXmlDeserializer = new OrderXmlDeserializer();

    @BeforeEach
    public void setUp() throws Exception {
        deliveryXmlDeserializer = XmlTestUtil.createDeserializerMock(
                DeliveryXmlDeserializer.class,
                new HashMap<String, Delivery>() {{
                    put("<delivery>delivery</delivery>", delivery);
                }}
        );
        orderItemXmlDeserializer = XmlTestUtil.createDeserializerMock(
                OrderItemXmlDeserializer.class,
                new HashMap<String, OrderItem>() {{
                    put("<item>item1</item>", item1);
                    put("<item>item2</item>", item2);
                }}
        );

        orderXmlDeserializer.setDeliveryXmlDeserializer(deliveryXmlDeserializer);
        orderXmlDeserializer.setOrderItemXmlDeserializer(orderItemXmlDeserializer);
        orderXmlDeserializer.setCheckoutDateFormat(new CheckoutDateFormat());
    }

    @Test
    public void testParsePrepaid() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='PREPAID'" +
                        "           fake='true' context='SANDBOX'>" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery>delivery</delivery>" +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertEquals(Currency.RUR, actual.getCurrency());
        assertEquals(delivery, actual.getDelivery());
        assertEquals(1234, actual.getId().longValue());
        assertEquals(Arrays.asList(item1, item2), new ArrayList<>(actual.getItems()));
        assertEquals(PaymentType.PREPAID, actual.getPaymentType());
        assertNull(actual.getPaymentMethod());
        assertEquals("notes-notes-notes", actual.getNotes());
        assertEquals(true, actual.isFake());
        assertEquals(Context.SANDBOX, actual.getContext());
    }

    @Test
    public void testParsePostpaid() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='POSTPAID'" +
                        "           payment-method='CARD_ON_DELIVERY'>" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery>delivery</delivery>" +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertEquals(Currency.RUR, actual.getCurrency());
        assertEquals(delivery, actual.getDelivery());
        assertEquals(1234, actual.getId().longValue());
        assertEquals(Arrays.asList(item1, item2), new ArrayList<>(actual.getItems()));
        assertEquals(PaymentType.POSTPAID, actual.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actual.getPaymentMethod());
        assertEquals("notes-notes-notes", actual.getNotes());
        assertNull(actual.getContext());
    }

    @Test
    public void testParseRGB() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='POSTPAID'" +
                        "           payment-method='CARD_ON_DELIVERY'" +
                        "           rgb='BLUE'" +
                        ">" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery>delivery</delivery>" +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertEquals(Color.BLUE, actual.getRgb());
    }

    @Test
    public void testParseRGBIfEmpty() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='POSTPAID'" +
                        "           payment-method='CARD_ON_DELIVERY'" +
                        ">" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery>delivery</delivery>" +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertNull(actual.getRgb());
    }

    @Test
    public void testParseRGBIfNotBLUE() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='POSTPAID'" +
                        "           payment-method='CARD_ON_DELIVERY'" +
                        "           rgb='RED'" +
                        ">" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery>delivery</delivery>" +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertNull(actual.getRgb());
    }

    @Test
    public void testParseEmpty() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "<order />"
        );

        assertNotNull(actual);
    }

    @Test
    public void testFullParse() throws Exception {
        final Order order = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "<order id='1234'" +
                        "   status='CANCELLED'" +
                        "   substatus='USER_CHANGED_MIND'" +
                        "   creation-date='06-07-2013 15:30:40'" +
                        "   currency='RUR'" +
                        "   items-total='10.75'" +
                        "   total='11.43'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='true' context='SANDBOX'>" +
                        "       <items>" +
                        "           <item>item1</item>" +
                        "           <item>item2</item>" +
                        "       </items>" +
                        "       <delivery>delivery</delivery>" +
                        "       <buyer id='blah' uid='2345' last-name='Иванов' first-name='Иван' middle-name='Иваныч' phone='123456789' email='vanya@localhost' />" +
                        "</order>"
        );

        assertEquals(1234, order.getId().longValue());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OrderSubstatus.USER_CHANGED_MIND, order.getSubstatus());
        assertEquals(createLongDate("2013-07-06 15:30:40"), order.getCreationDate());
        assertEquals(Currency.RUR, order.getCurrency());
        assertEquals(new BigDecimal("10.75"), order.getItemsTotal());
        assertEquals(new BigDecimal("11.43"), order.getTotal());
        assertEquals(PaymentType.PREPAID, order.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, order.getPaymentMethod());
        assertEquals(true, order.isFake());
        assertEquals(Context.SANDBOX, order.getContext());
        assertEquals(Arrays.asList(item1, item2), new ArrayList<>(order.getItems()));
        assertEquals(delivery, order.getDelivery());

        final Buyer buyer = order.getBuyer();
        assertNotNull(buyer);
        assertEquals("blah", buyer.getId());
        assertEquals(2345L, buyer.getUid().longValue());
        assertEquals("Иванов", buyer.getLastName());
        assertEquals("Иван", buyer.getFirstName());
        assertEquals("Иваныч", buyer.getMiddleName());
        assertEquals("123456789", buyer.getPhone());
        assertEquals("vanya@localhost", buyer.getEmail());
    }

    @Test
    public void testParseStatusSubstatus() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "<order id='1234' status='CANCELLED' substatus='USER_CHANGED_MIND'/>"
        );

        assertEquals(1234, actual.getId().longValue());
        assertEquals(OrderStatus.CANCELLED, actual.getStatus());
        assertEquals(OrderSubstatus.USER_CHANGED_MIND, actual.getSubstatus());
    }

    @Test
    public void testParseCertificateId() throws Exception {
        final Order actual = XmlTestUtil.deserialize(
                orderXmlDeserializer,
                "<order id='1234' certificateId='123'/>"
        );

        assertEquals(1234, actual.getId().longValue());
        assertEquals(123L, actual.getExternalCertificateId().longValue());
        assertTrue(actual.hasCertificate());
    }
}
