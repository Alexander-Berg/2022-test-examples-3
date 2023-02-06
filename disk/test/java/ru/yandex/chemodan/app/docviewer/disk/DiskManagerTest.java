package ru.yandex.chemodan.app.docviewer.disk;

import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.copy.provider.SerpUrlProvider;
import ru.yandex.chemodan.app.docviewer.disk.mpfs.MpfsOperationStatus;
import ru.yandex.chemodan.app.docviewer.disk.mpfs.MpfsOperationStatusWithMeta;
import ru.yandex.chemodan.app.docviewer.disk.mpfs.MpfsUrlHelper;
import ru.yandex.chemodan.app.docviewer.disk.resource.DiskPrivateFileId;
import ru.yandex.chemodan.app.docviewer.disk.resource.DiskPublicFileId;
import ru.yandex.chemodan.app.docviewer.disk.resource.MailAttachmentId2;
import ru.yandex.chemodan.app.docviewer.disk.resource.WebDocumentId;
import ru.yandex.chemodan.app.docviewer.disk.resource.YaBroResourceId;
import ru.yandex.chemodan.app.docviewer.utils.httpclient.MpfsHttpClient;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.lang.Validate;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author akirakozov
 */
public class DiskManagerTest extends DocviewerSpringTestBase {
    private static final Logger logger = LoggerFactory.getLogger(DiskManagerTest.class);

    private static final String PUBLIC_FILE_HASH = "iCcC1dRUMpXu9CkNsHoXojy5P45EhYaG+mY6SWbV3Bk=";
    // Mail attachment of user TestUser.TEST
    private static final MailAttachmentId2 PDF_ATTACHMENT_ID = new MailAttachmentId2("154248287237439490", "1.2");

    @Autowired
    private DiskManager diskManager;
    @Autowired
    private MpfsUrlHelper mpfsUrlHelper;
    @Autowired
    private MpfsHttpClient mpfsHttpClient;

    @Value("${serp.download.url}")
    private String serpDownloadUrl;

    @Test
    public void findPublicFileId() {
        String hash = "private-hash-xxxx";
        DocumentSourceInfo source = DocumentSourceInfo.builder().originalUrl("ya-disk-public://" + hash).build();
        String url = mpfsUrlHelper.getPublicDirectUrl(hash);
        DiskPublicFileId id = (DiskPublicFileId) diskManager.getDiskResourceId(source, new ActualUri(url)).get();
        Assert.equals(hash, id.getPrivateHash());
    }

    @Test
    public void findPrivateFileId() {
        DocumentSourceInfo source =
                DocumentSourceInfo.builder().originalUrl(YaDiskIntegrationTest.TEST_URI_PDF).uid(TestUser.PROD.uid)
                        .build();
        String path = StringUtils.substringAfter(YaDiskIntegrationTest.TEST_URI_PDF, "://");
        String url = mpfsUrlHelper.getUrl(TestUser.PROD.uid, path);
        DiskPrivateFileId id = (DiskPrivateFileId) diskManager.getDiskResourceId(source, new ActualUri(url)).get();
        Assert.equals(path, id.getPath());
    }

    @Test
    public void findWebDocumentId() {
        SerpUrlProvider provider = new SerpUrlProvider(serpDownloadUrl, false);

        DocumentSourceInfo source = DocumentSourceInfo.builder().originalUrl("ya-serp://some.example.ru/a.pdf").build();
        String url = provider.rewriteUrl(source);
        WebDocumentId webDocumentId = (WebDocumentId) diskManager.getDiskResourceId(source, new ActualUri(url)).get();

        Assert.equals(
                "https://" + mpfsUrlHelper.getMpfsHost() +
                        "/json/async_store_external?uid=4000475747&path=%2Fdisk%2F%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8%2Fa.pdf&external_url=http%3A%2F%2Fsome.example.ru%2Fa.pdf",
                webDocumentId.getCopyToDiskUrl(mpfsUrlHelper, TestUser.TEST.uid, Option.empty(), false));
    }

