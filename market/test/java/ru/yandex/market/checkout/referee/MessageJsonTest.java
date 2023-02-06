package ru.yandex.market.checkout.referee;

import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.MessageRequest;
import ru.yandex.market.checkout.entity.RefereeRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertAttachment;
import static ru.yandex.market.checkout.referee.test.BaseTest.assertAttachmentGroup;
import static ru.yandex.market.checkout.referee.test.BaseTest.getConvTitle;
import static ru.yandex.market.checkout.referee.test.BaseTest.newOrderId;
import static ru.yandex.market.checkout.referee.test.BaseTest.newUID;

/**
 * @author kukabara
 */
public class MessageJsonTest extends MessageTest {

    @Override
    @BeforeEach
    public void init() {
        this.client = checkoutRefereeJsonClient;
    }

    @Test
    public void testAttachWithEmptyText() throws IOException {
        long user = newUID();
        String title = getConvTitle();
        long orderId = newOrderId();
        Conversation conv1 = start(user, title, orderId);

        AttachmentGroup group = client.addAttachmentGroup(conv1.getId(), user, RefereeRole.USER, null, null);
        assertAttachmentGroup(group);

        Attachment att = uploadAttachment(group);
        assertAttachment(att);

        Message msg = client.sendMessage(new MessageRequest.Builder(conv1.getId(), user, RefereeRole.USER)
                .withGroupId(group.getId()).build());
        assertEquals(msg.getText(), null);
        assertNotNull(msg.getAttachments());
        assertEquals(1, msg.getAttachments().size());

        Collection<AttachmentGroup> attached = client.getAttached(conv1.getId(), user, RefereeRole.USER, null);
        assertNotNull(attached);
        assertEquals(1, attached.size());
        assertAttachmentGroup(attached.iterator().next());
    }
}
