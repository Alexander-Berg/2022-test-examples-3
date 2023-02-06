package ru.yandex.market.checkout.referee.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.ClaimType;
import ru.yandex.market.checkout.entity.ClosureType;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.InquiryType;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.Label;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.MessageRequest;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeErrorCode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.ResolutionSubtype;
import ru.yandex.market.checkout.entity.ResolutionType;
import ru.yandex.market.checkout.referee.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertAttachment;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertAttachmentGroup;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertConv;
import static ru.yandex.market.checkout.referee.test.BaseTest.getArbiterMessageForShop;
import static ru.yandex.market.checkout.referee.test.BaseTest.getArbiterMessageForUser;
import static ru.yandex.market.checkout.referee.test.BaseTest.getClosureMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getEscalateMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getIssueMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getReopenMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getShopMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getText;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;
import static ru.yandex.market.checkout.util.EnumSetUtil.enumSet;

public abstract class BaseConversationTest extends EmptyTest {
    // OPEN
    protected void testCanNotStart(Conversation conv) {
        try {
            client.startConversation(new ConversationRequest.Builder(
                    conv.getUid(), RefereeRole.USER, conv.getObject(), getText())
                    .withShopId(conv.getShopId())
                    .withRgb(conv.getRgb())
                    .withTitle("Еще остался один вопрос").build());
            fail("Can't start conversation after " + conv.getLastStatus());
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }

    protected Conversation start(long user, String title, long shopId, Label label) {
        long orderId = newOrderId();
        return start(user, shopId, title, ConversationObject.fromOrder(orderId), getText(), label);
    }

    protected Conversation start(long user, String title, long orderId) {
        return start(user, title, orderId, getText());
    }

    protected Conversation start(long user, String title, long orderId, String openText) {
        return start(user, title, ConversationObject.fromOrder(orderId), openText);
    }

    public Conversation start(long user, String title, ConversationObject object, String openText) {
        return start(user, newUID(), title, object, openText, null);
    }

    private Conversation start(long user, long shopId, String title,
                               ConversationObject object, String openText, Label label) {
        Conversation conv1 = restart(user, shopId, title, object, openText, label, State.AFTER_START);
        assertEquals(title.replaceAll("\r", " "), conv1.getTitle());

        if (conv1.getOrder() != null) {
            assertFalse(conv1.isCanRaiseIssue(), "Can not raise issue before " + conv1.getCanRaiseIssueAfterTs());
            assertNotNull(conv1.getCanRaiseIssueAfterTs());
            assertNotNull(conv1.getCanRaiseIssueBeforeTs());
        }
        // participated
        assertFalse(conv1.isParticipatedBy(RefereeRole.SHOP));

        // read
        assertEquals(1, conv1.getUnreadShopCount());
        assertEquals(1, conv1.getUnreadArbiterCount());
        return conv1;
    }

    protected Conversation restart(long user, long shopId, String title,
                                   ConversationObject object, String openText, Label label,
                                   State expectedState) {
        Conversation conv1 = client.startConversation(
                new ConversationRequest.Builder(user, RefereeRole.USER, object, openText)
                        .withShopId(shopId)
                        .withLabel(label)
                        .withTitle(title).build());
        assertConv(conv1);
        assertEquals(expectedState.status, conv1.getLastStatus());
        assertEquals(true, conv1.isCanAddMessage());
        assertEquals(true, conv1.isCanClose());
        assertEquals(expectedState.canEscalate, conv1.isCanEscalate());
        assertTrue(
                expectedState.canEscalateAfterNotNull && conv1.getCanEscalateAfterTs() != null ||
                        !expectedState.canEscalateAfterNotNull && conv1.getCanEscalateAfterTs() == null
        );
        assertTrue(conv1.getLastStatus() == ConversationStatus.ISSUE ? conv1.getAutoCloseIssueTs() != null :
                conv1.getAutoCloseIssueTs() == null);

        assertEquals(expectedState.resolutionCount, conv1.getResolutionCount());
        if (expectedState.resolutionCount == 1) {
            assertTrue(conv1.isCanReopen(), "Можно подать апелляцию, если вердикт вынесен 1 раз");
            assertNotNull(conv1.getCanReopenBeforeTs());
        } else {
            assertFalse(conv1.isCanReopen());
            assertNull(conv1.getCanReopenBeforeTs());
        }

        if (expectedState.resolutionCount >= 1) {
            assertFalse(conv1.isCanRaiseIssue(), "Нельзя подать претензию, если уже был вынесен вердикт");
            assertNull(conv1.getCanRaiseIssueAfterTs());
            assertNull(conv1.getCanRaiseIssueBeforeTs());
        }

        // participated
        assertTrue(conv1.isParticipatedBy(RefereeRole.USER));

        // read
        assertTrue(conv1.isReadBy(RefereeRole.USER));
        assertFalse(conv1.isReadBy(RefereeRole.SHOP));
        assertFalse(conv1.isReadBy(RefereeRole.ARBITER));
        assertEquals(0, conv1.getUnreadUserCount());

        // archive
        assertFalse(conv1.isArchive());

        Conversation conv2 = client.getConversation(conv1.getId(), newUID(), RefereeRole.SYSTEM, null);
        assertEquals(conv1, conv2);

        return conv1;
    }

    protected Message sendMessageWithAttachment(Conversation conv) throws IOException {
        long user = conv.getUid();
        AttachmentGroup group = client.addAttachmentGroup(conv.getId(), user, RefereeRole.USER, null, null);
        assertAttachmentGroup(group);

        Attachment att = uploadAttachment(group);
        assertAttachment(att);

        return client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.USER)
                .withGroupId(group.getId())
                .withText("Attached some shit").build());
    }

    protected Attachment uploadAttachment(AttachmentGroup group) throws IOException {
        return uploadAttachment(group, "file.bin", 5 * 1000);
    }

    protected Attachment uploadAttachment(AttachmentGroup group, String fileName) throws IOException {
        return uploadAttachment(group, fileName, 5 * 1000);
    }

    protected Attachment uploadAttachment(AttachmentGroup group, String fileName, int size) throws IOException {
        File file = File.createTempFile("test", "sfx");
        file.deleteOnExit();
        writeSomeData(file, size);
        return client.uploadAttachment(group.getConversationId(),
                group.getId(), group.getAuthorUid(), group.getAuthorRole(), null, fileName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", file);
    }

    private void writeSomeData(File file, int bytes) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (int i = 0; i < bytes; i++) {
                fos.write("H".getBytes());
            }
        } catch (IOException e) {
            /* ignore */
        }
    }

    // OPEN -> ISSUE
    protected void testCanNotRaiseIssue(Conversation conv) {
        try {
            client.raiseIssue(conv.getId(), conv.getUid(), RefereeRole.USER, enumSet(IssueType.DELIVERY_DELAY),
                    ClaimType.DELIVERY, null, getIssueMessage());
            fail("Can't raise issue for convId=" + conv.getId() + " after conversation after " + conv.getLastStatus());
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }

    protected void testCanNotRaiseIssue(ConversationRequest request) {
        try {
            client.raiseIssue(request);
            fail("Can't raise issue for user request=" + request.getUid());
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }

    protected Conversation redIssue(long uid, Long orderId, Long itemId, State expectedState) {
        ConversationObject conversationObject = ConversationObject.fromOrderItem(orderId, itemId);
        ConversationRequest request = redIssueRequest(uid, conversationObject);
        return redIssue(request, expectedState);
    }

    protected ConversationRequest redIssueRequest(long uid, ConversationObject conversationObject) {
        return new ConversationRequest.Builder(uid, RefereeRole.USER, conversationObject, "I want help from support")
                .withTitle("some title")
                .withUserEmail("user@mail.com")
                .withShopId(newUID())
                .withPrivacyMode(PrivacyMode.PM_TO_USER)
                .withRgb(Color.RED)
                .build();
    }

    protected Conversation redIssue(ConversationRequest request, State expectedState) {
        Conversation issue = client.raiseIssue(request);
        // read
        assertTrue(issue.isReadBy(RefereeRole.USER));
        assertEquals(0, issue.getUnreadUserCount());
        assertFalse(issue.isReadBy(RefereeRole.SHOP));

        assertEquals(ConversationStatus.ISSUE, issue.getLastStatus());
        assertTrue(issue.isCanAddMessage());
        assertTrue(issue.isCanClose());

        assertEquals(expectedState.canEscalate, issue.isCanEscalate());

        // participated
        assertTrue(issue.isParticipatedBy(RefereeRole.USER));
        assertEquals(expectedState.canEscalate, issue.isParticipatedBy(RefereeRole.SHOP));
        return issue;
    }

    protected Conversation raiseIssue(Conversation conv1) {
        return raiseIssueWithAttachments(conv1, null);
    }

    protected Conversation raiseIssueWithAttachments(Conversation conv1, Long groupId) {
        return raiseIssueWithAttachments(conv1, groupId, null);
    }

    protected Conversation raiseIssueWithAttachments(Conversation conv1, Long groupId, Label label) {
        long user = conv1.getUid();
        String issueMessage = getIssueMessage();

        String authorName = "User" + newUID();
        Message message = client.sendMessage(new MessageRequest.Builder(conv1.getId(), newUID(), RefereeRole.SHOP)
                .withShopId(conv1.getShopId())
                .withAuthorName(authorName)
                .withLabel(label)
                .withText(getShopMessage()).build());
        assertEquals(authorName, message.getAuthorName());

        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);
        assertConv(conv2);
        assertTrue(conv2.isCanRaiseIssue());
        assertTrue(conv2.isParticipatedBy(RefereeRole.USER));
        assertTrue(conv2.isParticipatedBy(RefereeRole.SHOP));

        Conversation issue = client.raiseIssue(conv1.getId(), user, RefereeRole.USER,
                enumSet(IssueType.DELIVERY_DELAY), ClaimType.DELIVERY, groupId, issueMessage);
        assertConv(issue);

        // read
        assertTrue(issue.isReadBy(RefereeRole.USER));
        assertFalse(issue.isReadBy(RefereeRole.SHOP));

        assertEquals(conv1.getTitle(), issue.getTitle());
        assertEquals(ConversationStatus.ISSUE, issue.getLastStatus());
        assertTrue(issue.isCanAddMessage());
        assertTrue(issue.isCanClose());

        assertFalse(issue.isCanRaiseIssue());
        assertNull(issue.getCanRaiseIssueAfterTs());
        assertNull(issue.getCanRaiseIssueBeforeTs());
        assertNotNull(issue.getAutoCloseIssueTs());

        assertFalse(issue.isCanEscalate());
        assertNotNull(issue.getCanEscalateAfterTs());

        assertTrue(issue.getIssueTypes().contains(IssueType.DELIVERY_DELAY));
        assertEquals(1, issue.getIssueTypes().size());
        assertEquals(ClaimType.DELIVERY, issue.getClaimType());
        assertNull(issue.getCanReopenBeforeTs());

        // participated
        assertTrue(issue.isParticipatedBy(RefereeRole.USER));
        assertFalse(issue.isParticipatedBy(RefereeRole.SHOP));

        assertEquals(0, issue.getUnreadUserCount());
        assertEquals(conv2.getUnreadShopCount() + 1, issue.getUnreadShopCount());
        assertEquals(conv2.getUnreadArbiterCount() + 1, issue.getUnreadArbiterCount());

        return issue;
    }

    // ISSUE -> ESCALATE
    protected Conversation escalate(Conversation conv) {
        Conversation conv1 = escalateSecondTime(conv);
        assertEquals(false, conv1.isParticipatedBy(RefereeRole.ARBITER));
        return conv1;
    }

    protected Conversation escalateSecondTime(Conversation conv) {
        client.sendMessage(new MessageRequest.Builder(conv.getId(), newUID(), RefereeRole.SHOP)
                .withShopId(conv.getShopId())
                .withText(getShopMessage()).build());

        Conversation conv0 = client.getConversation(conv.getId(), conv.getUid(), RefereeRole.USER, null);
        assertTrue(conv0.isCanEscalate(), "После сообщения магазина должна появляться возможность /escalate");
        assertNull(conv0.getCanEscalateAfterTs());
        assertTrue(conv0.isParticipatedBy(RefereeRole.USER));
        assertTrue(conv0.isParticipatedBy(RefereeRole.SHOP));

        return escalateAndAssert(conv0);
    }

    protected Conversation escalateAndAssert(Conversation conv) {
        Conversation conv1 = client.escalateToArbiter(
                conv.getId(), conv.getUid(), RefereeRole.USER, null, null, null, getEscalateMessage());

        assertConv(conv1);
        // read
        assertTrue(conv1.isReadBy(RefereeRole.USER));
        assertFalse(conv1.isReadBy(RefereeRole.SHOP));

        assertEquals(conv.getTitle(), conv1.getTitle());
        assertTrue(conv1.isCanAddMessage());
        assertTrue(conv1.isCanClose());
        assertFalse(conv1.isCanEscalate());
        assertNull(conv1.getCanEscalateAfterTs());
        assertFalse(conv1.isCanRaiseIssue());
        assertEquals(conv.getIssueTypes(), conv1.getIssueTypes());
        assertEquals(conv.getClaimType(), conv1.getClaimType());
        assertTrue(conv1.isParticipatedBy(RefereeRole.SHOP));
        assertNull(conv1.getCanReopenBeforeTs());
        assertEquals(0, conv1.getUnreadUserCount());
        assertEquals(conv.getUnreadShopCount() + 1, conv1.getUnreadShopCount());
        assertEquals(conv.getUnreadArbiterCount() + 1, conv1.getUnreadArbiterCount());

        return conv1;
    }

    protected Conversation resolveArbiter(Conversation conv) {
        String userResolution = getArbiterMessageForUser();
        String shopResolution = getArbiterMessageForShop();
        Conversation resolvedConv = client.resolveIssue(conv.getId(), newUID(), RefereeRole.ARBITER,
                ResolutionType.REFUND, ResolutionSubtype.RULES_VIOLATION, null, shopResolution, null, userResolution);
        assertConv(resolvedConv);
        assertFalse(resolvedConv.isCanReopen());
        assertEquals(ResolutionType.REFUND, resolvedConv.getResolutionType());
        assertEquals(ResolutionSubtype.RULES_VIOLATION, resolvedConv.getResolutionSubType());
        assertEquals(1, resolvedConv.getResolutionCount());
        assertNotNull(resolvedConv.getResolutionTs());
        assertTrue(resolvedConv.isParticipatedBy(RefereeRole.ARBITER));
        assertEquals(0, resolvedConv.getUnreadArbiterCount());
        assertEquals(conv.getUnreadShopCount() + 1, resolvedConv.getUnreadShopCount());
        assertEquals(conv.getUnreadUserCount() + 1, resolvedConv.getUnreadUserCount());

        Conversation conv1 = client.getConversation(conv.getId(), conv.getUid(), RefereeRole.USER, null);

        assertConv(conv1);
        assertEquals(ConversationStatus.CLOSED, conv1.getLastStatus());
        assertEquals(resolvedConv.getResolutionType(), conv1.getResolutionType());
        assertEquals(resolvedConv.getResolutionSubType(), conv1.getResolutionSubType());
        assertEquals(1, conv1.getResolutionCount());
        assertNotNull(conv1.getResolutionTs());
        assertTrue(conv1.isCanReopen());
        assertNotNull(conv1.getCanReopenBeforeTs());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));

        Page<Message> messages = client.getMessages(conv.getId(), conv.getUid(), RefereeRole.USER, null, null, null);
        assertNotNull(messages);
        ArrayList<Message> msgs = new ArrayList<>(messages.getItems());
        Message resolutionForUser = msgs.get(0);
        assertEquals(resolvedConv.getResolutionType(), resolutionForUser.getResolutionType());
        assertEquals(resolvedConv.getResolutionSubType(), resolutionForUser.getResolutionSubType());
        assertEquals(userResolution.replaceAll("\r", "\n"), resolutionForUser.getText());

        messages = client.getMessages(conv.getId(), conv.getUid(), RefereeRole.SHOP, conv.getShopId(), null, null);
        assertNotNull(messages);
        msgs = new ArrayList<>(messages.getItems());
        Message resolutionForShop = msgs.get(0);
        assertEquals(resolvedConv.getResolutionType(), resolutionForShop.getResolutionType());
        assertEquals(resolvedConv.getResolutionSubType(), resolutionForShop.getResolutionSubType());
        assertEquals(shopResolution.replaceAll("\r", "\n"), resolutionForShop.getText());

        return resolvedConv;
    }

    protected Conversation resolveArbiter2(Conversation conv) {
        Conversation conv1 = client.resolveIssue(conv.getId(),
                newUID(),
                RefereeRole.ARBITER,
                ResolutionType.REJECT,
                ResolutionSubtype.SPAM,
                null, getArbiterMessageForShop() + " It's our final desision.",
                null, getArbiterMessageForUser() + " It's our final desision.");

        assertConv(conv1);
        assertFalse(conv1.isCanReopen());
        assertEquals(2, conv1.getResolutionCount());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));
        assertEquals(0, conv1.getUnreadArbiterCount());
        assertEquals(conv.getUnreadShopCount() + 1, conv1.getUnreadShopCount());
        assertEquals(conv.getUnreadUserCount() + 1, conv1.getUnreadUserCount());

        conv1 = client.getConversation(conv1.getId(), conv.getUid(), RefereeRole.USER, null);

        assertConv(conv1);
        assertEquals(ConversationStatus.CLOSED, conv1.getLastStatus());
        assertEquals(ResolutionType.REJECT, conv1.getResolutionType());
        assertEquals(ResolutionSubtype.SPAM, conv1.getResolutionSubType());
        assertEquals(2, conv1.getResolutionCount());
        assertFalse(conv1.isCanReopen());
        assertNull(conv1.getCanReopenBeforeTs());

        Page<Message> messages =
                client.getMessages(conv.getId(), conv.getUid(), RefereeRole.USER, null, null, null);
        assertNotNull(messages);
        ArrayList<Message> msgs = new ArrayList<>(messages.getItems());
        for (Message msg : msgs) {
            if (msg.getConvStatusAfter() == ConversationStatus.CLOSED && msg.getAuthorRole() == RefereeRole.ARBITER) {
                if (msg.getMessageTs().equals(conv1.getResolutionTs())) {
                    // appeal
                    assertEquals(conv1.getResolutionType(), msg.getResolutionType());
                    assertEquals(conv1.getResolutionSubType(), msg.getResolutionSubType());
                } else {
                    // resolution
                    assertEquals(conv.getResolutionType(), msg.getResolutionType());
                    assertEquals(conv.getResolutionSubType(), msg.getResolutionSubType());
                }
            }
        }

        return conv1;
    }

    protected void testCanNotAppeal(Conversation conv) {
        try {
            client.reopen(conv.getId(),
                    conv.getUid(),
                    RefereeRole.USER,
                    null,
                    "Не согласен с вынесенным решением");

            fail("Can't reopen conversation after " + conv.getLastStatus());
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }

    protected Conversation reopen(Conversation conv) {
        Conversation conv1 = client.reopen(conv.getId(),
                conv.getUid(),
                RefereeRole.USER,
                null,
                getReopenMessage());

        assertConv(conv1);
        assertEquals(ConversationStatus.ESCALATED, conv1.getLastStatus());
        assertEquals(ResolutionType.REFUND, conv1.getResolutionType());
        assertEquals(ResolutionSubtype.RULES_VIOLATION, conv1.getResolutionSubType());
        assertEquals(1, conv1.getResolutionCount());
        assertNotNull(conv1.getResolutionTs());
        assertFalse(conv1.isCanReopen());
        assertNull(conv1.getCanReopenBeforeTs());
        assertEquals(0, conv1.getUnreadUserCount());

        return conv1;
    }

    protected void testCanNotArbitrage(Conversation conv1) {
        try {
            client.startArbitrage(conv1.getId(), -1L, RefereeRole.SYSTEM);
            fail("Can't start arbitrage after " + conv1.getLastStatus());
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }

    protected Conversation startArbitrage(Conversation conv1) {
        conv1 = client.startArbitrage(conv1.getId(), -1L, RefereeRole.SYSTEM);
        assertEquals(InquiryType.NONE, conv1.getInquiryType());
        assertNull(conv1.getInquiryDueTs());
        assertNull(conv1.getClosureType());
        assertFalse(conv1.isParticipatedBy(RefereeRole.ARBITER));
        assertNull(conv1.getCanReopenBeforeTs());

        return conv1;
    }

    protected Conversation startArbitrageSecondTime(Conversation conv1) {
        conv1 = client.startArbitrage(conv1.getId(), -1L, RefereeRole.SYSTEM);
        assertNull(conv1.getClosureType());
        assertTrue(conv1.isParticipatedBy(RefereeRole.ARBITER));
        assertNull(conv1.getCanReopenBeforeTs());

        return conv1;
    }

    protected void testCanNotClose(Conversation conv) {
        try {
            client.closeConversation(conv.getId(), conv.getUid(), RefereeRole.USER, null, null);
            fail("Can't close conversation from status " + conv.getLastStatus());
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }

    protected Conversation close(Conversation conv1, State expectedState) {
        return close(conv1, getClosureMessage(), expectedState);
    }

    protected Conversation close(Conversation conv1) {
        return close(conv1, getClosureMessage(), State.AFTER_CLOSE);
    }

    protected Conversation close(Conversation conv1, String closureMessage) {
        return close(conv1, closureMessage, State.AFTER_CLOSE);
    }

    protected Conversation close(Conversation conv1, String closureMessage, State expectedState) {
        long user = conv1.getUid();
        conv1 = client.closeConversation(conv1.getId(), user, RefereeRole.USER, ClosureType.REPLACED, closureMessage);

        assertConv(conv1);
        assertEquals(ClosureType.REPLACED, conv1.getClosureType());
        assertEquals(ConversationStatus.CLOSED, conv1.getLastStatus());

        Conversation conv2 = client.getConversation(conv1.getId(), user, RefereeRole.USER, null);

        assertConv(conv2);
        assertEquals(conv1.getTitle(), conv2.getTitle());
        assertEquals(expectedState.status, conv2.getLastStatus());
        assertEquals(ClosureType.REPLACED, conv2.getClosureType());
        assertFalse(conv2.isCanAddMessage());
        assertFalse(conv2.isCanClose());
        assertEquals(expectedState.canEscalate, conv2.isCanEscalate());
        assertEquals(expectedState.canRaiseIssue, conv2.isCanRaiseIssue());
        assertFalse(conv2.isCanReopen());
        assertNull(conv2.getAutoCloseIssueTs());
        assertTrue(conv2.isParticipatedBy(RefereeRole.USER));

        // read
        assertTrue(conv2.isReadBy(RefereeRole.USER));
        assertFalse(conv2.isReadBy(RefereeRole.SHOP));
        assertEquals(0, conv2.getUnreadUserCount());
        return conv1;
    }

    protected Page<Message> testMessagesCount(long convId, long uid, RefereeRole role, int cnt) {
        return testMessagesCount(convId, uid, role, cnt, null, null, null);
    }

    protected Page<Message> testMessagesCount(long convId, long uid, RefereeRole role,
                                              int cnt,
                                              Integer total, Integer pagesCount, Integer currentPage) {
        Page<Message> messages = client.getMessages(convId, uid, role, null, null, null);
        Pager pager = messages.getPager();
        assertNotNull(pager);
        if (total != null) {
            assertEquals(total, pager.getTotal());
        }
        if (pagesCount != null) {
            assertEquals(pagesCount, pager.getPagesCount());
        }
        if (currentPage != null) {
            assertEquals(currentPage, pager.getCurrentPage());
        }
        assertNotNull(messages.getItems());
        assertEquals(cnt, messages.getItems().size(),
                messages.getItems().stream()
                        .sorted(Comparator.comparingLong(Message::getId))
                        .map(m -> m.getId() + ", " +
                                m.getAuthorRole() + ", " +
                                m.getConvStatusBefore() + " -> " + m.getConvStatusAfter() + ", " +
                                m.getText())
                        .collect(Collectors.joining("\n")));

        return messages;
    }
}
