package ru.yandex.market.checkout.referee;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.MessageRequest;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeErrorCode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.impl.RefereeRateLimitsChecker;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;
import ru.yandex.market.checkout.referee.test.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertAttachment;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertAttachmentGroup;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getClosureMessage;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.getText;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;


public abstract class MessageTest extends BaseConversationTest {

    @Test
    public void testStartAndSendMessageAsUser() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        String code = "SUPER_CODE";
        Message message = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withPrivacy(PrivacyMode.PM_TO_SHOP)
                .withCode(code)
                .withText(opnText).build());
        assertEquals(message.getPrivacyMode(), null);

        String txt = "textx" + System.currentTimeMillis();
        Message msg = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withText(txt).build());
        assertMessage(msg);
        assertEquals(txt, msg.getText());
        assertEquals(conv1.getId(), msg.getConversationId());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusBefore());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusAfter());

        Page<Message> messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, null, null);
        assertNotNull(messages);
        assertNotNull(messages.getItems());

        ArrayList<Message> msgs = new ArrayList<>(messages.getItems());
        assertEquals(3, msgs.size());

        assertMessage(msgs.get(1));
        assertMessage(msgs.get(0));

        assertEquals(opnText, msgs.get(2).getText());
        assertEquals(code, msgs.get(1).getCode());
        assertEquals(txt, msgs.get(0).getText());

        try {
            client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER).withText("").build());
            fail("Can't send empty messages");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.MISSING_INPUT.toString(), e.getCode());
        }
    }

    @Test
    public void testStartAndSendMultilineMessage() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        String multiline = "line1\nline2\nline3";
        Message msg = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withText(multiline).build());
        assertMessage(msg);
        assertEquals(multiline, msg.getText());
        assertEquals(conv1.getId(), msg.getConversationId());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusBefore());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusAfter());

        Page<Message> pm = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, 1, 1);
        assertEquals(multiline, pm.getItems().iterator().next().getText());
    }

    @Test
    @Disabled // TODO brokenTest
    public void testObsceneLexic() {
        long user = newUID();
        String title = "Вы все ЗАДРоТЫ :)) ";
        long orderId = newOrderId();
        String opnText = getText();
        try {
            start(user, title, orderId, opnText);
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_VOCABULARY.toString(), e.getCode());
        }

        title = getConvTitle();
        Conversation conv1 = start(user, title, orderId, opnText);
        try {
            client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                    .withText("магазины гады и жлобы!" + System.currentTimeMillis()).build());
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_VOCABULARY.toString(), e.getCode());
        }

        String txt = "магазины плохие! " + System.currentTimeMillis();
        client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER).withText(txt).build());

        Page<Message> messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, null, null);
        assertNotNull(messages);
        assertNotNull(messages.getItems());

        ArrayList<Message> msgs = new ArrayList<>(messages.getItems());
        assertEquals(2, msgs.size());

        Message first = msgs.get(1);
        assertMessage(first);
        Message second = msgs.get(0);
        assertMessage(second);

        assertEquals(opnText, first.getText());
        assertEquals(txt, second.getText());

        try {
            client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER).withText("").build());
            fail("Can't send empty messages");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.MISSING_INPUT.toString(), e.getCode());
        }
    }

    @Test
    public void testStartConvAndReplyByGoodShop() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);
        Message message = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withPrivacy(PrivacyMode.PM_TO_SHOP)
                .withText(opnText).build());
        assertNull(message.getPrivacyMode());

        conv1 = client.markAsRead(conv1.getId(), newUID(), RefereeRole.SHOP, conv1.getShopId());
        conv1 = client.getConversation(conv1.getId(), newUID(), RefereeRole.SHOP, conv1.getShopId());
        assertTrue(conv1.isReadBy(RefereeRole.USER));
        assertTrue(conv1.isReadBy(RefereeRole.SHOP));
        assertFalse(conv1.isReadBy(RefereeRole.ARBITER));

        String txt = "textx" + System.currentTimeMillis();
        String authorName = "User-" + newUID();
        Message msg = client.sendMessage(new MessageRequest.Builder(conv1.getId(), newUID(), RefereeRole.SHOP)
                .withShopId(conv1.getShopId())
                .withAuthorName(authorName)
                .withText(txt).build());
        assertMessage(msg);
        assertEquals(txt, msg.getText());
        assertEquals(authorName, msg.getAuthorName());
        assertEquals(conv1.getId(), msg.getConversationId());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusBefore());
        assertEquals(ConversationStatus.OPEN, msg.getConvStatusAfter());

        Page<Message> messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, null, null);
        assertNotNull(messages);
        assertNotNull(messages.getItems());

        ArrayList<Message> msgs = new ArrayList<>(messages.getItems());
        assertEquals(3, msgs.size());

        Message first = msgs.get(1);
        assertMessage(first);
        Message second = msgs.get(0);
        assertMessage(second);

        assertEquals(opnText, first.getText());
        assertEquals(RefereeRole.USER, first.getAuthorRole());
        assertEquals(txt, second.getText());
        assertEquals(RefereeRole.SHOP, second.getAuthorRole());
        assertEquals(authorName, second.getAuthorName());

        try {
            start(user, title, orderId, "");
            fail("Can't start conversation with no message text");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.MISSING_INPUT.toString(), e.getCode());
        }
    }

    @Test
    public void testStartConvAndReplyByBadShop() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);
        Message message = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withPrivacy(PrivacyMode.PM_TO_SHOP)
                .withText("text1").build());
        assertNull(message.getPrivacyMode());

        String txt = "textx" + System.currentTimeMillis();
        try {
            client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.SHOP)
                    .withShopId(667L)
                    .withText(txt).build());
            fail("Shop can't send reply on behalf of another shop");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }
    }

    @Test
    public void testPrivateMessaging() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        // all=1
        Conversation conv1 = start(user, title, orderId, opnText);
        // all=3
        conv1 = raiseIssue(conv1);

        // all=4
        client.sendMessage(new MessageRequest.Builder(conv1.getId(), -666, RefereeRole.SHOP)
                .withShopId(conv1.getShopId())
                .withText("Text").build());

        // all=4, user=1
        client.sendMessage(new MessageRequest.Builder(conv1.getId(), -1, RefereeRole.SYSTEM)
                .withPrivacy(PrivacyMode.PM_TO_USER)
                .withText("Issue is about to expire").build());

        // all=6, user=1
        conv1 = escalate(conv1);
        // all=7, user=1
        conv1 = startArbitrage(conv1);

        // all=7, user=2
        Message prvt = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withPrivacy(PrivacyMode.PM_TO_USER)
                .withText("Private Text").build());
        assertMessage(prvt);

        Page<Message> msgsShop =
                client.getMessages(conv1.getId(), newUID(), RefereeRole.SHOP, conv1.getShopId(), null, null);
        assertEquals(7, (int) msgsShop.getPager().getTotal());

        Page<Message> msgsUser = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, null, null);
        assertEquals(9, (int) msgsUser.getPager().getTotal());

        Page<Message> msgsArbiter = client.getMessages(conv1.getId(), newUID(), RefereeRole.ARBITER, null, null, null);
        assertEquals(9, (int) msgsArbiter.getPager().getTotal());
    }

    @Test
    public void testEscalateOpenConversation() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);
        try {
            client.escalateToArbiter(conv1.getId(),
                    user,
                    RefereeRole.USER,
                    null,
                    null,
                    null,
                    "Please help");

        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }


    @Test
    public void testPostMessagesByArbiterToNonArbitrageConversation() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER).withText("text1").build());

        conv1 = raiseIssue(conv1);
        MessageRequest arbiterRequest = new MessageRequest.Builder(conv1.getId(), newUID(), RefereeRole.ARBITER)
                .withText("What's up, man?").build();
        try {
            client.sendMessage(arbiterRequest);
            fail("Arbiter can't send message into a conversation that is not in ARBITRAGE status");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }

        client.sendMessage(new MessageRequest.Builder(conv1.getId(), -666, RefereeRole.SHOP)
                .withShopId(conv1.getShopId())
                .withText("Text from SHOP?").build());

        try {
            client.sendMessage(arbiterRequest);
            fail();
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }

        conv1 = escalate(conv1);
        conv1 = startArbitrage(conv1);
        Message mess = client.sendMessage(arbiterRequest);
        assertMessage(mess);
    }


    @Test
    public void testSendMessageIntoAClosedConversation() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        conv1 = close(conv1);
        Page<Message> messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, null, null);
        Pager pager = messages.getPager();
        assertNotNull(pager);
        assertEquals(new Integer(2), pager.getTotal());
        assertEquals(new Integer(1), pager.getPagesCount());
        assertEquals(new Integer(1), pager.getCurrentPage());
        assertNotNull(messages.getItems());

        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        assertEquals(2, items.size());

        Message first = items.get(1);
        Message second = items.get(0);

        assertEquals(opnText, first.getText());
        assertEquals(getClosureMessage(), second.getText());

        try {
            client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                    .withText("Just for kicks").build());
            fail("No one can send messages to a closed conversation");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.INVALID_CONVERSATION_STATUS.toString(), e.getCode());
        }
    }


    @Test
    public void testAlterMessage() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        Message message = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withText("Naughty text").build());

        // Testing minId search
        Page<Message> messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, message.getId(),
                null, null, null);
        assertEquals(0, messages.getItems().size());

        // Testing minId search
        messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, message.getId() - 1, null, null,
                null);
        assertEquals(1, messages.getItems().size());

        String finalText = "Polite text";
        client.alterMessage(conv1.getId(), user, RefereeRole.USER, null, message.getId(), finalText);

        messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, null, null);
        Pager pager = messages.getPager();
        assertNotNull(pager);
        assertEquals(new Integer(2), pager.getTotal());
        assertEquals(new Integer(1), pager.getPagesCount());
        assertEquals(new Integer(1), pager.getCurrentPage());
        assertNotNull(messages.getItems());

        ArrayList<Message> items = new ArrayList<>(messages.getItems());
        assertEquals(2, items.size());

        Message first = items.get(1);
        Message second = items.get(0);

        assertEquals(opnText, first.getText());
        assertEquals(finalText, second.getText());
    }


    @Test
    public void testMultiPageMessagingSearch() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        String opnText = getText();
        Conversation conv1 = start(user, title, orderId, opnText);

        Page<Message> messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, 1, 1);
        Pager pager = messages.getPager();
        assertNotNull(pager);
        assertEquals(new Integer(1), pager.getTotal());
        assertEquals(new Integer(1), pager.getFrom());
        assertEquals(new Integer(1), pager.getTo());
        assertEquals(new Integer(1), pager.getPagesCount());
        assertEquals(new Integer(1), pager.getCurrentPage());
        assertNotNull(messages.getItems());
        assertEquals(opnText, messages.getItems().iterator().next().getText());

        int count = 10;
        for (int i = 0; i < count; i++) {
            String txt = "Just for kicks " + i;
            client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER).withText(txt).build());

            messages = client.getMessages(conv1.getId(), user, RefereeRole.USER, null, 1, 1);
            pager = messages.getPager();
            assertNotNull(pager);
            assertEquals(new Integer(i + 2), pager.getTotal());
            assertEquals(new Integer(1), pager.getFrom());
            assertEquals(new Integer(1), pager.getTo());
            assertEquals(new Integer(i + 2), pager.getPagesCount());
            assertEquals(new Integer(1), pager.getCurrentPage());
            assertNotNull(messages.getItems());
            assertEquals(txt, messages.getItems().iterator().next().getText());
        }
    }

    @Test
    public void testMultiPageConversationsSearch() {
        final long user = newUID();

        int count = 5;
        Conversation[] convs = new Conversation[count];
        for (int i = 0; i < count; i++) {
            convs[i] = start(user, getConvTitle(), newOrderId());
        }

        for (int i = 1; i <= count; i++) {
            SearchTerms search = SearchTerms.SearchTermsBuilder
                    .byUid(user)
                    .withStatus(ConversationStatus.OPEN)
                    .withArchive(false)
                    .withPage(i)
                    .withPageSize(1)
                    .build();

            Page<Conversation> p = client.searchConversations(search);
            Pager pager = p.getPager();
            assertNotNull(pager);
            assertEquals(new Integer(count), pager.getTotal());
            assertEquals(new Integer(i), pager.getFrom());
            assertEquals(new Integer(i), pager.getTo());
            assertEquals(new Integer(count), pager.getPagesCount());
            assertEquals(new Integer(i), pager.getCurrentPage());
            assertNotNull(p.getItems());
            assertEquals(convs[count - i].getTitle(), p.getItems().iterator().next().getTitle());
        }
    }

    @Test
    public void testStartCloseStart() {
        long user = newUID();
        long orderId = newOrderId();

        // start conversation one
        Conversation conv1 = start(user, getConvTitle(), orderId);
        client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withText("message 1").build());
        close(conv1);

        // restart conversation
        Conversation conv2 = restart(user, conv1.getShopId(), "title",
                ConversationObject.fromOrder(conv1.getOrder().getOrderId()), "message 2",
                null, new State.StateBuilder()
                        .withStatus(ConversationStatus.OPEN)
                        .withResolutionCount(conv1.getResolutionCount()).build());

        client.sendMessage(new MessageRequest.Builder(conv2.getId(), user, RefereeRole.USER)
                .withText("message 3").build());
        close(conv2);

        Page<Message> messages = client.getMessagesByOrder(user, RefereeRole.USER, null, orderId, null, null,
                null, null);
        assertEquals(messages.getItems().size(), 6);
    }

    @Test
    public void testAttached() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);
        close(conv1);

        Collection<AttachmentGroup> attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(0, attached.size());
    }

    @Test
    public void testAttachToAGroupDownload() throws IOException {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        AttachmentGroup group = client.addAttachmentGroup(conv1.getId(), user, RefereeRole.USER, null, null);
        assertAttachmentGroup(group);

        int fileLength = 6000;
        Attachment att = uploadAttachment(group, "file.bin", fileLength);
        assertAttachment(att);

        Attachment att2 = uploadAttachment(group, "file.png", fileLength);
        assertAttachment(att2);

        Collection<AttachmentGroup> attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(0, attached.size());

        Message msg = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withGroupId(group.getId())
                .withText("Attached some shit").build());

        attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(1, attached.size());
        assertAttachmentGroup(attached.iterator().next());
        long oldId = msg.getId();
        // nothing is attached until you send a message

        msg = client.alterMessage(conv1.getId(), user, RefereeRole.USER, null, msg.getId(), "Attached some awesome");
        assertEquals("Attached some awesome", msg.getText());
        assertTrue(msg.getId() > oldId);

        attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(1, attached.size());
        assertAttachmentGroup(attached.iterator().next());

        // Attachment is not lost

        List<Attachment> attachments = attached.iterator().next().getAttachments();
        assertNotNull(attachments);
        assertEquals(2, attachments.size());

        Attachment at = attachments.get(0);
        assertAttachment(at);
        assertEquals("file.bin", at.getFileName());

        try (InputStream is1 = client.downloadAttachment(conv1.getId(), at.getGroupId(), at.getId(), user, RefereeRole.USER, null)) {
            assertNotNull(is1);
            byte[] body = IOUtils.readInputStreamToBytes(is1);
            if (body.length != fileLength) {
                fail();
            }
        }

        at = attachments.get(1);
        assertAttachment(at);
        assertEquals("file.png", at.getFileName());

        try (InputStream is2 = client.downloadAttachment(conv1.getId(), at.getGroupId(), at.getId(), user, RefereeRole.USER, null)) {
            assertNotNull(is2);
            byte[] body = IOUtils.readInputStreamToBytes(is2);
            if (body.length != fileLength) {
                fail();
            }
        }
    }

    @Test
    public void testAttach20KAndDelete() throws IOException {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        AttachmentGroup group = client.addAttachmentGroup(conv1.getId(), user, RefereeRole.USER, null, null);
        assertAttachmentGroup(group);

        Attachment att = uploadAttachment(group, "file.bin", 20 * 1024);
        assertAttachment(att);

        Message msg = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withGroupId(group.getId())
                .withText("Attached some shit").build());

        Collection<AttachmentGroup> attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);

        assertNotNull(attached);
        assertEquals(1, attached.size());
        assertAttachmentGroup(attached.iterator().next());
        assertEquals(msg.getId(), (long) attached.iterator().next().getMessageId());

        try (InputStream is = client.downloadAttachment(
                conv1.getId(), group.getId(),
                attached.iterator().next().getAttachments().get(0).getId(),
                user,
                RefereeRole.USER,
                null)) {

            int count = 0;
            int rd;
            for (; (rd = is.read()) >= 0; ) {
                count++;
                assertEquals('H', rd);
            }
            assertEquals(20 * 1024, count);
        }

        AttachmentGroup ag = client.deleteAttachment(conv1.getId(),
                attached.iterator().next().getId(),
                attached.iterator().next().getAttachments().get(0).getId(), user, RefereeRole.USER, null);
        assertEquals(0, ag.getAttachments().size());

        attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(1, attached.size());
        assertAttachmentGroup(attached.iterator().next());
        assertEquals(msg.getId(), (long) attached.iterator().next().getMessageId());
        assertEquals(0, attached.iterator().next().getAttachments().size());
    }

    @Test
    public void testAttachDOCX() throws IOException {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        Message msg = sendMessageWithAttachment(conv1);

        Collection<AttachmentGroup> attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);

        assertNotNull(attached);
        assertEquals(1, attached.size());
        assertAttachmentGroup(attached.iterator().next());
        assertEquals(msg.getId(), (long) attached.iterator().next().getMessageId());
    }

    @Test
    public void testMultiAttachGroups() throws IOException {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        AttachmentGroup group1 = client.addAttachmentGroup(conv1.getId(), user, RefereeRole.USER, null, null);
        assertAttachmentGroup(group1);

        Attachment att1 = uploadAttachment(group1, "file1.bin");
        assertAttachment(att1);

        Message msg1 = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withGroupId(group1.getId())
                .withText("Attached some shit1").build());

        AttachmentGroup group2 = client.addAttachmentGroup(conv1.getId(), user, RefereeRole.USER, null, null);
        assertAttachmentGroup(group2);

        Attachment att2 = uploadAttachment(group2, "file2.bin");
        assertAttachment(att2);

        Message msg2 = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withGroupId(group2.getId())
                .withText("Attached some shit1").build());

        Collection<AttachmentGroup> attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(2, attached.size());

        int i = 1;
        for (AttachmentGroup ag : attached) {
            for (Attachment a : ag.getAttachments()) {
                assertEquals("file" + i + ".bin", a.getFileName());
            }
            if (i == 1) {
                assertEquals(Long.valueOf(msg1.getId()), ag.getMessageId());
            }
            if (i == 2) {
                assertEquals(Long.valueOf(msg2.getId()), ag.getMessageId());
            }
            i++;

        }
    }

    @Autowired
    private RefereeRateLimitsChecker rateLimitsChecker;

    @Test
    public void testStartConversationRateLimit() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        int count = rateLimitsChecker.getLimitStartForHour() + 1;

        try {
            for (int i = 0; i < count; i++) {
                start(user, title, orderId++);
            }
            fail("Rate limits are not checked");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.LIMIT_VIOLATION.toString(), e.getCode());
        }

    }

    @Test
    public void testSendMessageRateLimit() {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();

        int count = rateLimitsChecker.getLimitSendForHour() + 1;
        try {
            Conversation conv = start(user, title, orderId);
            for (int i = 0; i < count; i++) {
                client.sendMessage(new MessageRequest.Builder(conv.getId(), user, RefereeRole.USER)
                        .withText("" + System.currentTimeMillis()).build());
            }
            fail("Rate limits are not checked");
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.LIMIT_VIOLATION.toString(), e.getCode());
        }
    }
}
