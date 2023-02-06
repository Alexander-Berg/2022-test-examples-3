package ru.yandex.chemodan.util.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.Builder;
import lombok.SneakyThrows;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.ObjectMapperFactory;
import org.mockserver.client.serialization.deserializers.string.NottableStringDeserializer;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.model.Header;
import org.mockserver.model.Headers;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.NottableString;
import org.mockserver.model.Parameters;
import org.mockserver.model.StringBody;
import org.mockserver.socket.PortFactory;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.file.FileNotFoundIoException;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

import static org.mockserver.model.NottableString.deserializeNottableString;

@Builder
public class HttpRecorderRule implements TestRule {

    private static final Logger logger = LoggerFactory.getLogger(HttpRecorderRule.class);

    private final ExpectationSerializer serializer = new ExpectationSerializer();
    @Builder.Default
    private final int port = PortFactory.findFreePort();
    private final String prefix;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreParams {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreHeaders {
        String[] value() default {};
    }

    private static class PatchedNottableStringDeserializer extends NottableStringDeserializer {
        @Override
        public NottableString deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
            if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
                return deserializeNottableString(jsonParser.getCurrentName());
            } else {
                return super.deserialize(jsonParser, ctxt);
            }
        }
    }

    static {
        patchMapper();
    }

    @SneakyThrows
    private static void patchMapper() {
        Field field = ObjectMapperFactory.class.getDeclaredField("OBJECT_MAPPER");
        field.setAccessible(true);
        ObjectMapper mapper = (ObjectMapper) field.get(null);
        mapper.registerModule(
                new SimpleModule().addDeserializer(NottableString.class, new PatchedNottableStringDeserializer()));
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (ClientAndServer cas = ClientAndServer.startClientAndServer(port)) {
                    Option<String> data = loadExpectations(description.getTestClass(), description.getMethodName());
                    if (data.isPresent()) {
                        doReplay(cas, base, description, data.get());
                    } else {
                        doRecord(cas, base, description);
                    }
                }
            }
        };
    }

    private void doRecord(ClientAndServer cas, Statement base, Description description) throws Throwable {
        base.evaluate();
        save(cas, description);
    }

    private void doReplay(ClientAndServer cas, Statement base, Description description, String data) throws Throwable {
        load(cas, description, data);
        try {
            base.evaluate();
        } catch (Throwable e) {
            checkLogs(cas).ifPresent(log -> {
                throw new RuntimeException(log, e);
            });
            throw e;
        }
        checkLogs(cas).ifPresent(log -> {
            throw new RuntimeException(log);
        });
        Assert.A.equals(0, cas.retrieveActiveExpectations(null).length);
    }

    private static Option<String> checkLogs(ClientAndServer cas) {
        for (String log : cas.retrieveLogMessagesArray(null)) {
            if (log.contains("no active expectations")) {
                return Option.of(log);
            }
            if (log.contains("did not match expectation")) {
                return Option.of(log);
            }
        }
        return Option.empty();
    }

    private static Option<String> loadExpectations(Class<?> klass, String method) {
        try {
            return Option.ofNullable(
                    ClassLoaderUtils.loadText(klass, String.format("%s/%s.json", klass.getSimpleName(), method)));
        } catch (FileNotFoundIoException e) {
            return Option.empty();
        }
    }

    private void load(ClientAndServer cas, Description description, String data) {
        if (!data.isEmpty()) {
            data = serializer.serialize(Stream.of(serializer.deserializeArray(data)).peek(e -> {
                Option.ofNullable(e.getHttpRequest()).ifPresent(q -> {
                    if (hasAnnotation(description, IgnoreParams.class)) {
                        q.withQueryStringParameters((Parameters) null);
                    }
                    if (hasAnnotation(description, IgnoreHeaders.class)) {
                        IgnoreHeaders ignoreHeaders = getAnnotation(description, IgnoreHeaders.class);
                        if (ignoreHeaders.value().length == 0) {
                            q.withHeaders((Headers) null);
                        } else {
                            SetF<String> headers = Cf.x(ignoreHeaders.value()).unique();
                            q.withHeaders(
                                    Cf.x(q.getHeaderList()).filter(h -> !headers.containsTs(h.getName().getValue())));
                        }
                    }
                });
                Option.ofNullable(e.getHttpResponse()).ifPresent(s -> {
                    if (Objects.nonNull(s.getBody())) {
                        // when loading body saved body from file without type, encoding corrupted
                        int length = s.getBody() instanceof StringBody
                                ? s.getBodyAsString().getBytes(StandardCharsets.UTF_8).length
                                : s.getBody().getRawBytes().length;

                        s.getHeaders().replaceEntry("Content-Length", "" + length);
                    }
                });
            }).toArray(Expectation[]::new));
            new NettyHttpClient().sendRequest(HttpRequest.request()
                    .withMethod("PUT")
                    .withPath("/expectation")
                    .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
                    .withBody(data)
                    .withHeader(HttpHeaderNames.HOST.toString(),
                            "localhost:" + cas.remoteAddress().getPort())
            );
        }
    }

    @SneakyThrows
    private static boolean hasAnnotation(Description description, Class<? extends Annotation> klass) {
        return Objects.nonNull(description.getTestClass().getMethod(description.getMethodName()).getAnnotation(klass));
    }

    @SneakyThrows
    private static <T extends Annotation> T getAnnotation(Description description, Class<T> klass) {
        return description.getTestClass().getMethod(description.getMethodName()).getAnnotation(klass);
    }

    private void save(ClientAndServer cas, Description description) throws IOException {
        String json = serializer.serialize(cas.retrieveRecordedExpectations(null));
        File file = getDataPath(description.getTestClass(), description.getMethodName());
        if (file.getParentFile().mkdirs()) {
            logger.info("created " + file.getParentFile());
        }
        try (OutputStream stream = new FileOutputStream(file)) {
            stream.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private File getDataPath(Class<?> klass, String method) {
        return new File(prefix + ClassLoaderUtils.resourcePath(klass, klass.getSimpleName()) + "/" + method + ".json");
    }

    public HttpHost getHost() {
        return new HttpHost("localhost", port);
    }

    public static class CustomHttpsProxyRoutePlanner implements HttpRoutePlanner {
        private final SchemePortResolver schemePortResolver = DefaultSchemePortResolver.INSTANCE;

        @Override
        public HttpRoute determineRoute(HttpHost host, org.apache.http.HttpRequest request,
                                        HttpContext context) throws HttpException {
            Args.notNull(request, "Request");
            if (host == null) {
                throw new ProtocolException("Target host is not specified");
            }
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final RequestConfig config = clientContext.getRequestConfig();
            final InetAddress local = config.getLocalAddress();
            HttpHost proxy = config.getProxy();

            final HttpHost target;
            if (host.getPort() <= 0) {
                try {
                    target = new HttpHost(
                            host.getHostName(),
                            this.schemePortResolver.resolve(host),
                            host.getSchemeName());
                } catch (final UnsupportedSchemeException ex) {
                    throw new HttpException(ex.getMessage());
                }
            } else {
                target = host;
            }
            final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
            if (proxy == null) {
                return new HttpRoute(target, local, secure);
            } else {
                return new HttpRoute(target, local, proxy, false, RouteInfo.TunnelType.PLAIN,
                        RouteInfo.LayerType.PLAIN);
            }
        }
    }

}
