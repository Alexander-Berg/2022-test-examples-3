package ru.yandex.logbroker.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.logbroker.client.LogbrokerNodeServer.TestPartition;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.uri.CgiParams;

public class LogbrokerDcBalancer extends AbstractLogbrokerServer {
    private static final String IDENT_SEP = "--";
    private static final String LOCALHOST = ".localhost";
    private static final int ODD_MLT = 31;

    private static final int NODE_MAX_CONNECTIONS = 20;
    private static final int NODE_TIMEOUT = 2000;

    private final String dc;
    private final Set<String> topics;
    private final Map<PartitionMeta, LogbrokerNodeServer> nodes;
    private final List<LogbrokerNodeServer> definedNodes;
    private final GenericAutoCloseableChain<IOException> chain;

    public LogbrokerDcBalancer(
        final ImmutableBaseServerConfig config,
        final String dc,
        final int nodes)
        throws IOException, ConfigException
    {
        super(config, dc + LOCALHOST);
        this.dc = dc;

        GenericAutoCloseableHolder<IOException,
            GenericAutoCloseableChain<IOException>> holder =
            new GenericAutoCloseableHolder<>(
                new GenericAutoCloseableChain<>());

        register(new Pattern<>("/pull/suggest", true), new SuggestHandler());
        register(new Pattern<>("/pull/offsets", true), new OffsetsHandler());
        register(new Pattern<>("/pull/list", true), new ListHandler());

        this.nodes = new HashMap<>();
        this.topics = new HashSet<>();

        this.definedNodes = new ArrayList<>();

        BaseServerConfigBuilder nodeConfig =
            new BaseServerConfigBuilder(
                Configs.baseConfig(dc + "-LogbrokerNode"));
        nodeConfig.connections(NODE_MAX_CONNECTIONS);
        nodeConfig.timeout(NODE_TIMEOUT);
        nodeConfig.port(0);

        for (int i = 0; i <= nodes; i++) {
            LogbrokerNodeServer nodeServer =
                new LogbrokerNodeServer(
                    new NodeName(dc, dc + '-' + i + LOCALHOST),
                    nodeConfig.build());

            holder.get().add(nodeServer);
            this.definedNodes.add(nodeServer);
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
        if (hostName().equalsIgnoreCase(hostName)) {
            this.handlerMapper.lookup(request)
                .handle(request, response, context);
            return true;
        }

        boolean status = false;
        for (LogbrokerNodeServer node: definedNodes) {
            if (node.handleUnsupport(hostName, request, response, context)) {
                status = true;
                break;
            }
        }

        return status;
    }
    // CSON: ParameterNumber

    @Override
    public void start() throws IOException {
        super.start();

        for (LogbrokerNodeServer server: definedNodes) {
            server.start();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();

        this.chain.close();
    }

    public String dc() {
        return dc;
    }

    public synchronized TopicMeta addTopic(
        final String ident,
        final String logType,
        final int partitions)
        throws IOException
    {
        TopicMeta meta = new TopicMeta(ident, logType, partitions);
        topics.add(meta.toString());
        for (int i = 0; i < partitions; i++) {
            PartitionMeta p = new PartitionMeta(meta, i);
            LogbrokerNodeServer node = this.definedNodes.get(i);
            node.addPartition(p);
            nodes.put(p, node);
        }

        return meta;
    }

    public synchronized TestPartition partition(
        final TopicMeta topic,
        final int id)
    {
        PartitionMeta meta = new PartitionMeta(topic, id);
        return nodes.get(meta).partition(meta.toString());
    }

    private class SuggestHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            CgiParams params = new CgiParams(request);
            checkRequest(params);

            String topicStr = params.getString(LogbrokerClient.TOPIC);
            if (!topics.contains(topicStr)) {
                throw new BadRequestException("Invalid topic supplied");
            }

            int count = params.getInt("count", 1);
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Map.Entry<PartitionMeta, LogbrokerNodeServer> entry
                : nodes.entrySet())
            {
                if (i >= count) {
                    break;
                }

                PartitionMeta meta = entry.getKey();
                LogbrokerNodeServer node = entry.getValue();

                if (!meta.topic().toString().equalsIgnoreCase(topicStr)) {
                    continue;
                }

                if (node.partition(meta.toString()).locked()) {
                    continue;
                }

                sb.append(node.hostName());
                sb.append(':');
                sb.append(node.port());
                sb.append('\t');
                sb.append(meta.toString() + '\n');
                i++;
            }

            String result = sb.toString();
            logger().info("Suggesting " + result);
            response.setEntity(new StringEntity(result));
        }
    }

    private class ListHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            StringBuilder result = new StringBuilder();
            for (String topic: topics) {
                result.append(topic);
                result.append('\n');
            }

            if (result.length() > 0) {
                result.setLength(result.length() - 1);
            }

            response.setEntity(new StringEntity(result.toString()));
        }
    }

    private class OffsetsHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            CgiParams params = new CgiParams(request);
            checkRequest(params);

            String topicStr = params.getString(LogbrokerClient.TOPIC);
            Map<PartitionMeta, LogbrokerNodeServer> partitions =
                new LinkedHashMap<>();

            for (PartitionMeta partition: nodes.keySet()) {
                if (partition.topic.toString().equalsIgnoreCase(topicStr)) {
                    partitions.put(partition, nodes.get(partition));
                }
            }
            if (partitions.isEmpty()) {
                throw new BadRequestException("Bad topic supplied");
            }

            String client = params.getString(AbstractLogbrokerClient.CLIENT);

            StringBuilder fields = new StringBuilder();
            fields.append("topic:partition\t");
            fields.append("offset\t");
            fields.append("logStart\t");
            fields.append("logSize\t");
            fields.append("lag\t");
            fields.append("owner");

            response.addHeader("Fields", fields.toString());
            StringBuilder output = new StringBuilder();

            String tab = LogbrokerClientResponseParser.TAB;

            partitions.forEach((k, v) -> {
                String pId = k.toString();
                output.append(pId);
                TestPartition p = v.partition(pId);
                output.append(tab).append(p.offset(client));
                output.append(tab).append(0);
                output.append(tab).append(p.data().size());
                output.append(tab).append(p.data().size());
                output.append(tab);
                if (p.locked()) {
                    output.append(p.lockSession());
                } else {
                    output.append("none");
                }
                output.append("\n");
            });

            System.out.println("Offsets");
            System.out.println(output.toString());
            response.setEntity(new StringEntity(output.toString()));
        }
    }

    public final class TopicMeta {
        private final String ident;
        private final String logType;
        private final int partitions;

        public TopicMeta(
            final String ident,
            final String logType,
            final int partitions)
        {
            this.ident = ident;
            this.logType = logType;
            this.partitions = partitions;
        }

        public String ident() {
            return ident;
        }

        public String logType() {
            return logType;
        }

        public int partitions() {
            return partitions;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TopicMeta topic = (TopicMeta) o;

            if (!ident.equals(topic.ident)) {
                return false;
            }
            return logType.equals(topic.logType);
        }

        @Override
        public int hashCode() {
            int result = ident.hashCode();
            result = ODD_MLT * result + logType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "rt3." + dc() + IDENT_SEP + ident + IDENT_SEP + logType;
        }
    }

    public static final class PartitionMeta {
        private final TopicMeta topic;
        private final int id;

        public PartitionMeta(final TopicMeta topic, final int id) {
            this.topic = topic;
            this.id = id;
        }

        public TopicMeta topic() {
            return topic;
        }

        public long id() {
            return id;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PartitionMeta that = (PartitionMeta) o;
            if (id != that.id) {
                return false;
            }
            return topic.equals(that.topic);
        }

        @Override
        public int hashCode() {
            int result = topic.hashCode();
            result = ODD_MLT * result + id;
            return result;
        }

        @Override
        public String toString() {
            return topic.toString() + ':' + id;
        }
    }
}
