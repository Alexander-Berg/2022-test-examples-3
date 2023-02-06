package ru.yandex.market.checkout.referee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.entity.ArbitrageCheckType;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.ClosureType;
import ru.yandex.market.checkout.entity.Column;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.InquiryType;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.Label;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.MessageRequest;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.NoteEvent;
import ru.yandex.market.checkout.entity.NoteType;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeErrorCode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.ResolutionSubtype;
import ru.yandex.market.checkout.entity.ResolutionType;
import ru.yandex.market.checkout.entity.Sorting;
import ru.yandex.market.checkout.entity.structures.NotificationChunk;
import ru.yandex.market.checkout.referee.controller.DiscussionController;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;
import ru.yandex.market.checkout.referee.test.BaseTest;
import ru.yandex.market.checkout.referee.test.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertConv;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertNote;
import static ru.yandex.market.checkout.referee.test.BaseTest.getArbiterMessageForShop;
import static ru.yandex.market.checkout.referee.test.BaseTest.getClosureMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.getEscalateMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getIssueMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getShopMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getText;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newRealUID;
import static ru.yandex.market.checkout.referee.test.BaseTest.newSku;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;
import static ru.yandex.market.checkout.referee.test.BaseTest.someString;

/**
 * @author kukabara
 */
public abstract class ConversationTest extends BaseConversationTest {
    private static final Logger log = LoggerFactory.getLogger(ConversationTest.class);

    @Test
    @Disabled // TODO brokenTest
    public void testRussian() {
        String title = "English Новый диалог 1484050162016";

        // MockHttpServletRequestBuilder
        String encoded = UriUtils.encode(title, "UTF-8");
        System.out.println("encoded = " + encoded);
        String decoded = UriUtils.decode(encoded, "UTF-8");
        System.out.println("decoded " + decoded);

        String text = "Text\nMultiline with spaces!";

        Conversation conv1 = client.startConversation(
                new ConversationRequest.Builder(1L, RefereeRole.USER, ConversationObject.fromOrder(newOrderId()), text)
                        .withTitle(title).build());
        assertConv(conv1);

        assertEquals(title, conv1.getTitle());
    }

    @Test
    public void testStartAndGetConversation() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertConv(conv2);
        assertEquals(conv1, conv2);
        assertEquals(title, conv2.getTitle());

