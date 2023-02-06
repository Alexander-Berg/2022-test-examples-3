package ru.yandex.market.deepmind.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.config.properties.DeepmindYtProperties;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.BusinessProcessEconomicMetrics;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategorySettings;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AssortSskuRepository;
import ru.yandex.market.deepmind.common.repository.BusinessProcessEconomicMetricsRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.CategorySettingsRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.BackgroundUtil;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers;
import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtInfo;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.tracker_approver.configuration.EnhancedTrackerApproverConfiguration;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverExecutionContext;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory;
import ru.yandex.market.deepmind.tracker_approver.service.enhanced.EnhancedTrackerApproverExecutor;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.excel.ExcelIgnoresConfigImpl;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.tracker.tracker.MockSession;
import ru.yandex.market.tracker.tracker.pojo.MockQueue;
import ru.yandex.market.tracker.tracker.pojo.MockUser;

public abstract class DeepmindBaseEnhancedTrackerApproverExecutorTestClass extends DeepmindBaseDbTestClass {
    @Resource
    protected JdbcTemplate jdbcTemplate;
    @Resource
    protected TrackerApproverTicketRepository ticketRepository;
    @Resource
    protected TrackerApproverDataRepository dataRepository;
    @Resource
    protected TransactionTemplate transactionTemplate;
    @Resource(name = "deepmindTransactionHelper")
    protected TransactionHelper transactionHelper;
    @Resource
    protected SupplierRepository deepmindSupplierRepository;
    @Resource
    protected SskuStatusRepository sskuStatusRepository;
    @Resource
    protected MskuStatusRepository mskuStatusRepository;
    @Resource
    protected MskuRepository deepmindMskuRepository;
    @Resource
    protected AssortSskuRepository assortSskuRepository;
    @Resource(name = "serviceOfferReplicaRepository")
    protected ServiceOfferReplicaRepository offerRepository;
    @Resource
    protected MskuStockRepository mskuStockRepository;
    @Resource
    protected DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    protected MskuInfoRepository mskuInfoRepository;
    @Resource
    protected DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    protected ObjectMapper trackerApproverObjectMapper;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    protected BusinessProcessEconomicMetricsRepository economicMetricsRepository;
    @Resource
    protected OffersConverterImpl offersConverter;
    @Resource
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    protected SeasonRepository seasonRepository;
    @Resource
    protected DeepmindCategoryRepository categoryRepository;
    @Resource
    protected CategorySettingsRepository categorySettingsRepository;

    protected DeepmindCategoryCachingServiceMock categoryCachingService = new DeepmindCategoryCachingServiceMock();

    protected TrackerApproverExecutionContext executionContext;
    protected TrackerApproverFactory factory;
    protected EnhancedTrackerApproverExecutor executor;
    protected MockSession session;
    protected DeepmindYtProperties deepmindYtProperties;

    protected MockUser user;
    protected MockQueue queue;

    // section for test generated excel file settings
    protected List<Header> headerList = EnrichApproveToPendingExcelComposer.HEADERS;

    protected String deepmindRobotLogin;
    protected StorageKeyValueServiceMock storageKeyValueService = new StorageKeyValueServiceMock();

