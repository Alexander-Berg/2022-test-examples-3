package ru.yandex.search.disk.indexer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.charset.StringDecoder;
import ru.yandex.collection.LazyList;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.NotImplementedHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mail.mime.DefaultMimeConfig;
import ru.yandex.mail.mime.OverwritingBodyDescriptorBuilder;
import ru.yandex.mail.mime.Utf8FieldBuilder;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;

public class DiskIndexerTest extends TestBase {
    private static final String UID = "69000";
    private static final long OWNER = 5265082;
    private static final long BASE_VERSION = 1399621236819430L;
    private static final String SHARD = "3466";
    private static final String SERVICE_NAME = "cv-reindex";
    private static final String SERVICE_URI = "&service-name=" + SERVICE_NAME;
    private static final String MPFS_URI = "/json/snapshot?uid=" + UID;
    private static final String TEST1_1 = "test1-1.json";
    private static final String TEST1_1_KEY =
        "1484239826814380;fff785dc3d5afb4c44cd079304835363cf892461a697e1d2dace"
        + "80b2a710aa18;87e334648d167d10a6a0251658c6c5ff;";
    private static final String TEST1_2 = "test1-2.json";
    private static final String TEST1_2_KEY =
        "1486405439269381;2b937ebb7432276e65b6d952ffca55ec73af9689d22dc7dcebd1"
        + "64507fbae140;5582243c99b2a639eb3dab6384d807fd;";
    private static final String TEST1_3 = "test1-3.json";
    private static final String TEST1_ID =
        "e9f279603e3fb182bdf91ccb27dc16540c611670f295e98ea9b62c7a6b5a416";
    private static final String SUFFIX =
        "&prefix=69000&version=1484239826814380";
    private static final String RESOURCE_IDS = "&resource-ids=";
    private static final String BATCH_SIZE_2 = "/reindex?batch-size=2&uid=";

    private static String iterationKey(final String iterationKey) {
        StringBuilder sb = new StringBuilder("{\"iteration_key\":");
        if (iterationKey == null) {
            sb.append("null}");
        } else {
            sb.append('"');
            sb.append(iterationKey);
            sb.append('"');
            sb.append('}');
        }
        return new String(sb);
    }

    private void prepareMpfs(final StaticServer mpfs) throws Exception {
        mpfs.add(
            MPFS_URI,
            new ExpectingHttpItem(
                new JsonChecker(iterationKey(null)),
                HttpStatus.SC_OK,
                new FileEntity(
                    new File(getClass().getResource(TEST1_1).toURI()),
                    ContentType.APPLICATION_JSON)),
            new ExpectingHttpItem(
                new JsonChecker(iterationKey(TEST1_1_KEY)),
                HttpStatus.SC_OK,
                new FileEntity(
                    new File(getClass().getResource(TEST1_2).toURI()),
                    ContentType.APPLICATION_JSON)),
            new ExpectingHttpItem(
                new JsonChecker(iterationKey(TEST1_2_KEY)),
                HttpStatus.SC_OK,
                new FileEntity(
                    new File(getClass().getResource(TEST1_3).toURI()),
                    ContentType.APPLICATION_JSON)),
            NotImplementedHttpItem.INSTANCE);
    }

    private static String uri(
        final String prefix,
        final int id,
        final String suffix)
    {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append("&resource_id=5265082:" + TEST1_ID);
        sb.append(id);
        sb.append("&id=" + TEST1_ID);
        sb.append(id);
        sb.append(suffix);
        return new String(sb);
    }

    private static String docs(final int... ids) {
        StringBuilder sb = new StringBuilder("{\"docs\":[");
        for (int i = 0; i < ids.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            int id = ids[i];
            sb.append("{\"id\":\"");
            sb.append(TEST1_ID);
            sb.append(id);
            sb.append("\",\"resource_id\":\"");
            sb.append(OWNER);
            sb.append(':');
            sb.append(TEST1_ID);
            sb.append(id);
            sb.append("\",\"version\":");
            sb.append(BASE_VERSION + id);
            sb.append('}');
        }
        sb.append(']');
        sb.append('}');
        return new String(sb);
    }

    private static void producerExpect(
        final StaticServer producer,
        final String uri)
    {
        producerExpect(producer, uri, StaticHttpItem.OK);
    }

    private static void producerExpect(
        final StaticServer producer,
        final String uri,
        final HttpRequestHandler handler)
    {
        producer.add(
            uri,
            new ExpectingHeaderHttpItem(
                new ExpectingHeaderHttpItem(
                    handler,
                    YandexHeaders.SERVICE,
                    SERVICE_NAME),
                YandexHeaders.ZOO_SHARD_ID,
                SHARD),
            NotImplementedHttpItem.INSTANCE);
    }