        testCanNotStart(conv2);
    }

    @Test
    public void testStartBySku() {
        long user = newUID();
        ConversationObject object = newSku();
        Conversation conv1 = start(user, getConvTitle(), object, "Long text");

        // add message by shop
        client.sendMessage(new MessageRequest.Builder(conv1.getId(), 1L, RefereeRole.SHOP)
                .withShopId(conv1.getShopId())
                .withAuthorName("authorName")
                .withText("Text").build());
        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertEquals(ConversationStatus.OPEN, conv2.getLastStatus());
        assertFalse(conv2.isCanRaiseIssue());

        testCanNotRaiseIssue(conv1);

        // search by SKU
        assertEquals(1, client.searchConversations(
                SearchTerms.SearchTermsBuilder.byUid(user).withObject(object).build()
        ).getItems().size());
    }

    @Test
    public void testStatusUpdatesNullToOpen() {
        Page<Conversation> wait;
        Date now;
        do {
            now = new Date();
            wait =
                    client.getStatusUpdates(newUID(), RefereeRole.SYSTEM, null, null, ConversationStatus.OPEN, now,
                            null, null);
            System.out.println("Still getting updates: " + wait.getPager().getTotal());
        } while (wait.getPager().getTotal() > 0);

        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        Page<Conversation> updates =
                client.getStatusUpdates(newUID(), RefereeRole.SYSTEM, null, null, ConversationStatus.OPEN, now, null,
                        null);
        assertNotNull(updates.getPager());
        assertNotNull(updates.getItems());

        List<Conversation> items = new ArrayList<>(updates.getItems());
        assertEquals(1, items.size());

        Conversation conv = items.get(0);
        assertEquals(conv1, conv);
    }

    @Test
    public void testConversationUpdates() {
        Page<Conversation> wait;
        Date now;
        do {
            now = new Date();
            wait = client.getUpdates(newUID(), RefereeRole.SHOP, -1L, ConversationStatus.OPEN, now, null, null);
        } while (wait.getPager().getTotal() > 0);

        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        Page<Conversation> updates =
                client.getUpdates(newUID(), RefereeRole.SHOP, conv1.getShopId(), ConversationStatus.OPEN, now, null, null);
        assertNotNull(updates.getPager());
        assertNotNull(updates.getItems());

        List<Conversation> items = new ArrayList<>(updates.getItems());
        assertEquals(1, items.size());

        Conversation conv = items.get(0);
        assertEquals(conv1, conv);

        updates = client.getUpdates(newUID(), RefereeRole.SHOP, newUID(), ConversationStatus.OPEN, now, null, null);
        assertNotNull(updates.getPager());
        assertNotNull(updates.getItems());

        items = new ArrayList<>(updates.getItems());
        assertEquals(0, items.size());
    }

    @Test
    public void testStatusUpdatesOpenToClosed() {
        Page<Conversation> wait;
        Date now;
        do {
            now = new Date();
            wait = client.getUpdates(newUID(), RefereeRole.SHOP, 666L, ConversationStatus.OPEN, now, null, null);
        } while (wait.getPager().getTotal() > 0);

        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);
        conv1 = close(conv1);

        Page<Conversation> updates =
                client.getStatusUpdates(newUID(), RefereeRole.SYSTEM, null, ConversationStatus.OPEN,
                        ConversationStatus.CLOSED, now, null, null);
        assertNotNull(updates.getPager());
        assertNotNull(updates.getItems());

        List<Conversation> items = new ArrayList<>(updates.getItems());
        assertEquals(1, items.size());

        Conversation conv = items.get(0);
        assertEquals(ConversationStatus.CLOSED, conv1.getLastStatus());
        assertEquals(conv1, conv);
    }

    @Test
    public void testStartAndCloseConversation() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        final long shopId = conv1.getShopId();
        SearchTerms searchReplaced = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withShopId(shopId)
                .withClosureTypes(EnumSet.of(ClosureType.REPLACED))
                .build();

        Page<Conversation> replacedConv = client.searchConversations(searchReplaced);
        assertNotNull(replacedConv);
        assertEquals(0, replacedConv.getItems().size());

        conv1 = close(conv1);
        assertFalse(conv1.isParticipatedBy(RefereeRole.ARBITER));
        assertFalse(conv1.isParticipatedBy(RefereeRole.SHOP));

        replacedConv = client.searchConversations(searchReplaced);
        assertNotNull(replacedConv);
        assertEquals(1, replacedConv.getItems().size());
        assertEquals(conv1.getId(), replacedConv.getItems().iterator().next().getId());

        testCanNotClose(conv1);

        Page<Message> messages = testMessagesCount(conv1.getId(), user, RefereeRole.USER,
                2,
                /*total*/2, /*pagesCount*/1, /*currentPage*/1);
        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        Message first = items.get(1);
        Message second = items.get(0);

        assertEquals(opnText, first.getText());
        assertEquals(getClosureMessage(), second.getText());
    }

    @Test
    public void testStartAndCloseConversationWithEmptyMessage() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        String closureMessage = null;
        conv1 = close(conv1, closureMessage);

        Page<Message> messages = testMessagesCount(conv1.getId(), user, RefereeRole.USER,
                2,
                /*total*/2, /*pagesCount*/1, /*currentPage*/1);
        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        Message first = items.get(1);
        Message second = items.get(0);

        assertEquals(opnText, first.getText());
        assertEquals(closureMessage, second.getText());
    }

    @Test
    public void testReadUnreadConversations() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        Conversation conv2 = close(conv1);
        testCanNotClose(conv1);

        Page<Message> messages = testMessagesCount(conv1.getId(), user, RefereeRole.USER,
                2,
                /*total*/2, /*pagesCount*/1, /*currentPage*/1);
        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        Message first = items.get(1);
        Message second = items.get(0);

        assertEquals(opnText, first.getText());
        assertEquals(getClosureMessage(), second.getText());

        client.markAsRead(conv2.getId(), newUID(), RefereeRole.SHOP, conv2.getShopId());
        conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertTrue(conv2.isReadBy(RefereeRole.SHOP));
        assertEquals(0, conv2.getUnreadShopCount());

        assertTrue(conv2.isReadBy(RefereeRole.USER));
        assertEquals(0, conv2.getUnreadUserCount());

        assertFalse(conv2.isReadBy(RefereeRole.ARBITER));
        assertTrue(conv2.getUnreadArbiterCount() > 0);
    }

    @Test
    public void testArchiveUnArchiveConversations() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        conv1 = client.archive(conv1.getId(), user, RefereeRole.USER, true);
        assertTrue(conv1.isArchive());

        conv1 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertTrue(conv1.isArchive());

        conv1 = client.archive(conv1.getId(), user, RefereeRole.USER, false);
        assertFalse(conv1.isArchive());

        conv1 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertFalse(conv1.isArchive());

        SearchTerms searchArchive = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withArchive(true)
                .withRead(true)
                .build();

        Page<Conversation> convs = client.searchConversations(searchArchive);
        assertNotNull(convs.getPager());
        assertEquals(Integer.valueOf(0), convs.getPager().getTotal());

        conv1 = client.archive(conv1.getId(), user, RefereeRole.USER, true);
        assertTrue(conv1.isArchive());

        convs = client.searchConversations(searchArchive);
        assertEquals(Integer.valueOf(1), convs.getPager().getTotal());
    }

    @Test
    public void testSearchReadUnreadConversations() {
        final long user = newUID();
        String opnText = "Новый мега-текст " + System.currentTimeMillis();
        String title = "New conversation " + System.currentTimeMillis();
        final Conversation conv1 = start(user, title, newOrderId(), opnText);
        assertConv(conv1);

        SearchTerms searchReadConversations = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withRead(true)
                .withArchive(false)
                .build();

        SearchTerms searchUnreadConversations = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withRead(false)
                .withArchive(false)
                .build();

        SearchTerms searchReadConversationsShop = SearchTerms.SearchTermsBuilder
                .byShopId(newUID(), conv1.getShopId())
                .withUser(user)
                .withStatus(ConversationStatus.OPEN)
                .withRead(true)
                .withArchive(false)
                .build();

        SearchTerms searchUnreadConversationsShop = SearchTerms.SearchTermsBuilder
                .byShopId(newUID(), conv1.getShopId())
                .withUser(user)
                .withStatus(ConversationStatus.OPEN)
                .withRead(false)
                .withArchive(false)
                .build();

        Page<Conversation> read = client.searchConversations(searchReadConversations);
        assertEquals(new Integer(1), read.getPager().getTotal());

        Page<Conversation> unread = client.searchConversations(searchUnreadConversations);
        assertEquals(new Integer(0), unread.getPager().getTotal());

        Page<Conversation> shopUnread = client.searchConversations(searchUnreadConversationsShop);
        assertEquals(new Integer(1), shopUnread.getPager().getTotal());

        Page<Conversation> shopRead = client.searchConversations(searchReadConversationsShop);
        assertEquals(new Integer(0), shopRead.getPager().getTotal());
    }

    @Test
    public void testSearchOrderDateConversations() {
        final long user = newUID();
        String opnText = "Новый мега-текст " + System.currentTimeMillis();
        String title = "New conversation " + System.currentTimeMillis();
        start(user, title, newOrderId(), opnText);

        SearchTerms searchReadConversations = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withOrderBefore(minutes(5))
                .withOrderSince(minutes(-5))
                .build();

        Page<Conversation> convs = client.searchConversations(searchReadConversations);
        assertEquals(new Integer(1), convs.getPager().getTotal());

        searchReadConversations = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withOrderSince(minutes(5))
                .build();

        convs = client.searchConversations(searchReadConversations);
        assertEquals(new Integer(0), convs.getPager().getTotal());

        searchReadConversations = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withOrderSince(minutes(-5))
                .build();

        convs = client.searchConversations(searchReadConversations);
        assertEquals(new Integer(1), convs.getPager().getTotal());

        searchReadConversations = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withOrderBefore(minutes(-5))
                .build();

        convs = client.searchConversations(searchReadConversations);
        assertEquals(new Integer(0), convs.getPager().getTotal());

    }

    private Date minutes(int minutes) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, minutes);
        return c.getTime();
    }

    @Test
    public void testConversationTimeShifts() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String openText = getText();
        Conversation conv1 = start(user, title, orderId, openText);
        Conversation issue = raiseIssue(conv1);

        // shift lastStatusUpdateDate
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -10);
        issue.setLastStatusTs(c.getTime());
        issue = client.updateConversation(user, RefereeRole.SYSTEM, issue);

        issue = client.getConversation(issue.getId(), user, RefereeRole.USER, null);
        assertTrue(issue.isCanEscalate());
        assertNull(issue.getCanEscalateAfterTs());
    }

    @Test
    public void testStartAndRaiseIssue() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String openText = getText();
        Conversation conv1 = start(user, title, orderId, openText);
        testCanNotRaiseIssue(conv1);
        Conversation issue = raiseIssue(conv1);

        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertConv(conv2);
        assertEquals(title, conv2.getTitle());
        assertEquals(ConversationStatus.ISSUE, conv2.getLastStatus());
        assertTrue(conv2.isCanAddMessage());
        assertTrue(conv2.isCanClose());
        // assertEquals(false, conv2.isCanEscalate());
        // assertNotNull(conv2.getCanEscalateTs());
        assertFalse(conv2.isCanRaiseIssue());
        assertNull(conv2.getCanRaiseIssueAfterTs());
        assertEquals(issue.getIssueTypes(), conv2.getIssueTypes());
        assertEquals(1, conv2.getIssueTypes().size());
        assertEquals(issue.getClaimType(), conv2.getClaimType());

        Page<Message> messages = testMessagesCount(conv1.getId(), user, RefereeRole.USER,
                3,
                /*total*/3, /*pagesCount*/1, /*currentPage*/1);
        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        Message first = items.get(2);
        Message third = items.get(0);

        assertEquals(openText, first.getText());
        assertNull(first.getConvStatusBefore());
        assertEquals(ConversationStatus.OPEN, first.getConvStatusAfter());

        assertEquals(getIssueMessage(), third.getText());
        assertEquals(ConversationStatus.OPEN, third.getConvStatusBefore());
        assertEquals(ConversationStatus.ISSUE, third.getConvStatusAfter());

        testCanNotStart(conv2);
    }

    @Test
    public void testStartAndRaiseIssueAndThenEscalate() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String openText = getText();
        Conversation conv1 = start(user, title, orderId, openText);
        Conversation issueConv = raiseIssue(conv1);

        // TODO try escalate with exception
        Conversation esc = escalate(issueConv);

        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertConv(conv2);
        assertEquals(title, conv2.getTitle());
        assertEquals(ConversationStatus.ESCALATED, conv2.getLastStatus());
        assertEquals(ArbitrageCheckType.MANUAL, conv2.getCheckType());
        assertTrue(conv2.isCanAddMessage());
        assertTrue(conv2.isCanClose());
        assertFalse(conv2.isCanEscalate());
        assertFalse(conv2.isCanRaiseIssue());
        assertEquals(esc.getIssueTypes(), conv2.getIssueTypes());
        assertEquals(1, conv2.getIssueTypes().size());
        assertEquals(esc.getClaimType(), conv2.getClaimType());

        Page<Message> messages = testMessagesCount(conv1.getId(), user, RefereeRole.USER,
                5,
                /*total*/5, /*pagesCount*/1, /*currentPage*/1);

        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        Message open = items.get(4);
        Message issue = items.get(2);
        Message shopReply = items.get(1);
        Message escalation = items.get(0);

        assertEquals(openText, open.getText());
        assertNull(open.getConvStatusBefore());
        assertEquals(ConversationStatus.OPEN, open.getConvStatusAfter());

        assertEquals(getIssueMessage(), issue.getText());
        assertEquals(ConversationStatus.OPEN, issue.getConvStatusBefore());
        assertEquals(ConversationStatus.ISSUE, issue.getConvStatusAfter());

        assertEquals(getEscalateMessage(), escalation.getText());
        assertEquals(ConversationStatus.ISSUE, escalation.getConvStatusBefore());
        assertEquals(ConversationStatus.ESCALATED, escalation.getConvStatusAfter());

        assertEquals(getShopMessage(), shopReply.getText());
        assertEquals(ConversationStatus.ISSUE, shopReply.getConvStatusBefore());
        assertEquals(ConversationStatus.ISSUE, shopReply.getConvStatusAfter());

        testCanNotStart(conv2);
    }

    @Test
    public void testStartAndRaiseIssueEscalateResolveReopen() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();

        // OPEN -> ISSUE
        Conversation conv1 = start(user, title, orderId);
        SearchTerms sOpen = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .build();

        SearchTerms sOpenIssue = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatuses(EnumSet.of(ConversationStatus.OPEN, ConversationStatus.ISSUE))
                .build();

        Page<Conversation> one = client.searchConversations(sOpen);
        assertEquals(1, one.getItems().size());
        one = client.searchConversations(sOpenIssue);
        assertEquals(1, one.getItems().size());

        // OPEN -> ISSUE
        conv1 = raiseIssue(conv1);
        Page<Conversation> none = client.searchConversations(sOpen);
        assertEquals(0, none.getItems().size());
        one = client.searchConversations(sOpenIssue);
        assertEquals(1, one.getItems().size());

        // ISSUE -> ESCALATED
        conv1 = escalate(conv1);
        none = client.searchConversations(sOpenIssue);
        assertEquals(0, none.getItems().size());

        conv1 = startArbitrage(conv1);
        assertEquals(0, conv1.getResolutionCount());

        SearchTerms searchResolved = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withResolutionType(ResolutionType.REFUND)
                .build();

        Page<Conversation> resolved = client.searchConversations(searchResolved);
        assertEquals(0, resolved.getItems().size());

        // ESCALATED -> ARBITRAGE -> CLOSED
        conv1 = resolveArbiter(conv1);
        resolved = client.searchConversations(searchResolved);
        assertEquals(1, resolved.getItems().size());
        assertEquals(conv1.getId(), resolved.getItems().iterator().next().getId());

        // CLOSED -> ESCALATED
        conv1 = reopen(conv1);

        // ESCALATED -> ARBITRAGE
        conv1 = startArbitrageSecondTime(conv1);
        assertEquals(1, conv1.getResolutionCount());

        try {
            conv1 = client.resolveIssue(conv1.getId(),
                    newUID(),
                    RefereeRole.ARBITER,
                    ResolutionType.REJECT,
                    ResolutionSubtype.SPAM,
                    null, getArbiterMessageForShop(),
                    null, "User should go and fuck himself");
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_VOCABULARY.toString(), e.getCode());
        }
        // ARBITRAGE -> CLOSED
        conv1 = resolveArbiter2(conv1);
        testCanNotAppeal(conv1);
        // CLOSED -> OPEN
        testCanStartAgain(conv1);
        assertFalse(conv1.isCanRaiseIssue());
    }

    @Test
    public void testResolveOpenResolve() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        // OPEN -> ISSUE -> ESCALATED -> ARBITRAGE -> CLOSE
        Conversation conv1 = start(user, title, orderId);
        conv1 = raiseIssue(conv1);
        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);
        conv1 = resolveArbiter(conv1);
        conv1 = client.getConversation(conv1.getId(), conv1.getUid(), RefereeRole.USER, null);
        assertTrue(conv1.isCanReopen());

        // CLOSE -> OPEN
        conv1 = testCanStartAgain(conv1);
        assertFalse(conv1.isCanRaiseIssue(), "Нельзя подать претензию, т.к. уже был вынесен вердикт");
        assertTrue(conv1.isCanReopen(), "Можно подать апелляцию");

        // OPEN -> ESCALATED -> ARBITRAGE -> CLOSE
        conv1 = escalateAndAssert(conv1);
        conv1 = startArbitrageSecondTime(conv1);
        conv1 = resolveArbiter2(conv1);
        conv1 = client.getConversation(conv1.getId(), conv1.getUid(), RefereeRole.USER, null);
        assertFalse(conv1.isCanReopen(), "Нельзя подать апелляцию дважды");

        // CLOSE -> OPEN
        conv1 = testCanStartAgain(conv1);
        assertFalse(conv1.isCanRaiseIssue());
    }

    @Test
    public void testStartAndRaiseIssueEscalateArbitrageInquiryResolve() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        final long shopId = conv1.getShopId();
        SearchTerms searchDeliveryDelays = SearchTerms.SearchTermsBuilder
                .byShopId(-2, shopId)
                .withUser(user)
                .withIssueType(IssueType.DELIVERY_DELAY)
                .build();

        Page<Conversation> dd = client.searchConversations(searchDeliveryDelays);
        assertEquals(0, dd.getItems().size());

        conv1 = raiseIssue(conv1);

        dd = client.searchConversations(searchDeliveryDelays);
        assertEquals(1, dd.getItems().size());
        assertEquals(conv1.getId(), dd.getItems().iterator().next().getId());

        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);

        SearchTerms searchInquiries = SearchTerms.SearchTermsBuilder
                .byShopId(-777L, shopId)
                .withUser(user)
                .withStatus(ConversationStatus.ARBITRAGE)
                .withInquiryTypes(EnumSet.of(InquiryType.DOCS_FROM_SHOP))
                .build();

        Page<Conversation> inq = client.searchConversations(searchInquiries);
        assertEquals(0, inq.getItems().size());

        conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.ARBITER, InquiryType.DOCS_FROM_SHOP, new Date(),
                "Gimme docs");
        assertConv(conv1);
        assertEquals(InquiryType.DOCS_FROM_SHOP, conv1.getInquiryType());
        assertNotNull(conv1.getInquiryDueTs());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));

        inq = client.searchConversations(searchInquiries);
        assertEquals(1, inq.getItems().size());
        assertEquals(conv1.getId(), inq.getItems().iterator().next().getId());

        conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.ARBITER, InquiryType.NONE, null, "Ok, thanks");
        assertConv(conv1);
        assertEquals(InquiryType.NONE, conv1.getInquiryType());
        assertNull(conv1.getInquiryDueTs());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));

        try {
            conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.SHOP, InquiryType.NONE, null, "Fuccck");
            fail("Shouldn't work for shop");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }

        conv1 = resolveArbiter(conv1);
        testCanStartAgain(conv1);
    }

    @Test
    public void testStartAndRaiseIssueEscalateArbitrageClose() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        final long shopId = conv1.getShopId();
        SearchTerms searchDeliveryDelays = SearchTerms.SearchTermsBuilder
                .byShopId(-2, shopId)
                .withUser(user)
                .withIssueType(IssueType.DELIVERY_DELAY)
                .withPage(1)
                .withPageSize(100)
                .build();
        Page<Conversation> dd = client.searchConversations(searchDeliveryDelays);
        assertEquals(0, dd.getItems().size());

        conv1 = raiseIssue(conv1);

        dd = client.searchConversations(searchDeliveryDelays);
        assertEquals(1, dd.getItems().size());
        assertEquals(conv1.getId(), dd.getItems().iterator().next().getId());

        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);
        testCanNotStart(conv1);
        conv1 = close(conv1);

        testCanStartAgain(conv1);
    }

    /**
     * Можно переоткрыть переписку и продолжать писать магазину
     * - после автозакрытия
     * - после вынесения вердикта по арбитражу/апелляции
     * - после самостоятельного закрытия претензии.
     */
    private Conversation testCanStartAgain(Conversation conv) {
        assertEquals(ConversationStatus.CLOSED, conv.getLastStatus());

        Conversation restarted = restart(conv.getUid(), conv.getShopId(), "title",
                ConversationObject.fromOrder(conv.getOrder().getOrderId()),
                "Еще остался один вопрос", null,
                new State.StateBuilder()
                        .withStatus(ConversationStatus.OPEN)
                        .withResolutionCount(conv.getResolutionCount()).build());
        assertEquals(conv.getId(), restarted.getId());
        assertEquals(ConversationStatus.OPEN, restarted.getLastStatus());
        assertEquals(conv.getResolutionCount(), restarted.getResolutionCount());
        assertEquals(conv.getResolutionType(), restarted.getResolutionType());
        assertEquals(conv.getResolutionSubType(), restarted.getResolutionSubType());
        assertEquals(conv.getResolutionTs(), restarted.getResolutionTs());
        return restarted;
    }

    @Test
    public void testStartAndRaiseIssueEscalateArbitrageInquiryResolveWithAttachments() throws IOException {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        AttachmentGroup group = client.addAttachmentGroup(conv1.getId(), user, RefereeRole.USER, null, null);
        uploadAttachment(group);
        conv1 = raiseIssueWithAttachments(conv1, group.getId());

        Page<Message> last = testMessagesCount(conv1.getId(), user, RefereeRole.USER, 3);
        Message msg = last.getItems().iterator().next();
        assertMessage(msg);

        assertEquals(group.getId(), (long) msg.getAttachmentGroupId());
        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);

        conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.ARBITER, InquiryType.DOCS_FROM_SHOP, new Date(),
                "Gimme docs");
        assertConv(conv1);
        assertEquals(InquiryType.DOCS_FROM_SHOP, conv1.getInquiryType());
        assertNotNull(conv1.getInquiryDueTs());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));

        conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.ARBITER, InquiryType.NONE, null, "Ok, thanks");
        assertConv(conv1);
        assertEquals(InquiryType.NONE, conv1.getInquiryType());
        assertNull(conv1.getInquiryDueTs());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));

        try {
            conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.SHOP, InquiryType.NONE, null, "Fuccck");
            fail("Shouldn't work for shop");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }

        resolveArbiter(conv1);
    }

    @Test
    public void testSearchInquiryOverdue() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        conv1 = raiseIssue(conv1);
        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);

        Date due = new Date();
        due = addSeconds(due, -10);

        boolean complete = false;
        do {
            due = addSeconds(due, 1);
            conv1 = client.inquiry(conv1.getId(), -1L, RefereeRole.ARBITER, InquiryType.DOCS_FROM_SHOP, due,
                    "Gimme docs");

            assertConv(conv1);
            assertEquals(InquiryType.DOCS_FROM_SHOP, conv1.getInquiryType());
            Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(due);
            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime(conv1.getInquiryDueTs());
            assertEquals(calendar1.get(Calendar.SECOND), calendar2.get(Calendar.SECOND));
            assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));

            final long shopId = conv1.getShopId();
            SearchTerms searchDueArbiter = SearchTerms.SearchTermsBuilder
                    .byRole(-1L, RefereeRole.ARBITER)
                    .withShopId(shopId)
                    .withUser(user)
                    .withStatus(ConversationStatus.ARBITRAGE)
                    .withInquiryDueBefore(new Date())
                    .withArchive(false)
                    .withPage(1)
                    .withPageSize(100)
                    .build();
            Page<Conversation> convs = client.searchConversations(searchDueArbiter);

            assertNotNull(convs);
            if (!convs.getItems().isEmpty()) {
                for (Conversation c : convs.getItems()) {
                    if (conv1.getId() == c.getId()) {
                        complete = true;
                        break;
                    }
                }
            }
            if (!complete) {
                System.out.println("Conversation " + conv1.getId() + " is still due... (" + conv1.getInquiryDueTs()
                        + ")");
            }
        } while (!complete);

        complete = false;
        do {
            final long shopId = conv1.getShopId();
            SearchTerms searchUnDueArbiter = SearchTerms.SearchTermsBuilder
                    .byRole(-1L, RefereeRole.ARBITER)
                    .withShopId(shopId)
                    .withUser(user)
                    .withStatus(ConversationStatus.ARBITRAGE)
                    .withInquiryDueBefore(new Date())
                    .withArchive(false)
                    .withPage(1)
                    .withPageSize(100)
                    .build();

            Page<Conversation> convs = client.searchConversations(searchUnDueArbiter);
            assertNotNull(convs);
            if (!convs.getItems().isEmpty()) {
                for (Conversation c : convs.getItems()) {
                    if (conv1.getId() == c.getId()) {
                        complete = true;
                        break;
                    }
                }
            }
            if (!complete) {
                System.out.println("Conversation " + conv1.getId() + " is still not due...(" + conv1.getInquiryDueTs()
                        + ")");
            }

        } while (!complete);

    }

    private Date addSeconds(Date date, int nSeconds) {
        return new Date(date.getTime() + nSeconds * 1000);
    }

    @Test
    public void testArbitrageNonEscalatedIssue() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String openText = getText();
        Conversation conv1 = start(user, title, orderId, openText);
        conv1 = raiseIssue(conv1);

        testCanNotArbitrage(conv1);
    }

    @Test
    public void testStartAndGetMessage() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        Page<Message> messages = testMessagesCount(conv1.getId(), user, RefereeRole.USER, 1);
        Message msg = messages.getItems().iterator().next();
        assertEquals(opnText, msg.getText());
        assertEquals(null, msg.getConvStatusBefore());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusAfter());
        assertEquals(user, msg.getAuthorUid());
        assertEquals(conv1.getId(), msg.getConversationId());
        assertEquals(RefereeRole.USER, msg.getAuthorRole());
        assertEquals(null, msg.getPrivacyMode());
        assertEquals(conv1.getCreatedTs(), msg.getMessageTs());
    }

    @Test
    public void testStartAndSearchCreatedConversationAsUser() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        final long shopId = conv1.getShopId();

        SearchTerms search = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withShopId(shopId)
                .withStatus(ConversationStatus.OPEN)
                .withArchive(false)
                .build();

        Page<Conversation> convPage = client.searchConversations(search);
        assertNotNull(convPage.getItems());
        assertFalse(convPage.getItems().isEmpty());
        assertEquals(1, convPage.getItems().size());
        assertEquals(conv1, convPage.getItems().iterator().next());
        assertEquals(new Long(user), conv1.getUid());
        assertNotNull(convPage.getPager());
        assertTrue(convPage.getPager().getCurrentPage() > 0);
        assertEquals(1, (int) convPage.getPager().getTotal());

        start(user, getConvTitle(), newOrderId());
        SearchTerms.SearchTermsBuilder builder = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withStatus(ConversationStatus.OPEN)
                .withArchive(false);
        SearchTerms searchAsc = builder
                .withSorting(Collections.singletonList(new Sorting(true, Column.ORDER_DATE)))
                .build();

        SearchTerms searchDesc = builder
                .withSorting(Collections.singletonList(new Sorting(false, Column.ORDER_DATE)))
                .build();

        Page<Conversation> asc = client.searchConversations(searchAsc);
        Page<Conversation> desc = client.searchConversations(searchDesc);

        assertEquals(new ArrayList<>(asc.getItems()).get(0), new ArrayList<>(desc.getItems()).get(1));
        assertEquals(new ArrayList<>(asc.getItems()).get(1), new ArrayList<>(desc.getItems()).get(0));
    }

    @Test
    public void testLongStrings() {
        final long user = newUID();
        String title = someString(DiscussionController.MAX_TITLE_LENGTH);
        String text = someString(DiscussionController.MAX_MESSAGE_LENGTH);
        final Conversation conv1 = start(user, title, newOrderId());
        assertConv(conv1);

        SearchTerms search = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withShopId(conv1.getShopId())
                .withStatus(ConversationStatus.OPEN)
                .withRead(true)
                .withArchive(false)
                .build();

        Page<Conversation> convPage = client.searchConversations(search);

        assertNotNull(convPage.getPager());
        assertTrue(convPage.getPager().getCurrentPage() > 0);
        assertEquals(1, (int) convPage.getPager().getTotal());

        assertNotNull(convPage.getItems());
        assertFalse(convPage.getItems().isEmpty());
        assertEquals(1, convPage.getItems().size());
        assertEquals(conv1, convPage.getItems().iterator().next());

        try {
            start(user, someString(DiscussionController.MAX_TITLE_LENGTH + 1), newOrderId());
            fail("Long title was accepted");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.CONSTRAINT_VIOLATION.toString(), e.getCode());
        }

        try {
            start(user, title, newOrderId(), text + "a");
            fail("Long text was accepted");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.CONSTRAINT_VIOLATION.toString(), e.getCode());
        }
    }

    @Test
    public void testStartAndSearchCreatedConversationAsShop() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        final long shopId = conv1.getShopId();

        // Right shop
        SearchTerms searchShop = SearchTerms.SearchTermsBuilder
                .byShopId(-1L, shopId)
                .withUser(user)
                .withStatus(ConversationStatus.OPEN)
                .withRead(false)
                .withArchive(false)
                .build();

        SearchTerms searchShopWrong = SearchTerms.SearchTermsBuilder
                .byShopId(-1L, shopId + 1)
                .withUser(user)
                .withStatus(ConversationStatus.OPEN)
                .withRead(false)
                .withArchive(false)
                .build();

        Page<Conversation> convPage = client.searchConversations(searchShop);
        assertNotNull(convPage.getItems());
        assertFalse(convPage.getItems().isEmpty());
        assertEquals(1, convPage.getItems().size());
        assertEquals(conv1, convPage.getItems().iterator().next());
        assertNotNull(convPage.getPager());
        assertTrue(convPage.getPager().getCurrentPage() > 0);
        assertEquals(1, (int) convPage.getPager().getTotal());

        // Wrong shop
        convPage = client.searchConversations(searchShopWrong);
        assertTrue(convPage.getItems().isEmpty());
    }

    @Test
    public void testNoAccessToConversationForOtherUser() {
        long user = newUID();
        Conversation conv1 = start(user, getConvTitle(), newOrderId());
        try {
            client.getConversation(conv1.getId(), newUID(), RefereeRole.USER, null);
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }
    }

    @Test

    public void testNoSuchOrder() {
        try {
            start(newRealUID(), getConvTitle(), BaseTest.ORDER_ID_NOT_FOUND);
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.NO_SUCH_ORDER.toString(), e.getCode());
        }
    }

    @Test
    public void testGoodAccessToConversationForArbiterOrSystem() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        Conversation conv2 = client.getConversation(conv1.getId(), newUID(), RefereeRole.ARBITER, null);
        assertEquals(conv1, conv2);
        assertTrue(conv2.isReadBy(RefereeRole.USER));
        assertEquals(0, conv2.getUnreadUserCount());
        assertFalse(conv2.isReadBy(RefereeRole.ARBITER));
        assertTrue(conv2.getUnreadArbiterCount() > 0);
        assertFalse(conv2.isReadBy(RefereeRole.SHOP));
        assertTrue(conv2.getUnreadShopCount() > 0);

        conv2 = client.getConversation(conv1.getId(), -1L, RefereeRole.SYSTEM, null);
        assertTrue(conv2.isCanAddMessage());
    }

    @Test
    public void testNoStartConvOtherThanUserOrShop() {
        try {
            client.startConversation(new ConversationRequest.Builder(
                    newUID(), RefereeRole.ARBITER, ConversationObject.fromOrder(newOrderId()), getText())
                    .withTitle(getConvTitle()).build());
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }

        try {
            client.startConversation(new ConversationRequest.Builder(
                    newUID(), RefereeRole.SYSTEM, ConversationObject.fromOrder(newOrderId()), getText())
                    .withTitle(getConvTitle()).build());
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }
    }

    @Test
    public void testShop() {
        long uid = newUID();
        String authorName = "User-" + uid;
        String code = "SUPER_CODE";
        Conversation conv1 = client.startConversation(
                new ConversationRequest.Builder(
                        uid, RefereeRole.SHOP, ConversationObject.fromOrder(newOrderId()), getText())
                        .withShopId(newUID())
                        .withAuthorName(authorName)
                        .withCode(code)
                        .withTitle(getConvTitle()).build());
        assertConvByShop(conv1);
        assertEquals(authorName, conv1.getUpdatedMessages().get(0).getAuthorName());

        long systemUid = newUID();
        Conversation conv2 = client.getConversation(conv1.getId(), systemUid, RefereeRole.SYSTEM, null);
        assertConvByShop(conv2);

        Page<Message> messages = client.getMessages(conv1.getId(), systemUid, RefereeRole.SYSTEM, null, null, null);
        Message next = messages.getItems().iterator().next();
        assertEquals(authorName, next.getAuthorName());
        assertEquals(code, next.getCode());
    }

    private void assertConvByShop(Conversation conv1) {
        assertEquals(ConversationStatus.OPEN, conv1.getLastStatus());

        // read
        assertTrue(conv1.isReadBy(RefereeRole.SHOP));
        assertFalse(conv1.isReadBy(RefereeRole.USER));
        assertFalse(conv1.isReadBy(RefereeRole.ARBITER));

        assertEquals(1, conv1.getUnreadUserCount());
        assertEquals(1, conv1.getUnreadArbiterCount());
        assertEquals(0, conv1.getUnreadShopCount());

        // participate
        assertFalse(conv1.isParticipatedBy(RefereeRole.USER));
        assertTrue(conv1.isParticipatedBy(RefereeRole.SHOP));
    }

    @Test
    public void testShopIdRequired() {
        try {
            client.getConversation(25L, newUID(), RefereeRole.SHOP, null);
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.MISSING_INPUT.toString(), e.getCode());
        }
    }

    @Test
    public void testConversationException() {
        try {
            client.getConversation(100L, newUID(), null, newOrderId());
            throw new RuntimeException();
        } catch (ErrorCodeException e) {
            /* Expected */
        }
    }

    @Test
    public void testNoConversationFound() {
        try {
            client.getConversation(-100L, newUID(), null, newOrderId());
            throw new RuntimeException();
        } catch (ErrorCodeException e) {
            /* Expected */
        }
    }

    @Test
    public void testNotifications() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 5);
        NotificationChunk nc;
        boolean found = false;

        Set<Long> toRetire = new HashSet<>();

        do {
            nc = getNotifications();

            for (Note n : nc.getNotes()) {
                if (conv1.getId() == n.getConversationId()) {
                    found = true;
                    assertNote(n);
                    assertEquals(n.getType(), NoteType.NOTIFY_SHOP);
                }
                toRetire.add(n.getId());
            }
            client.deleteNotifications(toRetire);
        } while (!nc.getNotes().isEmpty());

        assertTrue(found);

        nc = getNotifications();
        assertTrue(nc.getNotes().isEmpty());

        start(user, title, newOrderId());

        nc = getNotifications();
        assertFalse(nc.getNotes().isEmpty());

    }

    @Test
    public void testNotificationsWithPrivacy() {
        final long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);
        conv1 = raiseIssue(conv1);
        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);


        NotificationChunk nc;
        boolean found = false;

        Set<Long> toRetire = new HashSet<>();

        do {
            nc = getNotifications();

            for (Note n : nc.getNotes()) {
                if (conv1.getId() == n.getConversationId()) {
                    found = true;
                    assertNote(n);
                    assertTrue(RefereeRole.USER == n.getAuthorRole());
                }
                toRetire.add(n.getId());
            }
            client.deleteNotifications(toRetire);
        } while (!nc.getNotes().isEmpty());

        assertTrue(found);

        nc = getNotifications();
        assertTrue(nc.getNotes().isEmpty());

        client.sendMessage(new MessageRequest.Builder(conv1.getId(), -1, RefereeRole.ARBITER)
                .withPrivacy(PrivacyMode.PM_TO_USER)
                .withText("You'd better watch this").build());
        nc = getNotifications();
        assertTrue(nc.getNotes().isEmpty());

        client.sendMessage(new MessageRequest.Builder(conv1.getId(), -1, RefereeRole.ARBITER)
                .withPrivacy(PrivacyMode.PM_TO_SHOP)
                .withText("You'd better watch this").build());

        nc = getNotifications();
        assertFalse(nc.getNotes().isEmpty());
    }

    private NotificationChunk getNotifications() {
        return client.getNotifications(null, null, null);
    }

    @Test
    public void testSubscribe() {
        long user = newUID();
        Conversation conv1 = start(user, getConvTitle(), newOrderId());
        assertEquals(false, conv1.isNoteEvent(NoteEvent.USER_WHEN_ESCALATE));
        conv1 = client.subscribe(conv1.getId(), user, RefereeRole.USER, null, NoteEvent.USER_WHEN_ESCALATE);
        assertEquals(true, conv1.isNoteEvent(NoteEvent.USER_WHEN_ESCALATE));
    }

    @Test
    public void testNotifier() {
        Set<Long> discardList = new HashSet<>();

        NotificationChunk chunk = getNotifications();
        Collection<Note> notes = chunk.getNotes();

        assertNotNull(notes);

        if (!notes.isEmpty()) {
            for (Note note : notes) {
                // count += sendNotification(note);
                discardList.add(note.getId());
            }

            deleteRefereeNotes(discardList);
            discardList.clear();
        }
    }

    private void deleteRefereeNotes(Set<Long> discardList) {
        if (!discardList.isEmpty()) {
            try {
                client.deleteNotifications(discardList);
            } catch (Exception e) {
                log.error("Unable to retire old notes...", e);
                fail();
            }
        }
    }

    @Test
    public void testLabels() {
        final long user = newUID();
        long shopId = newUID();
        String title = getConvTitle();
        Label label = Label.PROBLEM; // 32
        Conversation conv = start(user, title, shopId, label);
        assertEquals(label, conv.getLastLabel());

        SearchTerms searchTerms = SearchTerms.SearchTermsBuilder
                .byUid(user)
                .withShopId(conv.getShopId())
                .withLabels(Sets.newHashSet(Label.QUESTION)) // 2
                .build();

        Page<Conversation> found = client.searchConversations(searchTerms);
        assertNotNull(found);
        assertEquals(0, found.getItems().size());

        searchTerms.setLabels(Sets.newHashSet(Label.PROBLEM, Label.QUESTION));
        found = client.searchConversations(searchTerms);
        assertNotNull(found);
        assertEquals(1, found.getItems().size());

        // user send PROBLEM -> REFUND_BAD
        Label newLabel = Label.REFUND_BAD; // 16
        Message m = client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.USER)
                .withLabel(newLabel)
                .withText("text").build());
        assertEquals(newLabel, m.getLabel());
        conv = client.getConversation(conv.getId(), user, RefereeRole.USER, null);
        assertEquals(48, conv.getLabelMask()); // REFUND_BAD + PROBLEM
        assertEquals(newLabel, conv.getLastLabel());

        // user send the same PROBLEM -> REFUND_BAD -> REFUND_BAD
        Label nullLabel = null;
        m = client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.USER)
                .withLabel(nullLabel)
                .withText("text").build());
        assertNull(m.getLabel());
        conv = client.getConversation(conv.getId(), user, RefereeRole.USER, null);
        assertEquals(48, conv.getLabelMask()); // REFUND_BAD + PROBLEM
        assertEquals(newLabel, conv.getLastLabel());

        // user send none PROBLEM -> REFUND_BAD -> REFUND_BAD -> NULL
        Label emptyLabel = Label.EMPTY;
        m = client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.USER)
                .withLabel(emptyLabel)
                .withText("text").build());
        assertEquals(emptyLabel, m.getLabel());
        conv = client.getConversation(conv.getId(), user, RefereeRole.USER, null);
        assertEquals(49, conv.getLabelMask()); // REFUND_BAD + PROBLEM + NULL_LABEL
        assertEquals(emptyLabel, conv.getLastLabel());

        // shop send
        m = client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.SHOP)
                .withShopId(conv.getShopId())
                .withLabel(emptyLabel)
                .withText("text").build());
        assertEquals(null, m.getLabel());
        conv = client.getConversation(conv.getId(), user, RefereeRole.USER, null);
        assertEquals(49, conv.getLabelMask()); // REFUND_BAD + PROBLEM
        assertEquals(emptyLabel, conv.getLastLabel());
    }

    @Test
    public void testResolveConversation() {
        final long user = newUID();
        Conversation conv = start(user, getConvTitle(), newOrderId());

        conv = raiseIssue(conv);
        conv = escalate(conv);
        conv = startArbitrage(conv);
        resolveArbiter(conv);
    }

    @Test
    public void testConvStartTwice() {
        assertThrows(ErrorCodeException.class, () -> {
            long user = newUID();
            long orderId = newOrderId();

            start(user, getConvTitle(), orderId);
            start(user, getConvTitle(), orderId);
        });
    }

}
