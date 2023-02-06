package ru.yandex.market.checkout.pushapi.client.xml;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.serialize;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class OrderXmlSerializerTest {

    private static final EnhancedRandom RANDOM = EnhancedRandomHelper.createEnhancedRandom();

    private Delivery delivery = mock(Delivery.class);
    private OrderItem item1 = (new OrderItemBuilder()).withOfferId("item1").withFeedId(1L).build();
    private OrderItem item2 = (new OrderItemBuilder()).withOfferId("item2").withFeedId(1L).build();

    private DeliveryXmlSerializer deliveryXmlSerializer = mock(DeliveryXmlSerializer.class);
    private OrderItemXmlSerializer itemXmlSerializer = mock(OrderItemXmlSerializer.class);

    private OrderXmlSerializer serializer = new OrderXmlSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        XmlTestUtil.initMockSerializer(deliveryXmlSerializer, delivery, new XmlTestUtil.SimpleMockSerializer("delivery", "delivery"));
        XmlTestUtil.initMockSerializer(itemXmlSerializer, item1, new XmlTestUtil.SimpleMockSerializer("item", "item1"));
        XmlTestUtil.initMockSerializer(itemXmlSerializer, item2, new XmlTestUtil.SimpleMockSerializer("item", "item2"));

        serializer.setDeliveryXmlSerializer(deliveryXmlSerializer);
        serializer.setOrderItemXmlSerializer(itemXmlSerializer);
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
    }

    @RepeatedTest(10)
    public void testSerializePrepaid() throws Exception {
        final Order order = RANDOM.nextObject(Order.class,
                "status", "substatus", "creationDate", "itemsTotal", "total", "acceptMethod",
                "paymentMethod", "buyer", "preorder", "rgb", "externalCertificateId", "itemsByFeedOffer", "items",
                "changeRequests", "delivery.parcels.route", "delivery.shipments.route", "delivery.shipment.route",
                "buyerItemsTotal", "buyerTotal"
        );
        order.setCurrency(Currency.RUR);
        order.setDelivery(delivery);
        order.setId(1234l);
        order.setItems(Arrays.asList(item1, item2));
        order.setPaymentType(PaymentType.PREPAID);
        order.setNotes("notes-notes-notes");
        order.setFake(false);
        order.setContext(Context.MARKET);
        order.setTaxSystem(TaxSystem.ENVD);

        final String result = serialize(serializer, order);

        assertThat(result, is(sameXmlAs(
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           fulfilment=\"false\"" +
                        "           payment-type='PREPAID'" +
                        "           fake='false' accept-method=\"0\" context='MARKET'" +
                        "           tax-system='ENVD' subsidy-total='0'>" +
                        "        <items>" +
                        "            <item>item1</item>" +
                        "            <item>item2</item>" +
                        "        </items>" +
                        "        <delivery>delivery</delivery>" +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        )));
    }

    @RepeatedTest(10)
    public void serializesPaymentMethod() throws Exception {
        final Order order = new Order() {{
            setPaymentType(PaymentType.POSTPAID);
            setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        }};

        final String result = serialize(serializer, order);

        assertThat(result,
                is(sameXmlAs("<order fulfilment=\"false\" payment-type='POSTPAID' payment-method='CASH_ON_DELIVERY'" +
                        " accept-method=\"0\" fake=\"false\" subsidy-total='0'/>")));
    }

    @RepeatedTest(10)
    public void serializesEmptyOrder() throws Exception {
        final Order order = new Order();

        final String result = serialize(serializer, order);

        assertThat(result, is(sameXmlAs("<order fulfilment=\"false\" accept-method=\"0\" fake=\"false\" subsidy-total='0'/>")));
    }

    @RepeatedTest(10)
    public void serializesRGB() throws Exception {
        final Order order = new Order();
        order.setRgb(Color.BLUE);

        final String result = serialize(serializer, order);

        assertThat(result,
                is(sameXmlAs("<order rgb=\"BLUE\" fulfilment=\"false\" accept-method=\"0\" fake=\"false\" subsidy-total='0'/>")));
    }

    @RepeatedTest(10)
    public void serializesRGBNotBLUE() throws Exception {
        final Order order = new Order();
        order.setRgb(Color.RED);

        final String result = serialize(serializer, order);

        assertThat(result, is(sameXmlAs("<order rgb=\"RED\" fulfilment=\"false\" accept-method=\"0\" fake=\"false\" subsidy-total='0'/>")));
    }

    @RepeatedTest(10)
    public void testSerializeCancelled() throws Exception {
        Buyer buyer = RANDOM.nextObject(Buyer.class, "uuid", "muid", "externalCertificateId");
        buyer.setId("blah");
        buyer.setUid(2345l);
        buyer.setLastName("Иванов");
        buyer.setFirstName("Иван");
        buyer.setMiddleName("Иваныч");
        buyer.setPhone("123456789");
        buyer.setEmail("vanya@localhost");
        buyer.setYandexUid("9092990991508455157");

        final Order order = RANDOM.nextObject(Order.class, "delivery", "items", "acceptMethod", "buyer",
                "notes", "preorder", "rgb", "externalCertificateId", "itemsByFeedOffer", "items", "changeRequests");
        order.setId(1234L);
        order.setStatus(OrderStatus.CANCELLED);
        order.setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
        order.setCreationDate(createLongDate("2013-07-06 15:30:40"));
        order.setCurrency(Currency.RUR);
        order.setItemsTotal(new BigDecimal("10.75"));
        order.setTotal(new BigDecimal("11.43"));
        order.setBuyerItemsTotal(new BigDecimal("10.75"));
        order.setBuyerTotal(new BigDecimal("11.43"));
        order.setPaymentType(PaymentType.PREPAID);
        order.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        order.setFake(true);
        order.setContext(Context.SANDBOX);
        order.setItems(Arrays.asList(item1, item2));
        order.setDelivery(delivery);
        order.setBuyer(buyer);
        order.setTaxSystem(TaxSystem.USN_MINUS_COST);

        final String result = serialize(serializer, order);

        assertThat(result, sameXmlAs(
                "<order id='1234'" +
                        "   status='CANCELLED'" +
                        "   substatus='USER_CHANGED_MIND'" +
                        "   creation-date='06-07-2013 15:30:40'" +
                        "   currency='RUR'" +
                        "   fulfilment=\"false\"" +
                        "   items-total='10.75'" +
                        "   total='11.43'" +
                        "   buyer-items-total='10.75'" +
                        "   buyer-total='11.43'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='true'" +
                        "   context='SANDBOX'" +
                        "   accept-method=\"0\" tax-system='USN_MINUS_COST' subsidy-total='0'>" +
                        "       <items>" +
                        "           <item>item1</item>" +
                        "           <item>item2</item>" +
                        "       </items>" +
                        "       <delivery>delivery</delivery>" +
                        "       <buyer id='blah' uid='2345' last-name='Иванов' first-name='Иван' middle-name='Иваныч' phone='123456789' email='vanya@localhost' />" +
                        "</order>"
        ));
    }

    @RepeatedTest(10)
    public void testSerializeNotCancelled() throws Exception {
        final Order order = new Order() {{
            setId(1234L);
            setStatus(OrderStatus.DELIVERED);
        }};

        final String result = serialize(serializer, order);

        assertThat(result, is(sameXmlAs(
                "<order fulfilment=\"false\" id='1234' status='DELIVERED' accept-method=\"0\" fake=\"false\" subsidy-total='0'/>"
        )));
    }

    @RepeatedTest(10)
    public void testSerializeBuyerPrices() throws Exception {
        final Order order = new Order() {{
            setId(1234L);
            setStatus(OrderStatus.DELIVERED);
            setBuyerItemsTotal(BigDecimal.valueOf(100));
            setBuyerTotal(BigDecimal.valueOf(120));
            getPromoPrices().setBuyerItemsTotalBeforeDiscount(BigDecimal.valueOf(150));
            getPromoPrices().setBuyerTotalBeforeDiscount(BigDecimal.valueOf(170));
        }};

        final String result = serialize(serializer, order);

        assertThat(result, is(sameXmlAs(
                "<order fulfilment=\"false\" id='1234' status='DELIVERED' accept-method=\"0\" fake=\"false\" " +
                        "subsidy-total='0' buyer-total='120' buyer-items-total='100' " +
                        "buyer-items-total-before-discount='150' buyer-total-before-discount='170'/>"
        )));
    }

    @RepeatedTest(10)
    public void testSerializeExternalCertificate() throws Exception {
        final Order order = new Order() {{
            setId(1234L);
            setExternalCertificateId(123L);
        }};

        final String result = serialize(serializer, order);

        assertThat(result, is(sameXmlAs(
                "<order fulfilment=\"false\" id='1234' certificateId='123' accept-method=\"0\" fake=\"false\" subsidy-total='0'/>"
        )));
    }
}
