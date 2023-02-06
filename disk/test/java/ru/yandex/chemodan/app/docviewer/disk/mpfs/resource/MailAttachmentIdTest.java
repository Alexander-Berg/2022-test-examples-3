package ru.yandex.chemodan.app.docviewer.disk.mpfs.resource;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.disk.resource.MailAttachmentId;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MailAttachmentIdTest {

    @Test
    public void mulcaIdToMpfsUrl() {
        MailAttachmentId id = new MailAttachmentId(MulcaId.valueOf("123456", "1.2"), "");
        Assert.equals("/mulca/123456:1.2", id.getPath());
    }
}
