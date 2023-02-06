package ru.yandex.msearch.proxy.suggest;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;
import ru.yandex.msearch.proxy.config.ImmutableMsearchProxyConfig;
import ru.yandex.msearch.proxy.config.MsearchProxyConfigBuilder;
import ru.yandex.msearch.proxy.suggest.utils.Contact;
import ru.yandex.msearch.proxy.suggest.utils.MailUser;
import ru.yandex.msearch.proxy.suggest.utils.SuggestTestUtil;
import ru.yandex.msearch.proxy.suggest.utils.SuggestTestUtil.Email;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SuggestContactsTest extends MsearchProxyTestBase {
    protected static final String CONTACT_OLD_API = "/api/suggest/?";
    protected static final String CONTACT_NEW_API =
        "/api/async/mail/suggest/contact?";

    private final SuggestTestUtil old = new SuggestTestUtil(CONTACT_OLD_API);
    private final SuggestTestUtil async = new SuggestTestUtil(CONTACT_NEW_API);
    private static final long WAIT_TIME = 1000L;

    @Test
    public void testCorpSuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue_corp&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1120000000040290L);
            cluster.backend().add(
                prefix,
                "\"url\":\"10500/0\","
                + "\"received_date\":10,"
                + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                + "\"hdr_from\": \"\\\"\\\" <sandbox-noreply@yandex-team.ru>\","
                + "\"hdr_to\": \"<golem-cc@yandex-team.ru>, "
                    + "\\\"Ivan Dudinov\\\" <vonidu@yandex-team.ru>\""
                );

            cluster.backend().add(
                prefix,
                "\"url\":\"10500/0\","
                    + "\"received_date\":11,"
                    + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                    + "\"hdr_from\": "
                    + "\"\\\"Anna Glazyrina\\\" <annagl@yandex-team.ru>\","
                    + "\"hdr_to\": "
                    + "\"\\\"search (рассылка)\\\" <search@yandex-team.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"10501/0\","
                    + "\"received_date\":12,"
                    + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                    + "\"hdr_from\": \"\\\"Sergey Lyadzhin\\\" " +
                    "<lyadzhin@yandex-team.ru>\","
                    + "\"hdr_to\": "
                    + "\"\\\"tyht@yandex-team.ru\\\" <tyht@yandex-team.ru>\\n"
                    + "\\\"mail-task@yandex-team.ru\\\" <mail-task@yandex-team.ru>\","
                    + "\"hdr_cc\": "
                    + "\"\\\"no_reply@yandex-team.ru\\\" <no_reply@yandex-team.ru>\\n"
                    + "\\\"mail-search@yandex-team.ru\\\" <mail-search@yandex-team.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"10502/0\","
                    + "\"received_date\":13,"
                    + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                    + "\"hdr_from\": \"\\\"(st) robot-eksperimentus – Робот " +
                    "Экспериментус\\\" <startrek@yandex-team.ru>\","
                    + "\"hdr_to\": \"\\\"psslt (рассылка)\\\" "
                    + "<psslt@yandex-team.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"10503/0\","
                    + "\"received_date\":14,"
                    + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                    + "\"hdr_from\": "
                    + "\"\\\"(st) robot-help – robot-help robot-help\\\" " +
                    "<startrek@yandex-team.ru>\","
                    + "\"hdr_to\": \"\\\"nobody (рассылка)\\\" "
                    + "<nobody@yandex-team.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"10504/0\","
                    + "\"received_date\":15,"
                    + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                    + "\"hdr_from\": "
                    + "\"\\\"denikekate@yandex-team.ru\\\" " +
                    "<invites@mailer.surveygizmo.com>\","
                    + "\"hdr_to\": \"<olegenka@yandex-team.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"10505/0\","
                    + "\"received_date\":16,"
                    + "\"fid\":1,"
                    + "\"foder_type\": \"inbox\","
                    + "\"hdr_from\": \"\\\"Максим Котяков (kotjakov)\\\" "
                    + "<robot-atushka@yandex-team.ru>\","
                    + "\"hdr_to\": \"<psslt@yandex-team.ru>\"");
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                    + "&dbfields=hosts.db_id.2&sid=2&suid=1120000000040290",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2"
                + "&suid=1120000000040290",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));
            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2"
                + "&suid=1120000000040290",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            final String uri =
                "/api/async/mail/suggest?suid=" + prefix.toString()
                    + "&highlight&limit=10&type=contact&request=";

            //test startreck
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "robo")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(response.getEntity());
                System.out.println("Response txt:" + responseTxt);
                YandexAssert.check(
                    new JsonChecker(
                    "["
                        + "{\"display_name\":\"Максим Котяков\", "
                        + "\"display_name_highlighted\":\"Максим Котяков\","
                        + "\"search_params\":{},"
                        + "\"email\":\"robot-atushka@yandex-team.ru\", "
                        + "\"email_highlighted\":\"<span class=\\\"msearch" +
                        "-highlight\\\">robo</span>t-atushka@yandex-team.ru\", "
                        + "\"search_text\":\"from:(kotjakov robot-atushka@yandex-team.ru)\", "
                        + "\"show_text\":\"\\\"Максим Котяков\\\" robot-atushka@yandex-team.ru\", "
                        + "\"show_text_highlighted\":\"\\\"Максим " +
                        "Котяков\\\" <span class=\\\"msearch-highlight\\\">robo</span>t-atushka@yandex-team.ru\", "
                        + "\"target\":\"contact\", \"unread_cnt\":0},"
                        + "{\"display_name\":\"robot-help\", "
                        + "\"search_params\":{},"
                        + "\"display_name_highlighted\":\"<span " +
                        "class=\\\"msearch-highlight\\\">robo</span>t-help\", "
                        + "\"email\":\"startrek@yandex-team.ru\", "
                        + "\"email_highlighted\":\"startrek@yandex-team.ru\", "
                        + "\"search_text\":\"from:(robot-help startrek@yandex-team.ru)\", "
                        + "\"show_text\":\"\\\"robot-help\\\" startrek@yandex-team.ru\", "
                        + "\"show_text_highlighted\":\"\\\"<span " +
                        "class=\\\"msearch-highlight\\\">robo</span>t-help\\\" "
                        + "startrek@yandex-team.ru\", "
                        + "\"target\":\"contact\", \"unread_cnt\":0},"
                        + "{\"display_name\":\"Робот Экспериментус\", "
                        + "\"search_params\":{},"
                        + "\"display_name_highlighted\":\"<span " +
                        "class=\\\"msearch-highlight\\\">Робо</span>т Экспериментус\", "
                        + "\"email\":\"startrek@yandex-team.ru\", "
                        + "\"email_highlighted\":\"startrek@yandex-team.ru\", "
                        + "\"search_text\":\"from:(robot-eksperimentus startrek@yandex-team.ru)\", "
                        + "\"show_text\":\"\\\"Робот Экспериментус\\\" startrek@yandex-team.ru\", "
                        + "\"show_text_highlighted\":\"\\\"<span " +
                        "class=\\\"msearch-highlight\\\">Робо</span>т " +
                        "Экспериментус\\\" startrek@yandex-team.ru\", "
                        + "\"target\":\"contact\", \"unread_cnt\":0}"
                        + "]"),
                    responseTxt);
            }

            //test multi emails to
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(cluster.proxy().host() + uri + "tyh")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"display_name\":\"tyht\", "
                            + "\"search_params\":{},"
                            + "\"display_name_highlighted\":\"<span " +
                            "class=\\\"msearch-highlight\\\">tyh</span>t\", "
                            + "\"email\":\"tyht@yandex-team.ru\", "
                            + "\"email_highlighted\":"
                            + "\"<span class=\\\"msearch-highlight\\\">tyh"
                            + "</span>t@yandex-team.ru\", "
                            + "\"search_text\":\"tyht@yandex-team.ru\", "
                            + "\"show_text\":\"\\\"tyht\\\" tyht@yandex-team.ru\", "
                            + "\"show_text_highlighted\":\"\\\"<span "
                            + "class=\\\"msearch-highlight\\\">tyh</span>t\\\" "
                            + "<span class=\\\"msearch-highlight\\\">tyh"
                            + "</span>t@yandex-team.ru\", "
                            + "\"target\":\"contact\", \"unread_cnt\":0}"
                            + "]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.backend().add(
                prefix,
                "\"url\":\"105010/0\","
                    + "\"received_date\":110,"
                    + "\"fid\":1,"
                    + "\"hdr_from\": \"\\\"Лидия Попело\\\" <info@calendar" +
                    ".yandex-team.ru>\","
                    + "\"hdr_to\": \"<vonidu@yandex-team.ru>\","
                    + "\"reply_to_normalized\": \"inmotion@yandex-team.ru\\n\"");
            //test calendar

            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + uri + "%D0%9B%D0%B8%D0%B4%D0%B8%D1%8F")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"display_name\":\"Лидия Попело\", "
                            + "\"search_params\":{},"
                            + "\"display_name_highlighted\":\"<span " +
                            "class=\\\"msearch-highlight\\\">Лидия</span> Попело\", "
                            + "\"email\":\"info@calendar.yandex-team.ru\", "
                            + "\"email_highlighted\":\"info@calendar.yandex-team.ru\", "
                            + "\"search_text\":\"from:(inmotion info@calendar.yandex-team.ru)\", "
                            + "\"show_text\":\"\\\"Лидия Попело\\\" info@calendar.yandex-team.ru\", "
                            + "\"show_text_highlighted\":\"\\\"<span " +
                            "class=\\\"msearch-highlight\\\">Лидия</span> Попело\\\" "
                            + "info@calendar.yandex-team.ru\", "
                            + "\"target\":\"contact\", \"unread_cnt\":0}]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            //test wiki
