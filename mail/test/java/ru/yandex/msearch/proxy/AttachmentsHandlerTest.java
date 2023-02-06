package ru.yandex.msearch.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AttachmentsHandlerTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // first mail with 4 attaches, with one Disk attachment
                "\"mid\":\"100500\",\"hid\":\"0\",\"url\":\"100500/0\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\"",
                "\"mid\":\"100500\",\"hid\":\"1\",\"url\":\"100500/1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"attachname\":\"attach1.pdf\",\"md5\":\"abcdef\"",
                "\"mid\":\"100500\",\"hid\":\"2\",\"url\":\"100500/2\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"attachname\":\"attach2.pdf\",\"md5\":\"abcdeg\"",
                "\"mid\":\"100500\",\"hid\":\"3\",\"url\":\"100500/3\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"attachname\":\"narod_attachment_links.html\","
                + "\"md5\":\"abcdeh\",\"disposition_type\":\"inline\"",
                "\"mid\":\"100500\",\"hid\":\"4\",\"url\":\"100500/4\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"attachname\":\"attach4.pdf\",\"md5\":\"abcdei\"",
                "\"mid\":\"100500\",\"hid\":\"5\",\"url\":\"100500/5\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"attachname\":\"attach5.pdf\",\"md5\":\"abcdej\","
                + "\"disposition_type\":\"inline\"",
                "\"mid\":\"100500\",\"hid\":\"6\",\"url\":\"100500/6\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"attachname\":\"attach6.pdf\",\"md5\":\"abcdek\","
                + "\"headers\":\"Content-Id: atata\"",
                // another mail with non-trivial hids ordering
                "\"mid\":\"100501\",\"hid\":\"1.2\",\"url\":\"100501/1.2\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"attachname\":\"bttach1.pdf\",\"md5\":\"bbcdef\"",
                "\"mid\":\"100501\",\"hid\":\"1.10\",\"url\":\"100501/1.10\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"attachname\":\"bttach2.pdf\",\"md5\":\"bbcdeg\"",
                "\"mid\":\"100501\",\"hid\":\"1.3\",\"url\":\"100501/1.3\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"attachname\":\"bttach3.pdf\",\"md5\":\"bbcdeh\"",
                "\"mid\":\"100501\",\"hid\":\"1.2.1\","
                + "\"url\":\"100501/1.2.1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"attachname\":\"bttach4.pdf\",\"md5\":\"bbcdei\"",
                // this mail will be filtered out by filter_search
                "\"mid\":\"100502\",\"hid\":\"1.1\",\"url\":\"100502/1.1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567888\","
                + "\"attachname\":\"cttach1.pdf\",\"md5\":\"cbcdef\"",
                // some of attaches will be filtered due to md5 collisions
                "\"mid\":\"100503\",\"hid\":\"1.1\",\"url\":\"100503/1.1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"attachname\":\"attach1.pdf\",\"md5\":\"abcdef\"",
                "\"mid\":\"100503\",\"hid\":\"1.2\",\"url\":\"100503/1.2\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"attachname\":\"bttach2.pdf\",\"md5\":\"bbcdeg\"",
                "\"mid\":\"100503\",\"hid\":\"1.3\",\"url\":\"100503/1.3\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"attachname\":\"dttach3.pdf\",\"md5\":\"cbcdef\"",
                // this attaches will test that deduplication uses both md5 and
                // attachname
                "\"mid\":\"100503\",\"hid\":\"1.4\",\"url\":\"100503/1.4\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"attachname\":\"dttach4.pdf\",\"md5\":\"cbcdef\"",
                "\"mid\":\"100503\",\"hid\":\"1.5\",\"url\":\"100503/1.5\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"attachname\":\"attach1.pdf\",\"md5\":\"cbcdef\"",
                "\"mid\":\"100503\",\"hid\":\"1.6\",\"url\":\"100503/1.6\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"attachname\":\"attach1.pdf\"");
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                AsyncMailSearchTest.envelopes(
                    "",
                    AsyncMailSearchTest.envelope("100503")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                AsyncMailSearchTest.envelopes(
                    "",
                    AsyncMailSearchTest.envelope("100500"),
                    AsyncMailSearchTest.envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/attachments?uid=0&thread_id=1234")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.concatDocs(
                            "\"url\":\"100500/1\","
                            + "\"attachname\":\"attach1.pdf\",\"hid\":\"1\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100500/2\","
                            + "\"attachname\":\"attach2.pdf\",\"hid\":\"2\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100500/4\","
                            + "\"attachname\":\"attach4.pdf\",\"hid\":\"4\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100500/5\","
                            + "\"attachname\":\"attach5.pdf\",\"hid\":\"5\","
                            + "\"disposition_type\":\"inline\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100501/1.2\","
                            + "\"attachname\":\"bttach1.pdf\",\"hid\":\"1.2\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"url\":\"100501/1.2.1\","
                            + "\"attachname\":\"bttach4.pdf\","
                            + "\"hid\":\"1.2.1\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"url\":\"100501/1.3\","
                            + "\"attachname\":\"bttach3.pdf\",\"hid\":\"1.3\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"url\":\"100501/1.10\","
                            + "\"attachname\":\"bttach2.pdf\","
                            + "\"hid\":\"1.10\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"url\":\"100503/1.3\","
                            + "\"attachname\":\"dttach3.pdf\",\"hid\":\"1.3\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100503"),
                            "\"url\":\"100503/1.4\","
                            + "\"attachname\":\"dttach4.pdf\",\"hid\":\"1.4\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100503"),
                            "\"url\":\"100503/1.5\","
                            + "\"attachname\":\"attach1.pdf\",\"hid\":\"1.5\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100503"),
                            "\"url\":\"100503/1.6\","
                            + "\"attachname\":\"attach1.pdf\",\"hid\":\"1.6\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // test paging
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/attachments?uid=0&thread_id=1234"
                        + "&first=1&count=4")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.concatDocs(
                            "\"url\":\"100500/2\","
                            + "\"attachname\":\"attach2.pdf\",\"hid\":\"2\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100500/4\","
                            + "\"attachname\":\"attach4.pdf\",\"hid\":\"4\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100500/5\","
                            + "\"attachname\":\"attach5.pdf\",\"hid\":\"5\","
                            + "\"disposition_type\":\"inline\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"url\":\"100501/1.2\","
                            + "\"attachname\":\"bttach1.pdf\",\"hid\":\"1.2\","
                            + "\"message\":"
                            + AsyncMailSearchTest.envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

