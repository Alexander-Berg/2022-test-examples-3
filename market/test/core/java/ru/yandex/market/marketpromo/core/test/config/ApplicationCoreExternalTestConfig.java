package ru.yandex.market.marketpromo.core.test.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.yandex.ydb.auth.tvm.YdbClientId;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.YtImpl;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.OperationStatus;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.operations.map.Mapper;
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.common.trace.Tracer;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.config.LogbrokerServiceCommonConfig;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsServiceStub;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.marketpromo.core.application.context.Logbroker;
import ru.yandex.market.marketpromo.core.application.context.OfferStorage;
import ru.yandex.market.marketpromo.core.application.properties.LogbrokerAssortmentPropagationTopicProperties;
import ru.yandex.market.marketpromo.core.application.properties.LogbrokerAssortmentTopicProperties;
import ru.yandex.market.marketpromo.core.application.properties.LogbrokerProperties;
import ru.yandex.market.marketpromo.core.application.properties.OfferStorageProperties;
import ru.yandex.market.marketpromo.core.application.properties.S3Properties;
import ru.yandex.market.marketpromo.core.application.properties.SecurityAccessProperties;
import ru.yandex.market.marketpromo.core.application.properties.YdbProperties;
import ru.yandex.market.marketpromo.core.application.properties.YtProperties;
import ru.yandex.market.marketpromo.core.config.TvmConfig;
import ru.yandex.market.marketpromo.core.config.logbroker.LogbrokerConfig.AssortmentPropagationTopicConfig;
import ru.yandex.market.marketpromo.core.config.logbroker.LogbrokerConfig.LogbrokerCredentialProductionConfig;
import ru.yandex.market.marketpromo.core.config.offerstorage.OfferStorageConfig;
import ru.yandex.market.marketpromo.core.config.s3.S3Config;
import ru.yandex.market.marketpromo.core.config.ydb.YdbConfig;
import ru.yandex.market.marketpromo.core.config.yt.YtConfig;
import ru.yandex.market.marketpromo.core.dao.CatmanYtDao;
import ru.yandex.market.marketpromo.core.dao.OfferYtDao;
import ru.yandex.market.marketpromo.core.dao.PromoYtDao;
import ru.yandex.market.marketpromo.core.data.source.logbroker.OfferLogbrokerEvent;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.OfferStorageSaasClient;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.OfferStorageStrollerClient;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.util.OfferDataConverter;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.util.PromoDataConverter;
import ru.yandex.market.marketpromo.core.data.source.yt.DefaultYtTableClient;
import ru.yandex.market.marketpromo.core.data.source.yt.FallbackYtTableClientProxy;
import ru.yandex.market.marketpromo.core.data.source.yt.YtTableClient;
import ru.yandex.market.marketpromo.core.scheduling.executors.CPIExecutorService;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;

import static io.grpc.internal.GrpcUtil.getThreadFactory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtTableClient.YtCluster.HAHN;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.LOGBROKER_ACTIVE;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.OFFER_STORAGE_ACTIVE;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.S3_ACTIVE;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.YT_ACTIVE;

@Configuration
@Import({
        TvmConfig.class,
        YdbConfig.class
})
@EnableConfigurationProperties({
        YdbProperties.class,
        YtProperties.class,
        S3Properties.class,
        OfferStorageProperties.class,
        LogbrokerProperties.class,
        LogbrokerAssortmentTopicProperties.class,
        LogbrokerAssortmentPropagationTopicProperties.class
})
public class ApplicationCoreExternalTestConfig {

    @Configuration
    @Profile("!" + YT_ACTIVE)
    public static class YtMockedConfig {

        private final YtProperties ytProperties;

        public YtMockedConfig(YtProperties ytProperties) {
            this.ytProperties = ytProperties;
        }

        @Bean
        public YtTables ytTables() {
            return Mockito.mock(YtTables.class);
        }

        @Bean
        public YtOperations ytOperations() {
            YtOperations operations = Mockito.mock(YtOperations.class);
            Operation operation = Mockito.mock(Operation.class);
            when(operation.getId()).thenReturn(GUID.create());
            when(operation.getStatus()).thenReturn(OperationStatus.RUNNING);
            when(operations.mapReduceAndGetOp(
                    any(YPath.class),
                    any(YPath.class),
                    any(Mapper.class),
                    anyList(),
                    any(Reducer.class))).thenReturn(operation);
            return operations;
        }

