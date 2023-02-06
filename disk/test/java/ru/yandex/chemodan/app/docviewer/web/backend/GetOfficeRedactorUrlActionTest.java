package ru.yandex.chemodan.app.docviewer.web.backend;

import org.apache.http.client.methods.HttpGet;
import org.dom4j.Element;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.BrowserIntegrationTest;
import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.YaDiskIntegrationTest;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.states.StateMachine;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author akirakozov
 */
public class GetOfficeRedactorUrlActionTest extends DocviewerWebSpringTestBase {
    private static final String GET_OFFICE_REDACTOR_URL = "http://localhost:32405/get-office-redactor-url";
    private static final String NOT_SUPPORTED = "not-supported";

    @Autowired
    private Copier copier;
    @Autowired
    private StateMachine stateMachine;

    @Test
    public void getOfficeRedactorUrlForSupportedFile() {
        String response = getOfficeRedactorUrlResponse(YaDiskIntegrationTest.TEST_URI_DOC, TestUser.PROD.uid);
        Element root = Dom4jUtils.read(response.getBytes()).getRootElement();
        Assert.equals(root.getName(), "office-online-url");
        Assert.equals("/disk/Загрузки/", root.attribute("downloads").getText());
        Assert.notEmpty(root.getText());
    }

    @Test
    public void getOfficeRedactorUrlForNotSupportedFile() {
        String response = getOfficeRedactorUrlResponse(YaDiskIntegrationTest.TEST_URI_PDF, TestUser.PROD.uid);
        Element root = Dom4jUtils.read(response.getBytes()).getRootElement();
        Assert.equals(root.getName(), NOT_SUPPORTED);
    }

    @Test
    public void getOfficeRedactorUrlForYaTeamUser() {
        String response = getOfficeRedactorUrlResponse(
                YaDiskIntegrationTest.TEST_URI_DOC,
                PassportUidOrZero.fromUid(1120000000000144L));
        Element root = Dom4jUtils.read(response.getBytes()).getRootElement();
        Assert.equals(root.getName(), NOT_SUPPORTED);
    }

    @Test
    public void getOfficeRedactorUrlForBrowserMdsFile() {
        getOfficeRedactorUrlForBrowser(TestUser.TEST.uid);
    }

    @Test
    public void getOfficeRedactorUrlForBrowserMdsFileAndAnonymous() {
        getOfficeRedactorUrlForBrowser(PassportUidOrZero.zero());
    }

    private void getOfficeRedactorUrlForBrowser(PassportUidOrZero uid) {
        String response = getOfficeRedactorUrlResponse(
                BrowserIntegrationTest.YA_BROWSER_TEST_XLS_URL, uid, Option.of("123123"));
        Element root = Dom4jUtils.read(response.getBytes()).getRootElement();
        Assert.equals(root.getName(), "office-online-url");
        Assert.equals("/disk/Загрузки/", root.attribute("downloads").getText());
        Assert.assertContains(root.getText(), "/edit/browser/");
    }

    private String getOfficeRedactorUrlResponse(String url, PassportUidOrZero uid) {
        return getOfficeRedactorUrlResponse(url, uid, Option.empty());
    }

    private String getOfficeRedactorUrlResponse(String url, PassportUidOrZero uid, Option<String> yandexuid) {
        DocumentSourceInfo source = DocumentSourceInfo.builder().originalUrl(url).uid(uid).build();
        copier.setEnableNativeUrlFetching(false);

        stateMachine.onStart(source, "stage listener",
                Option.empty(), TargetType.HTML_WITH_IMAGES, "", Cf.list(), Instant.now(), false);

        String fileName = StringUtils.substringAfter(url, "/");
        String dvUrl = UrlUtils.addParameter(
                GET_OFFICE_REDACTOR_URL, "uid", uid.getUid(), "url", url, "name", fileName);
        if (yandexuid.isPresent()) {
            dvUrl = UrlUtils.addParameter(dvUrl, "yandexuid", yandexuid.get());
        }
        return ApacheHttpClientUtils.executeReadString(new HttpGet(dvUrl), Timeout.seconds(3));
    }

}
