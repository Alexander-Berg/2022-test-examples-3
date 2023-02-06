package ru.yandex.market.checkout.referee.test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author localstorm
 * Date: 04.06.13
 */
public final class BaseTest {
    private static final Random RNG = new Random(System.currentTimeMillis());
    private static final String AB = "0123456789ABCD";
    private static final long FEED_ID = 1234L;
    private static final String OFFER_ID = "OFFER_ID";
    private static final String OFFER_ID2 = "OFFER_ID2";
    private static final String EMAIL = "kukabara@yandex-team.ru";
    private static final String NAME = "Kate";
    public static final long ORDER_ID_NOT_FOUND = 1L;

    public static long newUID() {
        return (Math.abs(RNG.nextLong()) + 1);
    }

    public static long newOrderId() {
        return getOrderId(Color.BLUE, true);
    }

    public static long newRedOrderId() {
        return getOrderId(Color.RED, true);
    }

    public static long newRedOrderIdWithoutTrack() {
        return getOrderId(Color.RED, false);
    }

    private static int getRandomNumberInRange(int min, int max) {
        return RNG.ints(min, (max + 1)).findFirst().getAsInt();
    }

    public static ConversationObject newSku() {
        return ConversationObject.fromSku(UUID.randomUUID().toString(),
                Arrays.asList(
                        new FeedOfferId("offerId1", 1L),
                        new FeedOfferId("offerId2", 2L))
        );
    }

    private static int getOrderId(Color rgb, boolean withTrack) {
        switch (rgb) {
            case RED:
                if (withTrack) {
                    return getRandomNumberInRange(10001, 25000);
                } else {
                    return getRandomNumberInRange(25001, 50000);
                }
            case BLUE:
                return getRandomNumberInRange(50001, 100000);
        }
        throw new IllegalArgumentException("Unknown color " + rgb);
    }

    private static void setByOrderId(Order order, Long orderId) {
        order.setCreationDate(new Date());

        Color rgb;
        if (orderId > 50000) {
            rgb = Color.BLUE;
        } else {
            rgb = Color.WHITE;
            order.setCreationDate(DateUtil.addDay(new Date(), -76));
        }
        order.setRgb(rgb);
        order.setGlobal(rgb == Color.RED);

        if (orderId <= 25000) {
            order.getDelivery().setParcels(Collections.singletonList(new Parcel()));
        } else {
            order.getDelivery().setParcels(null);
        }
    }

    public static long newRealUID() {
        return (Math.abs(RNG.nextLong()) + 1);
    }

    public static Order getOrder(Long orderId, Long uid, Long shopId) {
        Order o = new Order();
        o.setId(orderId);
        Buyer buyer = new Buyer();
        buyer.setEmail(EMAIL);
        buyer.setFirstName(NAME);
        buyer.setUid(uid);
        o.setBuyer(buyer);
        o.setShopId(shopId);
        o.setUid(uid);
        o.setContext(Context.MARKET);
        o.setShopOrderId(String.valueOf(o.getId()));
        o.setStatus(OrderStatus.DELIVERY); // только в DELIVERY можно создать претензию
        o.setPaymentMethod(PaymentMethod.YANDEX);
        o.setPaymentType(PaymentType.PREPAID); // только для предоплаты можно создать арбитраж
        Delivery delivery = new Delivery(213L);
        delivery.setDeliveryDates(new DeliveryDates(
                DateUtil.addDay(new Date(), 1),
                DateUtil.addDay(new Date(), 2)
        ));

        o.setDelivery(delivery);
        o.setFake(false);
        o.setContext(Context.MARKET);
        o.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);

        OrderItem item1 = new OrderItem();
        item1.setFeedId(FEED_ID);
        item1.setOfferId(OFFER_ID);
        item1.setCount(3);
        item1.setBuyerPrice(BigDecimal.valueOf(100.5));

