package ru.yandex.xavier;

import java.io.File;
import java.io.IOException;

import java.net.URI;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.util.Collections;

import org.apache.http.client.utils.URIBuilder;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;

import ru.yandex.parser.config.IniConfig;

import ru.yandex.parser.string.URIParser;

import ru.yandex.search.mail.xavier.Xavier;
import ru.yandex.search.mail.xavier.config.XavierConfigBuilder;

public class XavierCluster implements GenericAutoCloseable<IOException> {
    public static final String XIVA_LIST_TOKEN =
        "bcd8d4d9b4acd4e56555b4c530f42f9b27805632";
    public static final String XIVA_NOTIDY_TOKEN =
        "abc8d4d9b4acd4e56555b4c530f42f9b27805632";
    private static final String XAVIER_CONFIG =
        "src/xavier/main/bundle/xavier.conf";

    private static final String PORT = "port";
    private static final String HOST = "host";
    private static final String URI = "uri";

    private final GenericAutoCloseableChain<IOException> chain;

    private final StaticServer producer;
    private final StaticServer proxy;
    private final StaticServer filterSearch;
    private final StaticServer xiva;
    private final Xavier xavier;

    public XavierCluster() throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            xiva = new StaticServer(Configs.baseConfig("Xiva"));
            chain.get().add(xiva);

            filterSearch =
                new StaticServer(Configs.baseConfig("FilterSearch"));
            chain.get().add(filterSearch);
            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);

            proxy = new StaticServer(Configs.baseConfig("Proxy"));
            chain.get().add(proxy);

            System.setProperty("XIVA_NOTIFY_TOKEN", XIVA_NOTIDY_TOKEN);
            System.setProperty("XIVA_LIST_TOKEN", XIVA_LIST_TOKEN);
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("BSCONFIG_INAME", "localhost:0");
            System.setProperty("BSCONFIG_IHOST", "localhost");
            System.setProperty("BSCONFIG_IDIR", "./");

            IniConfig iniConfig =
                patchConfig(
                    new IniConfig(new File(XAVIER_CONFIG)));

            this.xavier =
                new Xavier(new XavierConfigBuilder(iniConfig).build());

            chain.get().add(xavier);

            this.chain = chain.release();
        }
    }

    public void start() throws IOException {
        filterSearch.start();
        producer.start();
        proxy.start();
        xiva.start();
        xavier.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer proxy() {
        return proxy;
    }

    public StaticServer filterSearch() {
        return filterSearch;
    }

    public StaticServer xiva() {
        return xiva;
    }

    public Xavier xavier() {
        return xavier;
    }

    protected IniConfig patchConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("accesslog");
        config.sections().remove("log");
        config.sections().remove("stdout");
        config.sections().remove("stderr");

        IniConfig fs = config.section("filter-search");

        URI uri = fs.get(URI, URIParser.INSTANCE);

        String newUri =
            new URIBuilder(uri)
                .setHost(filterSearch.host().getHostName())
                .setPort(filterSearch.host().getPort()).build().toString();

        fs.put(URI, newUri);

        config.section("proxy").put(HOST, proxy.host().toHostString());
        config.section("xiva").put(HOST, xiva.host().toHostString());
        config.section("producer").put(HOST, producer.host().toHostString());

        File searchMapFile =
            File.createTempFile("xavier_searchmap_", ".test.searchmap");
        searchMapFile.deleteOnExit();
        fillSearchMap(searchMapFile);
        config.section("searchmap")
            .put("file", searchMapFile.getAbsolutePath());

        return config;
    }

    public String searchMapRule(final String db) {
        return db + " shards:0-65533,host:localhost,search_port:10500"
            + ",search_port_ng:10501,json_indexer_port:10502\n";
    }

    private void fillSearchMap(final File out) throws Exception {
        Files.write(
            out.toPath(),
            Collections.singleton(
                searchMapRule("change_log")
                    + searchMapRule("mdb100")
                    + searchMapRule("mdb200")
                    + searchMapRule("pg")),
            StandardOpenOption.WRITE);
    }
}
