package ru.yandex.chemodan.app.docviewer.disk.mpfs;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class MpfsUrlHelperTest extends DocviewerSpringTestBase {

    @Value("${mpfs.host}")
    private String mpfsHost;

    @Autowired
    private MpfsUrlHelper mpfsUrlHelper;

    @Test
    public void getUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/url?uid=666&path=somefileid",
                mpfsUrlHelper.getUrl(PassportUidOrZero.fromUid(666), "somefileid"));
    }

    @Test
    public void getDirectUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/direct_url?uid=666&path=somefileid",
                mpfsUrlHelper.getDirectUrl(PassportUidOrZero.fromUid(666), "somefileid"));
    }

    @Test
    public void getPubilcUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/public_url?private_hash=somehashvalue",
                mpfsUrlHelper.getPublicUrl("somehashvalue"));
    }

    @Test
    public void getPubilcDirectUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/public_direct_url?private_hash=somehashvalue",
                mpfsUrlHelper.getPublicDirectUrl("somehashvalue"));
    }

    @Test
    public void rewriteDirectUrlToUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/url?uid=666&path=somefileid",
                mpfsUrlHelper.rewriteDirectUrlToUrl(
                        "https://" + mpfsHost + "/service/direct_url?uid=666&path=somefileid", false,
                        PassportUidOrZero.fromUid(666)));
    }

    @Test
    public void rewriteDirectUrlToUrlWithInline() {
        Assert.equals(
                "https://" + mpfsHost + "/service/url?uid=666&path=somefileid&inline=1",
                mpfsUrlHelper.rewriteDirectUrlToUrl(
                        "https://" + mpfsHost + "/service/direct_url?uid=666&path=somefileid", true,
                        PassportUidOrZero.zero()));
    }

    @Test
    public void rewritePublicDirectUrlToPublicUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/public_url?private_hash=somehashvalue",
                mpfsUrlHelper.rewriteDirectUrlToUrl(
                        "https://" + mpfsHost + "/service/public_direct_url?private_hash=somehashvalue", false,
                        PassportUidOrZero.zero()));
    }

    @Test
    public void getAsyncCopyDefaultUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/async_copy_default?uid=12&path=%2Fmail%2F22220001%3A1.3",
                mpfsUrlHelper.getAsyncCopyDefaultUrl(PassportUidOrZero.fromUid(12), "/mail/22220001:1.3",
                        Option.empty(), false));
    }

    @Test
    public void getPublicCopyUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/service/public_copy?uid=12&private_hash=asdf",
                mpfsUrlHelper.getPublicCopyUrl(PassportUidOrZero.fromUid(12), "asdf", Option.empty()));
    }

    @Test
    public void getPublicInfoUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/json/public_info?private_hash=asdf&meta=short_url&uid=123",
                mpfsUrlHelper.getPublicInfoUrl(PassportUidOrZero.fromUid(123), "asdf", Cf.set("short_url")));
    }

    @Test
    public void getInfoUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/json/info?uid=123&path=%2Fdisk%2Ffile.pdf",
                mpfsUrlHelper.getInfoUrl(PassportUidOrZero.fromUid(123), "/disk/file.pdf", Cf.set()));
    }

    @Test
    public void getSetPublicUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/json/set_public?uid=123&path=%2Fdisk%2Ffile.txt",
                mpfsUrlHelper.getSetPublicFileUrl(PassportUidOrZero.fromUid(123), "/disk/file.txt"));
    }

    @Test
    public void getOperationStatus() {
        Assert.equals(
                "https://" + mpfsHost + "/json/status?uid=123&oid=asdfasdf&meta=short_url",
                mpfsUrlHelper.getOperationStatusUrl(PassportUidOrZero.fromUid(123), "asdfasdf", Cf.set("short_url")));
    }

    @Test
    public void getAsyncImportFileFromServiceUrl() {
        Assert.equals(
                "https://" + mpfsHost + "/json/import_file_from_service?uid=123&service_file_id=123%3A4321%2Fmds_key&" +
                        "file_name=document.docx&service_id=browser",
        mpfsUrlHelper.getAsyncImportFileFromServiceUrl(PassportUidOrZero.fromUid(123), "browser",
                "123:4321/mds_key", "document.docx"));
    }
}
