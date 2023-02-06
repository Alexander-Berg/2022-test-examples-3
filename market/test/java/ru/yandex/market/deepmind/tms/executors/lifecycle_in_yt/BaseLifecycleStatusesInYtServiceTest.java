package ru.yandex.market.deepmind.tms.executors.lifecycle_in_yt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.SeasonPeriodUtils;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.CategorySettingsRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepositoryImpl;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusDeletedRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusFilter;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.lifecycle.LifecycleStatusesInYtService;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.tms.config.DeepmindYtConfig;
import ru.yandex.market.deepmind.tms.executors.UploadMskuStatusForLifecycleToYtExecutor;
import ru.yandex.market.deepmind.tms.executors.UploadSskuStatusForLifecycleToYtExecutor;
import ru.yandex.market.ir.yt.util.tables.TestYtClientWrapper;
import ru.yandex.market.ir.yt.util.tables.YtClientWrapper;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.config.YtAndYqlJdbcAutoCluster;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yt.util.table.YtTableService;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.proxy.request.RemoveNode;

import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.STOCK_STORAGE;

/**
 * Юнит тесты для тестирования пайплайна по изменению ЖЦ в YT.
 * В качестве YT использует костыль: моки из arc/arcadia/market/mbo/libs/yt-mock
 */
public abstract class BaseLifecycleStatusesInYtServiceTest extends DeepmindBaseDbTestClass {
    public static final int BERU_ID = 465852;
    private static final DateTimeFormatter STOCKS_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd").appendLiteral(" 14:28:07.0").toFormatter();
    private static final DateTimeFormatter SALESWITHWAREHOUSE_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .toFormatter();
    private static final DateTimeFormatter YYYY_MM = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM")
        .toFormatter();
    protected LifecycleStatusesInYtService service;
    private List<Integer> invalidContractSuppliers = new ArrayList<>();

    @Resource(name = "deepmindDsl")
    protected DSLContext dsl;
    @Resource
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    protected SupplierRepository deepmindSupplierRepository;
    @Resource
    protected MskuRepository deepmindMskuRepository;
    @Resource
    protected SskuStatusRepository sskuStatusRepository;
    @Resource
    protected SskuStatusDeletedRepository sskuStatusDeletedRepository;
    @Resource
    protected MskuStatusRepository mskuStatusRepository;
    @Resource
    protected DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    protected CategorySettingsRepository categorySettingsRepository;
    @Resource
    protected SeasonRepository seasonRepository;
    @Resource
    protected DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    protected TransactionTemplate transactionTemplate;
    @Resource
    protected MskuStockRepository mskuStockRepository;
    @Resource
    protected DbMonitoring monitoring;
    @Resource
    protected QueryService queryService;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    protected StorageKeyValueService deepmindStorageKeyValueService;
    protected OffersConverter offersConverter;

    private UploadMskuStatusForLifecycleToYtExecutor uploadMskuStatusExecutor;
    private UploadSskuStatusForLifecycleToYtExecutor uploadSskuStatusExecutor;

    private YPath sskuStatusesDynTable = YPath.simple("//tmp/input/ssku_statuses");
    private YPath mskuStatusesDynTable = YPath.simple("//tmp/input/msku_statuses");
    private YPath stocksTable = YPath.simple("//tmp/input/stocks");
    private YPath purchasePriceTable = YPath.simple("//tmp/input/purchase_price");
    private YPath sskuTable = YPath.simple("//tmp/input/expanded_ssku");
    private YPath supplierTable = YPath.simple("//tmp/input/suppliers");
    private YPath mskuTable = YPath.simple("//tmp/input/msku");
    private YPath deadStockStatusTable = YPath.simple("//tmp/input/deadStockStatusTable");
    private YPath corefixTable = YPath.simple("//tmp/input/corefixTable");
    private YPath salesWithWarehouseFolder = YPath.simple("//tmp/input/salesWithWarehouseTable");
    private YPath assortSskuTable = YPath.simple("//tmp/input/assort_ssku");
    private YPath contractsTable = YPath.simple("//tmp/input/contracts");
    private YPath workingFolder = YPath.simple("//tmp/deepmind-tmp");
    private TestYt testYt;
    private Clock clock;

