package ru.yandex.market.vendor;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.bolts.collection.Option;
import ru.yandex.clickhouse.BalancedClickhouseDataSource;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseExternalData;
import ru.yandex.clickhouse.ClickHouseStatement;
import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.cs.billing.api.client.CsBillingApiRetrofitClient;
import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.DefaultBalanceService;
import ru.yandex.cs.billing.billing.BillingServiceSql;
import ru.yandex.cs.billing.history.impl.HistoryServiceSql;
import ru.yandex.cs.placement.tms.analytics.AnalyticsUploadService;
import ru.yandex.cs.placement.tms.contacts.service.CrmVendorContactUploadService;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.VendorDbUnitTestConfigH2AndPg;
import ru.yandex.market.core.config.MemCachedTestConfig;
import ru.yandex.market.ir.http.AutoGenerationService;
import ru.yandex.market.ir.http.MatcherService;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.security.AuthoritiesLoader;
import ru.yandex.market.security.checker.StaticDomainAuthorityChecker;
import ru.yandex.market.security.core.SimpleAuthoritiesLoader;
import ru.yandex.market.security.core.SimpleKampferFactory;
import ru.yandex.vendor.brand.Brand;
import ru.yandex.vendor.brand.BrandInfoService;
import ru.yandex.vendor.brand.DbBrandInfoLoader;
import ru.yandex.vendor.brand.MemoryBrandInfoService;
import ru.yandex.vendor.cataloger.CatalogerClient;
import ru.yandex.vendor.category.CategoryService;
import ru.yandex.vendor.content.CategoryInfoService;
import ru.yandex.vendor.csbilling.HttpCsBillingApiClient;
import ru.yandex.vendor.csbilling.HttpCsBillingApiService;
import ru.yandex.vendor.documents.DumpFileService;
import ru.yandex.vendor.documents.IFileStorage;
import ru.yandex.vendor.documents.S3Connection;
import ru.yandex.vendor.documents.S3FileStorage;
import ru.yandex.vendor.housekeeping.DbSystemErrorLogger;
import ru.yandex.vendor.mock.AutoGenerationServiceResolver;
import ru.yandex.vendor.modelbids.bidding.RestMbiBiddingClient;
import ru.yandex.vendor.modeleditor.DbModelEditorService;
import ru.yandex.vendor.modeleditor.ir.AgApiClient;
import ru.yandex.vendor.modeleditor.mbo.RestMboHttpExporterClient;
import ru.yandex.vendor.notification.VendorNotificationParameterFormatter;
import ru.yandex.vendor.notification.email_sender.EmailSenderService;
import ru.yandex.vendor.opinions.pers.PersGradeClient;
import ru.yandex.vendor.questions.dao.QuestionsNotificationDao;
import ru.yandex.vendor.report.ReportClient;
import ru.yandex.vendor.security.IVendorSecurityService;
import ru.yandex.vendor.security.VendorSecurityService;
import ru.yandex.vendor.staff.CachedStaffClient;
import ru.yandex.vendor.staff.HttpStaffClient;
import ru.yandex.vendor.staff.StaffUserResponseParser;
import ru.yandex.vendor.staff.service.StaffService;
import ru.yandex.vendor.stats.ClickHouseStatsUtils;
import ru.yandex.vendor.ugc.UgcDaemonClient;
import ru.yandex.vendor.util.FileUploadService;
import ru.yandex.vendor.util.NettyRestClient;
import ru.yandex.vendor.util.Substr;
import ru.yandex.vendor.util.jdbc.TempTableService;
import ru.yandex.vendor.vendors.CampaignProductInfoRetriever;
import ru.yandex.vendor.vendors.VendorServiceSql;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Import({
        VendorDbUnitTestConfigH2AndPg.class,
        MemCachedTestConfig.class,
})
@Configuration
public class CsPlacementTmsFunctionalTestConfig {

    @Value("${vendors.report.url}")
    private String vendorsReportUrl;

    @Value("${mbo.http-exporter.url}")
    private String mboHttpExporterUrl;

    @Value("${vendors.s3.bucket}")
    private String vendorsS3Bucket;

    @Value("${vendors.s3.root.folder}")
    private String s3RootFolder;

    @Value("${vendors.supercheck.url}")
    private String vendorsSupercheckUrl;

    @Value("${market.bidding.url}")
    private String mbiBinddingUrl;

    private static WireMockServer newWireMockServer() {
        return new WireMockServer(new WireMockConfiguration().dynamicPort().notifier(new ConsoleNotifier(true)));
    }

