package ru.yandex.logbroker.client;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.LoggingHttpServerConnection;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ServerException;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.logbroker.client.LogbrokerDcBalancer.PartitionMeta;
import ru.yandex.logger.IdGenerator;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.util.string.StringUtils;

public class LogbrokerNodeServer extends AbstractLogbrokerServer {
    private static final String CONNECTION_ATTR = "http.connection";
    private static final String COLON = ":";
    private static final String NEW_LINE = "\n";

    private final IdGenerator generator = new IdGenerator();
    private final Map<String, TestPartition> partitions = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final NodeName nodeName;
    private volatile boolean gzip = false;

    public LogbrokerNodeServer(
        final NodeName nodeName,
        final ImmutableBaseServerConfig config)
        throws IOException
    {
        super(config, nodeName.name());

        this.nodeName = nodeName;

        register(new Pattern<>("/pull/read", true), new ReadHandler());
        register(new Pattern<>("/pull/session", true), new SessionHandler());
        register(new Pattern<>("/pull/commit", true), new CommitHandler());
    }

    public synchronized void addPartition(final PartitionMeta meta)
        throws IOException
    {
        this.partitions.put(meta.toString(), new TestPartition(meta));
    }

    // CSOFF: ParameterNumber
    @Override
    protected boolean handleUnsupport(
        final String hostName,
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws
        HttpException, IOException
    {
        if (hostName().equalsIgnoreCase(hostName)) {
            handlerMapper.lookup(request).handle(request, response, context);
            return true;
        }

        return false;
    }
    // CSON: ParameterNumber

    public TestPartition partition(final String longId) {
        return partitions.get(longId);
    }

    public class TestPartition {
        private final PartitionMeta meta;
        private final String host = "127.0.0.1";
        private final int port;
        private final Map<String, Integer> offsets;
        private final Object readWriteLock = new Object();

        private final List<List<String>> data = new ArrayList<>();

        private String lockSession;
        private LoggingHttpServerConnection lockConnection;

        public TestPartition(final PartitionMeta meta) throws IOException {
            this.meta = meta;
            this.port = host().getPort();
            this.offsets = new HashMap<>();
        }

        public int offset(final String client) {
            synchronized (readWriteLock) {
                if (offsets.containsKey(client)) {
                    return offsets.get(client);
                }

                return -1;
            }
        }

        public String lockSession() {
            return lockSession;
        }

        public List<List<String>> data() {
            return data;
        }

        private String meta(final int offset, final int size) {
            return "seqno=1999147379\tserver=pass-dd-i63.sezam"
                + ".yandex.net\tpath=/opt/sezam-logs/blackbox-auth"
                + ".log\tsize=" + size + "\tpartition="
                + meta.id() + "\tident="
                + meta.topic().ident() + "\toffset="
                + offset
                + "\tsourceid=base64:3vM6ggT0po-fS0E32jvjBQ"
                + "\twtime=1502279353961\tsession="
                + lockSession + "\ttopic="
                + meta.topic().toString() + "\ttype="
                + meta.topic().logType()
                + "\tctime=" + System.currentTimeMillis() + '\n';
        }

        public ReadChunk read(final String client) {
            synchronized (readWriteLock) {
                int clientOffset = offset(client);
                if (clientOffset >= data.size() - 1) {
                    return null;
                }

                Charset charset = Charset.forName("utf-8");
                int startOffset = clientOffset;

                clientOffset++;
                List<String> records = data.get(clientOffset);
                String payload = String.join(NEW_LINE, records);
                //record += "\t_stbx=" + id + COLON + clientOffset;
                offsets.put(client, clientOffset);
                String meta =
                    meta(startOffset, payload.getBytes(charset).length);
                String chunk = StringUtils.concat(meta, payload, NEW_LINE);
                System.out.println("Reading");
                System.out.println(chunk);

                return new ReadChunk(
                    chunk.getBytes(charset),
                    records.size());
            }
        }

        public void write(final String string, final boolean newChunk) {
            synchronized (readWriteLock) {
                if (newChunk || data.size() <= 0) {
                    List<String> chunk = new ArrayList<>();
                    chunk.add(string);
                    data.add(chunk);
                } else {
                    data.get(data.size() - 1).add(string);
                }
            }
        }

        public void write(final String string) {
            this.write(string, true);
        }

        public synchronized boolean locked() {
            if (lockSession == null) {
                return false;
            }

            Socket socket = lockConnection.getSocket();
            return socket != null && !socket.isClosed();
        }

        public synchronized boolean locked(final HttpConnection c) {
            if (!locked()) {
                return false;
            }

            return lockConnection != c;
        }

        public synchronized void lock(
            final String session,
            final LoggingHttpServerConnection connection)
            throws ServerException
        {
            if (locked()) {
                if (!session.equals(lockSession)) {
                    throw new ServerException(
                        HttpStatus.SC_CONFLICT,
                        "Partition " + meta.toString()
                            + " locked by " + lockSession);
                }

                if (connection != lockConnection) {
                    throw new BadRequestException("Wrong connection");
                }

                return;
            }

            this.lockSession = session;
            this.lockConnection = connection;
        }

        private synchronized void unlock(
            final String session,
            final LoggingHttpServerConnection connection)
        {
            if (
                locked()
                    && session.equals(lockSession)
                    && connection == lockConnection)
            {
                lockSession = null;
                lockConnection = null;
            }
        }

        @Override
        public String toString() {
            return host + COLON + port + '\t' + meta.toString();
        }
    }

    private class SessionHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            CgiParams params = new CgiParams(request);
            checkRequest(params);

            LoggingHttpServerConnection connection =
                (LoggingHttpServerConnection) context.getAttribute(
                    CONNECTION_ATTR);

            List<TestPartition> paritionsToLock = new ArrayList<>();
            for (String topic: params.getAll(LogbrokerClient.TOPIC)) {
                TestPartition partition = partitions.get(topic);
                if (partition == null) {
                    throw new BadRequestException(
                        "No such partition " + topic
                            + " on " + nodeName.name());
                }

                if (partition.locked(connection)) {
                    throw new ServerException(
                        HttpStatus.SC_CONFLICT,
                        "Session " + topic);
                }

                paritionsToLock.add(partition);
            }

            String session = generator.next();
            List<TestPartition> locked = new ArrayList<>();

            try {
                for (TestPartition partition: paritionsToLock) {
                    partition.lock(session, connection);
                    locked.add(partition);
                }
            } catch (ServerException se) {
                for (TestPartition partition: locked) {
                    partition.unlock(session, connection);
                }

                throw se;
            }

            logger().info("Session created " + session);
            response.addHeader(
                DefaultLogbrokerClient.SESSION_HEADER,
                session);
        }
    }

    private class ReadTask implements Runnable {
        private final CgiParams params;
        private final ChunkedEntity stream;
        private final TestPartition partition;

        ReadTask(
            final TestPartition partition,
            final ChunkedEntity stream,
            final CgiParams params)
        {
            this.stream = stream;
            this.partition = partition;
            this.params = params;
        }

        @Override
        public void run() {
            try {
                int left = params.getInt(
                    ChunkedSession.LIMIT,
                    Integer.MAX_VALUE);

                boolean waitData =
                    params.getBoolean(ChunkedSession.WAIT, false);
                final long sleepInterval = 10;

                String client =
                    params.getString(AbstractLogbrokerClient.CLIENT);

                boolean oneMore = true;
                while (left > 0) {
                    ReadChunk record = partition.read(client);

                    if (record != null) {
                        left -= record.records();
                        stream.addChunk(record.data());
                    } else {
                        if (!waitData || !oneMore) {
                            break;
                        }

                        try {
                            Thread.sleep(sleepInterval);
                        } catch (InterruptedException e) {
                            logger().log(Level.WARNING, "Interrupted", e);
                        }
                        oneMore = false;
                    }
                }
            } catch (InterruptedException | BadRequestException e) {
                logger().log(Level.WARNING, "Unable to write data", e);
            } finally {
                stream.finish();
            }
        }
    }

    private static final class ReadChunk {
        private byte[] data;
        private int records;

        private ReadChunk(final byte[] data, final int records) {
            this.data = data;
            this.records = records;
        }

        public byte[] data() {
            return data;
        }

        public int records() {
            return records;
        }
    }

    private class ReadHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            CgiParams params = new CgiParams(request);
            checkRequest(params);

            HttpConnection httpConn = (HttpConnection) context.getAttribute(
                CONNECTION_ATTR);
            LoggingHttpServerConnection connection
                = (LoggingHttpServerConnection) httpConn;

            String session = params.getString(ChunkedSession.SESSION);
            if (session == null) {
                session = request.getFirstHeader(
                    DefaultLogbrokerClient.SESSION_HEADER).getValue();
            }

            String topicStr = params.getString(ChunkedSession.TOPIC);
            TestPartition partition = partition(topicStr);
            if (session == null) {
                throw new BadRequestException("No session supplied");
            }

            if (partition == null) {
                throw new BadRequestException("Invalid topic " + topicStr);
            }

            partition.lock(session, connection);

            PipedOutputStream pout = new PipedOutputStream();
            PipedInputStream pin = new PipedInputStream(pout);

            //BasicHttpEntity entity = new BasicHttpEntity();

            ChunkedEntity entity = new ChunkedEntity();
            entity.setChunked(true);
            entity.setContent(pin);
            response.setEntity(entity);

            String encodingHeader = "Content-Encoding";
            if (gzip) {
                response.addHeader(encodingHeader, "gzip");
            } else {
                response.addHeader(encodingHeader, "identity");
            }
            executor.submit(new ReadTask(partition, entity, params));
        }
    }

    private static class CommitHandler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity("ok"));
        }
    }
}
