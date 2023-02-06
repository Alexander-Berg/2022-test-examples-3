package ru.yandex.test.search.backend;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.parser.JsonException;
import ru.yandex.msearch.Config;
import ru.yandex.msearch.Daemon;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.Prefix;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.filesystem.CloseableDeleter;

public class TestSearchBackend implements GenericAutoCloseable<IOException> {
    private static final String LOCALHOST = "http://localhost:";
    private static final String XURLS_REGEX_FILE = "xurls_regex_file";
    private static final int MAX_SHARDS = 65534;
    private static final int WORKERS = 3;

    static {
        System.setProperty("BSCONFIG_IPORT", "24840");
        System.setProperty("BSCONFIG_INAME", "localhost:24840");
        System.setProperty("INUM", "42");
        System.setProperty("INDEX_BASE","/webcache/dsearch");
        System.setProperty("INDEX_DIR","$(INDEX_BASE)/$(INUM)/index");
        System.setProperty("REPLICA_NAME", "unit-test");
        System.setProperty(
             "LUCENE_DISK_CONFIG_SSD",
             "bacchus-nossd.conf");
    }

    private final CloseableHttpClient client = HttpClients.createDefault();
    private final GenericAutoCloseableHolder<
        IOException,
        GenericAutoCloseable<IOException>> indexDeleterHolder;
    private final Path root;
    private final boolean verbose;
    private final Daemon lucene;

    public TestSearchBackend(final File config) throws Exception {
        this(config.toPath());
    }

    public TestSearchBackend(final Path config) throws Exception {
        this((String) null, config);
    }

    public TestSearchBackend(final String config) throws Exception {
        this((String) null, config);
    }

    public TestSearchBackend(final IniConfig config) throws Exception {
        this((String) null, config);
    }

    public TestSearchBackend(final TestBase testBase, final File config)
        throws Exception
    {
        this(testBase, true, config);
    }

    public TestSearchBackend(
        final TestBase testBase,
        final boolean verbose,
        final File config)
        throws Exception
    {
        this(testBase, verbose, config.toPath());
    }

    public TestSearchBackend(final TestBase testBase, final Path config)
        throws Exception
    {
        this(testBase, true, config);
    }

    public TestSearchBackend(
        final TestBase testBase,
        final boolean verbose,
        final Path config)
        throws Exception
    {
        this(testBase.testName.getMethodName(), verbose, config);
    }

    public TestSearchBackend(final TestBase testBase, final String config)
        throws Exception
    {
        this(testBase.testName.getMethodName(), config);
    }

    public TestSearchBackend(final TestBase testBase, final IniConfig config)
        throws Exception
    {
        this(testBase.testName.getMethodName(), config);
    }

    public TestSearchBackend(final TestBase testBase, final Config config)
        throws Exception
    {
        this(testBase.testName.getMethodName(), config);
    }

    public TestSearchBackend(final String testName, final File config)
        throws Exception
    {
        this(testName, config.toPath());
    }

    public TestSearchBackend(final String testName, final Path config)
        throws Exception
    {
        this(testName, true, config);
    }

    public TestSearchBackend(
        final String testName,
        final boolean verbose,
        final Path config)
        throws Exception
    {
        this(Files.createTempDirectory(testName), verbose, config);
    }

    public TestSearchBackend(final String testName, final String config)
        throws Exception
    {
        this(Files.createTempDirectory(testName), config);
    }

    public TestSearchBackend(final String testName, final IniConfig config)
        throws Exception
    {
        this(Files.createTempDirectory(testName), config);
    }

    public TestSearchBackend(final String testName, final Config config)
        throws Exception
    {
        this(Files.createTempDirectory(testName), config);
    }

    public TestSearchBackend(final Path root, final File config) throws Exception {
        this(root, true, config);
    }

    public TestSearchBackend(
        final Path root,
        final boolean verbose,
        final File config)
        throws Exception
    {
        this(root, verbose, config.toPath());
    }

    public TestSearchBackend(final Path root, final Path config) throws Exception {
        this(root, true, config);
    }

