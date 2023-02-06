package ru.yandex.direct.grid.processing.configuration;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.github.shyiko.mysql.binlog.GtidSet;
import com.yandex.ydb.core.rpc.RpcTransport;
import com.yandex.ydb.table.SessionRetryContext;
import com.yandex.ydb.table.TableClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.jooq.Select;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.canvas.client.CanvasClient;
import ru.yandex.direct.canvas.client.CanvasClientConfiguration;
import ru.yandex.direct.canvas.client.model.video.Preset;
import ru.yandex.direct.canvas.client.model.video.PresetResponse;
import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.communication.config.CommunicationConfiguration;
import ru.yandex.direct.core.entity.adgroup.generation.AdGroupKeywordRecommendationService;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.container.FeatureRequestFactory;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.mobileapp.service.MobileAppService;
import ru.yandex.direct.core.entity.mobilegoals.repository.MobileGoalsStatisticRepository;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.configuration.UacYdbTestingConfiguration;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dialogs.client.DialogsClient;
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingConfiguration;
import ru.yandex.direct.grid.core.entity.sync.repository.MysqlStateRepository;
import ru.yandex.direct.grid.core.entity.sync.service.MysqlStateService;
import ru.yandex.direct.grid.core.frontdb.repository.JsonSettingsRepository;
import ru.yandex.direct.grid.core.util.yt.YtClusterFreshnessLoader;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.core.util.yt.YtSyncStatesLoader;
import ru.yandex.direct.grid.processing.annotations.GridGraphQLService;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.cache.storage.GridCacheStorage;
import ru.yandex.direct.grid.processing.service.cache.storage.GuavaGridCacheStorage;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.goal.GoalConversionsCacheService;
import ru.yandex.direct.grid.processing.service.region.RegionDataService;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.turbolandings.client.TurboLandingsClient;
import ru.yandex.direct.utils.Counter;
import ru.yandex.direct.ydb.YdbPath;
import ru.yandex.direct.ydb.client.YdbClient;
import ru.yandex.direct.ydb.client.YdbSessionProperties;
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfo;
import ru.yandex.direct.ydb.testutils.ydbinfo.YdbInfoFactory;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.service.OfferStatDynContextProvider;
import ru.yandex.direct.ytcore.entity.statistics.repository.ConversionStatisticsRepository;
import ru.yandex.direct.ytcore.spring.YtCoreConfiguration;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicConfig;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_CLIENT_BEAN;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_INFO_BEAN;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_PATH_BEAN;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_RPC_TRANSPORT_BEAN;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_SESSION_PROPERTIES_BEAN;
import static ru.yandex.direct.common.configuration.FrontDbYdbConfiguration.FRONTDB_YDB_TABLE_CLIENT_BEAN;
import static ru.yandex.direct.common.configuration.RedisConfiguration.LETTUCE;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.PUBLIC_GRAPH_QL_PROCESSOR;

@Configuration
@Import({GridProcessingConfiguration.class, CoreTestingConfiguration.class, YtCoreConfiguration.class,
        ExcelProcessingConfiguration.class, CommunicationConfiguration.class, UacYdbTestingConfiguration.class})
public class GridProcessingTestingConfiguration {

    @MockBean
    public YtClusterFreshnessLoader ytClusterFreshnessLoader;

    @MockBean
    public YtSyncStatesLoader ytSyncStatesLoader;

    @MockBean
    public AdGroupKeywordRecommendationService keywordGenerationService;

    @SpyBean
    public YtDynamicSupport ytDynamicSupport;

    @Lazy
    @Bean
    public YtDynamicSupport ytDynamicSupport(ShardHelper shardHelper,
                                             YtProvider ytProvider,
                                             DirectYtDynamicConfig dynamicConfig,
                                             YtClusterFreshnessLoader freshnessLoader,
                                             YtDynamicConfig ytDynamicConfig) {
        return new YtDynamicSupport(shardHelper, ytProvider, dynamicConfig, freshnessLoader, ytDynamicConfig) {
            @Override
            public UnversionedRowset selectRows(Select query) {
                return mock(UnversionedRowset.class);
            }

            @Override
            public UnversionedRowset selectRows(int shard, Select query) {
                return mock(UnversionedRowset.class);
            }

            @Override
            public UnversionedRowset selectRows(int shard, Select query, boolean withTotalStats) {
                return mock(UnversionedRowset.class);
            }
        };
    }

    @SpyBean
    public OfferStatDynContextProvider offerStatDynContextProvider;

    @Lazy
    @Bean
    public Collection<Object> gridGraphQLServices(ApplicationContext applicationContext) {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(GridGraphQLService.class);
        return beans.values();
    }

    @Bean
    public GridCacheStorage gridCacheStorage() {
        return new GuavaGridCacheStorage();
    }

    @Bean
    public CustomScopeConfigurer customScopeConfigurer() {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        Map<String, Object> scopes = Collections.singletonMap(WebApplicationContext.SCOPE_REQUEST,
                new SimpleThreadScope());
        configurer.setScopes(scopes);
        return configurer;
    }

    @SpyBean
    public MysqlStateService mysqlStateService;

