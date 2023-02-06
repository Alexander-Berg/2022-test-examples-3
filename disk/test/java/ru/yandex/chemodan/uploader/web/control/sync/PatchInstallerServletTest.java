package ru.yandex.chemodan.uploader.web.control.sync;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.glassfish.grizzly.http.Method;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.chemodan.test.MulcaTestManager;
import ru.yandex.chemodan.uploader.installer.InstallerModifierTest;
import ru.yandex.chemodan.uploader.web.AbstractWebTestSupport;
import ru.yandex.chemodan.uploader.web.ApiUrls;
import ru.yandex.chemodan.util.http.YandexUidCookieParser;
import ru.yandex.chemodan.util.oauth.OauthClient;
import ru.yandex.chemodan.util.test.JsonTestUtils;
import ru.yandex.chemodan.util.test.StubServerUtils;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.method;

/**
 * @author akirakozov
 */
public class PatchInstallerServletTest extends AbstractWebTestSupport {
    public static final String YUID_TEST_VALUE = "123456789012345678";
    public static final String SOURCE_TEST_VALUE = "Yandex.Greeting.SaveTime";
    private static String OAUTH_RESPONSE = "{\"status\": \"ok\", \"code\": \"secret-key\", \"expires_in\": 600}";

    @Autowired
    private MulcaClient mulcaClient;
    @Value("${oauth.host}")
    private String oauthHost;

    @Test
    public void patchInstaller() {

        int port = URI.create("http://" + oauthHost).getPort();
        StubServerUtils.withStubServer(port, stubServer -> {
            whenHttp(stubServer).match(method(Method.POST)).then(stringContent(OAUTH_RESPONSE));

            checkPatchedInstallerWithCookie();
            checkPatchedInstallerWithCookieHeader();
        });
    }

    private void checkPatchedInstallerWithCookie() {
        MulcaTestManager testManager = new MulcaTestManager(mulcaClient);
        ByteArrayInputStreamSource source = new ByteArrayInputStreamSource(
                InstallerModifierTest.prepareInstallerData());

        testManager.withUploadedToMulcaFile(source, false, mulcaId -> {
            String url = UrlUtils.addParameter(
                    "http://localhost:" + uploaderHttpPorts.getControlPort() + ApiUrls.PATCH_INSTALLER,
                    "mulca-id", mulcaId.toString(),
                    "src", SOURCE_TEST_VALUE,
                    "open_url_after_install", "yadisk://test.download");
            HttpGet get = new HttpGet(url);
            get.setHeader(OauthClient.YA_CONSUMER_CLIENT_IP_HEADER, "127.0.0.1");

            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(YandexUidCookieParser.YANDEXUID_COOKIE_NAME, YUID_TEST_VALUE);
            cookie.setPath("/");
            cookie.setExpiryDate(new Date(System.currentTimeMillis() + 24*60*60*1000));
            cookie.setDomain("localhost");
            cookieStore.addCookie(cookie);
            CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();

            byte[] result = ApacheHttpClientUtils.execute(get, httpClient, new ReadBytesResponseHandler());

            InstallerModifierTest.checkResult(result);

            byte[] jsonKey = InstallerModifierTest.extractJsonKey(result);

            Map<String, Object> modifications = JsonTestUtils.parseJsonToMap(jsonKey);

            Assert.equals(YUID_TEST_VALUE, modifications.get("yuid"),
                    "YUID must be passed from cookie storage unaltered.");
            Assert.equals(SOURCE_TEST_VALUE, modifications.get("src"),
                    "Source must be passed from request params unaltered.");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            String expectedDate = simpleDateFormat.format(new Date());
            Assert.equals(expectedDate, modifications.get("date"),
                    "Date must be today's date in format yyyy-MM-dd");
        });
    }

    private void checkPatchedInstallerWithCookieHeader() {
        MulcaTestManager testManager = new MulcaTestManager(mulcaClient);
        ByteArrayInputStreamSource source = new ByteArrayInputStreamSource(
                InstallerModifierTest.prepareInstallerData());

        testManager.withUploadedToMulcaFile(source, false, mulcaId -> {
            String url = UrlUtils.addParameter(
                    "http://localhost:" + uploaderHttpPorts.getControlPort() + ApiUrls.PATCH_INSTALLER,
                    "mulca-id", mulcaId.toString(),
                    "open_url_after_install", "yadisk://test.download");
            HttpGet get = new HttpGet(url);
            get.setHeader(OauthClient.YA_CONSUMER_CLIENT_IP_HEADER, "127.0.0.1");
            get.setHeader(OauthClient.YA_CLIENT_COOKIE_HEADER, "Session_id=FakeSessionId; sessionid2=FakeSession2; "
                    + YandexUidCookieParser.YANDEXUID_COOKIE_NAME + "=" + YUID_TEST_VALUE);

            byte[] result = ApacheHttpClientUtils.execute(get, new ReadBytesResponseHandler());

            InstallerModifierTest.checkResult(result);

            byte[] jsonKey = InstallerModifierTest.extractJsonKey(result);

            Map<String, Object> modifications = JsonTestUtils.parseJsonToMap(jsonKey);

            Assert.equals(YUID_TEST_VALUE, modifications.get("yuid"),
                    "YUID must be passed from cookie storage unaltered.");
            Assert.equals("Yandex.Unknown", modifications.get("src"),
                    "Source must be passed from request params unaltered.");

            String expectedDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Assert.equals(expectedDate, modifications.get("date"),
                    "Date must be today's date in format yyyy-MM-dd");
        });
    }

}
