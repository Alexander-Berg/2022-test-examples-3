package ru.yandex.chemodan.app.docviewer.web.backend;

import java.io.StringReader;

import org.apache.http.client.methods.HttpGet;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.BrowserIntegrationTest;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.config.DocviewerActiveProfiles;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 */
@ActiveProfiles(DocviewerActiveProfiles.UNISTORAGE)
public class SaveToDiskActionTest extends DocviewerWebSpringTestBase {
    private static final Logger logger = LoggerFactory.getLogger(SaveFileFromArchiveActionTest.class);

    @Autowired
    private TestManager testManager;

    @Autowired
    private Copier copier;

    @Override
    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);
    }

    @Test
    public void saveToDiskPublicFileByUrl() {
        saveToDiskInternal(TestUser.TEST.uid, "file.doc", YaDiskIntegrationTest.TEST_URI_PUBLIC, false);
    }

    @Test
    public void saveToDiskPublicNdaFileByUrl() {
        saveToDiskInternal(TestUser.TEST_WITH_NDA.uid, YaDiskIntegrationTest.SAVED_NDA_FILE_NAME,
                YaDiskIntegrationTest.TEST_URI_PUBLIC_NDA, true);
    }

    @Test
    @Ignore("Depends on remote resource")
    public void saveToDiskFileFromBrowserMds() {
        saveToDiskInternal(TestUser.TEST.uid, "name.xls",
                BrowserIntegrationTest.YA_BROWSER_TEST_XLS_URL, false);
    }

    private void saveToDiskInternal(PassportUidOrZero uid, String fileName, String sourceUrl, boolean showNda) {
        removeResourceSafely(uid, fileName);

        testManager.makeAvailable(uid, sourceUrl, Option.empty(), TargetType.HTML_WITH_IMAGES, showNda);

        String response = saveToDisk(uid, fileName, sourceUrl, showNda);
        Element root = Dom4jUtils.readRootElement(new StringReader(response));

        Assert.equals("ok", root.getName());

        Element downloads = root.element("downloads");
        Assert.equals("downloads", downloads.getName());
        Assert.equals("/disk/Загрузки/", downloads.getText());
    }

    private String saveToDisk(PassportUidOrZero uid, String fileName, String sourceUrl, boolean showNda) {
        String url = UrlUtils.addParameter("http://localhost:32405/save-to-disk",
                "uid", uid.getUid(),
                "url", sourceUrl,
                "name", fileName);
        if (showNda) {
            url = UrlUtils.addParameter(url, "show_nda", 1);
        }
        return ApacheHttpClientUtils.executeReadString(new HttpGet(url), Timeout.seconds(30));
    }

    private void removeResourceSafely(PassportUidOrZero uid, String fileName) {
        try {
            removeResource(uid, "/disk/Загрузки/" + fileName);
        } catch (HttpException e) {
            logger.info(e.getMessage(), e);
        }
    }

}
