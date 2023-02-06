package ru.yandex.mail.so.logger;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
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
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.mail.so.logger.config.AuxiliaryStorageConfig;
import ru.yandex.mail.so.logger.config.AuxiliaryStoragesConfig;
import ru.yandex.mail.so.logger.config.EnvironmentType;
import ru.yandex.mail.so.logger.config.LogRecordsHandlersConfigBuilder;
import ru.yandex.mail.so.logger.config.LogStorageConfig;
import ru.yandex.mail.so.logger.config.LogStoragesConfig;
import ru.yandex.mail.so.logger.config.RulesStatDatabasesConfig;
import ru.yandex.mail.so.logger.config.RulesStatDatabasesConfigDefaults;
import ru.yandex.mail.so.logger.config.SpLoggerConfigBuilder;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class SpLoggerCluster
    extends GenericAutoCloseableHolder<IOException, GenericAutoCloseableChain<IOException>>
    implements Cluster
{
    public static final String SP_LOGGER_QUEUE = "sologger";
    public static final String MDS_DELETES_QUEUE = "sologger_delete";
    public static final int SHARDS = 14;
    public static final long DELIVERY_LOG_TTL = 31536;
    public static final String DELIVERY_LOG_PREFIX = "delivery-log-";
    public static final String HTTP_LOCALHOST = "http://localhost";
    public static final int DEFAULT_BATCH_MIN_SIZE = 1;

    protected static final String HOST = "host = ";
    protected static final String DELIVERY_LOG_URI = "/delivery/";

    static {
        System.setProperty("LOG_DIR", ".");
        System.setProperty("INDEX_DIR", ".");
        System.setProperty("CPU_CORES", "2");
        System.setProperty("CTYPE", "testing");
        System.setProperty("NANNY_SERVICE_ID", "sp_logger");
        System.setProperty("SHARDS", Long.toString(SHARDS));
        System.setProperty("SHARDS_PER_HOST", Long.toString(SHARDS));
        System.setProperty("OLD_SEARCH_PORT", "");
        System.setProperty("SEARCH_PORT", "");
        System.setProperty("DUMP_PORT", "");
        System.setProperty("INDEXER_PORT", "");
        System.setProperty("ROUTE", "in");
        System.setProperty("QUEUE_NAME", SP_LOGGER_QUEUE);
        System.setProperty("MDS_NAMESPACE", MdsStorageCluster.MDS_NAMESPACE);
        System.setProperty("MDS_DELETE_QUEUE_NAME", MDS_DELETES_QUEUE);
        System.setProperty("SP_LOGGER_BATCH_MAX_SIZE", "200M");
        System.setProperty("SP_LOGGER_BATCHES_MEM_LIMIT", "1G");
        System.setProperty("SP_LOGGER_BATCH_SAVE_MAX_RPS", "1");
        System.setProperty("SP_LOGGER_BATCH_SAVE_TIMEOUT", "10s");
        System.setProperty("SP_LOGGER_STORAGE_RETRY_TIMEOUT", "1m");
        System.setProperty("MDS_PROD_TVM2_ID", MdsStorageCluster.TVM_CLIENT_ID);
        System.setProperty("MDS_TVM2_SECRET", MdsStorageCluster.TVM_SECRET);
        System.setProperty("SP_LOGGER_TVM2_ID", "1");
        System.setProperty("SEARCHMAP_PATH", "searchmap-testing.txt");
        System.setProperty("DELIVERYLOG_TTL", Long.toString(DELIVERY_LOG_TTL));
        System.setProperty("DELIVERYLOG_PATH", "delivery.log");
        System.setProperty("SPARE_DELIVERYLOG_PATH", "spare_delivery.log");
    }

    protected final Logger logger;
    protected final TestSearchBackend lucene;
    protected final StaticServer producer;
    protected final StaticServer producerAsyncClient;
    protected final StorageCluster<LogStorageConfig> logStorage;
    protected final StorageCluster<AuxiliaryStorageConfig> auxiliaryStorage;
    protected final Cluster db;
    protected final SpLogger spLogger;
    protected final GenericAutoCloseableChain<IOException> chain;
    protected final Route route;
    protected final LogStorageType logStorageType;
    protected final AuxiliaryStorageType auxiliaryStorageType;
    protected final RulesStatDatabaseType rulesStatDatabaseType;
    protected final String logStorageSectionName;
    protected final String auxiliaryStorageSectionName;
    protected final String rulesStatSectionName;

    public SpLoggerCluster(final TestBase testBase) throws Exception {
        this(testBase, DEFAULT_BATCH_MIN_SIZE);
    }

    public SpLoggerCluster(final TestBase testBase, final long batchMinSize) throws Exception
    {
        this(
            testBase,
            batchMinSize,
            Route.IN,
            LogStorageType.MDS,
            AuxiliaryStorageType.LOGS_CONSUMER,
            RulesStatDatabaseType.NULL);
    }

    public SpLoggerCluster(final TestBase testBase, final long batchMinSize, final Route route) throws Exception
    {
        this(
            testBase,
            batchMinSize,
            route,
            LogStorageType.MDS,
            AuxiliaryStorageType.LOGS_CONSUMER,
            RulesStatDatabaseType.NULL);
    }

    public SpLoggerCluster(
        final TestBase testBase,
        final long batchMinSize,
        final Route route,
        final LogStorageType logStorageType,
        final AuxiliaryStorageType auxiliaryStorageType,
        final RulesStatDatabaseType rulesStatDatabaseType)
        throws Exception
    {
        logger = testBase.logger;
        this.route = route;
        this.logStorageType = logStorageType == null ? LogStorageType.NULL : logStorageType;
        this.auxiliaryStorageType = auxiliaryStorageType == null ? AuxiliaryStorageType.NULL : auxiliaryStorageType;
        this.rulesStatDatabaseType = rulesStatDatabaseType == null ? RulesStatDatabaseType.NULL : rulesStatDatabaseType;
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
                new File(
                    Paths.getSourcePath(
                        "mail/so/daemons/sp_logger/sp_logger_service_config/files/search_backend.conf")));
            chain.get().add(lucene);

            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);
            producer.add("/_status?service=" + SP_LOGGER_QUEUE + "&prefix=*", "[{$localhost\0:100500}]");

            System.setProperty("PRODUCER_PORT", Integer.toString(producer.port()));
            System.setProperty("PRODUCER_INDEXING_HOST", producer.host().toString());
            System.setProperty("SP_LOGGER_BATCH_MIN_SIZE", Long.toString(batchMinSize));
            System.setProperty("SP_LOGGER_PORT", Long.toString(port()));

            if (this.logStorageType == LogStorageType.MDS) {
                logStorage = new MdsStorageCluster(batchMinSize, logger);
                logStorageSectionName = MdsStorageCluster.LOG_STORAGE;
            } else {
                logStorage = new NullStorageCluster<>(logger);
                logStorageSectionName = NullLogStorage.NULL;
            }
            chain.get().add(logStorage);
            logStorage.start();

            if (this.auxiliaryStorageType == AuxiliaryStorageType.LOGS_CONSUMER) {
                auxiliaryStorage = new AuxiliaryStorageCluster(batchMinSize, logger);
                auxiliaryStorageSectionName = AuxiliaryStorageCluster.AUXILIARY_STORAGE;
            } else {
                auxiliaryStorage = new NullStorageCluster<>(logger);
                auxiliaryStorageSectionName = NullAuxiliaryStorage.NULL;
            }
            chain.get().add(auxiliaryStorage);
            auxiliaryStorage.start();

            StaticHttpResource searchBackendIndexProxy = new StaticHttpResource(new ProxyHandler(lucene.indexerPort()));

            producer.add("/update?*", new ProxyMultipartHandler(lucene.indexerPort()));
            producer.add("/add?*", new ProxyMultipartHandler(lucene.indexerPort()));
            if (this.logStorageType != LogStorageType.NULL) {
                producer.add("/delete*", new ProxyMultipartHandler(logStorage.writePort()));
            }
            producer.add("/modify?*", searchBackendIndexProxy);
            producer.add("/ping*", searchBackendIndexProxy);

            producerAsyncClient.add("/update?*", new ProxyMultipartHandler(lucene.indexerPort()));
            producerAsyncClient.add("/add?*", new ProxyMultipartHandler(lucene.indexerPort()));
            if (this.logStorageType != LogStorageType.NULL) {
                producerAsyncClient.add("/delete*", new ProxyMultipartHandler(logStorage.writePort()));
            }
            producerAsyncClient.add("/modify?*", searchBackendIndexProxy);
            producerAsyncClient.add("/ping*", searchBackendIndexProxy);

            if (this.rulesStatDatabaseType != RulesStatDatabaseType.NULL) {
                db = createDbCluster(testBase.logger);
                if (db == null) {
                    logger.info("DB cluster not started!");
                } else {
                    chain.get().add(db);
                    db.start();
                    logger.info("DB cluster started on port " + db.port());
                }
            } else {
                db = null;
            }

            logger.info("SpLoggerCluster: ProducerPort = " + producer.port() + ", ProducerAsyncPort = "
                + producerAsyncClient.port() + ", LuceneIndexerPort = " + lucene.indexerPort() + ", LuceneSearchPort = "
                + lucene.searchPort() + ", MdsWritePort = " + logStorage.writePort());
            SpLoggerConfigBuilder builder = new SpLoggerConfigBuilder();
            builder.port(0)
                .connections(20)
                .timeout(10000)
                .route(Route.IN)
                .envType(EnvironmentType.TESTING)
                .indexingQueueName(SP_LOGGER_QUEUE);
            //builder.tvm2ServiceConfig(tvm2ServiceConfig);
            builder.searchMapConfig(
                new SearchMapConfigBuilder().content(
                    lucene.searchMapRule(SP_LOGGER_QUEUE, 0, SHARDS)
                    + MDS_DELETES_QUEUE + " host:localhost,target_host:localhost,consumer_name:mds_delete,shards:0-"
                    + (SHARDS - 1) + ",json_indexer_port:" + logStorage.writePort() + '\n'));
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

            if (this.logStorageType != LogStorageType.NULL) {
                builder.logStoragesConfig((LogStoragesConfig) logStorage.storagesConfig());
            }
            if (this.auxiliaryStorageType != AuxiliaryStorageType.NULL) {
                builder.auxiliaryStoragesConfig((AuxiliaryStoragesConfig) auxiliaryStorage.storagesConfig());
            }
            if (this.rulesStatDatabaseType == RulesStatDatabaseType.NULL) {
                rulesStatSectionName = NullRulesStatDatabase.NULL;
            } else {
                rulesStatSectionName = DELIVERY_LOG_PREFIX + this.rulesStatDatabaseType.name().toLowerCase(Locale.ROOT);
            }
            builder.rulesStatDatabasesConfig(createRulesStatDbConfig());
            IniConfig logRecordsHandlersIni;
            if (route == null) {
                logRecordsHandlersIni = new IniConfig(new StringReader(
                    "[log-handler./]"
                    + "\ntype = null"
                    + "\nstorage-type = " + logStorageSectionName
                    + "\nauxiliary-storage = " + auxiliaryStorageSectionName
                    + "\nrules-stat-db = " + rulesStatSectionName
                ));
            } else {
                logRecordsHandlersIni = new IniConfig(new StringReader(
                    "[log-handler." + DELIVERY_LOG_URI + route.lowerName() + "]"
                    + "\ntype = sp-daemon"
                    + "\nstorage-type = " + logStorageSectionName
                    + "\nauxiliary-storage = " + auxiliaryStorageSectionName
                    + "\nrules-stat-db = " + rulesStatSectionName
                    + "\ndecompression = lzo"
                    + "\nstore-ttl = " + (DELIVERY_LOG_TTL * 1000)
                    + "\nroute = " + route.lowerName()
                ));
            }
            LogRecordsHandlersConfigBuilder logRecordsHandlersConfig =
                new LogRecordsHandlersConfigBuilder(
                    logRecordsHandlersIni.section(LogRecordsHandlersConfigBuilder.SECTION));
            logRecordsHandlersIni.checkUnusedKeys();
            logger.info("SpLoggerCluster: route = " + route + ", logStorageType = " + logStorageType
                + ", rulesStatDatabaseType = " + rulesStatDatabaseType + ", rulesStatSectionName = "
                + rulesStatSectionName + ", logStorageSectionName = " + logStorageSectionName
                + ", auxiliaryStorageSectionName = " + auxiliaryStorageSectionName);
            builder.logRecordsHandlersConfig(logRecordsHandlersConfig);
            spLogger = new SpLogger(builder.build());
            chain.get().add(spLogger);
            this.chain = chain.release();
        }
    }

    @Override
    public void start() throws Exception {
        producer.start();
        spLogger.start();
        HttpAssert.stats(spLogger.port());
    }

    @Override
    public void close() throws IOException {
        logger.info("SpLoggerCluster shutdown started");
        chain.close();
        super.close();
    }

    @Override
    public int port() throws IOException {
        return 0;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    public TestSearchBackend lucene() {
        return lucene;
    }

    public StaticServer producer() {
        return producer;
    }

    public SpLogger spLogger() {
        return spLogger;
    }

    public StorageCluster<LogStorageConfig> logStorage() {
        return logStorage;
    }

    public StorageCluster<AuxiliaryStorageConfig> auxiliaryStorage() {
        return auxiliaryStorage;
    }

    public Cluster db() {
        return db;
    }

    protected Cluster createDbCluster(final PrefixedLogger logger) throws IOException {
        return new MockMongoDbCluster(logger);
    }

    protected RulesStatDatabasesConfig<BasicRoutedLogRecordProducer> createRulesStatDbConfig()
        throws ConfigException, IOException
    {
        return RulesStatDatabasesConfigDefaults.INSTANCE;
    }

    public StaticServer producerAsyncClient() {
        return producerAsyncClient;
    }

    public long prefix(final String s) {
        return LogRecordContext.prefix(s, spLogger);
    }

    public Route route() {
        return route;
    }

    public LogStorageType logStorageType() {
        return logStorageType;
    }

    public AuxiliaryStorageType auxiliaryStorageType() {
        return auxiliaryStorageType;
    }

    public RulesStatDatabaseType rulesStatDatabaseType() {
        return rulesStatDatabaseType;
    }

    public String logStorageSectionName() {
        return logStorageSectionName;
    }

    public String auxiliaryStorageSectionName() {
        return auxiliaryStorageSectionName;
    }

    public String rulesStatSectionName() {
        return rulesStatSectionName;
    }
}