    @Bean
    @Profile("functionalTest")
    public NamedParameterJdbcTemplate priceLabsYtNamedParameterJdbcTemplate() {
        return mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public ClickHouseStatement clickHouseStatement(DataSource csBillingDataSource) {
        ClickHouseStatement clickHouseStatement = spy(ClickHouseStatement.class);

        try {
            doAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                List<ClickHouseExternalData> externalDatas = invocation.getArgument(2);
                for (ClickHouseExternalData externalData : externalDatas) {
                    String name = externalData.getName();
                    String value = ClickHouseStatsUtils.getInlinedParameterValues(externalData.getContent());
                    sql = sql.replaceAll(String.format("\\(?%s\\)?", name), "(" + value + ")");
                }

                return csBillingDataSource.getConnection().createStatement().executeQuery(sql);
            }).when(clickHouseStatement).executeQuery(anyString(), any(), any());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clickHouseStatement;
    }

    @Bean
    public ClickHouseConnection clickHouseConnection(ClickHouseStatement clickHouseStatement) {
        ClickHouseConnection clickHouseConnection = mock(ClickHouseConnection.class);
        try {
            when(clickHouseConnection.createStatement()).thenReturn(clickHouseStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clickHouseConnection;
    }

    @Bean
    public DataSource balancedClickhouseDataSource(ClickHouseConnection clickHouseConnection) {
        BalancedClickhouseDataSource mock = mock(BalancedClickhouseDataSource.class);
        try {
            when(mock.getConnection()).thenReturn(clickHouseConnection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mock;
    }

    @Bean
    public CommandExecutor commandExecutor() {
        return mock(CommandExecutor.class);
    }

    @Bean
    public S3Connection s3Connection() {
        return mock(S3Connection.class);
    }

    @Bean
    public AuthoritiesLoader authoritiesLoader(DataSource dataSource) {
        SimpleAuthoritiesLoader simpleAuthoritiesLoader = new SimpleAuthoritiesLoader();
        simpleAuthoritiesLoader.setKampferFactory(new SimpleKampferFactory(dataSource));

        return simpleAuthoritiesLoader;
    }

    @Bean
    public StaticDomainAuthorityChecker staticDomainAuthorityChecker() {
        return mock(StaticDomainAuthorityChecker.class);
    }

    @Bean(name = "httpExporterRestClient")
    public NettyRestClient httpExporterRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(mboHttpExporterUrl);
        config.setTargetModule(Module.MBO_HTTP_EXPORTER);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "mboHttpExporterClientCategoryService")
    public CategoryService mboHttpExporterClientCategoryService(
            NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate) {
        CategoryService categoryService = new CategoryService();
        categoryService.setNamedParameterJdbcTemplate(vendorNamedParameterJdbcTemplate);
        return categoryService;
    }

    @Bean(name = "mboHttpExporterClient")
    public RestMboHttpExporterClient mboHttpExporterClient(
            NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate,
            NettyRestClient httpExporterRestClient,
            CategoryService mboHttpExporterClientCategoryService) {
        RestMboHttpExporterClient httpExporterClient = new RestMboHttpExporterClient();
        httpExporterClient.setRestClient(httpExporterRestClient);
        httpExporterClient.setCategoryService(mboHttpExporterClientCategoryService);
        httpExporterClient.setNamedParameterJdbcTemplate(vendorNamedParameterJdbcTemplate);
        return httpExporterClient;
    }

    @Bean(name = "agApiClient")
    public AgApiClient agApiClient() {
        return mock(AgApiClient.class);
    }

    @Bean(name = "modelEditorRestClient")
    public NettyRestClient modelEditorRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(vendorsReportUrl);
        config.setTargetModule(Module.AUTOGENERATION_API);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "mbiBiddingRestClient")
    public NettyRestClient mbiBiddingRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(mbiBinddingUrl);
        config.setTargetModule(Module.MBI_BIDDING);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "reportMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer reportMock() {
        return newWireMockServer();
    }

    @Bean(name = "pricelabsMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer pricelabsMock() {
        return newWireMockServer();
    }

    @Bean(name = "mbiBiddingMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer mbiBiddingMock() {
        return newWireMockServer();
    }

    @Bean(name = "mbiBiddingClient")
    public RestMbiBiddingClient biddingClient(
            @Autowired @Qualifier("mbiBiddingRestClient") NettyRestClient mbiBiddingRestClient) {
        RestMbiBiddingClient biddingClient = new RestMbiBiddingClient("username", "password");
        biddingClient.setRestClient(mbiBiddingRestClient);
        biddingClient.setQuickRestClient(mbiBiddingRestClient);
        return biddingClient;
    }

    @Bean(name = "emailSenderMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer emailSenderMock() {
        return newWireMockServer();
    }

    @Bean(name = "categoryInfoService")
    public CategoryInfoService categoryInfoService(
            @Autowired @Qualifier("vendorNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
            @Autowired ReportClient reportClient,
            @Autowired @Qualifier("vendorServiceSql") VendorServiceSql vendorServiceSql) {
        CategoryInfoService categoryInfoService = spy(new CategoryInfoService());
        categoryInfoService.setNamedParameterJdbcTemplate(jdbcTemplate);
        categoryInfoService.setReportClient(reportClient);
        categoryInfoService.setVendorServiceSql(vendorServiceSql);
        return categoryInfoService;
    }

    @Bean(name = "partnerContentService")
    public PartnerContentService partnerContentService() {
        return mock(PartnerContentService.class);
    }

    @Bean(name = "autoGenService")
    public AutoGenerationService autoGenService() {
        return mock(AutoGenerationService.class);
    }

    @Bean(name = "autoGenServiceResolver")
    public AutoGenerationServiceResolver autoGenServiceResolver() {
        return mock(AutoGenerationServiceResolver.class);
    }

    @Bean(name = "persQaRestClient")
    public NettyRestClient persQaRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl("");
        config.setTargetModule(Module.PERS_QA);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "persGradeClient")
    public PersGradeClient persGradeClient() {
        return mock(PersGradeClient.class);
    }

    @Bean(name = "ugcDaemonClient")
    public UgcDaemonClient ugcDaemonClient() {
        return mock(UgcDaemonClient.class);
    }

    @Bean(name = "blackboxMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer blackboxMock() {
        return newWireMockServer();
    }

    @Bean(name = "brandInfoService")
    public BrandInfoService brandInfoService(DbBrandInfoLoader brandInfoLoader,
                                             NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate) {
        return new MemoryBrandInfoService(brandInfoLoader, vendorNamedParameterJdbcTemplate) {
            // forced refreshing brands cache from the database on every method call

            @Override
            public void init() {
                //do nothing
            }

            @Override
            public List<Brand> findBrandsByName(@Nonnull Substr pattern) {
                invokeRefreshBrands();
                return super.findBrandsByName(pattern);
            }

            @Override
            public Optional<Brand> brandById(@Nonnull Long brandId) {
                invokeRefreshBrands();
                return super.brandById(brandId);
            }

            private void invokeRefreshBrands() {
                try {
                    Method refreshBrandsMethod = MemoryBrandInfoService.class.getDeclaredMethod("refreshBrands");
                    refreshBrandsMethod.setAccessible(true);
                    refreshBrandsMethod.invoke(this);
                } catch (Exception e) {
                    fail(e);
                }
            }
        };
    }

    @Bean
    public DbSystemErrorLogger dbSystemErrorLogger() {
        return mock(DbSystemErrorLogger.class);
    }

    @Bean(name = "csBillingApiRestClient")
    public NettyRestClient csBillingApiRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl("");
        config.setTargetModule(Module.CS_BILLING);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "csBillingApiClient")
    public HttpCsBillingApiClient csBillingApiClient() {
        return mock(HttpCsBillingApiClient.class);
    }

