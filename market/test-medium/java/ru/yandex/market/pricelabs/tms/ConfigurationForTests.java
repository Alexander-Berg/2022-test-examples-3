package ru.yandex.market.pricelabs.tms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.FileCopyUtils;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeTextSerializer;
import ru.yandex.market.pricelabs.CoreConfigurationForTests.Basic;
import ru.yandex.market.pricelabs.CoreConfigurationForTests.CleanableQueueDispatcher;
import ru.yandex.market.pricelabs.misc.PricelabsRuntimeException;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.processing.CoreConfig;
import ru.yandex.market.pricelabs.tms.quartz.DefaultTmsDataSourceConfig;
import ru.yandex.market.pricelabs.tms.s3.ExportService;
import ru.yandex.market.pricelabs.tms.yt.YtConfig;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;
import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.utils.LimitedExecutor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.pricelabs.misc.TimingUtils.timeSource;

@Slf4j
public class ConfigurationForTests {

    public interface MockWebServerControls {

        @PostConstruct
        void start();

        @PreDestroy
        void close();

        void cleanup();

        RecordedRequest getMessage();

        RecordedRequest waitMessage() throws InterruptedException;

        void checkNoMessages();

        void enqueue(MockResponse response);

        String url(String path);

        String getMockUrl();

        void addRequestMatcher(Function<RecordedRequest, MockResponse> matcher);

        default void addRequestMatcher(String requestUrl, Consumer<MockResponse> filler) {
            addRequestMatcher(request -> {
                if (Objects.equals(request.toString(), requestUrl)) {
                    var response = new MockResponse();
                    filler.accept(response);
                    return response;
                } else {
                    return null;
                }
            });
        }

        static MockWebServerControls wrap(MockWebServer mockWebServer, CleanableQueueDispatcher dispatcher) {
            return ConfigurationForTests.wrapMockWebServer(mockWebServer, dispatcher);
        }
    }

    private static MockWebServerControls wrapMockWebServer(MockWebServer mockWebServer,
                                                           CleanableQueueDispatcher defaultDispatcher) {
        mockWebServer.setDispatcher(defaultDispatcher);
        return new MockWebServerControls() {

            private final List<Function<RecordedRequest, MockResponse>> matchers = new ArrayList<>();
            private boolean setDefault;

            private void resetDispatcher() {
                mockWebServer.setDispatcher(defaultDispatcher);
                setDefault = true;
                matchers.clear();
            }

            @Override
            public void start() {
                try {
                    mockWebServer.start();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to start Mock WebServer", e);
                }
            }

            @Override
            public void close() {
                try {
                    mockWebServer.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to stop Mock WebServer", e);
                }
            }

            @Override
            public void cleanup() {
                this.resetDispatcher();
                while (true) {
                    if (getMessageImpl() == null) {
                        defaultDispatcher.clean();
                        break; // ---
                    }
                }
            }

            @Override
            public RecordedRequest getMessage() {
                return Objects.requireNonNull(getMessageImpl(), "No message received from " + mockWebServer);
            }

            @Override
            public RecordedRequest waitMessage() throws InterruptedException {
                return mockWebServer.takeRequest();
            }

            @Override
            public void checkNoMessages() {
                assertNull(getMessageImpl());
            }

            @Override
            public void enqueue(MockResponse response) {
                mockWebServer.enqueue(response);
            }


            @Override
            public String url(String path) {
                return mockWebServer.url(path).toString();
            }

            @Override
            public String getMockUrl() {
                return "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
            }

            @Nullable
            private RecordedRequest getMessageImpl() {
                try {
                    return mockWebServer.takeRequest(1, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new PricelabsRuntimeException("Unable to take requests from mock web server", e);
                }
            }

            @Override
            public void addRequestMatcher(Function<RecordedRequest, MockResponse> matcher) {
                this.initCustomDispatcher();
                matchers.add(matcher);
            }

            private void initCustomDispatcher() {
                if (setDefault) {
                    mockWebServer.setDispatcher(new Dispatcher() {
                        @Override
                        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                            for (Function<RecordedRequest, MockResponse> matcher : matchers) {
                                @Nullable var response = matcher.apply(request);
                                if (response != null) {
                                    return response;
                                }
                            }
                            return defaultDispatcher.dispatch(request);
                        }
                    });
                    setDefault = false;
                }
            }

        };
    }

    @Configuration
    @Import(Basic.class)
    public static class Services {

        @Bean
        public MockWebServerControls mockWebServerPartnerApi() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String partnerApiUrl(@Qualifier("mockWebServerPartnerApi") MockWebServerControls server) {
            return server.url("partner_api/");
        }

        @Bean
        public Supplier<String> partnerApiTicketSource() {
            return () -> "ticket-" + timeSource().getMillis();
        }

        @Bean
        public Supplier<String> amoreTicketSource() {
            return () -> "ticket-" + timeSource().getMillis();
        }

        @Bean
        public MockWebServerControls mockWebServerMarketReport() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public MockWebServerControls mockWebServerMarketReportLowLatency() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String marketReportUrl(@Qualifier("mockWebServerMarketReport") MockWebServerControls server) {
            return server.url("market_report/");
        }

