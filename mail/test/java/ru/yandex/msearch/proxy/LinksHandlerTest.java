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

public class LinksHandlerTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // first mail with links, one from attachment
                "\"mid\":\"100500\",\"hid\":\"0\",\"url\":\"100500/0\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\"",
                "\"mid\":\"100500\",\"hid\":\"1\",\"url\":\"100500/1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"md5\":\"abcdef0123456789\",\"x_urls\":\"abcdef\"",
                "\"mid\":\"100500\",\"hid\":\"2\",\"url\":\"100500/2\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"x_urls\":\"abcdeg\"",
                "\"mid\":\"100500\",\"hid\":\"3\",\"url\":\"100500/3\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"x_urls\":\"abcdeh\nabcdei\"",
                "\"mid\":\"100500\",\"hid\":\"4\",\"url\":\"100500/4\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567890\","
                + "\"x_urls\":\"abcdej\nabcdek\nabcdel\"",
                // another mail with non-trivial hids ordering
                "\"mid\":\"100501\",\"hid\":\"1.2\",\"url\":\"100501/1.2\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"x_urls\":\"bbcdef\"",
                "\"mid\":\"100501\",\"hid\":\"1.10\",\"url\":\"100501/1.10\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"x_urls\":\"bbcdeg\"",
                "\"mid\":\"100501\",\"hid\":\"1.3\",\"url\":\"100501/1.3\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"x_urls\":\"bbcdeh\"",
                "\"mid\":\"100501\",\"hid\":\"1.2.1\","
                + "\"url\":\"100501/1.2.1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567889\","
                + "\"x_urls\":\"bbcdei\nbbcdej\"",
                // this mail will be filtered out by filter_search
                "\"mid\":\"100502\",\"hid\":\"1.1\",\"url\":\"100502/1.1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567888\","
                + "\"x_urls\":\"cbcdef\"",
                // some of attaches will be filtered due to x_urls collisions
                // but not the first one
                "\"mid\":\"100503\",\"hid\":\"1.1\",\"url\":\"100503/1.1\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"x_urls\":\"abcdef\"",
                "\"mid\":\"100503\",\"hid\":\"1.2\",\"url\":\"100503/1.2\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"x_urls\":\"bbcdef\"",
                "\"mid\":\"100503\",\"hid\":\"1.3\",\"url\":\"100503/1.3\","
                + "\"thread_id\":\"1234\",\"received_date\":\"1234567887\","
                + "\"x_urls\":\"abcdek\"");
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=trash&excl_folders=spam&excl_folders=hidden_trash"
                + "&mids=100502&mids=100503",
                AsyncMailSearchTest.envelopes(
                    "",
                    AsyncMailSearchTest.envelope("100503")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=trash&excl_folders=spam&excl_folders=hidden_trash"
                + "&mids=100500&mids=100501",
                AsyncMailSearchTest.envelopes(
                    "",
                    AsyncMailSearchTest.envelope("100500"),
                    AsyncMailSearchTest.envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/links?uid=0"
                        + "&thread_id=1234&exclude-trash")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.concatDocs(
                            // first email
                            "\"src\":\"abcdeg\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"abcdeh\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"abcdei\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"abcdej\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"abcdek\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"abcdel\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            // second email
                            "\"src\":\"bbcdef\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"src\":\"bbcdei\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"src\":\"bbcdej\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"src\":\"bbcdeh\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"src\":\"bbcdeg\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            // last one
                            "\"src\":\"abcdef\",\"message\":"
                            + AsyncMailSearchTest.envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // test paging
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/links?uid=0&thread_id=1234"
                        + "&first=4&count=4&exclude-trash")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        TestSearchBackend.concatDocs(
                            "\"src\":\"abcdek\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"abcdel\",\"message\":"
                            + AsyncMailSearchTest.envelope("100500"),
                            "\"src\":\"bbcdef\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"),
                            "\"src\":\"bbcdei\",\"message\":"
                            + AsyncMailSearchTest.envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