    @Before
    public void setUp() throws Exception {
        testYt = new TestYt();

        deepmindStorageKeyValueService = new StorageKeyValueServiceMock();
        var mock = new NamedParameterJdbcTemplate(Mockito.mock(JdbcTemplate.class));
        var ytAutoCluster = YtAndYqlJdbcAutoCluster.createMock(testYt, mock);
        var ytTableServiceUI = UnstableInit.simple(new YtTableService(testYt));
        var ytClientWrapperUI = UnstableInit.<YtClientWrapper>simple(new TestYtClientWrapper(testYt));

        var config = new DeepmindYtConfig(null, null);
        uploadMskuStatusExecutor = new UploadMskuStatusForLifecycleToYtExecutor(
            mskuStatusRepository, jdbcTemplate,
            ytClientWrapperUI, ytTableServiceUI,
            config.mskuStatusForLifecycleTableModel().setYtPath(mskuStatusesDynTable)
        );

        deepmindStorageKeyValueService.putValue("beru_id.supplier_id", BERU_ID);
        offersConverter = new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository);
        uploadSskuStatusExecutor = new UploadSskuStatusForLifecycleToYtExecutor(
            sskuStatusRepository, sskuStatusDeletedRepository, offersConverter, jdbcTemplate,
            ytClientWrapperUI, ytTableServiceUI,
            config.sskuStatusForLifecycleTableModel().setYtPath(sskuStatusesDynTable),
            deepmindStorageKeyValueService
        );

        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        var sskuMskuStatusService = new SskuMskuStatusServiceImpl(
            sskuStatusRepository,
            mskuStatusRepository,
            sskuMskuStatusValidationService,
            sskuMskuHelperService,
            transactionTemplate
        );