//            cluster.backend().add(
//                prefix,
//                "\"url\":\"105011/0\","
//                    + "\"received_date\":111,"
//                    + "\"hdr_from\": \"\\\"(Wiki) Андрей Листопад\\\" <wiki@yandex-team.ru>\","
//                    + "\"hdr_to\": \"<vonidu@yandex-team.ru>\","
//                    + "\"reply_to\": \"nix-listopad@yandex-team.ru\","
//                    + "\"reply_to_normalized\": \"nix-listopad@yandex-team.ru\\n\"");
//
//            try (CloseableHttpResponse response = client.execute(
//                new HttpGet(
//                    cluster.proxy().host()
//                        + uri + "nix")))
//            {
//                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
//                YandexAssert.check(
//                    new JsonChecker(
//                        "["
//                            + "{\"display_name\":\"Андрей Листопад\", "
//                            + "\"search_params\":{},"
//                            + "\"display_name_highlighted\":\"Андрей Листопад\", "
//                            + "\"email\":\"wiki@yandex-team.ru\", "
//                            + "\"email_highlighted\":\"wiki@yandex-team.ru\", "
//                            + "\"search_text\":\"from:(nix-listopad wiki@yandex-team.ru)\", "
//                            + "\"show_text\":\"\\\"Андрей Листопад\\\" wiki@yandex-team.ru\", "
//                            + "\"show_text_highlighted\":\"\\\"Андрей " +
//                            "Листопад\\\" wiki@yandex-team.ru\", "
//                            + "\"target\":\"contact\", \"unread_cnt\":0}"
//                            + "]"),
//                    CharsetUtils.toString(response.getEntity()));
//            }
        }
    }

    @Test
    public void testContactMultiwordSuggest() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            MailUser user =
                new MailUser(0, "pg", "united-suggest@yandex.ru");

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            cluster.producer().add(
                "/_status?service=change_log&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            String[] emailsFrom = {
                "derevo@yandex.ru",
            };

            String[] emailsTo = {
                "vonidu@yandex.ru",
                "dudinov@yandex.ru"
            };

            String[] displayNames = {
                "Vonidu",
                "Ivan Dudinov"
            };

            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&emails=getall"
                + "&sid=2&uid=0",
                MsearchProxyCluster.blackboxResponse(0, 0, "pg"));

            for (String email: emailsFrom) {
                Email mail = new Email(RandomStringUtils.randomNumeric(18))
                    .from(email, email)
                    .to(user.email(), user.email());

                async.indexDoc(cluster, user, mail);
            }

            for (int i = 0; i < emailsTo.length; i++) {
                String email = emailsTo[i];
                String displayName = displayNames[i];
                Email mail = new Email(RandomStringUtils.randomNumeric(18))
                    .from(user.email(), user.email())
                    .to(displayName, email);

                async.indexDoc(cluster, user, mail);
            }

            String baseRequest =
                "/api/async/mail/suggest?mdb=pg&uid=0";

            String request = baseRequest + "&request=vostok";

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(request)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker("[]"),
                    CharsetUtils.toString(response.getEntity()));
            }

            QueryConstructor qc = new QueryConstructor(baseRequest);
            qc.append("request", "vostok d");
            String expected =
                "[" +
                    "{\"target\":\"contact\"," +
                    "\"show_text\":\"\\\"Ivan Dudinov\\\" dudinov@yandex.ru\"," +
                    "\"display_name\":\"Ivan Dudinov\"," +
                    "\"email\": \"dudinov@yandex.ru\"," +
                    "\"search_text\":\"vostok dudinov@yandex.ru\"," +
                    "\"search_params\":{}," +
                    "\"unread_cnt\": 0}," +
                    "{\"target\":\"contact\"," +
                    "\"show_text\":\"\\\"derevo\\\" derevo@yandex.ru\"," +
                    "\"display_name\":\"derevo\"," +
                    "\"search_params\":{}," +
                    "\"email\": \"derevo@yandex.ru\"," +
                    "\"search_text\":\"vostok derevo@yandex.ru\"," +
                    "\"unread_cnt\": 0}," +
                    "{\"search_text\":\"vostok метка:Важные\", " +
                    "\"search_params\":{}," +
                    "\"show_text\":\"Важные\", \"target\":\"important\""
                    + ", \"lid\":null}, "
                    + " {\"search_text\":\"vostok date-begin:\", "
                    + "\"show_text\":\"date-begin:\", " +
                    "\"search_params\":{}," +
                    "\"target\":\"ql\"},{\"search_text\":\"vostok date-end:\", "
                    + "\"show_text\":\""
                    + "date-end:\", \"target\":\"ql\", \"search_params\":{}}"
                    + "]";
            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr = CharsetUtils.toString(response.getEntity());

                YandexAssert.check(
                    new JsonChecker(expected),
                    responseStr
                );
            }

            qc = new QueryConstructor(baseRequest);
            qc.append("request", "ivan d");
            qc.append("senderReceiver", "true");
            expected =
                "[" +
                    "{\"target\":\"contact\"," +
                    "\"show_text\":\"\\\"Ivan Dudinov\\\" dudinov@yandex.ru\"," +
                    "\"display_name\":\"Ivan Dudinov\"," +
                    "\"email\": \"dudinov@yandex.ru\"," +
                    "\"search_text\":\"dudinov@yandex.ru\"," +
                    "\"search_params\":{}," +
                    "\"unread_cnt\": 0}," +
                    "{\"target\":\"sender\"," +
                    "\"show_text\":\"\\\"Ivan Dudinov\\\" dudinov@yandex.ru\"," +
                    "\"display_name\":\"Ivan Dudinov\"," +
                    "\"email\": \"dudinov@yandex.ru\"," +
                    "\"search_text\":\"от:dudinov@yandex.ru\"," +
                    "\"search_params\":{}," +
                    "\"unread_cnt\": 0}," +
                    "{\"target\":\"receiver\"," +
                    "\"show_text\":\"\\\"Ivan Dudinov\\\" dudinov@yandex.ru\"," +
                    "\"display_name\":\"Ivan Dudinov\"," +
                    "\"email\": \"dudinov@yandex.ru\"," +
                    "\"search_text\":\"кому:dudinov@yandex.ru\"," +
                    "\"search_params\":{}," +
                    "\"unread_cnt\": 0}," +
                    "{\"search_text\":\"ivan метка:Важные\", " +
                    "\"search_params\":{}," +
                    "\"show_text\":\"Важные\", \"target\":\"important\""
                    + ", \"lid\":null}, "
                    + " {\"search_text\":\"ivan date-begin:\", "
                    + "\"show_text\":\"date-begin:\", " +
                    "\"search_params\":{}," +
                    "\"target\":\"ql\"},{\"search_text\":\"ivan date-end:\", "
                    + "\"search_params\":{}," + "\"show_text\":\""
                    + "date-end:\", \"target\":\"ql\"}"
                    + "]";
