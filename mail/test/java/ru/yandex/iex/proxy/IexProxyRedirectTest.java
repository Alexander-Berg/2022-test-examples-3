package ru.yandex.iex.proxy;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.dbfields.OracleFields;
import ru.yandex.dbfields.PgFields;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class IexProxyRedirectTest extends TestBase {
    private static final String LCN = "123";
    private static final String UID_PARAM = "&uid=";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String SUBJECT = "test subject";
    private static final String URI_PATTERN_TTEOT = "/notify-tteot?*";
    private static final String ENTITIES_DEFAULT = "entities.default = events";
    private static final String STORE_PARAMS = "storefs.json";
    private static final String PG = "pg";
    private static final Long UID_VALUE = BlackboxUserinfo.CORP_UID_BEGIN;

    @Test
    public void testRedirect() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            HttpPost post = new HttpPost(
                HTTP_LOCALHOST + cluster.iexproxy().port()
                + NOTIFY + PG + "&service=old");
            String to = "\"Test \" <a861@ya.ru>";
            post.addHeader(YandexHeaders.RETRY_COUNT, "5555");
            Map<String, Object> request = new HashMap<>();
            request.put(OracleFields.SUBJECT, SUBJECT);
            request.put(OracleFields.LCN, LCN);
            request.put(OracleFields.FROM, to);
            post.setEntity(
                new StringEntity(
                    JsonType.NORMAL.toString(request),
                    ContentType.APPLICATION_JSON));
            cluster.producerAsyncClient().add(
                    URI_PATTERN_TTEOT,
                    new ExpectingHttpItem(new JsonChecker(request)));
            System.out.println("producer port is: "
                    + cluster.producerAsyncClient().port());
            System.out.println("producer uri pattern is: " + URI_PATTERN_TTEOT);
            HttpAssert.assertStatusCode(HttpStatus.SC_ACCEPTED, client, post);
        }
    }

    @Test
    public void testRedirectSmallRetryCounter() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(
                    this,
                    null,
                    ENTITIES_DEFAULT,
                    true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String mid = "100505";
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                "{\"users\":[{\"id\":\"220660041221\","
                + "\"uid\":{},\"karma\":{\"value\":0},"
                + "\"karma_status\":{\"value\":0}}]}");
            HttpPost post = notifyPost(cluster.iexproxy().port(), mid);
            post.addHeader(YandexHeaders.RETRY_COUNT, "5");
            HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, client, post);
        }
    }

    @Test
    public void testRedirectRetryCounterNotNumberIgnore() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(
                    this,
                    null,
                    ENTITIES_DEFAULT,
                    true);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String suid = "9006";
            String mid = "100507";
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(
                    UID_VALUE,
                    "aasdasd@aasdasd",
                    suid,
                    PG));
            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            String fsUri = "/filter_search?*";
            cluster.corpFilterSearch().add(
                fsUri,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            HttpPost post = notifyPost(cluster.iexproxy().port(), mid);
            post.addHeader(YandexHeaders.RETRY_COUNT, "5555d");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
        }
    }

    private HttpPost notifyPost(final int port, final String mid) {
        HttpPost post = new HttpPost(HTTP_LOCALHOST + port + NOTIFY + PG);
        String opId = "48";
        final long operationDate = 1234567890L;
        Map<String, Object> request = new HashMap<>();
        request.put(PgFields.UID, UID_VALUE);
        request.put(PgFields.CHANGE_TYPE, "iex-update");
        request.put(PgFields.CHANGED, Collections.singletonList(
            Collections.singletonMap(OracleFields.MID, mid)));
        request.put(OracleFields.MDB, PG);
        request.put(OracleFields.OPERATION_ID, opId);
        request.put(OracleFields.MID, mid);
        request.put(OracleFields.OPERATION_DATE, operationDate);
        post.setEntity(
            new StringEntity(
                JsonType.NORMAL.toString(request),
                ContentType.APPLICATION_JSON));
        return post;
    }
}
