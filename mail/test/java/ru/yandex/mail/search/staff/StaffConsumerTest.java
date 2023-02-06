package ru.yandex.mail.search.staff;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import ru.yandex.ace.ventura.AceVenturaPrefix;
import ru.yandex.ace.ventura.UserType;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.async.consumer.JsonAsyncTypesafeDomConsumerFactory;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class StaffConsumerTest extends TestBase {
    private static final long TIMEOUT = 10000;
    private static final long INTERVAL = 100;
    private static final String LOCK_NAME = "localhost@1";
    private static final String SERVICE = "aceventura_change_log_prod";
    private static final String PRODUCER_NAME = "staff_consumer";

    private static final String FIELDS =
        "login,work_email,official.is_dismissed,official.is_robot,official.position," +
            "department_group.id,department_group.ancestors.id,department_group.level," +
            "language,accounts,uid,id,name";

    private static final String STAFF_URI_BASE =
        "/v3/persons?&_fields=" + FIELDS;

    private static final String STAFF_SORT = "&_sort=id";

    private static final String LOCK_URI =
        "/_producer_lock?service=" + SERVICE
            + "&session-timeout=120000&producer-name=" + PRODUCER_NAME;
    private static final String POSITION_URI =
        "/_producer_position?service=" + SERVICE
            + "&producer-name=" + PRODUCER_NAME;
    // CSOFF: MultipleStringLiterals

    @Test
    public void test() throws Exception {
        StaticServer server = new StaticServer(Configs.baseConfig());
        server.start();
        server.add(
            "/disk/handler",
            new File(
                StaffConsumerTest.class.getResource("bad.json.out").toURI()));
        SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
            Configs.baseConfig(),
            Configs.dnsConfig());
        reactor.start();
        AsyncClient client =
            new AsyncClient(
                reactor,
                new HttpTargetConfigBuilder(Configs.targetConfig()).build());
        client.start();
        JsonObject result = client.execute(
           server.host(),
            new BasicAsyncRequestProducerGenerator("/disk/handler"),
            JsonAsyncTypesafeDomConsumerFactory.OK,
            EmptyFutureCallback.INSTANCE).get();
        System.out.println(JsonType.HUMAN_READABLE.toString(result));
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

            String firstUri = staffRangeUri(cluster, 1);
            String secondUri = staffRangeUri(cluster, 854);

            // first contains only robot
            cluster.staff().add(
                firstUri,
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue),
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        staffResResponse("simple/staff_response_1.json", null)),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue));

            cluster.staff().add(
                secondUri,
                new StaticHttpResource(
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(
                        staffResResponse("simple/staff_response_2.json", null)),
                    HttpHeaders.AUTHORIZATION,
                    expectAuthValue)));

            cluster.staff().add(
                staffRangeUri(cluster, 19605),
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new StaticHttpItem(
                            staffResponse("[]", null)),
                        HttpHeaders.AUTHORIZATION,
                        expectAuthValue)));

            String producerNotifyUri = "/notify*";
            cluster.producer().add(
                producerNotifyUri,
                new StaticHttpResource(
                    new ExpectingHeaderHttpItem(
                        new ProxyMultipartHandler(
                            cluster.lucene().indexerPort()),
                        new BasicHeader(
                            YandexHeaders.PRODUCER_NAME,
                            PRODUCER_NAME),
                        new BasicHeader(
                            YandexHeaders.SERVICE,
                            SERVICE))));

            AceVenturaPrefix sharedPrefix =
                new AceVenturaPrefix(2L, UserType.CONNECT_ORGANIZATION);

            AceVenturaPrefix persPrefix =
                new AceVenturaPrefix(1120000000040290L, UserType.PASSPORT_USER);

            cluster.lucene().add(
                sharedPrefix,
                "\"id\":\"ace_vonidu_doc_id\", \"av_email\":\"vonidu@yandex-team.ru\"",
                "\"id\":\"ace_tabolin_doc_id\", \"av_email\":\"tabolin@yandex-team.ru\"",
                "\"id\":\"ace_dpotapov_doc_id\", \"av_email\":\"dpotapov@yandex-team.ru\"");
            cluster.lucene().add(
                persPrefix,
                "\"id\": \"av_email_1120000000040290_passport_user_1\", "
                    + "\"av_record_type\":\"email\","
                    + "\"av_email\":\"tabolin@yandex-team.ru\","
                    + "\"av_is_corp\":\"true\""
            );
            cluster.lucene().flush();
            cluster.start();
            waitProducerRequests(cluster.producer(), producerNotifyUri, 2);

            cluster.lucene().checkSearch(
                "/search?text=id:*&get=*,__prefix&length=10",
                new JsonChecker(loadResource("simple/expected.json")));
        }
    }

    protected String staffSingleUri(
        final StaffConsumerCluster cluster,
        final int position,
        final int page)
        throws IOException
    {
        String result = STAFF_URI_BASE;
        if (page > 0) {
            result += "&page=" + page;
        }

        result += "&_query=_meta.message_id%3D%3D" + position;
        return result;
    }

    protected String staffRangeUri(
        final StaffConsumerCluster cluster,
        final int position)
        throws IOException
    {
        String result = STAFF_URI_BASE;

        result += "&_query=id%3E" + position + STAFF_SORT;
        return result;
    }

    protected String staffResResponse(
        final String resource,
        final String next)
        throws IOException
    {
        return staffResponse(loadResource(resource), next);
    }

    protected String staffResponse(
        final String resultArray,
        final String next)
    {
        String result =  "{\"links\": {\"last\": \"last_we_not_use\"";
        if (next != null) {
            result += ", \"next\": \"" + next + "\"";
        }
        result += "}, \"page\": 2, \"limit\": 2, \"result\": ";
        result += resultArray;
        result += ",\"total\": 3, \"pages\": 2}";

        return result;
    }

