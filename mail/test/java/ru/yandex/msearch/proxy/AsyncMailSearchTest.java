package ru.yandex.msearch.proxy;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.dbfields.MailIndexFields;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.HeadersHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpResource;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.msearch.proxy.MsearchProxyCluster.MproxyClusterContext;
import ru.yandex.msearch.proxy.api.async.mail.rules.SnippetFetchSearchRule;
import ru.yandex.msearch.proxy.api.async.suggest.history.DeleteStoredRequest;
import ru.yandex.msearch.proxy.api.async.suggest.history.StoredRequestFields;
import ru.yandex.msearch.proxy.api.async.suggest.history.UpdateStoredRequest;
import ru.yandex.msearch.proxy.api.mail.Mail;
import ru.yandex.msearch.proxy.highlight.HtmlHighlighter;
import ru.yandex.msearch.proxy.ora.WmiFilterSearchClient;
import ru.yandex.msearch.proxy.search.MailSearcher;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.string.StringUtils;

public class AsyncMailSearchTest extends MsearchProxyTestBase {
    @Test
    public void test() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // basic date ordering and morphology test
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                    + ",\"received_date\":\"1234567890\""
                    + ",\"hdr_subject\":\"???????? ??????\"",
                    "\"pure_body\":\"??????\""),
                // pure search test
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                    + ",\"received_date\":\"1234567891\""
                    + ",\"hdr_subject\":\"???????? ??????\",\"suid\":0",
                    "\"body_text\":\"??????\""),
                // subject ordering, batchSize & special chars test
                doc(
                    "100502",
                    "\"hdr_subject\":\"?????? + ???????? = ??????\",\"suid\":0",
                    ""),
                doc(
                    "100503",
                    "\"hdr_subject\":\"???????? ??????\"",
                    ""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes(
                    "",
                    envelope(
                        "100501",
                        "\"threadId\":\"100501\"",
                        "\"flag\":\"1\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200&suid=0"
                + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100500",
                envelopes("", envelope("100501"), envelope("100500")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200&suid=0"
                + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100502"), envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("??????", true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=%22??????%22&get=mid,threadId,pid")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "\\\"??????\\\"",
                            false,
                            envelope("100501", "\"threadId\":\"100501\""))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????&get=mid&pure-search=false")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("??????", false, "\"100501\"", "\"100500\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????&get=mid&order=subject1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            "\"100502\"",
                            "\"100500\"",
                            "\"100501\"",
                            "\"100503\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200&suid=0"
                + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????+%2b+????????&get=mid&query-language")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("?????? + ????????", true, "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.proxy().port(),
                "/api/async/mail/search?mdb=mdb202&suid=0&first=0&request=no");
        }
    }

    @Test
    public void testCorp() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            LongPrefix prefix =
                new LongPrefix(BlackboxUserinfo.CORP_UID_BEGIN + 1);
            long uid = BlackboxUserinfo.CORP_UID_BEGIN;

            cluster.backend().add(
                prefix,
                doc(
                    "100500",
                    "\"hdr_subject\":\"hello\""
                    + ",\"received_date\":\"1234567890\""
                    + ",\"message_type\":4",
                    ""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\""
                    + ",\"message_type\":4",
                    ""),
                doc(
                    "100502",
                    "\"message_type\":12",
                    ""));
            cluster.corpFilterSearch().add(
                "/filter_search?order=default&mdb=mdb100&uid="
                + uid
                + "&suid=" + (BlackboxUserinfo.CORP_UID_BEGIN + 1L)
                + "&folder_set=default&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash"
                + "&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?noshared&mdb=mdb200&uid="
                        + BlackboxUserinfo.CORP_UID_BEGIN
                        + "&first=0&message_type=4&get=mid&order=subject")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("", true, "\"100500\"", "\"100501\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.corpFilterSearch().add(
                "/filter_search?order=default&mdb=mdb100&uid="
                + BlackboxUserinfo.CORP_UID_BEGIN
                + "&suid=" + (BlackboxUserinfo.CORP_UID_BEGIN + 1L)
                + "&folder_set=default&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash"
                + "&mids=100500&mids=100501&mids=100502",
                envelopes(
                    "",
                    envelope("100500"),
                    envelope("100501"),
                    envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?noshared&mdb=mdb200&uid="
                        + BlackboxUserinfo.CORP_UID_BEGIN
                        + "&first=0&message_type=4&message_type=12&get=mid")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "",
                            true,
                            "\"100500\"",
                            "\"100501\"",
                            "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Ignore
    @Test
    public void testTabsThreads() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String uri = "/_status?service=change_log&prefix=0&allow_cached"
                + ALL_DOLLARS;
            cluster.producer().add(uri, "[{\"localhost\":100500}]");
            cluster.backend().add(
                // separate thread
                doc(
                    "100500",
                    "\"thread_id\":100500,\"received_date\":\"1234567890\","
                        + "\"message_type\":\"4 people\","
                        + "\"hdr_subject\":\"???????? ??????\"",
                    ""),
                // first search hit
                doc(
                    "100501",
                    "\"thread_id\":100505,\"received_date\":\"1234567889\","
                        + "\"message_type\":\"4 people\","
                        + "\"hdr_subject\":\"???????? ??????\"",
                    ""),
                // same thread, but no search hit
                doc(
                    "100502",
                    "\"thread_id\":100505,\"received_date\":\"1234567888\","
                        + "\"message_type\":\"45\","
                        + "\"hdr_subject\":\"???????????? beer\"",
                    ""),
                doc(
                    "100503",
                    "\"thread_id\":100505,\"received_date\":\"1234567887\","
                        + "\"message_type\":\"4 people\","
                        + "\"hdr_subject\":\"Re: ???????? ??????\"",
                    ""),
                doc(
                    "100504",
                    "\"thread_id\":100505,\"received_date\":\"1234567886\","
                        + "\"message_type\":\"4 people\","
                        + "\"hdr_subject\":\"?????? ???????? ??????\"",
                    "\"pure_body\":\"????????\"",
                    "\"pure_body\":\"??????\"",
                    "\"pure_body\":\"????????\"",
                    "\"pure_body\":\"??????\""),
                doc(
                    "100505",
                    "\"thread_id\":100505,\"received_date\":\"1234567885\","
                        + "\"message_type\":\"4 people\","
                        + "\"hdr_subject\":\"???????????????? ?????? ??????\"",
                    ""));

            cluster.threadSearch().add(
                "/threads_info?&mdb=pg&uid=0&suid=1&tid=100500&tid=100505",
                "{\"threads_info\": {"
                    + threadLabels("\"tid\": \"100500\"", "\"tid\": \"100505\"")
                    + envelopes(
                    "",
                    envelope(
                        "100500",
                        "\"threadId\":\"100500\", "
                            + "\"threadCount\":\"1\", "
                            + "\"receiveDate\":\"1234567890\""),
                    envelope(
                        "100501",
                        "\"threadId\":\"100505\", "
                            + "\"threadCount\":\"5\", "
                            + "\"receiveDate\":\"1234567889\"")).substring(1)
                    + "}"
            );

            cluster.threadSearch().add(
                "/threads_info?&mdb=pg&uid=0&suid=1&tid=100505",
                "{\"threads_info\": {"
                    + threadLabels("\"tid\": \"100505\"")
                    + envelopes(
                    "",
                    envelope(
                        "100501",
                        "\"threadId\":\"100505\", "
                            + "\"threadCount\":\"5\", "
                            + "\"receiveDate\":\"1234567889\"")).substring(1)
                    + "}"
            );

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100503&mids=100504",
                envelopes(
                    "",
                    envelope("100503", "\"threadId\":\"100505\""),
                    envelope("100504", "\"threadId\":\"100505\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes(
                    "",
                    envelope("100500", "\"threadId\":\"100500\""),
                    envelope("100501", "\"threadId\":\"100505\"")));

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash"
                    + "&mids=100503&mids=100504&mids=100505",
                envelopes("",
                          envelope("100503", "\"threadId\":\"100505\""),
                          envelope("100504", "\"threadId\":\"100505\""),
                          envelope("100505", "\"threadId\":\"100505\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&mids=100501&mids=100502",
                envelopes("",
                          envelope("100501", "\"threadId\":\"100505\""),
                          envelope("100502", "\"threadId\":\"100505\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100503&mids=100504",
                envelopes("",
                          envelope("100501", "\"threadId\":\"100505\""),
                          envelope("100503", "\"threadId\":\"100505\""),
                          envelope("100504", "\"threadId\":\"100505\"")));

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/tabs/content?uid=0&first=0"
                        + "&tab=people&threaded&side=web")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt =
                    CharsetUtils.toString(response.getEntity());

                String serp =
                    serp(
                        "",
                        false,
                        envelope(
                            "100500",
                            "\"threadId\":\"100500\"",
                            "\"receiveDate\":\"1234567890\"",
                            "\"threadCount\":\"1\"",
                            "\"threadSize\":1"),
                        envelope(
                            "100501",
                            "\"threadId\":\"100505\"",
                            "\"threadCount\":\"5\"",
                            "\"receiveDate\":\"1234567889\"",
                            "\"threadSize\":5"));
                serp = serp.substring(0, serp.length() - 1)
                    + ", \"threadLabels\":["
                    + "{\"tid\":\"100500\"}, {\"tid\":\"100505\"}]}";

                YandexAssert.check(
                    new JsonChecker(serp),
                    responseTxt
                );

                Assert.assertEquals(1, cluster.producer().accessCount(uri));
            }

            //not threaded test
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/tabs/content?uid=0&first=0"
                        + "&tab=people&side=web&get=mid")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "",
                            false,
                            "\"100500\"",
                            "\"100501\"",
                            "\"100503\"",
                            "\"100504\"",
                            "\"100505\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    @Test
    public void testThreads() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            String uri = "/_status?service=change_log&prefix=0&allow_cached"
                + ALL_DOLLARS;
            cluster.producer().add(uri, "[{\"localhost\":100500}]");
            cluster.backend().add(
                // separate thread
                doc(
                    "100500",
                    "\"thread_id\":100500,\"received_date\":\"1234567890\","
                    + "\"hdr_subject\":\"???????? ??????\"",
                    ""),
                // first search hit
                doc(
                    "100501",
                    "\"thread_id\":100505,\"received_date\":\"1234567889\","
                    + "\"hdr_subject\":\"???????? ??????\"",
                    ""),
                // same thread, but no search hit
                doc(
                    "100502",
                    "\"thread_id\":100505,\"received_date\":\"1234567888\","
                    + "\"hdr_subject\":\"???????????? beer\"",
                    ""),
                doc(
                    "100503",
                    "\"thread_id\":100505,\"received_date\":\"1234567887\","
                    + "\"hdr_subject\":\"Re: ???????? ??????\"",
                    ""),
                doc(
                    "100504",
                    "\"thread_id\":100505,\"received_date\":\"1234567886\","
                    + "\"hdr_subject\":\"?????? ???????? ??????\"",
                    "\"pure_body\":\"????????\"",
                    "\"pure_body\":\"??????\"",
                    "\"pure_body\":\"????????\"",
                    "\"pure_body\":\"??????\""),
                doc(
                    "100505",
                    "\"thread_id\":100505,\"received_date\":\"1234567885\","
                    + "\"hdr_subject\":\"???????????????? ?????? ??????\"",
                    ""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100503&mids=100504",
                envelopes(
                    "",
                    envelope("100503", "\"threadId\":\"100505\""),
                    envelope("100504", "\"threadId\":\"100505\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes(
                    "",
                    envelope("100500", "\"threadId\":\"100500\""),
                    envelope("100501", "\"threadId\":\"100505\"")));

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1&excl_folders=hidden_trash"
                + "&mids=100503&mids=100504&mids=100505",
                envelopes("",
                    envelope("100503", "\"threadId\":\"100505\""),
                    envelope("100504", "\"threadId\":\"100505\""),
                    envelope("100505", "\"threadId\":\"100505\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1&excl_folders=hidden_trash"
                + "&mids=100501&mids=100502",
                envelopes("",
                    envelope("100501", "\"threadId\":\"100505\""),
                    envelope("100502", "\"threadId\":\"100505\"")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100503&mids=100504",
                envelopes("",
                    envelope("100501", "\"threadId\":\"100505\""),
                    envelope("100503", "\"threadId\":\"100505\""),
                    envelope("100504", "\"threadId\":\"100505\"")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????&thread_id=100505")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope(
                                "100501",
                                "\"threadId\":\"100505\"",
                                "\"searchHit\":true"),
                            envelope(
                                "100502",
                                "\"threadId\":\"100505\""),
                            envelope(
                                "100503",
                                "\"threadId\":\"100505\"",
                                "\"searchHit\":true"),
                            envelope(
                                "100504",
                                "\"threadId\":\"100505\"",
                                "\"searchHit\":true"),
                            envelope(
                                "100505",
                                "\"threadId\":\"100505\""))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // producer will be called twice, because of two separate rules
            // used for thread listing
            Assert.assertEquals(2, cluster.producer().accessCount(uri));
        }
    }

    @Test
    public void testFidAndLidsFilter() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // document without distinctive marks
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"??????\""),
                // unread document
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\",\"unread\":1",
                    "\"pure_body\":\"??????\""),
                // unread document with fid
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\","
                    + "\"unread\":1,\"fid\":1",
                    "\"pure_body\":\"??????\""),
                // document with another fid
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\",\"fid\":2",
                    "\"pure_body\":\"??????\""),
                // document with fid and lids
                doc(
                    "100504",
                    "\"received_date\":\"1234567886\","
                    + "\"fid\":1,\"lids\":\"1\n2\"",
                    "\"pure_body\":\"??????\""),
                // document with another fid and lids
                doc(
                    "100505",
                    "\"received_date\":\"1234567885\","
                    + "\"fid\":2,\"lids\":\"3\n4\"",
                    "\"pure_body\":\"??????\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100502"), envelope("100503")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100504&mids=100505",
                envelopes("", envelope("100504"), envelope("100505")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100500"),
                            envelope("100501"),
                            envelope("100502"),
                            envelope("100503"),
                            envelope("100504"),
                            envelope("100505"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&unread=yes&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash"
                + "&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????"
                        + "&unread")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=hidden_trash&fids=1"
                + "&mids=100502&mids=100504",
                envelopes("", envelope("100502"), envelope("100504")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????"
                        + "&fid=1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100502"),
                            envelope("100504"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=hidden_trash&fids=1&fids=2&lids=2&lids=3"
                + "&mids=100504&mids=100505",
                envelopes("", envelope("100504"), envelope("100505")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????"
                        + "&fid=1&fid=2&lid=2&lid=3")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100504"),
                            envelope("100505"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testAttachmentsAndLinksFilter() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // document without distinctive marks
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"??????\",\"has_attachments\":1"),
                // document with attachment
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"??????\",\"disposition_type\":\"attachment\""
                    + ",\"attachsize_b\":\"10\""),
                // document with links
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"??????\",\"x_urls\":\"yandex.ru\""),
                // document with links and attahcment
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"??????\",\"disposition_type\":\"attachment\""
                    + ",\"attachsize_b\":\"10\",\"x_urls\":\"yandex.ru\""),
                // document with another content
                doc(
                    "100504",
                    "\"received_date\":\"1234567886\"",
                    "\"pure_body\":\"??????????\",\"has_attachments\":1"));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100503",
                envelopes("", envelope("100501"), envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????"
                        + "&only_attachments")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100501"),
                            envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100503",
                envelopes("", envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????"
                        + "&only_attachments&has_links")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100504",
                envelopes("", envelope("100504")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=-??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "-??????",
                            true,
                            envelope("100504"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100502"
                + "&mids=100504",
                envelopes(
                    "",
                    envelope("100500"),
                    envelope("100502"),
                    envelope("100504")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&exclude_attachments")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "",
                            true,
                            envelope("100500"),
                            envelope("100502"),
                            envelope("100504"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=-??????????&has_attachments")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "-??????????",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMisspell() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(this, false, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // document without mispells
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"???????????? ??????\""),
                // document with misspell
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"???????????? ??????\""),
                // two documents which will be found by original request
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"misspell mispell\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"mispell\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            erratum(cluster, "????????????", "????????????");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=????????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "????????????\",\"suggest\":\"????????????\","
                            + "\"rule\":\"Misspell",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            erratum(cluster, "??????", "??????");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????\",\"rule\":\"Misspell",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100502"), envelope("100503")));
            erratum(cluster, "mispell", "misspell");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=mispell")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "mispell",
                            true,
                            envelope("100502"),
                            envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // fixed documents count will be same as the original documents
            // count, but documents will be different
            erratum(cluster, "??????", "mispell");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????\",\"suggest\":\"mispell\","
                            + "\"rule\":\"Misspell",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testExtendedSearch() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(this, false, true);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // document with first two words
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"???????????? ??????\","
                    + "\"hdr_to\":\"analizer@yandex.ru\","
                    + "\"hdr_subject\":\"?????????? ??????????\""),
                // document with second two words
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"?????? ????????\""),
                // document with all words
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"hdr_to\":\"analizer@ya.ru\","
                    + "\"hdr_subject\":\"?????????????????????? ????????????????????\""),
                // document with partial words
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"???????????? ?????? ??????\""),
                // document with another partial words
                doc(
                    "100504",
                    "\"received_date\":\"1234567886\"",
                    "\"pure_body\":\"?????? ?????? ????????\""),
                // document with another partial words
                doc(
                    "100505",
                    "\"received_date\":\"1234567885\"",
                    "\"hdr_subject\":\"?????????? ??????????\""),
                doc(
                    "100506",
                    "\"received_date\":\"1234567884\"",
                    "\"attachments\":\"pic.jpg\""),
                doc(
                    "100507",
                    "\"received_date\":\"1234567883\"",
                    "\"body_text\":\"hello world\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&body_text=????????????+??????+-??????"
                        + "&body_text=??????+????????+-??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serpRequestless(
                            "\"request\":\"\",\"requests\":{\"body_text\":["
                            + "{\"request\":\"???????????? ?????? -??????\"},"
                            + "{\"request\":\"?????? ???????? -??????\"}]}",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100502",
                envelopes("", envelope("100500"), envelope("100502")));
            String erratumUri = erratum(cluster, "analizer@ya.ru", "nothing");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&hdr_to_cc_bcc=analizer@ya.ru"
                        // check that empty field is ignored
                        + "&hdr_to_cc_bcc="
                        + "&hdr_subject=??????????????????????"
                        + "&hdr_subject=??????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serpRequestless(
                            "\"request\":\"\",\"requests\":{"
                            + "\"hdr_to_cc_bcc\":["
                            + "{\"request\":\"analizer@ya.ru\"},"
                            + "{\"request\":\"\"}],"
                            + "\"hdr_subject\":["
                            + "{\"request\":\"??????????????????????\"},"
                            + "{\"request\":\"??????????\"}]}",
                            true,
                            envelope("100500"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            Assert.assertEquals(0, cluster.erratum().accessCount(erratumUri));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100502"
                + "&mids=100505",
                envelopes(
                    "",
                    envelope("100500"),
                    envelope("100502"),
                    envelope("100505")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&hdr_subject=??????????????????????"
                        + "&hdr_subject=??????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serpRequestless(
                            "\"request\":\"\",\"requests\":{\"hdr_subject\":["
                            + "{\"request\":\"??????????????????????\"},"
                            + "{\"request\":\"??????????\"}]}",
                            true,
                            envelope("100500"),
                            envelope("100502"),
                            envelope("100505"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&hdr_to_cc_bcc=analizer@ya.ru"
                        + "&hdr_subject=??????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serpRequestless(
                            "\"request\":\"\",\"requests\":{"
                            + "\"hdr_to_cc_bcc\":["
                            + "{\"request\":\"analizer@ya.ru\"}],"
                            + "\"hdr_subject\":[{\"request\":\"??????????\"}]}",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
//            cluster.filterSearch().add(
//                    "/filter_search?order=default&mdb=pg"
//                            + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
//                    envelopes("", envelope("100500")));
//            try (CloseableHttpResponse response = client.execute(
//                    new HttpGet(
//                            cluster.proxy().host()
//                                    + "/api/async/mail/search?uid=0&first=0"
//                                    + "&hdr_to=analizer@ya.ru")))
//            {
//                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
//                YandexAssert.check(
//                        new JsonChecker(
//                                serpRequestless(
//                                        "\"request\":\"\",\"requests\":{"
//                                                + "\"hdr_to\":["
//                                                + "{\"request\":\"analizer@ya.ru\"}]}",
//                                        true,
//                                        envelope("100500"))),
//                        CharsetUtils.toString(response.getEntity()));
//            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100506",
                envelopes("", envelope("100506")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&body_text=jpg")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serpRequestless(
                            "\"request\":\"\",\"requests\":{"
                            + "\"body_text\":[{\"request\":\"jpg\"}]}",
                            true,
                            envelope("100506"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100507",
                envelopes("", envelope("100507")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&body_text=worlds")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serpRequestless(
                            "\"request\":\"\",\"requests\":{"
                            + "\"body_text\":[{\"request\":\"worlds\"}]}",
                            true,
                            envelope("100507"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testRequestRewrite() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"?????????? - ????\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"??????????\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????????+-+????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "?????????? - ????",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // QueryParser should not be able to parse this request
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????????+-+????&query-language=true")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "?????????? - ????",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????????+--????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "?????????? --????",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // QueryParser will handle double negation properly
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????????+--????&query-language=true")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "?????????? --????",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    /**
     * Helper method for searching directly in index
     * for stored requests
     * @param cluster - cluster
     * @param client - client
     * @param chunk - chunk to search
     * @param fields - fields to get
     * @return response
     * @throws Exception
     */
    protected static String requestInIndex(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String chunk,
        final String... fields)
        throws Exception
    {
        QueryConstructor query = new QueryConstructor("/search?");
        query.append("get", StringUtils.join(fields, ","));
        query.append("prefix", "0");
        query.append("merge_func", "none");
        String text = "((";
        text += StoredRequestFields.RAW.name() + ":" + chunk + " OR ";
        text += StoredRequestFields.SPACELESS.name() + ":" + chunk + "* OR ";
        text += StoredRequestFields.MORPHOLOGY.name() + ":" + chunk + " OR ";
        text += StoredRequestFields.SUGGESTED.name() + ":" + chunk + " OR ";
        text += StoredRequestFields.ORIGINAL.name() + ":" + chunk + "))";
        query.append("text", text);

        HttpGet request = new HttpGet(
            cluster.backend().searchUri() + query.toString());

        try (CloseableHttpResponse response = client.execute(request)) {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            return CharsetUtils.toString(response.getEntity());
        }
    }

    @Test
    public void testBodyTextScope() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(this, MsearchProxyCluster.PROD_CONFIG);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            String fsUri =
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            cluster.filterSearch().add(
                fsUri + "&mids=100500&mids=100501",
                envelopes(
                    "",
                    envelope("100500"),
                    envelope("100501")));
            cluster.backend().add(
                // document with first two words
                doc(
                    "100500",
                    "\"received_date\":\"1234567891\"",
                    "\"pure_body\":\"??????????????????????\\n?????????????? ?????????????????????? "
                        + "?????????????????????? ????????????????::\\n1-10\\n???????? ??????????????????::\","
                        + "\"body_text\":\"\","
                        + "\"hdr_to\":\"analizer@yandex.ru\","
                        + "\"thread_id\":\"100500\","
                        + "\"hdr_subject\":\"?????????? ??????????\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"\","
                        + "\"body_text\":\"??????????????????????\\n?????????????? ?????????????????????? "
                        + "?????????????????????? ????????????????::\\n1-10\\n???????? ??????????????????::\","
                        + "\"hdr_to\":\"analizer@yandex.ru\","
                        + "\"thread_id\":\"100500\","
                        + "\"hdr_subject\":\"?????????? ??????????\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"\","
                        + "\"body_text\":\"\","
                        + "\"hdr_to\":\"??????????????????????\\n?????????????? ?????????????????????? "
                        + "?????????????????????? ????????????????::\\n1-10\\n???????? ??????????????????::\","
                        + "\"hdr_from\":\"??????????????????????\\n?????????????? ?????????????????????? "
                        + "?????????????????????? ????????????????::\\n1-10\\n???????? ??????????????????::\","
                        + "\"hdr_subject\":\"??????????????????????\\n?????????????? ?????????????????????? "
                        + "?????????????????????? ????????????????::\\n1-10\\n???????? ??????????????????::\","
                        + "\"hdr_from_normalized\":\"??????????????????????\\n?????????????? "
                        + "?????????????????????? ?????????????????????? ????????????????::\\n1-10\\n"
                        + "???????? ??????????????????::\",\"thread_id\":\"100503\""));

            // ?????????????? ?????????????????????? ?????????????????????? ????????????????:: 1-10
            String baseRequest =
                "/api/async/mail/search?mdb=pg&first=0"
                    + "&uid=0&get=mid&request="
                    + "?????????????? ?????????????????????? ?????????????????????? ????????????????:: 1-10";

            String expected =
                "{\"details\":{\"crc32\":\"0\","
                    + "\"search-limits\":{\"offset\":0,\"length\":200},"
                    + "\"search-options\":{\"request\":\""
                    + "?????????????? ?????????????????????? ?????????????????????? ????????????????:: 1-10\"},"
                    + "\"total-found\":2},\"envelopes\":["
                    + "\"100500\",\"100501\"]}";
            String response = searchOk(
                cluster,
                client,
                baseRequest
                    + "&scope=body_text");
            YandexAssert.check(new JsonChecker(expected), response);

            cluster.filterSearch().add(
                fsUri + "&mids=100500",
                envelopes(
                    "",
                    envelope("100500")));
            response = searchOk(
                cluster,
                client,
                baseRequest
                    + "&scope=pure_body&get=mid");
            YandexAssert.check(
                new JsonChecker(
                    "{\"details\":{\"crc32\":\"0\","
                    + "\"search-limits\":{\"offset\":0,\"length\":200},"
                    + "\"search-options\":{\"request\":\""
                    + "?????????????? ?????????????????????? ?????????????????????? ????????????????:: 1-10\"},"
                    + "\"total-found\":1},\"envelopes\":["
                    + "\"100500\"]}"),
                response);
        }
    }

    @Test
    public void testSearchAfterSubjectSuggest() throws Exception {
        try (MsearchProxyCluster cluster =
                new MsearchProxyCluster(this, MsearchProxyCluster.PROD_CONFIG);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            String fsUri =
                "/filter_search?order=default&mdb=pg&uid=0&suid=1"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            cluster.filterSearch().add(
                fsUri + "&mids=100500",
                envelopes("", envelope("100500")));
            cluster.backend().add(
                // document with first two words
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"???????????? ??????\","
                        + "\"hdr_to\":\"analizer@yandex.ru\","
                        + "\"thread_id\":\"100500\","
                        + "\"hdr_subject\":\"?????????? ??????????\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"???????????? ??????\","
                        + "\"hdr_to\":\"analizer@yandex.ru\","
                        + "\"thread_id\":\"100501\","
                        + "\"hdr_subject\":\"?????????? ??????????\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"???????????? ??????\","
                        + "\"hdr_to\":\"analizer@yandex.ru\","
                        + "\"thread_id\":\"100502\","
                        + "\"hdr_subject\":\"?????????? ?????????? - ??????????????????????\""));
            filterSearch(cluster, fsUri, "100500");

            Thread.sleep(100);
            // ????????:(?????????? ??????????)
            String response = searchOk(
                cluster,
                client,
                "/api/async/mail/search?mdb=pg&first=0"
                    + "&uid=0&request=????????:(?????????? ??????????)"
                    + "&scope=subject_thread_100500&get=mid");
            YandexAssert.check(
                new JsonChecker(
                    "{\"details\":{\"crc32\":\"0\","
                        + "\"search-limits\":{\"offset\":0,\"length\":200},"
                        + "\"search-options\":{\"request\":\""
                        + "????????:(?????????? ??????????)\"},"
                        + "\"total-found\":1},\"envelopes\":[\"100500\"]}"),
                response);

            filterSearch(cluster, fsUri, "100502");
            response = searchOk(
                cluster,
                client,
                "/api/async/mail/search?mdb=pg&first=0"
                    + "&uid=0&request=????????:(?????????? ?????????? - ??????????????????????)"
                    + "&scope=subject_thread_100502&get=mid");
            YandexAssert.check(
                new JsonChecker(
                    "{\"details\":{\"crc32\":\"0\","
                        + "\"search-limits\":{\"offset\":0,\"length\":200},"
                        + "\"search-options\":{\"request\":\""
                        + "????????:(?????????? ?????????? - ??????????????????????)\"},"
                        + "\"total-found\":1},\"envelopes\":[\"100502\"]}"),
                response);
        }
    }

    @Test
    public void testManualStoreRequests() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(
            this,
            new MproxyClusterContext()
                .producer(true).useSocheck(true));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.soCheck().add("/check*", "{\"check\":{\"spam\":false}}");

            cluster.start();
            String producerSave = UpdateStoredRequest.API_SAVE_ROUTE + "*";
            SinxProxyHandler indexerProxy = new SinxProxyHandler(
                cluster.backend().indexerHost(),
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(
                producerSave,
                new StaticHttpResource(indexerProxy));

            final String uri =
                cluster.proxy().host()
                    + "/api/async/mail/suggest/history/save?";
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(uri + "&uid=0&request=save+me"));

            String response =
                requestInIndex(
                    cluster,
                    client,
                    "sav",
                    MailIndexFields.REQUEST_RAW);

            String expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"save me\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(uri + "&uid=0&request=temps+en+temps&mid=10"));

            response =
                requestInIndex(
                    cluster,
                    client,
                    "te",
                    MailIndexFields.REQUEST_RAW,
                    MailIndexFields.REQUEST_MIDS);

            expected = "{\"hitsCount\":1,\"hitsArray\":["
                + "{\"request_raw\":\"temps en temps\", "
                + "\"request_mids\":\"10\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                new HttpGet(uri + "&request=temps+en+temps&mid=10"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                new HttpGet(uri + "mid=10"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                new HttpGet(uri + "&uid=2&mid=10"));
        }
    }

    @Test
    public void testStoreRequests() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(
                this,
                new MproxyClusterContext()
                    .producer(true)
                    .erratum(true)
                    .useSocheck(true));
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.soCheck().add("/check*", "{\"check\":{\"spam\":false}}");
            String producerSave = UpdateStoredRequest.API_SAVE_ROUTE + "*";
            String producerDelete = DeleteStoredRequest.API_DELETE_ROUTE+ "*";

            long bpSuid = 0;

            String bpFsURI = "/filter_search?order=default&mdb=mdb200&suid="
                + bpSuid + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            String asyncDefaultParams = "/api/async/mail/search?"
                + "mdb=mdb200&first=0";
            String bpAsyncDefaultParams = asyncDefaultParams
                + "&suid=" + bpSuid + "&request=";

            String oldDefaultParams = "/?user=0&db=pg&how=tm&format=json" +
                "&getfields=mid&length=10&text=";
            String syncDefaultParams = "/api/mail/search?mdb=mdb200&" +
                "suid=0&first=0&request=";

            String[] requestFields = {StoredRequestFields.RAW.name()};
            cluster.start();

            cluster.backend().add(
                fixedDoc("100500", "?????????????????? ????????????", "?????????????? ???????? ????????????"));
            cluster.backend().add(
                fixedDoc("100501", "100501", "???? ?????????? ?????????????? \\\"????????\\\""));
            cluster.backend().add(
                fixedDoc(
                    "100502",
                    "100502",
                    "?????????????? ?????????? ?????? ???????????? " +
                        "???????????????? ?? ?????????? ???????????? ?????????? ??????"));

            cluster.backend().add(
                fixedDoc(
                    "100504",
                    "100504",
                    "?????????? fxd")
                    .replaceAll("test", "tst"));
            cluster.backend().add(
                fixedDoc(
                    "100503",
                    "100503",
                    "???????????????????? ?????????????????????? ????-???????? ?????????????? ?? ?????? ?? ??????"));

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + ALL_DOLLARS,
                "[{\"localhost\":100500}]");

            final HttpHost backendHost = new HttpHost(
                "localhost",
                cluster.backend().indexerPort());

            SinxProxyHandler indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);

            filterSearch(cluster, bpFsURI, "100500");
            filterSearch(cluster, bpFsURI, "100501");
            filterSearch(cluster, bpFsURI, "100502");
            filterSearch(cluster, bpFsURI,  "100502", "100504");

            cluster.producer().add(producerSave, indexerProxy);
            cluster.producer().add(producerDelete, indexerProxy);

            String request = "????????????";
            erratum(cluster, request, null);

            String response = searchOk(
                cluster,
                client,
                bpAsyncDefaultParams + request);

            String serp = serp(request, true, envelope("100500"));

            YandexAssert.check(new JsonChecker(serp), response);
            indexerProxy.waitForRequests(0, 1, 10000);

            String expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"????????????\"}]}";
            YandexAssert.check(
                new JsonChecker(expected),
                requestInIndex(cluster, client, "????????", requestFields));

            // Testing lemmer
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(producerSave, indexerProxy);
            cluster.producer().add(producerDelete, indexerProxy);

            response = requestInIndex(cluster, client, "????????????", requestFields);
            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"????????????\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            // Testing not saving to index, on empty result
            request = "????????????";
            erratum(cluster, request, null);

            response = searchOk(cluster, client, bpAsyncDefaultParams + request);
            serp = serp(request, false);

            YandexAssert.check(new JsonChecker(serp), response);

            indexerProxy.waitForRequests(1, 1, 10000);

            // Testing normalized search
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(producerSave, indexerProxy);
            request = "?????????? ??????????????\u0306";
            erratum(cluster, request, null);

            serp = serp(request, true, envelope("100501"));

            response = searchOk(cluster, client, bpAsyncDefaultParams + request);
            YandexAssert.check(new JsonChecker(serp), response);

            indexerProxy.waitForRequests(0, 1, 1000);
            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"?????????? ??????????????\u0306\"}]}";
            response = requestInIndex(
                cluster,
                client,
                "??????????\\ ??????????????",
                requestFields);

            YandexAssert.check(new JsonChecker(expected), response);

            // Testing erratum stored
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(
                producerSave,
                new ChainedHttpResource(indexerProxy, indexerProxy));
            cluster.producer().add(producerDelete, indexerProxy);
            request = "??????????????";
            erratum(cluster, request, "????????????????");

            response = searchOk(cluster, client, bpAsyncDefaultParams + request);
            serp = serp(
                "????????????????\",\"rule\":\"Misspell",
                true,
                envelope("100502"));
            YandexAssert.check(new JsonChecker(serp), response);

            indexerProxy.waitForRequests(1, 2, 1000);
            response = requestInIndex(
                cluster,
                client,
                request,
                StoredRequestFields.RAW.name(),
                StoredRequestFields.ORIGINAL.name(),
                StoredRequestFields.SUGGESTED.name());

            expected = "{\"hitsCount\":1,\"hitsArray\":" +
                "[{\"request_raw\":\"????????????????\"," +
                "\"request_original\":\"??????????????\",\"request_suggest\":null}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            //Testing erratum suggest stored
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(
                producerSave,
                new ChainedHttpResource(indexerProxy, indexerProxy));
            request = "??????";
            erratum(cluster, request, "??????????");

            response =
                searchOk(
                    cluster,
                    client,
                    bpAsyncDefaultParams + request + "&failingRequest");
            indexerProxy.waitForRequests(0, 2, 1000);
            // building suggest serp
            String suggest = "??????????";
            StringBuilder sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\"," + SEARCH_LIMITS
                    + ",\"total-found\":1"
                    + ",\"search-options\":{\"request\":\"");
            sb.append(request);
            sb.append('"');
            sb.append(",\"rule\":\"Misspell\"");
            sb.append(",\"suggest\":\"");
            sb.append(suggest);
            sb.append("\"");

            sb.append(",\"pure\":true");
            sb.append("}},");

            serp = envelopes(new String(sb), envelope("100502"));
            YandexAssert.check(new JsonChecker(serp), response);

            response = requestInIndex(
                cluster,
                client,
                suggest,
                StoredRequestFields.RAW.name(),
                StoredRequestFields.ORIGINAL.name(),
                StoredRequestFields.SUGGESTED.name());

            expected = "{\"hitsCount\":1,\"hitsArray\":" +
                "[{\"request_raw\":\"??????\"," +
                "\"request_original\":null,\"request_suggest\":\"??????????\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            // Testing nostore param in request and imap=1
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(producerSave, indexerProxy);
            request = "??????????????";
            erratum(cluster, request, null);
            filterSearch(cluster, bpFsURI, "100500", "100501");

            response = searchOk(
                cluster,
                client,
                bpAsyncDefaultParams + request + "&nostore=true");

            serp = serp(request, true, envelope("100501"), envelope("100500"));

            YandexAssert.check(new JsonChecker(serp), response);

            filterSearch(
                cluster,
                "/filter_search?order=default&mdb=mdb200"
                    + "&suid=0&excl_folders=spam&excl_folders=hidden_trash",
                "100502",
                "100503");

            filterSearch(
                cluster,
                "/filter_search?order=default&mdb=mdb200"
                    + "&suid=0&excl_folders=spam&excl_folders=hidden_trash",
                "100500",
                "100501");

            request = "hdr_from:(test@yandex.ru)";
            erratum(cluster, request, null);
            response = searchOk(
                cluster,
                client,
                bpAsyncDefaultParams + request + "&imap=1");
            serp = serp(request,
                true,
                envelope("100500"),
                envelope("100501"),
                envelope("100502"),
                envelope("100503"));

            indexerProxy.waitForNoRequests(500);

            // Testing spaceless search
            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"?????????? ??????????????\u0306\"}]}";
            response = requestInIndex(
                cluster,
                client,
                "????????????????????",
                requestFields);

            YandexAssert.check(new JsonChecker(expected), response);

            //Testing more complex morph
            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"?????????? ??????????????\u0306\"}]}";
            response = requestInIndex(
                cluster,
                client,
                "??????????\\ ??????????????",
                requestFields);

            YandexAssert.check(new JsonChecker(expected), response);

            // Testing counter
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(producerSave, indexerProxy);
            request = "????????????";
            erratum(cluster, request, null);
            response = searchOk(cluster, client, bpAsyncDefaultParams + request);
            serp = serp(request, true, envelope("100500"));
            YandexAssert.check(new JsonChecker(serp), response);
            indexerProxy.waitForRequests(1, 10000);
            response = requestInIndex(
                cluster,
                client,
                request,
                StoredRequestFields.RAW.name(),
                StoredRequestFields.COUNT.name());

            expected = "{\"hitsCount\":1,\"hitsArray\":" +
                "[{\"request_raw\":\"????????????\", \"request_count\":\"2\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            // Testing delete from index
            // First testing request in index
            request = "????????????";
            response = requestInIndex(cluster, client, request, requestFields);
            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"????????????\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            // delete mail itself
            QueryConstructor query = new QueryConstructor(
                cluster.backend().indexerHost()  + "/delete?");
            query.append("prefix", "0");
            query.append("text", "hdr_subject:??????????????????\\ ????????????");

            client.execute(new HttpGet(query.toString()));
            // check request still in index
            response = requestInIndex(cluster, client, request, requestFields);
            YandexAssert.check(new JsonChecker(expected), response);
            // check that search return empty
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(
                producerSave,
                indexerProxy);
            cluster.producer().add(
                producerDelete,
                indexerProxy);
            response = searchOk(cluster, client, bpAsyncDefaultParams + request);
            serp = serp(request, false);

            YandexAssert.check(new JsonChecker(serp), response);
            indexerProxy.waitForRequests(1, 1, 1000);

            response = requestInIndex(cluster, client, request, requestFields);
            expected = "{\"hitsCount\":0,\"hitsArray\":[]}";

            YandexAssert.check(new JsonChecker(expected), response);
            // Testing /? route
            indexerProxy = new SinxProxyHandler(
                backendHost,
                HttpPost.METHOD_NAME,
                HttpGet.METHOD_NAME);
            cluster.producer().add(
                producerSave,
                new StaticHttpResource(indexerProxy));
            cluster.producer().add(
                producerDelete,
                new StaticHttpResource(indexerProxy));

            erratum(cluster, "?????????????? ?????????? ??????", null);

            response = searchOk(cluster,
                client,
                oldDefaultParams
                    + "body_text:%D1%81%D0%BB%D0%BE%D0%B6"
                    + "%D0%BD%D1%8B%D0%B9%5C%20%D1%82%D0%B5%D0%BA%D1%81%D1%82%5C"
                    + "%20%D0%B4%D0%BB%D1%8F&imap=1");

            response = searchOk(cluster,
                client,
                oldDefaultParams + "??????????????+??????????+??????&nostore=true");

            indexerProxy.waitForNoRequests(500);
            response = searchOk(cluster,
                client,
                oldDefaultParams + "????????????????????????????????????????");
            indexerProxy.waitForRequests(1, 1, 10000);

            response = searchOk(cluster,
                client,
                oldDefaultParams + "??????????????+??????????+??????");
            indexerProxy.waitForRequests(0, 1, 10000);

            YandexAssert.check(
                new JsonChecker(
                    TestSearchBackend.prepareResult("\"mid\":\"100502\"")),
                    response);

            response = requestInIndex(
                cluster,
                client,
                "??????????????\\ ??????????",
                requestFields);

            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"?????????????? ?????????? ??????\"}]}";
            YandexAssert.check(new JsonChecker(expected), response);

            // Testing /api/mail/search route
            IniConfig iniConfig = new IniConfig(
                new InputStreamReader(new ByteArrayInputStream(new byte[0])));

            Mail.init(iniConfig, cluster.proxy().config());
            MailSearcher.init(iniConfig);
            WmiFilterSearchClient.init(cluster.proxy().config());
            cluster.filterSearch().add(
                "/filter_search",
                envelopes("", envelope("100502")));

            // testing no store cases
            erratum(cluster, "?????? ???????????? ????????????????", null);
            response = searchOk(
                cluster,
                client,
                syncDefaultParams + "??????+????????????+????????????????&imap=1");

            response = searchOk(
                cluster,
                client,
                syncDefaultParams + "??????+????????????+????????????????&nostore=true");

            indexerProxy.waitForNoRequests(500);

            response = searchOk(
                cluster,
                client,
                syncDefaultParams + "????????????????????????????????????????");

            indexerProxy.waitForRequests(1, 0, 10000);
            response = searchOk(
                cluster,
                client,
                syncDefaultParams + "??????+????????????+????????????????");
            indexerProxy.waitForRequests(0, 1, 10000);
            expected = "{\"threaded\":\"\",\"details\":{\"crc32\":\"0\"," +
                "\"pager\":{\"items-per-page\":200,\"prev\":\"\"," +
                "\"next\":\"\"}," +
                "\"search-options\":" +
                "{\"request\":\"?????? ???????????? ????????????????\"}}," +
                "\"list\":{\"message\":[{\"id\":\"100502\"," +
                "\"message\":{\"id\":null},\"folder\":{\"id\":null}," +
                "\"date\":{\"utc_timestamp\":\"1234567890\"}," +
                "\"size\":null,\"last_status\":\"\",\"subject\":null," +
                "\"firstline\":null,\"types\":null," +
                "\"other\":{\"received_date\":\"1234567890\"," +
                "\"mid\":\"100502\"}}]}}";

            YandexAssert.check(new JsonChecker(expected), response);

            response = requestInIndex(
                cluster,
                client,
                "??????\\ ????????????",
                requestFields);

            expected = "{\"hitsCount\":1," +
                "\"hitsArray\":[{\"request_raw\":\"?????? ???????????? ????????????????\"}]}";

            YandexAssert.check(new JsonChecker(expected), response);

            //test bad symbols deleting
            HttpGet clean = new HttpGet(
                cluster.backend().indexerUri()
                    + "/delete?prefix=0&text=request_raw:*");

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, clean);

            request =
                "??\u0082??????\u0081??\u0082 ??\u0082??????\u0081??\u0082 "
                    + "??\u0082??????\u0081??\u0082";
            searchOk(
                cluster,
                client,
                "/api/async/mail/search?uid=0&mdb=pg"
                    + "&first=0&count=31&request=" + request);

            Thread.sleep(100);
            cluster.backend().checkSearch(
                "/search?prefix=0&service=change_log"
                    + "&text=request_raw:*&length=100&get=*",
                "{\"hitsCount\": 0, \"hitsArray\":[]}");
        }
    }

    @Test
    public void testUserSplit() throws Exception {
        MproxyClusterContext context = new MproxyClusterContext();
        context.userSplit();
        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"hdr_subject\":\"???????? ??????\"",
                    "\"body_text\":\"??????\""));

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                    + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                new SlowpokeHttpResource(
                    new StaticHttpResource(
                        HttpStatus.SC_OK,
                        new StringEntity(envelopes("", envelope("100501"))))
                    , 500));

            HttpGet request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=??????");
            request.setHeader("X-Real-IP", "1.1.1.1");

            cluster.userSplit().add(
                "/mail?&uuid=0&service=mail",
                new HeadersHttpItem(
                    null,
                    "X-Yandex-ExpConfigVersion", "5183",
                    "X-Yandex-ExpBoxes", "28158,0,82;27877,0,62;28235,0,50",
                    "X-Yandex-ExpFlags", "W3siSEFORExFUiI6ICJSRVBPUlQiLCAiQ09OV"
                        + "EVYVCI6IHsiUkVQT1JUIjogeyJmbGFncyI6IFsidmlkZW9fdGh1bW"
                        + "JfcHJldmlldz0xNSw2NTAsMiwyIiwgInZpZGVvX3RodW1iX3pvb2"
                        + "0iXSwgInRlc3RpZCI6IFsiMjgxNTgiXX0sICJNQUlOIjoge319fV"
                        + "0=,W3siSEFORExFUiI6ICJHQVRFV0FZIiwgIkNPTlRFWFQiOiB7I"
                        + "kdBVEVXQVkiOiB7InByZSI6IFsidHJ1ZSJdLCAiYXRvbS5wYXJhb"
                        + "XMucmVsZXYiOiBbImF0b21fdGVhc2VyX3N0b3JlZC1jYW5kaWRhd"
                        + "GUtbGlzdD1tb3JkYV90ZWFzZXJfRVhQX2V4cDthdG9tX3RlYXNlc"
                        + "l9mbWw9RVhQX3BvcnRhbF90ZWFzZXJfZm1sO2F0b21fdGVhc2VyX"
                        + "3ByZWZlci1mbWxib29zdD0wIl19fSwgIkNPTkRJVElPTiI6ICJTR"
                        + "VNTSU9OX2F0b21fY2xpZW50ID09ICdkaXN0cl9wb3J0YWwnIn1d,"
                        + "W3siSEFORExFUiI6ICJHQVRFV0FZIiwgIkNPTlRFWFQiOiB7IkdB"
                        + "VEVXQVkiOiB7InByZSI6IFsidHJ1ZSJdLCAiYXRvbS5wYXJhbXMu"
                        + "cmVsZXYiOiBbImF0b21fc3RvcmVkLWNhbmRpZGF0ZS1saXN0PXBy"
                        + "b21vbGliYV9FWFBfcHJvZCJdfX0sICJDT05ESVRJT04iOiAiU0VT"
                        + "U0lPTl9hdG9tX2NsaWVudCA9PSAncHJvbW9saWInIn1d"
                    ));

            StringBuilder sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\"," + SEARCH_LIMITS
                    + ",\"total-found\":1"
                    + ",\"search-options\":{");
            sb.append("\"experiments\":\"28158,27877,28235\",");
            sb.append(" \"pure\": true, \"request\":\"??????\"}}, ");
            String expected = envelopes(sb.toString(), envelope("100501"));

            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=??????");
            request.setHeader(
                YandexHeaders.X_ENABLED_BOXES,
                "28245,0,99;25466,0,33;32188,0,77");

            sb = new StringBuilder(
                "\"details\":{\"crc32\":\"0\"," + SEARCH_LIMITS
                    + ",\"total-found\":1"
                    + ",\"search-options\":{");
            sb.append("\"experiments\":\"28245,25466,32188\",");
            sb.append(" \"pure\": true, \"request\":\"??????\"}}, ");
            expected = envelopes(sb.toString(), envelope("100501"));

            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSnippetFetchSearch() throws Exception {
        Assert.assertEquals("?????? <span class=\"msearch-highlight\">????????</span"
            + "> ?????????????? ?????? ?? <span class=\"msearch-highlight\">????????</span> <span"
            + " class=\"msearch-highlight\">????????</span>",
            SnippetFetchSearchRule.prepareSnippet(
                "???????? ???????? ????????",
                "?????? ???????? ?????????????? ?????? ?? ???????? ????????",
                HtmlHighlighter.INSTANCE
            ));

        Assert.assertEquals(null, SnippetFetchSearchRule.prepareSnippet(
            "???????? ????????, ???? ???????????",
            "???????? ???????? ?????????? ??????????",
            HtmlHighlighter.INSTANCE
        ));

        Assert.assertEquals(
            "?????? ???????? <span class=\"msearch-highlight\">??????????</span>?? ???? ?????????????? ..",
            SnippetFetchSearchRule.prepareSnippet(
                "??????????",
                "?????? ???????? ???????????? ???? ?????????????? ?? ???? ???????????? ?????? ???????????? ???????????? ???????????? ????????",
                HtmlHighlighter.INSTANCE
            ));

        MproxyClusterContext context = new MproxyClusterContext();
        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, context);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            final String common = "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                + ",\"received_date\":\"1234567891\""
                + ",\"hdr_subject\":\"???????? ??????\"";
            final String part = "\"body_text\":\"??????\"";
            final String request = "??????";
            final String filterSearchRequest = "/filter_search?order=default"
                + "&mdb=pg&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash";

            HttpGet get = new HttpGet(
                cluster.proxy().host()
                + "/api/async/mail/search?mdb=pg&uid=0&first=0&request="
                + request);

            // check for one mid

            cluster.backend().add(doc("1", common, part));

            cluster.filterSearch().add(
                filterSearchRequest + "&mids=1",
                envelopes("", envelope("1")));


            String uri = "/facts?mdb=pg&uid=0&mid=1"
                + "&fact_names=snippet,snippet-text";
            cluster.iexProxy().add(uri, "{}");

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request, true, envelope("1"))),
                    CharsetUtils.toString(response.getEntity()));
                waitForRequests(cluster.iexProxy(), uri, 1, 1000);
            }

            // check for many mids

            cluster.backend().add(
                doc("2", common, part),
                doc("3", common, part),
                doc("4", common, part));

            cluster.filterSearch().add(
                filterSearchRequest + "&mids=1&mids=2",
                envelopes("", envelope("1"), envelope("2")));
            cluster.filterSearch().add(
                filterSearchRequest + "&mids=3&mids=4",
                envelopes("", envelope("3"), envelope("4")));

            uri = "/facts?mdb=pg&uid=0&mid=1&mid=2&mid=3&mid=4"
                + "&fact_names=snippet,snippet-text";
            cluster.iexProxy().add(uri, "{}");

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(
                        request,
                        true,
                        envelope("4"),
                        envelope("3"),
                        envelope("2"),
                        envelope("1"))),
                    CharsetUtils.toString(response.getEntity()));
                waitForRequests(cluster.iexProxy(), uri, 1, 1000);
            }

            uri = "/facts?mdb=pg&uid=0&mid=1&mid=2&mid=3&mid=4"
                + "&fact_names=snippet,snippet-text";
            cluster.iexProxy().add(
                uri,
                "{\"3\":[{\"length\":-1,\"" +
                    "taksa_widget_type_1234543456546\":\"snippet\"}," +
                    "{\"text\":\"?? ?????????? ???????????? ??????????, "
                    + "???? ?????? ?????? ???? ?????????????????????? - ?????? ?????? ??????????????\", "
                    + "\"text_html\":\"<br>?????????????? ??????????????</br>\"," +
                    "\"taksa_widget_type_1234543456546\":\"snippet-text\"}]}");

            get = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0"
                    + "&first=0&wait-snippets&full-snippet=false&request="
                    + request);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(
                        request,
                        true,
                        envelope("4"),
                        envelope(
                            "3",
                            "\"snippet\": \".. ???????????? ??????????, ???? ?????? ..\""),
                        envelope("2"),
                        envelope("1"))),
                    CharsetUtils.toString(response.getEntity()));
                waitForRequests(cluster.iexProxy(), uri, 1, 1000);
            }
        }
    }

    @Test
    public void testTotalFound() throws Exception {
        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, true, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                    + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));

            cluster.backend().add(
                doc("100500", "\"suid\":0,\"hdr_subject\":\"??????\"", ""),
                doc("100501", "\"suid\":0,\"hdr_subject\":\"??????\"", ""),
                doc("100502", "\"suid\":0,\"hdr_subject\":\"??????\"", ""),
                doc("100503", "\"suid\":0,\"hdr_subject\":\"??????\"", ""));

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                    + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                    + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100503")));

            final String request = "??????";

            HttpGet get = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200"
                    + "&suid=0&first=0&request="
                    + request);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            request,
                            true,
                            envelope("100503"),
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.backend().add(
                new LongPrefix(2L),
                doc(2L, "100504", "\"hdr_subject\":\"??????\"", ""),
                doc(2L, "100505", "\"hdr_subject\":\"??????\"", ""),
                doc(2L, "100506", "\"hdr_subject\":\"??????\"", ""),
                doc(2L, "100507", "\"hdr_subject\":\"??????\"", ""),
                doc(2L, "100508", "\"hdr_subject\":\"??????\"", ""),
                doc(2L, "100509", "\"hdr_subject\":\"??????\"", ""));

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=2&suid=3"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100504&mids=100505",
                envelopes(""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=2&suid=3"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100506&mids=100507",
                envelopes("", envelope("100506")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg&uid=2&suid=3"
                    + "&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100508&mids=100509",
                envelopes(""));

            get = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg"
                    + "&uid=2&mdb=pg&suid=3&first=0&count=2&request="
                    + request);
            String expected = "{\"details\":{\"crc32\":\"0\","
                + "\"search-limits\":"
                + "{\"offset\":0,\"length\":2},\"total-found\": 1,"
                + "\"search-options\":{\"request\":\"" + request
                + "\",\"pure\":true}}, \"envelopes\":[{\"mid\":\"100506\"}]}";

            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testEscapingRequest() throws Exception {
        try (MsearchProxyCluster cluster =
                 new MsearchProxyCluster(this, true, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                    + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));

            cluster.backend().add(
                doc(
                    "100500",
                    "\"suid\":0,"
                    + "\"hdr_subject\":\"/?????? (??????][????) ^???????????? }{????????????????????: "
                        + "//????????????// "
                        + "????????????? +??????-??! ??????????*????????????\\\\????* ?????? \\\\\"",
                    ""));

            final String requestBase = cluster.proxy().host()
                    + "/api/async/mail/search?mdb=mdb200&suid=0&first=0";

            String request = "/?????? (??????][????) ^????????????";
            QueryConstructor qc = new QueryConstructor(requestBase);
            qc.append("request", request);

            HttpGet get = new HttpGet(qc.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request, true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(requestBase);
            request = "}{????????????????????: ";
            qc.append("request", request);

            get = new HttpGet(qc.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request, true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(requestBase);
            request = " //????????????//  ";
            qc.append("request", request);

            get = new HttpGet(qc.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request, true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(requestBase);
            request = "+??????-??! ?????????????";
            qc.append("request", request);

            get = new HttpGet(qc.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp(request, true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(requestBase);
            request = "??????????*????????????\\\\????*";
            qc.append("request", request);

            get = new HttpGet(qc.toString());
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("??????????*????????????\\\\\\\\????*",
                             true,
                             envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            qc = new QueryConstructor(requestBase + "&query-language");
            request = "?????? \\";
            qc.append("request", request);
            get = new HttpGet(qc.toString());
            erratum(cluster, "??????", null);
            erratum(cluster, "\\", null);
            try (CloseableHttpResponse response = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("?????? \\\\",
                             true,
                             envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSoCheck() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(
                this,
                new MproxyClusterContext().useSocheck(true));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // These mails matches request
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                    + ",\"received_date\":\"1234567892\"",
                    "\"pure_body\":\"??????\""),
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                    + ",\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"??????\""),
                // This mail matches fake request
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                    + ",\"received_date\":\"1234567891\"",
                    "\"body_text\":\"????????????\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100502",
                envelopes("", envelope("100500"), envelope("100502")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            cluster.soCheck().add(
                "/check*",
                // first request step1
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"),
                // first request step2
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"),
                // second request step1
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"),
                // second request step2
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"));
            // First request will cause empty result
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("??????", true)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Second request will hit fake requests interval
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("??????", true, envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Third request will hit error in socheck
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100500"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSoCheckImap() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(
                this,
                new MproxyClusterContext().useSocheck(true));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // These mails matches request
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                    + ",\"received_date\":\"1234567892\"",
                    "\"pure_body\":\"??????\""),
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                    + ",\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"??????\""),
                // This mail matches fake request
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                    + ",\"received_date\":\"1234567891\"",
                    "\"body_text\":\"????????????\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                + "&suid=0&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100502",
                envelopes("", envelope("100500"), envelope("100502")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=mdb200"
                + "&suid=0&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            cluster.soCheck().add(
                "/check*",
                // first request step1
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"),
                // first request step2
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"),
                // second request step1
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"),
                // second request step2
                new StaticHttpItem(
                    "{\"check\":{\"spam\":true,\"id\""
                    + ":\"A3B0-149140012800005F\"}}"));
            // First request will cause empty result
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request=pure_body:??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("pure_body:??????", false)),
                    CharsetUtils.toString(response.getEntity()));
            }
            HttpAssert.assertStat(
                "socheck-result-imap-1-empty-spam_ammm",
                "0",
                cluster.proxy().port());
            HttpAssert.assertStat(
                "socheck-result-imap-1-spam_ammm",
                "1",
                cluster.proxy().port());
            HttpAssert.assertStat(
                "socheck-result-imap-1-good_ammm",
                "0",
                cluster.proxy().port());
            HttpAssert.assertStat(
                "socheck-result-imap-1-empty-total_ammm",
                "0",
                cluster.proxy().port());
            HttpAssert.assertStat(
                "socheck-result-imap-1-total_ammm",
                "1",
                cluster.proxy().port());
            HttpAssert.assertStat(
                "socheck-result-imap-0-total_ammm",
                "0",
                cluster.proxy().port());
            // Second request will hit fake requests interval
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request=pure_body:??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("pure_body:??????", false, envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Third request will hit error in socheck
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request=pure_body:??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "pure_body:??????",
                            false,
                            envelope("100500"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testImapRegexp() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(
                this,
                new MproxyClusterContext().useSocheck(true));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // These mails matches request
                doc(
                    "100500",
                    "\"hdr_from_normalized\":\"potapovd@gmail.com\""
                    + ",\"received_date\":\"1234567892\""
                        + ",\"message_type\": \"13 news\"",
                    "\"pure_body\":\"??????\""),
                doc(
                    "100502",
                    "\"hdr_from_normalized\":\"potapovd+2@gmail.com\""
                    + ",\"received_date\":\"1234567890\""
                        + ",\"message_type\": \"4 people\"",
                    "\"pure_body\":\"??????\""),
                // This mail matches fake request
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                    + ",\"received_date\":\"1234567891\"",
                    "\"body_text\":\"????????????\""));

            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/furita?mdb=mdb200&suid=0&first=0"
                        + "&request="
                        + "subscription-email:potapovd@gmail.com")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subscription-email:potapovd@gmail.com",
                            false,
                            "\"100500\"",
                            "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/furita?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request="
                        + "subscription-email:(potapovd@gmail.com)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subscription-email:(potapovd@gmail.com)",
                            false,
                            "\"100500\"",
                            "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/furita?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request="
                        + "subscription-email:%22potapovd@gmail.com+"
                        + "potapov@gmail.com%22+AND+pure_body:??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subscription-email:\\\"potapovd@gmail.com "
                                + "potapov@gmail.com\\\" AND pure_body:??????",
                            false,
                            "\"100500\"",
                            "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/furita?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request="
                        + "subscription-email:%22potapovd@gmail.com+"
                        + "potapov@gmail.com%22+AND+message_type:4")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subscription-email:\\\"potapovd@gmail.com "
                                + "potapov@gmail.com\\\" AND message_type:4",
                            false,
                            "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/furita?mdb=mdb200&suid=0&first=0"
                        + "&imap=1&request="
                        + "subscription-email:%22potapovd@gmail.com+"
                        + "potapov@gmail.com%22+AND+type:4")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subscription-email:\\\"potapovd@gmail.com "
                                + "potapov@gmail.com\\\" AND type:4",
                            false,
                            "\"100502\"")),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testOldSearch() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // basic date ordering and morphology test
                doc(
                    "100500",
                    "\"hdr_to\":\"\\\"Vasya\\\" nope@gmail.com\","
                    + "\"hdr_to_normalized\":\"nope@gmail.com\""
                        + ",\"received_date\":\"1234567890\""
                        + ",\"hdr_subject\":\"???????? ????????????\"",
                    "", "\"body_text\":\"??????\""),
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"hdr_subject\":\"???????? nope ??????\",\"suid\":0",
                    "\"body_text\":\"???????? ????????????\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567892\"," +
                        "\"hdr_subject\":\"?????? + ???????? = ??????\",\"suid\":0",
                    "\"body_text\": \"nope one\"",
                    "\"body_text\": \"nope two\"",
                    "\"body_text\": \"nope too much\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567893\"," +
                        "\"hdr_subject\":\"???????? nope\"",
                    ""));
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/?user=0&db=pg&wizard=on&spcctx=doc&how=tm&np=1"
                        + "&utf8=1&format=json&getfields=mid"
                        + "&remote_ip=188.113.131.17&length=10000&text=????????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"mid\":\"100501\"}, {\"mid\":\"100500\", "
                            + "\"merged_docs\":[{\"mid\":\"100500\"}]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/?user=0&db=pg&wizard=on&spcctx=doc&how=tm&np=1"
                        + "&utf8=1&format=json&getfields=mid"
                        + "&remote_ip=188.113.131.17&length=1&text=????????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"mid\":\"100501\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/?user=0&db=pg&wizard=on&spcctx=doc&how=tm&np=1"
                        + "&utf8=1&format=json&getfields=mid"
                        + "&remote_ip=188.113.131.17&offset=1&length=10"
                        + "&text=????????????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":2,\"hitsArray\":["
                            + "{\"mid\":\"100500\", "
                            + "\"merged_docs\":[{\"mid\":\"100500\"}]}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/?user=0&db=pg&wizard=on&spcctx=doc&how=tm&np=1"
                        + "&utf8=1&format=json&getfields=mid"
                        + "&remote_ip=188.113.131.17&offset=-100&length=10"
                        + "&text=nope")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":4,\"hitsArray\":["
                            + "{\"mid\":\"100503\"}, {\"mid\":\"100502\","
                            + "\"merged_docs\":["
                            + "{\"mid\":\"100502\"}, {\"mid\":\"100502\"}]},"
                            + "{\"mid\":\"100501\"},"
                            + "{\"mid\":\"100500\", \"merged_docs\":["
                            + "{\"mid\":\"100500\"}]}"
                            + "]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/?user=0&db=pg&wizard=on&spcctx=doc&how=tm&np=1"
                        + "&utf8=1&format=json&getfields=mid&imap=1"
                        + "&remote_ip=188.113.131.17&offset=-100&length=10"
                        + "&text=received_date:*&near=mid_p:100501")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"hitsCount\":4,\"hitsArray\":["
                            + "{\"mid\":\"100501\"},"
                            + "{\"mid\":\"100500\", \"merged_docs\":["
                            + "{\"mid\":\"100500\"}]}"
                            + "]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHamonFilter() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.backend().add(
                doc(
                    "100500",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"received_date\":\"1234567892\","
                        + "\"lids\":\"FAKE_SEEN_LABEL\n101\n102\"",
                    "\"pure_body\":\"body\""),
                doc(
                    "100502",
                    "\"hdr_to_normalized\":\"potapovd@gmail.com\""
                        + ",\"received_date\":\"1234567890\","
                        + "\"lids\":\"FAKE_SEEN_LABEL\n102\"",
                    "\"pure_body\":\"body\""
                ));

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                    + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));

            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=pg&uid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                    + "\"name\":\"Jira\"},"
                    + "\"9\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"draft\"}},"
                    + "\"8\":{\"type\":{\"title\":\"system\"},"
                    + "\"symbolicName\":{\"title\":\"important_label\"},"
                    + "\"name\":\"priority_high\"},"
                    + "\"102\":{\"name\":\"????????????\","
                    + "\"creationTime\":\"1484658495\","
                    + "\"color\":\"3262267\",\"isUser\":true,"
                    + "\"isSystem\":false,"
                    + "\"type\":{\"code\":1,\"title\":\"user\"}},"
                    + "\"101\" : {\n"
                    + "         \"isUser\" : false,\n"
                    + "         \"creationTime\" : \"1482511637\",\n"
                    + "         \"name\" : \"system_hamon\",\n"
                    + "         \"isSystem\" : true,\n"
                    + "         \"symbolicName\" : {\n"
                    + "            \"title\" : \"hamon_label\",\n"
                    + "            \"code\" : 37\n"
                    + "         },\n"
                    + "         \"color\" : \"\",\n"
                    + "         \"type\" : {\n"
                    + "            \"title\" : \"system\",\n"
                    + "            \"code\" : 3\n"
                    + "         },\n"
                    + "         \"messagesCount\" : 133\n"
                    + "      },"
                    + "\"103\":{\"name\":\"????????????\","
                    + "\"creationTime\":\"1484658528\",\"color\":\"8176580\","
                    + "\"isUser\":true,\"isSystem\":false,\"type\":{\"code\":1,"
                    + "\"title\":\"user\"},\"symbolicName\":{\"code\":0,"
                    + "\"title\":\"\"},\"messagesCount\":1}"
                    + "}}");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search?mdb=pg&uid=0&suid=1&first=0"
                        + "&request=&search-filter=hamon")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("", true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search?mdb=pg&uid=0&suid=1&first=0"
                        + "&request=filter:hamon&query-language")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("filter:hamon", true, envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSharedFilter() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                // document without distinctive marks
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"??????\""),
                // another document withoud distinctive marks
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"??????\",\"lids\":\"1\""),
                // document in shared folder
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"??????\",\"lids\":\"1\\nFAKE_SYNCED_LBL\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501"
                + "&mids=100502",
                envelopes(
                    "",
                    envelope("100500"),
                    envelope("100501"),
                    envelope("100502")));
            // Request without any filters
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&request=??????")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100500"),
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            // Search only in shared folders
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????&shared")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("??????", true, envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Once again
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????+shared:true&query-language")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("?????? shared:true", true, envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            // Search only in user folders
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????&shared=false")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "??????",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Once again
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????+shared:false&query-language")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "?????? shared:false",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // And again
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????+AND+NOT+??????????:????&query-language")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "?????? AND NOT ??????????:????",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Conflicting filters
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????+shared:false&query-language&shared")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("?????? shared:false", false)),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSearchHighlight() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.backend().add(
                doc(
                    "100501",
                    "\"hdr_to_normalized\":\"analizer@yandex.ru\""
                        + ",\"received_date\":\"1234567891\""
                        + ",\"hdr_subject\":\"???????? ?????? ?????????????? ?????????? "
                        + "?????????????? utkonos ivan ??????????\",\"suid\":0",
                    "\"body_text\":\"????????\", \"hid\":\"0\"",
                    "\"body_text\":\"??????????\", \"hid\":\"1.2\""));
            String envelope = envelope(
                "100501",
                "\"to\":[{\"displayName\":\"ivan"
                    + ".dudinov@yandex.ru\",\"local\":\"ivan.dudinov\","
                    + "\"domain\":\"yandex.ru\"}]",
                "\"subjectInfo\":{\"subject\":\"???????????????? ?????????? "
                    + "utkonos.ru\",\"type\":\"\",\"postfix\":\"\","
                    + "\"prefix\":\"Re:\",\"isSplitted\":true}",
                "\"attachments\":[]",
                "\"firstline\":\"???????????????????? ???? ???????????? ????????????\"",
                "\"cc\":["
                    + "{\"displayName\":\"??????????\","
                    + "\"local\":\"copy\","
                    + "\"domain\":\"utkonos.ru\"}]",
                "\"bcc\":["
                    + "{\"displayName\":\"??????????????\","
                    + "\"local\":\"hidden\","
                    + "\"domain\":\"utkonos.ru\"}]",
                "\"from\":["
                    + "{\"displayName\":\"??????????????\","
                    + "\"local\":\"auto\","
                    + "\"domain\":\"utkonos.ru\"}]");
            System.out.println("ENVELOPE " + envelope);
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                    + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope));

            String expected = "{\"details\":{\"crc32\":\"0\","
                + "\"search-limits\":{\"offset\":0,\"length\":200},"
                + "\"search-options\":{\"request\":\"??????????\",\"pure\":true},"
                + "\"total-found\":1},"
                + "\"envelopes\":[{\"firstline_highlighted\":\"???????????????????? ???? "
                + "???????????? <span class=\\\"msearch-highlight\\\">??????????</span>??\","
                + "\"to\":[{\"local\":\"ivan.dudinov\",\"domain\":\"yandex.ru\","
                + "\"displayName\":\"ivan.dudinov@yandex.ru\"}],"
                + "\"firstline\":\"???????????????????? ???? ???????????? ????????????\","
                + "\"cc\":[{\"local\":\"copy\",\"domain\":\"utkonos.ru\","
                + "\"displayName\":\"??????????\"}],\"subjectInfo\":{\"type\":\"\","
                + "\"subject_highlighted\":\"???????????????? <span "
                + "class=\\\"msearch-highlight\\\">??????????</span> utkonos.ru\","
                + "\"postfix\":\"\",\"isSplitted\":true,\"prefix\":\"Re:\","
                + "\"subject\":\"???????????????? ?????????? utkonos.ru\"},"
                + "\"bcc\":[{\"local\":\"hidden\",\"domain\":\"utkonos.ru\","
                + "\"displayName\":\"??????????????\"}],\"mid\":\"100501\","
                + "\"from\":[{\"local\":\"auto\",\"domain\":\"utkonos.ru\","
                + "\"displayName\":\"??????????????\"}],\"attachments\":[]}]}";

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=??????????&highlight")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void waitForRequests(
        final StaticServer res,
        final String uri,
        final int reqs,
        final int timeout)
        throws Exception
    {
        int sleepTime = 100;
        int waiting = 0;
        while (res.accessCount(uri) != reqs) {
            Thread.sleep(sleepTime);
            waiting += sleepTime;
            if (waiting > timeout) {
                throw new TimeoutException("Timeout waiting requests");
            }
        }
    }
}

