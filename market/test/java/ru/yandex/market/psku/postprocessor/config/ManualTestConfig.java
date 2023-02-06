package ru.yandex.market.psku.postprocessor.config;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.CloseableYt;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.ytbuilder.YtSyncBuilder;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.psku.postprocessor.clusterization.ClusterWriter;
import ru.yandex.market.psku.postprocessor.clusterization.yt.ToYtClusterTransformer;
import ru.yandex.market.psku.postprocessor.clusterization.yt.YtClusterWriter;
import ru.yandex.market.psku.postprocessor.clusterization.yt.YtMboModelTable;
import ru.yandex.market.psku.postprocessor.clusterization.yt.YtTable;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.SessionDao;
import ru.yandex.market.psku.postprocessor.msku_creation.YtPskuClusterDao;
import ru.yandex.market.psku.postprocessor.service.newvalue.NewValueAggregatorService;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;
import ru.yandex.market.psku.postprocessor.service.yt.YtNewValueService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtPskuSessionService;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

@Configuration
@Import({
        ClusterizationConfig.class,
        MskuCreationConfig.class,
})
public class ManualTestConfig {
    public final static String YT_USER = "insert username";

    private final static String YT_USER_TOKEN = "insert token here";
    private final static String YQL_TOKEN = "insert token here";
    private int modelStorageConnectionTimeoutMillis = 300000;
    private String defaultUserAgent = "psku-post-processor";
    private int defaultTriesBeforeFail=5;
    private int defaultSleepBetweenTries=200;
    private int defaultConnectionTimeoutMillis=300;

    private void initServiceClient(ServiceClient serviceClient) {
        serviceClient.setUserAgent(defaultUserAgent);
    }

    private void initServiceClient(ServiceClient serviceClient, String serviceHost) {
        serviceClient.setUserAgent(defaultUserAgent);
        serviceClient.setTriesBeforeFail(defaultTriesBeforeFail);
        serviceClient.setSleepBetweenTries(defaultSleepBetweenTries);
        serviceClient.setConnectionTimeoutMillis(defaultConnectionTimeoutMillis);
        serviceClient.setHost(serviceHost);
    }


    @Bean
    CategoryDataKnowledge categoryDataKnowledge(
        CategoryParametersService categoryParametersService,
        CategorySizeMeasureService categorySizeMeasureService,
        @Value("4") int categoryDataRefreshersCount
    ) {
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
        categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
        categoryDataKnowledge.setCategoryDataRefreshersCount(categoryDataRefreshersCount);
        categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
        return categoryDataKnowledge;
    }


    @Bean
    CategoryParametersService categoryParametersServiceClient(
        @Value("http://mbo-http-exporter.yandex.net:8084/categoryParameters/") String categoryParametersServiceHost
    ) {
        CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
        initServiceClient(categoryParametersService, categoryParametersServiceHost);
        return categoryParametersService;
    }

    @Bean
    CategorySizeMeasureService categorySizeMeasureServiceClient(
        @Value("http://mbo-http-exporter.yandex.net:8084/categorySizeMeasure/") String categorySizeMeasureServiceHost
    ) {
        CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();
        initServiceClient(categorySizeMeasureService, categorySizeMeasureServiceHost);
        return categorySizeMeasureService;
    }


    @Bean
    PskuKnowledgeDao pskuKnowledgeDao() {
        return Mockito.mock(PskuKnowledgeDao.class);
    }

    @Bean
    PskuResultStorageDao pskuResultStorageDao() {
        return Mockito.mock(PskuResultStorageDao.class);
    }

    @Bean(name = "model.storage.service.with.retry")
    ModelStorageService modelStorageService() {
        ModelStorageServiceStub result = new ModelStorageServiceStub();
        result.setHost("http://mbo-card-api.http.yandex.net:33714/modelStorage/");
        result.setConnectionTimeoutMillis(modelStorageConnectionTimeoutMillis);
        initServiceClient(result);
        return result;
    }

    @Bean
    ModelStorageHelper modelStorageHelper(
        @Qualifier("model.storage.service.with.retry") ModelStorageService modelStorageServiceWithRetry
    ) {
        return new ModelStorageHelper(modelStorageServiceWithRetry, null);
    }

    @Bean
    CategoryModelsService categoryModelsService() {
        return new CategoryModelsServiceMock();
    }


    @Bean
    YtTable pskuYtTable(
            @Value("//home/market/testing/ir/psku-post-processor/psku-sessions/recent/psku") String recentPath
    ) {
        return new YtMboModelTable(recentPath);
    }

    @Bean
    Yt hahnYtApi(
            @Value("hahn.yt.yandex.net") String ytHttpProxy,
            @Value(YT_USER_TOKEN) String ytToken
    ) {
        return createYtApi(ytHttpProxy, ytToken);
    }

    @Bean
    ClusterWriter ytClusterWriter(
            Yt hahnYtApi,
            @Value("//home/market/users/" + YT_USER + "/psku_clusterization/name_test") String destinationDirectoryPath,
            ToYtClusterTransformer toYtClusterTransformer
    ) {
        return new YtClusterWriter(hahnYtApi, destinationDirectoryPath, toYtClusterTransformer);
    }

