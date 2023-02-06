package ru.yandex.autotests.innerpochta.imap.core.imap;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.rules.ExternalResource;

import ru.yandex.autotests.innerpochta.imap.config.ImapProperties;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.ConnectionTypes.SSL;

public class ImapSessionImpl extends ExternalResource implements Closeable {

    public static final int MAX_LENGTH = 10000000;
    public static final int TIMEOUT_SEC = 40;
    private final Logger log = LogManager.getLogger(this.getClass());
    private IoSession ioSession;
    private String host;
    private int port;
    private String connectionType;

    private IoConnector connector;

    private String id = "unknown_session_id";
    private String usertag = "B";

    public ImapSessionImpl(String host, Integer port, String type) {
        this.connectionType = type;
        this.host = host;
        this.port = port;
    }

    public static Callable<String> callRead(final IoSession ioSession, final IoConnector connector) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                ReadFuture read = ioSession.read().awaitUninterruptibly();
                if (read.isRead()) {
                    return read.getMessage().toString();
                } else {
                    //если не можем прочитать выкидываем исключение
                    connector.dispose();
                    ioSession.close(true);
                    throw new RuntimeException();
                }
            }
        };
    }

    public String readLine() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Future<String>> futureList = executor.invokeAll(asList(callRead(ioSession, connector)),
                    TIMEOUT_SEC, TimeUnit.SECONDS);
            return futureList.get(0).get();
        } catch (CancellationException | InterruptedException e) {
            connector.dispose();
            ioSession.close(true);
            throw new AssertionError(String.format("ERROR: Reading message was canceled, waited response > %d sec",
                    TIMEOUT_SEC), e);

        } catch (ExecutionException e) {
            connector.dispose();
            ioSession.close(true);
            throw new RuntimeException("ERROR: Message wasn't read in session", e);
        } finally {
            executor.shutdown();
        }
    }

    public void writeLine(String line) {
        WriteFuture write = ioSession.write(line).awaitUninterruptibly();
        // Wait until the message is completely written out to the O/S buffer.
        if (!write.isWritten()) {
            connector.dispose();
            ioSession.close(true);
            throw new RuntimeException("ERROR: The message couldn't be written out completely for some reason");
        }
    }

    public String id() {
        return id;
    }

    public String usertag() {
        return usertag;
    }

    public ImapSessionImpl usertag(String usertag) {
        this.usertag = usertag;
        return this;
    }

    private IoSession connect(String host, int port, String type) throws Throwable {
        connector = new NioSocketConnector();

        TextLineCodecFactory codecFactory = new TextLineCodecFactory(Charsets.UTF_8);
        codecFactory.setDecoderMaxLineLength(MAX_LENGTH);
        codecFactory.setEncoderMaxLineLength(MAX_LENGTH);

        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));

        connector.setHandler(new TelnetSessionHandler(randomAlphanumeric(3)));

        connector.getSessionConfig().setReadBufferSize(2048);
        connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        connector.getSessionConfig().setUseReadOperation(true);
        connector.getSessionConfig().setWriteTimeout(TIMEOUT_SEC);

        //SSL, костыль из-за [DIRMINA-805][AUTOTESTPERS-136]
        SslFilter sslFilter = new SslFilter(getSslContext());
        sslFilter.setEnabledCipherSuites(SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites());
        sslFilter.setUseClientMode(true);
        sslFilter.setNeedClientAuth(true);

        ConnectFuture future = connector.connect(new InetSocketAddress(host, port)).await();
        future.awaitUninterruptibly();

        if (connectionType.equals(SSL.value())) {
            log.warn("[Session]: Start SSL connection...");
            addSslFilter(future.getSession());
        }


        return future.getSession();
    }

    private SSLContext getSslContext() throws Throwable {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    private static boolean isCipherSuiteOk(final String cipherSuite) {
        // Как минимум некоторые из остальных шифров приводят к одному из следующего:
        // - Зависание ввода/вывода через `ioSession`. (Для шифров без "_WITH_" в названии.)
        // - `getMessage` в `before` возвращает `null` (и, как следствие, обращение к `toString` бросает NPE).
        return cipherSuite.startsWith("TLS_ECDHE_RSA_WITH_") || cipherSuite.startsWith("TLS_RSA_WITH_");
    }

    private SslFilter getSslFilter() throws Throwable {
        String[] cipherSuites = SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites();
        cipherSuites = Arrays.stream(cipherSuites).filter(ImapSessionImpl::isCipherSuiteOk).toArray(String[]::new);

        //SSL, костыль из-за [DIRMINA-805][AUTOTESTPERS-136]
        SslFilter sslFilter = new SslFilter(getSslContext());
        sslFilter.setEnabledCipherSuites(cipherSuites);
        sslFilter.setUseClientMode(true);
        sslFilter.setNeedClientAuth(true);
        return sslFilter;
    }

    public ImapSessionImpl addSslFilter(IoSession session) throws Throwable {
        log.warn("[Session]: PLAINTEXT -> SSL/TLS ...");
        SslFilter sslFilter = getSslFilter();
        session.getFilterChain().addFirst(ImapProperties.ConnectionTypes.SSL.value(), sslFilter);

        if (!sslFilter.isSslStarted(session)) {
            throw new RuntimeException("ERROR: SSL not started");
        }
        return this;
    }

    public IoSession getSession() {
        return ioSession;
    }

    @Override
    protected void before() throws Throwable {
        log.warn("[Session]: Start new session...");
        ioSession = connect(host, port, connectionType);
        //Get session id
        ReadFuture greeting = ioSession.read().await();
        id = parseId(greeting.getMessage().toString());
    }

    @Override
    protected void after() {
        close();
    }

    @Override
    public void close() {
        log.warn("[Session]: Close current session <" + id + ">");
        connector.dispose();
        ioSession.close(true);
    }

    private String parseId(String from) {
        return StringUtils.substringAfterLast(from, ", ");
    }
}
