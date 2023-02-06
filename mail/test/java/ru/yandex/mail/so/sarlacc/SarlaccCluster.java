package ru.yandex.mail.so.sarlacc;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.mail.so.sarlacc.config.SarlaccConfigBuilder;
import ru.yandex.mail.so.sarlacc.config.ShinglersConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class SarlaccCluster
    extends GenericAutoCloseableHolder<IOException, GenericAutoCloseableChain<IOException>>
{
    public static final String SARLACC_QUEUE = "sarlacc";
    public static final int SHARDS = 14;
    public static final String HTTP_LOCALHOST = "http://localhost";
    public static final int DEFAULT_BATCH_MIN_SIZE = 1;
    public static final String MASS_IN = "/mass-in/";

    protected static final String HOST = "host = ";

    static {
        System.setProperty("LOG_DIR", ".");
        System.setProperty("INDEX_DIR", ".");
        System.setProperty("CPU_CORES", "2");
        System.setProperty("CTYPE", "testing");
        System.setProperty("NANNY_SERVICE_ID", "sarlacc");
        System.setProperty("SHARDS", Long.toString(SHARDS));
        System.setProperty("SHARDS_PER_HOST", Long.toString(SHARDS));
        System.setProperty("LUCENE_PORT", "");
        System.setProperty("OLD_SEARCH_PORT", "");
        System.setProperty("LUCENE_SEARCHER_PORT", "");
        System.setProperty("LUCENE_DUMP_PORT", "");
        System.setProperty("LUCENE_INDEXER_PORT", "");
        System.setProperty("QUEUE_NAME", SARLACC_QUEUE);
        System.setProperty("SARLACC_BATCH_MAX_SIZE", "200M");
        System.setProperty("SARLACC_BATCHES_MEM_LIMIT", "1G");
        System.setProperty("SARLACC_BATCH_SAVE_MAX_RPS", "1");
        System.setProperty("SARLACC_BATCH_SAVE_TIMEOUT", "10s");
        System.setProperty("SARLACC_STORAGE_RETRY_TIMEOUT", "1m");
        System.setProperty("SEARCHMAP_PATH", "searchmap-testing.txt");
    }

    protected final Logger logger;
    protected final TestSearchBackend lucene;
    protected final StaticServer producer;
    protected final StaticServer producerAsyncClient;
    protected final Sarlacc sarlacc;
    protected final GenericAutoCloseableChain<IOException> chain;
    protected final ShinglerType shinglerType;

    public SarlaccCluster(final TestBase testBase) throws Exception {
        this(testBase, DEFAULT_BATCH_MIN_SIZE);
    }

    public SarlaccCluster(final TestBase testBase, final long batchMinSize) throws Exception
    {
        this(testBase, batchMinSize, ShinglerType.MASS_IN);
    }

    public SarlaccCluster(final TestBase testBase, final long batchMinSize, final ShinglerType shinglerType)
        throws Exception
    {
        logger = testBase.logger;
        this.shinglerType = shinglerType == null ? ShinglerType.NULL : shinglerType;
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(new GenericAutoCloseableChain<>()))
        {
            producerAsyncClient = new StaticServer(Configs.baseConfig("producer-server-mock"));
            chain.get().add(producerAsyncClient);
            producerAsyncClient.start();

            lucene = new TestSearchBackend(
                testBase,
                new File(Paths.getSourcePath("mail/so/daemons/sarlacc/sarlacc_backend/files/lucene.conf")));
            chain.get().add(lucene);

            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);
            producer.add("/_status?service=" + SARLACC_QUEUE + "&prefix=*", "[{$localhost\0:100500}]");

            System.setProperty("PRODUCER_PORT", Integer.toString(producer.port()));
            System.setProperty("PRODUCER_INDEXING_HOST", producer.host().toString());
            System.setProperty("SARLACC_BATCH_MIN_SIZE", Long.toString(batchMinSize));
            System.setProperty("SARLACC_PORT", Long.toString(port()));

            StaticHttpResource searchBackendIndexProxy = new StaticHttpResource(new ProxyHandler(lucene.indexerPort()));

            producer.add("/update?*", new ProxyMultipartHandler(lucene.indexerPort()));
            producer.add("/add?*", new ProxyMultipartHandler(lucene.indexerPort()));
            producer.add("/modify?*", searchBackendIndexProxy);
            producer.add("/ping*", searchBackendIndexProxy);

            producerAsyncClient.add("/update?*", new ProxyMultipartHandler(lucene.indexerPort()));
            producerAsyncClient.add("/add?*", new ProxyMultipartHandler(lucene.indexerPort()));
            producerAsyncClient.add("/modify?*", searchBackendIndexProxy);
            producerAsyncClient.add("/ping*", searchBackendIndexProxy);

            logger.info("SarlaccCluster: ProducerPort = " + producer.port() + ", ProducerAsyncPort = "
                + producerAsyncClient.port() + ", LuceneIndexerPort = " + lucene.indexerPort() + ", LuceneSearchPort = "
                + lucene.searchPort());
            SarlaccConfigBuilder builder = new SarlaccConfigBuilder();
            builder.port(0)
                .connections(20)
                .timeout(10000)
                //.envType(EnvironmentType.TESTING)
                .indexingQueueName(SARLACC_QUEUE);
            //builder.tvm2ServiceConfig(tvm2ServiceConfig);
            builder.searchMapConfig(
                new SearchMapConfigBuilder().content(lucene.searchMapRule(SARLACC_QUEUE, 0, SHARDS)));
            //builder.searchConfig(Configs.targetConfig());
            builder.indexerConfig(Configs.targetConfig());
            builder.upstreamsConfig(new UpstreamsConfigBuilder().asterisk(new UpstreamConfigBuilder().connections(2)));
            IniConfig producerConfig = new IniConfig(new StringReader(
                HOST + HTTP_LOCALHOST + ':' + producer.port()
                    + "\nconnections = 122"
                    + "\ncache-ttl = 0"));
            ProducerClientConfigBuilder producerClientConfigBuilder = new ProducerClientConfigBuilder(producerConfig);
            builder.producerClientConfig(producerClientConfigBuilder.build());
            IniConfig producerAsyncClientConfig = new IniConfig(new StringReader(
                HOST + HTTP_LOCALHOST + ':' + producerAsyncClient.port()
                    + "\nconnections = 122"
                    + "\ncache-ttl = 0"));
            HttpHostConfigBuilder producerAsyncClientConfigBuilder =
                new HttpHostConfigBuilder(producerAsyncClientConfig);
            builder.producerAsyncClientConfig(producerAsyncClientConfigBuilder.build());

            IniConfig luceneConfig = new IniConfig(new StringReader(HOST + "\nconnections = 10"));
            HttpTargetConfigBuilder testLuceneConfigBuilder = new HttpTargetConfigBuilder(luceneConfig);
            builder.searchConfig(testLuceneConfigBuilder.build());

            IniConfig shinglersHandlersIni;
            if (shinglerType == null) {
                shinglersHandlersIni = new IniConfig(new StringReader(
                    "[shingler./]"
                    + "\ntype = null"
                ));
            } else {
                shinglersHandlersIni = new IniConfig(new StringReader(
                    "[shingler." + MASS_IN + "]"
                    + "\ntype = mass-in"
                ));
            }
            ShinglersConfigBuilder shinglersConfig =
                new ShinglersConfigBuilder(shinglersHandlersIni.section(ShinglersConfigBuilder.SECTION));
            shinglersHandlersIni.checkUnusedKeys();
            logger.info("SarlaccCluster: shinglerType = " + shinglerType);
            builder.shinglersConfig(shinglersConfig);
            sarlacc = new Sarlacc(builder.build());
            chain.get().add(sarlacc);
            this.chain = chain.release();
        }
    }

    public void start() throws Exception {
        producer.start();
        sarlacc.start();
        HttpAssert.stats(sarlacc.port());
    }

    @Override
    public void close() throws IOException {
        logger.info("SarlaccCluster shutdown started");
        chain.close();
        super.close();
    }

    public int port() throws IOException {
        return 0;
    }

    public Logger logger() {
        return logger;
    }

    public TestSearchBackend lucene() {
        return lucene;
    }

    public StaticServer producer() {
        return producer;
    }

    public Sarlacc sarlacc() {
        return sarlacc;
    }

    public StaticServer producerAsyncClient() {
        return producerAsyncClient;
    }

    /* public long prefix(final String s) {
        return LogRecordContext.prefix(s, sarlacc);
    }*/

    public ShinglerType shinglerType() {
        return shinglerType;
    }

    public String loadResource(final String resourcePath) throws Exception {
        return loadFile(getClass().getResource(resourcePath).toURI());
    }

    public String loadSource(final String sourcePath) throws Exception {
        return loadFile(new File(ru.yandex.devtools.test.Paths.getSourcePath(sourcePath)).toPath());
    }

    public static String loadFile(final URI pathUri) throws Exception {
        Path path = java.nio.file.Paths.get(pathUri);
        return loadFile(path);
    }

    public static String loadFile(final Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }

    public void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            logger().warning("Cluster.sleep interrupted: " + e);
        }
    }
}
