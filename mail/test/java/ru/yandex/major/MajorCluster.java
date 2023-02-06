package ru.yandex.major;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Arrays;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;

import ru.yandex.major.config.MajorConfigBuilder;
import ru.yandex.parser.config.IniConfig;

public class MajorCluster implements GenericAutoCloseable<IOException> {
    public static final String MAIN_MAJOR = "major1.test.net";
    public static final String MAIN_MAJOR_BACKUP = "major1-backup.test.net";
    public static final String SECOND_MAJOR = "major2.test.net";
    private static final String RANGE_1 = "0-32000";
    private static final String RANGE_2 = "32001-65534";

    private static final String MAJOR_CONFIG =
        "src/major/main/bundle/major.conf";

    private final Major firstMajor;
    private final StaticServer firstMajorBackup;
    private final StaticServer secondMajor;
    private final StaticServer producer;
    private final StaticServer proxy;

    private final GenericAutoCloseableChain<IOException> chain;

    public MajorCluster() throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            firstMajorBackup = new StaticServer(Configs.baseConfig());
            chain.get().add(firstMajorBackup);
            firstMajorBackup.start();
            secondMajor = new StaticServer(Configs.baseConfig());
            chain.get().add(secondMajor);
            secondMajor.start();
            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);
            producer.start();
            proxy = new StaticServer(Configs.baseConfig());
            chain.get().add(proxy);
            proxy.start();

            System.setProperty("BSCONFIG_IDIR", "src/major/main/bundle");
            System.setProperty("BSCONFIG_INAME", "major");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("HOSTNAME", MAIN_MAJOR);
            MajorConfigBuilder config =
                new MajorConfigBuilder(
                    patchConfig(new IniConfig(new File(MAJOR_CONFIG))));
            config.searchMapConfig().content(
                String.join(
                    "\n",
                    Arrays.asList(
                        searchMapLine(
                            MAIN_MAJOR,
                            String.valueOf(firstMajorBackup.port()),
                            RANGE_1),
                        searchMapLine(
                            MAIN_MAJOR_BACKUP,
                            String.valueOf(firstMajorBackup.port()),
                            RANGE_1),
                        searchMapLine(
                            SECOND_MAJOR,
                            String.valueOf(secondMajor.port()),
                            RANGE_2))));

            config.proxyConfig(Configs.hostConfig(proxy));
            config.producerConfig(Configs.hostConfig(producer));

            setupDns(MAIN_MAJOR_BACKUP);
            setupDns(SECOND_MAJOR);
            setupDns(MAIN_MAJOR);

            firstMajor = new Major(config.build());
            chain.get().add(firstMajor);
            firstMajor.start();
            this.chain = chain.release();
        }
    }

    public Major firstMajor() {
        return firstMajor;
    }

    public StaticServer backupHead() {
        return firstMajorBackup;
    }

    public StaticServer secondMajor() {
        return secondMajor;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer proxy() {
        return proxy;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    protected String searchMapLine(
        final String host,
        final String port,
        final String shards)
    {
        return "major host:" + host
            + ",search_port:" + port
            + ",search_port_ng:" + port
            + ",json_indexer_port:" + port
            + ",shards:" + shards;
    }

    protected IniConfig patchConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("accesslog");
        config.sections().remove("log");
        config.sections().remove("stdout");
        config.sections().remove("stderr");
        config.sections().remove("storage");
        config.sections()
            .get("server").sections()
            .remove("free-space-signals");
        return config;
    }

    protected void setupDns(final String name) throws Exception {
        Field addressCache =
            InetAddress.class.getDeclaredField("addressCache");

        InetAddress[] localhost = InetAddress.getAllByName("localhost");

        addressCache.setAccessible(true);
        Object cache = addressCache.get(null);
        Method putMethod = cache.getClass()
            .getMethod("put", String.class, InetAddress[].class);

        putMethod.setAccessible(true);
        putMethod.invoke(cache, name, localhost);
    }
}
