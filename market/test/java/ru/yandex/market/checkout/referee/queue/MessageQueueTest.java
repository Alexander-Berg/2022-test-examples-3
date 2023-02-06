package ru.yandex.market.checkout.referee.queue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationRequest;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.RefereeErrorCode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.getText;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * {@link ru.yandex.market.checkout.referee.controller.QueueController#messageUpdates}
 *
 * @author kukabara
 */
@Transactional(propagation = Propagation.NEVER)
public class MessageQueueTest extends BaseConversationTest {
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Override
    @BeforeEach
    public void init() {
        this.client = checkoutRefereeJsonClient;
    }

    @AfterEach
    public void tearDown() throws Exception {
        pgJdbcTemplate.update("delete from a_attachment");
        pgJdbcTemplate.update("update a_attachment_group set message_id = null");
        pgJdbcTemplate.update("delete from a_message");
        pgJdbcTemplate.update("delete from a_attachment_group");
        pgJdbcTemplate.update("delete from a_conversation");
    }

    @Test
    public void testConvNotFound() throws Exception {
        try {
            client.messageUpdates(newUID(), newUID(), null, null, null);
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.NO_SUCH_CONVERSATION.toString(), e.getCode());
        }
    }

    @Test
    public void testMessages() throws Exception {
        long shopId = newUID();
        long convId = startConvByShop(shopId);
        Conversation conversation = client.getConversation(convId, 1L, RefereeRole.SHOP, shopId);
        assertNotNull(conversation);

        List<Message> messages = client.messageUpdates(convId, shopId, null, null, null);
        assertEquals(1, messages.size());

        messages = client.messageUpdates(convId, shopId, new Date(0), 0L, 10);
        assertEquals(1, messages.size());

        Date publishedTs = messages.get(0).getPublishedTs();
        long publishedId = messages.get(0).getPublishedId();
        assertEquals(1, client.messageUpdates(convId, shopId, publishedTs, 0L, 10).size());
        assertEquals(0, client.messageUpdates(convId, shopId, publishedTs, publishedId, 10).size());
    }

    @Test
    public void testMessagesWithAttachments() throws Exception {
        long shopId = newUID();
        long convId = startAndSendWithAttachments(shopId);

        List<Message> messages = client.messageUpdates(convId, shopId, null, null, null);
        assertEquals(2, messages.size());
        List<Attachment> attachments = messages.stream()
                .map(Message::getAttachments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertEquals(1, attachments.size());
    }

    public long startAndSendWithAttachments(long shopId) throws Exception {
        long convId = startConvByShop(shopId);
        Conversation conversation = client.getConversation(convId, 1L, RefereeRole.SHOP, shopId);
        assertNotNull(conversation);
        sendMessageWithAttachment(conversation);
        return convId;
    }

    @Test
    public void testFilterMessages() throws Exception {
        long shopId = newUID();
        long convId = startAndSendWithAttachments(shopId);

        List<Message> messages = client.messageUpdates(convId, shopId, null, null, null);
        Message first = messages.get(0);
        Message second = messages.get(1);

        Date publishedFromTs = second.getPublishedTs();
        Long publishedFromId = first.getMessageTs().equals(second.getMessageTs()) ? first.getPublishedId() : null;

        messages = client.messageUpdates(convId, shopId, publishedFromTs, publishedFromId, null);
        assertEquals(1, messages.size());
    }

    private long startConvByShop(long shopId) {
        return client.startConversation(new ConversationRequest.Builder(
                newUID(), RefereeRole.SHOP, ConversationObject.fromOrder(newOrderId()), getText())
                .withShopId(shopId)
                .withTitle(getConvTitle()).build()
        ).getId();
    }

    @Test
    public void testIllegalAccess() throws Exception {
        long shopId = newUID();
        long convId = startConvByShop(shopId);

        long anotherShopId = shopId + 1;
        try {
            client.messageUpdates(convId, anotherShopId, null, null, null);
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.ILLEGAL_ACCESS.toString(), e.getCode());
        }
    }
}
