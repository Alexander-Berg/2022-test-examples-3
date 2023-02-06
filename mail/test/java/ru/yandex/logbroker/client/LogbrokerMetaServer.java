package ru.yandex.logbroker.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.collection.Pattern;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.config.ConfigException;

public class LogbrokerMetaServer extends AbstractLogbrokerServer {
    private final Map<String, LogbrokerDcBalancer> cluster
        = new HashMap<>();

    private final GenericAutoCloseableChain<IOException> chain;

    public LogbrokerMetaServer(
        final ImmutableBaseServerConfig config,
        final Set<String> dcList,
        final int nodesPerDc)
        throws IOException, ConfigException
    {
        super(config, "localhost");

        GenericAutoCloseableHolder<IOException,
            GenericAutoCloseableChain<IOException>> holder =
            new GenericAutoCloseableHolder<>(
                new GenericAutoCloseableChain<>());

        register(new Pattern<>("/clusters", false), new ClusterHandler());

        for (String dc: dcList) {
            LogbrokerDcBalancer balancer =
                new LogbrokerDcBalancer(
                    new BaseServerConfigBuilder(
                        Configs.baseConfig(dc + "-LogbrokerBalancer"))
                        .connections(2).port(0).build(),
                    dc,
                    nodesPerDc);

            this.cluster.put(dc, balancer);
            holder.get().add(balancer);
        }

        this.chain = holder.release();
    }

    // CSOFF: ParameterNumber
    @Override
    protected boolean handleUnsupport(
        final String hostName,
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        for (LogbrokerDcBalancer balancer: cluster.values()) {
            boolean status =
                balancer.handleUnsupport(hostName, request, response, context);
            if (status) {
                return true;
            }
        }

        return false;
    }
    // CSON: ParameterNumber

    public LogbrokerDcBalancer dc(final String dc) {
        return cluster.get(dc);
    }

    @Override
    public void start() throws IOException {
        super.start();

        for (LogbrokerDcBalancer balancer: cluster.values()) {
            balancer.start();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();

        this.chain.close();
    }

    @Override
    public void checkErrors() {
        super.checkErrors();

        for (LogbrokerDcBalancer balancer: cluster.values()) {
            balancer.checkErrors();
        }
    }

    private class ClusterHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            StringBuilderWriter result =
                new StringBuilderWriter(new StringBuilder());

            try (JsonWriter jw = new JsonWriter(result)) {
                jw.startObject();
                for (String dc: cluster.keySet()) {
                    jw.key(dc);
                    jw.startObject();
                    jw.key("balancer");
                    jw.value(dc + ".localhost");
                    jw.endObject();
                }
                jw.endObject();
            }

            response.setEntity(new StringEntity(result.toString()));
        }
    }
}
