package config.classmapping;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.delivery.Delivery.SELF_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;

public class OrderClassMappingsTest extends BaseClassMappingsTest {
    private static final String SERIALIZED_DELIVERY = "<delivery delivery-service-id=\"99\" region-id=\"2\" " +
            "delivery-partner-type=\"SHOP\"/>";
    private static final String SERIALIZED_ITEM_1 = "<item feed-id=\"1\" offer-id=\"item1\" count=\"1\" subsidy='0'/>";
    private static final String SERIALIZED_ITEM_2 = "<item feed-id=\"1\" offer-id=\"item2\" count=\"1\" subsidy='0'/>";

    private Delivery delivery = new Delivery(2L);

    {
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setDeliveryServiceId(SELF_DELIVERY_SERVICE_ID);
    }

    private OrderItem item1 = new OrderItem(new FeedOfferId("item1", 1l), null, 1);
    private OrderItem item2 = new OrderItem(new FeedOfferId("item2", 1l), null, 1);

    @Test
    public void testParsePrepaid() throws Exception {
        final Order actual = deserialize(Order.class,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='PREPAID'" +
                        "           fake='true' context='SANDBOX'>" +
                        "        <items>" +
                        "            " + SERIALIZED_ITEM_1 +
                        "            " + SERIALIZED_ITEM_2 +
                        "        </items>" +
                        "        " + SERIALIZED_DELIVERY +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertEquals(Currency.RUR, actual.getCurrency());
        assertEquals(delivery, actual.getDelivery());
        assertEquals(1234, actual.getId().longValue());
        compareItems(actual.getItems());
        assertEquals(PaymentType.PREPAID, actual.getPaymentType());
        assertNull(actual.getPaymentMethod());
        assertEquals("notes-notes-notes", actual.getNotes());
        assertEquals(true, actual.isFake());
        assertEquals(Context.SANDBOX, actual.getContext());
    }

    @Test
    public void testParsePostpaid() throws Exception {
        final Order actual = deserialize(Order.class,
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           payment-type='POSTPAID'" +
                        "           payment-method='CARD_ON_DELIVERY' subsidy-total='0'>" +
                        "        <items>" +
                        "            " + SERIALIZED_ITEM_1 +
                        "            " + SERIALIZED_ITEM_2 +
                        "        </items>" +
                        "        " + SERIALIZED_DELIVERY +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        );

        assertEquals(Currency.RUR, actual.getCurrency());
        assertEquals(delivery, actual.getDelivery());
        assertEquals(1234, actual.getId().longValue());
        compareItems(actual.getItems());
        assertEquals(PaymentType.POSTPAID, actual.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actual.getPaymentMethod());
        assertEquals("notes-notes-notes", actual.getNotes());
        assertNull(actual.getContext());
    }

    @Test
    public void testParseEmpty() throws Exception {
        final Order actual = deserialize(Order.class,
                "<order />"
        );

        assertNotNull(actual);
    }

    @Test
    public void testFullParse() throws Exception {
        final Order order = deserialize(Order.class,
                "<order id='1234'" +
                        "   status='CANCELLED'" +
                        "   substatus='USER_CHANGED_MIND'" +
                        "   creation-date='06-07-2013 15:30:40'" +
                        "   currency='RUR'" +
                        "   items-total='10.75'" +
                        "   total='11.43'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='true' context='SANDBOX' subsidy-total='0'>" +
                        "       <items>" +
                        "           " + SERIALIZED_ITEM_1 +
                        "           " + SERIALIZED_ITEM_2 +
                        "       </items>" +
                        "       " + SERIALIZED_DELIVERY +
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
        compareItems(order.getItems());
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

    private void compareItems(Collection<OrderItem> items) {
        assertEquals(item1.getFeedOfferId(), Iterables.get(items, 0).getFeedOfferId());
        assertEquals(item1.getCount(), Iterables.get(items, 0).getCount());
        assertEquals(item2.getFeedOfferId(), Iterables.get(items, 1).getFeedOfferId());
        assertEquals(item2.getCount(), Iterables.get(items, 1).getCount());
    }

    @Test
    public void testParse() throws Exception {
        final Order actual = deserialize(Order.class,
                "<order id='1234' status='CANCELLED' substatus='USER_CHANGED_MIND'/>"
        );

        assertEquals(1234, actual.getId().longValue());
        assertEquals(OrderStatus.CANCELLED, actual.getStatus());
        assertEquals(OrderSubstatus.USER_CHANGED_MIND, actual.getSubstatus());
    }

    @Test
    public void testSerializePrepaid() throws Exception {
        final Order order = new Order() {{
            setCurrency(Currency.RUR);
            setDelivery(delivery);
            setId(1234l);
            setItems(Arrays.asList(item1, item2));
            setPaymentType(PaymentType.PREPAID);
            setNotes("notes-notes-notes");
            setFake(false);
            setContext(Context.MARKET);
        }};

        final String result = serialize(order);

        assertThat(result, is(sameXmlAs(
                "    <order id='1234'" +
                        "           currency='RUR'" +
                        "           fulfilment=\"false\"" +
                        "           payment-type='PREPAID'" +
                        "           fake='false' accept-method=\"0\" context='MARKET' subsidy-total='0'>" +
                        "        <items>" +
                        "            " + SERIALIZED_ITEM_1 +
                        "            " + SERIALIZED_ITEM_2 +
                        "        </items>" +
                        "        " + SERIALIZED_DELIVERY +
                        "        <notes>notes-notes-notes</notes>" +
                        "    </order>"
        )));
    }

    @Test
    public void serializesPaymentMethod() throws Exception {
        final Order order = new Order() {{
            setPaymentType(PaymentType.POSTPAID);
            setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        }};

        final String result = serialize(order);

        assertThat(result, is(sameXmlAs("<order fulfilment=\"false\" payment-type='POSTPAID' payment-method='CASH_ON_DELIVERY' accept-method=\"0\" fake=\"false\" subsidy-total='0'/>")));
    }

    @Test
    public void serializesEmptyOrder() throws Exception {
        final Order order = new Order();

        final String result = serialize(order);

        assertThat(result, is(sameXmlAs("<order accept-method=\"0\" fulfilment=\"false\" fake=\"false\" subsidy-total='0'/>")));
    }

    @Test
    public void testSerializeCancelled() throws Exception {
        final Order order = new Order() {{
            setId(1234L);
            setStatus(OrderStatus.CANCELLED);
            setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
            setCreationDate(createLongDate("2013-07-06 15:30:40"));
            setCurrency(Currency.RUR);
            setItemsTotal(new BigDecimal("10.75"));
            setTotal(new BigDecimal("11.43"));
            setPaymentType(PaymentType.PREPAID);
            setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            setFake(true);
            setContext(Context.SANDBOX);
            setItems(Arrays.asList(item1, item2));
            setDelivery(delivery);
            setBuyer(new Buyer() {{
                setId("blah");
                setUid(2345L);
                setLastName("Иванов");
                setFirstName("Иван");
                setMiddleName("Иваныч");
                setPhone("123456789");
                setEmail("vanya@localhost");
            }});
        }};

        final String result = serialize(order);

        assertThat(result, is(sameXmlAs(
                "<order id='1234'" +
                        "   status='CANCELLED'" +
                        "   substatus='USER_CHANGED_MIND'" +
                        "   creation-date='06-07-2013 15:30:40'" +
                        "   currency='RUR'" +
                        "   fulfilment=\"false\" " +
                        "   items-total='10.75'" +
                        "   total='11.43'" +
                        "   payment-type='PREPAID'" +
                        "   payment-method='CARD_ON_DELIVERY'" +
                        "   fake='true'" +
                        "   context='SANDBOX'" +
                        "   accept-method=\"0\"" +
                        "   subsidy-total='0'>" +
                        "       <items>" +
                        "           " + SERIALIZED_ITEM_1 +
                        "           " + SERIALIZED_ITEM_2 +
                        "       </items>" +
                        "       " + SERIALIZED_DELIVERY +
                        "       <buyer id='blah' uid='2345' last-name='Иванов' first-name='Иван' middle-name='Иваныч' phone='123456789' email='vanya@localhost'/>" +
                        "</order>"
        )));
    }

    @Test
    public void testSerializeNotCancelled() throws Exception {
        final Order order = new Order() {{
            setId(1234L);
            setStatus(OrderStatus.DELIVERED);
        }};

        final String result = serialize(order);

        assertThat(result, is(sameXmlAs(
                "<order id='1234' status='DELIVERED' fulfilment=\"false\"  accept-method=\"0\" fake=\"false\" subsidy-total='0'/>"
        )));
    }
}
