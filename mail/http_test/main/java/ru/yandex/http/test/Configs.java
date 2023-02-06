package ru.yandex.http.test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import ru.yandex.http.config.AbstractHttpTargetConfigBuilder;
import ru.yandex.http.config.ClientHttpsConfigBuilder;
import ru.yandex.http.config.DnsConfigBuilder;
import ru.yandex.http.config.FilterSearchConfigBuilder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.ImmutableClientHttpsConfig;
import ru.yandex.http.config.ImmutableDnsConfig;
import ru.yandex.http.config.ImmutableFilterSearchConfig;
import ru.yandex.http.config.ImmutableHttpHostConfig;
import ru.yandex.http.config.ImmutableHttpTargetConfig;
import ru.yandex.http.config.ImmutableURIConfig;
import ru.yandex.http.config.KeyStoreConfigBuilder;
import ru.yandex.http.config.ServerHttpsConfigBuilder;
import ru.yandex.http.config.URIConfigBuilder;
import ru.yandex.http.server.async.BaseAsyncServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.BasicAsyncResponseConsumerFactory;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.passport.tvmauth.Version;

public final class Configs {
    private static final int TIMEOUT = 20000;
    private static final int CONNECTIONS = 20;
    private static final String TLS = "TLS";
    private static final String PKCS12 = "PKCS12";
    private static final String PKIX = "PKIX";
    private static final String KEYSTOREPASSWORD = "keystorepassword";
    private static final String CAPASSWORD = "capassword";
    private static final String NAME = "StaticServer";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final ImmutableBaseServerConfig DEFAULT_SERVER_CONFIG;
    private static final ImmutableDnsConfig DEFAULT_DNS_CONFIG;
    private static final URL KEYSTORE =
        Configs.class.getResource("keystore.jks");
    private static final URL CA_CERT = Configs.class.getResource("ca.jks");
    private static final int ENTROPY_PREPARE_SIZE = 2048;
    private static final ImmutableHttpTargetConfig DEFAULT_HTTP_TARGET_CONFIG;
    private static ImmutableClientHttpsConfig defaultClientHttpsConfig;
    private static ImmutableClientHttpsConfig defaultClientHttpsBcConfig;
    private static ImmutableHttpTargetConfig defaultTargetConfig;
    private static ImmutableHttpTargetConfig defaultTargetBcConfig;
    private static final DecodableByteArrayOutputStream TVM_KEYS;

