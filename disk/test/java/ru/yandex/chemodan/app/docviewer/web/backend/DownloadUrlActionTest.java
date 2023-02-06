package ru.yandex.chemodan.app.docviewer.web.backend;

import java.io.StringReader;

import org.apache.http.client.methods.HttpGet;
import org.dom4j.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 */
public class DownloadUrlActionTest extends DocviewerWebSpringTestBase {

    private static final String DOWNLOAD_URL = "http://localhost:32405/download-url";

    @Autowired
    private TestManager testManager;

    @Autowired
    private Copier copier;

    @Test
    public void getDownloadUrlForPrivateYaDiskFile() {
        makeAvailableAndGetDownloadUrl(YaDiskIntegrationTest.TEST_URI_PDF, TestUser.PROD.uid);
    }

    @Test
    public void getDownloadUrlForPublicYaDiskFile() {
        makeAvailableAndGetDownloadUrl(YaDiskIntegrationTest.TEST_URI_PUBLIC, PassportUidOrZero.zero());
    }

    @Test
    public void getDownloadUrlForYaMailFile() {
        testManager.withUploadedToMulcaFile(TestResources.EML_WITH_PDF_ATTACH, true, a -> {
            MulcaId attachmentId = MulcaId.valueOf(a.getStidCheckNoPart(), "1.2");
            try {
                makeAvailableAndGetDownloadUrl(attachmentId.asMulcaUri().toString(), PassportUidOrZero.zero());
            } catch (Exception e) {
                ExceptionUtils.translate(e);
            }
        });
    }

    @Test
    @Ignore("Not stable")
    public void shouldAddUploadUrlUsingExtUrl() {
        String downloadUrl = makeAvailableAndGetDownloadUrl(
                YaDiskIntegrationTest.TEST_URI_PDF, TestUser.PROD.uid,
                Cf.<String, Object>map(
                        "uid", TestUser.PROD.uid.toString(),
                        "url", YaDiskIntegrationTest.TEST_URI_PDF));
        Assert.notNull(downloadUrl);
    }

    @Test
    public void shouldUseInlineContentDispInExtUrl() {
        String downloadUrl = makeAvailableAndGetDownloadUrl(
                YaDiskIntegrationTest.TEST_URI_PDF, TestUser.PROD.uid,
                Cf.<String, Object>map("uid", TestUser.PROD.uid.toString())
                        .plus1("url", YaDiskIntegrationTest.TEST_URI_PDF)
                        .plus1("disposition", "inline"));
        Assert.assertContains(downloadUrl, "disposition=inline");
    }

    @Test
    public void getNdaUrl() {
        PassportUidOrZero uid = TestUser.TEST_WITH_NDA.uid;
        copier.setEnableNativeUrlFetching(false);
        String fileId = testManager.makeAvailableWithShowNda(
                uid, YaDiskIntegrationTest.TEST_URI_PUBLIC_NDA, TargetType.PDF);

        MapF<String, Object> params = Cf.map("id", fileId, "uid", uid, "show_nda", 1);

        getDownloadUrl(DOWNLOAD_URL, params);
    }

    private void makeAvailableAndGetDownloadUrl(String url, PassportUidOrZero uid) {
        copier.setEnableNativeUrlFetching(false);
        String fileId = testManager.makeAvailable(uid, url, TargetType.HTML_WITH_IMAGES);

        String downloadUrl = getDownloadUrl(DOWNLOAD_URL, Cf.map("id", fileId, "uid", uid));
        if (downloadUrl.contains("/source")) {
            Assert.equals(fileId, UrlUtils.getQueryParameterFromUrl(downloadUrl, "id").get());
        }
    }

    private String makeAvailableAndGetDownloadUrl(
            String url, PassportUidOrZero uid, MapF<String, Object> params) {
        copier.setEnableNativeUrlFetching(false);
        testManager.makeAvailable(uid, url, TargetType.HTML_WITH_IMAGES);

        return getDownloadUrl(DOWNLOAD_URL, params);
    }

    private String getDownloadUrl(String downloadUrlBase, MapF<String, Object> params) {
        String urlWithParams = UrlUtils.addParameters(downloadUrlBase, params);

        HttpGet httpGet = new HttpGet(urlWithParams);
        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));

        Element downloadUrl = Dom4jUtils.readRootElement(new StringReader(response));

        Assert.equals("download-url", downloadUrl.getName());
        return downloadUrl.getText();
    }
}
