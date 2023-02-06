package ru.yandex.mail.search.staff;

import java.util.concurrent.TimeoutException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import org.apache.http.message.BasicHeader;

import org.junit.Test;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;

import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;

import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.lucene.TestLucene;
import ru.yandex.test.util.TestBase;

public class StaffConsumerTest extends TestBase {
    private static final long TIMEOUT = 5000;
    private static final long INTERVAL = 100;
    private static final String LOCK_NAME = "localhost@1";
    private static final String SERVICE = "corp_change_log";
    private static final String PRODUCER_NAME = "staff-consumer";
    private static final String UPDATE_REQUEST =
        "/update?&staffloader=localhost&prefix=0&service=" + SERVICE;

    private static final String STAFF_URI_BASE =
        "/v3/persons?&_fields="
            + "name,login,official,language,uid,_meta.message_id"
            + "&_query=_meta.message_id";
    private static final String STAFF_GE = "%3E%3D";
    private static final String STAFF_SORT = "&_sort=_meta.message_id";

    private static final String LOCK_URI =
        "/_lock?service=" + SERVICE
            + "&timeout=600000&name=" + PRODUCER_NAME
            + "&id=";
    private static final String POSITION_URI =
        "/_producer_position?service=" + SERVICE
                    + "&producer-name=" + PRODUCER_NAME;
    private static final String LAST_BASE =
        "/v3/persons?&_fields=name%2Clogin%2Cofficial.is_robot%2C"
        + "official.is_dismissed%2Clanguage%2Cuid%2C&_meta.message_id&_page=";

