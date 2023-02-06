package ru.yandex.mail.so.factors.sherlock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import com.google.protobuf.Int64Value;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.ConstFunction;
import ru.yandex.function.NullConsumer;
import ru.yandex.http.proxy.HttpProxy;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.EmptyRequestsListener;
import ru.yandex.http.util.nio.client.RequestsListener;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.http.util.server.HttpProxyConfigBuilder;
import ru.yandex.http.util.server.ImmutableHttpProxyConfig;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.writer.JsonType;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.mail.so.api.v1.ConnectInfo;
import ru.yandex.mail.so.api.v1.EmailInfo;
import ru.yandex.mail.so.api.v1.SmtpEnvelope;
import ru.yandex.mail.so.factors.BasicSoFunctionInputs;
import ru.yandex.mail.so.factors.FactorsAccessViolationHandler;
import ru.yandex.mail.so.factors.LoggingFactorsAccessViolationHandler;
import ru.yandex.mail.so.factors.SoFactor;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorFactoryContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorsRegistry;
import ru.yandex.mail.so.factors.types.SmtpEnvelopeSoFactorType;
import ru.yandex.mail.so.factors.types.SoFactorTypesRegistry;
import ru.yandex.mail.so.factors.types.TikaiteDocSoFactorType;
import ru.yandex.parser.mail.envelope.SmtpEnvelopeHolder;
import ru.yandex.parser.mail.errors.ErrorInfo;
import ru.yandex.stater.NullStatersRegistrar;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;

public class TemplateMasterExtractorTest extends TestBase {
    private static final int TIMEOUT = 2000;

    private static ImmutableHttpProxyConfig config() throws Exception {
        BaseServerConfigBuilder baseConfig =
            new BaseServerConfigBuilder(
                Configs.baseConfig("TemplateMasterTest"));
        HttpProxyConfigBuilder httpProxyConfig = new HttpProxyConfigBuilder();
        baseConfig.copyTo(httpProxyConfig);
        return httpProxyConfig.build();
    }

    @Test
    public void testFound() throws Exception {
        try (StaticServer templateMaster =
                 new StaticServer(Configs.baseConfig("TemplateMaster"));
             HttpProxy<ImmutableHttpProxyConfig> proxy =
                 new HttpProxy<>(config()))
        {
            templateMaster.start();
            System.setProperty(
                "TEMPLATE_MASTER_HOST",
                templateMaster.host().toString());

            String templateMasterHandle = "/route?domain=gmail.com&attributes="
                + "%7B%22from%22:%22tnse@gmail.com%22,%22subject%22:%22Hello+where"
                + "!%22,%22queueId%22:%22aba-caba%22,%22uids%22:%5B1234567%5D%7D";
            templateMaster.add(
                templateMasterHandle,
                new ExpectingHttpItem(
                    new StringChecker("<a href='yandex.ru'>text</a>"),
                    "{\"status\":\"FoundInDb\","
                        + "\"delta\":[[\"<a href='yandex.ru'>\"],[]],"
                        + "\"attributes\":[],"
                        + "\"stable_sign\":663465322314928363}"));

            JsonMap doc =
                JsonObject.adapt(
                    BasicContainerFactory.INSTANCE,
                    Map.of(
                        "hdr_subject", "Hello where!",
                        "hdr_from_normalized", "tnse@gmail.com",
                        "html_body", "<a href='yandex.ru'>text</a>"))
                    .asMap();


            SmtpEnvelope smtpEnvelope = SmtpEnvelope.newBuilder()
                .setConnectInfo(
                    ConnectInfo.newBuilder().setSessionId("aba-caba"))
                .addRecipients(
                    EmailInfo.newBuilder().setUid(Int64Value.of(1234567)))
                .build();

            ExpectingCallback callback = new ExpectingCallback();

            FakeContext context = new FakeContext();
            BasicSoFunctionInputs inputs =
                new BasicSoFunctionInputs(
                    context.accessViolationHandler(),
                    TikaiteDocSoFactorType.TIKAITE_DOC.createFactor(doc),
                    SmtpEnvelopeSoFactorType.SMTP_ENVELOPE.createFactor(
                        new SmtpEnvelopeHolder(smtpEnvelope)));
            SoFactorsExtractorFactoryContext factoryContext =
                new SoFactorsExtractorFactoryContext(
                    null,
                    new SoFactorsExtractorsRegistry(
                        NullStatersRegistrar.INSTANCE,
                        new SoFactorTypesRegistry()),
                    new ConstFunction<>(NullConsumer.instance()),
                    new LongAdder(),
                    new LongAdder(),
                    Thread.currentThread().getThreadGroup(),
                    proxy,
                    proxy,
                    new HashMap<>(),
                    new HashMap<>(),
                    logger,
                    null,
                    0L);
            TemplateMasterExtractor extractor =
                new TemplateMasterExtractor(
                    "TemplateMaster",
                    factoryContext,
                    factoryContext.readBodyAsIniConfig(
                        "host = $(TEMPLATE_MASTER_HOST)\n"
                            + "connections = 32\n"
                            + "body-fields = html_body, pure_body"));
            proxy.start();
            extractor.extract(context, inputs, callback);
            waitAndCheck(templateMaster, templateMasterHandle, 1);
            callback.assertFactor(
                "{\"stable_sign\": 663465322314928363,"
                    + "\"donor_and_contains_urls\": false,"
                    + "\"delta\": [\"<a href='yandex.ru'>\"]}");
        }
    }