    static {
        try {
            TVM_KEYS = IOStreamUtils.consume(
                Configs.class.getResourceAsStream("tvm-keys.txt"));

            DEFAULT_SERVER_CONFIG = createBaseConfig(0, NAME);
            DEFAULT_DNS_CONFIG = new DnsConfigBuilder().build();

            DEFAULT_HTTP_TARGET_CONFIG =
                disableHttps(new HttpTargetConfigBuilder())
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .connectTimeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)
                    .sessionTimeout(TIMEOUT)
                    .build();
            try (StaticServer staticServer =
                    new StaticServer(DEFAULT_SERVER_CONFIG);
                BaseAsyncServer<ImmutableBaseServerConfig> asyncServer =
                    new BaseAsyncServer<>(
                        new BaseServerConfigBuilder(DEFAULT_SERVER_CONFIG)
                            .name("AsyncServer")
                            .build());
                CloseableHttpClient syncClient =
                    ClientBuilder.createClient(
                        DEFAULT_HTTP_TARGET_CONFIG,
                        DEFAULT_DNS_CONFIG);
                SharedConnectingIOReactor reactor =
                    new SharedConnectingIOReactor(
                        DEFAULT_SERVER_CONFIG,
                        DEFAULT_DNS_CONFIG);
                AsyncClient asyncClient =
                    new AsyncClient(reactor, DEFAULT_HTTP_TARGET_CONFIG))
            {
                staticServer.logger().info("Pre-tests warmup");
                staticServer.start();
                asyncServer.start();
                reactor.start();
                asyncClient.start();
                try (CloseableHttpResponse response =
                        syncClient.execute(
                            new HttpGet(staticServer.host() + "/stat")))
                {
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                }
                asyncClient.execute(
                    asyncServer.host(),
                    new BasicAsyncRequestProducerGenerator("/stat"),
                    BasicAsyncResponseConsumerFactory.INSTANCE,
                    EmptyFutureCallback.INSTANCE)
                    .get();
                staticServer.logger().info("Warmup completed");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Configs() {
    }

    private static void prepareEntropy() throws IOException {
        Path tmpFile = Files.createTempFile(null, null);
        try (OutputStream out =
                Files.newOutputStream(
                    tmpFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.DSYNC,
                    StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.SYNC))
        {
            for (int i = 0; i < ENTROPY_PREPARE_SIZE; ++i) {
                out.write((byte) i);
                out.flush();
            }
        }
    }

    private static <T extends AbstractHttpTargetConfigBuilder<T>>
        T disableHttps(final T config)
    {
        return config.httpsConfig(
            new ClientHttpsConfigBuilder()
                .protocol(TLS)
                .keyStoreConfig(null)
                .verifyHostname(false)
                .verifyCertificate(false));
    }

    private static ImmutableBaseServerConfig createBaseConfig(
        final int port,
        final String name)
        throws ConfigException
    {
        return new BaseServerConfigBuilder()
            .port(port)
            .name(name)
            .connections(CONNECTIONS)
            .workers(2)
            .timeout(TIMEOUT)
            .build();
    }

    private static ServerHttpsConfigBuilder defaultServerHttpsConfig() {
        return new ServerHttpsConfigBuilder()
            .protocol(TLS)
            .keyStoreConfig(
                new KeyStoreConfigBuilder()
                    .file(KEYSTORE)
                    .type(PKCS12)
                    .password(KEYSTOREPASSWORD));
    }

    private static ServerHttpsConfigBuilder defaultServerHttpsBcConfig() {
        return new ServerHttpsConfigBuilder()
            .protocol(TLS)
            .keyManagerAlgorithm(PKIX)
            .jsseProvider(BouncyCastleJsseProvider.PROVIDER_NAME)
            .secureRandomAlgorithm("NONCEANDIV")
            .secureRandomProvider(BouncyCastleProvider.PROVIDER_NAME)
            .keyStoreConfig(
                new KeyStoreConfigBuilder()
                    .file(KEYSTORE)
                    .type(PKCS12)
                    .password(KEYSTOREPASSWORD));
    }

    private static ClientHttpsConfigBuilder clientHttpsConfigBuilder() {
        return new ClientHttpsConfigBuilder()
            .protocol(TLS)
            .keyStoreConfig(
                new KeyStoreConfigBuilder()
                    .file(CA_CERT)
                    .type(PKCS12)
                    .password(CAPASSWORD));
    }

    private static ImmutableClientHttpsConfig defaultClientHttpsConfig() {
        if (defaultClientHttpsConfig == null) {
            System.err.println("Preparing default client https config");
            try {
                prepareEntropy();
                defaultClientHttpsConfig =
                    clientHttpsConfigBuilder().build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.err.println("Default client https config created");
        }
        return defaultClientHttpsConfig;
    }

    private static ClientHttpsConfigBuilder clientHttpsBcConfigBuilder() {
        return new ClientHttpsConfigBuilder()
            .protocol(TLS)
            .trustManagerAlgorithm(PKIX)
            .jsseProvider(BouncyCastleJsseProvider.PROVIDER_NAME)
            .secureRandomAlgorithm("DEFAULT")
            .secureRandomProvider(BouncyCastleProvider.PROVIDER_NAME)
            .keyStoreConfig(
                new KeyStoreConfigBuilder()
                    .file(CA_CERT)
                    .type(PKCS12)
                    .password(CAPASSWORD));
    }

    private static ImmutableClientHttpsConfig defaultClientHttpsBcConfig() {
        if (defaultClientHttpsBcConfig == null) {
            System.err.println("Preparing default client BC https config");
            try {
                prepareEntropy();
                defaultClientHttpsBcConfig =
                    clientHttpsBcConfigBuilder().build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.err.println("Default client BC https config created");
        }
        return defaultClientHttpsBcConfig;
    }

    private static ImmutableHttpTargetConfig defaultTargetConfig() {
        if (defaultTargetConfig == null) {
            System.err.println("Preparing default target config");
            try {
                prepareEntropy();
                defaultTargetConfig = new HttpTargetConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .connectTimeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)
                    .sessionTimeout(TIMEOUT)
                    .httpsConfig(clientHttpsConfigBuilder())
                    .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.err.println("Default target config created");
        }
        return defaultTargetConfig;
    }

    private static ImmutableHttpTargetConfig defaultTargetBcConfig() {
        if (defaultTargetBcConfig == null) {
            System.err.println("Preparing default BC target config");
            try {
                prepareEntropy();
                defaultTargetBcConfig = new HttpTargetConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .connectTimeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)
                    .sessionTimeout(TIMEOUT)
                    .httpsConfig(clientHttpsBcConfigBuilder())
                    .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.err.println("Default BC target config created");
        }
        return defaultTargetBcConfig;
    }

    public static ImmutableBaseServerConfig baseConfig()
        throws ConfigException
    {
        return baseConfig(NAME);
    }

    public static ImmutableBaseServerConfig baseConfig(final int port)
        throws ConfigException
    {
        return baseConfig(port, NAME);
    }

    public static ImmutableBaseServerConfig baseConfig(final String name)
        throws ConfigException
    {
        return baseConfig(0, name);
    }

    public static ImmutableBaseServerConfig baseConfig(
        final int port,
        final String name)
        throws ConfigException
    {
        if (port == 0 && NAME.equals(name)) {
            return DEFAULT_SERVER_CONFIG;
        } else {
            return createBaseConfig(port, name);
        }
    }

    public static ImmutableHttpTargetConfig targetConfig() {
        return DEFAULT_HTTP_TARGET_CONFIG;
    }

    public static ImmutableHttpTargetConfig targetConfig(final boolean bc) {
        if (bc) {
            return defaultTargetBcConfig();
        } else {
            return defaultTargetConfig();
        }
    }

    public static ImmutableHttpTargetConfig targetConfig(int timeout) {
        try {
            return new HttpTargetConfigBuilder()
                .connections(CONNECTIONS)
                .timeout(timeout)
                .connectTimeout(timeout)
                .poolTimeout(timeout)
                .sessionTimeout(timeout)
                .httpsConfig(clientHttpsBcConfigBuilder())
                .build();
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    public static ImmutableHttpHostConfig hostConfig(
        final HttpServer<?, ?> server)
        throws ConfigException, IOException
    {
        return hostConfig(server.port());
    }

    public static ImmutableHttpHostConfig hostConfig(final int port)
        throws ConfigException
    {
        return hostConfig(new HttpHost("localhost", port));
    }

    public static ImmutableHttpHostConfig hostConfig(final HttpHost host)
        throws ConfigException
    {
        return disableHttps(new HttpHostConfigBuilder())
            .host(host)
            .connections(CONNECTIONS)
            .timeout(TIMEOUT)
            .build();
    }

    public static ImmutableFilterSearchConfig filterSearchConfig(
        final HttpServer<?, ?> server)
        throws ConfigException, IOException, URISyntaxException
    {
        return filterSearchConfig(server, "/filter_search");
    }

    public static ImmutableFilterSearchConfig filterSearchConfig(
        final HttpServer<?, ?> server,
        final String uri)
        throws ConfigException, IOException, URISyntaxException
    {
        return filterSearchConfig(server, uri, 2);
    }

    public static ImmutableFilterSearchConfig filterSearchConfig(
        final HttpServer<?, ?> server,
        final String uri,
        final int batchSize)
        throws ConfigException, IOException, URISyntaxException
    {
        return disableHttps(new FilterSearchConfigBuilder())
            .connections(CONNECTIONS)
            .uri(new URI(HTTP_LOCALHOST + server.port() + uri))
            .batchSize(batchSize)
            .build();
    }

    public static ImmutableURIConfig uriConfig(
        final HttpServer<?, ?> server,
        final String uri)
        throws ConfigException, IOException, URISyntaxException
    {
        return disableHttps(new URIConfigBuilder())
            .connections(CONNECTIONS)
            .timeout(TIMEOUT)
            .uri(new URI(HTTP_LOCALHOST + server.port() + uri))
            .build();
    }

    public static ImmutableDnsConfig dnsConfig() {
        return DEFAULT_DNS_CONFIG;
    }

    public static ServerHttpsConfigBuilder serverHttpsConfig(
        final boolean bc)
    {
        if (bc) {
            return defaultServerHttpsBcConfig();
        } else {
            return defaultServerHttpsConfig();
        }
    }

    public static ImmutableClientHttpsConfig clientHttpsConfig(
        final boolean bc)
    {
        if (bc) {
            return defaultClientHttpsBcConfig();
        } else {
            return defaultClientHttpsConfig();
        }
    }

    public static CloseableHttpClient createDefaultClient() {
        return ClientBuilder.createClient(
            DEFAULT_HTTP_TARGET_CONFIG,
            DEFAULT_DNS_CONFIG);
    }

    public static CloseableHttpClient createDefaultClient(final boolean bc) {
        return ClientBuilder.createClient(
            targetConfig(bc),
            DEFAULT_DNS_CONFIG);
    }

    public static CloseableHttpClient createClient(final int timeout) {
        return ClientBuilder.createClient(
            targetConfig(timeout),
            DEFAULT_DNS_CONFIG);
    }

    public static void setupTvmKeys(final StaticServer tvm2) {
        tvm2.add(
            "/2/keys/?lib_version=" + Version.get(),
            TVM_KEYS.processWith(ByteArrayEntityFactory.INSTANCE));
    }
}