    @Bean(name = "csBillingApiService")
    public HttpCsBillingApiService csBillingApiService(CsBillingApiRetrofitClient csBillingApiRetrofitClient,
                                                       HttpCsBillingApiClient csBillingApiClient,
                                                       CampaignProductInfoRetriever campaignProductInfoRetriever,
                                                       VendorServiceSql vendorServiceSql) {
        return spy(new HttpCsBillingApiService(csBillingApiRetrofitClient, csBillingApiClient,
                campaignProductInfoRetriever, vendorServiceSql));
    }

    @Bean(name = "modelbidsReportClientCategoryService")
    public CategoryService modelbidsReportClientCategoryService(@Autowired @Qualifier(
            "vendorNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        CategoryService categoryService = new CategoryService();
        categoryService.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
        return categoryService;
    }

    @Bean(name = "advIncutMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer advIncutMock() {
        return newWireMockServer();
    }

    @Bean(name = "staffMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer staffMock() {
        return newWireMockServer();
    }

    @Bean(name = "abcMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer abcMock() {
        return newWireMockServer();
    }

    @Bean(name = "csBillingApiMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer csBillingApiMock() {
        return newWireMockServer();
    }

    @Bean(name = "staffClient")
    public HttpStaffClient staffClient(
            @Autowired @Qualifier("staffRestClient") NettyRestClient staffRestClient,
            StaffUserResponseParser staffUserResponseParser) {

        return spy(new HttpStaffClient(staffRestClient, staffUserResponseParser, "staff-token"));
    }

    @Bean
    public IVendorSecurityService vendorSecurityService() {
        return mock(VendorSecurityService.class);
    }

    @Bean
    public StaffUserResponseParser staffUserResponseParser() {
        IFileStorage fileStorage = mock(IFileStorage.class);

        when(fileStorage.isPublicUrlSupported()).thenReturn(true);
        when(fileStorage.createPublicUrl(any())).thenReturn(Optional.empty());

        return spy(new StaffUserResponseParser(fileStorage));
    }

    @Bean(name = "cachedStaffClient")
    public CachedStaffClient cachedStaffClient(
            @Autowired @Qualifier("staffClient") HttpStaffClient staffClient) {
        CachedStaffClient cachedStaffClient = spy(new CachedStaffClient());
        cachedStaffClient.setStaffClient(staffClient);
        doAnswer(invocation -> {
            String login = invocation.getArgument(0);
            return staffClient.findUserByStaffLogin(login);
        }).when(cachedStaffClient).findUserByStaffLogin(any());
        return cachedStaffClient;
    }

    @Bean
    public S3FileStorage s3PublicFileStorage(S3Connection connection) {
        return spy(
                new S3FileStorage(
                        connection,
                        vendorsS3Bucket,
                        s3RootFolder,
                        true
                )
        );
    }

    @Bean
    public FileUploadService fileUploadService() {
        return spy(new FileUploadService());
    }

    @Bean
    public MatcherService matcherService() {
        return mock(MatcherService.class);
    }

    @Bean
    public DbModelEditorService dbModelEditorService() {
        return mock(DbModelEditorService.class);
    }

    @Bean
    public VendorNotificationParameterFormatter vendorNotificationParameterFormatter(Clock clock) {
        return spy(new VendorNotificationParameterFormatter(clock));
    }

    @Bean
    public BalanceService balanceService() {
        return mock(DefaultBalanceService.class);
    }

    @Bean
    public EmailSenderService emailSenderService() {
        return mock(EmailSenderService.class);
    }

    @Bean
    public CatalogerClient catalogerClient() {
        return mock(CatalogerClient.class);
    }

    @Bean
    @Profile("functionalTest")
    public QuestionsNotificationDao questionsNotificationDao() {
        return mock(QuestionsNotificationDao.class);
    }

    @Bean
    public DumpFileService privateDumpFileService() {
        return mock(DumpFileService.class);
    }

    @Bean
    public TempTableService vendorsTempTableService(JdbcTemplate csBillingJdbcTemplate) throws SQLException {
        TempTableService tempTableService = new TempTableService(csBillingJdbcTemplate);
        return spy(tempTableService);
    }

    @Bean
    public AnalyticsUploadService analyticsCutoffUploadService() {
        return mock(AnalyticsUploadService.class);
    }

    @Bean
    public AnalyticsUploadService analyticsOfferBillingUploadService() {
        return mock(AnalyticsUploadService.class);
    }

    @Bean
    public BillingServiceSql billingServiceSql(NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate,
                                               SimpleJdbcCall updateActualBalanceJdbcCall,
                                               SimpleJdbcCall getAvgSpending,
                                               Clock clock) {
        return spy(new BillingServiceSql(csBillingNamedParameterJdbcTemplate, updateActualBalanceJdbcCall,
                getAvgSpending, clock));
    }

    @Bean
    public HistoryServiceSql historySql(JdbcTemplate csBillingJdbcTemplate) {
        return spy(new HistoryServiceSql(csBillingJdbcTemplate));
    }

    @Bean
    public Clock clock() {
        return mock(Clock.class);
    }

    @Bean(destroyMethod = "close")
    public LogbrokerCluster logbrokerCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean(destroyMethod = "close")
    public LogbrokerCluster lbkxCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean
    public ExecutorService logbrokerExecutorService() {
        return mock(ExecutorService.class);
    }

    @Bean
    public ExecutorService lbkxExecutorService() {
        return mock(ExecutorService.class);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Tvm2 logbrokerTvm2() {
        return mock(Tvm2.class);
    }

    @Bean
    public Tvm2 vendorTvm2() {
        Tvm2 vendorTvm2 = mock(Tvm2.class);
        when(vendorTvm2.getServiceTicket(anyInt())).thenReturn(Option.of("TICKET"));
        return vendorTvm2;
    }

    @Bean
    public LogbrokerService logbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public NettyRestClient supercheckNettyRestClient(MappingJackson2HttpMessageConverter vendorJsonMessageConverter) {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setTargetModule(Module.MARKET_FMCG_MAIN);
        config.setServiceUrl(vendorsSupercheckUrl);
        return spy(
                new NettyRestClient(
                        config,
                        new NettyHttpClientContext(new HttpClientConfig()),
                        Collections.singletonList(vendorJsonMessageConverter)
                )
        );
    }

    @Bean
    public CrmVendorContactUploadService crmVendorContactUploadService() {
        return mock(CrmVendorContactUploadService.class);
    }

    @Primary
    @Bean
    public PlatformTransactionManager vendorTransactionManager(DataSource dataSource) {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setDataSource(dataSource);
        return jpaTransactionManager;
    }

    @Bean
    public StaffService staffService() {
        return mock(StaffService.class);
    }
}
