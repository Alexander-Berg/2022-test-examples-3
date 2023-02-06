package ru.yandex.logbroker.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Collection;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.logbroker.server.LogbrokerConsumerServer;

public class LogbrokerConsumerCluster
    implements GenericAutoCloseable<IOException>
{
    public static final String SAS = "sas";
    public static final String IVA = "iva";
    public static final String MAN = "man";

    private final LogbrokerMetaServer metaServer;
    private final StaticServer producer;
    private final GenericAutoCloseableHolder<IOException,
        GenericAutoCloseableChain<IOException>> holder;

    private GenericAutoCloseableChain<IOException> chain;
    private LogbrokerConsumerServer consumerServer;

    public LogbrokerConsumerCluster(
        final LogbrokerMetaServer metaServer,
        final LogbrokerConsumerServer consumerServer)
        throws Exception
    {
        this.holder =
            new GenericAutoCloseableHolder<>(
                new GenericAutoCloseableChain<>());

        producer = new StaticServer(Configs.baseConfig("Producer"));
        holder.get().add(producer);

        this.metaServer = metaServer;
        holder.get().add(metaServer);

        this.consumerServer = consumerServer;
        this.holder.get().add(consumerServer);

        this.chain = holder.release();
    }

    public LogbrokerMetaServer emulator() {
        return metaServer;
    }

    public LogbrokerConsumerServer server() {
        return consumerServer;
    }

    public StaticServer producer() {
        return producer;
    }

    public void start() throws Exception {
        producer.start();
        metaServer.start();
        consumerServer.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public static void setUpDns(
        final Collection<String> dcs,
        final int nodes)
        throws Exception
    {
        Field addressCache =
            InetAddress.class.getDeclaredField("addressCache");

        InetAddress[] localhost = InetAddress.getAllByName("localhost");

        addressCache.setAccessible(true);
        Object cache = addressCache.get(null);
        Method putMethod = cache.getClass()
            .getMethod("put", String.class, InetAddress[].class);

        putMethod.setAccessible(true);

        for (String dc: dcs) {
            final String postfix = ".localhost";
            putMethod.invoke(cache, dc + postfix, localhost);
            if (nodes == 0) {
                putMethod.invoke(cache, dc + postfix, localhost);
            } else {
                for (int i = 0; i < nodes; i++) {
                    putMethod.invoke(cache, dc + '-' + i + postfix, localhost);
                }
            }
        }
    }
}

