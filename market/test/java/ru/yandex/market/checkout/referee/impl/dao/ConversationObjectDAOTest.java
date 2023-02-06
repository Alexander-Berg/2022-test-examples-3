package ru.yandex.market.checkout.referee.impl.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationObjectType;
import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.criteria.ConversationSearch;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeService;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeServiceTest;
import ru.yandex.market.checkout.referee.impl.RetriableCheckouterService;
import ru.yandex.market.checkout.referee.test.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author kukabara
 */
public class ConversationObjectDAOTest extends EmptyTest {
    @Autowired
    protected CheckoutRefereeService checkoutRefereeService;

    @Test
    public void testOrderConversation() {
        testSaveAndSearchByObject(genOrderConv());
    }

    @Test
    public void testItemConversation() {
        Conversation conv = genOrderItemConv();
        testSaveAndSearchByObject(conv);

        ConversationSearch cs = new ConversationSearch();
        cs.setUser(conv.getUid());
        cs.setOrderIds(Sets.newHashSet(conv.getObject().getOrderId()));
        cs.setFrom(1);
        cs.setTo(10);
        assertEquals(1, checkoutRefereeService.searchConversations(cs, 10, 1).getItems().size(),
                "Переписка по ORDER_ITEM должна искаться по заказу");
    }

    @Test
    public void testSkuConversation() {
        testSaveAndSearchByObject(genSkuConv());
    }

    @Test
    public void testObjects() {
        Conversation conv = addAndGetConv(genUserShopConv());
        List<ConversationObject> list = checkoutRefereeService.getObjects(Arrays.asList(conv.getId()));
        assertEquals(1, list.size());
        assertEquals(ConversationObjectType.USER_SHOP, list.get(0).getObjectType());
    }

    @Test
    public void testUserShopConversation() {
        Conversation conv = genUserShopConv();
        testSaveAndSearchByObject(conv);
    }

    private void testSaveAndSearchByObject(Conversation conv) {
        Conversation loadedConv = addAndGetConv(conv);
        assertEquals(conv.getObject(), loadedConv.getObject());
        testSearchByObject(loadedConv);
    }

    private void testSearchByObject(Conversation conv) {
        testSearchByObject(conv.getUid(), conv.getObject(), conv.getShopId());
    }

    private void testSearchByObject(long uid, ConversationObject obj, long shopId) {
        ConversationSearch cs = new ConversationSearch();
        cs.setUser(uid);
        cs.setObject(obj);
        cs.setFrom(1);
        cs.setTo(10);
        assertEquals(1, checkoutRefereeService.searchConversations(cs, 10, 1).getItems().size(),
                "Переписка по " + obj + " должна искаться для USER-a");

        cs = new ConversationSearch();
        cs.setShopId(shopId);
        cs.setObject(obj);
        cs.setFrom(1);
        cs.setTo(10);
        assertEquals(1, checkoutRefereeService.searchConversations(cs, 10, 1).getItems().size(),
                "Переписка по " + obj + " должна искаться для SHOP-a");
    }

    public static Conversation genOrderConv() {
        OrderInfo info = genRedOrderInfo();

        Conversation conv = CheckoutRefereeServiceTest.generateConv();
        conv.setObject(ConversationObject.fromOrder(info.getOrderId()));
        conv.setOrder(info);
        conv.setUid(info.getUid());
        conv.setShopId(info.getShopId());
        return conv;
    }

    private static OrderInfo genRedOrderInfo() {
        Order order = BaseTest.getOrder(getId(), getId(), getId());
        order.setRgb(Color.RED);
        return RetriableCheckouterService.convert(order, new OrderHistoryEvents(Collections.emptyList()));
    }

    private static Conversation genSkuConv() {
        Conversation conv = CheckoutRefereeServiceTest.generateConv();
        conv.setObject(BaseTest.newSku());
        return conv;
    }

    private static Conversation genUserShopConv() {
        Conversation conv = CheckoutRefereeServiceTest.generateConv();
        conv.setObject(ConversationObject.fromUserShop());
        return conv;
    }

    public static Conversation genOrderItemConv() {
        OrderInfo info = genRedOrderInfo();

        Conversation conv = CheckoutRefereeServiceTest.generateConv();
        conv.setObject(ConversationObject.fromOrderItem(info.getOrderId(), RND.nextLong()));
        conv.setOrder(info);
        conv.setUid(info.getUid());
        conv.setShopId(info.getShopId());
        return conv;
    }

    Conversation addAndGetConv(Conversation conv) {
        if (conv.getOrder() != null) {
            checkoutRefereeService.insertOrder(conv.getOrder());
        }
        Conversation savedConv = checkoutRefereeService.insertConversation(conv);
        Conversation loadedConv = checkoutRefereeService.getConversation(savedConv.getId());
        assertNotNull(loadedConv);
        assertNotNull(loadedConv.getObject());
        return loadedConv;
    }
}
