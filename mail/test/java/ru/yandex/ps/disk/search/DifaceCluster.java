package ru.yandex.ps.disk.search;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.devtools.test.Paths;
import ru.yandex.disk.search.AvatarSrwKeyConverter;
import ru.yandex.disk.search.DiskParams;
import ru.yandex.disk.search.face.ClusterModification;
import ru.yandex.disk.search.face.DeltaChangeType;
import ru.yandex.disk.search.face.DeltaItem;
import ru.yandex.disk.search.face.FaceModification;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.ps.disk.search.config.DifaceConfigBuilder;
import ru.yandex.ps.disk.search.delta.ClusterDelta;
import ru.yandex.search.disk.kali.KaliCluster;
import ru.yandex.search.disk.kali.KaliConfigBuilder;
import ru.yandex.search.disk.kali.config.KaliFaceConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.string.StringUtils;

public class DifaceCluster implements GenericAutoCloseable<IOException> {
    private static final File SERVER_CONFIG =
        new File(Paths.getSourcePath("mail/search/disk/diface/files/diface.conf"));

    private final Diface server;
    private final KaliCluster kaliCluster;
    private final TestSearchBackend backend;
    private final StaticServer producer;
    private final StaticServer imageparser;
    private final StaticServer djfs;
    protected final GenericAutoCloseableChain<IOException> chain;

