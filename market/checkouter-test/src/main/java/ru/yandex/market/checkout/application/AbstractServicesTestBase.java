package ru.yandex.market.checkout.application;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.zookeeper.KeeperException;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.util.InMemoryResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.checkout.checkouter.Main;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryDraftFetcher;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.feature.CachedService;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.NamedFeatureTypeRegister;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.logging.LoggingBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.lock.LockStorage;
import ru.yandex.market.checkout.checkouter.order.OrderCreateService;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils;
import ru.yandex.market.checkout.checkouter.test.config.services.IntTestServicesConfig;
import ru.yandex.market.checkout.checkouter.test.config.services.LocalLockService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.tasks.EnableAwareTask;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.helpers.QueuedCallsHelper;
import ru.yandex.market.checkout.helpers.TmsTaskHelper;
import ru.yandex.market.checkout.liquibase.config.DbMigrationCheckouterArchiveConfig;
import ru.yandex.market.checkout.liquibase.config.DbMigrationCheckouterConfig;
import ru.yandex.market.checkout.liquibase.config.OmsServiceDbMigrationConfiguration;
import ru.yandex.market.checkout.storage.impl.LockService;
import ru.yandex.market.checkout.stub.lock.LockStorageStub;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.personal.PersonalMockConfigurer;
import ru.yandex.market.checkouter.jooq.Tables;
import ru.yandex.market.common.zk.ZooClient;
import ru.yandex.market.monitoring.thread.pool.InstrumentedExecutors;
import ru.yandex.market.request.context.IContextFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.APPLE_PAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.GOOGLE_PAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.TINKOFF_CREDIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.TINKOFF_INSTALLMENTS;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkouter.jooq.oms.tables.TaskProperties.TASK_PROPERTIES;

