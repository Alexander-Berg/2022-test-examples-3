package ru.yandex.chemodan.uploader.web;

import org.apache.http.client.HttpClient;
import org.dom4j.Element;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.web.client.UploaderClient;
import ru.yandex.chemodan.util.http.HttpClientUtils;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.digest.Md5;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.webdav.DeltaEncodingTest;

/**
 * @author vavinov
 */
public class WebPatchTest extends AbstractWebTestSupport {

    @Test
    public void uploadPatchAndDownload() throws Exception {
        HttpClient httpClient = null;
        UploaderClient uploaderClient = null;
        try {
            httpClient = ApacheHttpClientUtils.singleConnectionClient(Timeout.seconds(5));
            uploaderClient = newUploaderClient();

            DeltaEncodingTest.Triple data = new DeltaEncodingTest.Triple(this.getClass(), "insert-start");

            MulcaId originalMulcaId = uploaderClient
                    .uploadToDiskUntilDone(CHE_FILE, data.original)
                    .getFileMulcaId().get();

            Element uploadPatchUrlResponse = HttpClientUtils.parseXmlResponse(httpClient,
                    HttpClientUtils.httpPost(
                            "http://localhost:" + uploaderHttpPorts.getControlPort() + ApiUrls.PATCH_URL,
                            UploaderClient.toParameterMap(CHE_FILE)
                                    .plus1(ApiArgs.API_VERSION, ApiVersion.V_0_2.toSerializedString())
                                    .plus1(ApiArgs.ORIGINAL_MD5, Md5.A.digest(data.original).hex())
                                    .plus1(ApiArgs.MULCA_ID, originalMulcaId.getStidCheckNoPart())));
            String postPatchUrl = uploadPatchUrlResponse.attributeValue("delta-target");
            String pollPatchUrl = uploadPatchUrlResponse.attributeValue("poll-result");
            Assert.assertEquals(HttpStatus.SC_201_CREATED, HttpClientUtils.downloadStatus(httpClient,
                    HttpClientUtils.httpPut(postPatchUrl, data.delta.readBytes(),
                            Cf.map("Yandex-Diff", Md5.A.digest(data.result).hex(),
                                    "If-Match", Md5.A.digest(data.original).hex()))));

            Element result = pollStatusUntilCompleted(httpClient, pollPatchUrl);
            MulcaId patchedMulcaId = patchInfoDiskFileId(result).get();

            Assert.assertArrayEquals(data.result.readBytes(),
                    mulcaClient.download(patchedMulcaId).readBytes());
        } finally {
            ApacheHttpClientUtils.stopQuietly(httpClient);
            if (uploaderClient != null) {
                uploaderClient.close();
            }
        }
    }
}