    @Test
    public void getPublicFileShortUrl() {
        Assert.equals(
                "https://yadi.sk/i/GikQMsWr3PA3k8",
                diskManager.getPublicFileShortUrl(TestUser.PROD.uid, new DiskPublicFileId(PUBLIC_FILE_HASH)));
    }

    @Test
    public void getPublishedPrivateFileShortUrl() {
        String path = StringUtils.substringAfter(YaDiskIntegrationTest.TEST_URI_PDF, "://");
        Assert.equals(
                "https://front.tst.clk.yandex.net/i/iqHq0LNfRez6",
                diskManager.getFileShortUrl(TestUser.PROD.uid, path));
    }

    @Test
    public void setPublicAndGetShortUrl() {
        String path = StringUtils.substringAfter(YaDiskIntegrationTest.TEST_URI_PDF, "://");
        Assert.equals(
                "https://front.tst.clk.yandex.net/i/iqHq0LNfRez6",
                diskManager.setPublicAndGetShortUrl(TestUser.PROD.uid, new DiskPrivateFileId(path)));
    }

    @Test
    public void setPublicNotExistsFile() {
        String path = "//disk/nofile.doc";
        try {
            diskManager.setPublicAndGetShortUrl(TestUser.PROD.uid, new DiskPrivateFileId(path));
            Assert.fail();
        } catch (HttpException ex) {
            Assert.some(HttpStatus.SC_404_NOT_FOUND, ex.getStatusCode());
        }
    }

    @Test
    public void extractAndSaveFileFromArchive() {
        try {
            removeResource(TestUser.TEST.uid, "/disk/Загрузки/" + YaDiskIntegrationTest.EXTRACTED_FILE_NAME);
        } catch (HttpException e) {
            logger.info(e.getMessage(), e);
        }

        DocumentSourceInfo documentSourceInfo = DocumentSourceInfo.builder()
                .originalUrl(YaDiskIntegrationTest.TEST_URI_ARCHIVE)
                .uid(TestUser.TEST.uid)
                .archivePath(Option.of(YaDiskIntegrationTest.PATH_IN_ARCHIVE_2))
                .build();

        String path = StringUtils.substringAfter(YaDiskIntegrationTest.TEST_URI_ARCHIVE, "//");

        Option<String> dstFilename = Option.of(YaDiskIntegrationTest.EXTRACTED_FILE_NAME);

        String oid = diskManager.extractAndSaveFileFromArchiveToDiskAndGetOperationId(
                TestUser.TEST.uid, documentSourceInfo, dstFilename);

        checkMpfsOperationStatus(TestUser.TEST.uid, oid);
    }

    @Test
    @Ignore
    public void saveMailAttachment() {
        asyncCopy(PDF_ATTACHMENT_ID);
    }

    @Test
    @Ignore
    public void shareMailAttachment() {
        asyncCopyAndShare(PDF_ATTACHMENT_ID);
    }

    @Test
    public void checkOfficeActionForDiskFile() {
        String path = StringUtils.substringAfter(TestUser.DOCX_FILE_URI, "://");
        String fileName = StringUtils.substringAfterLast(TestUser.DOCX_FILE_URI, "/");
        String url = diskManager.getOfficeRedactorUrl(
                TestUser.TEST.uid, new DiskPrivateFileId(path), Option.of(fileName), Option.empty(), Option.empty())
                .get().officeOnlineUrl;
        Assert.notEmpty(url);
    }

    @Test
    public void checkOfficeActionForMailAttachment() {
        String url = diskManager.getOfficeRedactorUrl(
                TestUser.TEST.uid, PDF_ATTACHMENT_ID, Option.of("file.docx"), Option.of(DataSize.MEGABYTE),
                Option.empty()).get()
                .officeOnlineUrl;
        String path = StringUtils.substringAfter(url, UrlUtils.extractHost(url));
        path = StringUtils.substringBefore(path, "?");
        Assert.equals("/edit/mail/154248287237439490%2F1.2", path);
    }

