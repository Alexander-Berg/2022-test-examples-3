package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.disk.resource.DiskResourceId;
import ru.yandex.chemodan.app.docviewer.disk.resource.MailAttachmentId2;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MailUrlProvider2Test {
    private final static String URL = "https://meta.mail.yandex.net:443/dv-mimes" +
            "?uid=1&mid=54321&with_mulca=yes&with_inline=yes&part=1.2";

    private final MailUrlProvider2 provider = new MailUrlProvider2(
            "meta.mail.yandex.net", "metacorp.mail.yandex.net",
            "meta.mail.yandex.net", "metacorp.mail.yandex.net", 443);

    @Test
    public void rewriteUrl() {
        Assert.equals(URL, provider.rewriteUrl(createDocInfo()));
    }

    @Test
    public void getDiskResourceId() {
        Option<DiskResourceId> id = provider.getDiskResourceId(new ActualUri(URL));
        Assert.isTrue(id.isPresent() && id.get() instanceof MailAttachmentId2, "Incorrect resource id type");
        Assert.equals("54321/1.2", id.get().getServiceFileId());
    }

    @Test
    public void isSupportedActualUri() {
        Assert.isTrue(provider.isSupportedActualUri(new ActualUri(URL)), "Url should be supported");
    }

    private DocumentSourceInfo createDocInfo() {
        return DocumentSourceInfo.builder().originalUrl(
                "ya-mail://54321/1.2").uid(PassportUidOrZero.fromUid(1)).build();
    }

}
