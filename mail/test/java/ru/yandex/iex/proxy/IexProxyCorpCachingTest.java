package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.dbfields.ChangeType;
import ru.yandex.dbfields.OracleFields;
import ru.yandex.dbfields.PgFields;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class IexProxyCorpCachingTest extends TestBase {
    private static final String UID_PARAM = "&uid=";
    private static final String PG = "pg";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String NOTIFY = "/notify?mdb=";
    private static final String FILTER_SEARCH = "/filter_search?*";
    private static final String STORE_PARAMS = "storefsora.json";
    private static final String STORE_FS_USER_NOT_INITIALIZED =
        "storefs_user_not_initialized.json";
    private static final String IEX_UPDATE_CHANGE_TYPE =
        ChangeType.IEX_UPDATE.name().toLowerCase(Locale.ROOT).replace('_', '-');
    private static final String STORE_PARAMS_WITH_TYPES =
        "storefsora_types.json";

    private static final long UID_VALUE = BlackboxUserinfo.CORP_UID_BEGIN;

    //CSOFF: MultipleStringLiterals
    @Test
    public void testNotifyNoTypesForCoke() throws Exception {
        UnaryOperator<String> configPostProcessor = config ->
            config.replace("entities.default = contentline\n", "");
        try (IexProxyCluster cluster =
             new IexProxyCluster(
                 this,
                 null,
                 "",
                 configPostProcessor,
                 false,
                 false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String suid = "9006";
            String mid = "100506";
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@b.c", suid, PG));

            FileEntity entity = new FileEntity(
                new File(getClass().getResource(STORE_PARAMS).toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                FILTER_SEARCH,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            HttpPost post = notifyPost(cluster.iexproxy().port(), mid);
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println("response:\n" + entityString);
                String checkUri = "/search?prefix=" + UID_VALUE + '&'
                     + "service=iex&get=fact_name,"
                     + "fact_data,fact_mid&text=fact_mid:(" + mid + ')';
                String stringToCompare = TestSearchBackend.prepareResult(
                     "\"fact_name\": \"no_facts\", \"fact_data\": null,"
                         + "\"fact_mid\": \"" + mid + '\"');
                cluster.testLucene().checkSearch(checkUri, stringToCompare);
            }
        }
    }

    @Test
    public void testNotifyWithFacts() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            String suid = "9006";
            String mid = "100506";
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@b.c", suid, PG));
            FileEntity entity = new FileEntity(
                new File(
                    getClass().getResource(STORE_PARAMS_WITH_TYPES).
                    toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                FILTER_SEARCH,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            cluster.producerAsyncClient().add(
                "/add*",
                new ProxyHandler(cluster.testLucene().indexerPort())
            );
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            HttpPost post = notifyPost(cluster.iexproxy().port(), mid);
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println("response:\n" + entityString);
                String checkUri = "/search?prefix=" + UID_VALUE + '&'
                     + "service=iex&sort=fact_name&asc&get=fact_name,"
                     + "fact_data,fact_mid&text=fact_mid:(" + mid + ')';
                String fileName = "lucene_cache.json";
                String stringToCompare = stringToLucene(fileName);
                System.out.println("Expecting that lucene will contain:\n"
                    + stringToCompare);
                cluster.testLucene().checkSearch(checkUri, stringToCompare);
            }
        }
    }

    @Test
    public void testUserNotInitialized() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();
            String suid = "9006";
            String mid = "100506";
            String blackboxUri = IexProxyCluster.blackboxUri(
                UID_PARAM + UID_VALUE);
            cluster.corpBlackbox().add(
                blackboxUri,
                IexProxyCluster.blackboxResponse(UID_VALUE, "a@b.c", suid, PG));

            FileEntity entity = new FileEntity(
                new File(
                    getClass().getResource(STORE_FS_USER_NOT_INITIALIZED).
                    toURI()),
                ContentType.APPLICATION_JSON);
            cluster.corpFilterSearch().add(
                FILTER_SEARCH,
                new StaticHttpItem(HttpStatus.SC_OK, entity));
            FileEntity entityFromCoke = new FileEntity(
                new File(getClass().
                    getResource("response_from_coke.json").toURI()),
                ContentType.APPLICATION_JSON);
            cluster.cokemulatorIexlib().add(
                "/process?*",
                new StaticHttpItem(HttpStatus.SC_OK, entityFromCoke));
            HttpPost post = notifyPost(cluster.iexproxy().port(), mid);
            try (CloseableHttpResponse response = client.execute(post)) {
                String entityString =
                    CharsetUtils.toString(response.getEntity());
                System.out.println("response:\n" + entityString);
                String checkUri = "/search?prefix=" + UID_VALUE + '&'
                     + "service=iex&get=fact_name,"
                     + "fact_data,fact_mid&text=fact_mid:(" + mid + ')';
                String fileName = "lucene_empty_cache.json";
                String stringToCompare = stringToLucene(fileName);
                System.out.println("Expecting that lucene will contain:\n"
                    + stringToCompare);
                cluster.testLucene().checkSearch(checkUri, stringToCompare);
            }
        }
    }

    private HttpPost notifyPost(final int port, final String mid) {
        HttpPost post = new HttpPost(HTTP_LOCALHOST + port + NOTIFY + PG);
        String opId = "48";
        final long operationDate = 1234567890L;
        Map<String, Object> request = new HashMap<>();
        request.put(PgFields.UID, UID_VALUE);
        request.put(PgFields.CHANGE_TYPE, IEX_UPDATE_CHANGE_TYPE);
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

    private String stringToLucene(final String fileName) {
        try {
            Path path = Paths.get(getClass().getResource(fileName).toURI());
            return java.nio.file.Files.readString(path);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