    @Test
    public void checkOfficeActionForWebDocument() {
        WebDocumentId id = new WebDocumentId("http://some.host.ru/file.doc", mpfsHttpClient);
        String fileName = "file.docx";
        String url = diskManager.getOfficeRedactorUrl(
                TestUser.TEST.uid, id, Option.of(fileName), Option.of(DataSize.MEGABYTE), Option.empty()).get()
                .officeOnlineUrl;
        String path = StringUtils.substringAfter(url, UrlUtils.extractHost(url));
        Assert.isTrue(StringUtils.startsWith(path, "/edit/web/"));
    }

    @Test
    public void checkOfficeActionForDiskPublicFile() {
        DiskPublicFileId id = new DiskPublicFileId(PUBLIC_FILE_HASH);
        String fileName = "file.docx";
        String url = diskManager.getOfficeRedactorUrl(
                TestUser.TEST.uid, id, Option.of(fileName), Option.of(DataSize.MEGABYTE), Option.empty()).get()
                .officeOnlineUrl;
        String path = StringUtils.substringAfter(url, UrlUtils.extractHost(url));
        path = StringUtils.substringBefore(path, "?");
        Assert.equals("/edit/public/" + UrlUtils.urlEncode(PUBLIC_FILE_HASH), path);
    }

    @Test
    public void checkOfficeActionForBrowserMdsFile() {
        YaBroResourceId id = new YaBroResourceId(MdsFileKey.parse("5070/testxls.xls"));
        String fileName = "testxls.xls";
        String url = diskManager.getOfficeRedactorUrl(
                TestUser.TEST.uid, id, Option.of(fileName), Option.of(DataSize.MEGABYTE), Option.of("12321")).get()
                .officeOnlineUrl;
        Assert.assertContains(url, "/edit/browser/");
    }

    private void asyncCopy(MailAttachmentId2 attachmentId) {
        String oid = asyncCopyAndGetOperationId(attachmentId, false);
        checkMpfsOperationStatus(TestUser.TEST.uid, oid);
    }

    private void asyncCopyAndShare(MailAttachmentId2 attachmentId) {
        String oid = asyncCopyAndGetOperationId(attachmentId, true);
        while (true) {
            AsyncCopyStatus status = diskManager.getAsyncCopyStatus(TestUser.TEST.uid, oid);
            if (status.status == MpfsOperationStatus.DONE) {
                Validate.some(status.shortUrl);
                Assert.isTrue(status.shortUrl.get().startsWith("https://yadi.sk/"));
                break;
            } else if (status.status == MpfsOperationStatus.EXECUTING || status.status == MpfsOperationStatus.WAITING) {
                ThreadUtils.sleep(100);
            } else {
                Assert.fail("Invalid status of operation: " + status.status);
            }
        }
    }

    private String asyncCopyAndGetOperationId(MailAttachmentId2 attachmentId, boolean publish) {
        return diskManager.asyncCopyToDiskAndGetOperationId(
                TestUser.TEST.uid, attachmentId, Option.of("new.pdf"), publish, false);
    }

    private void checkMpfsOperationStatus(PassportUidOrZero uid, String oid) {
        while (true) {
            MpfsOperationStatusWithMeta status = diskManager.getMpfsOperationStatusWithMeta(uid, oid, Option.empty());
            if (status.status == MpfsOperationStatus.DONE) {
                break;
            } else if (status.status == MpfsOperationStatus.EXECUTING || status.status == MpfsOperationStatus.WAITING) {
                ThreadUtils.sleep(100);
            } else {
                Assert.fail("Invalid status of operation: " + status.status);
            }
        }
    }

}