    @Bean
    ExecutorService clusterizerExecutorService() {
        return Executors.newFixedThreadPool(16, new ThreadFactoryBuilder().setDaemon(false).build());
    }

    @Bean
    YtPskuClusterDao ytPskuClusterDao(
        Yt hahnYtApi,
        @Value("//home/market/testing/ir/psku-post-processor/psku-clusters") String destinationDirectoryPath
    ) {
        return new YtPskuClusterDao(hahnYtApi, destinationDirectoryPath);
    }

    @Bean
    public YtNewValueService ytNewValueService(
            @Qualifier("hahnYtApi") Yt yt,
            @Qualifier("yqlJdbcTemplate") JdbcTemplate yqlJdbcTemplate,
            @Value("market-testing-priority") String mapReducePool,
            @Value("//home/market/testing/ir/psku-post-processor/psku-sessions/recent/psku") String recentPskuPath,
            @Value("//home/market/production/mstat/dictionaries/mbo/size_measure/latest") String sizeMeasureParamsPath,
            @Value("//home/market/users/" + YT_USER + "/psku_post_processor/new_values") String ytNewValuesPath) {
        return new YtNewValueService(
                yt,
                yqlJdbcTemplate,
                mapReducePool,
                recentPskuPath,
                sizeMeasureParamsPath,
                ytNewValuesPath);
    }

    @Bean
    YtPskuSessionService ytPskuSessionService(
            @Qualifier("hahnYtApi") Yt hahnYtApi,
            @Value("//home/market/testing/ir/psku-post-processor/psku-sessions") String ytSessionPath
    ){
        return new YtPskuSessionService(
                hahnYtApi,
                Mockito.mock(SessionDao.class),
                ytSessionPath
        );
    }

    @Bean
    YtDataService ytDataService(
            @Qualifier("hahnYtApi") Yt yt,
            @Qualifier("yqlJdbcTemplate") JdbcTemplate yqlJdbcTemplate,
            @Value("market-testing-priority") String mapReducePool,
            @Value("//home/market/testing/ir/psku-post-processor/psku-sessions") String ytPskuSessionsPath,
            @Value("//home/market/testing/ir/psku-post-processor/enriched-psku-sessions") String ytEnrichedSessionsPath,
            @Value("//home/market/testing/ir/psku-post-processor/pmodel-deleter-sessions") String ytPModelDeleterSessionsPath,
            @Value("//home/market/testing/mbo/export/recent") String ytMboExportPath,
            @Value("//home/market/production/mstat/dictionaries") String mStatDictionaryPath,
            @Value("//home/market/testing/ir") String irYtBasePath,
            @Value("//home/market/testing/mbo/model-storage/mbo-models") String ytMboModelStoragePath,
            @Value("${skip.stock.threshold.days:60}") long skipStockThresholdDays,
            @Value("testing") String environment
    ) {
        YtDataService ytDataService = new YtDataService();
        ytDataService.setYt(yt);
        ytDataService.setYqlJdbcTemplate(yqlJdbcTemplate);
        ytDataService.setMapReducePool(mapReducePool);
        ytDataService.setYtPskuSessionsPath(ytPskuSessionsPath);
        ytDataService.setYtEnrichedSessionsPath(ytEnrichedSessionsPath);
        ytDataService.setYtPModelDeleterSessionsPath(ytPModelDeleterSessionsPath);
        ytDataService.setYtMboExportPath(ytMboExportPath);
        ytDataService.setYtMStatDictionaryPath(mStatDictionaryPath);
        ytDataService.setYtIrYtBasePath(irYtBasePath);
        ytDataService.setYtMboModelStoragePath(ytMboModelStoragePath);
        ytDataService.setEnvironment(environment);
        ytDataService.setSkipStockThresholdDays(skipStockThresholdDays);
        ytDataService.setSkippedSuppliers(Collections.emptySet());
        return ytDataService;
    }

    @Bean
    NewValueAggregatorService newValueAggregatorService(YtNewValueService ytNewValueService) {
        return new NewValueAggregatorService(ytNewValueService);
    }

    @Bean
    public DataSource yqlDataSource(
            @Value(YT_USER) String yqlUsername,
            @Value(YQL_TOKEN) String yqlToken,
            @Value("jdbc:yql://yql.yandex.net:443/hahn") String yqlUrl
    ) {
        YqlProperties properties = new YqlProperties();
        properties.setUser(yqlUsername);
        properties.setPassword(yqlToken);
        properties.setSyntaxVersion(1);
        return new YqlDataSource(yqlUrl, properties);
    }

    private CloseableYt createYtApi(String ytHttpProxy, String ytToken) {
        YtConfiguration.Builder configuration = YtConfiguration.builder()
                .withApiHost(ytHttpProxy)
                .withToken(ytToken);
        YtSyncBuilder ytBuilder = Yt.builder(configuration.build());
        return ytBuilder
                .http()
                .build();
    }

}

