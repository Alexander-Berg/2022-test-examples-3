package ru.yandex.search.msal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.search.msal.mock.DataType;
import ru.yandex.search.msal.mock.StaticDatabase;
import ru.yandex.search.msal.mock.StaticTable;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class MessagesTableTest extends MsalTestBase {
    // CSOFF: MultipleStringLiterals
    @Test
    public void testMidByMessageId() throws Exception {
        try (MsalCluster cluster = new MsalCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();

            Map<String, DataType> meta =
                Collections.singletonMap("mid", DataType.VARCHAR);

            cluster.sharpei().add(
                "/conninfo?format=json&uid=300&mode=write_only",
                "{\"id\":2095,\"name\":\"xdb305\","
                    + "\"databases\":[{\"address\":{\"host\":\"first-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"MYT\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"second-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"SAS\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"master-node\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"IVA\"},\"role\":\"master\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":0}}]}");

            StaticDatabase replica =
                cluster.db().create(
                    "jdbc:postgresql://first-replica:6432/maildb");
            StaticTable table =
                replica.create("mail.messages", meta);
            String sql =
                "select mid from mail.messages where uid = 300"
                    + " and hdr_message_id = "
                    + "<E1fKtLn-hNxIDx-LM@ucs101-ucs-7.msgpanel.com>";

            Map<String, String> row1 =
                generateData("123512351235", meta);

            table.onetime(sql, Collections.singletonList(row1));

            HttpHost msal =
                new HttpHost("localhost", cluster.msal().port());

            QueryConstructor qc =
                new QueryConstructor("/get-mid-by-message-id?");

            qc.append("uid", "300");
            qc.append(
                "message-id",
                "<E1fKtLn-hNxIDx-LM@ucs101-ucs-7.msgpanel.com>");

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + qc.toString())))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "123512351235",
                    CharsetUtils.toString(response.getEntity()));
            }

            table.onetime(
                sql,
                Arrays.asList(generateData("500", meta),
                    generateData("123512351235", meta)));

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + qc.toString())))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "500",
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + qc.toString())))
            {
                Assert.assertEquals(
                    HttpStatus.SC_BAD_GATEWAY,
                    response.getStatusLine().getStatusCode());
            }

            table.onetime(sql, Collections.emptyList());

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + qc.toString())))
            {
                Assert.assertEquals(
                    HttpStatus.SC_NOT_FOUND,
                    response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testListShards() throws Exception {
        String sharpeiBody = "{\"2276\":{\"databases\":[{\"address"
            + "\":{\"dbname\":\"maildb\",\"port\":\"6432\","
            + "\"dataCenter\":\"IVA\",\"host\":\"xdb15e.mail.yandex.net\"},"
            + "\"role\":\"replica\",\"state\":{\"lag\":0},"
            + "\"status\":\"alive\"},{\"address\":{\"dbname\":\"maildb\","
            + "\"port\":\"6432\",\"dataCenter\":\"MYT\",\"host\":\"xdb15f"
            + ".mail.yandex.net\"},\"role\":\"master\",\"state\":{\"lag\":0},"
            + "\"status\":\"alive\"},{\"address\":{\"dbname\":\"maildb\","
            + "\"port\":\"6432\",\"dataCenter\":\"SAS\",\"host\":\"xdb15h"
            + ".mail.yandex.net\"},\"role\":\"replica\","
            + "\"state\":{\"lag\":0},\"status\":\"alive\"}],"
            + "\"name\":\"xdb15\",\"id\":\"2276\"},"
            + "\"2430\":{\"databases\":[{\"address\":{\"dbname\":\"maildb\","
            + "\"port\":\"6432\",\"dataCenter\":\"SAS\",\"host\":\"xdb414h"
            + ".mail.yandex.net\"},\"role\":\"replica\","
            + "\"state\":{\"lag\":0},\"status\":\"alive\"},"
            + "{\"address\":{\"dbname\":\"maildb\",\"port\":\"6432\","
            + "\"dataCenter\":\"MAN\",\"host\":\"xdb414i.mail.yandex.net\"},"
            + "\"role\":\"replica\",\"state\":{\"lag\":0},"
            + "\"status\":\"alive\"},{\"address\":{\"dbname\":\"maildb\","
            + "\"port\":\"6432\",\"dataCenter\":\"VLA\",\"host\":\"xdb414k"
            + ".mail.yandex.net\"},\"role\":\"master\",\"state\":{\"lag\":0},"
            + "\"status\":\"alive\"}],\"name\":\"xdb414\",\"id\":\"2430\"}}";

        try (MsalCluster cluster = new MsalCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            cluster.sharpei().add("/v2/stat", sharpeiBody);
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             cluster.msal().host().toString()
                                 + "/list-shards")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                YandexAssert.check(
                    new JsonChecker("[\"2276\", \"2430\"]"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testFirstline() throws Exception {
        try (MsalCluster cluster = new MsalCluster();
             CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            Map<String, DataType> meta =
                Collections.singletonMap("firstline", DataType.VARCHAR);

            cluster.sharpei().add(
                "/conninfo?format=json&uid=350&mode=read_write",
                "{\"id\":2095,\"name\":\"xdb305\","
                    + "\"databases\":[{\"address\":{\"host\":\"first-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"MYT\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"second-replica\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"SAS\"},\"role\":\"replica\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":1}},"
                    + "{\"address\":{\"host\":\"master-node\","
                    + "\"port\":6432,\"dbname\":\"maildb\","
                    + "\"dataCenter\":\"IVA\"},\"role\":\"master\","
                    + "\"status\":\"alive\",\"state\":{\"lag\":0}}]}");

            StaticDatabase replica =
                cluster.db().create(
                    "jdbc:postgresql://first-replica:6432/maildb");
            StaticTable table =
                replica.create("mail.messages", meta);

            Map<String, String> row1 =
                generateData("cool firstline", meta);

            String sql =
                "select firstline from mail.messages "
                    + "where mid = 870 and uid = 350";
            table.onetime(sql, Collections.singletonList(row1));

            HttpHost msal =
                new HttpHost("localhost", cluster.msal().port());

            // testing simple request
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + "/get-firstline?mid=870&uid=350")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());

                Assert.assertEquals(
                    "cool firstline",
                    CharsetUtils.toString(response.getEntity()));
            }

            // testing simple request
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + "/get-firstline?mid=870&uid=350")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_BAD_GATEWAY,
                    response.getStatusLine().getStatusCode());
            }

            table.onetime(sql, Collections.emptyList());
            // testing not found
            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + "/get-firstline?mid=870&uid=350")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_NOT_FOUND,
                    response.getStatusLine().getStatusCode());
            }

            try (CloseableHttpResponse response =
                     client.execute(
                         new HttpGet(
                             msal + "/get-firstline?&uid=350")))
            {
                Assert.assertEquals(
                    HttpStatus.SC_BAD_REQUEST,
                    response.getStatusLine().getStatusCode());
            }
        }
    }
    // CSON: MultipleStringLiterals
}