    public TestSearchBackend(
        final Path root,
        final boolean verbose,
        final Path config)
        throws Exception
    {
        this(root, verbose, patchConfig(root, verbose, new IniConfig(config)));
    }

    public TestSearchBackend(final Path root, final String config) throws Exception {
        this(root, patchConfig(root, new IniConfig(new StringReader(config))));
    }

    public TestSearchBackend(final Path root, final IniConfig config)
        throws Exception
    {
        this(root, true, config);
    }

    public TestSearchBackend(final Path root, final Config config)
        throws Exception
    {
        this(root, true, config);
    }

    public TestSearchBackend(
        final Path root,
        final boolean verbose,
        final IniConfig config)
        throws Exception
    {
        this(root, verbose, new Config(config));
        config.checkUnusedKeys();
    }

    public TestSearchBackend(
        final Path root,
        final boolean verbose,
        final Config config)
        throws Exception
    {
        this.root = root;
        this.verbose = verbose;
        indexDeleterHolder =
            new GenericAutoCloseableHolder<>(
                new CloseableDeleter(root));
        lucene = new Daemon(config);
    }

    public CloseableHttpClient client() {
        return client;
    }

    public Path root() {
        return root;
    }

    public Daemon lucene() {
        return lucene;
    }

    public int indexerPort() throws IOException {
        return lucene.jsonServerPort();
    }

    public HttpHost indexerHost() throws IOException {
        return lucene.jsonServerHost();
    }

    public int searchPort() throws IOException {
        return lucene.searchServerPort();
    }

    public HttpHost searchHost() throws IOException {
        return lucene.searchServerHost();
    }

    public String indexerUri() throws IOException {
        return LOCALHOST + indexerPort();
    }

    public String searchUri() throws IOException {
        return LOCALHOST + searchPort();
    }

    public String oldSearchUri() {
        return LOCALHOST + lucene.searchPort();
    }

    public int dumpPort() {
        return lucene.dumpPort();
    }

    public String searchMapRule(final String db) throws IOException {
        return searchMapRule(db, 0, MAX_SHARDS);
    }

    public String searchMapRule(final String db, final PrefixType prefixType)
        throws IOException
    {
        return searchMapRule(db, prefixType, 0, MAX_SHARDS);
    }

    // CSOFF: MultipleStringLiterals
    public String searchMapRule(
        final String db,
        final int shardsFrom,
        final int shardsTo)
        throws IOException
    {
        return db + " shards:" + shardsFrom + '-' + (shardsTo - 1)
            + ",host:localhost,search_port:" + lucene.searchPort()
            + ",search_port_ng:" + lucene.searchServerPort()
            + ",json_indexer_port:" + lucene.jsonServerPort() + '\n';
    }

    // CSOFF: ParameterNumber
    public String searchMapRule(
        final String db,
        final PrefixType prefixType,
        final int shardsFrom,
        final int shardsTo)
        throws IOException
    {
        return db + " prefix_type:" + prefixType.name()
            + ",shards:" + shardsFrom + '-' + (shardsTo - 1)
            + ",host:localhost,search_port:" + lucene.searchPort()
            + ",search_port_ng:" + lucene.searchServerPort()
            + ",json_indexer_port:" + lucene.jsonServerPort() + '\n';
    }
    // CSON: ParameterNumber
    // CSON: MultipleStringLiterals

