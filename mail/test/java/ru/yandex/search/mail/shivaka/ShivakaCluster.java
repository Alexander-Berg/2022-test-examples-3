package ru.yandex.search.mail.shivaka;

import java.io.IOException;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.test.util.TestBase;

public class ShivakaCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String ANSWER = "OK";
    public static final String QUEUE_URI = "/queuelen";
    public static final long STAT_UPDATE_DELAY = 500000L;
    public static final String QUEUE_TEXT =
            "change_log\t1234\t1234\t2321";

    private final Shivaka shivaka;
    private final StaticServer queueServer;

    public ShivakaCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>()))
        {
            ShivakaConfigBuilder shivakaConfig = new ShivakaConfigBuilder();
            new BaseServerConfigBuilder(
                    Configs.baseConfig("Shivaka")).copyTo(shivakaConfig);
            shivakaConfig.queueConfig(Configs.targetConfig());
            shivakaConfig.searchMapConfig(new SearchMapConfigBuilder()
                    .content("change_log iNum:0,"
                    + "tag:sas1-0285_18073,"
                    + "host:sas1-0285.search.yandex.net,shards:32768-32798,"
                    + "zk:myt1-1496.search.yandex.net:18662/18663"
                    + "|myt1-1821.search.yandex.net:18662/18663"
                    + "|sas1-9317.search.yandex.net:18662/18663"
                    + "|myt1-1487.search.yandex.net:18662/18663"
                    + "|man1-7049.search.yandex.net:18662/18663"
                    + "|sas1-9184.search.yandex.net:18662/18663"
                    + "|man1-6366.search.yandex.net:18662/18663"
                    + "|man1-7970.search.yandex.net:18662/18663"
                    + "|sas1-9224.search.yandex.net:18662/18663,"
                    + "json_indexer_port:18077,"
                    + "search_port_ng:18074,"
                    + "search_port:18073"));

            shivakaConfig.queueSuffix(QUEUE_URI);
            shivakaConfig.filterHostPattern("");
            shivakaConfig.statUpdateDelay(STAT_UPDATE_DELAY);
            shivaka = new Shivaka(shivakaConfig.build());
            // цепочка освобождения ресурсов - если где то вылетит экспешн -
            // вызовет close у всех доб ресурсов
            chain.get().add(shivaka);
            // как задать таргет - в моем случае список очередей для теста?
            // предположим я хочу сделать базовый
            // хттп сервер который будет отвечать
            // на ручку queuelen каким то плейнтейстом
            // полагаю это так:
            queueServer = new StaticServer(Configs.baseConfig("Queue"));
            chain.get().add(queueServer);
            queueServer.add(QUEUE_URI, QUEUE_TEXT);
            // register - для честного хттп сервера
            //        register(
            //        new Pattern<>(QUEUE_URI, false),
            //        new StaticHttpItem(QUEUE_TEXT);
            reset(chain.release());
        }
    }

    public void start() throws IOException {
        queueServer.start();
        shivaka.start();
    }

    public StaticServer queueServer() {
        return queueServer;
    }

    public Shivaka shivaka() {
        return shivaka;
    }
}
