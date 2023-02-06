package ru.yandex.search.mail.tupita;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class TupitaBatchedTest extends TupitaTestBase {
    // CSOFF: MultipleStringLiterals

    private void testNewFormat(
        final TupitaCluster cluster)
        throws Exception
    {
        cluster.tikaite().add(
            TIKAITE_URI,
            new StaticHttpResource(
                new ExpectingHeaderHttpItem(
                    new StaticHttpItem(TICKAITE_RSP),
                    new BasicHeader(
                        YandexHeaders.X_SRW_KEY,
                        STID),
                    new BasicHeader(
                        YandexHeaders.X_SRW_NAMESPACE,
                        SRW_NS_MAIL),
                    new BasicHeader(
                        YandexHeaders.X_SRW_KEY_TYPE,
                        KEY_STID))));

        String query1 =
            "\"query\": \"uid:227356512 AND hdr_from_email:ivan.d*\",";
        String query2 =
            "\"query\": \"uid:999999 AND hdr_from_email:ivan.d*\",";
        String query3 =
            "\"query\": \"uid:55555 AND hdr_from_email:ivan.d*\",";
        String stopFalse = "\"stop\": \"false\"}";
        String stopTrue = "\"stop\": \"true\"}";

        String request = "{\"users\": ["
            + "{\"uid\": \"227356512\",\"queries\": ["
            + "{\"id\": \"100500\"," + query1 + stopFalse
            + ",{\"id\": \"100501\"," + query1 + stopFalse
            + ",{\"id\": \"100502\"," + query1 + stopFalse
            + ",{\"id\": \"100503\"," + query1 + stopFalse
            + ",{\"id\": \"100504\"," + query1 + stopFalse
            + ",{\"id\": \"100505\"," + query1 + stopFalse
            + ",{\"id\": \"100506\"," + query1 + stopFalse
            + ",{\"id\": \"100507\"," + query1 + stopTrue
            + ",{\"id\": \"100508\"," + query1 + stopFalse
            + ",{\"id\": \"100509\"," + query1 + stopFalse
            + ",{\"id\": \"1005010\"," + query1 + stopFalse
            + "]},{\"uid\": \"55555\",\"queries\": ["
            + "{\"id\": \"100507\"," + query3 + stopFalse
            + "]},{\"uid\": \"999999\",\"queries\": ["
            + "{\"id\": \"100495\"," + query2 + stopFalse
            + ",{\"id\": \"100496\"," + query2 + stopFalse
            + ",{\"id\": \"100497\"," + query2 + stopTrue
            + ",{\"id\": \"100498\"," + query2 + stopTrue
            + ",{\"id\": \"100499\"," + query2 + stopTrue
            + ",{\"id\": \"100500\"," + query2 + stopFalse
            + ",{\"id\": \"100501\"," + query2 + stopFalse
            + ",{\"id\": \"100502\"," + query2 + stopTrue
            + ",{\"id\": \"100503\"," + query2 + stopFalse
            + ",{\"id\": \"100504\"," + query2 + stopFalse
            + ",{\"id\": \"100505\"," + query2 + stopFalse
            + ",{\"id\": \"100506\"," + query2 + stopFalse
            + ",{\"id\": \"100507\"," + query2 + stopFalse
            + ",{\"id\": \"100508\"," + query2 + stopTrue
            + "]}]" + message(STID);

        HttpPost post1 = new HttpPost(
            HTTP_LOCALHOST + cluster.tupita().port() + CHECK + UID);
        post1.addHeader(YandexHeaders.TICKET, TICKET);
        post1.setEntity(new StringEntity(request));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post1))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            YandexAssert.check(
                new JsonChecker(
                    "{\"result\": ["
                        + "{\"uid\": 227356512, \"matched_queries\": "
                        + "[\"100500\", \"100501\","
                        + "\"100502\",\"100503\",\"100504\",\"100505\","
                        + "\"100506\",\"100507\"]}, {"
                        + "\"uid\":55555,\"matched_queries\":[\"100507\"]},"
                        + "{\"uid\":999999, \"matched_queries\":"
                        + "[\"100495\",\"100496\",\"100497\"]}]}"),
                CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testBatchParsing() throws Exception {
        String config = "batched-queries-parsing.query-batch-size = 2\n"
            + "batched-queries-parsing.core-threads = 2\n"
            + "batched-queries-parsing.max-threads = 2\n";
        try (TupitaCluster cluster =
                 new TupitaCluster(this, config))
        {
            testNewFormat(cluster);
        }
    }

    @Test
    public void test() throws Exception {
        String config = "batched-queries-parsing.query-batch-size = 100\n"
            + "batched-queries-parsing.core-threads = 2\n"
            + "batched-queries-parsing.max-threads = 2\n";
        try (TupitaCluster cluster =
                 new TupitaCluster(this, config))
        {
            testNewFormat(cluster);
        }
    }
    // CSON: MultipleStringLiterals
}
