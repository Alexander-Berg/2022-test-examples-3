package ru.yandex.downloader.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.downloader.TestData;
import ru.yandex.downloader.mulca.MulcaUtils;
import ru.yandex.downloader.url.BaseUrlParams;
import ru.yandex.downloader.url.Disposition;
import ru.yandex.downloader.url.MulcaTargetId;
import ru.yandex.downloader.url.UrlCreator;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.mulca.MulcaTestManager;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author akirakozov
 */
public class DownloadInstallerTest extends DownloaderTestBase {
    private static final byte[] START_MARKER = "B94FA2E208DD4e97979604809696358E".getBytes();
    private static final byte[] END_MARKER = "DEC70B359B524b5cA98CDA849B5A0788".getBytes();
    private static final int ZERO_COUNT = 1024;

    private static final String SESSION_ID_COOKIE = "";

    private static final String YANDEX_UID_COOKIE = "123456789012345678";
    private static final ObjectMapper PARSER = new ObjectMapper();

    private static final MulcaTestManager mulcaManager = MulcaUtils.createMulcaTestManager();

    @Test
    public void downloadPatchedInstaller() throws IOException {
        Assert.notEmpty(SESSION_ID_COOKIE, "Fill session id with valid value");

        mulcaManager.withUploadedToMulcaFile(
                new ByteArrayInputStreamSource(prepareInstallerData()), false,
                mulcaId -> { checkPatchedInstaller(mulcaId, true, false); }
        );

        mulcaManager.withUploadedToMulcaFile(
                new ByteArrayInputStreamSource(prepareInstallerData()), false,
                mulcaId -> { checkPatchedInstaller(mulcaId, false, true); }
        );

        mulcaManager.withUploadedToMulcaFile(
                new ByteArrayInputStreamSource(prepareInstallerData()), false,
                mulcaId -> { checkPatchedInstaller(mulcaId, true, true); }
        );
    }

    /**
     * Checks installer patcher in various parameter configuration.
     * Warning: calling it with both autoLogin and provideSrc params as false, will make Downloader access the file
     * directly, not through Uploader, which may cause a 404 error if Downloader is not configured for direct
     * file storage access.
     *
     * @param autoLogin provide autologin information. If no, key should not be included in response.
     * @param provideSrc Provide source information. If there's no one, but autoLogin is provided, response
     *                   src value should be "Yandex.Unknown"
     */
    private void checkPatchedInstaller(MulcaId mulcaId, boolean autoLogin, boolean provideSrc) {
        HttpGet get = new HttpGet(createBaseUrl(mulcaId, autoLogin, provideSrc));
        get.setHeader("cookie", "yandexuid=" + YANDEX_UID_COOKIE + "; " + SESSION_ID_COOKIE );
        get.setHeader("host", "downloader.yandex.ru");
        byte[] result = ApacheHttpClientUtils.execute(get, new ReadBytesResponseHandler());

        checkResult(result, autoLogin, provideSrc);
    }

    private String createBaseUrl(MulcaId mulcaId, boolean autoLogin, boolean provideSrc) {
        BaseUrlParams params = new BaseUrlParams();
        params.fileName = "installer.exe";
        params.contentType = "application/x-msdownload";
        params.targetRef = new MulcaTargetId(mulcaId);
        params.disposition = Disposition.ATTACHMENT;
        params.autoLogin = Option.some(autoLogin);
        params.uid = Option.some(TestData.TEST_UID);
        params.src = provideSrc ? Option.some("Yandex.SomeSource") : Option.none();

        return UrlCreator.createInstallerUrl(params);
    }

    private static void checkResult(byte[] data, boolean autoLogin, boolean provideSrc) {
        byte[] jsonKey = Arrays.copyOfRange(data, START_MARKER.length, START_MARKER.length + ZERO_COUNT);

        try {
            if (! autoLogin && ! provideSrc) {
                Assert.equals(0, data[0], "Direct download expected, no JSON in data-specific area");
            } else {
                JsonNode jsonNode = PARSER.readTree(jsonKey);
                if (autoLogin) {
                    Assert.isTrue(jsonNode.has("key"), "Expected key node in response");
                    String key = jsonNode.get("key").textValue();
                    Assert.equals(16, key.length(), "Incorrect auth key length");
                } else {
                    Assert.isFalse(jsonNode.has("key"), "No auth key expected in reply");
                }

                Assert.isTrue(jsonNode.has("src"), "Source must be provided in any case, either existing or unknown");
                String src = jsonNode.get("src").textValue();
                if (provideSrc) {
                    Assert.equals("Yandex.SomeSource", src, "Incorrect source: provided, but not returned");
                } else {
                    Assert.equals("Yandex.Unknown", src, "Incorrect source: not specified must be mapped to Yandex.Unknown");
                }

                Assert.isTrue(jsonNode.has("yuid"));
                Assert.equals(YANDEX_UID_COOKIE, jsonNode.get("yuid").textValue(), "YUID must be passes as is");
            }
        } catch (IOException e) {
            throw ExceptionUtils.translate(e);
        }
    }

    private static byte[] prepareInstallerData() {
        byte[] data = new byte[ZERO_COUNT + START_MARKER.length + END_MARKER.length];

        System.arraycopy(START_MARKER, 0, data, 0, START_MARKER.length);
        System.arraycopy(END_MARKER, 0, data, ZERO_COUNT + START_MARKER.length, END_MARKER.length);
        return data;
    }

}