    @Before
    public void setUp() {
        session = new MockSession();

        user = new MockUser("user_login", "Sergei User", session);
        deepmindRobotLogin = user.getLogin();
        queue = new MockQueue("TEST", session);

        // config components
        session.components().create("1P", queue);
        session.components().create("Возвращение ассортимента", queue);
        session.components().create("Спец.закупка", queue);
        session.components().create("Вывод ассортимента", queue);
        session.components().create("Ввод ассортимента", queue);

        // config stats
        var statuses = session.statuses();
        var transitions = session.transitions();

        var open = statuses.add("open");
        var closed = statuses.add("closed");
        var resolved = statuses.add("resolved");
        var check = statuses.add("check");
        var autoRule = statuses.add("dataProcessing");
        var needApproval = statuses.add("needApproval");
        var needCorrection = statuses.add("needCorrection");
        var awaitActivation = statuses.add("awaitingForActivation");


        var closeTransition = transitions.add(
            List.of(open, autoRule, needApproval, resolved, check, needCorrection, awaitActivation), "close", closed);
        var reopenTransition = transitions.add(List.of(resolved, closed), "reopen", open);
        var resolveTransition = transitions.add(List.of(open, closed), "resolve", resolved);
        var autoRuleTransition = transitions.add(List.of(open, needApproval), "dataProcessing", autoRule);
        var needApprovalTransition = transitions.add(List.of(autoRule), "needApproval", needApproval);
        var checkTransition = transitions.add(List.of(open, needApproval, needCorrection), "check", check);
        var awaitActivationTransition = transitions.add(
            List.of(needCorrection), "awaitingForActivation", awaitActivation);
        var fixTransition = transitions.add(List.of(check, awaitActivation), "needCorrection", needCorrection);

        factory = new TrackerApproverFactory(
            dataRepository, ticketRepository, transactionTemplate, trackerApproverObjectMapper);
        executionContext = new TrackerApproverExecutionContext().setThreadCount(1);

        var storageKeyValueServiceSpy = Mockito.spy(storageKeyValueService);
        Mockito.doReturn(null).when(storageKeyValueServiceSpy).getOffsetDateTime(Mockito.any(), Mockito.any());

        var configuration = new EnhancedTrackerApproverConfiguration(
            "test",
            queue.getName(),
            factory,
            executionContext
        );
        executor = new EnhancedTrackerApproverExecutor(
            ticketRepository,
            configuration,
            null,
            transactionTemplate,
            storageKeyValueServiceSpy
        );
        executor.setExecutorService(new CurrentThreadExecutorService());

        deepmindYtProperties = new DeepmindYtProperties()
            .setMskuStatusTablePath("//home/market/production/deepmind/dictionaries/msku_status/latest")
            .setDcoUploadTableTablePath("home/market/production/mstat/analyst/regular/mbi/blue_prices/dco_upload_table")
            .setAssortmentSsku("//home/market/production/mstat/dictionaries/dynamic_pricing/assortment_ssku/latest")
            .setCubeMarketplaceOfferTablePath("//home/market/production/mstat/dwh/presentation/cube_marketplace_offer")
            .setModelsSkuTablePath("//home/market/production/mbo/export/recent/models/sku")
            .setTotalAssortmentTablePath("//home/market/users/MKDANALYTICS/blue_sources/total_assortment")
            .setDeadstockStatusTablePath(
                "//home/market/production/monetize/dynamic_pricing/deadstock_sales/deadstock_status/latest")
            .setCubeStockMovementTablePath("//home/market/production/mstat/dwh/presentation/" +
                "cube_calc_stock_movement_stock")
            .setCubeOrderItemDictTablePath("//home/market/production/mstat/analyst/regular/cubes_vertica" +
                "/cube_order_item_dict")
            .setFactUePartitionedTablePath("home/market/production/mstat/analyst/regular/cubes_vertica" +
                "/fact_ue_partitioned")
            .setMskuPricebandTablePath("//home/market/production/deepmind/dictionaries/msku_priceband/latest")
            .setMbocOffersExpandedSkuTablePath("//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest")
            .setFactNewOrderItemDictFlattenedTablePath("//home/market/production/mstat/analyst/regular/cubes_vertica" +
                "/fact_new_order_item_dict_flattened")
            .setPrepareEnrichToPendingTablePath("//home/market/production/deepmind/business_process/to_pending" +
                "/prepared_enrich");

        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(111L).setName("category1")));
        deepmindSupplierRepository.save(
            create1PSupplier(111, "000111"),
            create1PSupplier(222, "000222"),
            create1PSupplier(333, "000333"),
            create1PSupplier(444, "000444"),
            create1PSupplier(555, "000555"),
            create1PSupplier(666, "000666"),
            create1PSupplier(777, "000777"),
            create1PSupplier(888, "000888"),

            create1PSupplier(901, "000901"),
            create1PSupplier(902, "000902"),
            create1PSupplier(903, "000903"),
            create1PSupplier(904, "000904"),
            create1PSupplier(905, "000905")
        );
        deepmindMskuRepository.save(
            msku(111, 111),

            msku(99901, 111),
            msku(99902, 111),
            msku(99903, 111),
            msku(99904, 111),
            msku(99905, 111)
        );
        mskuInfoRepository.save(
            mskuInfo(99901),
            mskuInfo(99902),
            mskuInfo(99903),
            mskuInfoCorefix(99904),
            mskuInfoCorefix(99905),
            mskuInfoCorefix(111)
        );
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", 111, 111),
            offer(222, "shop-sku-222", 111, 111),
            offer(333, "shop-sku-333", 111, 111),
            offer(444, "shop-sku-444", 111, 111),
            offer(555, "shop-sku-555", 111, 111),
            offer(666, "shop-sku-666", 111, 111),
            offer(777, "shop-sku-777", 111, 111),
            offer(888, "shop-sku-888", 111, 111),

            offer(901, "shop-sku-111", 99901, 111),
            offer(902, "shop-sku-222", 99902, 111),
            offer(903, "shop-sku-333", 99903, 111),
            offer(904, "shop-sku-444", 99904, 111),
            offer(905, "shop-sku-555", 99905, 111),

            offer(901, "assort-sku-1", 99901, 111),
            offer(902, "assort-sku-2", 99902, 111),
            offer(903, "assort-sku-3", 99903, 111),
            offer(904, "assort-sku-4", 99904, 111),
            offer(905, "assort-sku-5", 99905, 111)
        );
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT_FOR_SPECIAL_ORDERS);
        seasonRepository.save(
            new Season(1L, "Сезон 1", Instant.now())
        );
    }
    protected List<EnrichApproveToPendingYtInfo> getEnrichApproveToPendingYtInfo() {
        var mskuIds = List.of(111L, 99901L, 99902L, 99903L, 99904L, 99905L);
        List<EnrichApproveToPendingYtInfo> list = new ArrayList<>();
        for (var mskuId : mskuIds) {
            var enrichApproveToPendingYtInfo = EnrichApproveToPendingYtInfo.builder()
                .purchasePrice(10d)
                .hid(12L)
                .mskuId(mskuId)
                .build();
            list.add(enrichApproveToPendingYtInfo);
        }
        return list;
    }

    protected Supplier create1PSupplier(int id, String rsId) {
        return new Supplier().setId(id).setName("test").setSupplierType(SupplierType.REAL_SUPPLIER)
            .setRealSupplierId(rsId);
    }

    protected SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability availability, String comment) {
        return sskuStatus(supplierId, shopSku, availability, comment, null);
    }

    protected SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability availability, String comment,
                                    Instant statusFinishAt) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(availability)
            .setComment(comment)
            .setStatusFinishAt(statusFinishAt)
            .setModifiedByUser(false);
    }

    protected MskuStatus mskuStatus(long mskuId, MskuStatusValue mskuStatus) {
        if (mskuStatus == MskuStatusValue.SEASONAL) {
            return new MskuStatus()
                .setMarketSkuId(mskuId)
                .setMskuStatus(mskuStatus)
                .setSeasonId(1L);
        }
        return new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(mskuStatus);
    }

    protected Msku msku(long mskuId, long categoryId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setCategoryId(categoryId)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setSkuType(SkuTypeEnum.SKU)
            .setDeleted(false);
    }

    protected MskuInfo mskuInfo(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(false);
    }

    protected MskuInfo mskuInfoCorefix(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(true);
    }

    protected ServiceOfferReplica offer(int supplierId, String ssku, long mskuId, long categoryId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    protected void makeCategorySeasonal(Long categoryId) {
        categoryRepository.insertOrUpdate(new Category().setCategoryId(categoryId).setName("category"));
        categorySettingsRepository.save(new CategorySettings(categoryId, 1L, Instant.now()));
    }

    protected ExcelFile createCorrectExcelFile(List<ServiceOfferKey> keys, List<Header> headers) {
        var names = headers.stream().map(Header::getName).collect(Collectors.toList());
        var builder = new ExcelFile.Builder().addHeaders(names);
        for (int i = 0; i < headerList.size(); i++) {
            //row with column number
            builder.setSubHeaderValue(1, i, i + 1);
        }
        int row = 1;
        for (ServiceOfferKey key : keys) {
            row++;
            builder.setValue(row, Headers.SHOP_SKU_KEY, key.getShopSku());
            builder.setValue(row, Headers.SUPPLIER_ID_KEY, key.getSupplierId());
        }
        return builder.build();
    }

    protected ExcelFile.Builder createCorrectExcelFileBuilderWithLegend(List<ServiceOfferKey> keys,
                                                                        List<Header> headers) {
        var names = headers.stream().map(Header::getName).collect(Collectors.toList());
        var builder = new ExcelFile.Builder().addHeaders(names);
        for (int i = 0; i < headerList.size(); i++) {
            //row with column number
            builder.setSubHeaderValue(1, i, i + 1);
        }
        int row = 1;
        for (ServiceOfferKey key : keys) {
            row++;
            builder.setValue(row, Headers.SHOP_SKU_KEY, key.getShopSku());
            builder.setValue(row, Headers.SUPPLIER_ID_KEY, key.getSupplierId());
        }
        BackgroundUtil.addLegend(builder);
        return builder;
    }

    protected ExcelFile createCorrectExcelFileWithLegend(List<ServiceOfferKey> keys, List<Header> headers) {
        var names = headers.stream().map(Header::getName).collect(Collectors.toList());
        var builder = new ExcelFile.Builder().addHeaders(names);
        for (int i = 0; i < headerList.size(); i++) {
            //row with column number
            builder.setSubHeaderValue(1, i, i + 1);
        }
        int row = 1;
        for (ServiceOfferKey key : keys) {
            row++;
            builder.setValue(row, Headers.SHOP_SKU_KEY, key.getShopSku());
            builder.setValue(row, Headers.SUPPLIER_ID_KEY, key.getSupplierId());
        }
        BackgroundUtil.addLegend(builder);
        return builder.build();
    }

    protected ExcelFile createNotCorrectExcelFile(List<ServiceOfferKey> keys) {
        String badHeader = "Bad header";
        if (headerList.contains(new Header(badHeader))) {
            throw new RuntimeException("Error while running business process test wrong header is allowed");
        }

        var builder = new ExcelFile.Builder()
            .addHeader(badHeader)
            .addHeader(Headers.SUPPLIER_ID_KEY);
        for (int i = 0; i < headerList.size(); i++) {
            //row with column number
            builder.setSubHeaderValue(1, i, i + 1);
        }
        int row = 1;
        for (ServiceOfferKey key : keys) {
            row++;
            builder.setValue(row, "Bad header", key.getShopSku());
            builder.setValue(row, Headers.SUPPLIER_ID_KEY, key.getSupplierId());
        }
        return builder.build();
    }

    protected BusinessProcessEconomicMetrics businessProcessMetric(String ticket, String bp) {
        return new BusinessProcessEconomicMetrics()
            .setTicket(ticket)
            .setBusinessProcess(bp);
    }

    protected ExcelFile getExcelFrom(String path) {
        try (var is = this.getClass().getClassLoader().getResourceAsStream(path)) {
            return ExcelFileConverter.convert(is, ExcelIgnoresConfigImpl.empty());
        } catch (IOException e) {
            throw new RuntimeException("File not found " + path);
        }
    }

    @SneakyThrows
    protected ExcelFile getExcelFrom(Workbook workbook) {
        try (var os = new ByteArrayOutputStream(1024)) {
            workbook.write(os);
            try (var is = new ByteArrayInputStream(os.toByteArray())) {
                return ExcelFileConverter.convert(is, ExcelIgnoresConfigImpl.empty());
            }
        }
    }
}