        @Bean
        public Cypress cypress() {
            return Mockito.mock(Cypress.class);
        }

        @Bean
        public YtTableClient ytTableClient(YtTables tables,
                                           YtOperations operations,
                                           Cypress cypress,
                                           Tracer tracer) {
            return new FallbackYtTableClientProxy(Map.of(HAHN, client(tables, operations, cypress, tracer)));
        }

        private YtTableClient client(YtTables tables,
                                     YtOperations operations,
                                     Cypress cypress,
                                     Tracer tracer) {
            YtImpl ytMock = Mockito.mock(YtImpl.class);
            YtConfiguration ytConfiguration = Mockito.mock(YtConfiguration.class);
            when(ytMock.tables()).thenReturn(tables);
            when(ytMock.operations()).thenReturn(operations);
            when(ytMock.cypress()).thenReturn(cypress);
            when(ytMock.getConfiguration()).thenReturn(ytConfiguration);
            return new DefaultYtTableClient(ytMock, tracer);
        }

        @Bean
        public PromoYtDao promoYtDao(
                YtTableClient ytTableClient,
                PromoDataConverter promoDataConverter
        ) {
            return new PromoYtDao(ytTableClient, promoDataConverter, ytProperties.getPromoDescriptionPath(),
                    ytProperties.getPromoStoragePath());
        }

        @Bean
        public CatmanYtDao catmanYtDaoMock(YtTableClient ytTableClient) {
            return new CatmanYtDao(ytTableClient,
                    ytProperties.getCatmanCategoriesPath(),
                    ytProperties.getCategoryTreePath());
        }

        @Bean
        public OfferYtDao offerYtDaoMock(YtTableClient ytTableClient,
                                         OfferDataConverter offerDataConverter,
                                         SecurityAccessProperties securityAccessProperties) {
            return new OfferYtDao(ytTableClient, offerDataConverter, ytProperties.getOffersPath(),
                    ytProperties.getOffersResultPath(), ytProperties.getOffersCountResultPath(),
                    securityAccessProperties);
        }
    }

    @Configuration
    @Profile(YT_ACTIVE)
    public static class YtIntegrationConfig extends YtConfig {

        public YtIntegrationConfig(YtProperties ytProperties) {
            super(ytProperties);
        }
    }

    @Configuration
    @Profile("!" + OFFER_STORAGE_ACTIVE)
    public static class OfferStorageMockedConfig {

        @Bean
        public SaasSearchService mockedSaasSearchService() {
            return Mockito.mock(SaasSearchService.class, withSettings()
                    .defaultAnswer(RETURNS_MOCKS));
        }

        @Bean("offerStorageSaasClient")
        public OfferStorageSaasClient mockedStorageSaasClient(SaasSearchService saasSearchService) {
            return Mockito.mock(OfferStorageSaasClient.class, withSettings()
                    .useConstructor(saasSearchService)
                    .defaultAnswer(RETURNS_MOCKS));
        }

        @Bean
        @OfferStorage
        public HttpClientBuilder mockedHttpClientBuilder() {
            HttpClientBuilder b = Mockito.mock(HttpClientBuilder.class);
            CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
            when(b.build()).thenReturn(httpClientMock);
            return b;
        }

        @Bean("offerStorageStrollerClient")
        public OfferStorageStrollerClient mockedStorageStrollerClient(
                OfferStorageProperties offerStorageProperties,
                @OfferStorage HttpClientBuilder clientBuilder,
                OfferDataConverter offerDataConverter
        ) {
            return Mockito.mock(OfferStorageStrollerClient.class, withSettings()
                    .useConstructor(
                            offerStorageProperties,
                            clientBuilder,
                            clientBuilder,
                            offerDataConverter
                    )
                    .defaultAnswer(RETURNS_MOCKS));
        }
    }

    @Configuration
    @Profile(OFFER_STORAGE_ACTIVE)
    public static class OfferStorageIntegrationConfig extends OfferStorageConfig {

        public OfferStorageIntegrationConfig(OfferStorageProperties offerStorageProperties) {
            super(offerStorageProperties);
        }

        @Bean
        @OfferStorage
        public HttpClientBuilder mockedHttpClientBuilder() {
            return httpClientBuilder();
        }
    }

