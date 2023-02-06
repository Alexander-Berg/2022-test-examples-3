package ru.yandex.msearch.proxy;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.junit.Test;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class QueryLanguageTest extends MsearchProxyTestBase {
    @Test
    public void testQueryLanguage() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"shulgin@yandex-team.ru договорились\","
                    + "\"hdr_from\":\"aaz@yandex-team.ru\","
                    + "\"hdr_from_normalized\":\"aaz@yandex-team.ru\","
                    + "\"hdr_to\":\"garkushin_by@yandex-team.ru\","
                    + "\"hdr_to_normalized\":\"garkushin_by@yandex-team.ru\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"договорились aaz@yandex-team.ru "
                    + "garkushin_by@yandex-team.com\","
                    + "\"hdr_from\":\"garkushin_by@yandex-team.com\","
                    + "\"hdr_from_normalized\":"
                    + "\"garkushin_by@yandex-team.ru\","
                    + "\"hdr_subject\":\"юзкейз\","
                    + "\"hdr_to\":\"dpotapov@yandex-team.ru\","
                    + "\"hdr_to_normalized\":\"dpotapov@yandex-team.ru\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"shulgin@yandex-team.ru юзкейз\","
                    + "\"hdr_from\":\"garkushin_by@yandex-team.com\","
                    + "\"hdr_from_normalized\":"
                    + "\"garkushin_by@yandex-team.ru\","
                    + "\"hdr_to\":\"tabolin@yandex-team.ru\","
                    + "\"hdr_to_normalized\":\"tabolin@yandex-team.ru\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"за слово слово\""),
                doc(
                    "100504",
                    "\"received_date\":\"1234567886\"",
                    "\"pure_body\":\"\","
                    + "\"hdr_subject\":\"слово за слово\""),
                doc(
                    "100505",
                    "\"received_date\":\"1234567885\"",
                    "\"pure_body\":\"\","
                    + "\"hdr_subject\":\"более-менее\""),
                doc(
                    "100506",
                    "\"received_date\":\"1234567884\"",
                    "\"pure_body\":\"\","
                    + "\"hdr_subject\":\"менее, а не более\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100505",
                envelopes("", envelope("100505")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=from:aaz@yandex-team.com")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "from:aaz@yandex-team.com",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=aaz!договорились")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("aaz!договорились", false)),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=договорились!aaz")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "договорились!aaz",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=from:garkushin_by@yandex-team.ru"
                        + "+to:tabolin")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "from:garkushin_by@yandex-team.ru to:tabolin",
                            true,
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=garkushin_by+-юзкейз")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "garkushin_by -юзкейз",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=garkushin_by+-text:юзкейз")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "garkushin_by -text:юзкейз",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=to:%28tabolin+OR+dpotapov%29")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "to:(tabolin OR dpotapov)",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=subject:юзкейз+AND+"
                        + "%28to:tabolin+OR+to:dpotapov%29")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subject:юзкейз AND (to:tabolin OR to:dpotapov)",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100503",
                envelopes("", envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=%22слово+слово%22")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "\\\"слово слово\\\"",
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
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=subject:%22за+слово%22")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subject:\\\"за слово\\\"",
                            true,
                            envelope("100504"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=mid:100500")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "mid:100500",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=более-менее")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "более-менее",
                            true,
                            envelope("100505"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSubscriptionEmail() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"shulgin@yandex-team.ru договорились\","
                    + "\"hdr_from\":\"aaz+1@yandex-team.ru\","
                    + "\"hdr_from_normalized\":\"aaz+1@yandex-team.ru\","
                    + "\"hdr_to\":\"garkushin_by@yandex-team.ru\","
                    + "\"hdr_to_normalized\":\"garkushin_by@yandex-team.ru\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"shulgin@yandex-team.ru договорилис2\","
                    + "\"hdr_from\":\"aaz@yandex-team.ru\","
                    + "\"hdr_from_normalized\":\"aaz@yandex-team.ru\","
                    + "\"hdr_to\":\"garkushin_by@yandex-team.ru\","
                    + "\"hdr_to_normalized\":\"garkushin_by@yandex-team.ru\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=subscription-email:aaz@yandex-team.com")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subscription-email:aaz@yandex-team.com",
                            true,
                            envelope("100501"),
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testQueryLanguageLinks() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"http://ya.ru\","
                    + "\"hdr_to\":\"mark\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"hdr_subject\":\"http://ya.ru\",\"hdr_to\":\"mark\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"to:mark subject:http://ya.ru\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=to:mark+http://ya.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "to:mark http://ya.ru",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=subject:http://ya.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "subject:http://ya.ru",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLuceneKeywords() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"text:OR AND NOT mark\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"OR\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&request=OR")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "OR",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=text:OR+AND+NOT+mark")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:OR AND NOT mark",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLabels() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"Jira\",\"fid\":\"1\",\"lids\":\"8\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"folder_name\":\"Jira\",\"fid\":\"5\",\"lids\":\"8\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"labels_names\":\"Jira\",\"lids\":\"5\n7\","
                    + "\"fid\":\"9\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"Jira\",\"fid\":\"11\""));
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
                "{\"folders\":{\"1\":{\"type\":{\"title\":\"system\"},"
                + "\"name\":\"Inbox\"},"
                + "\"5\":{\"type\":{\"title\":\"user\"},"
                + "\"name\":\"Баги|jira\"},"
                + "\"9\":{\"type\":{\"title\":\"user\"},"
                + "\"name\":\"ждут ответа\"},"
                + "\"11\":{\"type\":{\"title\":\"user\"},"
                    // Beware! nbsp in next line
                + "\"name\":\"Архив|Ждут ответа\"}}}");
            cluster.filterSearch().add(
                "/labels?caller=msearch&mdb=pg&uid=0",
                "{\"labels\":{\"5\":{\"type\":{\"title\":\"user\"},"
                + "\"name\":\"Jira\"},"
                + "\"9\":{\"type\":{\"title\":\"system\"},"
                + "\"symbolicName\":{\"title\":\"draft\"}},"
                + "\"8\":{\"type\":{\"title\":\"system\"},"
                + "\"symbolicName\":{\"title\":\"important_label\"},"
                + "\"name\":\"priority_high\"}}}");
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=folder:jira+OR+label:jira")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "folder:jira OR label:jira",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=метка:+важные")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "метка: важные",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100502"), envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=папка:+%22Ждут+Ответа%22")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "папка: \\\"Ждут Ответа\\\"",
                            true,
                            envelope("100502"),
                            envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCcBccL10n() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"Jira\","
                    + "\"hdr_cc\":\"me\","
                    + "\"hdr_bcc\":\"you\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"hdr_cc\":\"me\","
                    + "\"hdr_bcc\":\"you\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"hdr_cc\":\"me you\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"Jira\","
                    + "\"hdr_cc\":\"you\","
                    + "\"hdr_bcc\":\"me\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=(Копия:me+ИЛИ+Скрытая-Копия:you)"
                        + "+И+НЕ+текст:jira")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "(Копия:me ИЛИ Скрытая-Копия:you)"
                            + " И НЕ текст:jira",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testPartiallyUnknownFields() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"вложение уничтожение поражение\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"вложение поражение уничтожение\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=вложение,уничтожение:поражение")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "вложение,уничтожение:поражение",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testUnknownEmail() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"me@example.com\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"me@example.ru\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=me@example.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "me@example.ru",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testAttachRequest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"Jira\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"attachments\":\"Jira\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"attachname\":\"Jira\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            // Test both attachments search and spaces after field name
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=attachment:+jira")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "attachment: jira",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=filename:+jira")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "filename: jira",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=-вложение:+jira")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "-вложение: jira",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testHasAttachments() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"me@example.com\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"me@example.ru\","
                    + "\"has_attachments\":1"),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"с вложением беды\","
                    + "\"has_attachments\":1"));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=has-attachments:yes")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "has-attachments:yes",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=с-вложениями:нет")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "с-вложениями:нет",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=с-вложениями:беда")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "с-вложениями:беда",
                            true,
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testUnreadQueryLanguage() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"me@example.com\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"me@example.ru\","
                    + "\"unread\":1"),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"unread example\","
                    + "\"unread\":1"));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=непрочитанные:да")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "непрочитанные:да",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=label:unread")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "label:unread",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=unread:no")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "unread:no",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=метка:прочитанные")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "метка:прочитанные",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=unread:example")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "unread:example",
                            true,
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFolderTypeLocalization() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"Jira\","
                    + "\"folder_type\":\"inbox\",\"fid\":\"1\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"Jira\","
                    + "\"folder_name\":\"Мусор\",\"fid\":\"100500\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"Jira\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            cluster.filterSearch().add(
                "/folders?caller=msearch&mdb=pg&uid=0",
                "{\"folders\":{\"1\":{\"type\":{\"title\":\"system\"},"
                + "\"name\":\"Inbox\"},"
                + "\"100500\":{\"type\":{\"title\":\"user\"},"
                + "\"name\":\"Мусор\"}}}");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=папка:+входящие")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "папка: входящие",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=folder:inbox")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "folder:inbox",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=folder:мусор")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "folder:мусор",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testQueryLanguageMessageTypesAndFilters() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"message_type\":\"4 people 19 s_travel\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"filter tips\","
                    + "\"message_type\":\"4 people\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"filter people\","
                    + "\"message_type\":\"19 s_travel\""),
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"pure_body\":\"filter travels\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=type:people")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "type:people",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=filter:people")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "filter:people",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=тип:+(people+-s_travel)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "тип: (people -s_travel)",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=фильтр:trips")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "фильтр:trips",
                            true,
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // test unknown filter
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=filter:tips")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "filter:tips",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100503",
                envelopes("", envelope("100501"), envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=тип:-s_travel")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "тип:-s_travel",
                            true,
                            envelope("100501"),
                            envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSpecialCharsRequests() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "20040",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"40 20\",\"attachname\":\"20040\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"20 40\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"20040\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=20*40")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("20*40", true, envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testDateInterval() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    // noon 15 June 2016
                    "\"received_date\":\"1465981200\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100501",
                    // noon 14 June 2016
                    "\"received_date\":\"1465894800\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100502",
                    // noon 13 June 2016
                    "\"received_date\":\"1465808400\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100503",
                    // noon 12 June 2016
                    "\"received_date\":\"1465722000\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100504",
                    "\"pure_body\":\"before creation\"",
                    ""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            String[] dateBeginAliases = new String[] {
                "date-begin",
                "с-даты",
                "after",
                "после",
                "newer",
                "новее"
            };
            for (String field: dateBeginAliases) {
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/mail/search?uid=0&first=0"
                            + "&query-language"
                            + "&request=" + field + ":15.06.16")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new JsonChecker(
                            serp(
                                field + ":15.06.16",
                                true,
                                envelope("100500"))),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501&mids=100502",
                envelopes("", envelope("100501"), envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=date-begin:13.06.2016"
                        + "+AND+date-end:2016-06-14")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "date-begin:13.06.2016 AND date-end:2016-06-14",
                            true,
                            envelope("100501"),
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=date-begin:trash+OR+date-end:trash")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp("date-begin:trash OR date-end:trash", false)),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100503",
                envelopes("", envelope("100500"), envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=заканчивая-датой:12.06.2016"
                        + "+ИЛИ+с-даты:2016-06-15")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "заканчивая-датой:12.06.2016"
                            + " ИЛИ с-даты:2016-06-15",
                            true,
                            envelope("100500"),
                            envelope("100503"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100502"), envelope("100503")));
            String[] beforeAliases = new String[] {
                "before",
                "до-даты",
                "older",
                "старее"
            };
            for (String field: beforeAliases) {
                try (CloseableHttpResponse response = client.execute(
                        new HttpGet(
                            cluster.proxy().host()
                            + "/api/async/mail/search?uid=0&first=0"
                            + "&query-language"
                            + "&request=" + field + ":14.06.16")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                    YandexAssert.check(
                        new JsonChecker(
                            serp(
                                field + ":14.06.16",
                                true,
                                envelope("100502"),
                                envelope("100503"))),
                        CharsetUtils.toString(response.getEntity()));
                }
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100504",
                envelopes("", envelope("100504")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                    cluster.proxy().host()
                    + "/api/async/mail/search?uid=0&first=0&query-language"
                    + "&request=before:creation")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "before:creation",
                            true,
                            envelope("100504"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testRelativeDateSearch() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            // one minute ago
            long start = System.currentTimeMillis() / 1000 - 60;
            cluster.backend().add(
                doc(
                    "100500",
                    // one minute ago
                    "\"received_date\":" + start,
                    "\"pure_body\":\"\""),
                doc(
                    "100501",
                    // one day and one minute ago
                    "\"received_date\":" + (start - 86400),
                    "\"pure_body\":\"\""),
                doc(
                    "100502",
                    // two days and one minute ago
                    "\"received_date\":" + (start - 86400 - 86400),
                    "\"pure_body\":\"\""),
                doc(
                    "100503",
                    // one week and one minute ago
                    "\"received_date\":" + (start - 86400 * 7),
                    "\"pure_body\":\"\""),
                doc(
                    "100504",
                    "\"pure_body\":\"newer than new\"",
                    ""),
                doc(
                    "100505",
                    "\"pure_body\":\"older than eternity\"",
                    ""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=newer-than:1d")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "newer-than:1d",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=новее-чем:2d")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "новее-чем:2d",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502&mids=100503",
                envelopes("", envelope("100502"), envelope("100503")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=older-than:2d")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "older-than:2d",
                            true,
                            envelope("100502"),
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
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=старее-чем:1w")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "старее-чем:1w",
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
                        + "&query-language"
                        + "&request=newer-than:new")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "newer-than:new",
                            true,
                            envelope("100504"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100505",
                envelopes("", envelope("100505")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=older-than:eternity")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "older-than:eternity",
                            true,
                            envelope("100505"))),
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
                    "\"pure_body\":\"привет мир\","
                    + "\"hdr_from\":\"dpotapov@yandex-team.ru\","
                    + "\"hdr_from_normalized\":\"dpotapov@yandex-team.ru\""),
                // document with misspell
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"пирвет мир\""),
                // two documents which will be found by original request
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"mispell\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));

            // No erratum request should be sent for emails
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=from:dpotapov@yandex-team.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "from:dpotapov@yandex-team.ru",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Nothing will change for request without misspell
            erratum(cluster, "привет", null);
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language&request="
                        + "text:привет+from:dpotapov@yandex-team.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:привет from:dpotapov@yandex-team.ru",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test simple misspell, missed letters checks proper rewrites
            // order, from last to first token
            erratum(cluster, "првет", "привет");
            erratum(cluster, "мр", "мир");
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language&request="
                        + "text:првет+text:мр+from:dpotapov@yandex-team.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:привет text:мир "
                            + "from:dpotapov@yandex-team.ru\","
                            + "\"rule\":\"Misspell",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // force original request
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language&force&request="
                        + "text:првет+text:мр+from:dpotapov@yandex-team.ru")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:првет text:мр from:dpotapov@yandex-team.ru",
                            false)),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test suggest
            erratum(cluster, "пирвет", "привет");
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100501",
                envelopes("", envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=text:пирвет")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:пирвет\",\"suggest\":\"text:привет\","
                            + "\"rule\":\"Misspell",
                            true,
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            // Test fix which won't match any documents
            erratum(cluster, "mispell", "misspell");
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0"
                        + "&query-language"
                        + "&request=text:mispell")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:mispell",
                            true,
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                    + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100502",
                envelopes("", envelope("100502")));

            // DARIA-59728 return non escaped request and suggest in options
            erratum(cluster, "mispell", "привет мир");

            QueryConstructor qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?uid=0&first=0"
                    + "&query-language");
            qc.append("request", "mispell");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "mispell\",\"suggest\":\"привет мир\","
                                + "\"rule\":\"Misspell",
                            true,
                            envelope("100502"))),
                    CharsetUtils.toString(response.getEntity()));
            }


            erratum(cluster, "mispell", "привет мир");
            erratum(cluster, "dpotapov@yandex-team.ru", null);

            qc = new QueryConstructor(
                cluster.proxy().host()
                    + "/api/async/mail/search?uid=0&first=0"
                    + "&query-language");
            qc.append("request", "от:dpotapov@yandex-team.ru текст:mispell");

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "от:dpotapov@yandex-team.ru текст:(привет мир)\""
                                + ",\"rule\":\"Misspell",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testSynonyms() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"море\","
                    + "\"hdr_to\":\"Саша\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"pure_body\":\"море\","
                    + "\"hdr_to\":\"Александр\""),
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"pure_body\":\"море\","
                    + "\"hdr_to\":\"Эдуард\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100501",
                envelopes("", envelope("100500"), envelope("100501")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=to:Шура+море!")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "to:Шура море!",
                            true,
                            envelope("100500"),
                            envelope("100501"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testNestedFields() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"pure_body\":\"folder:inbox\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500",
                envelopes("", envelope("100500")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=text:folder:inbox")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:folder:inbox",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                        cluster.proxy().host()
                        + "/api/async/mail/search?uid=0&first=0&query-language"
                        + "&request=text:(folder:inbox)")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "text:(folder:inbox)",
                            true,
                            envelope("100500"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testMonthFilter() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.backend().add(
                doc(
                    "100500",
                    // Aug 08 2017
                    "\"received_date\":\"1502139600\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100501",
                    // Jul 01 02:00:00 2017 MSK, will not match
                    "\"received_date\":\"1498863600\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100502",
                    // Jun 05 2017
                    "\"received_date\":\"1496610000\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100503",
                    // Jun 01 02:00:00 2017 MSK
                    "\"received_date\":\"1496271600\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100504",
                    // Aug 09 2016
                    "\"received_date\":\"1470690000\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100505",
                    // Jul 02 2016, will not match
                    "\"received_date\":\"1467406800\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100506",
                    // Jun 06 2016
                    "\"received_date\":\"1465160400\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100507",
                    // Sep 10 2016 will not match
                    "\"received_date\":\"1473454800\"",
                    "\"pure_body\":\"\""),
                doc(
                    "100508",
                    // Aug 05 2015, will not match because of year
                    "\"received_date\":\"1438722000\"",
                    "\"pure_body\":\"\""));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100500&mids=100502",
                envelopes("", envelope("100500"), envelope("100502")));
            cluster.filterSearch().add(
                "/filter_search?order=default&mdb=pg"
                + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=100503&mids=100504"
                + "&mids=100506",
                envelopes(
                    "",
                    envelope("100503"),
                    envelope("100504"),
                    envelope("100506")));
            try (CloseableHttpResponse response = client.execute(
                    new HttpGet(
                    cluster.proxy().host()
                    + "/api/async/mail/search?uid=0&first=0&query-language"
                    + "&request=month:%28june+OR+aug%29"
                    + "+year:%282016+OR+2017%29")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        serp(
                            "month:(june OR aug) year:(2016 OR 2017)",
                            true,
                            envelope("100500"),
                            envelope("100502"),
                            envelope("100503"),
                            envelope("100504"),
                            envelope("100506"))),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