    public static String concatDocs(final String... docs) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < docs.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append('{');
            sb.append(docs[i]);
            sb.append('}');
        }
        sb.append(']');
        return new String(sb);
    }

    public static String concatDocs(
        final Prefix prefix,
        final String... docs)
    {
        StringBuilder sb = new StringBuilder("{\"prefix\":\"");
        sb.append(prefix);
        sb.append("\",\"docs\":");
        sb.append(concatDocs(docs));
        sb.append('}');
        return new String(sb);
    }

    public void add(final String... docs) throws IOException {
        add(0L, docs);
    }

    public void add(final long prefix, final String... docs)
        throws IOException
    {
        add(new LongPrefix(prefix), docs);
    }

    public void add(final Prefix prefix, final String... docs)
        throws IOException
    {
        HttpPost post = new HttpPost(indexerUri() + "/add");
        post.setEntity(
            new StringEntity(
                concatDocs(prefix, docs),
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
    }

    public void update(final String... docs) throws IOException {
        update(0L, docs);
    }

    public void update(final long prefix, final String... docs)
        throws IOException
    {
        update(new LongPrefix(prefix), docs);
    }

    public void update(final Prefix prefix, final String... docs)
        throws IOException
    {
        HttpPost post = new HttpPost(indexerUri() + "/update");
        post.setEntity(
            new StringEntity(
                concatDocs(prefix, docs),
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
    }

    public void delete(final String... docs) throws IOException {
        delete(0L, docs);
    }

    public void delete(final long prefix, final String... docs)
        throws IOException
    {
        delete(new LongPrefix(prefix), docs);
    }

    public void delete(final Prefix prefix, final String... docs)
        throws IOException
    {
        HttpPost post = new HttpPost(indexerUri() + "/delete");
        post.setEntity(
            new StringEntity(
                concatDocs(prefix, docs),
                ContentType.APPLICATION_JSON));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
    }

    public void flush() throws IOException {
        HttpGet get = new HttpGet(indexerUri() + "/flush?wait=true");
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
    }

    public void optimize(final int segmentCount) throws IOException {
        HttpGet get =
            new HttpGet(oldSearchUri() + "/?optimize=" + segmentCount);
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, get);
    }

    public static String prepareResult() {
        return "{\"hitsCount\":0,\"hitsArray\":[]}";
    }

    public static String prepareResult(final String... docs) {
        return prepareResult(docs.length, docs);
    }

    public static String prepareResult(
        final int hitsCount,
        final String... docs)
    {
        return prepareResultWithPrefix(hitsCount, "", docs);
    }

    public static String prepareResultWithPrefix(
        final String prefix,
        final String... docs)
    {
        return prepareResultWithPrefix(docs.length, prefix, docs);
    }

    public static String prepareResultWithPrefix(
        final int hitsCount,
        final String prefix,
        final String... docs)
    {
        StringBuilder sb = new StringBuilder("{\"hitsCount\":");
        if (hitsCount == -1) {
            sb.append("\"<any value>\"");
        } else {
            sb.append(hitsCount);
        }
        sb.append(prefix);
        sb.append(",\"hitsArray\":");
        sb.append(concatDocs(docs));
        sb.append('}');
        return new String(sb);
    }

    public void checkSearch(final String uri, final String expected)
        throws HttpException, IOException, JsonException
    {
        checkSearch(uri, new JsonChecker(expected));
    }

    public void checkSearch(final String uri, final Checker expected)
        throws HttpException, IOException, JsonException
    {
        checkSearch(new HttpGet(searchUri() + uri), expected);
    }

    public void checkSearch(
        final HttpUriRequest request,
        final String expected)
        throws HttpException, IOException, JsonException
    {
        checkSearch(request, new JsonChecker(expected));
    }

    public void checkSearch(
        final HttpUriRequest request,
        final Checker expected)
        throws HttpException, IOException, JsonException
    {
        try (CloseableHttpResponse response = client.execute(request)) {
            String responseEntity =
                CharsetUtils.toString(response.getEntity());
            if (verbose) {
                System.out.println("Lucene response: " + responseEntity);
            }
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

            String result =
                expected.check(responseEntity);
            if (result != null) {
                Assert.fail(result);
            }
        }
    }

    public String getSearchOutput(final String uri)
        throws HttpException, IOException, JsonException
    {
        return getSearchOutput(new HttpGet(searchUri() + uri));
    }

    public String getSearchOutput(final HttpUriRequest request)
        throws HttpException, IOException, JsonException
    {
        try (CloseableHttpResponse response = client.execute(request)) {
            String responseEntity =
                CharsetUtils.toString(response.getEntity());
            return responseEntity;
        }
    }

    public long getQueueId(final User user) throws HttpException, IOException {
        try (CloseableHttpResponse response =
                client.execute(
                    new HttpGet(
                        indexerHost()
                        + "/getQueueId?check-copyness=false&service="
                        + user.service() + "&prefix=" + user.prefix())))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            return Long.parseLong(CharsetUtils.toString(response.getEntity()));
        }
    }

    public void setQueueId(final User user, final long pos)
        throws HttpException, IOException
    {
        HttpPost post = new HttpPost(indexerUri() + "/delete?commit");
        post.setEntity(
            new StringEntity(
                "{$prefix\0:$" + user.prefix().toString() + "\0,$docs\0:[]}",
                ContentType.APPLICATION_JSON));
        post.addHeader(YandexHeaders.ZOO_QUEUE, user.service());
        post.addHeader(
            YandexHeaders.ZOO_SHARD_ID,
            Long.toString(user.shard()));
        post.addHeader(
            YandexHeaders.ZOO_QUEUE_ID,
            Long.toString(pos));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
    }

    @Override
    @SuppressWarnings("try")
    public void close() throws IOException {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseable<IOException>> indexDeleterHolder =
                this.indexDeleterHolder;
            CloseableHttpClient client = this.client)
        {
            lucene.close();
        }
    }

    public GenericAutoCloseable<IOException> releaseIndex() {
        return indexDeleterHolder.release();
    }

    public static IniConfig patchConfig(
        final Path root,
        final IniConfig config)
        throws ConfigException, IOException
    {
        return patchConfig(root, true, config);
    }

    public static IniConfig patchConfig(
        final Path root,
        final boolean verbose,
        final IniConfig config)
        throws ConfigException, IOException
    {
        TestBase.clearLoggerSection(config.section("access_log"));
        TestBase.clearLoggerSection(config.section("index_log"));
        TestBase.clearLoggerSection(config.section("index_access_log"));
        TestBase.clearLoggerSection(config.section("error_log"));
        TestBase.clearLoggerSection(config.section("full_log"));
        IniConfig indexerSection = config.section("indexer");
        indexerSection.sections().remove("free-space-signals");
        patchHttpServerConfig(indexerSection);
        patchHttpServerConfig(config.sectionOrNull("http"));
        patchHttpServerConfig(config.sectionOrNull("dump"));
        patchHttpServerConfig(config.sectionOrNull("search"));

        if (config.getLong("shards", -1L) > 0) {
            patchDatabaseConfig(root, "default", config);
        }

        IniConfig databases = config.section("database");
        if (databases != null && databases.sections().size() > 0) {
            for (Map.Entry<String, IniConfig> entry: databases.sections().entrySet()) {
                patchDatabaseConfig(root, entry.getKey(), entry.getValue());
            }
        }

        if (!verbose) {
            config.put("index_log.level.min", "off");
        }
        return config;
    }

    private static IniConfig patchDatabaseConfig(final Path root, final String name, final IniConfig config) {
        config.put("shards", "10");
        config.put("check-copyness", "false");
        if (config.getOrNull(XURLS_REGEX_FILE) != null) {
            config.put(
                XURLS_REGEX_FILE,
                System.getenv("ARCADIA_SOURCE_ROOT")
                    + "/mail/search/mail/search_backend_mail_config/files"
                    + "/search_backend_xurls_patterns");
        }

        System.err.println("Index Path " + resolveIndexPath(root, name).toString());
        config.put(
            "index_path",
            resolveIndexPath(root, name).toString());
        config.put("index_threads", "2");
        return config;
    }

    public static void patchHttpServerConfig(final IniConfig config) {
        if (config != null) {
            config.sections().remove("free-space-signals");
            config.put("port", Integer.toString(0));
            config.put("workers.percent", Integer.toString(0));
            config.put("workers.min", Integer.toString(WORKERS));
        }
    }

    public static Path resolveIndexPath(final Path root, final String name) {
        return root.resolve("index").resolve(name).normalize().toAbsolutePath();
    }
}

