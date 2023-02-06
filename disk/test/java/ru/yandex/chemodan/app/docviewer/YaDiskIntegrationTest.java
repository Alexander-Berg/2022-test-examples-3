package ru.yandex.chemodan.app.docviewer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.copy.StoredUriManager;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUri;
import ru.yandex.misc.test.Assert;

/**
 * @see DOCVIEWER-200
 */
public class YaDiskIntegrationTest extends DocviewerSpringTestBase {

    // Files of production user @see TestUser.PROD
    public static final String TEST_URI_DOC = "ya-disk:///disk/file.doc";
    public static final String TEST_URI_PDF = "ya-disk:///disk/file.pdf";
    public static final String TEST_URI_PUBLIC = "ya-disk-public://iCcC1dRUMpXu9CkNsHoXojy5P45EhYaG+mY6SWbV3Bk=";

    public static final String TEST_URI_PUBLIC_NDA = "ya-disk-public://hj0wQdr4M5cD/UV7Oho6HMa1/pR4xOQ8AciOBXW05HA=";
    public static final String SAVED_NDA_FILE_NAME = "file1.pdf";

    public static final String TEST_URI_ARCHIVE = "ya-disk:///disk/test_archive.zip";
    public static final String PATH_IN_ARCHIVE_1 = "//test_archive/file2.txt";
    public static final String PATH_IN_ARCHIVE_2 = "test_archive/folder1/file3.txt";
    public static final String EXTRACTED_FILE_NAME = "file_from_archive.txt";

    public static final String TEST_URI_PUBLIC_NDA_ARCHIVE = "ya-disk-public://pkJ+//mEyJL09oSqMCMZRBMxuE190AZe3MrPQKx+/IU=";
    public static final String PATH_IN_NDA_ARCHIVE = "file2.pdf";
    public static final String EXTRACTED_NDA_FILE_NAME = "file_from_archive.pdf";

    @Autowired
    private StoredUriManager storedUriManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private Copier copier;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);
    }

    private void test(String url) {
        testManager.makeAvailable(TestUser.PROD.uid, url, TargetType.HTML_WITH_IMAGES);
    }

    private void testXTargetContentType(String url, String mimeType) {
        String fileId = testManager.makeAvailable(TestUser.PROD.uid, url, TargetType.PLAIN_TEXT);

        Option<StoredUri> option = storedUriManager.findByFileIdAndUidO(fileId, TestUser.PROD.uid);
        Assert.A.some(option);

        StoredUri storedUri = option.get();
        Assert.A.equals(mimeType, storedUri.getContentType());
    }

    @Test
    @Ignore("Long test not for CI")
    public void testDoc() {
        test(TEST_URI_DOC);
    }

    @Test
    @Ignore("Long test not for CI")
    public void testPdf() {
        test(TEST_URI_PDF);
    }

    @Test
    @Ignore("Long test not for CI")
    public void testPublic() {
        test(TEST_URI_PUBLIC);
    }

    @Test
    @Ignore("Long test not for CI")
    public void testContentTypeDoc() {
        testXTargetContentType(TEST_URI_DOC, MimeTypes.MIME_MICROSOFT_WORD);
    }

    @Test
    @Ignore("Long test not for CI")
    public void testContentTypePdf() {
        testXTargetContentType(TEST_URI_PDF, MimeTypes.MIME_PDF);
    }


}
