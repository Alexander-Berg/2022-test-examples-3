package ru.yandex.search.mail.indexer.pg;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.dbfields.OracleFields;
import ru.yandex.dbfields.PgFields;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HeaderHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.mail.mime.DefaultMimeConfig;
import ru.yandex.mail.mime.OverwritingBodyDescriptorBuilder;
import ru.yandex.mail.mime.Utf8FieldBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class PgIndexerTest extends TestBase {
    private static final int BUFFER_SIZE = 1024;
    private static final String ROWS = "{$rows\0:[";
    private static final String EMPTY_ROWS = ROWS + ']' + '}';
    private static final String ZERO = Integer.toString(0);
    private static final String PGSHARD = "50";
    private static final String CLEANUP = "search-mids-cleanup";
    private static final String UPDATE = "search-update";
    private static final String SERVICE = "change_log";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final Header PGSHARD_HEADER =
        new BasicHeader(YandexHeaders.PGSHARD, PGSHARD);

    @Test
    public void test() throws Exception {
        try (PgIndexerCluster cluster = new PgIndexerCluster(1);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "9000";
            String revision = "20";
            String msalRevision =
                "/get-user-revision?json-type=dollar&uid=9000";
            cluster.msal().add(msalRevision, revision);

            String msalMids = "/user-mids?json-type=dollar&uid=9000";
            cluster.msal().add(
                msalMids,
                new StaticHttpItem(EMPTY_ROWS),
                new HeaderHttpItem(
                    new StaticHttpItem(
                        ROWS
                        + "{$mid\0:1},{$mid\0:3},{$mid\0:5},{$mid\0:7},"
                        + "{$mid\0:9},{$mid\0:11},{$mid\0:13},{$mid\0:15},"
                        + "{$mid\0:17},{$mid\0:19},{$mid\0:21}]}"),
                    PGSHARD_HEADER),
                NotImplementedHttpItem.INSTANCE);

            Map<String, Object> fields = new HashMap<>();
            fields.put(OracleFields.OPERATION_ID, ZERO);
            fields.put(OracleFields.FRESH_COUNT, ZERO);
            fields.put(OracleFields.USEFUL_NEW_MESSAGES, ZERO);
            fields.put(OracleFields.LCN, revision);
            fields.put(OracleFields.OPERATION_DATE, JsonChecker.ANY_VALUE);
            fields.put(PgFields.PGSHARD, PGSHARD);
            fields.put(PgFields.UID, uid);
            fields.put(PgFields.CHANGE_TYPE, CLEANUP);

            Header[] headers = new Header[] {
                new BasicHeader(YandexHeaders.SERVICE, SERVICE),
                new BasicHeader(YandexHeaders.ZOO_SHARD_ID, uid)
            };
            String producerPrefix =
                "/notify?mdb=pg&uid=9000&revision=20&change-type=";
            String producerCleanup =
                producerPrefix
                + "search-mids-cleanup&cleanup-user&batch-size=1&mids=1-21";
            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse("[{\"mid\":1},{\"mid\":21}]"));
            cluster.producer().add(
                producerCleanup,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            fields.put(PgFields.CHANGE_TYPE, UPDATE);
            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse(
                    "[{\"mid\":1},{\"mid\":3},{\"mid\":5},{\"mid\":7}]"));
            String producerUpdate1 =
                producerPrefix + "search-update&batch-size=1&mids=1-7";
            cluster.producer().add(
                producerUpdate1,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse(
                    "[{\"mid\":7},{\"mid\":9},{\"mid\":11},{\"mid\":13}]"));
            String producerUpdate2 =
                producerPrefix + "search-update&batch-size=1&mids=7-13";
            cluster.producer().add(
                producerUpdate2,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse(
                    "[{\"mid\":13},{\"mid\":15},{\"mid\":17},{\"mid\":19}]"));
            String producerUpdate3 =
                producerPrefix + "search-update&batch-size=1&mids=13-19";
            cluster.producer().add(
                producerUpdate3,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse("[{\"mid\":19},{\"mid\":21}]"));
            String producerUpdate4 =
                producerPrefix + "search-update&batch-size=1&mids=19-21";
            cluster.producer().add(
                producerUpdate4,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    HTTP_LOCALHOST + cluster.indexer().port()
                    + "/reindex?prefix=9000"));
            Assert.assertEquals(2, cluster.msal().accessCount(msalRevision));
            Assert.assertEquals(2, cluster.msal().accessCount(msalMids));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerCleanup));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUpdate1));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUpdate2));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUpdate3));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUpdate4));
        }
    }

    @Test
    public void testClean() throws Exception {
        try (PgIndexerCluster cluster = new PgIndexerCluster(1);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "9001";
            String revision = "21";
            String msalRevision =
                "/get-user-revision?json-type=dollar&uid=9001";
            cluster.msal().add(msalRevision, revision);

            String msalMids = "/user-mids?json-type=dollar&uid=9001";
            cluster.msal().add(msalMids, EMPTY_ROWS);

            Map<String, Object> fields = new HashMap<>();
            fields.put(OracleFields.OPERATION_ID, ZERO);
            fields.put(OracleFields.FRESH_COUNT, ZERO);
            fields.put(OracleFields.USEFUL_NEW_MESSAGES, ZERO);
            fields.put(OracleFields.LCN, revision);
            fields.put(OracleFields.OPERATION_DATE, JsonChecker.ANY_VALUE);
            fields.put(PgFields.PGSHARD, null);
            fields.put(PgFields.UID, uid);
            fields.put(PgFields.CHANGE_TYPE, CLEANUP);

            Header[] headers = new Header[] {
                new BasicHeader(YandexHeaders.SERVICE, SERVICE),
                new BasicHeader(YandexHeaders.ZOO_SHARD_ID, uid)
            };

            String producerCleanup =
                "/notify?mdb=pg&uid=9001&revision=21&change-type="
                + "search-mids-cleanup&empty-user&batch-size=1&mids=2-1";
            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse("[{\"mid\":2},{\"mid\":1}]"));
            cluster.producer().add(
                producerCleanup,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    HTTP_LOCALHOST + cluster.indexer().port()
                    + "/reindex?prefix=9001"));
            Assert.assertEquals(2, cluster.msal().accessCount(msalRevision));
            Assert.assertEquals(2, cluster.msal().accessCount(msalMids));
        }
    }

    @Test
    public void testProducerBatches() throws Exception {
        try (PgIndexerCluster cluster = new PgIndexerCluster(2);
            CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "9002";
            String revision = "22";
            String msalRevision =
                "/get-user-revision?json-type=dollar&uid=9002";
            cluster.msal().add(msalRevision, revision);

            String msalMids = "/user-mids?json-type=dollar&uid=9002";
            cluster.msal().add(
                msalMids,
                new HeaderHttpItem(
                    new StaticHttpItem(
                        ROWS
                        + "{$mid\0:21},{$mid\0:23},{$mid\0:25},{$mid\0:27},"
                        + "{$mid\0:29},{$mid\0:31},{$mid\0:33},{$mid\0:35},"
                        + "{$mid\0:37},{$mid\0:39}]}"),
                    PGSHARD_HEADER));

            Map<String, Object> fields = new HashMap<>();
            fields.put(OracleFields.OPERATION_ID, ZERO);
            fields.put(OracleFields.FRESH_COUNT, ZERO);
            fields.put(OracleFields.USEFUL_NEW_MESSAGES, ZERO);
            fields.put(OracleFields.LCN, revision);
            fields.put(OracleFields.OPERATION_DATE, JsonChecker.ANY_VALUE);
            fields.put(PgFields.PGSHARD, PGSHARD);
            fields.put(PgFields.UID, uid);
            fields.put(PgFields.CHANGE_TYPE, CLEANUP);

            Header[] headers = new Header[] {
                new BasicHeader(YandexHeaders.SERVICE, SERVICE),
                new BasicHeader(YandexHeaders.ZOO_SHARD_ID, uid)
            };

            String producerPrefix =
                "/notify?mdb=pg&uid=9002&revision=22&change-type=";
            String producerCleanup =
                producerPrefix
                + "search-mids-cleanup&cleanup-user&batch-size=1&mids=21-39";
            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse("[{\"mid\":21},{\"mid\":39}]"));
            cluster.producer().add(
                producerCleanup,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            fields.put(PgFields.CHANGE_TYPE, UPDATE);

            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse(
                    "[{\"mid\":21},{\"mid\":23},{\"mid\":25},{\"mid\":27}]"));
            JsonChecker checker1 = new JsonChecker(fields);
            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse(
                    "[{\"mid\":27},{\"mid\":29},{\"mid\":31},{\"mid\":33}]"));
            JsonChecker checker2 = new JsonChecker(fields);
            MultipartAssert check = new MultipartAssert(checker1, checker2);
            String producerUpdate1 =
                producerPrefix + "search-update&batch-size=2&mids=21-33";
            cluster.producer().add(
                producerUpdate1,
                new ExpectingHeaderHttpItem(check, headers[0]),
                NotImplementedHttpItem.INSTANCE);

            fields.put(
                PgFields.CHANGED,
                ValueContentHandler.parse(
                    "[{\"mid\":33},{\"mid\":35},{\"mid\":37},{\"mid\":39}]"));
            String producerUpdate2 =
                producerPrefix + "search-update&batch-size=1&mids=33-39";
            cluster.producer().add(
                producerUpdate2,
                new ExpectingHeaderHttpItem(
                    new ExpectingHttpItem(new JsonChecker(fields)),
                    headers),
                NotImplementedHttpItem.INSTANCE);

            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(
                    HTTP_LOCALHOST + cluster.indexer().port()
                    + "/reindex?prefix=9002"));
            Assert.assertEquals(1, cluster.msal().accessCount(msalRevision));
            Assert.assertEquals(1, cluster.msal().accessCount(msalMids));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerCleanup));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUpdate1));
            Assert.assertEquals(
                1,
                cluster.producer().accessCount(producerUpdate2));
        }
    }

    private static class MultipartAssert implements HttpRequestHandler {
        private final List<JsonChecker> checkers;

        MultipartAssert(final JsonChecker... checkers) {
            this.checkers = Arrays.asList(checkers);
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws NotImplementedException
        {
            MimeTokenStream stream = new MimeTokenStream(
                DefaultMimeConfig.INSTANCE,
                null,
                new Utf8FieldBuilder(),
                new OverwritingBodyDescriptorBuilder());
            try {
                HttpEntity entity =
                    ((HttpEntityEnclosingRequest) request).getEntity();
                char[] buf = new char[BUFFER_SIZE];
                List<String> requests = new ArrayList<>();
                stream.parseHeadless(
                    entity.getContent(),
                    entity.getContentType().getValue());
                EntityState state = stream.getState();
                while (state != EntityState.T_END_OF_STREAM) {
                    switch (state) {
                        case T_BODY:
                            StringBuilder sb = new StringBuilder();
                            try (Reader reader = new InputStreamReader(
                                    stream.getDecodedInputStream(),
                                    StandardCharsets.UTF_8))
                            {
                                int read = reader.read(buf);
                                while (read != -1) {
                                    sb.append(buf, 0, read);
                                    read = reader.read(buf);
                                }
                            }
                            requests.add(new String(sb));
                            break;
                        default:
                            break;
                    }
                    state = stream.next();
                }
                Assert.assertEquals(checkers, requests);
            } catch (Throwable t) {
                throw new NotImplementedException(t);
            }
        }
    }
}

