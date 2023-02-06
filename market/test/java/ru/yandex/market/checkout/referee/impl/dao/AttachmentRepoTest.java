package ru.yandex.market.checkout.referee.impl.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.jpa.AttachmentGroupRepo;
import ru.yandex.market.checkout.referee.jpa.AttachmentRepo;

import static org.junit.Assert.assertEquals;

public class AttachmentRepoTest extends EmptyTest {

    @Autowired
    AttachmentGroupRepo attachmentGroupRepoDao;

    @Autowired
    AttachmentRepo itemDao;

    @Test
    public void enumConverter() {
        AttachmentGroup gr = new AttachmentGroup();
        gr.setAuthorRole(RefereeRole.ARBITER);
        gr.setPrivacyMode(PrivacyMode.PM_TO_USER);
        attachmentGroupRepoDao.save(gr);

        AttachmentGroup db = attachmentGroupRepoDao.findAll().get(0);
        assertEquals(db.getAuthorRole(), RefereeRole.ARBITER);
        assertEquals(db.getPrivacyMode(), PrivacyMode.PM_TO_USER);
    }

    @Test
    public void save() {
        AttachmentGroup gr = new AttachmentGroup();
        gr.setAuthorRole(RefereeRole.ARBITER);
        gr.setPrivacyMode(PrivacyMode.PM_TO_USER);
        attachmentGroupRepoDao.save(gr);

        Attachment at = new Attachment();
        at.setFileName("test.pdf");
        at.setFileSize(12345L);
        at.setGroup(gr);
        itemDao.save(at);
    }
}
