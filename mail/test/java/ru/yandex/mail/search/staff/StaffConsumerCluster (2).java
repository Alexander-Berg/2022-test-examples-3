package ru.yandex.mail.search.staff;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIBuilder;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;

import ru.yandex.mail.search.staff.config.StaffConsumerConfigBuilder;

import ru.yandex.mail.search.staff.consumer.StaffConsumerServer;

import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.string.URIParser;
import ru.yandex.test.lucene.TestLucene;
import ru.yandex.test.util.TestBase;

public class StaffConsumerCluster implements GenericAutoCloseable<IOException> {
    private static final String STAFF_CONSUMER_CONFIG =
        "src/staff-consumer/main/bundle/staff-consumer.conf";
    private static final File LUCENE_CONFIG =
        new File("rpm/perseus-team/src/lucene.conf_perseus-team");

    private static final String TOKEN = "supertoken";
    private static final String STAFF_SECTION = "staff";
    private static final String URI_SECTION = "uri";
    private static final String PORT = "0";

    private final StaticServer producer;
    private final StaticServer staff;
    private final StaffConsumerServer consumer;
    private final TestLucene lucene;

    private final GenericAutoCloseableChain<IOException> chain;

    public StaffConsumerCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            staff = new StaticServer(Configs.baseConfig("Staff"));
            chain.get().add(staff);
            staff.start();

            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);
            producer.start();

            System.setProperty("STAFF_TOKEN", TOKEN);
            System.setProperty("BSCONFIG_IPORT", PORT);
            System.setProperty("BSCONFIG_INAME", "localhost:0");
            System.setProperty("BSCONFIG_IHOST", "localhost");

            IniConfig config =
                patchConfig(
                    new IniConfig(new File(STAFF_CONSUMER_CONFIG)),
                    staff.host(),
                    producer.host());

            consumer =
                new StaffConsumerServer(
                    new StaffConsumerConfigBuilder(config).build());
            chain.get().add(consumer);

            lucene = new TestLucene(testBase, LUCENE_CONFIG);
            chain.get().add(lucene);

            this.chain = chain.release();
        }
    }

    public void start() throws IOException {
        consumer.start();
    }

    public String token() {
        return TOKEN;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer staff() {
        return staff;
    }

    public StaffConsumerServer consumer() {
        return consumer;
    }

    public TestLucene lucene() {
        return lucene;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    private static IniConfig patchConfig(
        final IniConfig config,
        final HttpHost staff,
        final HttpHost producer)
        throws Exception
    {
        config.sections().remove("accesslog");
        config.sections().remove("log");
        config.sections().remove("stdout");
        config.sections().remove("stderr");
        config.section("server").put("port", PORT);

        URI uri =
            config.section(STAFF_SECTION).get(URI_SECTION, URIParser.INSTANCE);

        String newUri =
            new URIBuilder(uri)
                .setHost(staff.getHostName())
                .setPort(staff.getPort()).build().toString();

        config.section(STAFF_SECTION).put(URI_SECTION, newUri);

        config.section("producer").put("host", producer.toHostString());
        config.put("check-interval", "500");

        return config;
    }
}
