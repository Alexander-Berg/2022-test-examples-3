package ru.yandex.search.msal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.dom.JsonDouble;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.search.msal.mock.DataType;
import ru.yandex.search.msal.mock.StaticDatabase;
import ru.yandex.search.msal.mock.StaticTable;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class OperationsQueueTest extends MsalTestBase {
    private static final Map<String, DataType> CHANGE_LOG_META;

    // CSOFF: MultipleStringLiterals
    static {
        Map<String, DataType> meta = new LinkedHashMap<>();
        meta.put("operation_id", DataType.INTEGER);
        meta.put("uid", DataType.INTEGER);
        meta.put("lcn", DataType.INTEGER);
        meta.put("change_type", DataType.VARCHAR);
        meta.put("operation_date", DataType.TIMESTAMP);
        meta.put("changed", DataType.VARCHAR);
        meta.put("arguments", DataType.VARCHAR);
        meta.put("fresh_count", DataType.INTEGER);
        meta.put("useful_new_messages", DataType.INTEGER);
        meta.put("db_user", DataType.VARCHAR);
        meta.put("session_key", DataType.VARCHAR);
        CHANGE_LOG_META = Collections.unmodifiableMap(meta);
    }

    private static final String SELECT =
        "select cid as operation_id, uid, revision as lcn, "
        + "type as change_type, change_date as operation_date, "
        + "changed, arguments, fresh_count, "
        + "useful_new_count as useful_new_messages, db_user, session_key";

    private static final String EXPECTED =
        "{\"rows\": ["
            + "        {"
            + "            \"operation_id\": \"327066761\","
            + "            \"uid\": \"1120000000005153\","
            + "            \"lcn\": \"981178\","
            + "            \"change_type\": \"update\","
            + "            \"changed\": [{"
            + "                    \"fid\": 1,"
            + "                    \"deleted\": false,"
            + "                    \"mid\": 166070236260296660,"
            + "                    \"recent\": false,"
            + "                    \"tid\": 166070236260288270,"
            + "                    \"lids\": [9, 20,22],"
            + "                    \"seen\": false"
            + "                },"
            + "                {"
            + "                    \"fid\": 1,"
            + "                    \"deleted\": false,"
            + "                    \"mid\": 166070236260296846,"
            + "                    \"recent\": false,"
            + "                    \"tid\": 166070236260285047,"
            + "                    \"lids\": [9,20,22],"
            + "                    \"seen\": false"
            + "                }],"
            + "            \"arguments\": {"
            + "                \"deleted\": null,"
            + "                \"lids_del\": [],"
            + "                \"recent\": false,"
            + "                \"lids_add\": [],"
            + "                \"seen\": null},"
            + "            \"fresh_count\": \"4855\","
            + "            \"useful_new_messages\": \"633440\","
            + "            \"pgshard\": \"1\","
            + "            \"select_date\": 0,"
            + "            \"db_user\": \"mops\","
            + "            \"session_key\": \"LIZA-57745244-1652461882809\""
            + "        }]}";

    private static void replaceSelectDate(
        final JsonMap result,
        final long startTs)
        throws Exception
    {
        long endTs =
            TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis());

        for (JsonObject jo: result.getList("rows")) {
            long sd = (long) jo.asMap().getDouble("select_date");
            Assert.assertTrue(
                "Bad select time "
                    + startTs + " <= " + sd + " =>" + startTs,
                (sd >= startTs) && (endTs >= sd));
            jo.asMap().replace("select_date", new JsonDouble(0));
        }
    }

    @Test
    public void testOperationsQueue() throws Exception {
        try (MsalCluster cluster = new MsalCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            long startTs =
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            String statOut =
                "{\"1\":{\"id\":\"1\",\"name\":\"xdb1001\","
                    + "\"databases\":[{\"address\":"
                    + "{\"host\":\"master-node\",\"port\":\"6432\","
                    + "\"dbname\":\"maildb\",\"dataCenter\":\"MYT\"},"
                    + "\"role\":\"master\",\"status\":\"alive\","
                    + "\"state\":{\"lag\":0}},{\"address\":{"
                    + "\"host\":\"first-replica\",\"port\":\"6432\","
                    + "\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"IVA\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"second-replica\","
                    + "\"port\":\"6432\",\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"SAS\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}}]}}";
            cluster.sharpei().add("/v2/stat?shard_id=1", statOut);
            cluster.sharpei().add("/v2/stat", statOut);

            StaticDatabase replica =
                cluster.db().create(
                    "jdbc:postgresql://first-replica:6432/maildb");
            StaticTable table =
                replica.create("mail.change_log", CHANGE_LOG_META);

            String sql = SELECT + " from mail.change_log "
                + "where cid >= 0 order by cid limit 10";

            Map<String, String> row1 = generateData(
                "327066761 | 1120000000005153 | 981178 | update  | "
                    + "2018-07-06 00:00:00.438625+03 | [{\"fid\": 1, \"mid\":"
                    + " 166070236260296660, \"tid\": 166070236260288270, "
                    + "\"lids\": [9, 20, 22], \"seen\": false, \"recent\": "
                    + "false, \"deleted\": false}, {\"fid\": 1, \"mid\": "
                    + "166070236260296846, \"tid\": 166070236260285047, "
                    + "\"lids\": [9, 20, 22], \"seen\": false, \"recent\": "
                    + "false, \"deleted\": false}] | {\"seen\": null, "
                    + "\"recent\": false, \"deleted\": null, \"lids_add\": "
                    + "[], \"lids_del\": []} | 4855 | 633440 | mops | "
                    + "LIZA-57745244-1652461882809",
                CHANGE_LOG_META);

            table.onetime(sql, Collections.singletonList(row1));

            HttpHost msalHost =
                new HttpHost("localhost", cluster.msal().port());

            String request = msalHost.toString()
                + "/operations-queue-envelopes?hr"
                + "&namespace=operations_queue&"
                + "&length=10&op-id=0&pgshard=1";

            // testing simple request
            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(request)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                JsonMap result =
                    TypesafeValueContentHandler.parse(
                        CharsetUtils.toString(response.getEntity())).asMap();

                replaceSelectDate(result, startTs);

                YandexAssert.check(
                    new JsonChecker(EXPECTED),
                    JsonType.NORMAL.toString(result));
            }

            System.out.println("Simple test passed");
            // test empty response from database
            table.onetime(sql, Collections.emptyList());

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(request)))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                YandexAssert.check(
                    new JsonChecker("{\"rows\":[]}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            System.out.println("Empty test passed");
            //test fail on SqlException
            table = cluster.db()
                .create("jdbc:postgresql://master-node:6432/maildb")
                .create("mail.change_log", CHANGE_LOG_META);

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(request + "&no-ro")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_BAD_GATEWAY,
                    response.getStatusLine().getStatusCode());
            } catch (Exception e) {
                // Ignore
            }

            System.out.println("Exception test passed");
            //test no-ro option
            table.onetime(sql, Collections.singletonList(row1));

            try (CloseableHttpResponse response =
                     client.execute(new HttpGet(request + "&no-ro")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                JsonMap result =
                    TypesafeValueContentHandler.parse(
                        CharsetUtils.toString(response.getEntity())).asMap();
                replaceSelectDate(result, startTs);
                YandexAssert.check(
                    new JsonChecker(EXPECTED),
                    JsonType.NORMAL.toString(result));
            }

            System.out.println("NoRo test passed");
            //test date option
            table =
                replica.create(
                    "mail.change_log_p2018_07_06",
                    CHANGE_LOG_META);

            sql = SELECT + " from mail.change_log_p2018_07_06 "
                + "where cid >= 0 order by cid limit 10";
            table.onetime(sql, Collections.singletonList(row1));

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(request + "&date=2018_07_06")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                JsonMap result =
                    TypesafeValueContentHandler.parse(
                        CharsetUtils.toString(response.getEntity())).asMap();
                replaceSelectDate(result, startTs);
                YandexAssert.check(
                    new JsonChecker(EXPECTED),
                    JsonType.NORMAL.toString(result));
            }
        }
    }
    // CSON: MultipleStringLiterals
}
