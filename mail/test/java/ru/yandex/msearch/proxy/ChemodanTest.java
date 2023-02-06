package ru.yandex.msearch.proxy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.base64.Base64Encoder;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.XmlChecker;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.msearch.proxy.api.async.mail.chemodan.AttachShieldMailPS3183;
import ru.yandex.test.util.YandexAssert;

public class ChemodanTest extends MsearchProxyTestBase {

    @Test
    public void testChemodanSearch() throws Exception {
        String get =
            "ctime,id,name,size,source,type,fid,date,ImapModSeq,hid,bcc,"
                + "references,subject,part,mid,stid,attach_size,thread_id,"
                + "reply_to,imapId,tab,receive_date,attachname,attachsize_b";
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.backend().add(
                // only in lucene
                doc(
                    "168603511049701785",
                        "\"received_date\": \"1554758733\"",
                        "\"attachname\": \"2345.jpg\",\n"
                        + "\"attachsize_b\": \"206329\",\n"
                        + "\"mimetype\": \"image/jpeg\""),

                doc(
                    "168603511049701582",
                    "\"received_date\":\"1553344567\"",
                    "\"hid\":\"0\"",
                    "\"attachname\": \"DC4B14D8.jpeg\",\n"
                        + "\"attachsize_b\": \"467447\",\n"
                        + "\"mimetype\": \"image/jpeg\"",
                    "\"attachname\": \"0AA29900.jpeg\",\n"
                        + "\"attachsize_b\": \"2250793\",\n"
                        + "\"mimetype\": \"image/jpeg\"",
                    "\"attachname\": \"E0C9EF67.png\",\n"
                        + "\"attachsize_b\": \"3287352\",\n"
                        + "\"mimetype\": \"image/png\""),
                doc(
                    "168603511049701498",
                    "\"received_date\": \"1553165357\"",
                    "",
                    "\"mimetype\": \"text/plain\"",
                    "\"mimetype\": \"image/jpeg\",\n"
                        + "\"attachsize_b\": \"9068\",\n"
                        + "\"attachname\": \"IMG-20190318-WA0002.jpg\"",
                    "\"mimetype\": \"image/jpeg\",\n"
                        + "\"attachsize_b\": \"87668\",\n"
                        + "\"attachname\": \"IMG-20190318-WA0003.jpg\"",
                    "\"mimetype\": \"image/jpeg\",\n"
                        + "\"attachsize_b\": \"46145\",\n"
                        + "\"attachname\": \"IMG-20190318-WA0004.jpg\""));

            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
                loadResourceAsString("chemodan.search.folders.json"));
            cluster.filterSearch().add(
                "/filter_search?&uid=0&mdb=pg&fids=6&fids=4&fids=1&fids=3&fids=7&fids=2&fids=5&excl_folders=spam&excl_folders=trash&order=default&mids=168603511049701785&mids=168603511049701582&mids=168603511049701498",
                new StaticHttpResource(
                    HttpStatus.SC_OK, new StringEntity(
                        loadResourceAsString("chemodan.search.filter.search.json"),
                    StandardCharsets.UTF_8)));

            String expected = loadResourceAsString("chemodan.search.expected.out.xml");
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/chemodan_search?offset=0&order=desc&sort"
                        + "=ctime&amt=26&client=mobile_mail&newstyle=1"
                        + "&service=mail&tree=0&uid=0&precise"
                        + "-resources-count=false&nosid")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseText =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new XmlChecker(expected),
                    responseText);
            }
        }
    }

    @Test
    public void testChemodanInfo() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.start();

            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.backend().add(
                // only in lucene
                doc(
                    "168603511049701785",
                    "\"received_date\": \"1554758733\"",
                    "\"attachname\": \"2345.jpg\",\n"
                        + "\"attachsize_b\": \"206329\",\n"
                        + "\"mimetype\": \"image/jpeg\""),

                doc(
                    "168603511049701582",
                    "\"received_date\":\"1553344567\"",
                    "\"hid\":\"0\"",
                    "\"attachname\": \"DC4B14D8.jpeg\",\n"
                        + "\"attachsize_b\": \"467447\",\n"
                        + "\"mimetype\": \"image/jpeg\"",
                    "\"attachname\": \"0AA29900.jpeg\",\n"
                        + "\"attachsize_b\": \"2250793\",\n"
                        + "\"mimetype\": \"image/jpeg\"",
                    "\"attachname\": \"E0C9EF67.png\",\n"
                        + "\"attachsize_b\": \"3287352\",\n"
                        + "\"mimetype\": \"image/png\""),
                doc(
                    "168603511049701498",
                    "\"received_date\": \"1553165357\"",
                    "",
                    "\"mimetype\": \"text/plain\"",
                    "\"mimetype\": \"image/jpeg\",\n"
                        + "\"attachsize_b\": \"9068\",\n"
                        + "\"attachname\": \"IMG-20190318-WA0002.jpg\"",
                    "\"mimetype\": \"image/jpeg\",\n"
                        + "\"attachsize_b\": \"87668\",\n"
                        + "\"attachname\": \"IMG-20190318-WA0003.jpg\"",
                    "\"mimetype\": \"image/jpeg\",\n"
                        + "\"attachsize_b\": \"46145\",\n"
                        + "\"attachname\": \"IMG-20190318-WA0004.jpg\""));

            cluster.filterSearch().add(
                "/filter_search?&uid=0&mdb=pg&excl_folders=spam&excl_folders=trash&order=default&mids=168603511049701582",
                new StaticHttpResource(
                    HttpStatus.SC_OK, new StringEntity(
                    loadResourceAsString("chemodan.info.filter.search.json"),
                    StandardCharsets.UTF_8)));

            String expected = loadResourceAsString("chemodan.info.expected.xml");
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/chemodan_info?service=mail&uid=0&id=file:168603511049701582/2&nosid")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseText =
                    CharsetUtils.toString(response.getEntity());
                YandexAssert.check(
                    new XmlChecker(expected),
                    responseText);
            }
        }
    }

    @Ignore
    public void testAttachSid() throws Exception {
        byte[] iv = new byte[16];

        String jsonPart = "single_message_part:{\"ts\":1000"
            + ",\"uid\":\"uid\",\"mid\":\"mid\","
            + "\",\"hid\":\"hid\"}";

        Arrays.fill(iv, (byte) 16);

        byte[] hmac = new byte[32];
        byte[] aesKey = new byte[32];

        Arrays.fill(hmac, (byte) 16);
        Arrays.fill(aesKey, (byte) 18);

        Base64Encoder encoder = new Base64Encoder();
        encoder.process(hmac);
        String hmcaKey =
            "{\"178\" : \"" + encoder.toString() + "\"}";
        encoder = new Base64Encoder();
        encoder.process(aesKey);
        String sidKey = "{\"178\" : \"" + encoder.toString() + "\"}";

        AttachShieldMailPS3183 shield =
            new AttachShieldMailPS3183(sidKey, hmcaKey, 1000);

        System.out.println(shield.encode(jsonPart, iv));
    }
}
