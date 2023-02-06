package ru.yandex.market.checkout.referee.impl;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.entity.ArbitrageCheckType;
import ru.yandex.market.checkout.entity.ClaimType;
import ru.yandex.market.checkout.entity.ClosureType;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.InquiryType;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.Label;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.Normalization;
import ru.yandex.market.checkout.referee.criteria.ConversationSearch;
import ru.yandex.market.checkout.referee.criteria.ConversationUpdateSearch;
import ru.yandex.market.checkout.referee.test.BaseTest;
import ru.yandex.market.common.ping.CheckResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
public class CheckoutRefereeServiceTest extends EmptyTest {
    private static final int PAGE_SIZE = 10;

    @Autowired
    private CheckoutRefereeService checkoutRefereeService;
    @Autowired
    private RefereeManager refereeManager;

    @Test
    public void testStartAndSearchConv() {
        OrderInfo info = refereeManager.getOrder(BaseTest.newRedOrderId(), getId(), RefereeRole.USER, getId());

        startConversation(1L, RefereeRole.SHOP, info, info.getShopId(), "title", null, "text");

        ConversationSearch cs = new ConversationSearch();
        cs.setOrderIds(Sets.newHashSet(info.getOrderId()));
        cs.setRgbs(Sets.newHashSet(Color.BLUE));
        int page = 1;
        int pageSize = 10;
        int from = Normalization.from(page, pageSize);
        int to = from + pageSize;
        cs.setFrom(from);
        cs.setTo(to);
        Page<Conversation> found = checkoutRefereeService.searchConversations(cs, pageSize, page);
        assertTrue(found.getItems().isEmpty());

        cs.setRgbs(Sets.newHashSet(info.getRgb()));
        found = checkoutRefereeService.searchConversations(cs, pageSize, page);
        assertEquals(1, found.getItems().size());
    }

    @Nonnull
    public static Conversation generateConv() {
        Conversation conv = new Conversation();
        conv.setTitle("Title");
        Date now = new Date();
        RefereeRole role = RefereeRole.USER;
        conv.setCreatedTs(now);


        conv.setLastMessageTs(now);
        conv.setLastStatusTs(now);
        conv.setLastStatus(ConversationStatus.CLOSED);
        conv.setLastAuthorRole(role);
        conv.setIssueTypes(EnumSet.of(IssueType.DELIVERY_DELAY, IssueType.BROKEN_PRODUCT_DELIVERED));
        conv.setClaimType(ClaimType.REFUND);
        conv.setClosureType(ClosureType.EXPIRED);
        conv.setParticipatedBy(role);
        conv.setReadStatusMask(0);
        conv.setReadBy(role);
        conv.incrementUnreadExcept(role);
        conv.resetUnreadCount(role);
        conv.setArchive(false);

        conv.setCheckType(ArbitrageCheckType.AUTO);
        conv.setLastLabel(Label.RED_BAD_PRODUCT);
        conv.setLabel(Label.RED_BAD_PRODUCT);
        conv.setLabel(Label.PROBLEM);
        conv.setInquiryType(InquiryType.DOCS_FROM_SHOP);
        conv.setInquiryDueTs(new Date());
        conv.setInquiryFromTs(new Date());
        conv.setResolutionCount(1);
        conv.setUid(RND.nextLong());
        conv.setShopId(RND.nextLong());
        return conv;
    }

    public Conversation startConversation(Long uid, RefereeRole role, @Nullable OrderInfo orderInfo, Long shopId,
                                          String title, Label label, String text) {
        Conversation conv = RefereeManager.initConversation(role, uid, orderInfo, shopId, title, label);
        conv.setLastStatus(ConversationStatus.OPEN);

        Message message = new Message.Builder(conv.getId(), uid, role)
                .withText(text)
                .withMessageTs(conv.getCreatedTs())
                .withConversation(conv)
                .withConvStatusBefore(null)
                .build();


        return refereeManager.start(conv, message, null);
    }

    @Test
    public void testGetUpdatesStartByShop() {
        startConversation(1L, RefereeRole.SHOP, null, 1L, "title", null, "text");

        Collection<Conversation> found = getUpdates();
        assertFalse(found.isEmpty(), "No conv in updates");

        checkoutRefereeService.markAsRead(found.stream().findFirst().orElse(null), RefereeRole.SYSTEM, true);
        assertTrue(getUpdates().isEmpty(), "Conv not processed");
    }

    private Collection<Conversation> getUpdates() {
        ConversationUpdateSearch cs = new ConversationUpdateSearch(RefereeRole.USER);
        cs.setSince(DateUtil.addDay(new Date(), -7));
        cs.setBefore(DateUtil.addDay(new Date(), 1));
        cs.setFrom(0);
        cs.setTo(PAGE_SIZE);
        cs.setRead(false);
        cs.setProcessed(false);

        return checkoutRefereeService.getUpdates(cs, PAGE_SIZE, 1).getItems();
    }

    @Test
    public void testGetUpdatesStartByUser() {
        long uid = 1L;
        long shopId = 1L;
        long orderId = Math.abs(RND.nextInt());

        OrderInfo orderInfo = refereeManager.getOrder(orderId, uid, RefereeRole.USER, shopId);
        Conversation conv = startConversation(uid, RefereeRole.USER, orderInfo, shopId, "title", null, "text");

        Collection<Conversation> found = getUpdates();
        assertTrue(found.isEmpty());

        // если написал магазин, то надо уведомить
        Message message = new Message.Builder(conv.getId(), 1L, RefereeRole.SHOP)
                .withText("text")
                .withMessageTs(new Date())
                .withConversation(conv)
                .build();
        refereeManager.send(conv, message, null, RefereeManager.UpdateConvFunction.EMPTY);
        assertFalse(conv.isReadBy(RefereeRole.USER));
        assertFalse(conv.isReadBy(RefereeRole.SYSTEM));
        assertFalse(getUpdates().isEmpty());

        // если прочитано пользователем, то не уведомляем
        checkoutRefereeService.markAsRead(conv, RefereeRole.USER, true);
        assertTrue(getUpdates().isEmpty(), "No update cause read by user");

        // если пользователь уже ответил, то не уведомляем
        Message shopMessage = new Message.Builder(conv.getId(), 1L, RefereeRole.SHOP)
                .withText("text")
                .withMessageTs(new Date())
                .withConversation(conv)
                .build();
        refereeManager.send(conv, shopMessage, null, RefereeManager.UpdateConvFunction.EMPTY);

        Message userMessage = new Message.Builder(conv.getId(), uid, RefereeRole.USER)
                .withText("text")
                .withMessageTs(new Date())
                .withConversation(conv)
                .build();
        refereeManager.send(conv, userMessage, null, RefereeManager.UpdateConvFunction.EMPTY);
        assertTrue(conv.isReadBy(RefereeRole.USER));
        assertFalse(conv.isReadBy(RefereeRole.SYSTEM));
        assertTrue(getUpdates().isEmpty());
    }

    @Test
    public void testPing() {
        assertSame(refereeManager.pingCheckouter().getLevel(), CheckResult.Level.OK);
        assertTrue(checkoutRefereeService.testConnection());
    }

    @Test
    public void export() {
        long userId = 1L;
        startConversation(userId, RefereeRole.USER, null, 1L, "title", null, "text");

        assertFalse(checkoutRefereeService.exportMessages(userId, new Date()).isEmpty());
    }
}