        OrderItem item2 = new OrderItem();
        item2.setFeedId(FEED_ID);
        item2.setOfferId(OFFER_ID2);
        item2.setCount(2);
        item2.setBuyerPrice(BigDecimal.valueOf(200));
        o.setItems(Arrays.asList(item1, item2));

        setByOrderId(o, orderId);
        return o;
    }

    // User messages
    public static String getConvTitle() {
        return "A new conversation " + System.currentTimeMillis();
    }

    public static String getText() {
        return "New mega text " + System.currentTimeMillis();
    }

    public static String getClosureMessage() {
        return "I am agree and close this conversation";
    }

    public static String getIssueMessage() {
        return "I have a problem.";
    }

    public static String getEscalateMessage() {
        return "I will complain. Please consider our situation.";
    }

    static String getReopenMessage() {
        return "I don't agree. Please see again.";
    }

    // Shop message
    public static String getShopMessage() {
        return "My shop is good. You are not right.";
    }

    public static String getArbiterMessageForShop() {
        return someString(1000);
    }

    static String getArbiterMessageForUser() {
        return someString(1000);
    }

    public static void assertNote(Note n) {
        assertNotNull(n.getId());
        assertNotNull(n.getType());
    }

    public static void assertConv(Conversation conv) {
        assertNotNull(conv);
        assertNotNull(conv.getObject());
        assertNotNull(conv.getObject().getObjectType());
        switch (conv.getObjectType()) {
            case ORDER:
                assertNotNull(conv.getObject().getOrderId());
                assertNotNull(conv.getOrder());
                break;
            case ORDER_ITEM:
                assertNotNull(conv.getObject().getOrderId());
                assertNotNull(conv.getObject().getItemId());
                assertNotNull(conv.getOrder());
                break;
            case SKU:
                assertNotNull(conv.getObject().getFeedGroupIdHash());
                assertNotNull(conv.getObject().getFeedOfferIds());
                assertFalse(conv.getObject().getFeedOfferIds().isEmpty());
                assertNull(conv.getObject().getOrderId());
                assertNull(conv.getObject().getItemId());
                assertNull(conv.getOrder());
                break;
            default:
                throw new UnsupportedOperationException();
        }
        assertNotEquals(0, conv.getId());
        OrderInfo order = conv.getOrder();
        if (order != null) {
            assertTrue("noname".equals(order.getName()) || NAME.equals(order.getName()), "Wrong name " + order.getName());
            assertTrue("nomail@nomail.nomail".equals(order.getEmail()) || EMAIL.equals(order.getEmail()));
            assertNotNull(order.getShopOrderId());
            // TODO
//        assertTrue(conv.getOrder().getShopOrderId().matches("[a-zA-Z]+-[0-9]+"));
        }
        assertNotNull(conv.getTitle());
        assertNotNull(conv.getCreatedTs());
        assertNotEquals(0, (long) conv.getShopId());
    }

    public static void assertMessage(Message msg) {
        assertNotNull(msg);
        assertNotEquals(0, msg.getId());
        assertNotNull(msg.getText());
        assertNotNull(msg.getMessageTs());
        assertNotEquals(0, msg.getAuthorUid());
    }

    public static void assertAttachmentGroup(AttachmentGroup group) {
        assertNotNull(group);
        assertTrue(group.getId() != 0);
        assertNotNull(group.getAttachments());
        assertNotSame(0L, group.getAuthorUid());
    }

    public static void assertAttachment(Attachment att) {
        assertNotNull(att);
        assertNull(att.getContentId());
        assertNotNull(att.getUploadTs());
        assertNotNull(att.getContentType());
        assertNotNull(att.getFileName());
        assertNotNull(att.getLink());
        assertNotEquals(0, att.getFileSize());
        assertNotEquals(0, att.getId());
    }

    public static String someString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(AB.charAt(RNG.nextInt(AB.length())));
        }
        return sb.toString();
    }
}