//            try (CloseableHttpResponse response = client.execute(
//                cluster.proxy().host(), new HttpGet(qc.toString())))
//            {
//                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
//                String responseStr = CharsetUtils.toString(response.getEntity());
//                YandexAssert.check(
//                    new JsonChecker(expected),
//                    responseStr
//                );
//            }
        }
    }

    @Test
    public void testContactSuggestFromDomain() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true) {
            @Override
            public ImmutableMsearchProxyConfig config(
                final MproxyClusterContext clusterContext,
                final IniConfig iniConfig)
                throws Exception
            {
                MsearchProxyConfigBuilder config
                    = new MsearchProxyConfigBuilder(
                    super.config(clusterContext, iniConfig));
                config.suggestConfig().contactDomainSuggest(true);
                return config.build();
            }
        };
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=0",
                MsearchProxyCluster.blackboxResponse(1, 0, "mdb200"));

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            MailUser user =
                new MailUser(0, "pg", "united-suggest@gmail.com");

            String[] emailsFrom = {
                "vas@yandex.ru",
                "von@yandex.ru",
                "vos@yandex.ru",
                "abc@yadex.ru",
                "cde@yadex.ru",
                "fgh@yadex.ru",
            };

            String[] mids = {
                "100504", "100502",
                "100505", "100506", "100507",
                "100508"
            };

            int midInd = 0;
            for (String email: emailsFrom) {
                String displayName = "From";
                Email mail = new Email(mids[midInd])
                    .from(displayName, email)
                    .to(user.email(), user.email());

                async.indexDoc(cluster, user, mail);
                midInd ++;
            }

            QueryConstructor qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "yad");

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());

                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"\\\"From\\\" fgh@yadex.ru\", "
                            + "\"display_name\": \"From\", "
                            + "\"email\": \"fgh@yadex.ru\", "
                            + "\"search_params\":{},"
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"fgh@yadex.ru\"},"
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"\\\"From\\\" cde@yadex.ru\", "
                            + "\"display_name\": \"From\", "
                            + "\"search_params\":{},"
                            + "\"email\": \"cde@yadex.ru\", "
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"cde@yadex.ru\"},"
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"\\\"From\\\" abc@yadex.ru\", "
                            + "\"display_name\": \"From\", "
                            + "\"search_params\":{},"
                            + "\"email\": \"abc@yadex.ru\", "
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"abc@yadex.ru\"},"
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"yadex.ru\", "
                            + "\"display_name\": \"yadex.ru\", "
                            + "\"email\": \"yadex.ru\", "
                            + "\"search_params\":{},"
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"yadex.ru\"}"
                            + "]"),
                    responseTxt);
            }

            qc = new QueryConstructor("/api/async/mail/suggest?");
            qc.append("mdb", "mdb200");
            qc.append("suid", "0");
            qc.append("request", "yand");

            try (CloseableHttpResponse response = client.execute(
                cluster.proxy().host(), new HttpGet(qc.toString())))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseTxt = CharsetUtils.toString(
                    response.getEntity());

                YandexAssert.check(
                    new JsonChecker(
                        "["
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"\\\"From\\\" vos@yandex.ru\", "
                            + "\"display_name\": \"From\", "
                            + "\"search_params\":{},"
                            + "\"email\": \"vos@yandex.ru\", "
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"vos@yandex.ru\"},"
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"\\\"From\\\" von@yandex.ru\", "
                            + "\"display_name\": \"From\", "
                            + "\"search_params\":{},"
                            + "\"email\": \"von@yandex.ru\", "
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"von@yandex.ru\"},"
                            + "{\"target\": \"contact\", "
                            + "\"show_text\": \"\\\"From\\\" vas@yandex.ru\", "
                            + "\"display_name\": \"From\", "
                            + "\"search_params\":{},"
                            + "\"email\": \"vas@yandex.ru\", "
                            + "\"unread_cnt\": 0, "
                            + "\"search_text\": \"vas@yandex.ru\"}"
                            + "]"),
                    responseTxt);
            }
        }
    }

    @Test
    public void testContactSuggestLimit() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&emails=getall"
                + "&sid=2&uid=1",
                MsearchProxyCluster.blackboxResponse(1, 1, "pg"));

            MailUser user =
                new MailUser(1, "pg", "suggest.user3@yandex.ru");
            final int fromCnt = 8;
            final int toCnt = 8;

            String[] emailsFrom = new String[fromCnt];
            String[] emailsTo= new String[toCnt];

            for (int i = 0; i < fromCnt; i++ ) {
                emailsFrom[i] = "from" + i + "@yandex.ru";
            }

            for (int i = 0; i < toCnt; i++ ) {
                emailsTo[i] = "to" + i + "@yandex.ru";
            }

            old.sendFromContactsByEmails(cluster, user, emailsFrom);
            old.sendToContactsByRcpts(cluster, user, emailsTo);

            final String query = "yandex&limit=13";
            final HttpHost host = cluster.proxy().host();
            // Not working for old suggest
            //List<Contact> contacts =
            //      old.suggestRaw(client, host, user, query).contacts();
            //Assert.assertEquals(13, contacts.size());

            List<Contact> contacts =
                async.suggestRaw(client, host, user, query).contacts();
            Assert.assertEquals(13, contacts.size());
        }
    }

    @Test
    public void testNoAddress() throws Exception {
        MailUser user = new MailUser(34, "pg", "suggest.user34@yandex.ru");
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&emails=getall"
                + "&sid=2&uid=34",
                MsearchProxyCluster.blackboxResponse(34, 34, "pg"));

            final HttpHost host = cluster.proxy().host();

            cluster.backend().add(
                new LongPrefix(user.prefix()),
                // documents with match
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"hdr_from\": \"\\\"No address\\\" <>\\n\""
                ),
                doc(
                    "100501",
                    "\"received_date\":\"1234567890\"",
                    "\"hdr_from\": \"\\\"Petya\\\" <petya@ya.ru>\\n\","
                    + "\"hdr_to\": \"\\\"No address\\\" <>\\n\""
                ),
                doc(
                    "100502",
                    "\"received_date\":\"1234567889\"",
                    "\"hdr_from\":\"\\\"Address\\\" <address@ya.ru>\\n\""));
                // two different names with same abbreviation

            List<Contact> contacts =
                async.suggest(client, host, user, "address").contacts();

            Assert.assertEquals(1, contacts.size());
            Assert.assertEquals("Address", contacts.get(0).getName());

            contacts =
                old.suggest(client, host, user, "address").contacts();

            Assert.assertEquals(1, contacts.size());
            Assert.assertEquals("Address", contacts.get(0).getName());
        }
    }


    @Test
    public void testSuggestContacts() throws Exception {
        String SUGGEST_ROUTE = "/api/async/mail/suggest/contact";
        //String suggestRoute = "/api/suggest/";
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&emails=getall"
                + "&sid=2&uid=0",
                MsearchProxyCluster.blackboxResponse(0, 0, "pg"));

            cluster.backend().add(
                // documents with match
                doc(
                    "100500",
                    "\"received_date\":\"1234567890\"",
                    "\"hdr_from\":\"Потапов, Александр\""),
                doc(
                    "100501",
                    "\"received_date\":\"1234567889\"",
                    "\"hdr_from\":\"Саша Пушкин\""),
                // document without synonyms match
                doc(
                    "100502",
                    "\"received_date\":\"1234567888\"",
                    "\"hdr_from\":\"Ипполит Матвеевич Воробьянинов\""),
                // two different names with same abbreviation
                doc(
                    "100503",
                    "\"received_date\":\"1234567887\"",
                    "\"hdr_from\":\"Эдгар Глостер\""),
                doc(
                    "100504",
                    "\"received_date\":\"1234567886\"",
                    "\"hdr_from\":\"Эдмунд Бастард\""),
                // document alphabetically located after all known names
                doc(
                    "100505",
                    "\"received_date\":\"1234567885\"",
                    "\"hdr_from\":\"яяя\""));
