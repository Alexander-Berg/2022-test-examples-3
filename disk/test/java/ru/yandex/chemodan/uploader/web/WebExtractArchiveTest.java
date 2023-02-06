package ru.yandex.chemodan.uploader.web;

import org.apache.http.client.HttpClient;
import org.dom4j.Element;
import org.junit.Test;

import ru.yandex.chemodan.uploader.ChemodanService;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.services.ServiceFileId;
import ru.yandex.chemodan.uploader.web.client.UploaderClient;
import ru.yandex.chemodan.util.http.HttpClientUtils;
import ru.yandex.chemodan.util.test.TestUser;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

/**
 * @author alexm
 */
public class WebExtractArchiveTest extends AbstractWebTestSupport {

    @Test
    public void uploadArchiveAndTryToExtractAllFile() throws Exception {
        HttpClient httpClient = ApacheHttpClientUtils.singleConnectionClient(Timeout.seconds(5));
        UploaderClient uploaderClient = newUploaderClient();
        ClassPathResourceInputStreamSource source = new ClassPathResourceInputStreamSource(WebExtractArchiveTest.class,
                "precise.tar.gz");
        MulcaId originalMulcaId = uploaderClient
                .uploadToDiskUntilDone(CHE_FILE, source)
                .getFileMulcaId().get();

        ServiceFileId serviceFileId = new ServiceFileId(TestUser.uid, originalMulcaId.getStidCheckNoPart());
        Element response = HttpClientUtils.parseXmlResponse(httpClient,
                HttpClientUtils.httpPost(
                        "http://localhost:" + uploaderHttpPorts.getControlPort() + ApiUrls.EXTRACT_ARCHIVE_URL,
                        UploaderClient.toParameterMap(CHE_FILE)
                                .plus1(ApiArgs.API_VERSION, ApiVersion.V_0_2.toSerializedString())
                                .plus1(ApiArgs.SOURCE_SERVICE, ChemodanService.MULCA)
                                .plus1(ApiArgs.SERVICE_FILE_ID, serviceFileId.toSerializedString())));
        String pollUrl = response.attributeValue("poll-result");
        pollStatusUntilCompleted(httpClient, pollUrl);
    }
}
