package ru.yandex.market.checkout.referee.impl;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.referee.impl.CheckoutRefereeServiceTest.generateConv;

public class AttachmentManagerTest extends EmptyTest {

    @Autowired
    private AttachmentManager attachmentManager;
    @Autowired
    private CheckoutRefereeService checkoutRefereeService;

    @Test
    public void getAttachment() {

        long uid = 1L;
        Conversation c = generateConv();
        c.setObject(ConversationObject.fromUserShop());
        checkoutRefereeService.insertConversation(c);

        AttachmentGroup gr = new AttachmentGroup();
        gr.setPrivacyMode(PrivacyMode.PM_TO_USER);
        gr.setConversationId(c.getId());
        gr.setAuthorRole(RefereeRole.USER);
        gr.setCreatedTs(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)));
        attachmentManager.save(gr);

        Attachment at = new Attachment();
        at.setGroup(gr);
        at.setFileName("file-name.txt");
        at.setFileSize(1L);
        gr.getAttachments().add(at);

        attachmentManager.save(at);
        attachmentManager.save(gr);

        checkoutRefereeService.sendOne(c, uid, RefereeRole.USER, "text", gr, null, null, null, ConversationStatus.OPEN, new Date());

        Attachment file = attachmentManager.getAttachment(at.getId(), c.getId(), RefereeRole.USER);
        assertNotNull(file);

        assertTrue(0 < attachmentManager.deleteUnbindAttachedGroups(1));

        assertNull(attachmentManager.load(gr.getId()));
    }

}