//            for (String request: new String[]{"Александр", "Саша"}) {
//                try (CloseableHttpResponse response = client.execute(
//                    new HttpGet(
//                        cluster.proxy().host()
//                            + SUGGEST_ROUTE + "?uid=0&maildb=pg&q=" + request)))
//                {
//                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
//                    YandexAssert.check(
//                        new JsonChecker(
//                            "{\"contacts\":[{\"id\":-2,\"u\":-1,\"phones\":[],"
//                                + "\"email\":\"Потапов, Александр\","
//                                + "\"name\":\"Потапов, Александр\","
//                                + "\"t\":\"1234567890\"},"
//                                + "{\"id\":-2,\"u\":-1,\"email\":\""
//                                + "Саша Пушкин\",\"name\":\"Саша Пушкин"
//                                + "\",\"phones\":[],\"t\":\"1234567889\"}]}"),
//                        CharsetUtils.toString(response.getEntity()));
//                }
//            }
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + SUGGEST_ROUTE + "?uid=0&maildb=pg&q=Воробьянинов")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"contacts\":[{\"id\":-2,\"u\":-1,\"phones\":[],"
                            + "\"email\":\"Ипполит Матвеевич Воробьянинов\","
                            + "\"name\":\"Ипполит Матвеевич Воробьянинов\","
                            + "\"search_text\":\"Ипполит Матвеевич Воробьянинов\","
                            + "\"show_text\":\"\\\"Ипполит Матвеевич " +
                            "Воробьянинов\\\" Ипполит Матвеевич Воробьянинов\","
                            + "\"target\":\"contact\","
                            + "\"t\":\"1234567888\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + SUGGEST_ROUTE + "?uid=0&maildb=pg&q=эд")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"contacts\":[{\"id\":-2,\"u\":-1,\"phones\":[],"
                            + "\"email\":\"Эдгар Глостер\","
                            + "\"name\":\"Эдгар Глостер\","
                            + "\"search_text\":\"Эдгар Глостер\","
                            + "\"show_text\":\"\\\"Эдгар Глостер" +
                            "\\\" Эдгар Глостер\","
                            + "\"target\":\"contact\","
                            + "\"t\":\"1234567887\"},"
                            + "{\"id\":-2,\"u\":-1,\"email\":\""
                            + "Эдмунд Бастард\",\"name\":\"Эдмунд Бастард"
                            + "\",\"search_text\":\"Эдмунд Бастард\","
                            + "\"show_text\":\"\\\"Эдмунд Бастард" +
                            "\\\" Эдмунд Бастард\","
                            + "\"target\":\"contact\","
                            + "\"phones\":[],\"t\":\"1234567886\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
            try (CloseableHttpResponse response = client.execute(
                new HttpGet(
                    cluster.proxy().host()
                        + SUGGEST_ROUTE + "?uid=0&maildb=pg&q=яяя")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"contacts\":[{\"id\":-2,\"u\":-1,\"phones\":[],"
                            + "\"email\":\"яяя\",\"name\":\"яяя\","
                            + "\"search_text\":\"яяя\","
                            + "\"show_text\":\"\\\"яяя" +
                            "\\\" яяя\","
                            + "\"target\":\"contact\","
                            + "\"t\":\"1234567885\"}]}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    private void checkRanking(
        final List<Contact> contacts,
        final List<Contact> expected)
        throws Exception
    {
        int index = 0;
        while (index < expected.size()) {
            Contact exp = expected.get(index);
            Contact got = contacts.get(index);
            if (exp.equals(got)) {
                index += 1;
                continue;
            }

            if ((index + 1) < expected.size()
                && expected.get(index + 1).getT().equals(exp.getT()))
            {
                int eqTCount = 0;
                Contact next = expected.get(index + 1);
                while (exp.getT().equals(next.getT())) {
                    eqTCount += 1;
                    if (index + eqTCount + 1 >= expected.size()) {
                        break;
                    }

                    exp = expected.get(index + eqTCount);
                    next = contacts.get(index + eqTCount + 1);
                }

                for (int i = index; i <= eqTCount + index; i++) {
                    exp = expected.get(i);
                    int gotIndex = contacts.indexOf(exp);
                    int max = index + eqTCount;
                    Assert.assertTrue(
                        exp.toString()
                            + " got index "
                            + gotIndex
                            + " but expected positive and less "
                            + max,
                        gotIndex >= 0 && gotIndex <= max);
                }

                index += eqTCount + 1;
            } else {
                throw new AssertionError(
                    "Expected " + exp.toString()
                        + " but got " + got.toString());
            }
        }
    }

    @Test
    public void testTranslit() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.backend().add(
                prefix,
                "\"url\":\"10500/0\","
                + "\"received_date\":1,"
                + "\"hdr_from\": "
                + "\"\\\"Алина Васильева\\\" <leskivn@yandex.ru>\","
                + "\"hdr_to\": "
                + "\"\\\"search (рассылка)\\\" <search@yandex.ru>\"");

            String nameSearchResult = '['
                + getSearchResult("Алина Васильева", "leskivn@yandex.ru", "алина")
                + ']';

            String emailSearchResult = '['
                + getSearchResult("Алина Васильева", "leskivn@yandex.ru", "leskivn")
                + ']';

            String emptySearchResult = "[]";

            // test mobile
            final String mobileUri =
                "/api/async/mail/suggest?suid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&side=mobile&request=";

            checkResponse(cluster, client, mobileUri + "alina", nameSearchResult);
            checkResponse(cluster, client, mobileUri + "fkbyf", emptySearchResult);
            checkResponse(cluster, client, mobileUri + "фдштф", emptySearchResult);
            checkResponse(cluster, client, mobileUri + "лескивн", emailSearchResult);
            checkResponse(cluster, client, mobileUri + "дуылшмт", emptySearchResult);
            checkResponse(cluster, client, mobileUri + "ktcrbdy", emptySearchResult);

            // test web
            final String webUri =
                "/api/async/mail/suggest?suid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&side=webpdd&request=";

            checkResponse(cluster, client, webUri + "alina", nameSearchResult);
            checkResponse(cluster, client, webUri + "fkbyf", nameSearchResult);
            checkResponse(cluster, client, webUri + "фдштф", emptySearchResult);
            checkResponse(cluster, client, webUri + "лескивн", emailSearchResult);
            checkResponse(cluster, client, webUri + "дуылшмт", emailSearchResult);
            checkResponse(cluster, client, webUri + "ktcrbdy", emptySearchResult);
        }
    }

    @Test
    public void testSearchOnlyByLogins() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"received_date\":1,"
                + "\"hdr_from\": "
                + "\"\\\"Lamoda.ru\\\" <support@lamoda.com>\","
                + "\"hdr_to\": "
                + "\"\\\"Maria\\\" <maria@yandex.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"received_date\":1,"
                + "\"hdr_from\": "
                + "\"\\\"Maria\\\" <maria@yandex.ru>\","
                + "\"hdr_to\": "
                + "\"\\\"Anna Rus\\\" <anna-rus@gmail.com>\"");

            String searchResult = '['
                + getSearchResult("Anna Rus", "anna-rus@gmail.com", "ru") + ']';

            String emptySearchResult = "[]";

            final String uri =
                "/api/async/mail/suggest?suid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&side=web"
                + "&request=";

            checkResponse(cluster, client, uri + "ru", searchResult);
            checkResponse(cluster, client, uri + "ру", searchResult);
            checkResponse(cluster, client, uri + "com", emptySearchResult);
        }
    }

    @Test
    public void testSearchMultipleContacts() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=1111111&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2"
                + "&sid=2&uid=" + prefix,
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111110",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111110",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            final String uri =
                "/api/async/mail/suggest?uid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&side=web&request=";

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"received_date\":1,"
                + "\"hdr_from\": \""
                + "\\\"\\\" <wjehfhr@hsjd.su>\\n"
                + "\\\"Lamoda.ru-\\\" <support@lamoda.ru>\\n"
                + "\\\"Gmail\\\" <google@gmail.com>\\n"
                + "\\\"masha\\\" <masha@yandex.ru>\\n"
                + "\","
                + "\"hdr_to\": \""
                + "\\\"a.russkih\\\" <lamoda@yandex.ru>\\n"
                + "\\\"Maria\\\" <maria@yandex.ru>\\n"
                + "\"");

            String searchResultYa = '['
                + getSearchResult("Maria", "maria@yandex.ru", null) + ','
                + getSearchResult("a.russkih", "lamoda@yandex.ru", null) + ','
                + getSearchResult("masha", "masha@yandex.ru", null)
                + ']';

            checkResponse(cluster, client, uri + "%40ya", searchResultYa);
            checkResponse(cluster, client, uri + "%40yan", searchResultYa);

            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"received_date\":1,"
                + "\"hdr_from\": \""
                + "\\\"Maria\\\" <maria@yandex.ru>\\n"
                + "\\\"\\\" <alla.rudneva@yandex.ru>\\n"
                + "<anna.rudneva@yandex.ru>\\n"
                + "\","
                + "\"hdr_to\": \""
                + "\\\"Sberbank.ru\\\" <noreply@sberbank.ru>\\n"
                + "\\\"Alisa\\\" <alisa@yandex.ru>\\n"
                + "\\\"Anna Rus\\\" <anna@gmail.com>\\n"
                + "\\\"anna-rus@gmail.com\\\" <anna-rus@gmail.com>\\n"
                + "\\\"Lamoda Service\\\" <>\\n"
                + "\","
                + "\"hdr_cc\": \""
                + "\\\"Sberbank.ru\\\" <noreply@sberbank.ru>\\n"
                + "\\\"Alisa\\\" <alisa@yandex.ru>\\n"
                + "gmgmg@yandex.ru\\n"
                + "\\\"Alisa Ivanova\\\" <alisa@lamoda.ru>\\n"
                + "\\\"Dalai Lama\\\"\\n"
                + "\","
                + "\"reply_to\": \""
                + "\\\"alex-ruuq@gmail.com\\\" <alex-ruuq@gmail.com>\\n"
                + "\"");

            String searchResultRu = '['
                + getSearchResult("Anna Rus", "anna@gmail.com", "ru") + ','
                + getSearchResult("a.russkih", "lamoda@yandex.ru", "ru") + ','
                + getSearchResult("alla.rudneva", "alla.rudneva@yandex.ru", "ru") + ','
                + getSearchResult("anna.rudneva", "anna.rudneva@yandex.ru", "ru") + ','
                + getSearchResult("anna-rus", "anna-rus@gmail.com", "ru")
                + ']';

            checkResponse(cluster, client, uri + "ru", searchResultRu);
            checkResponse(cluster, client, uri + "ру", searchResultRu);
            checkResponse(cluster, client, uri + "com", "[]");

            String searchResultGm = '['
                + getSearchResult("Gmail", "google@gmail.com", "gm") + ','
                + getSearchResult("gmgmg", "gmgmg@yandex.ru", "gm")
                + ']';

            checkResponse(cluster, client, uri + "gm", searchResultGm);

            String searchResultGmail = '['
                + getSearchResult("Anna Rus", "anna@gmail.com", "gmail") + ','
                + getSearchResult("Gmail", "google@gmail.com", "gmail") + ','
                + getSearchResult("anna-rus", "anna-rus@gmail.com", "gmail")
                + ']';

            checkResponse(cluster, client, uri + "gmail", searchResultGmail);

            String searchResultLam = '['
                + getSearchResult("Alisa Ivanova", "alisa@lamoda.ru", "lam") + ','
                + getSearchResult("Dalai Lama", "Dalai Lama", "lam") + ','
                + getSearchResult("Lamoda Service", "Lamoda Service", "lam") + ','
                + getSearchResult("Lamoda.ru-", "support@lamoda.ru", "lam") + ','
                + getSearchResult("a.russkih", "lamoda@yandex.ru", "lam")
                + ']';

            checkResponse(cluster, client, uri + "lam", searchResultLam);

            String searchResultSup = '['
                + getSearchResult("Lamoda.ru-", "support@lamoda.ru", "sup")
                + ']';

            checkResponse(cluster, client, uri + "sup", searchResultSup);
        }
    }

    @Test
    public void testSearchMultipleContactsCorp() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=1111111&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1120000000000001L);

            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2"
                + "&sid=2&uid=" + prefix,
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2"
                + "&suid=1120000000000000",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            cluster.corpBlackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2"
                + "&suid=1120000000000000",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            final String uri =
                "/api/async/mail/suggest?uid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&side=web&request=";

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"received_date\":1,"
                + "\"fid\":1,"
                + "\"hdr_from\": \""
                + "\\\"\\\" <.@cx.su>\\n"
                + "__ <wjehfhr@hsjd.su>\\n"
                + "\\\"Gmail\\\" <google@gmail.com>\\n"
                + "\\\"masha ruk\\\" <masha@yandex-team.ru>\\n"
                + "\","
                + "\"hdr_to\": \""
                + "\\\"a.russkih\\\" <aaa@yandex-team.ru>\\n"
                + "\\\"Maria\\\" <maria@yandex-team.ru>\\n"
                + "\\\"Yandex Music\\\" <music@noreply.com>\\n"
                + "\\\"Yandex Taxi\\\" <taxi@yandex-team.ru>\\n"
                + "\"");

            String searchResultYa = '['
                + getSearchResult("Maria", "maria@yandex-team.ru", "@ya") + ','
                + getSearchResult("Yandex Taxi", "taxi@yandex-team.ru", "@ya") + ','
                + getSearchResult("a.russkih", "aaa@yandex-team.ru", "@ya") + ','
                + getSearchResult("masha ruk", "masha@yandex-team.ru", "@ya")
                + ']';

            checkResponse(cluster, client, uri + "%40ya", searchResultYa);

            String searchResultYan = '['
                + getSearchResult("Yandex Music", "music@noreply.com", "yan") + ','
                + getSearchResult("Yandex Taxi", "taxi@yandex-team.ru", "yan")
                + ']';

            checkResponse(cluster, client, uri + "yan", searchResultYan);

            String searchResultRu = '['
                + getSearchResult("a.russkih", "aaa@yandex-team.ru", "ru") + ','
                + getSearchResult("masha ruk", "masha@yandex-team.ru", "ru")
                + ']';

            checkResponse(cluster, client, uri + "ru", searchResultRu);
            checkResponse(cluster, client, uri + "ру", searchResultRu);
            checkResponse(cluster, client, uri + "com", "[]");

            String searchResultMusic = '['
                + getSearchResult("Yandex Music", "music@noreply.com", "music")
                + ']';

            checkResponse(cluster, client, uri + "music", searchResultMusic);
        }
    }

    @Test
    public void testSearchIgnoreDomains() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"received_date\":1,"
                + "\"hdr_from\": "
                + "\"\\\"Lamoda.ru\\\" <support@lamoda.com>\","
                + "\"hdr_to\": "
                + "\"\\\"Maria\\\" <maria@yandex.ru>\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"received_date\":1,"
                + "\"hdr_from\": "
                + "\"\\\"Maria\\\" <maria@yandex.ru>\","
                + "\"hdr_to\": "
                + "\"\\\"Anna Rus\\\" <anna-rus@gmail.com>\"");

            String emptySearchResult = "[]";

            final String uri =
                "/api/async/mail/suggest?suid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&side=web&request=";

            checkResponse(cluster, client, uri + "yan", emptySearchResult);
            checkResponse(cluster, client, uri + "gma", emptySearchResult);
        }
    }

    @Test
    public void testSynonyms() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix() - 1,
                    prefix.prefix(),
                    "mdb100"));

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"received_date\":1,"
                + "\"hdr_from\": "
                + "\"\\\"Екатерина София\\\" <k1234@yandex.ru>\","
                + "\"hdr_to\": "
                + "\"\\\"Мария\\\" <m4829@yandex.ru>\"");

            String searchResult = '['
                + getSearchResult("Екатерина София", "k1234@yandex.ru", null)
                + ']';

            String emptySearchResult = "[]";

            final String uri =
                "/api/async/mail/suggest?suid=" + prefix.toString()
                + "&highlight&limit=10&types=contact,mail,subject&side=web"
                + "&request=";

            checkResponse(cluster, client, uri + "катя", searchResult);
            checkResponse(cluster, client, uri + "катя+софия+", searchResult);

            // rules over limit - empty result
            checkResponse(cluster, client, uri + "катя+катя+катя+софия+", emptySearchResult);

            // check finished tokens
            checkResponse(cluster, client, uri + "катюшк", searchResult);
            checkResponse(cluster, client, uri + "катюшка+софия", searchResult);
            checkResponse(cluster, client, uri + "катюшк+", emptySearchResult);

            // check large request
            String almostLargeRequest =
                "екатерина+екатерина+екатерина+екатерина+екатерина+екатерина+";
            checkResponse(cluster, client, uri + almostLargeRequest, searchResult);

            String largeRequest = almostLargeRequest + "екатерина+екатерина+";
            checkResponse(cluster, client, uri + largeRequest, emptySearchResult);
        }
    }

    @Test
    public void testSearchWithPriority() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2"
                + "&sid=2&uid=" + prefix,
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() - 1,
                    "pg"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111110",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix() -1,
                    "pg"));

            final String uri =
                "/api/async/mail/suggest?uid=" + prefix.toString()
                + "&highlight&type=contact&side=web&request=";

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"received_date\":1,"
                + "\"hdr_from\": \""
                + "\\\"\\\" <wjehfhr@hsjd.su>\\n"
                + "\\\"Lamoda.ru-\\\" <support@lamoda.ru>\\n"
                + "\\\"Gmail\\\" <google@gmail.com>\\n"
                + "\\\"masha\\\" <masha@yandex.ru>\\n"
                + "\","
                + "\"hdr_to\": \""
                + "\\\"a.russkih\\\" <lamoda@yandex.ru>\\n"
                + "\\\"Maria\\\" <maria@yandex.ru>\\n"
                + "\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"received_date\":1,"
                + "\"hdr_from\": \""
                + "\\\"Maria\\\" <maria@yandex.ru>\\n"
                + "\\\"\\\" <alla.rudneva@yandex.ru>\\n"
                + "<anna.rudneva@yandex.ru>\\n"
                + "\","
                + "\"hdr_to\": \""
                + "\\\"Sberbank.ru\\\" <noreply@sberbank.ru>\\n"
                + "\\\"Alisa\\\" <alisa@yandex.ru>\\n"
                + "\\\"Anna Rus\\\" <anna@gmail.com>\\n"
                + "\\\"anna-rus@gmail.com\\\" <anna-rus@gmail.com>\\n"
                + "\\\"Lamoda Service\\\" <>\\n"
                + "\","
                + "\"hdr_cc\": \""
                + "\\\"Sberbank.ru\\\" <noreply@sberbank.ru>\\n"
                + "\\\"Alisa\\\" <alisa@yandex.ru>\\n"
                + "gmgmg@yandex.ru\\n"
                + "\\\"Alisa Ivanova\\\" <alisa@lamoda.ru>\\n"
                + "\\\"Dalai Lama\\\"\\n"
                + "\","
                + "\"reply_to\": \""
                + "\\\"alex-ruuq@gmail.com\\\" <alex-ruuq@gmail.com>\\n"
                + "\"");

            String searchResultRu = '['
                + getSearchResult("Anna Rus", "anna@gmail.com", "ru") + ','
                + getSearchResult("a.russkih", "lamoda@yandex.ru", "ru") + ','
                + getSearchResult("alla.rudneva", "alla.rudneva@yandex.ru", "ru") + ','
                + getSearchResult("anna.rudneva", "anna.rudneva@yandex.ru", "ru") + ','
                + getSearchResult("anna-rus", "anna-rus@gmail.com", "ru")
                + ']';

            String searchResultRuHdrTo = '['
                + getSearchResult("Anna Rus", "anna@gmail.com", "ru") + ','
                + getSearchResult("a.russkih", "lamoda@yandex.ru", "ru") + ','
                + getSearchResult("anna-rus", "anna-rus@gmail.com", "ru")
                + ']';

            checkResponse(cluster, client, uri + "ru&limit=3", searchResultRuHdrTo);
            checkResponse(cluster, client, uri + "ru&limit=10", searchResultRu);


            String searchResultAlisa = '['
                + getSearchResult("Alisa Ivanova", "alisa@lamoda.ru", "alisa") + ','
                + getSearchResult("Alisa", "alisa@yandex.ru", "alisa")
                + ']';

            String searchResultAlisaOne = '['
                + getSearchResult("Alisa", "alisa@yandex.ru", "alisa")
                + ']';

            checkResponse(cluster, client, uri + "alisa&limit=1", searchResultAlisaOne);
            checkResponse(cluster, client, uri + "alisa&limit=3", searchResultAlisa);
        }
    }

    @Test
    public void testRanking() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&sid=2"
                + "&uid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix(),
                    "pg"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix(),
                    "pg"));

            DateTime currentDate = new DateTime();
            long currentTs = currentDate.getMillis() / 1000;
            long monthAgoTs = currentDate.minusMonths(1).getMillis() / 1000;
            long monthsAgoTs = currentDate.minusMonths(10).getMillis() / 1000;
            long yearAgoTs = currentDate.minusYears(1).getMillis() / 1000;

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":0,"
                + "\"hdr_from\": \"\\\"Maria1\\\" <maria1@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria1@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria2\\\" <maria2@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria2@yandex.ru\\n\","
                + "\"message_type\": \"5 eticket 16 s_aviaeticket\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + monthAgoTs + ","
                + "\"hdr_from\": \"\\\"Maria3\\\" <maria3@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria3@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria4\\\" <maria4@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria4@yandex.ru\\n\","
                + "\"message_type\": \"51 trust_1 4 people\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"3\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + yearAgoTs + ","
                + "\"hdr_from\": \"\\\"Maria5\\\" <maria5@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria5@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria6\\\" <maria6@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria6@yandex.ru\\n\","
                + "\"message_type\": \"5 eticket 4 people 16 s_aviaeticket\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria1@yandex.ru\","
                + "\"senders_received_count\": \"0\","
                + "\"senders_sent_count\": \"0\","
                + "\"senders_last_contacted\": \"" + currentTs + "\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria2@yandex.ru\","
                + "\"senders_received_count\": \"150\","
                + "\"senders_sent_count\": \"287\","
                + "\"senders_last_contacted\": \"" + currentTs + "\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria3@yandex.ru\","
                + "\"senders_received_count\": \"1090\","
                + "\"senders_sent_count\": \"287\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria4@yandex.ru\","
                + "\"senders_received_count\": \"10\","
                + "\"senders_last_contacted\": \"" + monthsAgoTs + "\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria5@yandex.ru\","
                + "\"senders_received_count\": \"4\","
                + "\"senders_sent_count\": \"287\","
                + "\"senders_last_contacted\": \"" + yearAgoTs + "\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria6@yandex.ru\","
                + "\"senders_received_count\": \"0\","
                + "\"senders_sent_count\": \"0\","
                + "\"senders_last_contacted\": \"" + monthsAgoTs + "\"");

            final String uri =
                "/api/async/mail/suggest?uid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&request=";

            String result = '[' + suggestRankingResult(2) + ','
                +  suggestRankingResult(3) + ','
                +  suggestRankingResult(1) + ','
                +  suggestRankingResult(6) + ','
                +  suggestRankingResult(4) + ','
                +  suggestRankingResult(5) + ']';
            checkResponse(cluster, client, uri + "m", result);
        }
    }

    @Test
    public void testRankingSameEmails() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&sid=2"
                + "&uid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix(),
                    "pg"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix(),
                    "pg"));

            DateTime currentDate = new DateTime();
            long currentTs = currentDate.getMillis() / 1000;
            long monthAgoTs = currentDate.minusMonths(1).getMillis() / 1000;
            long twoMonthsAgoTs = currentDate.minusMonths(2).getMillis() / 1000;
            long yearAgoTs = currentDate.minusYears(1).getMillis() / 1000;

            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + currentTs + ","
                + "\"hdr_from\": \"\\\"Maria1\\\" <maria1@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria1@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria2\\\" <maria2@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria2@yandex.ru\\n\","
                + "\"message_type\": \"51 trust_1\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + monthAgoTs + ","
                + "\"hdr_from\": \"\\\"Maria2\\\" <maria2@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria2@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria1\\\" <maria1@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria1@yandex.ru\\n\","
                + "\"message_type\": \"51 trust_1 4 people\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"3\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + twoMonthsAgoTs + ","
                + "\"hdr_from\": \"\\\"Maria3\\\" <maria3@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria3@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria2\\\" <maria2@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria2@yandex.ru\\n\","
                + "\"message_type\": \"4 people\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria2@yandex.ru\","
                + "\"senders_received_count\": \"0\","
                + "\"senders_sent_count\": \"0\","
                + "\"senders_last_contacted\": \"" + yearAgoTs + "\"");

            final String uri =
                "/api/async/mail/suggest?uid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&request=";

            String result = '[' + suggestRankingResult(1) + ','
                            +  suggestRankingResult(3) + ','
                            +  suggestRankingResult(2) + ']';
            checkResponse(cluster, client, uri + "m", result);
        }
    }

    @Test
    public void testRankingSelfEmails() throws Exception {
        try (MsearchProxyCluster cluster = new MsearchProxyCluster(this, true);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            cluster.producer().add(
                "/_status?service=opqueue&prefix=0&allow_cached"
                + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");

            LongPrefix prefix = new LongPrefix(1111111L);

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2,subscription.suid.2&sid=2"
                + "&uid=1111111",
                MsearchProxyCluster.blackboxResponse(
                    prefix.prefix(),
                    prefix.prefix(),
                    "pg"));

            cluster.blackbox().add(
                "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
                + "&dbfields=hosts.db_id.2&emails=getall&sid=2&suid=1111111",
                "{\"users\":["
                    + "{\"id\":\"1111111\","
                    + "\"uid\":"
                        + "{\"value\":\"1111111\","
                        + "\"lite\":false,"
                        + "\"hosted\":false},"
                    + "\"login\":\"maria1\","
                    + "\"have_password\":true,"
                    + "\"have_hint\":false,"
                    + "\"karma\":{\"value\":0},"
                    + "\"karma_status\":{\"value\":0},"
                    + "\"dbfields\":"
                        + "{\"subscription.suid.2\":\"1111111\","
                        + "\"hosts.db_id.2\":\"pg\"},"
                    + "\"address-list\":[{"
                        + "\"born-date\":\"2018-12-13 18:10:17\","
                        + "\"native\":true,"
                        + "\"unsafe\":false,"
                        + "\"silent\":false,"
                        + "\"rpop\":false,"
                        + "\"default\":false,"
                        + "\"validated\":true,"
                        + "\"address\":\"maria1@ya.ru\""
                    + "},{"
                        + "\"born-date\":\"2018-12-13 18:10:17\","
                        + "\"native\":true,"
                        + "\"unsafe\":false,"
                        + "\"silent\":false,"
                        + "\"rpop\":false,"
                        + "\"default\":false,"
                        + "\"validated\":true,"
                        + "\"address\":\"maria1@yandex.ru\""
                    + "},{"
                        + "\"born-date\":\"2018-12-13 18:10:17\","
                        + "\"native\":true,"
                        + "\"unsafe\":false,"
                        + "\"silent\":false,"
                        + "\"rpop\":false,"
                        + "\"default\":false,"
                        + "\"validated\":true,"
                        + "\"address\":\"maria1@yandex.com\""
                    + "}"
                + "]}]}");

            DateTime currentDate = new DateTime();
            long currentTs = currentDate.getMillis() / 1000;
            long monthAgoTs = currentDate.minusMonths(1).getMillis() / 1000;


            // maria1 boost 18, but self email - boost 0
            // maria2 boost 11
            cluster.backend().add(
                prefix,
                "\"url\":\"1\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + monthAgoTs + ","
                + "\"hdr_from\": \"\\\"Maria2\\\" <maria2@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria2@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria1\\\" <maria1@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria1@yandex.ru\\n\","
                + "\"message_type\": \"51 trust_1\"");

            // maria3 boost 14
            cluster.backend().add(
                prefix,
                "\"url\":\"2\","
                + "\"uid\":\"1111111\","
                + "\"received_date\":" + monthAgoTs + ","
                + "\"hdr_from\": \"\\\"Maria3\\\" <maria3@yandex.ru>\","
                + "\"hdr_from_normalized\": \"maria3@yandex.ru\\n\","
                + "\"hdr_to\": \"\\\"Maria1\\\" <maria1@yandex.ru>\","
                + "\"hdr_to_normalized\": \"maria1@yandex.ru\\n\","
                + "\"message_type\": \"51 trust_1 5 eticket "
                + "16 s_aviaeticket\"");

            cluster.backend().add(
                prefix,
                "\"url\":\"senders_uid_1111111_maria1@yandex.ru\","
                + "\"senders_received_count\": \"120\","
                + "\"senders_sent_count\": \"462\","
                + "\"senders_last_contacted\": \"" + currentTs + "\"");

            final String uri =
                "/api/async/mail/suggest?uid=" + prefix.toString()
                + "&highlight&limit=10&type=contact&request=";

            String result = '[' + suggestRankingResult(3) + ','
                            +  suggestRankingResult(2) + ','
                            +  suggestRankingResult(1) + ']';
            checkResponse(cluster, client, uri + "m", result);
        }
    }

    private String suggestRankingResult(final int n) {
        return "{"
            + "\"display_name\": \"Maria" + n + "\","
            + "\"display_name_highlighted\": "
                + "\"<span class=\\\"msearch-highlight\\\">M</span>aria"
                + n + "\","
            + "\"email\": \"maria" + n + "@yandex.ru\","
            + "\"email_highlighted\": \"<span class="
                + "\\\"msearch-highlight\\\">m</span>aria" + n + "@yandex.ru\","
            + "\"search_params\": {},"
            + "\"search_text\": \"maria" + n + "@yandex.ru\","
            + "\"show_text\": \"\\\"Maria" + n + "\\\" maria" + n
                + "@yandex.ru\","
            + "\"show_text_highlighted\": \"\\\"<span class="
                + "\\\"msearch-highlight\\\">M</span>aria" + n
                + "\\\" <span class=\\\"msearch-highlight\\\">m</span>aria"
                + n + "@yandex.ru\","
            + "\"target\": \"contact\","
            + "\"unread_cnt\": 0"
        + "}";
    }

    private void checkResponse(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String uri,
        final String expected)
        throws IOException, HttpException
    {
        try (CloseableHttpResponse response = client.execute(
            new HttpGet(cluster.proxy().host() + uri)))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            String responseTxt = CharsetUtils.toString(response.getEntity());
            System.out.println("Response txt:" + responseTxt);
            YandexAssert.check(
                new JsonChecker(expected),
                responseTxt);
        }
    }

    private String getSearchResult(
        final String name,
        final String email,
        final String request)
    {
        String nameHighlighted = getHighlightedField(name, request);
        String emailHighlighted = getHighlightedField(email, request);
        return '{'
            + "\"target\":\"contact\","
            + "\"show_text\":\"\\\"" + name + "\\\" " + email + "\","
            + "\"search_text\":\"" + email + "\","
            + "\"display_name\":\"" + name + "\","
            + "\"email\":\"" + email + "\","
            + "\"unread_cnt\":0,"
            + "\"search_params\":{},"
            + "\"email_highlighted\":\"" + emailHighlighted + "\","
            + "\"display_name_highlighted\":\"" + nameHighlighted + "\","
            + "\"show_text_highlighted\":\"\\\"" + nameHighlighted + "\\\" "
            + emailHighlighted + '\"'
            + '}';
    }

    private String getHighlightedField(
        final String field,
        final String request)
    {
        if (request == null) {
            return field;
        }
        int indx = field.toLowerCase().indexOf(request.toLowerCase());
        if (indx == -1) {
            return field;
        } else {
            return field.substring(0, indx)
                + "<span class=\\\"msearch-highlight\\\">"
                + field.substring(indx, indx + request.length())
                + "</span>"
                + field.substring(indx + request.length());
        }
    }
}