    /**
     * Мок для MysqlStateService, чтобы генерировать GTID для тестов, т.к. для тестового mysql отключен GTID mode
     * При каждом вызове {@link MysqlStateService#getCurrentServerGtidSet(String)} инкрементиться GTID
     */
    @Bean
    public MysqlStateService mysqlStateService(MysqlStateRepository mysqlStateRepository, ShardHelper shardHelper) {
        return new MysqlStateService(mysqlStateRepository, shardHelper) {
            String serverUuid = UUID.randomUUID().toString();
            Counter counter = new Counter(1);
            GtidSet gtidSet = new GtidSet(serverUuid + ":1-1");

            @Override
            public GtidSet.UUIDSet getCurrentServerGtidSet(String login) {
                gtidSet.add(serverUuid + ":" + counter.next());
                return gtidSet.getUUIDSet(serverUuid);
            }

            @Override
            public GtidSet.UUIDSet getCurrentServerGtidSet(ClientId clientId) {
                gtidSet.add(serverUuid + ":" + counter.next());
                return gtidSet.getUUIDSet(serverUuid);
            }

            @Override
            public GtidSet.UUIDSet getCurrentServerGtidSet(int shard) {
                gtidSet.add(serverUuid + ":" + counter.next());
                return gtidSet.getUUIDSet(serverUuid);
            }
        };
    }

    @Bean
    public GraphQlTestExecutor graphQlTestExecutor(@Qualifier(GRAPH_QL_PROCESSOR) GridGraphQLProcessor processor) {
        return new GraphQlTestExecutor(processor);
    }

    @Bean
    public KtGraphQLTestExecutor ktGraphQLTestExecutor(@Qualifier(GRAPH_QL_PROCESSOR) GridGraphQLProcessor processor,
                                                       @Qualifier(PUBLIC_GRAPH_QL_PROCESSOR) GridGraphQLProcessor publicProcessor,
                                                       GridContextProvider gridContextProvider) {
        return new KtGraphQLTestExecutor(processor, publicProcessor, gridContextProvider);
    }

    @MockBean
    public BalanceService balanceService;

    @MockBean
    public InventoriClient inventoriClient;

    @Bean(LETTUCE)
    public LettuceConnectionProvider lettuceConnectionProvider() {
        return new LettuceConnectionProvider("ppc", mock(RedisClusterClient.class), 3) {
            @Override
            public <T> T call(String name, Function<RedisAdvancedClusterCommands<String, String>, T> function) {
                @SuppressWarnings("unchecked")
                T res = (T) function.apply(mock(RedisAdvancedClusterCommands.class));
                return res;
            }
        };
    }

    @SpyBean
    public MobileAppService mobileAppService;

    @SpyBean
    public MetrikaGoalsService metrikaGoalsService;

    @SpyBean
    public CampaignGoalsService campaignGoalsService;

    @MockBean
    public MetrikaSegmentService metrikaSegmentService;

    @MockBean
    public DialogsClient dialogsClient;

    @MockBean
    public RegionDataService regionDataService;

    @Lazy
    @MockBean
    public NetAcl netAcl;

    @Bean
    public FeatureRequestFactory featureRequestFactory() {
        return (clientId, uid) -> new FeatureRequest().withClientId(clientId).withUid(uid);
    }

    @MockBean
    public TurboLandingsClient turboLandingsClient;

    @Bean(FRONTDB_YDB_INFO_BEAN)
    public YdbInfo ydbInfo() {
        return YdbInfoFactory.getExecutor();
    }

    @Bean(FRONTDB_YDB_PATH_BEAN)
    public YdbPath ydbPath(@Qualifier(FRONTDB_YDB_INFO_BEAN) YdbInfo ydbInfo) {
        return ydbInfo.getDb();
    }

    @Bean(FRONTDB_YDB_TABLE_CLIENT_BEAN)
    public TableClient tableClient(@Qualifier(FRONTDB_YDB_INFO_BEAN) YdbInfo ydbInfo) {
        return ydbInfo.getClient();
    }

    @MockBean(name = FRONTDB_YDB_RPC_TRANSPORT_BEAN)
    public RpcTransport grpcTransport;

    @Bean(FRONTDB_YDB_SESSION_PROPERTIES_BEAN)
    public YdbSessionProperties ydbProperties() {
        return YdbSessionProperties.builder().build();
    }

    @Bean(FRONTDB_YDB_CLIENT_BEAN)
    public YdbClient ydbClient(
            @Qualifier(FRONTDB_YDB_TABLE_CLIENT_BEAN) TableClient tableClient,
            @Qualifier(FRONTDB_YDB_SESSION_PROPERTIES_BEAN) YdbSessionProperties hourglassYdbProperties) {
        var sessionRetryContext = SessionRetryContext.create(tableClient)
                .maxRetries(hourglassYdbProperties.getMaxQueryRetries())
                .retryNotFound(hourglassYdbProperties.isRetryNotFound())
                .build();

        return new YdbClient(sessionRetryContext, Duration.ofMinutes(1));
    }

    @SpyBean
    public CanvasClient canvasClient;

    @Bean
    public CanvasClient canvasClient() {
        PresetResponse simpleResponce = new PresetResponse()
                .withCanvasPresets(emptyList())
                .withVideoPresets(List.of(new Preset().withPresetName("name").withPresetId(7L)))
                .withHtml5Presets(List.of(new Preset().withPresetName("1024x768").withPresetId(12L)));

        return new CanvasClient(mock(CanvasClientConfiguration.class), mock(ParallelFetcherFactory.class)) {
            @Override
            public PresetResponse getCreativeTemplates() {
                return simpleResponce;
            }
        };
    }

    @MockBean
    public JsonSettingsRepository jsonSettingsRepository;

    @MockBean
    public NotificationService notificationService;

    @MockBean
    public MobileGoalsStatisticRepository mobileGoalsStatisticRepository;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    public ConversionStatisticsRepository conversionStatisticsRepository;

    @MockBean
    public GoalConversionsCacheService goalConversionsCacheService;
}