        deepmindWarehouseRepository.deleteAll();
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT_FOR_UNIT_TESTS);

        service = new LifecycleStatusesInYtService(
            dsl,
            seasonRepository,
            sskuMskuStatusService,
            deepmindStorageKeyValueService,
            ytAutoCluster,
            workingFolder,
            "unit-test",
            BERU_ID,
            queryService,
            sskuStatusesDynTable,
            mskuStatusesDynTable,
            stocksTable,
            purchasePriceTable,
            sskuTable,
            supplierTable,
            mskuTable,
            deadStockStatusTable,
            corefixTable,
            salesWithWarehouseFolder,
            assortSskuTable,
            contractsTable,
            Mockito.mock(DeepmindSolomonPushService.class),
            monitoring,
            deepmindWarehouseRepository,
            deepmindCategoryRepository
        );

        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_DROPSHIP);

        clock = Clock.fixed(Instant.parse("2022-02-02T10:00:00.00Z"), ZoneOffset.UTC);
        service.setClock(clock);

        // initial data
         /*  Иерархия категорий
              1 ___ 10 ___ 33
                \__ 11 ___ 22
                       \__ 12
         */
        deepmindCategoryRepository =
            new DeepmindCategoryRepositoryImpl(this.namedParameterJdbcTemplate, transactionTemplate);
        deepmindCategoryRepository.insert(category(CategoryTree.ROOT_CATEGORY_ID, "Все товары", -1));
        deepmindCategoryRepository.insert(category(1, "Всё для кухни", CategoryTree.ROOT_CATEGORY_ID));
        deepmindCategoryRepository.insert(category(10, "Посуда", 1));
        deepmindCategoryRepository.insert(category(11, "Печное оборудование", 1));
        deepmindCategoryRepository.insert(category(33, "Селёдошницы", 10));
        deepmindCategoryRepository.insert(category(22, "Ухваты", 11));
        deepmindCategoryRepository.insert(category(12, "Горшки", 11));
        deepmindCategoryRepository.insert(category(198119, "Электроника", CategoryTree.ROOT_CATEGORY_ID));
        deepmindCategoryRepository.insert(category(198118, "Бытовая техника", CategoryTree.ROOT_CATEGORY_ID));
        deepmindCategoryRepository.insert(category(90563, "Техника для дома", 198118));
        deepmindCategoryRepository.insert(category(90574, "Климатическая техника для дома", 198118));
        deepmindCategoryRepository.insert(category(90607, "Фото и видеокамеры", 198119));
        deepmindCategoryRepository.insert(category(91149, "Карманные электронные устройства", 198119));

        // import suppliers & offers
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));

        // set msku
        deepmindMskuRepository.save(TestUtils.newMsku(404040).setCategoryId(33L));
        deepmindMskuRepository.save(TestUtils.newMsku(505050).setCategoryId(22L));
        deepmindMskuRepository.save(TestUtils.newMsku(100000).setCategoryId(22L));
        deepmindMskuRepository.save(TestUtils.newMsku(10).setCategoryId(12L));
        deepmindMskuRepository.save(TestUtils.newMsku(20).setCategoryId(12L));

        // set msku && ssku statuses
        for (var offer : serviceOfferReplicaRepository.findAll()) {
            sskuStatusRepository.save(new SskuStatus().setSupplierId(offer.getSupplierId())
                .setShopSku(offer.getShopSku()).setAvailability(OfferAvailability.ACTIVE));
        }
        for (var msku : deepmindMskuRepository.findAll()) {
            mskuStatusRepository.save(new MskuStatus().setMarketSkuId(msku.getId())
                .setMskuStatus(MskuStatusValue.EMPTY));
        }
    }

    @Before
    public void setUpUser() {
        SecurityContextAuthenticationUtils.setAuthenticationToken("test-user");
    }

    @After
    public void tearDown() {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
    }

    @Test
    public void emptyRun() {
        doubleRun();
    }

    @Test
    public void returnToInactive() {
        sskuStatusRepository.save(sskuStatusRepository.findByKey(60, "sku4").get()
            .setAvailability(OfferAvailability.DELISTED));

        mskuStockRepository.insert(new MskuStockInfo().setSupplierId(60).setShopSku("sku4")
            .setWarehouseId((int) DeepmindWarehouseRepository.ROSTOV_ID)
            .setFitInternal(1)
        );

        doubleRun();

        SskuStatus status = sskuStatusRepository.findByKey(60, "sku4").get();
        Assertions.assertThat(status.getAvailability()).isEqualTo(OfferAvailability.INACTIVE);
    }

    @Test
    public void blockSaveIfTooManyChanges() {
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get()
            .setMskuStatus(MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1))
            .setNpdFinishDate(LocalDate.now().minusDays(1)));

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(404040L);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);

    }

    @Test
    public void testIsModifiedByUser() {
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get()
            .setMskuStatus(MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1))
            .setNpdFinishDate(LocalDate.now().minusDays(1))
            .setModifiedByUser(true)
        );

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(404040L).orElseThrow();
        Assertions.assertThat(mskuStatus.getMskuStatus()).isEqualTo(MskuStatusValue.REGULAR);
        Assertions.assertThat(mskuStatus.getModifiedByUser()).isFalse();
    }

    protected void doubleRun() {
        doRun();

        // сразу делаем второй прогон.
        // Если после него система сделает изменения в БД, значит статусы не пришли в консистентное состояние
        // Это очень плохо, значит правила написаны неправильно и образуется бесконечный цикл на обновление
        doRun();

        var sskus = sskuStatusRepository.find(new SskuStatusFilter().setNotUploadedToYt(true));
        var mskus = mskuStatusRepository.find(new MskuStatusRepository.Filter().setNotYtUploadedTs(true));

        Assertions.assertThat(Stream.concat(sskus.stream(), mskus.stream()))
            .isEmpty();
    }

    private void doRun() {
        // Перед прогоном очищаем рабочую папку (чтобы при повторном прогоне таблиц еще не было).
        testYt.cypress().remove(new RemoveNode(workingFolder).setForce(true));

        // upload ssku
        var sskus = serviceOfferReplicaRepository.findAll().stream()
            .map(o -> {
                var real = offersConverter.convertInternalToReal(o.getServiceOfferKey());
                return YTree.mapBuilder()
                    .key("supplier_id").value(real.getSupplierId())
                    .key("shop_sku").value(real.getShopSku())
                    .key("raw_supplier_id").value(o.getSupplierId())
                    .key("raw_shop_sku").value(o.getShopSku())
                    .key("category_id").value(o.getCategoryId())
                    .key("approved_market_sku_id").value(o.getMskuId())
                    .key("supplier_type").value(o.getSupplierType().name())
                    .buildMap();
            })
            .collect(Collectors.toList());
        testYt.tables().write(sskuTable, YTableEntryTypes.YSON, sskus);

        // upload msku
        var mskus = deepmindMskuRepository.findAll().stream()
            .map(m -> YTree.mapBuilder()
                .key("id").value(m.getId())
                .key("hid").value(m.getCategoryId())
                .buildMap())
            .collect(Collectors.toList());
        testYt.tables().write(mskuTable, YTableEntryTypes.YSON, mskus);

        // upload stocks
        List<YTreeMapNode> stocks = dsl.selectFrom(STOCK_STORAGE).fetch().stream()
            .map(r -> {
                var key = new ServiceOfferKey(r.getSupplierId(), r.getShopSku());
                var realkey = offersConverter.convertInternalToReal(key);
                return YTree.mapBuilder()
                    .key("supplier_id").value((long) realkey.getSupplierId()) // приводим к лонгу как на проде
                    .key("shop_sku").value(realkey.getShopSku())
                    .key("warehouse_id").value(r.getWarehouseId())
                    .key("fit").value(r.getFit())
                    .buildMap();
            }).collect(Collectors.toList());
        testYt.tables().write(stocksTable, YTableEntryTypes.YSON, stocks);

        // create price table if not
        if (!testYt.cypress().exists(purchasePriceTable)) {
            testYt.cypress().create(new CreateNode(purchasePriceTable, ObjectType.Table).setRecursive(true));
        }
        // create deadStockStatus table if not
        if (!testYt.cypress().exists(deadStockStatusTable)) {
            testYt.cypress().create(new CreateNode(deadStockStatusTable, ObjectType.Table).setRecursive(true));
        }
        // create corefix table if not
        if (!testYt.cypress().exists(corefixTable)) {
            testYt.cypress().create(new CreateNode(corefixTable, ObjectType.Table).setRecursive(true));
        }

        var first = salesWithWarehouseFolder.child(LocalDate.now().withDayOfMonth(1).format(YYYY_MM));
        var second = salesWithWarehouseFolder.child(LocalDate.now().withDayOfMonth(1).minusMonths(1).format(YYYY_MM));
        var third = salesWithWarehouseFolder.child(LocalDate.now().withDayOfMonth(1).minusMonths(2).format(YYYY_MM));
        var fourth = salesWithWarehouseFolder.child(LocalDate.now().withDayOfMonth(1).minusMonths(3).format(YYYY_MM));
        var fifth = salesWithWarehouseFolder.child(LocalDate.now().withDayOfMonth(1).minusMonths(4).format(YYYY_MM));
        var salesTables = List.of(first, second, third, fourth, fifth);
        salesTables.forEach(table -> {
            if (!testYt.cypress().exists(table)) {
                testYt.cypress().create(new CreateNode(table, ObjectType.Table).setRecursive(true));
            }
        });

        // upload msku & ssku statuses
        uploadMskuStatusExecutor.execute();
        uploadSskuStatusExecutor.execute();

        createTmpTables(salesTables);

        service.run();
    }

    /**
     * Так как шаг enrich у нас замокирован, то сами заполняем промежуточные таблицы (DEEPMIND-2027).
     */
    private void createTmpTables(List<YPath> salesTables) {
        var now = Instant.now(clock);
        var stocksTmp = service.createTmpTablePath(now, "stocks");
        var sskuStatusesTmp = service.createTmpTablePath(now, "ssku_statuses");
        var salesWithWarehouseTmp = service.createTmpTablePath(now, "sales_with_warehouse");
        var deadstockStatusTmp = service.createTmpTablePath(now, "dead_stock_status");
        var contractsTmp = service.createTmpTablePath(now, "invalid_contracts");

        var statusesData = testYt.tables().read(sskuStatusesDynTable, YTableEntryTypes.YSON).stream()
            .map(v -> {
                var offerO = testYt.tables().read(sskuTable, YTableEntryTypes.YSON).stream()
                    .filter(o -> v.getInt("supplier_id") == o.getInt("supplier_id")
                        && v.getString("shop_sku").equals(o.getString("shop_sku")))
                    .filter(o -> o.getLong("approved_market_sku_id") > 0)
                    .findFirst();
                var map = new HashMap<String, Object>(v.asMap());
                return offerO.map(entries -> {
                    map.put("msku_id", entries.getLong("approved_market_sku_id"));
                    map.put("supplier_type", entries.getString("supplier_type"));
                    map.put("category_id", entries.getLong("category_id"));
                    return YTree.node(map).mapNode();
                }).orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        var stocksData = testYt.tables().read(stocksTable, YTableEntryTypes.YSON).stream()
            .map(v -> {
                var offerO = testYt.tables().read(sskuTable, YTableEntryTypes.YSON).stream()
                    .filter(o -> v.getInt("supplier_id") == o.getInt("supplier_id")
                        && v.getString("shop_sku").equals(o.getString("shop_sku")))
                    .filter(o -> o.getLong("approved_market_sku_id") > 0)
                    .findFirst();
                var map = new HashMap<String, Object>(v.asMap());
                return offerO.map(entries -> {
                    map.put("msku_id", entries.getLong("approved_market_sku_id"));
                    return YTree.node(map).mapNode();
                }).orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        var deadstocksData = testYt.tables().read(deadStockStatusTable, YTableEntryTypes.YSON).stream()
            .map(v -> {
                var offerO = testYt.tables().read(sskuTable, YTableEntryTypes.YSON).stream()
                    .filter(o -> v.getInt("supplier_id") == o.getInt("supplier_id")
                        && v.getString("shop_sku").equals(o.getString("shop_sku")))
                    .filter(o -> o.getLong("approved_market_sku_id") > 0)
                    .findFirst();
                var map = new HashMap<String, Object>(v.asMap());
                return offerO.map(entries -> {
                    map.put("msku_id", entries.getLong("approved_market_sku_id"));
                    return YTree.node(map).mapNode();
                }).orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        var salesData = salesTables.stream()
            .flatMap(table -> testYt.tables().read(table, YTableEntryTypes.YSON).stream())
            .map(v -> {
                var offerO = testYt.tables().read(sskuTable, YTableEntryTypes.YSON).stream()
                    .filter(o -> v.getInt("supplier_id") == o.getInt("supplier_id")
                        && v.getString("shop_sku").equals(o.getString("shop_sku")))
                    .filter(o -> o.getLong("approved_market_sku_id") > 0)
                    .findFirst();
                var map = new HashMap<String, Object>(v.asMap());
                return offerO.map(entries -> {
                    map.put("msku_id", entries.getLong("approved_market_sku_id"));
                    return YTree.node(map).mapNode();
                }).orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        var contractsData = testYt.tables().read(sskuStatusesDynTable, YTableEntryTypes.YSON).stream()
            .map(v -> {
                var offerO = testYt.tables().read(sskuTable, YTableEntryTypes.YSON).stream()
                    .filter(o -> v.getInt("supplier_id") == o.getInt("supplier_id")
                        && v.getString("shop_sku").equals(o.getString("shop_sku")))
                    .filter(o -> o.getLong("approved_market_sku_id") > 0)
                    .findFirst();
                var map = new HashMap<String, Object>();
                return offerO.map(entries -> {
                    if (invalidContractSuppliers.contains(entries.getInt("supplier_id"))) {
                        map.put("msku_id", entries.getLong("approved_market_sku_id"));
                        map.put("raw_supplier_id", entries.getInt("raw_supplier_id"));
                        return YTree.node(map).mapNode();
                    }
                    return null;
                }).orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        testYt.tables().write(sskuStatusesTmp, YTableEntryTypes.YSON, statusesData);
        testYt.tables().write(stocksTmp, YTableEntryTypes.YSON, stocksData);
        testYt.tables().write(deadstockStatusTmp, YTableEntryTypes.YSON, deadstocksData);
        testYt.tables().write(salesWithWarehouseTmp, YTableEntryTypes.YSON, salesData);
        testYt.tables().write(contractsTmp, YTableEntryTypes.YSON, contractsData);
    }

    protected Category category(long id, String name, long parentId) {
        return new Category().setCategoryId(id).setName(name).setParentCategoryId(parentId)
            .setPublished(true).setParameterValues(List.of());
    }

    protected void addCorefix(long mskuId) {
        if (!testYt.cypress().exists(corefixTable)) {
            testYt.cypress().create(new CreateNode(corefixTable, ObjectType.Table).setRecursive(true));
        }

        testYt.tables().write(corefixTable.append(true),
            YTableEntryTypes.YSON, List.of(
                YTree.mapBuilder()
                    .key("msku_id").value(mskuId)
                    .buildMap()
            ));
    }

    protected void addSalesWithWarehouse(int supplierId, String shopSku, long warehouseId,
                                         LocalDateTime grossDatetime) {
        var date = grossDatetime.toLocalDate().withDayOfMonth(1);
        var table = salesWithWarehouseFolder.child(date.format(YYYY_MM));

        if (!testYt.cypress().exists(table)) {
            testYt.cypress().create(new CreateNode(table, ObjectType.Table).setRecursive(true));
        }

        var realkey = offersConverter.convertInternalToReal(new ServiceOfferKey(supplierId, shopSku));
        testYt.tables().write(table.append(true),
            YTableEntryTypes.YSON, List.of(
                YTree.mapBuilder()
                    .key("supplier_id").value(realkey.getSupplierId())
                    .key("shop_sku").value(realkey.getShopSku())
                    .key("warehouse_id").value(warehouseId)
                    .key("gross_datetime").value(SALESWITHWAREHOUSE_DATE_TIME_FORMATTER.format(grossDatetime))
                    .buildMap()
            ));
    }

    protected void addDeadstock(int supplierId, String shopSku, long warehouseId, LocalDate deadstockSince) {
        var realkey = offersConverter.convertInternalToReal(new ServiceOfferKey(supplierId, shopSku));

        if (!testYt.cypress().exists(deadStockStatusTable)) {
            testYt.cypress().create(new CreateNode(deadStockStatusTable, ObjectType.Table).setRecursive(true));
        }

        testYt.tables().write(deadStockStatusTable.append(true),
            YTableEntryTypes.YSON, List.of(
                YTree.mapBuilder()
                    .key("supplier_id").value(realkey.getSupplierId())
                    .key("shop_sku").value(realkey.getShopSku())
                    .key("warehouse_id").value(warehouseId)
                    .key("deadstock_since").value(deadstockSince.toString())
                    .buildMap()
            ));
    }

    protected void addPurchasePrice(int supplierId, String shopSku, long price) {
        var innerKey = new ServiceOfferKey(supplierId, shopSku);
        var realKey = offersConverter.convertInternalToReal(innerKey);
        if (realKey.getSupplierId() != BERU_ID) {
            throw new IllegalArgumentException("Sorry, only 1P offers can have purchase price, your key: " + realKey);
        }

        if (!testYt.cypress().exists(purchasePriceTable)) {
            testYt.cypress().create(new CreateNode(purchasePriceTable, ObjectType.Table).setRecursive(true));
        }

        var offer = serviceOfferReplicaRepository.findOfferByKey(innerKey);
        testYt.tables().write(purchasePriceTable.append(true), YTableEntryTypes.YSON, List.of(
            YTree.mapBuilder()
                .key("msku").value(offer.getMskuId())
                .key("ssku").value(realKey.getShopSku())
                .key("purchprice").value(price)
                .buildMap()
        ));
    }

    protected void addInvalidContractSupplier(int supplierId) {
        invalidContractSuppliers.add(supplierId);
    }

    protected void removeInvalidContractSupplier(Integer supplierId) {
        invalidContractSuppliers.remove(supplierId);
    }

    protected CategorySettings categorySettings(long categoryId, long seasonId) {
        return new CategorySettings()
            .setCategoryId(categoryId)
            .setSeasonId(seasonId);
    }

    protected SeasonRepository.SeasonWithPeriods season(LocalDate start, LocalDate end) {
        var startMMDD = SeasonPeriodUtils.toMmDD(start);
        var endMMDd = SeasonPeriodUtils.toMmDD(end);
        return new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Test season"),
            List.of(new SeasonPeriod().setWarehouseId(DeepmindWarehouseRepository.TOMILINO_ID)
                .setDeliveryFromMmDd(startMMDD).setDeliveryToMmDd(endMMDd)
                .setFromMmDd("01-01").setToMmDd("01-01")
            )
        );
    }

    protected SeasonRepository.SeasonWithPeriods season(LocalDate start1, LocalDate end1,
                                                        LocalDate start2, LocalDate end2) {
        var startMMDD1 = SeasonPeriodUtils.toMmDD(start1);
        var endMMDd1 = SeasonPeriodUtils.toMmDD(end1);
        var startMMDD2 = SeasonPeriodUtils.toMmDD(start2);
        var endMMDd2 = SeasonPeriodUtils.toMmDD(end2);
        return new SeasonRepository.SeasonWithPeriods(
            new Season().setName("Test season"),
            List.of(
                new SeasonPeriod().setWarehouseId(DeepmindWarehouseRepository.TOMILINO_ID)
                    .setDeliveryFromMmDd(startMMDD1).setDeliveryToMmDd(endMMDd1)
                    .setFromMmDd("01-01").setToMmDd("01-01"),
                new SeasonPeriod().setWarehouseId(DeepmindWarehouseRepository.SOFINO_ID)
                    .setDeliveryFromMmDd(startMMDD2).setDeliveryToMmDd(endMMDd2)
                    .setFromMmDd("01-01").setToMmDd("01-01")
            )
        );
    }

    protected MskuStockInfo stock(int supplierId, String shopSku, long warehouseId, int fit) {
        return new MskuStockInfo()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId((int) warehouseId)
            .setFitInternal(fit);
    }
}