    // CSOFF: MagicNumber
    @Test
    public void test() throws Exception {
        try (DiskIndexerCluster cluster = new DiskIndexerCluster()) {
            prepareMpfs(cluster.mpfs());
            String prefix = "/ocr?";
            String uri1 = uri(prefix, 0, SUFFIX);
            producerExpect(cluster.producer(), uri1);
            String uri2 = uri(prefix, 1, SUFFIX);
            producerExpect(cluster.producer(), uri2);
            String uri3 = uri(prefix, 4, SUFFIX);
            producerExpect(cluster.producer(), uri3);
            String uri4 = uri(prefix, 6, SUFFIX);
            producerExpect(cluster.producer(), uri4);
            String uri5 = uri(prefix, 8, SUFFIX);
            producerExpect(cluster.producer(), uri5);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.indexer().port(),
                "/reindex?uri=/ocr&batch-size=1&mediatype=image&uid=" + UID
                + SERVICE_URI);
            Assert.assertEquals(1, cluster.producer().accessCount(uri1));
            Assert.assertEquals(1, cluster.producer().accessCount(uri2));
            Assert.assertEquals(1, cluster.producer().accessCount(uri3));
            Assert.assertEquals(1, cluster.producer().accessCount(uri4));
            Assert.assertEquals(1, cluster.producer().accessCount(uri5));
        }
    }

    @Test
    public void testNoFilter() throws Exception {
        try (DiskIndexerCluster cluster = new DiskIndexerCluster()) {
            prepareMpfs(cluster.mpfs());
            String prefix = "/ocr?filter=none";
            String uri1 = uri(prefix, 0, SUFFIX);
            producerExpect(cluster.producer(), uri1);
            String uri2 = uri(prefix, 1, SUFFIX);
            producerExpect(cluster.producer(), uri2);
            String uri3 = uri(prefix, 2, SUFFIX);
            producerExpect(cluster.producer(), uri3);
            String uri4 = uri(prefix, 3, SUFFIX);
            producerExpect(cluster.producer(), uri4);
            String uri5 = uri(prefix, 4, SUFFIX);
            producerExpect(cluster.producer(), uri5);
            String uri6 = uri(prefix, 5, SUFFIX);
            producerExpect(cluster.producer(), uri6);
            String uri7 = uri(prefix, 6, SUFFIX);
            producerExpect(cluster.producer(), uri7);
            String uri8 = uri(prefix, 7, SUFFIX);
            producerExpect(cluster.producer(), uri8);
            String uri9 = uri(prefix, 8, SUFFIX);
            producerExpect(cluster.producer(), uri9);
            String uri10 = uri(prefix, 9, SUFFIX);
            producerExpect(cluster.producer(), uri10);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.indexer().port(),
                "/reindex?uri=/ocr%3Ffilter%3Dnone&batch-size=1&uid=" + UID
                + SERVICE_URI);
            Assert.assertEquals(1, cluster.producer().accessCount(uri1));
            Assert.assertEquals(1, cluster.producer().accessCount(uri2));
            Assert.assertEquals(1, cluster.producer().accessCount(uri3));
            Assert.assertEquals(1, cluster.producer().accessCount(uri4));
            Assert.assertEquals(1, cluster.producer().accessCount(uri5));
            Assert.assertEquals(1, cluster.producer().accessCount(uri6));
            Assert.assertEquals(1, cluster.producer().accessCount(uri7));
            Assert.assertEquals(1, cluster.producer().accessCount(uri8));
            Assert.assertEquals(1, cluster.producer().accessCount(uri9));
            Assert.assertEquals(1, cluster.producer().accessCount(uri10));
        }
    }

    @Test
    public void testBatches() throws Exception {
        try (DiskIndexerCluster cluster = new DiskIndexerCluster()) {
            prepareMpfs(cluster.mpfs());
            String uri1 =
                BATCH_SIZE_2 + UID
                + RESOURCE_IDS + OWNER + ':' + TEST1_ID + 3
                + '-' + OWNER + ':' + TEST1_ID + 7;
            String prefix = "/ocr?filter=8,10";
            cluster.producer().add(
                uri1,
                new ExpectingHeaderHttpItem(
                    new MultipartAssert(
                        uri(prefix, 3, SUFFIX),
                        uri(prefix, 7, SUFFIX)),
                    YandexHeaders.SERVICE,
                    SERVICE_NAME));
            String uri2 = uri(prefix, 9, SUFFIX);
            producerExpect(cluster.producer(), uri2);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.indexer().port(),
                "/reindex?uri=/ocr?filter=8,10&batch-size=2&mediatype=doc"
                + "&mediatype=video&uid=" + UID + SERVICE_URI);
            Assert.assertEquals(1, cluster.producer().accessCount(uri1));
            Assert.assertEquals(1, cluster.producer().accessCount(uri2));
        }
    }

    @Test
    public void testFullReindex() throws Exception {
        try (DiskIndexerCluster cluster = new DiskIndexerCluster()) {
            prepareMpfs(cluster.mpfs());
            String innerCleanup = "&cleanup-type=inner";
            String batchSize = BATCH_SIZE_2 + UID + RESOURCE_IDS;
            String uri1 =
                batchSize + OWNER + ':' + TEST1_ID + 0
                + '-' + OWNER + ':' + TEST1_ID + 6;
            String prefix = "/?action=reindex&docs-count=";
            cluster.producer().add(
                uri1,
                new ExpectingHeaderHttpItem(
                    new MultipartAssert(
                        new String[] {
                            prefix + 4 + RESOURCE_IDS
                                + OWNER + ':' + TEST1_ID + 0 + '-'
                                + OWNER + ':' + TEST1_ID + 3
                                + SUFFIX + innerCleanup,
                            prefix + 4 + RESOURCE_IDS
                                + OWNER + ':' + TEST1_ID + 3 + '-'
                                + OWNER + ':' + TEST1_ID + 6
                                + SUFFIX + innerCleanup
                        },
                        new String[] {
                                docs(0, 1, 2, 3),
                                docs(3, 4, 5, 6)
                        }),
                    YandexHeaders.SERVICE,
                    SERVICE_NAME));
            String uri2 =
                prefix + 4 + RESOURCE_IDS
                + OWNER + ':' + TEST1_ID + 6 + '-'
                + OWNER + ':' + TEST1_ID + 9
                + SUFFIX + innerCleanup;
            producerExpect(
                cluster.producer(),
                uri2,
                new ExpectingHttpItem(new JsonChecker(docs(6, 7, 8, 9))));
            String cleanupUri =
                prefix + 2 + RESOURCE_IDS
                + OWNER + ':' + TEST1_ID + 0 + '-'
                + OWNER + ':' + TEST1_ID + 9
                + SUFFIX + "&cleanup-type=outer&callback=https://ya.ru";
            producerExpect(
                cluster.producer(),
                cleanupUri,
                new ExpectingHttpItem(new JsonChecker(docs(0, 9))));
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                cluster.indexer().port(),
                "/reindex?uri=/?action=reindex&cleanup&callback=https://ya.ru"
                + "&docs-per-request=4&batch-size=2&uid=" + UID + SERVICE_URI);
            Assert.assertEquals(1, cluster.producer().accessCount(uri1));
            Assert.assertEquals(1, cluster.producer().accessCount(uri2));
            Assert.assertEquals(1, cluster.producer().accessCount(cleanupUri));
        }
    }
    // CSON: MagicNumber

    @Test
    public void test403() throws Exception {
        try (DiskIndexerCluster cluster = new DiskIndexerCluster()) {
            cluster.mpfs().add(MPFS_URI, HttpStatus.SC_FORBIDDEN);
            cluster.start();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                cluster.indexer().port(),
                "/reindex?uri=/cv-reindex&uid=" + UID + SERVICE_URI);
            cluster.mpfs().add(MPFS_URI, HttpStatus.SC_SERVICE_UNAVAILABLE);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                cluster.indexer().port(),
                "/reindex?uri=/cv-reindex2&uid=" + UID + SERVICE_URI);
            cluster.mpfs().close();
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_GATEWAY,
                cluster.indexer().port(),
                "/reindex?uri=/cv-reindex3&uid=" + UID + SERVICE_URI);
        }
    }

    private static class MultipartAssert implements HttpRequestHandler {
        private final List<Checker> uriCheckers;
        private final List<Checker> bodyCheckers;

        MultipartAssert(final String... uris) {
            this(uris, new String[0]);
        }

        MultipartAssert(final String[] uris, final String[] bodies) {
            uriCheckers = new LazyList<>(
                Arrays.asList(uris),
                uri -> new StringChecker(uri));
            bodyCheckers = new LazyList<>(
                Arrays.asList(bodies),
                body -> new JsonChecker(body));
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
                int shardsFound = 0;
                List<String> uris = new ArrayList<>(uriCheckers.size());
                List<String> bodies = new ArrayList<>(bodyCheckers.size());
                stream.parseHeadless(
                    entity.getContent(),
                    entity.getContentType().getValue());
                EntityState state = stream.getState();
                while (state != EntityState.T_END_OF_STREAM) {
                    if (state == EntityState.T_FIELD) {
                        Field field = stream.getField();
                        String name = field.getName();
                        if (name.equals(YandexHeaders.URI)) {
                            uris.add(field.getBody());
                        } else if (name.equals(YandexHeaders.ZOO_SHARD_ID)) {
                            Assert.assertEquals(SHARD, field.getBody());
                            ++shardsFound;
                        }
                    } else if (state == EntityState.T_BODY) {
                        bodies.add(
                            IOStreamUtils.consume(
                                stream.getDecodedInputStream())
                                .processWith(StringDecoder.UTF_8.get()));
                    }
                    state = stream.next();
                }
                Assert.assertEquals(uriCheckers, uris);
                if (!bodyCheckers.isEmpty()) {
                    Assert.assertEquals(bodyCheckers, bodies);
                }
                Assert.assertEquals(uriCheckers.size(), shardsFound);
            } catch (Throwable t) {
                throw new NotImplementedException(t);
            }
        }
    }
}

