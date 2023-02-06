package ru.yandex.chemodan.app.docviewer.web.backend;

import java.text.MessageFormat;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.cleanup.CleanupManager;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.copy.UriHelper;
import ru.yandex.chemodan.app.docviewer.states.State;
import ru.yandex.chemodan.app.docviewer.states.StateMachine;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.test.Assert;

public class Url2IdActionTest extends DocviewerWebSpringTestBase {

    private static final String FIELD_ID =
        "9hc-an2gjny6dx5ctizd7rkvnwxokkaljkt12r07gbnt461j1arb2o3fx8pu1p40hu5mrb3gy0bqhi8hr0lb8rwpac5k6xlenkxu9ek";

    @Autowired
    private MulcaClient mulcaClient;

    @Autowired
    private StateMachine stateMachine;

    @Autowired
    private UriHelper uriHelper;

    @Autowired
    private CleanupManager cleanupManager;

    @Autowired
    private Copier copier;

    @Test
    public void test() {
        HttpGet httpGet = new HttpGet(MessageFormat.format(
                "http://localhost:32405{0}?uid=0&type={1}&url={2}", Url2IdAction.PATH,
                TargetType.PLAIN_TEXT.name(),
                UrlUtils.urlEncode(UriUtils.toUrlString(TestResources.Microsoft_Word_97_001p))));

        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));
        Assert.equals(FIELD_ID, response);
    }

    @Test
    public void testComplete() {
        cleanupManager.cleanupByActualUri(new ActualUri(TestResources.Microsoft_Word_97_001p));

        final String originalUrl = UriUtils.toUrlString(TestResources.Microsoft_Word_97_001p);
        HttpGet httpGet = new HttpGet(MessageFormat.format(
                "http://localhost:32405{0}?uid=0&type={1}&url={2}&complete=true",
                Url2IdAction.PATH, TargetType.PLAIN_TEXT.name(), UrlUtils.urlEncode(originalUrl)));

        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));
        Assert.equals(FIELD_ID, response);
        Assert.equals(State.AVAILABLE, stateMachine.getState(
                uriHelper.rewriteForTests(originalUrl, PassportUidOrZero.zero()),
                TargetType.PLAIN_TEXT));
    }

    @Test
    public void testMulca() {
        String mulcaId = mulcaClient.upload(File2.fromFileUrl(
                TestResources.Microsoft_Word_97_001p), "docviewer_test").toSerializedString();

        HttpGet httpGet = new HttpGet(MessageFormat.format(
                "http://localhost:32405{0}?uid=0&type={1}&unsafe=true&url={2}", Url2IdAction.PATH,
                TargetType.PLAIN_TEXT.name(), UrlUtils.urlEncode("mulca://" + mulcaId)));

        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));
        Assert.equals(FIELD_ID, response);
    }

    @Test
    public void mulcaDoubleCall() {
        copier.setEnableNativeUrlFetching(false);

        String mulcaId = mulcaClient.upload(File2.fromFileUrl(
                TestResources.Microsoft_Word_97_001p), "docviewer_test").toSerializedString();

        HttpGet httpGet = new HttpGet(MessageFormat.format(
                "http://localhost:32405{0}?uid=0&type={1}&unsafe=true&url={2}", Url2IdAction.PATH,
                TargetType.PLAIN_TEXT.name(), UrlUtils.urlEncode("mulca://" + mulcaId)));

        String response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));
        Assert.equals(FIELD_ID, response);

        response = ApacheHttpClient4Utils.executeReadString(httpGet, Timeout.seconds(30));
        Assert.equals(FIELD_ID, response);
    }

    @Test
    public void testUnsafe() {
        uriHelper.setDisableOriginalUrlCheck(false);
        String originalUrl = UrlUtils.urlEncode(UriUtils.toUrlString(
                TestResources.Microsoft_Word_97_001p));
        try {
            HttpGet method = new HttpGet(MessageFormat.format(
                    "http://localhost:32405{0}?uid=0&type={1}&url={2}",
                    Url2IdAction.PATH, TargetType.PLAIN_TEXT.name(), originalUrl));
            ApacheHttpClient4Utils.executeReadString(method, Timeout.seconds(120));
            Assert.fail("403 error is expected");
        } catch (HttpException exc) {
            Assert.assertTrue(exc.statusCodeIs(403));
        }

        HttpGet method = new HttpGet(MessageFormat.format(
                "http://localhost:32405{0}?uid=0&type={1}&unsafe=true&url={2}",
                Url2IdAction.PATH, TargetType.PLAIN_TEXT.name(), originalUrl));
        String response = ApacheHttpClient4Utils
                .executeReadString(method, Timeout.seconds(120));
        Assert.equals(FIELD_ID, response);
    }
}