    @Test
    public void testNotFound() throws Exception {
        try (StaticServer templateMaster =
                 new StaticServer(Configs.baseConfig("TemplateMaster"));
             HttpProxy<ImmutableHttpProxyConfig> proxy =
                new HttpProxy<>(config()))
        {
            templateMaster.start();
            System.setProperty(
                "TEMPLATE_MASTER_HOST",
                templateMaster.host().toString());

            String templateMasterHandle = "/route?domain=gmail.com"
                + "&attributes=%7B%22from%22:%22tnse@gmail.com%22%7D";
            templateMaster.add(
                templateMasterHandle,
                new ExpectingHttpItem(
                    new StringChecker("Hello, world"),
                    "{\"status\":\"NotFound\"}"));

            ExpectingCallback callback = new ExpectingCallback();
            JsonMap doc =
                JsonObject.adapt(
                    BasicContainerFactory.INSTANCE,
                    Map.of(
                        "pure_body", "Hello, world",
                        "hdr_from_normalized", "tnse@gmail.com"
                    ))
                    .asMap();

            SoFactor<?> docFactor =
                TikaiteDocSoFactorType.TIKAITE_DOC.createFactor(doc);
            FakeContext context = new FakeContext();
            BasicSoFunctionInputs inputs =
                new BasicSoFunctionInputs(
                    context.accessViolationHandler(),
                    2);
            inputs.set(0, docFactor);
            SoFactorsExtractorFactoryContext factoryContext =
                new SoFactorsExtractorFactoryContext(
                    null,
                    new SoFactorsExtractorsRegistry(
                        NullStatersRegistrar.INSTANCE,
                        new SoFactorTypesRegistry()),
                    new ConstFunction<>(NullConsumer.instance()),
                    new LongAdder(),
                    new LongAdder(),
                    Thread.currentThread().getThreadGroup(),
                    proxy,
                    proxy,
                    new HashMap<>(),
                    new HashMap<>(),
                    logger,
                    null,
                    0L);
            TemplateMasterExtractor extractor =
                new TemplateMasterExtractor(
                    "TemplateMaster",
                    factoryContext,
                    factoryContext.readBodyAsIniConfig(
                        "host = $(TEMPLATE_MASTER_HOST)\n"
                            + "connections = 32\n"
                            + "body-fields = html_body, pure_body"));
            proxy.start();
            extractor.extract(context, inputs, callback);
            waitAndCheck(templateMaster, templateMasterHandle, 1);
            callback.assertFactor(null);
        }
    }

    private static class ExpectingCallback
        implements FutureCallback<List<SoFactor<?>>>
    {
        private CompletableFuture<List<SoFactor<?>>> saved =
            new CompletableFuture<>();

        @Override
        public void completed(List<SoFactor<?>> soFactors) {
            saved.complete(soFactors);
        }

        @Override
        public void failed(Exception e) {
            saved.completeExceptionally(e);
        }

        @Override
        public void cancelled() {
            saved.cancel(false);
        }

        public void assertFactor(String expectedJson)
            throws InterruptedException, ExecutionException, TimeoutException
        {
            List<SoFactor<?>> soFactors =
                saved.get(TIMEOUT, TimeUnit.MILLISECONDS);
            int size = soFactors.size();
            if (size == 1 && soFactors.get(0) == null) {
                size = 0;
            }
            Assert.assertEquals(
                "Return factors count",
                expectedJson == null ? 0 : 1,
                size);
            if (expectedJson == null) {
                return;
            }
            JsonChecker jc = new JsonChecker(expectedJson);
            jc.check(JsonType.NORMAL.toString(soFactors.get(0)));
        }
    }

    private class FakeContext implements SoFactorsExtractorContext {
        private final FactorsAccessViolationHandler accessViolationHandler;
        private final HttpContext httpContext;

        FakeContext() {
            accessViolationHandler =
                new LoggingFactorsAccessViolationHandler(
                    new LongAdder(),
                    logger);
            httpContext = new BasicHttpContext();
            httpContext.setAttribute(
                HttpCoreContext.HTTP_REQUEST,
                new BasicHttpRequest("POST", ""));
        }

        @Override
        public FactorsAccessViolationHandler accessViolationHandler() {
            return accessViolationHandler;
        }

        @Override
        public PrefixedLogger logger() {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpContext httpContext() {
            return httpContext;
        }

        @Override
        public RequestsListener requestsListener() {
            return EmptyRequestsListener.INSTANCE;
        }

        @Override
        public Consumer<ErrorInfo> errorsConsumer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Executor executor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean debugExtractors() {
            return false;
        }

        @Override
        public Set<String> debugFlags() {
            return Collections.emptySet();
        }

        // CancellationSubscriber implementation
        @Override
        public boolean cancelled() {
            return false;
        }

        @Override
        public void subscribeForCancellation(final Cancellable callback) {
        }

        @Override
        public void unsubscribeFromCancellation(final Cancellable callback) {
        }
    }

    private void waitAndCheck(StaticServer server, String handle, int times)
        throws InterruptedException
    {
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Assert.assertEquals(server.getName() + " " + handle + " calls",
                                    times, server.accessCount(handle));
                break;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() - start > TIMEOUT) {
                    throw e;
                }
                Thread.sleep(TIMEOUT >> 4);
            }
        }
    }
}