        @Bean
        public String marketReportLowLatencyUrl(
                @Qualifier("mockWebServerMarketReportLowLatency") MockWebServerControls server) {
            return server.url("market_report/");
        }

        @Bean
        public MockWebServerControls mockWebServerMarketIndexer() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String marketIndexerUrl(@Qualifier("mockWebServerMarketIndexer") MockWebServerControls server) {
            return server.url("market_indexer/");
        }

        @Bean
        public MockWebServerControls mockWebServerAmore() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String marketAmoreUrl(@Qualifier("mockWebServerAmore") MockWebServerControls server) {
            return server.url("amore/");
        }

        @Bean
        public MockWebServerControls mockWebServerStatface() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String statfaceUploadUrl(@Qualifier("mockWebServerStatface") MockWebServerControls server) {
            return server.url("statface/");
        }

        @Bean
        public MockWebServerControls mockWebServerGaRefresh() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String gaRefreshBaseUrl(@Qualifier("mockWebServerGaRefresh") MockWebServerControls server) {
            return server.url("ga_refresh/");
        }

        @Bean
        public MockWebServerControls mockWebServerGa() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String gaBaseUrl(@Qualifier("mockWebServerGa") MockWebServerControls server) {
            return server.url("ga/");
        }

        @Bean
        public MockWebServerControls mockWebServerYm() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String ymBaseUrl(@Qualifier("mockWebServerYm") MockWebServerControls server) {
            return server.url("ym/");
        }

    }

    @Configuration
    @Import(Services.class)
    public static class Juggler {

        @Bean
        public MockWebServerControls mockWebServerJuggler() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String jugglerUrl(@Qualifier("mockWebServerJuggler") MockWebServerControls mockWebServerMds) {
            return mockWebServerMds.url("juggler/");
        }
    }

    @Configuration
    @Import(Services.class)
    public static class Mds {

        @Bean
        public MockWebServerControls mockWebServerMds() {
            return MockWebServerControls.wrap(Basic.mockWebServer(), Basic.getAgnosticDispatcher());
        }

        @Bean
        public String mdsApiUrl(@Qualifier("mockWebServerMds") MockWebServerControls mockWebServerMds) {
            return mockWebServerMds.url("mds/");
        }
    }

    @Configuration
    public static class Exports {

        private final BlockingQueue<ExportItem> items = new BlockingArrayQueue<>();

        @Bean
        public ExportService exportService() {
            return (filePath, file) -> {
                byte[] bytes;
                try {
                    bytes = FileCopyUtils.copyToByteArray(file);
                } catch (IOException io) {
                    throw new RuntimeException(io);
                }
                items.add(new ExportItem(filePath, bytes));
            };
        }

        @Bean
        public Supplier<ExportItem> exportServiceQueue() {
            return items::poll;
        }

        @Value
        public static class ExportItem {
            @NonNull String filePath;
            @NonNull byte[] content;
        }
    }

    @Configuration
    public static class UtilityMethods {

        @DependsOn("postgresForTests")
        @Bean
        public TestControls testControls() {
            return new TestControls();
        }
    }

    @Configuration
    public static class Quartz {

        @Bean
        @Autowired
        public Properties quartzProperties(ResourceLoader resourceLoader) throws IOException {
            try (InputStream stream = resourceLoader.getResource("classpath:quartz.properties").getInputStream()) {
                Properties properties = new Properties();
                properties.load(stream);
                return properties;
            }
        }
    }

    @Configuration
    @Import(DefaultTmsDataSourceConfig.class)
    public static class MyDatabaseSchedulerFactoryConfig extends DatabaseSchedulerFactoryConfig {

        public MyDatabaseSchedulerFactoryConfig(ApplicationContext applicationContext,
                                                TmsDataSourceConfig tmsDataSourceConfig) {
            super(applicationContext, tmsDataSourceConfig);
        }

        @Bean
        @Override
        public SchedulerFactoryBean schedulerFactoryBean() {
            var bean = super.schedulerFactoryBean();
            bean.setAutoStartup(false); // Выключаем autostart
            return bean;
        }
    }

    @Configuration
    @Import({YtConfig.class, CoreConfig.class})
    public static class YtConfiguration {

        private final List<String> functions = List.of("has_substring_all", "has_substring_any");

        @Autowired
        public void initYt(YtClientProxy ytClient, LimitedExecutor executor) throws IOException {
            try (var group = executor.groupExecutor()) {
                for (String function : functions) {
                    log.info("Initializing function: {}", function);

                    var body = Utils.getResourceStream("classpath:functions/" + function).readAllBytes();

                    var map = YTreeTextSerializer.deserialize(
                            Utils.getResourceStream("classpath:functions/" + function + ".yson")).asMap();

                    var attributes = YTree.mapBuilder()
                            .key("function_descriptor").value(map)
                            .buildMap().asMap();

                    log.info("Function descriptor: {}", attributes);
                    ytClient.matchReplicas().stream()
                            .map(replica -> (Runnable) () -> {
                                @Nullable String udfPath = replica.getUdfRegistryPath();
                                if (udfPath != null) {
                                    replica.writeFile(udfPath + "/" + function,
                                            new ByteArrayInputStream(body), attributes);
                                }
                            }).forEach(group::addCall);
                }
            }
            log.info("Functions were uploaded...");
        }
    }
}
