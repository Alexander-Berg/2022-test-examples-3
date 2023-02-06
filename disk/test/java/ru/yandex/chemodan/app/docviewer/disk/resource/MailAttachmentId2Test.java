package ru.yandex.chemodan.app.docviewer.disk.resource;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MailAttachmentId2Test {

    @Test
    public void getPath() {
        MailAttachmentId2 id = new MailAttachmentId2("12323", "1.2");
        Assert.equals("/mail/file:12323:1.2", id.getPath());
    }

    @Test
    public void getServiceFileId() {
        MailAttachmentId2 id = new MailAttachmentId2("12323", "1.2");
        Assert.equals("12323/1.2", id.getServiceFileId());
    }
}
