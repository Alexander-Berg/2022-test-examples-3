package ru.yandex.search.mail.yt.consumer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.search.mail.yt.consumer.cypress.CypressNode;
import ru.yandex.search.mail.yt.consumer.cypress.NodeType;
import ru.yandex.search.mail.yt.consumer.yt.YtPath;
import ru.yandex.tskv.BasicTskvParser;
import ru.yandex.tskv.TskvException;
import ru.yandex.tskv.TskvHandler;
import ru.yandex.tskv.TskvRecord;

public class YtCypressCluster
    extends BaseHttpServer<ImmutableBaseServerConfig>
{
    private static final String PATH = "path";
    private static final String RECURSIVE = "recursive";
    private static final String TRANSACTION = "transaction_id";
    private static final String STATE = "state";
    private static final String ATTRIBUTES = "attributes";
    private static final String OK_STATUS = "{\"status\":\"ok\"}";
    private static final String APPEND_PREFIX = "<append=true>";

    private static final String API = "/api/v3/";
    private static final int MERGE_OP_DELAY = 100;

    private final CypressNode root = CypressNode.createRoot();
    private final ExecutorService executor;

    private volatile Set<String> transactions = new HashSet<>();

    public YtCypressCluster(
        final ImmutableBaseServerConfig config)
        throws IOException
    {
        super(config);

        this.executor = Executors.newSingleThreadExecutor();

        this.register(
            new Pattern<>(API + "create", false),
            new CreateHandler(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>(API + "move", false),
            new MoveHandler(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>(API + "remove", false),
            new RemoveHandler(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>(API + "merge", false),
            new MergeHandler(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>(API + "get", false),
            new GetHandler(),
            RequestHandlerMapper.GET);
        this.register(
            new Pattern<>(API + "list", false),
            new ListHandler(),
            RequestHandlerMapper.GET);
        this.register(
            new Pattern<>(API + "exists", false),
            new ExistsHandler(),
            RequestHandlerMapper.GET);
        this.register(
            new Pattern<>(API + "read_table", false),
            new ReadHandler(),
            RequestHandlerMapper.GET);
        this.register(
            new Pattern<>(API + "write_table", false),
            new WriteHandler(),
            RequestHandlerMapper.PUT);
        this.register(
            new Pattern<>(API + "start_tx", false),
            new StartTx(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>(API + "abort_tx", false),
            new AbortTx(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>(API + "commit_tx", false),
            new CommitTx(),
            RequestHandlerMapper.POST);
        this.register(
            new Pattern<>("/hosts", false),
            new HostsHandler(),
            RequestHandlerMapper.GET);
    }

    public CypressNode root() {
        return root;
    }

    public synchronized CypressNode create(
        final String path,
        final NodeType type)
    {
        CypressNode current = getClosest(path);
        List<String> toCreate = missingNodes(current.path(), path);
        for (int i = 0; i < toCreate.size(); i++) {
            CypressNode node;
            if (i == toCreate.size() - 1) {
                node =
                    new CypressNode(
                        current,
                        type,
                        toCreate.get(i));
            } else {
                node =
                    new CypressNode(
                        current,
                        NodeType.MAP_NODE,
                        toCreate.get(i));
            }

            current = node;
        }

        return current;
    }

    public synchronized CypressNode getNode(final String path) {
        return getClosest(path);
    }

    private static void assertAuth(
        final HttpRequest request)
        throws HttpException
    {
        Header auth = request.getFirstHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null
            || auth.getValue() == null
            || auth.getValue().isEmpty())
        {
            throw new BadRequestException(
                "Missing " + HttpHeaders.AUTHORIZATION + " header");
        }
    }

    private static List<String> resolve(final String path) {
        System.out.println("Resolving " + path);
        List<String> result = new ArrayList<>();

        if (path.startsWith(CypressNode.ROOT)) {
            String[] parts =
                path.replaceFirst(CypressNode.ROOT, "")
                    .split(CypressNode.SEPARATOR);
            result.add(CypressNode.ROOT);
            result.addAll(Arrays.asList(parts));
        } else {
            result.addAll(Arrays.asList(path.split(CypressNode.SEPARATOR)));
        }

        return result;
    }

    private synchronized CypressNode getClosest(final String path) {
        List<String> parts = resolve(path);
        CypressNode current = root;
        // start from 1, skip root node
        for (int i = 1; i < parts.size(); i++) {
            CypressNode node = current.children().get(parts.get(i));
            if (node == null) {
                return current;
            }

            current = node;
        }

        return current;
    }

    private synchronized CypressNode get(final String path) {
        CypressNode closest = getClosest(path);
        if (path.equals(closest.path())) {
            return closest;
        }

        return null;
    }

    public synchronized void merge(final List<String> src, final String dst) {
        CypressNode dstNode = get(dst);
        if (dstNode == null) {
            dstNode = create(dst, NodeType.TABLE);
        }

        for (String path: src) {
            YtPath ytPath = YtPath.fromString(path);
            CypressNode node = get(ytPath.path());
            for (
                int i = ytPath.start();
                i < Math.min(node.data().size(),
                    ytPath.end()); i++)
            {
                dstNode.data().add(node.data().get(i));
            }
        }
    }

    private final class MergeTask implements Runnable {
        private final List<String> src;
        private final String dst;
        private final CypressNode opStat;

        private MergeTask(
            final String opId,
            final List<String> src,
            final String dst)
        {
            this.src = src;
            this.dst = dst;
            this.opStat =
                create(
                    YtClient.OPERATIONS_CHECK_PATH + opId,
                    NodeType.TABLE);
        }

        @Override
        public void run() {
            try {
                opStat.set(
                    STATE,
                    YtClient.OperationStatus.INITIALIZING.toString()
                        .toLowerCase(Locale.ROOT));

                opStat.set(
                    STATE,
                    YtClient.OperationStatus.PENDING.toString()
                        .toLowerCase(Locale.ROOT));

                Thread.sleep(MERGE_OP_DELAY);
                opStat.set(
                    STATE,
                    YtClient.OperationStatus.PREPARING.toString()
                        .toLowerCase(Locale.ROOT));

                opStat.set(
                    STATE,
                    YtClient.OperationStatus.RUNNING.toString()
                        .toLowerCase(Locale.ROOT));

                CypressNode node = get(dst);
                if (node == null || node.type() != NodeType.TABLE) {
                    opStat.set(
                        STATE,
                        YtClient.OperationStatus.FAILED.toString()
                            .toLowerCase(Locale.ROOT));
                    logger().warning("Missing destinatio");
                    return;
                }

                merge(src, dst);
                Thread.sleep(MERGE_OP_DELAY);

                opStat.set(
                    STATE,
                    YtClient.OperationStatus.COMPLETING.toString()
                        .toLowerCase(Locale.ROOT));

                Thread.sleep(MERGE_OP_DELAY);
                opStat.set(
                    STATE,
                    YtClient.OperationStatus.COMPLETED.toString()
                        .toLowerCase(Locale.ROOT));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<String> missingNodes(final String root, final String child) {
        int index = child.indexOf(root);
        assert index >= 0;

        String sub = child.substring(index + root.length());
        if (sub.startsWith(CypressNode.SEPARATOR)) {
            sub = sub.substring(CypressNode.SEPARATOR.length());
        }

        if (sub.isEmpty()) {
            return Collections.emptyList();
        }

        return resolve(sub);
    }

    private final class CreateHandler implements HttpRequestHandler {
        private CreateHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);
            CgiParams params = new CgiParams(request);
            String path = params.getString(PATH);
            NodeType type = params.getEnum(NodeType.class, "type");
            boolean recursive = params.getBoolean(RECURSIVE);
            CypressNode closest;
            synchronized (this) {
                closest = getClosest(path);
                List<String> diff = missingNodes(closest.path(), path);
                if (diff.size() == 0) {
                    throw new BadRequestException(
                        "Path already exists " + path);
                }

                if (diff.size() > 1 && !recursive) {
                    throw new BadRequestException(
                        "Parent node does not exists for " + path
                            + " closest is " + closest.path());
                }

                if (closest.type() != NodeType.MAP_NODE) {
                    System.err.println(root.toString());
                    System.err.println(diff);
                    throw new BadRequestException(
                        "Parent is not a map_node " + closest.path());
                }

                if (diff.size() > 1) {
                    for (int i = 0; i < diff.size() - 1; i++) {
                        closest =
                            new CypressNode(
                                closest,
                                NodeType.MAP_NODE,
                                diff.get(i));
                    }
                }

                closest =
                    new CypressNode(closest, type, diff.get(diff.size() - 1));
            }

            logger().info("Creating node " + closest.path());

            System.err.println(root.toString());
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity(OK_STATUS));
        }
    }

    private final class MoveHandler implements HttpRequestHandler {
        private MoveHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);
            CgiParams params = new CgiParams(request);

            String sourcePath = params.getString("source_path");
            String destPath = params.getString("destination_path");

            CypressNode source = get(sourcePath);
            if (source == null) {
                throw new BadRequestException(
                    "Source path does not exists " + sourcePath
                        + '\n' + root.toString());
            }

            CypressNode dest = get(destPath);
            if (dest != null) {
                throw new BadRequestException(
                    "Already exists " + dest.toString());
            }

            dest = getClosest(destPath);
            List<String> diff = missingNodes(dest.path(), destPath);
            if (diff.size() > 1) {
                throw new BadRequestException(
                    "No parent node exists for " + destPath);
            }

            synchronized (root) {
                source.parent().remove(source.name());
                dest.add(new CypressNode(dest, source, diff.get(0)));
            }
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity(OK_STATUS));
        }
    }

    private final class MergeHandler implements HttpRequestHandler {
        private MergeHandler() {
        }

        @Override
        @SuppressWarnings("FutureReturnValueIgnored")
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            List<String> src = new ArrayList<>();
            String dst;

            try {
                JsonObject reqObj =
                    TypesafeValueContentHandler.parse(
                        CharsetUtils.toString(
                            ((HttpEntityEnclosingRequest) request)
                                .getEntity()));

                JsonMap spec = reqObj.get("spec").asMap();
                for (JsonObject s: spec.get("input_table_paths").asList()) {
                    src.add(s.asString());
                }

                dst = spec.get("output_table_path").asString();
            } catch (JsonException je) {
                throw new BadRequestException(je);
            }

            String opId = (String) context.getAttribute(HttpServer.SESSION_ID);
            executor.submit(new MergeTask(opId, src, dst));

            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity(OK_STATUS));
        }
    }

    public synchronized boolean exists(final String path) {
        CypressNode node = get(path);
        return node != null;
    }

    private final class ExistsHandler implements HttpRequestHandler {
        private ExistsHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);
            CgiParams params = new CgiParams(request);
            String path = params.getString(PATH);

            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity(String.valueOf(exists(path))));
        }
    }

    public synchronized void remove(
        final String path)
        throws BadRequestException
    {
        CypressNode node = get(path);
        if (node == null) {
            throw new BadRequestException("Path not exists " + path);
        }

        if (node.children().size() != 0) {
            throw new BadRequestException("Path is not leaf " + path);
        }

        node.parent().remove(node.name());
    }

    private final class RemoveHandler implements HttpRequestHandler {
        private RemoveHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);
            CgiParams params = new CgiParams(request);

            remove(params.getString(PATH));
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    private final class ListHandler implements HttpRequestHandler {
        private ListHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);
            CgiParams params = new CgiParams(request);
            String path = params.getString(PATH);
            CypressNode node = get(path);
            if (node == null || node.type() != NodeType.MAP_NODE) {
                System.err.println(root.toString());
                throw new BadRequestException(
                    "Node not exists or not a map_node " + path);
            }

            StringBuilderWriter sb = new StringBuilderWriter();
            try (JsonWriter writer = JsonType.HUMAN_READABLE.create(sb)) {
                writer.startArray();
                for (String child: node.children().keySet()) {
                    writer.value(child);
                }
                writer.endArray();
            }

            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(
                new StringEntity(sb.toString(), ContentType.APPLICATION_JSON));
        }
    }

    private final class ReadHandler implements HttpRequestHandler {
        private ReadHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            CgiParams params = new CgiParams(request);
            String path = params.getString(PATH);

            int leftRange = 0;
            int rightRange = Integer.MAX_VALUE;

            YtPath range =
                YtPath.fromHeader(
                    request.getFirstHeader(YtClient.YT_PARAMS_HEADER));
            if (range == null) {
                range = YtPath.fromString(path);
            }

            boolean printIndex =
                params.getBoolean(
                    "control_attributes[enable_row_index]",
                    false);

            //parse range
            if (range.end() < Integer.MAX_VALUE) {
                leftRange = range.start();
                rightRange = range.end();
            }

            CypressNode node = get(range.path());
            if (node == null || node.type() != NodeType.TABLE) {
                throw new BadRequestException("Node not exists or not table");
            }

            rightRange = Math.min(node.data().size(), rightRange);

            StringBuilder sb = new StringBuilder();
            for (int i = leftRange; i < rightRange; i++) {
                if (i != leftRange) {
                    sb.append('\n');
                }

                if (printIndex) {
                    sb.append("{\"$attributes\":{\"row_index\":");
                    sb.append(i);
                    sb.append("},\"$value\":null}");
                    sb.append("\n");
                }

                sb.append(node.data().get(i));
            }

            logger().fine("Reading " + path + " offset "
                + leftRange + " length " + rightRange + '\n' + sb.toString());

            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(
                new StringEntity(
                    sb.toString(),
                    ContentType.create(
                        YtClient.TAB_SEPARATED_VALUES,
                        Charset.forName("utf-8"))));
        }
    }

    private final class GetHandler implements HttpRequestHandler {
        private GetHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            CgiParams params = new CgiParams(request);
            String path = params.getString(PATH);

            CypressNode node = get(path);
            if (node == null || node.type() != NodeType.TABLE) {
                throw new BadRequestException("Path not exist or not table");
            }

            Header ytParameters =
                request.getFirstHeader(YtClient.YT_PARAMS_HEADER);

            List<String> attrs;
            try {
                JsonObject paramObj =
                    TypesafeValueContentHandler.parse(ytParameters.getValue());
                JsonList attrList = paramObj.get(ATTRIBUTES).asList();
                attrs = new ArrayList<>(attrList.size());
                for (JsonObject attrObj: attrList) {
                    attrs.add(attrObj.asString());
                }
            } catch (JsonException je) {
                throw new BadRequestException(je);
            }

            StringBuilderWriter sbw = new StringBuilderWriter();
            try (JsonWriter jw = JsonType.HUMAN_READABLE.create(sbw)) {
                jw.startObject();
                jw.key('$' + ATTRIBUTES);
                jw.startObject();
                for (String attr: attrs) {
                    jw.key(attr);
                    jw.value(node.get(attr));
                }
                jw.endObject();
                jw.endObject();
            }

            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(
                new StringEntity(
                    sbw.toString(),
                    ContentType.APPLICATION_JSON));
        }
    }

    private final class WriteHandler implements HttpRequestHandler {
        private WriteHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            CgiParams params = new CgiParams(request);
            String path = params.getString(PATH);

            boolean append = false;
            if (path.startsWith(APPEND_PREFIX)) {
                path = path.substring(APPEND_PREFIX.length());
                append = true;
            }

            CypressNode node = get(path);
            if (node == null || node.type() != NodeType.TABLE) {
                throw new BadRequestException("Path not exists or not table");
            }

            HttpEntity entity =
                ((HttpEntityEnclosingRequest) request).getEntity();

            YtTskvHandler handler = new YtTskvHandler();
            new BasicTskvParser(handler).parse(CharsetUtils.content(entity));

            if (handler.exception() != null) {
                throw new BadRequestException(handler.exception());
            }

            if (!append) {
                node.write(handler.data());
            } else {
                List<String> data = new ArrayList<>(node.data());
                data.addAll(handler.data());
                node.write(data);
            }

            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    private final class StartTx implements HttpRequestHandler {
        private StartTx() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            String transaction =
                (String) context.getAttribute(HttpServer.SESSION_ID);
            synchronized (this) {
                transactions.add(transaction);
            }

            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity('\"' + transaction + '\"'));
        }
    }

    private final class CommitTx implements HttpRequestHandler {
        private CommitTx() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            CgiParams params = new CgiParams(request);
            String paramTransaction = params.getString(TRANSACTION);
            synchronized (this) {
                if (!transactions.remove(paramTransaction)) {
                    throw new BadRequestException(
                        "Transaction already gone " + paramTransaction);
                }
            }

            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    private final class AbortTx implements HttpRequestHandler {
        private AbortTx() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            assertAuth(request);

            CgiParams params = new CgiParams(request);
            String paramTransaction = params.getString(TRANSACTION);
            synchronized (this) {
                if (!transactions.remove(paramTransaction)) {
                    throw new BadRequestException(
                        "No such transaction " + paramTransaction);
                }
            }

            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    private final class HostsHandler implements HttpRequestHandler {
        private HostsHandler() {
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(
                new StringEntity("[\"" + host().toHostString() + "\"]"));
        }
    }

    private static final class YtTskvHandler
        implements TskvHandler<TskvRecord>
    {
        private final List<String> data;
        private Exception exception;

        private YtTskvHandler() {
            this.data = new ArrayList<>();
        }

        @Override
        public boolean onRecord(final TskvRecord record) {
            this.data.add(record.toString());

            if (exception != null) {
                return false;
            }

            return true;
        }

        @Override
        public boolean onError(final TskvException exc) {
            exception = exc;
            return false;
        }

        public List<String> data() {
            return data;
        }

        public Exception exception() {
            return exception;
        }
    }
}
