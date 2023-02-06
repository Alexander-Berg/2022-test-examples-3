package ru.yandex.market.checkout.referee;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.RefereeErrorCode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.test.BaseConversationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author kukabara
 */
public class PartnerApiTest extends BaseConversationTest {
    @Override
    @BeforeEach
    public void init() {
        this.client = checkoutRefereeJsonClient;
    }

    @Test
    public void testNoAttachment() {
        long convId = RND.nextLong();
        try {
            Attachment link = client.downloadLink(convId, RND.nextLong(), RND.nextLong(),
                    RefereeRole.SHOP, RND.nextLong());
            fail("Can't load attachment, but get link = " + link);
        } catch (ErrorCodeException e) {
            assertEquals(RefereeErrorCode.NO_SUCH_CONVERSATION.toString(), e.getCode());
        }
    }

    @Test
    public void testDownloadLink() throws Exception {
        Conversation conv = start(RND.nextLong(), "title", RND.nextInt());
        sendMessageWithAttachment(conv);

        Collection<AttachmentGroup> attached = client.getAttached(conv.getId(), conv.getUid(), RefereeRole.USER, null);
        Attachment attachment = attached.stream().findFirst()
                .map(a -> a.getAttachments().stream().findFirst().orElse(null))
                .orElse(null);
        assertNotNull(attachment);

        Attachment link = client.downloadLink(conv.getId(), attachment.getId(), conv.getUid(),
                RefereeRole.USER, null);
        assertNotNull(link.getLink());
    }
}
