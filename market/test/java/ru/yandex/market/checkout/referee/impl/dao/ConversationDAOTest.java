package ru.yandex.market.checkout.referee.impl.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.entity.ArbitrageCheckType;
import ru.yandex.market.checkout.entity.ClosureType;
import ru.yandex.market.checkout.entity.Column;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.InquiryType;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.NoteEvent;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.ResolutionType;
import ru.yandex.market.checkout.entity.Sorting;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.Normalization;
import ru.yandex.market.checkout.referee.criteria.ConversationSearch;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeService;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeServiceTest;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
public class ConversationDAOTest extends EmptyTest {

    private static final int PAGE = 1;
    private static final int PAGE_SIZE = 10;

    @Autowired
    protected CheckoutRefereeService checkoutRefereeService;

    @Test
    public void testSaveGetConversation() {
        Conversation conv = genConvUserShop();
        Conversation saved = checkoutRefereeService.insertConversation(conv);
        assertNotNull(saved);

        Conversation loaded = checkoutRefereeService.getConversation(saved.getId());
        assertEquals(loaded, conv);
        assertEquals(conv.getCheckType(), loaded.getCheckType());

        loaded.setCheckType(ArbitrageCheckType.MANUAL);
        checkoutRefereeService.updateConversation(loaded);
        assertEquals(ArbitrageCheckType.MANUAL, checkoutRefereeService.getConversation(saved.getId()).getCheckType());
    }

    @Test
    public void testMarkAsRead() {
        Conversation conv = ConversationObjectDAOTest.genOrderItemConv();
        conv.setReadBy(RefereeRole.USER);
        conv.setUnreadBy(RefereeRole.SHOP);
        conv.setUnreadBy(RefereeRole.ARBITER);
        checkoutRefereeService.insertOrder(conv.getOrder());
        checkoutRefereeService.insertConversation(conv);

        Long orderId = conv.getObject().getOrderId();
        checkoutRefereeService.markAsRead(orderId, RefereeRole.SHOP);
        conv = checkoutRefereeService.getConversation(conv.getId());
        assertTrue(conv.isReadBy(RefereeRole.SHOP));
        assertFalse(conv.isReadBy(RefereeRole.ARBITER));
        assertTrue(conv.isReadBy(RefereeRole.USER));

        checkoutRefereeService.markAsRead(orderId, RefereeRole.ARBITER);
        conv = checkoutRefereeService.getConversation(conv.getId());
        assertTrue(conv.isReadBy(RefereeRole.SHOP));
        assertTrue(conv.isReadBy(RefereeRole.ARBITER));
        assertTrue(conv.isReadBy(RefereeRole.USER));
    }

    @Test
    public void testSearch() {
        Conversation conversation = saveConversation();

        ConversationSearch cs = createConversationSearch(conversation);
        Page<Conversation> found = checkoutRefereeService.searchConversations(cs, PAGE_SIZE, PAGE);
        assertNotNull(found);
    }

    @Test
    public void testSearchByStatus() {
        Conversation conversation = saveConversation();
        EnumSet<OrderStatus> orderStatus = EnumSet.of(conversation.getOrder().getOrderStatus());

        ConversationSearch cs = new ConversationSearch();
        cs.setOrderStatuses(orderStatus);
        setPageable(cs);

        Page<Conversation> found = checkoutRefereeService.searchConversations(cs, PAGE_SIZE, PAGE);
        assertFalse(found.getItems().isEmpty());

        cs.setOrderStatusesNotIn(orderStatus);
        setPageable(cs);

        found = checkoutRefereeService.searchConversations(cs, PAGE_SIZE, PAGE);
        assertTrue(found.getItems().isEmpty());
    }

    private Conversation saveConversation() {
        Conversation conversation = ConversationObjectDAOTest.genOrderItemConv();
        checkoutRefereeService.insertOrder(conversation.getOrder());
        checkoutRefereeService.insertConversation(conversation);
        return conversation;
    }

    @NotNull
    private static ConversationSearch createConversationSearch(Conversation conv) {
        ConversationSearch cs = new ConversationSearch();
        cs.setCheckType(ArbitrageCheckType.AUTO);
        cs.setRgbs(EnumSet.of(Color.RED));
        cs.setOrderIds(Sets.newHashSet(conv.getOrder().getOrderId()));
        cs.setStatus(conv.getLastStatus());
        cs.setArchive(false);
        cs.setUser(conv.getUid());
        cs.setShopId(774L);
        cs.setRoleBit(2);
        cs.setRead(false);
        cs.setParticipated(false);
        cs.setStatusSince(new Date());
        cs.setStatusBefore(new Date());
        cs.setIssueType(IssueType.BROKEN_PRODUCT_DELIVERED);
        cs.setResolutionType(ResolutionType.REFUND);
        cs.setInquiryTypes(EnumSet.of(InquiryType.DOCS_FROM_USER));
        cs.setInquiryDueBefore(new Date());
        cs.setClosureTypes(EnumSet.of(ClosureType.EXPIRED));
        cs.setLabelMask(0);
        cs.setNoteEvents(EnumSet.of(NoteEvent.USER_DOCS_INQUIRY));
        cs.setNoteEventsNotSend(EnumSet.of(NoteEvent.USER_DOCS_INQUIRY));
        cs.setObject(ConversationObject.fromOrderItem(1L, 1L));
        cs.setShopOrderId("asdf");
        cs.setOrderSince(new Date());
        cs.setOrderBefore(new Date());
        cs.setOrderStatuses(EnumSet.of(OrderStatus.CANCELLED));
        cs.setOrderSubstatuses(EnumSet.of(OrderSubstatus.BROKEN_ITEM));
        cs.setFake(false);
        cs.setCpa20(false);
        cs.setBooked(false);
        cs.setContexts(Collections.singletonList(Context.MARKET));
        cs.setGroupByOrder(false);

        cs.setSorting(
                Arrays.stream(Column.values())
                        .map(column -> new Sorting(RND.nextBoolean(), column))
                        .collect(toList())
        );

        setPageable(cs);
        return cs;
    }

    private static void setPageable(ConversationSearch cs) {
        int from = Normalization.from(PAGE, PAGE_SIZE);
        int to = from + PAGE_SIZE;
        cs.setFrom(from);
        cs.setTo(to);
    }


    private static Conversation genConvUserShop() {
        Conversation conv = CheckoutRefereeServiceTest.generateConv();
        conv.setObject(ConversationObject.fromUserShop());
        return conv;
    }
}
