package ru.yandex.market.rg.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.core.environment.ActiveParamService;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.supplier.config.SupplierFeedConfig;
import ru.yandex.market.core.feed.validation.model.result.XlsTemplateInfo;
import ru.yandex.market.core.feed.validation.result.FeedTemplateXlsService;
import ru.yandex.market.core.feed.validation.result.FeedXlsService;
import ru.yandex.market.core.order.returns.os.OrderServiceReturnDao;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.core.solomon.SolomonTestJvmConfig;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics4shops.client.api.InternalOrderApi;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.rg.asyncreport.statistics.supplier.dao.PartnerSalesStatisticsDao;
import ru.yandex.market.rg.asyncreport.statistics.supplier.xls.SalesStatisticExcelReportGeneratorService;
import ru.yandex.market.rg.client.orderservice.RgOrderServiceClient;
import ru.yandex.market.rg.client.yadoc.rr.YadocRRServiceClient;
import ru.yandex.market.rg.config.reports.unitedcatalog.MigrationGrpcTestConfig;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ParametersAreNonnullByDefault
@Configuration
@Import({
        TestDsmClientConfig.class,
        SupplierFeedConfig.class,
        SolomonTestJvmConfig.class,
        MigrationGrpcTestConfig.class,
        StatisticsReportConfigTest.class
})
public class FunctionalTestEnvironmentConfig {

    @Value("${servant.name}")
    private String moduleName;

    @Autowired
    private Handler mvcHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${mbi.clickhouse.jdbc.db:mbi}")
    private String clickHouseDb;

    public String getClickHouseJdbcUrl() {
        String port = Optional.ofNullable(System.getenv("RECIPE_CLICKHOUSE_HTTP_PORT")).orElse("8123");
        return "jdbc:clickhouse://localhost:" + port;
    }

    @Bean(name = "baseUrl")
    @DependsOn("httpServerInitializer")
    public String baseUrl() {
        Server jettyServer = mvcHandler.getServer();
        Connector[] connectors = jettyServer.getConnectors();
        NetworkConnector firstConnector = (NetworkConnector) connectors[0];
        int actualPort = firstConnector.getLocalPort();
        return "http://localhost:" + actualPort;
    }

    /**
     * Датасурс кликхауса для функицональных тестов, сам кликхаус поднимается с помощью рецепта указанного в ya make.
     */
    @Bean
    public ClickHouseDataSource clickHouseDataSource() {
        ClickHouseProperties properties = new ClickHouseProperties();
        properties.setDatabase(clickHouseDb);
        return new ClickHouseDataSource(getClickHouseJdbcUrl(), properties);
    }

    /**
     * выключаем лишние фоновые таски
     */
    @Bean(name = {
            "mockScheduledExecutorService",
            "returnToPendingReportsExecutorExecutor",
            "defaultReportWorkerExecutorService",
            "majorReportWorkerExecutorService",
            "migrationReportWorkerExecutorService",
            "assortmentReportWorkerExecutorService",
            "reportMetricsWorkerExecutor",
            "nettingReportWorkerExecutorService",
            "nonDatabaseReportWorkerExecutorService",
            "marketplaceServicesReportWorkerExecutorService",
            "yaDocRRServiceReportWorkerExecutorService"
    })
    public ScheduledExecutorService mockScheduledExecutorService() {
        return mock(ScheduledExecutorService.class);
    }

    @Bean
    public DataCampClient dataCampShopClient() {
        return mock(DataCampClient.class);
    }

    @Bean
    public DataCampClient dataCampMigrationClient() {
        return mock(DataCampClient.class);
    }

    @Bean
    public SaasService saasDatacampService() {
        return mock(SaasService.class);
    }

    @Bean
    @Primary
    public FeedXlsService<OfferInfo> unitedFeedTemplateXlsService(
            @Qualifier("unitedXlsTemplateInfoMap") Map<MarketTemplate, XlsTemplateInfo> xlsTemplateInfoMap
    ) {
        return spy(new FeedTemplateXlsService(xlsTemplateInfoMap));
    }

    @Bean
    @Primary
    public SupplierXlsHelper unitedSupplierXlsHelper() {
        ClassPathResource resource = new ClassPathResource("supplier/feed/marketplace-catalog-standard.xlsx");
        return spy(new SupplierXlsHelper(resource, ".xlsx"));
    }

    @Bean
    public SupplierXlsHelper priceSupplierXlsHelper() {
        ClassPathResource resource = new ClassPathResource("united/feed/marketplace-prices.xlsm");
        return spy(new SupplierXlsHelper(resource, ".xlsm"));
    }

    @Bean
    public Tvm2 mbiTvm() {
        return mock(Tvm2.class);
    }

    @Bean
    public Tvm2 migrationTvm() {
        return mock(Tvm2.class);
    }

    @Bean
    public TvmClient ticketParserTvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    public TvmClient migrationTicketParserTvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    public SalesStatisticExcelReportGeneratorService salesStatisticExcelReportGeneratorService(
            PartnerSalesStatisticsDao partnerSalesStatisticsDao
    ) {
        return new SalesStatisticExcelReportGeneratorService(partnerSalesStatisticsDao, "default", 5);
    }

    @Bean
    public WwClient wwClient() {
        return mock(WwClient.class);
    }

    @Bean
    public PilotSupplierYtDao pilotSupplierYtDao() {
        return mock(PilotSupplierYtDao.class);
    }

    @Bean("salesDynamicsYt")
    public Yt salesDynamicsYt() {
        return mock(Yt.class);
    }

    @Bean
    public Supplier<Boolean> useSearchTablesSupplier(
            UnitedCatalogEnvironmentService unitedCatalogEnvironmentService
    ) {
        return () -> unitedCatalogEnvironmentService.getSearchTablesFlag(moduleName);
    }

    @Bean
    public NamedParameterJdbcTemplate autoClusterChytJdbcTemplate() {
        return mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public LMSClient lmsClient() {
        return mock(LMSClient.class);
    }

    @Bean(name = "importedStocksActiveParamService")
    public ActiveParamService importedStocksActiveParamService() {
        return new ActiveParamService(
                jdbcTemplate,
                "shops_web.imported_stocks",
                "shops_web.imported_stocks_alt",
                "shops_web.imported_stocks_pointer"
        );
    }

    @Bean
    public OrderServiceReturnDao orderServiceReturnDao() {
        return mock(OrderServiceReturnDao.class);
    }

    @Bean
    public RgOrderServiceClient rgOrderServiceClient() {
        return mock(RgOrderServiceClient.class);
    }

    @Bean
    public InternalOrderApi internalOrderApi() {
        return mock(InternalOrderApi.class);
    }

    @Bean
    public YadocRRServiceClient yadocRRServiceClient() {
        return mock(YadocRRServiceClient.class);
    }


    @Bean("contentLoyaltyResource")
    public Resource contentLoyaltyResource() {
        return new ClassPathResource("supplier/feed/card_rating_loyalty.xlsx");
    }

    @Bean
    public PersonalMarketService personalMarketService() {
        return mock(PersonalMarketService.class);
    }

    @Bean
    public StockStorageSearchClient stockStorageSearchClient() {
        return Mockito.mock(StockStorageSearchClient.class);
    }
}
