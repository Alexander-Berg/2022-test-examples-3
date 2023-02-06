package ru.yandex.market.abo.core.arbitrage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.arbitrage.model.AboRefund;
import ru.yandex.market.abo.core.arbitrage.model.AboRefundItem;
import ru.yandex.market.abo.core.arbitrage.model.AboRefundStatus;
import ru.yandex.market.abo.core.arbitrage.model.ArbitrageComment;
import ru.yandex.market.abo.core.arbitrage.model.ArbitrageInfo;
import ru.yandex.market.abo.core.arbitrage.model.ResolutionRequest;
import ru.yandex.market.checkout.entity.ArbitrageCheckType;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.OrderInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.core.arbitrage.ArbitrageService.AUTO_ARBITRAGE_DAYS;

/**
 * @author antipov93@yndx-team.ru, kukabara@yandex-team.ru
 */
public class ArbitrageServiceTest extends EmptyTest {

    @Autowired
    private ArbitrageService arbitrageService;
    @Autowired
    private ArbitrageService.ArbitrageInfoRepo arbitrageInfoRepo;

    @Test
    public void testLoadAutoArbitrageToResolve() {
        ArbitrageInfo info = new ArbitrageInfo();
        info.setCheckType(ArbitrageCheckType.AUTO);
        info.setCreationTime(DateUtil.addDay(new Date(), -(AUTO_ARBITRAGE_DAYS + 1)));
        info.setConversationId(1L);
        info.setStatus(ConversationStatus.ARBITRAGE);
        arbitrageInfoRepo.save(info);

        List<ArbitrageInfo> infos = arbitrageService.loadAutoArbitrageToResolve();
        assertEquals(1, infos.size());
    }

    @Test
    public void testSaveLoadConversation() throws Exception {
        // save
        Conversation a = generateConversation(1, ConversationStatus.ARBITRAGE, true, false);
        Conversation b = generateConversation(2, ConversationStatus.ESCALATED, false, true);
        arbitrageService.updateConversations(Arrays.asList(a, b));
        ArbitrageInfo aLoaded = arbitrageService.getArbitrageByConversationId(1);
        ArbitrageInfo bLoaded = arbitrageService.getArbitrageByConversationId(2);
        assertNotNull(aLoaded);
        assertNotNull(bLoaded);
        assertEquals(aLoaded.getStatus(), ConversationStatus.ARBITRAGE);
        assertEquals(bLoaded.getStatus(), ConversationStatus.ESCALATED);

        assertTrue(loadUnread(1));
        assertFalse(loadOverdue(1));
        assertFalse(loadUnread(2));
        assertTrue(loadOverdue(2));

        // update
        a.setReadStatusMask(4);
        b.setLastStatus(ConversationStatus.ARBITRAGE);
        arbitrageService.updateConversations(Arrays.asList(a, b));

        assertFalse(loadUnread(1));
        assertEquals(ConversationStatus.ARBITRAGE, arbitrageService.getArbitrageByConversationId(2).getStatus());

        // update status
        Long arbiterUid = (long) RND.nextInt(10000);
        arbitrageService.saveTicketStatus(arbiterUid, 2, ConversationStatus.CLOSED);
        ArbitrageInfo arbitrageInfo = arbitrageService.getArbitrageByConversationId(2);
        assertEquals(ConversationStatus.CLOSED, arbitrageInfo.getStatus());
        assertEquals(arbiterUid, arbitrageInfo.getArbiterUid());
    }

    @Test
    public void testComment() {
        Conversation conv = generateConversation(1, ConversationStatus.ARBITRAGE, true, false);
        arbitrageService.updateConversations(Collections.singletonList(conv));

        long userId = 123L;
        long shopId = 774L;
        arbitrageService.addComment(1L, shopId, ArbitrageComment.CommentType.SHOP, "плохой магазин");
        arbitrageService.addComment(1L, shopId, ArbitrageComment.CommentType.SHOP, "да вроде норм");
        arbitrageService.addComment(1L, userId, ArbitrageComment.CommentType.USER, "мошенник");

        Collection<ArbitrageComment> comments = arbitrageService.getComments(userId, shopId);
        assertEquals(2, comments.size());
    }

    @Test
    public void testRefund() throws Exception {
        // создаём возврат
        long orderId = RND.nextLong();
        AboRefund refund = new AboRefund(orderId, BigDecimal.ONE, "test", 1L);
        AboRefundItem item1 = new AboRefundItem(1L, "OFFER_ID", 2);
        AboRefundItem item2 = new AboRefundItem(1L, "OFFER_ID2", 3);
        AboRefundItem delivery = new AboRefundItem();
        delivery.setDelivery(true);
        refund.setAboRefundItems(new ArrayList<>(Arrays.asList(item1, item2, delivery)));
        refund.setStatus(AboRefundStatus.CREATED);
        long conversationId = RND.nextLong();
        refund.setConversationId(conversationId);
        AboRefund saved = arbitrageService.addRefund(refund);
        assertNotNull(saved.getId());

        // получаем
        AboRefund loaded = arbitrageService.getRefundById(saved.getId());
        assertNotNull(loaded.getId());
        assertEquals(loaded.getConversationId(), saved.getConversationId());

        List<AboRefund> refundsByOrderId = arbitrageService.getRefundsByOrderId(orderId);
        assertFalse(refundsByOrderId.isEmpty());

        // обновляем
        Long superArbiterUid = RND.nextLong();
        loaded.setStatus(AboRefundStatus.APPROVED);
        loaded.setSuperArbiterUid(superArbiterUid);
        arbitrageService.updateRefund(loaded);

        loaded = arbitrageService.getRefundById(saved.getId());
        assertEquals(AboRefundStatus.APPROVED, loaded.getStatus());
        assertEquals(superArbiterUid, loaded.getSuperArbiterUid());

        // ResolutionRequest extends AboRefund
        ResolutionRequest request = ResolutionRequest.fromRefund(refund);
        saved = arbitrageService.addRefund(request.toRefund());
        assertNotNull(saved.getId());
    }

    private Boolean loadUnread(int conversationId) {
        return Optional.ofNullable(arbitrageService.getArbitrageByConversationId(conversationId))
                .map(ArbitrageInfo::isUnread)
                .orElse(null);
    }

    private Boolean loadOverdue(int conversationId) {
        return Optional.ofNullable(arbitrageService.getArbitrageByConversationId(conversationId))
                .map(ArbitrageInfo::isOverdue)
                .orElse(null);
    }


    public static Conversation generateConversation(int id, ConversationStatus status, boolean unread, boolean overdue) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setShopId(2L);
        orderInfo.setOrderId(3L);
        orderInfo.setUid(0L);
        conversation.setOrder(orderInfo);
        conversation.setLastStatus(status);
        conversation.setReadStatusMask(unread ? 0 : 4);
        Date inquiryDue = new Date();
        inquiryDue = DateUtil.addDay(inquiryDue, overdue ? -1 : 1);
        conversation.setInquiryDueTs(inquiryDue);
        return conversation;
    }
}
