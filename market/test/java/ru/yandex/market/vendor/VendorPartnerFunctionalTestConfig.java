package ru.yandex.market.vendor;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.amazonaws.services.s3.AmazonS3;
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
import ru.yandex.cs.billing.billing.BillingServiceSql;
import ru.yandex.cs.billing.history.impl.HistoryServiceSql;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.VendorDbUnitTestConfigH2;
import ru.yandex.market.VendorDbUnitTestConfigH2AndPg;
import ru.yandex.market.core.config.MemCachedTestConfig;
import ru.yandex.market.ir.http.AutoGenerationService;
import ru.yandex.market.ir.http.MatcherService;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.security.AuthoritiesLoader;
import ru.yandex.market.security.checker.StaticDomainAuthorityChecker;
import ru.yandex.market.security.core.IStaticDomainAuthoritiesLoader;
import ru.yandex.market.vendor.sec.OperationRolesService;
import ru.yandex.market.vendor.utils.MockedAuthoritiesLoader;
import ru.yandex.market.vendor.utils.MockedStaticDomainAuthoritiesLoader;
import ru.yandex.market.vendor.utils.MockedStaticDomainAuthorityChecker;
import ru.yandex.market.vendor.utils.StaticDomainAuthoritiesProvider;
import ru.yandex.vendor.blackbox.IBlackboxClient;
import ru.yandex.vendor.brand.Brand;
import ru.yandex.vendor.brand.BrandInfoService;
import ru.yandex.vendor.brand.DbBrandInfoLoader;
import ru.yandex.vendor.brand.MemoryBrandInfoService;
import ru.yandex.vendor.cache.MemCachedWithBackupBalanceService;
import ru.yandex.vendor.category.CategoryService;
import ru.yandex.vendor.content.CategoryInfoService;
import ru.yandex.vendor.csbilling.CsBillingApiService;
import ru.yandex.vendor.csbilling.HttpCsBillingApiClient;
import ru.yandex.vendor.csbilling.HttpCsBillingApiService;
import ru.yandex.vendor.documents.IFileStorage;
import ru.yandex.vendor.documents.S3Connection;
import ru.yandex.vendor.documents.S3FileStorage;
import ru.yandex.vendor.housekeeping.DbSystemErrorLogger;
import ru.yandex.vendor.mock.AutoGenerationServiceResolver;
import ru.yandex.vendor.modelbids.bidding.RestMbiBiddingClient;
import ru.yandex.vendor.modeleditor.DbModelEditorService;
import ru.yandex.vendor.modeleditor.ir.AgApiClient;
import ru.yandex.vendor.modeleditor.mbi.MbiPartnerClient;
import ru.yandex.vendor.modeleditor.mbo.RestMboHttpExporterClient;
import ru.yandex.vendor.notification.VendorNotificationParameterFormatter;
import ru.yandex.vendor.report.ReportClient;
import ru.yandex.vendor.security.IVendorSecurityService;
import ru.yandex.vendor.security.VendorSecurityService;
import ru.yandex.vendor.security.dao.VendorSecurityDao;
import ru.yandex.vendor.staff.CachedStaffClient;
import ru.yandex.vendor.staff.HttpStaffClient;
import ru.yandex.vendor.staff.StaffUserResponseParser;
import ru.yandex.vendor.stats.ClickHouseStatsUtils;
import ru.yandex.vendor.util.FileUploadService;
import ru.yandex.vendor.util.NettyRestClient;
import ru.yandex.vendor.util.Substr;
import ru.yandex.vendor.vendors.CampaignProductInfoRetriever;
import ru.yandex.vendor.vendors.VendorServiceSql;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Конфигурация с моками для всех интеграций.
 */
@Import({
        VendorDbUnitTestConfigH2.class,
        SolomonTestJvmConfig.class,
        MemCachedTestConfig.class,
})
@Configuration
public class VendorPartnerFunctionalTestConfig {
    @Value("${mbo.http-exporter.url}")
    private String mboHttpExporterUrl;

    @Value("${vendors.s3.bucket}")
    private String vendorsS3Bucket;

    @Value("${vendors.s3.root.folder}")
    private String s3RootFolder;

    @Value("${vendors.ugcdaemon.url}")
    private String ugcDaemonClientUrl;

    @Value("http://vendors.pers-grade.url")
    private String persGradeRestClientUrl;

    @Value("http://vendors.pers-pay.url")
    private String persPayRestClientUrl;

    @Value("${vendors.supercheck.url}")
    private String vendorsSupercheckUrl;

    private static WireMockServer newWireMockServer() {
        return new WireMockServer(new WireMockConfiguration().dynamicPort().notifier(new ConsoleNotifier(true)));
    }

