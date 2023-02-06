package ru.yandex.downloader.tests;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.serialize.JsonParser;
import ru.yandex.downloader.TestData;
import ru.yandex.downloader.url.BaseUrlParams;
import ru.yandex.downloader.url.Disposition;
import ru.yandex.downloader.url.StringTargetId;
import ru.yandex.downloader.url.UrlCreator;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;
import ru.yandex.misc.io.http.apache.v4.ReadBytesResponseHandler;
import ru.yandex.misc.test.Assert;

import java.io.UnsupportedEncodingException;

/**
 * @author metal
 */
public class ZipFilesTest extends DownloaderTestBase {
    public static String OID = "";

    @Before
    public void init() throws UnsupportedEncodingException {
        String uri = UrlUtils.addParameter("http://" + TestData.MPFS_HOST + "/json/bulk_download_prepare",
                "uid", TestData.ZIP_TEST_UID.getUid());
        StringEntity postingString =
                new StringEntity("{\"items\":[\"/disk/Test/Img/Several/cut1.jpg\",\"/disk/Test/Img/Several/ddddddddddd.jpg\"]}");

        HttpPost init = new HttpPost(uri);
        init.setEntity(postingString);
        init.setHeader("Content-type", "application/json");

        String response = ApacheHttpClientUtils.executeReadString(init, Timeout.seconds(30));

        JsonObject resp = (JsonObject) JsonParser.getInstance().parse(response);
        OID = ((JsonString) resp.get("oid")).getString();
    }

    @Test
    public void downloadZipFiles() {
        String url = createBaseUrl(OID);
        byte[] result = ApacheHttpClientUtils.execute(
                new HttpGet(url), new ReadBytesResponseHandler(), Timeout.seconds(3), true);
        Assert.equals(442318, result.length);
    }

    private String createBaseUrl(String oid) {
        BaseUrlParams params = new BaseUrlParams();
        params.uid = Option.some(TestData.ZIP_TEST_UID);
        params.fileName = "files.zip";
        params.disposition = Disposition.ATTACHMENT;
        params.targetRef = new StringTargetId(oid);
        params.contentType = "application/zip";

        return UrlCreator.createZipFilesUrl(params);
    }
}