@ContextConfiguration(classes = AbstractServicesTestBase.TestBaseConfig.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractServicesTestBase {

    public static final int POPULATE_DB_TRIES_COUNT = 3;
    protected static final String TEST_DISPLAY_NAME = "[" + ParameterizedTest.INDEX_PLACEHOLDER + "] "
            + ParameterizedTest.DISPLAY_NAME_PLACEHOLDER;
    private static final String CLEAN_ORDERS = "TRUNCATE " + Tables.ORDERS.getName() + " CASCADE";
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Resource truncateScript = new ClassPathResource("/files/truncate-db.sql");
    private final Resource truncateArchivedTablesScript = new InMemoryResource(buildTruncateArchivedTablesSql());
    private final Resource setUpScript = new ClassPathResource("/files/setup-db.sql");
    private final ResourceDatabasePopulator truncateDb = new ResourceDatabasePopulator(truncateScript,
            truncateArchivedTablesScript);
    private final ResourceDatabasePopulator truncateArchiveDb = new ResourceDatabasePopulator(truncateScript);
    private final ResourceDatabasePopulator setUpPopulator = new ResourceDatabasePopulator(setUpScript);
    @Autowired
    protected CheckouterProperties checkouterProperties;
    @Autowired
    protected CheckouterFeatureWriter checkouterFeatureWriter;
    protected CheckouterFeatureReader checkouterFeatureReader;
    @Autowired
    protected WireMockServer reportMock;
    @Autowired
    protected WireMockServer reportMockWhite;
    @Autowired
    protected WireMockServer pushApiMock;
    @Autowired
    protected PersonalMockConfigurer personalMockConfigurer;
    @Autowired
    protected TrustMockConfigurer trustMockConfigurer;
    @Autowired
    protected OrderService orderService;
    @Autowired
    protected OrderUpdateService orderUpdateService;
    @Autowired
    protected OrderCreateService orderCreateService;
    @Autowired
    protected EventService eventService;
    @Autowired
    protected ShopService shopService;
    @Autowired
    protected DataSource masterDatasource;
    @Autowired
    protected JdbcTemplate masterJdbcTemplate;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected Map<String, ZooTask> taskMap;
    @Autowired
    protected TmsTaskHelper tmsTaskHelper;
    @Autowired
    protected QueuedCallsHelper queuedCallsHelper;
    @Autowired
    protected ActualDeliveryDraftFetcher actualDeliveryDraftFetcher;
    @Autowired
    @Qualifier("archiveMasterDataSources")
    protected List<DataSource> archiveMasterDataSources;
    @Autowired
    @Qualifier("archiveTransactionTemplates")
    protected List<TransactionTemplate> archiveTransactionTemplates;
    @Autowired
    @Qualifier("archiveMasterJdbcTemplates")
    protected List<JdbcTemplate> archiveMasterJdbcTemplates;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    private DSLContext taskV2DslContext;
    @Autowired
    @Qualifier("clock")
    private TestableClock testableClock;
    @Autowired
    private List<WireMockServer> mocks;
    @Autowired
    private ZooClient zooClient;
    @Autowired
    @Qualifier("personalDataService")
    private CachedService cachedPersonalDataService;

    private static String buildTruncateArchivedTablesSql() {
        return ArchivedTableUtils.TRACKED_ARCHIVING_TABLES.stream()
                .map(ArchivedTableUtils::archivedTable)
                .map(Table::getName)
                .collect(Collectors.joining(", ", "truncate table ", ";"));
    }

    @Autowired
    public void setCheckouterFeatureReader(CheckouterFeatureReader checkouterFeatureReader) {
        this.checkouterFeatureReader = checkouterFeatureReader;
    }

    protected void truncateDatabase() {
        populateDb(transactionTemplate, truncateDb, masterDatasource, POPULATE_DB_TRIES_COUNT, null);
    }

    protected void truncateArchiveDatabase() {
        for (int i = 0; i < archiveTransactionTemplates.size(); i++) {
            populateDb(archiveTransactionTemplates.get(i), truncateArchiveDb, archiveMasterDataSources.get(i),
                    POPULATE_DB_TRIES_COUNT, null);
        }
    }

    private void populateDb(
            TransactionTemplate template,
            ResourceDatabasePopulator populator,
            DataSource dataSource,
            int tries,
            @Nullable Exception lastException
    ) {
        try {
            if (tries <= 0) {
                log.error("No more tries", lastException);
                return;
            }
            try {
                template.execute(tc -> {
                    DatabasePopulatorUtils.execute(populator, dataSource);
                    return null;
                });
            } catch (Exception e) {
                tries--;
                Thread.sleep(1000);
                populateDb(template, populator, dataSource, tries, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void resetZooKeeperPaths() {
        try {
            var lock = zooClient.lock("/checkout");
            try {
                zooClient.deleteWithChildren("/checkout/locks");
            } finally {
                lock.unlock();
            }
        } catch (KeeperException e) {
            //do nothing;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected void setUpDBDefaultData() {
        populateDb(transactionTemplate, setUpPopulator, masterDatasource, POPULATE_DB_TRIES_COUNT, null);
    }

    protected void enableAllTasks() {
        taskMap.values().stream()
                .map(ZooTask::asEnableAwareTask)
                .flatMap(Optional::stream)
                .peek(t -> t.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive())))
                .forEach(EnableAwareTask::enable);
    }

    protected JdbcTemplate getRandomWritableJdbcTemplate() {
        return masterJdbcTemplate;
    }

    @PostConstruct
    private void afterConstruction() throws SQLException {
        try (Connection connection = masterDatasource.getConnection()) {
            log.info("Using connection url: " + connection.getMetaData().getURL());
        }
        for (DataSource archiveMasterDataSource : archiveMasterDataSources) {
            try (Connection connection = archiveMasterDataSource.getConnection()) {
                log.info("Using connection url: " + connection.getMetaData().getURL());
            }
        }
    }

    @BeforeEach
    public void setUpBase() {
        cachedPersonalDataService.invalidateAll();
        setupFeatureDefaults();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_ADJUST_REFUND_ITEMS_BY_PAYMENT, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_YANDEX_EMPLOYEE_CART, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DELIVERY_ADDRESS_RECIPIENT_HIDDEN, true);

        // Проект заморожен, нет смысла в тестах гонять YT реализацию https://st.yandex-team.ru/MARKETCHECKOUT-27367
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_YT_FOR_LOCK, false);

        checkouterFeatureWriter.writeValue(MapFeatureType.CHANGEABLE_PAYMENT_METHOD, Map.of(
                YANDEX, Set.of(YANDEX),
                APPLE_PAY, Set.of(YANDEX),
                GOOGLE_PAY, Set.of(YANDEX),
                TINKOFF_CREDIT, Set.of(YANDEX),
                TINKOFF_INSTALLMENTS, Set.of(YANDEX)));

        checkouterFeatureWriter.writeValue(IntegerFeatureType.NEW_CHECK_RENDER_USAGE_STEP, 3);

        checkouterFeatureWriter.writeValue(CollectionFeatureType.ENABLE_UID_FOR_TRUST_ENDPOINTS, Set.of(
                "post_payments", "post_payments_deliver", "post_payments_unhold", "post_payments_clear",
                "post_payments_start", "post_payments_orders_resize", "get_payments_receipts", "post_orders_batch",
                "post_refunds", "post_refunds_start", "post_credit", "post_credit_deliver",
                "post_credit_unhold", "post_credit_start", "get_credit_gateway_info"
        ));

        // логируем получение фиче-флагов, удобно для анализа флакающих тестов
        checkouterFeatureWriter.writeValue(LoggingBooleanFeatureType.CHECKOUTER_FEATURE, true);

        setUpDBDefaultData();
        enableAllTasks();

        checkouterProperties.setAsyncRefundStrategies(
                Set.of(
                        CheckouterProperties.RefundStrategy.SUPPLIER_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.ITEMS_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.BNPL_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.VIRTUAL_BNPL_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.CASH_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.TINKOFF_CREDIT_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.CASHBACK_EMIT_REFUND_STRATEGY,
                        CheckouterProperties.RefundStrategy.PLUS_REFUND_STRATEGY
                )
        );

        // множество тесты не настроено на асинхронное взаимодействие с Push-Api
        checkouterFeatureWriter.writeValue(PermanentBooleanFeatureType.ASYNC_PUSH_API, false);
        checkouterFeatureWriter.writeValue(PermanentBooleanFeatureType.ASYNC_PUSH_API_FBS, false);

        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_OLD_BNPL_PLAN_CHECK_URL, false);

        checkouterFeatureWriter.writeValue(BooleanFeatureType.UPDATE_STATUS_VALIDATION_NEW_FLOW, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_PERSONAL, true);
    }

    @AfterEach
    public void tearDownBase() {
        cachedPersonalDataService.invalidateAll();
        clearFixed();
        for (WireMockServer mock : mocks) {
            mock.resetAll();
        }
        truncateDatabase();
        truncateArchiveDatabase();
        resetZooKeeperPaths();
        taskMap.values().forEach(ZooTask::stop);
        taskV2DslContext.truncateTable(TASK_PROPERTIES).execute();
        setupFeatureDefaults();
    }

    protected void setupFeatureDefaults() {
        NamedFeatureTypeRegister.getAllowableTypes().values().forEach(type -> checkouterFeatureWriter.writeValue(type,
                type.getDefaultValue()));
    }

    /**
     * Передвинуть время в будущее.
     *
     * @param amount количество временных единиц
     * @param unit   единица измерения времени
     */
    protected void jumpToFuture(long amount, TemporalUnit unit) {
        Instant time = getClock().instant().plus(amount, unit);
        setFixedTime(time);
    }

    /**
     * Передвинуть время в прошлое.
     *
     * @param amount количество временных единиц
     * @param unit   единица измерения времени
     */
    protected void jumpToPast(long amount, TemporalUnit unit) {
        setFixedTime(getClock().instant().minus(amount, unit));
    }

    /**
     * Зафиксировать время в текущей точке.
     */
    protected Instant freezeTime() {
        Instant instant = getClock().instant();
        setFixedTime(instant);
        return instant;
    }

    /**
     * Зафиксировать время в данной точке.
     *
     * @param point временная точка в формате {@link DateTimeFormatter#ISO_INSTANT}.
     */
    protected Instant freezeTimeAt(String point) {
        Instant instant = Instant.parse(point);
        setFixedTime(instant);
        return instant;
    }

    /**
     * Зафиксировать время в данной точке.
     *
     * @param point  временная точка в формате {@link DateTimeFormatter#ISO_INSTANT}.
     * @param zoneId часовой пояс
     */
    protected Instant freezeTimeAt(String point, ZoneId zoneId) {
        Instant instant = Instant.parse(point);
        setFixedTime(instant, zoneId);
        return instant;
    }

    /**
     * Замораживает время во всем приложении.
     *
     * @param instant временная точка
     * @see #jumpToFuture(long, TemporalUnit)
     * @see #jumpToPast(long, TemporalUnit)
     * @see #freezeTime()
     */
    protected void setFixedTime(Instant instant) {
        setFixedTime(instant, ZoneId.systemDefault());
    }

    /**
     * Замораживет время во всем приложении.
     *
     * @param instant временная точка
     * @param zoneId  часовой пояс
     */
    protected void setFixedTime(Instant instant, ZoneId zoneId) {
        this.testableClock.setFixed(instant, zoneId);
    }

    /**
     * Очищает зафиксированное время.
     */
    protected void clearFixed() {
        testableClock.clearFixed();
    }

    protected Clock getClock() {
        return this.testableClock;
    }

    public TrustMockConfigurer getTrustMockConfigurer() {
        return trustMockConfigurer;
    }

    protected void verifyReportColorCalls(WireMockServer mock, ru.yandex.market.common.report.model.Color color,
                                          int callCount) {
        mock.verify(callCount, anyRequestedFor(urlPathEqualTo("/yandsearch"))
                .withQueryParam("rgb", color == null ? absent() : equalTo(color.getValue())));
    }

    protected void cleanOrders() {
        transactionTemplate.execute(ts -> {
            jdbcTemplate.execute(CLEAN_ORDERS);
            return null;
        });
    }

    @Configuration
    @Import({Main.class, DbMigrationCheckouterConfig.class, DbMigrationCheckouterArchiveConfig.class,
            OmsServiceDbMigrationConfiguration.class})
    @ComponentScan(basePackageClasses = IntTestServicesConfig.class)
    public static class TestBaseConfig {

        @Bean
        public ExecutorService cashbackServiceExecutor(
                IContextFactory checkouterRequestContextFactory
        ) {
            return InstrumentedExecutors.contextFixedThreadPoolBuilder(1, "CashbackServiceExecutor")
                    .setContextFactories(List.of(checkouterRequestContextFactory))
                    .setDaemon(true)
                    .build();
        }

        @Bean
        @Primary
        public CheckouterFeatureResolverStub checkouterFeatureResolverStub() {
            return new CheckouterFeatureResolverStub();
        }

        @Bean
        @Primary
        public LockStorage lockStorageStub() {
            return new LockStorageStub();
        }

        @Bean
        @Primary
        public LockService zookeeperLockService(
                @Qualifier("entityGroupList") List<String> entityGroupList
        ) {
            return new LocalLockService(entityGroupList);
        }
    }
}