    @Bean
    public CommandExecutor commandExecutor() {
        return mock(CommandExecutor.class);
    }

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        when(amazonS3.listBuckets())
                .thenReturn(List.of());
        return amazonS3;
    }

    @Bean
    public S3Connection s3Connection() {
        S3Connection connection = mock(S3Connection.class);
        AmazonS3 amazonS3 = amazonS3();
        when(connection.getS3())
                .thenReturn(amazonS3);
        return connection;
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
            when(clickHouseConnection.unwrap(eq(ClickHouseConnection.class))).thenReturn(clickHouseConnection);
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
    public AuthoritiesLoader authoritiesLoader(OperationRolesService operationRolesService) {
        return new MockedAuthoritiesLoader(operationRolesService);
    }

    @Bean
    public StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider() {
        return new StaticDomainAuthoritiesProvider();
    }

    @Bean
    public StaticDomainAuthorityChecker staticDomainAuthorityChecker(StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider) {
        return new MockedStaticDomainAuthorityChecker(staticDomainAuthoritiesProvider);
    }

    @Bean(name = {"httpExporterRestClient"})
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

    @Bean(name = {"mboHttpExporterClient"})
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

    @Bean(name = {"agApiClient"})
    public AgApiClient agApiClient() {
        return mock(AgApiClient.class);
    }

    @Bean(name = {"modelEditorRestClient"})
    public NettyRestClient modelEditorRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl("");
        config.setTargetModule(Module.AUTOGENERATION_API);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = {"mbiBiddingRestClient"})
    public NettyRestClient mbiBiddingRestClient(@Value("${market.bidding.url}") String url) {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(url);
        config.setTargetModule(Module.MBI_BIDDING);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "advIncutMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer advIncutMock() {
        return newWireMockServer();
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

    @Bean(name = "csBillingApiMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer csBillingApiMock() {
        return newWireMockServer();
    }

    @Bean(name = {"mbiBiddingClient"})
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

    @Bean(name = {"categoryInfoService"})
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

    @Bean(name = "persGradeRestClient")
    public NettyRestClient persGradeRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(persGradeRestClientUrl);
        config.setTargetModule(Module.PERS_GRADE);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "persPayRestClient")
    public NettyRestClient persPayRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(persPayRestClientUrl);
        config.setTargetModule(Module.PERS_PAY);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "ugcDaemonRestClient")
    public NettyRestClient ugcDaemonRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setServiceUrl(ugcDaemonClientUrl);
        config.setTargetModule(Module.UGC_DAEMON);
        return spy(new NettyRestClient(config));
    }

    @Bean(name = "blackboxMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer blackboxMock() {
        return newWireMockServer();
    }

    @Bean
    public Tvm2 vendorTvm2() {
        Tvm2 vendorTvm2 = mock(Tvm2.class);
        when(vendorTvm2.getServiceTicket(anyInt())).thenReturn(Option.of("TICKET"));
        return vendorTvm2;
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

    @Bean(name = "staffMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer staffMock() {
        return newWireMockServer();
    }

    @Bean(name = "abcMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer abcMock() {
        return newWireMockServer();
    }

    @Bean(name = {"staffClient"})
    public HttpStaffClient staffClient(
            @Autowired @Qualifier("staffRestClient") NettyRestClient staffRestClient,
            StaffUserResponseParser staffUserResponseParser) {

        return spy(new HttpStaffClient(staffRestClient, staffUserResponseParser, "staff-token"));
    }

    @Bean
    public StaffUserResponseParser staffUserResponseParser() {
        IFileStorage fileStorage = mock(IFileStorage.class);

        when(fileStorage.isPublicUrlSupported()).thenReturn(true);
        when(fileStorage.createPublicUrl(any())).thenReturn(Optional.empty());

        return spy(new StaffUserResponseParser(fileStorage));
    }

    @Bean
    public IVendorSecurityService vendorSecurityService(
            VendorSecurityDao vendorSecurityDao,
            IStaticDomainAuthoritiesLoader staticDomainAuthoritiesLoader,
            IBlackboxClient blackboxClient,
            CsBillingApiService csBillingApiService,
            BalanceService balanceService) {
        return spy(new VendorSecurityService(
                vendorSecurityDao,
                staticDomainAuthoritiesLoader,
                blackboxClient,
                csBillingApiService,
                balanceService)
        );
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

    @Bean(name = "javaSecApiMock", initMethod = "start", destroyMethod = "stop")
    public WireMockServer javaSecApiMock() {
        return newWireMockServer();
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

    @Bean(name = {"balanceService", "cachedBalanceService"})
    public BalanceService balanceService() {
        return mock(MemCachedWithBackupBalanceService.class);
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
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.now()));
        return clock;
    }

    @Bean
    public MbiPartnerClient mbiPartnerClient() {
        return mock(MbiPartnerClient.class);
    }

    @Bean
    public NettyRestClient supercheckNettyRestClient() {
        NettyRestClient.Config config = new NettyRestClient.Config();
        config.setTargetModule(Module.MARKET_FMCG_MAIN);
        config.setServiceUrl(vendorsSupercheckUrl);
        return spy(
                new NettyRestClient(
                        config,
                        new NettyHttpClientContext(new HttpClientConfig())
                )
        );
    }

    @Bean
    @Primary
    public PlatformTransactionManager vendorTransactionManager(DataSource vendorDataSource) {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setDataSource(vendorDataSource);
        return jpaTransactionManager;
    }

    @Bean
    public IStaticDomainAuthoritiesLoader staticDomainAuthoritiesLoader(StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider) {
        return new MockedStaticDomainAuthoritiesLoader(staticDomainAuthoritiesProvider);
    }
}
