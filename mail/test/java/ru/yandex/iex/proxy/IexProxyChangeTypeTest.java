package ru.yandex.iex.proxy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Pattern;
import ru.yandex.dbfields.OracleFields;
import ru.yandex.dbfields.PgFields;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.HeadersParser;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.TestBase;

public class IexProxyChangeTypeTest extends TestBase {
    private static final String IEX_UPDATE = "iex_update";
    private static final String MID = "mid";
    private static final String UIDN = "&uid=";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String LOCATION = "Location";
    private static final String STORE_PARAMS = "storefs.json";

    // CSOFF: MethodLength
    @Test
    public void testChangeTypeUpdate() throws Exception {
        final String cfg = "extrasettings.axis-facts = _ticket, events";
        try (IexProxyCluster cluster = new IexProxyCluster(this, null, cfg);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String mdb = "pg";
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port() + NOTIFY + mdb
                + "&pgshard=2082&operation-id=869408973&operation-date="
                + "1471595291.614835&uid=30357681&change-type=iex_update"
                + "&reindex");
            String uid = "30357681";
            String pgshard = "2082";
            String lcn = "32262";
            String opId = "49";
            final long operationDate = 1234567890L;
            String blackboxUri = IexProxyCluster.blackboxUri(UIDN + uid);
            cluster.blackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(1, "a@b.d", uid));
            Map<String, Object> request = new HashMap<>();
            request.put(PgFields.UID, uid);
            request.put(PgFields.PGSHARD, pgshard);
            request.put("lcn", lcn);
            request.put(OracleFields.OPERATION_DATE, operationDate);
            request.put(OracleFields.OPERATION_ID, opId);
            request.put(PgFields.CHANGE_TYPE, IEX_UPDATE);
            List<Map<String, String>> list =
                new ArrayList<Map<String, String>>();
            Map<String, String> mid1 = new HashMap<String, String>();
            mid1.put(MID, "123");
            Map<String, String> mid2 = new HashMap<String, String>();
            mid2.put(MID, "1234");
            Map<String, String> mid3 = new HashMap<String, String>();
            mid3.put(MID, "12345");
            Map<String, String> mid4 = new HashMap<String, String>();
            mid4.put(MID, "123456");
            Map<String, String> mid5 = new HashMap<String, String>();
            String fifthMid = "1234567";
            mid5.put(MID, fifthMid);
            list.add(mid1);
            list.add(mid2);
            list.add(mid3);
            list.add(mid4);
            list.add(mid5);
            request.put(PgFields.CHANGED, list);
            post.setEntity(
                new StringEntity(
                    JsonType.NORMAL.toString(request),
                    ContentType.APPLICATION_JSON));
            post.addHeader(YandexHeaders.ZOO_QUEUE, IEX_UPDATE);
            cluster.producerAsyncClient().add(
                "/add*",
                new ExpectingHeaderHttpItem(
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    YandexHeaders.X_INDEX_OPERATION_QUEUE,
                    cluster.iexproxy().xIndexOperationQueueNameUpdate())
            );
            String fsUri = "/filter_search?order=default"
                + "&full_folders_and_labels=1&uid=30357681&mdb=pg&mids=";
            logger.info("fsUri = " + fsUri);
            String firstFSUri = fsUri + "123&mids=1234";
            String secondFSUri = fsUri + "123456&mids=12345";
            String thirdFSUri = fsUri + fifthMid;
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            // as batch size is 2 put to FS
            // 3 entities: first for 1 and 2 mids, second - for 3 and 4, third
            // - for 5th.
            cluster.filterSearch().add(
                firstFSUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.filterSearch().add(
                secondFSUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.filterSearch().add(
                thirdFSUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            try (CloseableHttpResponse response = client.execute(post)) {
                logger.info("/notify first response = " + response);
                cluster.producerAsyncClient().register(
                    new Pattern<>("/add", false),
                    new ProxyHandler(cluster.testLucene().indexerPort()),
                    RequestHandlerMapper.POST);
                Assert.assertEquals(
                        HttpStatus.SC_TEMPORARY_REDIRECT,
                        response.getStatusLine().getStatusCode());
                String location = new HeadersParser(response).getOrNull(
                        LOCATION);
                logger.info("location = " + location);
                Assert.assertEquals(1, cluster.filterSearch().
                    accessCount(firstFSUri));
                Assert.assertEquals(0, cluster.filterSearch().
                    accessCount(secondFSUri));
                Assert.assertEquals(0, cluster.filterSearch().
                    accessCount(thirdFSUri));
                HttpPost secondPost = new HttpPost(
                    HTTP_LOCALHOST + cluster.iexproxy().port() + location);
                secondPost.setEntity(
                    new StringEntity(
                        JsonType.NORMAL.toString(request),
                        ContentType.APPLICATION_JSON));
                CloseableHttpResponse secondResponse =
                    client.execute(secondPost);
                logger.info("/notify second response = " + secondResponse);
                Assert.assertEquals(
                    HttpStatus.SC_TEMPORARY_REDIRECT,
                    secondResponse.getStatusLine().getStatusCode());
                location = new HeadersParser(secondResponse).getOrNull(LOCATION);
                logger.info("second location = " + location);
                Assert.assertEquals(1, cluster.filterSearch().
                    accessCount(firstFSUri));
                Assert.assertEquals(1, cluster.filterSearch().
                    accessCount(secondFSUri));
                Assert.assertEquals(0, cluster.filterSearch().
                    accessCount(thirdFSUri));
                HttpPost thirdPost = new HttpPost(
                    HTTP_LOCALHOST + cluster.iexproxy().port() + location);
                thirdPost.setEntity(
                    new StringEntity(
                        JsonType.NORMAL.toString(request),
                        ContentType.APPLICATION_JSON));
                CloseableHttpResponse thirdResponse =
                    client.execute(thirdPost);
                logger.info("/notify third response = " + thirdResponse);
                Assert.assertEquals(
                        HttpStatus.SC_OK,
                        thirdResponse.getStatusLine().getStatusCode());
                Assert.assertEquals(1, cluster.filterSearch().
                    accessCount(firstFSUri));
                Assert.assertEquals(1, cluster.filterSearch().
                    accessCount(secondFSUri));
                Assert.assertEquals(1, cluster.filterSearch().
                    accessCount(thirdFSUri));
            }
        }
    }
    // CSON: MethodLength
}
