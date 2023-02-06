package ru.yandex.chemodan.app.docviewer.web.backend;

import java.io.StringReader;

import org.apache.http.client.methods.HttpGet;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author metal
 */
public class SaveFileFromArchiveActionTest extends DocviewerWebSpringTestBase {
    private static final Logger logger = LoggerFactory.getLogger(SaveFileFromArchiveActionTest.class);

    @Autowired
    private TestManager testManager;

    @Autowired
    private Copier copier;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);

        removeFromDownloadsSafe(TestUser.TEST.uid, YaDiskIntegrationTest.EXTRACTED_FILE_NAME);
        removeFromDownloadsSafe(TestUser.TEST_WITH_NDA.uid, YaDiskIntegrationTest.EXTRACTED_NDA_FILE_NAME);
    }

    private void removeFromDownloadsSafe(PassportUidOrZero uid, String filename) {
        try {
            removeResource(uid, "/disk/Загрузки/" + filename);
        } catch (HttpException e) {
            logger.info(e.getMessage(), e);
        }
    }

    @Test
    public void saveToDiskPublicFile() {
        testManager.makeAvailable(TestUser.TEST.uid, YaDiskIntegrationTest.TEST_URI_ARCHIVE,
                TargetType.HTML_WITH_IMAGES);

        String url = UrlUtils.addParameter("http://localhost:32405/save-file-from-archive",
                "uid", TestUser.TEST.uid.getUid());
        url = UrlUtils.addParameter(url,
                "url", YaDiskIntegrationTest.TEST_URI_ARCHIVE,
                "archive-path", YaDiskIntegrationTest.PATH_IN_ARCHIVE_1,
                "name", YaDiskIntegrationTest.EXTRACTED_FILE_NAME);

        String response = ApacheHttpClient4Utils.executeReadString(new HttpGet(url), Timeout.seconds(30));
        Element root = Dom4jUtils.readRootElement(new StringReader(response));

        Assert.equals("ok", root.getName());

        Assert.equals(2, root.elements().size());

        Element downloads = (Element) root.elements().get(1);
        Assert.equals("downloads", downloads.getName());
        Assert.equals("/disk/Загрузки/", downloads.getText());
    }

    @Test
    public void saveToDiskPublicNdaFile() {
        testManager.makeAvailableWithShowNda(TestUser.TEST_WITH_NDA.uid,
                YaDiskIntegrationTest.TEST_URI_PUBLIC_NDA_ARCHIVE, TargetType.HTML_WITH_IMAGES);

        String url = UrlUtils.addParameter("http://localhost:32405/save-file-from-archive",
                "uid", TestUser.TEST_WITH_NDA.uid.getUid());
        url = UrlUtils.addParameters(url, Tuple2List.fromPairs(
                "url", YaDiskIntegrationTest.TEST_URI_PUBLIC_NDA_ARCHIVE,
                "archive-path", YaDiskIntegrationTest.PATH_IN_NDA_ARCHIVE,
                "name", YaDiskIntegrationTest.EXTRACTED_NDA_FILE_NAME,
                "show_nda", 1));

        String response = ApacheHttpClientUtils.executeReadString(new HttpGet(url), Timeout.seconds(30));
        Assert.equals("ok", Dom4jUtils.readRootElement(new StringReader(response)).getName());
    }


}