    // CSOFF: MultipleStringLiterals
    @Test
    public void test() throws Exception {
        try (StaffConsumerCluster cluster = new StaffConsumerCluster(this)) {
            cluster.producer().add(
                LOCK_URI + '*',
                new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                new StaticHttpItem(LOCK_NAME),
                new StaticHttpItem(LOCK_NAME),
                new StaticHttpItem(LOCK_NAME),
                new StaticHttpItem(LOCK_NAME));

            final String position = "1";
            cluster.producer().add(
                POSITION_URI,
                new StaticHttpItem(HttpStatus.SC_FORBIDDEN),
                new StaticHttpItem("Not A Number"),
                new StaticHttpItem(position),
                new StaticHttpItem(position));

            String expectAuthValue = "OAuth " + cluster.token();

            String lastUri =
                LAST_BASE + "2&_query=_meta.message_id%3E%3D1" + STAFF_SORT;

            String last = cluster.staff().host().toHostString() + lastUri;

            String staffPage1 =
                "{\"links\":{\"last\":\"http://" + last
                    + "\",\"next\": \"http://" + last + "\"},"
                    + "\"page\": 1, \"limit\": 2, \"result\": ["
                    + "{\"uid\": \"1120000000042189\", \"language\": "
                    + "{\"content\": \"ru\",\"ui\":\"ru\",\"native\":\"ru\"}"
                    + ",\"official\": "
                    + "{\"is_dismissed\": false, \"is_robot\": false}, "
                    + "\"_meta\": {\"message_id\": 1}, "
                    + "\"login\": \"gaetanya\", \"name\": {\"last\": "
                    + "{\"ru\": \"Ашмарина\", \"en\": \"Ashmarina\"}, "
                    + "\"first\": {\"ru\": \"Татьяна\", \"en\": \"Tatiana\"}}},"
                    + "{\"uid\": \"1120000000045952\", "
                    + "\"language\": {\"content\": \"ru\", \"ui\": \"ru\", "
                    + "\"native\": \"ru\"}, \"official\": "
                    + "{\"is_dismissed\": false, \"is_robot\": false},"
                    + " \"_meta\": {\"message_id\": 2}, "
                    + "\"login\": \"adigamova\", \"name\": {\"first\":"
                    + "{\"ru\": \"Маргарита\", \"en\": \"Margarita\"},"
                    + "\"last\": {\"ru\":\"Адигамова\",\"en\":\"Adigamova\"}}}]"
                    + ", \"total\": 3, \"pages\": 2}";

            String staffPage2 =
                "{\"links\": {\"last\": \"http://" + last + "\"}, "
                    + "\"page\": 2, \"limit\": 2, \"result\": ["
                    + "{\"_meta\": {\"message_id\": 3}, \"language\": "
                    + "{\"content\": \"\", \"ui\": \"ru\", \"native\": \"ru\"},"
                    + "\"official\":{\"is_dismissed\":true,\"is_robot\":false}"
                    + ",\"name\":{\"first\":{\"ru\":\"Дмитрий\","
                    + "\"en\":\"Dmitry\"}, \"last\": {\"ru\": \"Кудинов\", "
                    + "\"en\": \"Kudinov\"}, \"hidden_middle\": true},"
                    + "\"login\": \"dimaku\", \"uid\": \"1120000000012105\"}]"
                    + ",\"total\": 3, \"pages\": 2}";

            cluster.staff().add(
                STAFF_URI_BASE + STAFF_GE + '1' + STAFF_SORT,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue),
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(staffPage1),
                        HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            cluster.staff().add(
                lastUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(staffPage2),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            String mes1 = UPDATE_REQUEST + "&message-id=1&batch-size=1";

            cluster.producer().add(
                mes1,
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.lucene().indexerPort()),
                    new BasicHeader(
                        YandexHeaders.PRODUCER_NAME,
                        PRODUCER_NAME),
                    new BasicHeader(
                        YandexHeaders.SERVICE,
                        SERVICE),
                    new BasicHeader(
                        YandexHeaders.PRODUCER_POSITION,
                        position)));

            String mes2 = UPDATE_REQUEST + "&message-id=-1&batch-size=1";

            ExpectingHeaderHttpItem noPositionItem =
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.lucene().indexerPort()),
                    new BasicHeader(
                        YandexHeaders.PRODUCER_NAME,
                        PRODUCER_NAME),
                    new BasicHeader(
                        YandexHeaders.SERVICE,
                        SERVICE),
                    new BasicHeader(
                        YandexHeaders.PRODUCER_POSITION,
                        null));
            cluster.producer().add(mes2, noPositionItem, noPositionItem);

            cluster.start();
            waitProducerRequests(cluster.producer(), mes1, 1);
            waitProducerRequests(cluster.producer(), mes2, 2);

            final String expected = "\"url\":\"staff_1120000000045952\","
                + "\"staff_login\":\"adigamova\","
                + "\"staff_first_name_ru\":\"Маргарита\","
                + "\"staff_first_name_en\":\"Margarita\","
                + "\"staff_last_name_ru\":\"Адигамова\","
                + "\"staff_last_name_en\":\"Adigamova\","
                + "\"staff_email\":\"adigamova@yandex-team.ru\","
                + "\"staff_uid\":\"1120000000045952\","
                + "\"staff_language\":\"ru\",\"staff_dismissed\":\"false\"";
            cluster.lucene().checkSearch(
                "/search?prefix=0&text=staff_uid:1120000000045952"
                    + "&get=*&length=10",
                TestLucene.prepareResultWithPrefix(1, "", expected));
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testSortOverflow() throws Exception {
        try (StaffConsumerCluster cluster = new StaffConsumerCluster(this)) {
            cluster.producer().add(
                LOCK_URI + '*',
                new StaticHttpItem(LOCK_NAME));

            final String position = "10";
            cluster.producer().add(POSITION_URI, new StaticHttpItem(position));
            String expectAuthValue = "OAuth " + cluster.token();

            String lastUri =
                LAST_BASE + "2&_query=_meta.message_id%3E%3D10" + STAFF_SORT;
            String staffHost = cluster.staff().host().toHostString();
            String staffPage1 =
                "{\"links\":{\"last\":\"http://" + staffHost + lastUri
                    + "\",\"next\": \"http://" + staffHost + lastUri + "\"},"
                    + "\"page\": 1, \"limit\": 2, \"result\": ["
                    + "{\"uid\": \"1120000000042189\", \"language\": "
                    + "{\"content\": \"ru\",\"ui\":\"ru\",\"native\":\"ru\"}"
                    + ",\"official\":{\"is_dismissed\": false, "
                    + "\"is_robot\": false},\"_meta\": {\"message_id\": 10},"
                    + "\"login\": \"gaetanya\", \"name\": {\"last\": "
                    + "{\"ru\": \"Ашмарина\", \"en\": \"Ashmarina\"}, "
                    + "\"first\": {\"ru\": \"Татьяна\", \"en\": \"Tatiana\"}}},"
                    + "{\"uid\": \"1120000000045952\",\"language\": "
                    + "{\"content\": \"ru\", \"ui\": \"ru\",\"native\": "
                    + "\"ru\"}, \"official\":{\"is_dismissed\": false, "
                    + "\"is_robot\": false}, \"_meta\": {\"message_id\": 11}, "
                    + "\"login\": \"adigamova\", \"name\": {\"first\":"
                    + "{\"ru\": \"Маргарита\", \"en\": \"Margarita\"},"
                    + "\"last\": {\"ru\":\"Адигамова\",\"en\":\"Adigamova\"}}}]"
                    + ", \"total\": 3, \"pages\": 2}";

            cluster.staff().add(
                STAFF_URI_BASE + STAFF_GE + "10" + STAFF_SORT,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(staffPage1),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            cluster.staff().add(
                lastUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        "{\"error_message\": \"database error: "
                            + "Plan executor error during find: Overflow sort "
                            + "stage buffered data usage of 33556631 bytes "
                            + "exceeds internal limit of 33554432 bytes\"}"),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            String lastPreciseUri =
                LAST_BASE + "2&_query=_meta.message_id%3D%3D11";

            String staffPagePrecise1 =
                "{\"links\":{\"last\":\"http://" + staffHost + lastPreciseUri
                    + "\",\"next\": \"http://" + staffHost + lastPreciseUri
                    + "\"},\"page\": 1, \"limit\": 2, \"result\": [{\"uid\": "
                    + "\"1120000000030000\", \"language\": "
                    + "{\"content\": \"ru\",\"ui\":\"ru\",\"native\":\"ru\"}"
                    + ",\"official\":{\"is_dismissed\":false,"
                    + "\"is_robot\":false},\"_meta\": {\"message_id\": 11},"
                    + "\"login\": \"vonidu\", \"name\": {\"last\": "
                    + "{\"ru\": \"Дудинов\", \"en\": \"Dudinov\"}, "
                    + "\"first\": {\"ru\": \"Иван\", \"en\": \"Ivan\"}}},"
                    + "{\"uid\": \"1120000000030001\", "
                    + "\"language\": {\"content\": \"ru\", \"ui\": \"ru\", "
                    + "\"native\": \"ru\"}, \"official\": "
                    + "{\"is_dismissed\": false, \"is_robot\": false},"
                    + " \"_meta\": {\"message_id\": 11}, "
                    + "\"login\": \"okkk\", \"name\": {\"first\":"
                    + "{\"ru\": \"Олег\", \"en\": \"Oleg\"},"
                    + "\"last\": {\"ru\":\"Кузнецов\",\"en\":\"Kuznetsov\"}}}]"
                    + ", \"total\": 3, \"pages\": 2}";

            String staffPagePrecise2 =
                "{\"links\": {\"last\": \"http://" + staffHost + lastPreciseUri
                    + "\"},\"page\": 2, \"limit\": 2, \"result\": ["
                    + "{\"_meta\": {\"message_id\": 11}, \"language\": "
                    + "{\"content\": \"\", \"ui\": \"ru\", \"native\": \"ru\"},"
                    + "\"official\":{\"is_dismissed\":false,\"is_robot\":false}"
                    + ",\"name\":{\"first\":{\"ru\":\"Дмитрий\","
                    + "\"en\":\"Dmitry\"}, \"last\": {\"ru\": \"Потапов\", "
                    + "\"en\": \"Potapov\"}, \"hidden_middle\": true},"
                    + "\"login\": \"dpotapov\", \"uid\": \"1120000000030002\"}]"
                    + ",\"total\": 3, \"pages\": 2}";
            String preciseRequest =
                "/v3/persons?&_fields=name,login,official,language,uid,"
                    + "_meta.message_id&_query=_meta.message_id%3D%3D11";

            cluster.staff().add(
                preciseRequest,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(staffPagePrecise1),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            cluster.staff().add(
                lastPreciseUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(staffPagePrecise2),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            lastUri = LAST_BASE + "1&_query=_meta.message_id%3E11" + STAFF_SORT;
            String staff3 =
                "{\"links\": {\"last\": \"http://" + staffHost + lastUri
                    + "\"},\"page\": 1, \"limit\": 1, \"result\": ["
                    + "{\"_meta\": {\"message_id\": 12}, \"language\": "
                    + "{\"content\": \"\", \"ui\": \"ru\", \"native\": \"ru\"},"
                    + "\"official\":{\"is_dismissed\":false,\"is_robot\":false}"
                    + ",\"name\":{\"first\":{\"ru\":\"Юлия\","
                    + "\"en\":\"Julia\"}, \"last\": {\"ru\": \"Сергеева\", "
                    + "\"en\": \"Sergeeva\"}, \"hidden_middle\": true},"
                    + "\"login\": \"juliyas\", \"uid\": \"1120000000030003\"}]"
                    + ",\"total\": 1, \"pages\": 1}";

            cluster.staff().add(
                STAFF_URI_BASE + "%3E11" + STAFF_SORT,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(staff3),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            final String producerUri = "/update*";
            cluster.producer().add(
                producerUri,
                new StaticHttpResource(
                    new ProxyHandler(cluster.lucene().indexerPort())));

            cluster.start();
            final int expReqs = 4;
            waitProducerRequests(cluster.producer(), producerUri, expReqs);

            cluster.lucene().checkSearch(
                "/search?prefix=0&text=staff_uid:1120000000030003"
                    + "&get=*&length=10",
                TestLucene.prepareResultWithPrefix(
                    1,
                    "",
                    "\"url\":\"staff_1120000000030003\","
                        + "\"staff_login\":\"juliyas\","
                        + "\"staff_first_name_ru\":\"Юлия\","
                        + "\"staff_first_name_en\":\"Julia\","
                        + "\"staff_last_name_ru\":\"Сергеева\","
                        + "\"staff_last_name_en\":\"Sergeeva\","
                        + "\"staff_email\":\"juliyas@yandex-team.ru\","
                        + "\"staff_uid\":\"1120000000030003\","
                        + "\"staff_language\":\"ru\","
                        + "\"staff_dismissed\":\"false\""));
            cluster.lucene().checkSearch(
                "/search?prefix=0&text=staff_uid:1120000000030001"
                    + "&get=*&length=10",
                TestLucene.prepareResultWithPrefix(
                    1,
                    "",
                    "\"url\":\"staff_1120000000030001\","
                        + "\"staff_login\":\"okkk\","
                        + "\"staff_first_name_ru\":\"Олег\","
                        + "\"staff_first_name_en\":\"Oleg\","
                        + "\"staff_last_name_ru\":\"Кузнецов\","
                        + "\"staff_last_name_en\":\"Kuznetsov\","
                        + "\"staff_email\":\"okkk@yandex-team.ru\","
                        + "\"staff_uid\":\"1120000000030001\","
                        + "\"staff_language\":\"ru\","
                        + "\"staff_dismissed\":\"false\""));
            cluster.lucene().checkSearch(
                "/search?prefix=0&text=staff_uid:1120000000030000"
                    + "&get=staff_login&length=10",
                TestLucene.prepareResultWithPrefix(
                    1,
                    "",
                        "\"staff_login\":\"vonidu\""));
            cluster.lucene().checkSearch(
                "/search?prefix=0&text=staff_uid:1120000000045952"
                    + "&get=staff_login&length=10",
                TestLucene.prepareResultWithPrefix(
                    1,
                    "",
                    "\"staff_login\":\"adigamova\""));
            cluster.lucene().checkSearch(
                "/search?prefix=0&text=staff_uid:1120000000042189"
                    + "&get=staff_login&length=10",
                TestLucene.prepareResultWithPrefix(
                    1,
                    "",
                    "\"staff_login\":\"gaetanya\""));
        }
    }
    // CSON: MethodLength
    // CSON: MultipleStringLiterals

    private static void waitProducerRequests(
        final StaticServer producer,
        final String uri,
        final int count)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (producer.accessCount(uri) < count) {
            Thread.sleep(INTERVAL);
            if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new TimeoutException(
                    "Expecting " + count
                        + " requests to " + uri
                        + " but got " + producer.accessCount(uri));
            }
        }
    }
}