    @Configuration
    @Profile("!" + S3_ACTIVE)
    public static class S3ConfigMockedConfig extends S3Config {

        public S3ConfigMockedConfig(S3Properties s3Properties) {
            super(s3Properties);
        }

        @Bean("s3Client")
        public AmazonS3 mockedAmazonS3() throws MalformedURLException {
            AmazonS3 s3Client = Mockito.mock(AmazonS3.class);
            when(s3Client.getUrl(any(), any())).thenReturn(new URL("http://localhost/"));
            when(s3Client.putObject(any(), any(), any(), any(ObjectMetadata.class))).thenReturn(new PutObjectResult());
            return s3Client;
        }
    }

    @Configuration
    @Profile(S3_ACTIVE)
    public static class S3ConfigIntegrationConfig extends S3Config {

        public S3ConfigIntegrationConfig(S3Properties s3Properties) {
            super(s3Properties);
        }
    }

    @Configuration
    @Profile("!" + LOGBROKER_ACTIVE)
    public static class LogbrokerMockedConfig {

        private static final ThreadLocal<Queue<OfferLogbrokerEvent>> MOCKED_LOGBROKER_QUEUE = ThreadLocal.withInitial(
                LinkedBlockingDeque::new);

        @Bean
        @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.INTERFACES)
        public Queue<OfferLogbrokerEvent> mockedLogbrokerQueue() {
            return MOCKED_LOGBROKER_QUEUE.get();
        }

        @Bean
        public LogbrokerService logbrokerService() {
            return new LogbrokerService() {
                @Override
                public void close() throws Exception {
                    MOCKED_LOGBROKER_QUEUE.get().clear();
                }

                @Override
                @SuppressWarnings("rawtypes")
                public CompletableFuture<LogbrokerEvent> publishEventAsync(@NotNull LogbrokerEvent event) {
                    MOCKED_LOGBROKER_QUEUE.get().add((OfferLogbrokerEvent) event);
                    return CompletableFuture.completedFuture(event);
                }
            };
        }
    }

    @Configuration
    @Profile(LOGBROKER_ACTIVE)
    @Import(LogbrokerServiceCommonConfig.class)
    public static class LogbrokerActiveConfig {

        private final LogbrokerProperties logbrokerProperties;

        public LogbrokerActiveConfig(LogbrokerProperties logbrokerProperties) {
            this.logbrokerProperties = logbrokerProperties;
        }

        @Bean(destroyMethod = "close")
        public NativeTvmClient tvmClient(
                @Value("${market.marketpromo.ciface-promo.tvm.clientId}") int applicationPromoClientId,
                @Value("${market.marketpromo.ciface-promo.tvm.logbroker.clientId}") int logbrokerClientId,
                @Value("${client_secret}") String applicationPromoClientSecret
        ) {
            final HashMap<String, Integer> ids = new HashMap<>();
            for (YdbClientId id : YdbClientId.values()) {
                ids.put(id.name(), id.getId());
            }
            ids.put(TvmConfig.TvmSystemName.LOGBROKER, logbrokerClientId);

            try (TvmApiSettings apiSettings = TvmApiSettings.create()
                    .setSelfTvmId(applicationPromoClientId)
                    .enableServiceTicketChecking()
                    .enableServiceTicketsFetchOptions(applicationPromoClientSecret, ids)) {
                return NativeTvmClient.create(apiSettings);
            }
        }

        @Bean(destroyMethod = "shutdown")
        @Logbroker
        public ExecutorService logbrokerExecutor() {
            return CPIExecutorService.wrap(Executors.newCachedThreadPool(
                    getThreadFactory("grpc-logbroker-executor-%d", true)));
        }

        @Bean
        public LogbrokerMonitorExceptionsService logbrokerMonitorExceptionsService() {
            return new LogbrokerMonitorExceptionsServiceStub();
        }

        @Configuration
        public static class LogbrokerCredentialTestConfig extends LogbrokerCredentialProductionConfig {

        }

        @Configuration
        private static class AssortmentPropagationTopicTestConfig extends AssortmentPropagationTopicConfig {

            public AssortmentPropagationTopicTestConfig(LogbrokerAssortmentPropagationTopicProperties topicProperties) {
                super(topicProperties);
            }
        }
    }

}
