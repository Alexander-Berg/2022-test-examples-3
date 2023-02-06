package ru.yandex.mail.so.logger;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mail.so.logger.config.ImmutableLogStoragesConfig;
import ru.yandex.mail.so.logger.config.LogStorageConfig;
import ru.yandex.mail.so.logger.config.LogStoragesConfigBuilder;
import ru.yandex.mail.so.logger.config.MdsLogStorageConfig;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.Version;

public class MdsStorageCluster implements StorageCluster<LogStorageConfig> {
    public static final String MDS_NAMESPACE = MdsLogStorageConfig.DEFAULTS.mdsNamespace();
    public static final String LOG_STORAGE = SpLoggerCluster.DELIVERY_LOG_PREFIX + "mds";
    public static final String TVM_CLIENT_ID = "2";
    public static final String TVM_SECRET = "1234567890123456789012";

    private static final String MDS_ANSWER_TEMPLATE_PATH = "mds_answer.xml.template";
    private static final int CONNECTIONS = 100;
    private static final String TVM_TICKET = "2:53:1522749939:116:CDcQ";
    private static final String TVM2_TICKET = "3:serv:MDSSTORA";
    private static final long TVM_RENEWAL_INTERVAL = 60000L;

    private final CloseableHttpClient client = Configs.createDefaultClient();
    private final GenericAutoCloseableChain<IOException> chain;
    private final Map<String, HttpResource> data = new HashMap<>();
    private final MdsStaticServer toWriteServer;
    private final MdsStaticServer toReadServer;
    private final LogStoragesConfigBuilder logStoragesConfig;
    private final Logger logger;

    public MdsStorageCluster(final long batchMinSize, final Logger logger) throws Exception {
        this.logger = logger;
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(new GenericAutoCloseableChain<>()))
        {
            StaticServer tvm2 = new StaticServer(Configs.baseConfig("tvm2"));
            chain.get().add(tvm2);
            tvm2.add("/ticket/", TVM_TICKET);
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add("/2/ticket/", "{\"" + TVM_CLIENT_ID + "\":{\"ticket\":\"" + TVM2_TICKET + "\"}}");
            tvm2.start();
            Tvm2ServiceConfigBuilder tvm2ServiceConfig = new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2)).copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            toWriteServer = new MdsStaticServer(Configs.baseConfig("MdsWriteServer"), this);
            chain.get().add(toWriteServer);
            toWriteServer.handler().add(MdsRequestHandler.HOSTNAME_URI, HttpServer.LOCALHOST);

            toReadServer = new MdsStaticServer(Configs.baseConfig("MdsReadServer"), this);
            chain.get().add(toReadServer);

            Tvm2ClientConfigBuilder tvm2ClientConfig =
                new Tvm2ClientConfigBuilder().destinationClientId(TVM_CLIENT_ID).renewalInterval(TVM_RENEWAL_INTERVAL);
            IniConfig logStoragesConfigSection = new IniConfig(new StringReader(
                "[" + LOG_STORAGE + "]"
                + "\ntype = mds"
                + "\nstore-ttl = 365d"
                + "\nnamespace = " + MDS_NAMESPACE
                + "\nmds-deletes-queue-name = " + SpLoggerCluster.MDS_DELETES_QUEUE
                + "\nworkers = 128"
                + "\nbatch-min-size = " + batchMinSize
                + "\nbatch-max-size = 200k"
                + "\nbatch-save-period = 2s"
                + "\nbatch-save-retries = 2"
                + "\nsaving-operation-timeout = 2s"
                + "\nto-write.host = localhost"
                + "\nto-write.port = " + toWriteServer.port()
                + "\nto-write.connections = " + CONNECTIONS
                + "\nto-write.timeout = 20s"
                + "\nto-read.host = localhost"
                + "\nto-read.port = " + toReadServer.port()
                + "\nto-read.connections = " + CONNECTIONS
                + "\nto-read.timeout = 20s"
                + "\n[" + LOG_STORAGE + "." + MdsLogStorageConfig.TVM2 + "]"
                + "\ndestination-client-id = " + TVM_CLIENT_ID
                + "\nrenewal-interval = " + TVM_RENEWAL_INTERVAL
                + "\nclient-id = " + TVM_CLIENT_ID
                + "\nsecret = " + TVM_SECRET
                + "\nhost = localhost:" + tvm2.port()
                + "\nconnections = " + CONNECTIONS + '\n'));
            logStoragesConfig = new LogStoragesConfigBuilder(logStoragesConfigSection);
            logStoragesConfigSection.checkUnusedKeys();
            MdsLogStorageConfig mdsLogStorageConfig = storageConfig();
            mdsLogStorageConfig.tvm2ServiceConfig(tvm2ServiceConfig);
            mdsLogStorageConfig.tvm2ClientConfig(tvm2ClientConfig);
            this.chain = chain.release();
        }
    }

    @Override
    public void start() throws IOException {
        toWriteServer.start();
        toReadServer.start();
        logger.info("MdsStorageCluster STARTED");
    }

    @Override
    @SuppressWarnings("try")
    public void close() throws IOException {
        logger.info("MdsStorageCluster SHUTDOWN");
        try (CloseableHttpClient ignored = this.client) {
            chain.close();
        }
    }

    @Override
    public ImmutableLogStoragesConfig storagesConfig() throws ConfigException {
        return logStoragesConfig.build();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public int writePort() throws IOException {
        return toWriteServer.port();
    }

    @Override
    @SuppressWarnings("unused")
    public int readPort() throws IOException {
        return toReadServer.port();
    }

    @Override
    public int port() throws IOException {
        return toWriteServer.httpPort();
    }

    @Override
    public void add(final String uri, final HttpResource resource) {
        data.put(uriPreprocessor().apply(uri), resource);
    }

    @Override
    public void addBlock(final String stid, final String body) {
        add(getUri(stid), body);
        add(deleteUri(stid), new StaticHttpResource(HttpStatus.SC_OK));
    }

    @Override
    public void deleteBlock(final String stid) {
        data.remove(getUri(stid));
        add(getUri(stid), new StaticHttpResource(HttpStatus.SC_NOT_FOUND));
        add(deleteUri(stid), new StaticHttpResource(HttpStatus.SC_NOT_FOUND));
        logger.info("MdsStorageCluster.deleteBlock: stid = " + stid);
    }

    public String getUri(final String stid) {
        return MdsRequestHandler.GET_URI + storageConfig().mdsNamespace() + "/" + stid;
    }

    public String deleteUri(final String stid) {
        return MdsRequestHandler.DELETE_URI + storageConfig().mdsNamespace() + "/" + stid;
    }

    public String uploadResponse(final String stid) throws IOException {
        return
            new String(getClass().getResourceAsStream(MDS_ANSWER_TEMPLATE_PATH).readAllBytes(), StandardCharsets.UTF_8)
                .replaceAll("\\$\\{stid}", stid);
    }

    @Override
    public MdsLogStorageConfig storageConfig() {
        return (MdsLogStorageConfig) logStoragesConfig.storageConfigs().get(LOG_STORAGE);
    }

    @Override
    public Map<String, HttpResource> data() {
        return data;
    }
}