    public DifaceCluster(final TestBase testBase) throws Exception {
        System.setProperty("YT_ACCESS_LOG", "");
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>())) {
            producer = new StaticServer(Configs.baseConfig("ProducerStaticServer"));
            chain.get().add(producer);
            producer.start();

            KaliConfigBuilder kaliConfig = new KaliConfigBuilder();
            kaliConfig.faceConfig(
                new KaliFaceConfigBuilder()
                    .enabled(true)
                    .faceQueue("face_in_queue")
                    .callbackBaseUri("/face?")
                    .host(producer.host()).connections(10).build());

            kaliCluster = new KaliCluster(testBase, kaliConfig);
            backend = kaliCluster.lucene();

            djfs = new StaticServer(Configs.baseConfig("DjfsStaticServer"));
            chain.get().add(djfs);
            djfs.start();

            imageparser = new StaticServer(Configs.baseConfig("ImageParser"));
            chain.get().add(imageparser);
            imageparser.start();
            System.setProperty("IMAGEPARSER_SRW_HOST", imageparser.host().toString());
            System.setProperty("DJFS_HOST", djfs.host().toString());
            System.setProperty("DJFS_ALBUM_HOST", djfs.host().toString());
            System.setProperty("APE_TVM_CLIENT_ID", "1");
            System.setProperty("DISK_TVM_CLIENT_ID", "1");
            System.setProperty("MIN_CLUSTER_SIZE", "1");
            System.setProperty("DEFAULT_COKEMULATOR", "true");
            System.setProperty("COKEMULATOR_HOST", imageparser.host().toString());
            System.setProperty("UNISTORAGE_TVM_CLIENT_ID", "2");
            System.setProperty("SEARCHMAP_PATH", "searchmap.txt");
            System.setProperty("PRODUCER_INDEXING_HOST", producer.host().toString());
            System.setProperty("FACE_OUT_QUEUE", "face_index_queue");
            System.setProperty("TVM_CLIENT_ID", "1");
            System.setProperty("BSCONFIG_IDIR", "");
            System.setProperty("SECRET", "1");
            System.setProperty("TVM_API_HOST", "");

            DifaceConfigBuilder config = new DifaceConfigBuilder(
                patchDifaceCluster(
                    new IniConfig(SERVER_CONFIG)));
            config.searchMapConfig().content(backend.searchMapRule("face_index_queue"));
            config.minClusterSize(1);
            config.defaultCokemulator(false);
            config.checkDimensions(false);
            config.sideConfig().enabled(false);
            config.bannedFacesConfig().enabled(false);
            server = new Diface(config.build());
            chain.get().add(server);

            final AtomicInteger faceInQueueId = new AtomicInteger(0);
            kaliCluster.callbacks().add(
                "/face*",
                new StaticHttpResource(
                    new ZooHashCheckHandler(
                        new ProxyHandler(
                            server.port(),
                            (s) -> s + "&zoo-queue-id=" + faceInQueueId.incrementAndGet() + "&cokemulator=true"))));

            producer.add("/_status*", "[{$localhost\0:100500}]");
            producer.add(
                "*",
                new StaticHttpResource(
                    new ZooHashCheckHandler(
                        new ProxyMultipartHandler(
                            backend.indexerPort()))));

            server.start();
            kaliCluster.start();
            this.chain = chain.release();
        }
    }


    public void status(final long prefix) throws Exception {
        producer.add(
            "/_status?service=face_index_queue&prefix=" + prefix
                + "&allow_cached"
                + "&all&json-type=dollar",
            "[{\"localhost\":100500}]");
    }

    public Diface server() {
        return server;
    }

    public TestSearchBackend backend() {
        return backend;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer imageparser() {
        return imageparser;
    }

    private IniConfig patchDifaceCluster(
        final IniConfig config)
        throws Exception {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        config.section("http").sections().remove("free-space-signals");
        config.section("server").sections().remove("free-space-signals");
        config.section("chats").sections().remove("https");
        config.section("users").sections().remove("https");
        config.section("messages").sections().remove("https");
        config.section("djfs").sections().remove("https");
        config.section("coke").sections().remove("tvm-headers");

        IniConfig server = config.section("server");
        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public static Image createImage() throws Exception {
        return new Image();
    }

    public static Image createImage(final String id) throws Exception {
        Image image = new Image();
        image.ids(id);
        return image;
    }

    public DeltaItem clusterAdd(final String clusterId) {
        return new ClusterModification(DeltaChangeType.CLUSTER_CREATED, clusterId);
    }

    public DeltaItem faceMod(final DeltaChangeType ctype, final String faceId) {
        return new FaceModification(ctype, faceId, faceId.split("_")[0]);
    }

    public DeltaItem faceAdd(final String faceId) {
        return new FaceModification(DeltaChangeType.ITEM_ADDED, faceId, faceId.split("_")[0]);
    }

    public DeltaItem faceRemove(final String faceId) {
        return new FaceModification(DeltaChangeType.ITEM_DELETED, faceId, faceId.split("_")[0]);
    }

    public ClusterDelta delta(
        final long prefix,
        final String clusterId,
        final String refQueue,
        final long refQueueId,
        final long version,
        final DeltaItem... items)
        throws Exception {
        return new ClusterDelta(prefix, clusterId, version, refQueue, refQueueId, 0L, Arrays.asList(items));
    }

    public void checkDelta(
        final long prefix,
        final List<ClusterDelta> deltas)
        throws Exception {
        ClusterDelta[] array = new ClusterDelta[deltas.size()];
        deltas.toArray(array);
        checkDelta(prefix, array);
    }

    public void checkDelta(
        final long prefix,
        final ClusterDelta... deltas)
        throws Exception {
        checkDelta(prefix, "", deltas);
    }

    public void checkDelta(
        final long prefix,
        final String extraQuery,
        final ClusterDelta... deltas)
        throws Exception {
        backend().checkSearch(
            "/search?prefix=" + prefix + "&text=type:face_delta"
                + extraQuery + "&get=*,-facedelta_timestamp&sort=facedelta_version&asc=true",
            expectDeltas(Arrays.asList(deltas)));
    }

    public void checkDelta(
        final long prefix,
        final String extraQuery,
        final String clusterId,
        final String refQueue,
        final long refQueueId,
        final long version,
        final DeltaItem... items) throws Exception {
        checkDelta(prefix, extraQuery, delta(prefix, clusterId, refQueue, refQueueId, version, items));
    }

    public void checkDelta(
        final long prefix,
        final String clusterId,
        final String refQueue,
        final long refQueueId,
        final long version,
        final DeltaItem... items) throws Exception {
        checkDelta(prefix, "", clusterId, refQueue, refQueueId, version, items);
    }

    public JsonChecker expectDeltas(final ClusterDelta delta) throws Exception {
        return expectDeltas(Collections.singletonList(delta));
    }

    public JsonChecker expectDeltas(final List<ClusterDelta> deltas) throws Exception {
        JsonList hits = new JsonList(BasicContainerFactory.INSTANCE);
        for (ClusterDelta delta : deltas) {
            StringBuilderWriter sbw = new StringBuilderWriter();
            try (JsonWriter writer = JsonType.HUMAN_READABLE.create(sbw)) {
                writer.value(delta);
            }

            JsonMap map = TypesafeValueContentHandler.parse(sbw.toString()).asMap();
            map.remove(FaceBackendFields.FACEDELTA_TIMESTAMP.stored());
            // convert to string - string map
            JsonMap searchBackendMap = new JsonMap(BasicContainerFactory.INSTANCE);
            for (Map.Entry<String, JsonObject> item : map.entrySet()) {
                searchBackendMap.put(item.getKey(), new JsonString(item.getValue().asString()));
            }

            hits.add(searchBackendMap);
        }

        JsonMap result = new JsonMap(BasicContainerFactory.INSTANCE);
        result.put("hitsCount", new JsonLong(deltas.size()));
        result.put("hitsArray", hits);
        return new JsonChecker(JsonType.HUMAN_READABLE.toString(result));
    }

    public void indexResource(
        final Resource resource,
        final String action,
        final String service,
        final long queueId)
        throws Exception {
        JsonMap map = resource.map();
        long uid = map.getLong("uid");
        String resourceId = resource.map.getString("resource_id");

        kaliCluster.djfs().add(
            "/api/v1/indexer/resources?"
                + "uid=" + uid
                + "&resourceId=" + resourceId,
            "{\"items\":[" + JsonType.HUMAN_READABLE.toString(map) + "]}");

        kaliCluster.lenulca().add(
            "/get/" + map.getString("stid") + "?raw",
            new File(
                Paths.getSandboxResourcesRoot() + "/lepestrichestvo.JPG"));

        if (resource.cv() != null) {
            kaliCluster.imageparser().add(
                "/process/handler?stid=" + resource.map().getString("stid"),
                new StaticHttpItem(JsonType.HUMAN_READABLE.toString(resource.cv())));
        }

        if (resource.face() != null) {
            String stid = resource.map().getString("stid");
            String previewStid = resource.map().getString("preview_stid", stid);
            if (AvatarSrwKeyConverter.INSTANCE.isAvatarStid(previewStid)) {
                stid = StringUtils.concat(
                    DiskParams.AVATAR_X_SRW_NAMESPACE.getValue(),
                    '/',
                    AvatarSrwKeyConverter.INSTANCE.parse(previewStid),
                    '/',
                    "1280_nocrop/webp");
            }

            System.err.println("AddingStaticFace to " + kaliCluster.imageparser().getName()
                + "/process?extract-faces=true&old-cv=false&fail-on-empty=false&stid=" + stid);

            imageparser.add(
                "/process?extract-faces=true&old-cv=false&fail-on-empty=false&stid=" + stid,
                new StaticHttpItem(JsonType.HUMAN_READABLE.toString(resource.face())));
        }

        if (resource.ocrText() != null) {
            kaliCluster.ocraas().add(
                "/process/handler?stid=" + resource.map().getString("stid"),
                new StaticHttpItem(resource.ocrText()));
        }

        HttpGet get = new HttpGet(kaliCluster.kali().host().toString() + "/?id=" + map.getString("id")
            + "&prefix=" + uid + "&action=" + action
            + "&service=" + service + "&timestamp="
            + System.currentTimeMillis() + "&version="
            + map.getLong("version") + "&resource_id="
            + resourceId + "&fast-moved"
            + "&clusterize_face=true"
            + "&zoo-queue-id=" + queueId);
        get.addHeader(YandexHeaders.ZOO_QUEUE, service);
        get.addHeader(YandexHeaders.ZOO_QUEUE_ID, String.valueOf(queueId));
        get.addHeader(YandexHeaders.ZOO_SHARD_ID, String.valueOf(uid % 65534));

        HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
    }

    public abstract static class Resource {
        private final JsonMap map;
        private JsonObject face = null;
        private JsonMap cv = null;
        private String ocrText = null;

        public JsonMap map() {
            return map;
        }

        public JsonObject face() {
            return face;
        }

        public JsonMap cv() {
            return cv;
        }

        public String ocrText() {
            return ocrText;
        }

        protected Resource(final JsonMap map) {
            this.map = map;
        }

        protected Resource() {
            this.map = new JsonMap(BasicContainerFactory.INSTANCE);
        }

        public void set(final String key, final String value) {
            this.map.put(key, new JsonString(value));
        }

        public void set(final String key, final long value) {
            this.map.put(key, new JsonLong(value));
        }

        public Resource face(final JsonObject face) {
            this.face = face;
            return this;
        }

        public Resource cv(final JsonMap cv) {
            this.cv = cv;
            return this;
        }

        public Resource folderId(final String folderId) {
            set("folder", folderId);
            return this;
        }

        public Resource stid(final String stid) {
            set("stid", stid);
            return this;
        }

        public Resource id(final String id) {
            set("id", id);
            return this;
        }

        public Resource ids(final String id) {
            set("id", id);
            set("resource_id", map.getString("uid", "") + ":" + id);
            return this;
        }

        public Resource name(final String name) {
            set("name", name);
            return this;
        }

        public Resource key(final String key) {
            set("key", key);
            return this;
        }

        public Resource version(final long version) {
            set("version", version);
            return this;
        }

        public Resource ctime(final long ctime) {
            set("version", ctime);
            return this;
        }

        public Resource etime(final String etime) {
            set("etime", etime);
            return this;
        }

        public Resource mtime(final String mtime) {
            set("mtime", mtime);
            return this;
        }

        public Resource uid(final String uid) throws JsonException {
            this.set("uid", uid);
            this.set("resource_id", uid + ':' + map.getString("id"));
            return this;
        }
    }

    public static class Image extends Resource {
        public Image() throws Exception {
            super(
                TypesafeValueContentHandler.parse(
                    TestBase.loadResourceAsString(DifaceCluster.class, "default_image.json"))
                    .asMap());
        }

        public Image mediatype(final String mediatype) {
            this.set("media_type", mediatype);
            return this;
        }

        public Image djfsAlbum(final String djfsAlbum) {
            this.set("photoslice_album_type", djfsAlbum);
            return this;
        }

        public Image mimetype(final String mimetype) {
            this.set("mimetype", mimetype);
            return this;
        }
    }

    private static class ZooHashCheckHandler implements HttpRequestHandler {
        private final HttpRequestHandler handler;
        private final Set<String> hashed = new LinkedHashSet<>();

        public ZooHashCheckHandler(final HttpRequestHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(
            final HttpRequest httpRequest,
            final HttpResponse httpResponse,
            final HttpContext httpContext)
            throws HttpException, IOException {
            Header header = httpRequest.getFirstHeader(YandexHeaders.CHECK_DUPLICATE);
            if (header != null && Boolean.valueOf(header.getValue())) {
                Header zooHashHeader = httpRequest.getFirstHeader(YandexHeaders.ZOO_HASH);
                if (zooHashHeader != null) {
                    synchronized (this) {
                        if (!hashed.add(zooHashHeader.getValue())) {
                            httpResponse.setStatusCode(HttpStatus.SC_OK);
                            System.err.println(
                                "Skipping request, zoo hash already present "
                                    + zooHashHeader.getValue());
                            return;
                        }
                    }
                }
            }

            handler.handle(httpRequest, httpResponse, httpContext);
        }
    }
}