//    @Test
//    public void testComplex() throws Exception {
//        try (StaffConsumerCluster cluster = new StaffConsumerCluster(this)) {
//            cluster.producer().add(
//                LOCK_URI + '*',
//                new StaticHttpResource(new StaticHttpItem(LOCK_NAME)));
//
//            final String position = "0";
//            cluster.producer().add(POSITION_URI, new StaticHttpResource(new StaticHttpItem(position)));
//
//            String expectAuthValue = "OAuth " + cluster.token();
//
//            String last =
//                cluster.staff().host().toString() + staffSingleUri(cluster, 1, 2);
//
//            String staffSinglePage0 =
//                "{\"links\":{},\"page\": 1, \"limit\": 2, \"result\":[], \"total\": 0, \"pages\": 1}";
//
//            String staffSinglePage1 =
//                "{\"links\":{\"last\":\"" + last
//                    + "\",\"next\": \"" + last + "\"},"
//                    + "\"page\": 1, \"limit\": 2, \"result\":"
//                    + loadResource("complex/staff_single_mode_1_page_1.json")
//                    + ", \"total\": 3, \"pages\": 2}";
//
//            String staffSinglePage2 =
//                "{\"links\": {\"last\": \"" + last + "\"}, "
//                    + "\"page\": 2, \"limit\": 2, \"result\": "
//                    + loadResource("complex/staff_single_mode_1_page_2.json")
//                    + ",\"total\": 3, \"pages\": 2}";
//
//            HttpRequestHandler staffDbOverflow = new ExpectingHeaderHttpItem(
//                new StaticHttpItem(
//                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
//                    "Sort operation used more than the maximum 33554432 bytes of RAM." +
//                        " Add an index, or specify a smaller limit"),
//                HttpHeaders.AUTHORIZATION,
//                expectAuthValue);
//
//            cluster.staff().add(
//                staffRangeUri(cluster, 0, 0),
//                staffDbOverflow);
//            cluster.staff().add(
//                staffRangeUri(cluster, 1, 0),
//                staffDbOverflow);
//
//            cluster.staff().add(
//                staffSingleUri(cluster, 0, 0),
//                new ExpectingHeaderHttpItem(
//                    new StaticHttpItem(staffSinglePage0),
//                    HttpHeaders.AUTHORIZATION,
//                    expectAuthValue));
//            cluster.staff().add(
//                staffSingleUri(cluster, 1, 0),
//                new ExpectingHeaderHttpItem(
//                    new StaticHttpItem(staffSinglePage1),
//                    HttpHeaders.AUTHORIZATION,
//                    expectAuthValue));
//            cluster.staff().add(
//                staffSingleUri(cluster, 1, 2),
//                new ExpectingHeaderHttpItem(
//                    new StaticHttpItem(staffSinglePage2),
//                    HttpHeaders.AUTHORIZATION,
//                    expectAuthValue));
//
//            cluster.staff().add(
//                staffRangeUri(cluster, 2, 0),
//                new StaticHttpResource(
//                    new ExpectingHeaderHttpItem(
//                        new StaticHttpItem(
//                            staffResResponse("complex/staff_response_2.json", null)),
//                        HttpHeaders.AUTHORIZATION,
//                        expectAuthValue)));
//
//            cluster.staff().add(
//                staffRangeUri(cluster, 3, 0),
//                new StaticHttpResource(
//                    new ExpectingHeaderHttpItem(
//                        new StaticHttpItem(staffResponse("[]", null)),
//                        HttpHeaders.AUTHORIZATION,
//                        expectAuthValue)));
//
//            String producerNotifyUri = "/notify*";
//            cluster.producer().add(
//                producerNotifyUri,
//                new StaticHttpResource(
//                    new ExpectingHeaderHttpItem(
//                        new ProxyMultipartHandler(
//                            cluster.lucene().indexerPort()),
//                        new BasicHeader(
//                            YandexHeaders.PRODUCER_NAME,
//                            PRODUCER_NAME),
//                        new BasicHeader(
//                            YandexHeaders.SERVICE,
//                            SERVICE))));
//
//            AceVenturaPrefix sharedPrefix =
//                new AceVenturaPrefix(2L, UserType.CONNECT_ORGANIZATION);
//
//            cluster.lucene().add(
//                sharedPrefix,
//                "\"id\":\"ace_vonidu_doc_id\", \"av_email\":\"vonidu@yandex-team.ru\"");
//            cluster.start();
//            waitProducerRequests(cluster.producer(), producerNotifyUri, 2);
//
//            cluster.lucene().checkSearch(
//                "/search?text=id:*&get=*,__prefix&length=10",
//                new JsonChecker(loadResource("complex/expected.json")));
//        }
//    }

    private static void waitProducerRequests(
        final StaticServer producer,
        final String uri,
        final int count)
        throws Exception {
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

    private String loadResource(final String name) throws IOException {
        return IOStreamUtils.consume(
            new InputStreamReader(
                this.getClass().getResourceAsStream(name),
                StandardCharsets.UTF_8))
            .toString();
    }
}
