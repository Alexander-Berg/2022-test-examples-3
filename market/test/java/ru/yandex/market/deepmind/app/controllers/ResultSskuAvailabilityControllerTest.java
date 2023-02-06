package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.google.common.base.Preconditions;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.jooq.impl.TableRecordImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumn;
import ru.yandex.market.deepmind.app.pojo.DisplayMsku;
import ru.yandex.market.deepmind.app.pojo.ExtendedResultAvailabilityFilter;
import ru.yandex.market.deepmind.app.pojo.ResultAvailability;
import ru.yandex.market.deepmind.app.pojo.ResultSskuAvailabilityExportRequest;
import ru.yandex.market.deepmind.app.web.DisplayMskuInfo;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilter.ExtendedMskuStatusValue;
import ru.yandex.market.deepmind.app.web.availability.DisplayMatrixAvailability;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.ExcelFileDownloader;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuKeyLastFilter;
import ru.yandex.market.deepmind.common.background.BackgroundExportService;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AvailabilityMatrixIndex;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalDictionary;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalMsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.hiding.DisplayHiding;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.mocks.GlobalVendorsCachingServiceMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalDictionaryRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaFilter;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.AssortType;
import ru.yandex.market.deepmind.common.services.MskuInfoFeatures;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceMock;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.app.offers.ExcelS3Context;
import ru.yandex.market.mboc.app.offers.S3UploadException;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.HIDING;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.IN_TARGET_ASSORTMENT;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.MSKU_ID;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.PRICEBAND_ID;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.PRICEBAND_LABEL;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.SEASONAL_MSKU;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.SSKU;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.SUPPLIER_ID;
import static ru.yandex.market.deepmind.app.exportable.ResultSskuAvailabilityExportColumnKey.WAREHOUSE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.BUSINESS;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_FAULTY_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45Y_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_REASON;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.ALL_CROSSDOCK;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.ALL_FULFILLMENT;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.SORTING_CENTER_1;
import static ru.yandex.market.deepmind.common.utils.excel.ExcelUtils.convertWarehouseToExcelHeader;

@SuppressWarnings("checkstyle:magicnumber")
public class ResultSskuAvailabilityControllerTest extends DeepmindBaseDbTestClass {
    private static final int BERU_ID = 465852;
    private static final long SUB_CATEGORY_ID = 42;
    private static final long ROOT_CATEGORY_ID = 12;
    private static final long VENDOR_ID = 23;
    private static final String VENDOR_NAME = "VendorName";
    private static final long CATEGORY_ID = 147;
    private static final String CATEGORY_NAME = "CategoryName";
    private static final Warehouse TOMILINO = new Warehouse().setId(TOMILINO_ID).setName("Томилино");

    private static final MboUser USER1 = new MboUser(12345, "Вася Пупкин", "agent007@y.ru");
    private static final MboUser USER2 = new MboUser(11111, "User 2", "user@y.ru");
    @Resource(name = "deepmindDsl")
    private DSLContext dslContext;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private MskuInfoRepository mskuInfoRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private HidingReasonDescriptionRepository hidingReasonDescriptionRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private HidingRepository hidingRepository;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private SeasonalDictionaryRepository seasonalDictionaryRepository;
    @Resource
    private SeasonalMskuRepository seasonalMskuRepository;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository categoryTeamRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    private ResultSskuAvailabilityController controller;

    private StorageKeyValueServiceMock deepmindStorageKeyValueService;
    private DeepmindCategoryCachingServiceMock categoryCachingService;
    private GlobalVendorsCachingServiceMock globalVendorsCachingService;
    private ShopSkuMatrixAvailabilityServiceMock shopSkuMatrixAvailabilityService;
    private BackgroundServiceMock backgroundServiceMock;
    private OffersConverter offersConverter;
    private ExcelS3ServiceMock excelS3Service;
    private ExcelFileDownloader excelFileDownloader;

    private EnhancedRandom random;

    private Msku msku;
    private Msku msku2;
    private Msku npdMsku;
    private Msku noStatusMsku;
    private Msku deletedStatusMsku;
    private DisplayMsku displayMsku;
    private DisplayMsku displayMsku2;
    private DisplayMsku npdDisplayMsku;
    private DisplayMsku displayNoStatusMsku;
    private DisplayMsku displayDeletedStatusMsku;
    private Season season;
    private MskuStatus mskuStatus;
    private Category rootCategory;
    private Category category;
    private Category subCategory;

    private ServiceOfferReplica offer11;
    private ServiceOfferReplica offer12;
    private ServiceOfferReplica offer21;
    private ServiceOfferReplica offer31;
    private ServiceOfferReplica offer32;
    private ServiceOfferReplica offer33;
    private List<Warehouse> warehouses;

    private HidingReasonDescription aboFaultyDescr;
    private HidingReasonDescription aboWrongGoodInfoDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription skk45YDescr;
    private HidingReasonDescription skk45JDescr;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(ALL_CROSSDOCK);
        warehouses = deepmindWarehouseRepository.findAllForMatrixAvailability();
        assertThat(warehouses).isNotEmpty();

        random = TestUtils.createMskuRandom();
        deepmindStorageKeyValueService = new StorageKeyValueServiceMock();
        categoryCachingService = new DeepmindCategoryCachingServiceMock();
        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        globalVendorsCachingService.addVendor(new CachedGlobalVendor(VENDOR_ID, VENDOR_NAME));

        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        rootCategory = randomCategory()
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setCategoryId(ROOT_CATEGORY_ID)
            .setName(CATEGORY_NAME + " root " + ROOT_CATEGORY_ID);
        category = randomCategory()
            .setParentCategoryId(rootCategory.getCategoryId())
            .setCategoryId(CATEGORY_ID)
            .setName(CATEGORY_NAME + " " + CATEGORY_ID);
        subCategory = randomCategory()
            .setParentCategoryId(category.getCategoryId())
            .setCategoryId(SUB_CATEGORY_ID)
            .setName(CATEGORY_NAME + " " + SUB_CATEGORY_ID);
        categoryCachingService.addCategory(rootCategory);
        categoryCachingService.addCategory(category);
        categoryCachingService.addCategory(subCategory);

        msku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID));
        msku2 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID));
        npdMsku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID).setTitle("Ноутбук Acer"));
        noStatusMsku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID).setTitle("noStatusMsku"));
        deletedStatusMsku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID).setTitle("deletedStatusMsku"));

        displayMsku = new DisplayMsku(msku, category.getName(), VENDOR_NAME);
        displayMsku2 = new DisplayMsku(msku2, category.getName(), VENDOR_NAME);
        npdDisplayMsku = new DisplayMsku(npdMsku, category.getName(), VENDOR_NAME);
        displayNoStatusMsku = new DisplayMsku(noStatusMsku, category.getName(), VENDOR_NAME);
        displayDeletedStatusMsku = new DisplayMsku(deletedStatusMsku, category.getName(), VENDOR_NAME);

        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            createOffer(1, "offer-11", msku),
            createOffer(1, "offer-12", msku2),
            createOffer(2, "offer-21", msku2),
            createOffer(3, "offer-31", npdMsku),
            createOffer(3, "offer-32", noStatusMsku),
            createOffer(3, "offer-33", deletedStatusMsku)
        );

        offer11 = serviceOfferReplicaRepository.findOfferByKey(1, "offer-11");
        offer12 = serviceOfferReplicaRepository.findOfferByKey(1, "offer-12");
        offer21 = serviceOfferReplicaRepository.findOfferByKey(2, "offer-21");
        offer31 = serviceOfferReplicaRepository.findOfferByKey(3, "offer-31");
        offer32 = serviceOfferReplicaRepository.findOfferByKey(3, "offer-32");
        offer33 = serviceOfferReplicaRepository.findOfferByKey(3, "offer-33");

        var mskuInfo1 = mskuInfo(offer21.getMskuId()).setPricebandId(111L).setPricebandLabel("123-345");
        var mskuInfo2 = mskuInfo(offer31.getMskuId()).setInTargetAssortment(true);
        mskuInfoRepository.save(mskuInfo1);
        mskuInfoRepository.save(mskuInfo2);

        var hidingsDescriptionMap = hidingReasonDescriptionRepository.save(
            createHidingDescription(SKK_REASON, "Другие проблемы"),
            createHidingDescription(ABO_REASON, ABO_FAULTY_SUBREASON, "Брак"),
            createHidingDescription(ABO_REASON, ABO_WRONG_GOOD_INFO_SUBREASON, ""),
            createHidingDescription(SKK_REASON, SKK_45K_SUBREASON, "Не важно что тут написано")
                .setReplaceWithDesc("cтоп слово"),
            createHidingDescription(SKK_REASON, SKK_45Y_SUBREASON, ""),
            createHidingDescription(SKK_REASON, SKK_45J_SUBREASON, "Предложение невозможно разместить на Маркете")
        ).stream().collect(Collectors.toMap(HidingReasonDescription::getReasonKey, Function.identity()));

        aboFaultyDescr = hidingsDescriptionMap.get(ABO_FAULTY_SUBREASON.toReasonKey());
        aboWrongGoodInfoDescr = hidingsDescriptionMap.get(ABO_WRONG_GOOD_INFO_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        skk45YDescr = hidingsDescriptionMap.get(SKK_45Y_SUBREASON.toReasonKey());
        skk45JDescr = hidingsDescriptionMap.get(SKK_45J_SUBREASON.toReasonKey());

        season = seasonRepository.save(new Season().setName("testSeason"));

        mskuStatus = mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(msku.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));
        mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(npdMsku.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));
        MskuStatus emptyStatus = mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(deletedStatusMsku.getId())
            .setMskuStatus(MskuStatusValue.EMPTY));

        shopSkuMatrixAvailabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        backgroundServiceMock = new BackgroundServiceMock();

        excelS3Service = Mockito.spy(new ExcelS3ServiceMock());
        excelFileDownloader = new ExcelFileDownloader(backgroundServiceMock, excelS3Service);
        offersConverter = new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository);
        controller = new ResultSskuAvailabilityController(
            dslContext,
            serviceOfferReplicaRepository,
            deepmindMskuRepository,
            mskuInfoRepository,
            shopSkuMatrixAvailabilityService,
            categoryCachingService,
            globalVendorsCachingService,
            mskuStatusRepository,
            seasonRepository,
            offersConverter,
            deepmindCategoryManagerRepository,
            categoryTeamRepository,
            deepmindWarehouseRepository,
            deepmindStorageKeyValueService,
            mskuStockRepository,
            new HidingController(hidingRepository, null),
            deepmindSupplierRepository,
            new DeepmindCargoTypeCachingServiceMock(),
            new BackgroundExportService(backgroundServiceMock, transactionTemplate, excelS3Service),
            sskuStatusRepository,
            seasonalDictionaryRepository,
            seasonalMskuRepository
        );
        controller.setExcelBatchSize(2);
        controller.setExcelMaxRows(10);
        SecurityContextAuthenticationUtils.setAuthenticationToken();
    }

    @After
    public void tearDown() {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
        controller.setExcelBatchSize(ResultSskuAvailabilityController.EXCEL_BATCH_SIZE);
        controller.setExcelMaxRows(ResultSskuAvailabilityController.EXCEL_MAX_ROWS);
    }

    @Test
    public void testWithoutFilterParams() {
        ExtendedResultAvailabilityFilter filter = new ExtendedResultAvailabilityFilter();
        List<ResultAvailability> list = controller.listWithBigFilter(filter, ShopSkuKeyLastFilter.all());

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12, offer21, offer31, offer32, offer33);
    }

    @Test
    public void testReturnOnlyOffersWithMappings() {
        Msku newMsku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID));
        DeepmindAssertions.assertThat(offer11)
            .hasApprovedMapping(msku.getId());

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(msku.getId(), newMsku.getId()),
            ShopSkuKeyLastFilter.all()
        );

        assertThat(list)
            .extracting(ResultAvailability::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(displayMsku);
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11);
    }

    @Test
    public void testListWithMskuInfo() {
        var mskuInfo = mskuInfo(msku.getId());
        mskuInfoRepository.save(mskuInfo);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuIds(msku.getId()), ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(displayMsku);
        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactly(mskuInfo);
    }

    @Test
    public void testListOffersWithCorefixFilter() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L), msku(444555L));
        var mskuInfo1 = mskuInfo(111222L);
        var mskuInfo2 = mskuInfo(333444L);
        var mskuInfo3 = mskuInfo(444555L).setInTargetAssortment(true);
        insertOffer(1, "ssku-1", OfferAvailability.ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", OfferAvailability.DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-2", OfferAvailability.INACTIVE, mskuInfo3.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(true)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactly(mskuInfo3);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures()),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactly(mskuInfo1, mskuInfo2, mskuInfo3);
    }

    @Test
    public void testListOffersWithPriceFilter() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L), msku(444555L));
        var mskuInfo1 = mskuInfo(111222L).setPricebandId(123L);
        var mskuInfo2 = mskuInfo(333444L).setPricebandId(123L).setPricebandLabel("123_label");
        var mskuInfo3 = mskuInfo(444555L)
            .setInTargetAssortment(true).setPricebandId(234L).setPricebandLabel("234_label");
        insertOffer(1, "ssku-1", OfferAvailability.ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", OfferAvailability.DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-2", OfferAvailability.INACTIVE, mskuInfo3.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        var list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandId(123L)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandLabel("234_label")),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandId(123L).setPricebandLabel("234_label")),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .isEmpty();

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(true).setPricebandId(234L)
                    .setPricebandLabel("234_label")),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);
    }

    @Test
    public void testListOffersWithPriceLimitsFilter() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L), msku(444555L));
        var mskuInfo1 = mskuInfo(111222L).setPrice(300.0).setPricebandId(111L);
        var mskuInfo2 = mskuInfo(333444L).setPrice(1300.0).setPricebandId(111L);
        var mskuInfo3 = mskuInfo(444555L).setPrice(5000.0).setPricebandId(222L);
        insertOffer(1, "ssku-1", OfferAvailability.ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", OfferAvailability.DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-2", OfferAvailability.INACTIVE, mskuInfo3.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        var list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(200.0)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2, mskuInfo3);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(350.0).setToPriceInclusive(3000.0)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo2);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(300.0).setToPriceInclusive(1300.0)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setToPriceInclusive(3000.0)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(111222L, 333444L, 444555L)
                .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(200.0).setPricebandId(111L)),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);
    }

    @Test
    public void testFilter() {
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(msku.getId(), msku2.getId()),
            ShopSkuKeyLastFilter.limit(1)
        );

        assertThat(list).hasSize(1);
        assertThat(list)
            .extracting(ResultAvailability::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(displayMsku);
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setMskuIds(msku.getId(), msku2.getId()),
            ShopSkuKeyLastFilter.all()
        );

        assertThat(list).hasSize(3);
        assertThat(list)
            .extracting(ResultAvailability::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(displayMsku, displayMsku2, displayMsku2);
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12, offer21);
    }

    @Test
    public void testSearchByShopSku() {
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setShopSkus(offer11.getShopSku(), offer12.getShopSku()),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12);
    }

    @Test
    public void testSearchByRealSupplierIdShopSku() {
        serviceOfferReplicaRepository.save(
            createOffer(77, "offer-77", msku).setSupplierType(REAL_SUPPLIER),
            createOffer(1, "offer.with.dots", msku)
        );
        var offer77 = serviceOfferReplicaRepository.findOfferByKey(77, "offer-77");
        var offer1 = serviceOfferReplicaRepository.findOfferByKey(1, "offer.with.dots");

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setShopSkus(offer11.getShopSku(), "000042.offer-77", "offer.with.dots"),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactlyInAnyOrder(
                offer11,
                offer1,
                offer77
            );
    }

    @Test
    public void testSskuStatus() {
        serviceOfferReplicaRepository.save(
            createOffer(77, "offer-77", msku).setSupplierType(REAL_SUPPLIER),
            createOffer(1, "offer-1", msku)
        );
        sskuStatusRepository.save(
            new SskuStatus().setSupplierId(77).setShopSku("offer-77").setModifiedByUser(false)
                .setAvailability(OfferAvailability.ACTIVE),
            new SskuStatus().setSupplierId(1).setShopSku("offer-1")
                .setAvailability(OfferAvailability.INACTIVE).setModifiedByUser(true)
                .setStatusFinishAt(Instant.parse("2021-12-03T10:15:30.00Z"))
        );

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setShopSkuKeys(
                new ServiceOfferKey(77, "offer-77"), new ServiceOfferKey(1, "offer-1")
            ), ShopSkuKeyLastFilter.all());

        assertThat(list)
            .<ImmutablePair<?, ?>>extracting(it -> ImmutablePair.of(it.getSskuStatus(), it.getSskuStatusFinishAt()))
            .containsExactlyInAnyOrder(
                ImmutablePair.of(OfferAvailability.ACTIVE, null),
                ImmutablePair.of(OfferAvailability.INACTIVE, Instant.parse("2021-12-03T10:15:30.00Z"))
            );
    }

    @Test
    public void testSomeAvailabilities() {
        MatrixAvailability mskuTrue = MatrixAvailabilityUtils.mskuInWarehouse(true, msku, TOMILINO, null, null, null);
        MatrixAvailability mskuFalse = MatrixAvailabilityUtils.mskuInWarehouse(false, this.msku, TOMILINO, null,
            null, null);
        shopSkuMatrixAvailabilityService.addAvailability(offer11, TOMILINO_ID, mskuTrue);
        shopSkuMatrixAvailabilityService.addAvailability(offer11, CROSSDOCK_SOFINO_ID, mskuFalse);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setShopSkuKeys(offer11.getServiceOfferKey(), offer33.getServiceOfferKey()),
            ShopSkuKeyLastFilter.all()
        );

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer33);
        ResultAvailability resultAvailability = list.get(0);

        for (Warehouse warehouse : warehouses) {
            List<DisplayMatrixAvailability> availabilities = resultAvailability.getAvailabilities(warehouse.getId());
            if (warehouse.getId() == CROSSDOCK_SOFINO_ID) {
                assertThat(availabilities)
                    .extracting(DisplayMatrixAvailability::getFullText)
                    .containsExactly(mskuFalse.render());
                assertThat(availabilities)
                    .extracting(DisplayMatrixAvailability::getShortText)
                    .containsExactly(mskuFalse.shortRender());
            }
        }
    }

    @Test
    public void testAvailabilitiesForCrossdock() {
        MatrixAvailability matrixAvailability = MatrixAvailabilityUtils
            .mskuInWarehouse(
                false,
                msku,
                new Warehouse()
                    .setId(CROSSDOCK_SOFINO_ID)
                    .setName("Crossdock"),
                null,
                null,
                null
            );

        shopSkuMatrixAvailabilityService.addAvailability(offer11, CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.offerDelisted(offer11.getServiceOfferKey()));
        shopSkuMatrixAvailabilityService.addAvailability(offer11, CROSSDOCK_SOFINO_ID, matrixAvailability);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setShopSkuKeys(offer11.getServiceOfferKey()),
            ShopSkuKeyLastFilter.all()
        );

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11);
        ResultAvailability resultAvailability = list.get(0);

        assertThat(resultAvailability.getAvailabilities(CROSSDOCK_SOFINO_ID))
            .extracting(DisplayMatrixAvailability::getFullText)
            .containsExactly(matrixAvailability.render());
    }

    @Test
    public void testListRegularStatus() {
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuIds(msku.getId()), ShopSkuKeyLastFilter.all());

        ResultAvailability result = list.get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getNpdMsku()).isNull();
        assertThat(result.getSeason()).isNull();
        assertThat(result.getAvailabilitiesByWarehouseId().values()).flatExtracting(l -> l)
            .isEmpty();
        assertThat(result.getStatus()).isEqualToComparingFieldByField(mskuStatus.getMskuStatus());
    }

    @Test
    public void testListSeasonalStatus() {
        mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL);
        mskuStatus.setSeasonId(season.getId());
        mskuStatusRepository.save(mskuStatus);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuIds(msku.getId()), ShopSkuKeyLastFilter.all());

        ResultAvailability result = list.get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getNpdMsku()).isNull();
        assertThat(result.getSeason()).isEqualToComparingFieldByField(season);
        assertThat(result.getAvailabilitiesByWarehouseId().values()).flatExtracting(l -> l)
            .isEmpty();
        assertThat(result.getStatus()).isEqualToComparingFieldByField(mskuStatus.getMskuStatus());
    }

    @Test
    public void testListNpdStatus() {
        mskuStatus.setMskuStatus(MskuStatusValue.NPD);
        mskuStatus.setNpdStartDate(LocalDate.now());
        mskuStatusRepository.save(mskuStatus);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuIds(msku.getId()), ShopSkuKeyLastFilter.all());

        ResultAvailability result = list.get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getSeason()).isNull();
        assertThat(result.getAvailabilitiesByWarehouseId().values()).flatExtracting(l -> l)
            .isEmpty();
        assertThat(result.getStatus()).isEqualToComparingFieldByField(mskuStatus.getMskuStatus());
    }

    @Test
    public void testListNoStatus() {
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.EMPTY));

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuIds(msku.getId()), ShopSkuKeyLastFilter.all());

        ResultAvailability result = list.get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getNpdMsku()).isNull();
        assertThat(result.getSeason()).isNull();
        assertThat(result.getAvailabilitiesByWarehouseId().values()).flatExtracting(l -> l)
            .isEmpty();
        assertThat(result.getStatus()).isEqualTo(DisplayMskuInfo.DisplayMskuStatusValue.EMPTY);
    }

    @Test
    public void testFilterByMskuTitle() {
        if (SystemUtils.IS_OS_WINDOWS) {
            // Encoding settings of embedded postgres does not allow to search russian text on Windows.
            return;
        }
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuSearchText("ноутбук"), ShopSkuKeyLastFilter.all());

        ResultAvailability result = list.get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(npdDisplayMsku);
    }

    @Test
    public void testFilterByMskuTitleWithCapital() {
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuSearchText("Ноутбук"), ShopSkuKeyLastFilter.all());

        ResultAvailability result = list.get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(npdDisplayMsku);
    }

    @Test
    public void testFilterByCategoryManager() {
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(CATEGORY_ID).setStaffLogin("pupkin").setRole(CATMAN)
                .setFirstName("").setLastName(""),
            new CategoryManager().setCategoryId(SUB_CATEGORY_ID).setStaffLogin("pupkin").setRole(CATMAN)
                .setFirstName("").setLastName("")
        );

        categoryCachingService.addCategory(randomCategory()
            .setParentCategoryId(rootCategory.getCategoryId())
            .setCategoryId(163)
            .setName(CATEGORY_NAME + " 163"));

        var msku42 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID).setCategoryId(SUB_CATEGORY_ID));
        serviceOfferReplicaRepository.save(createOffer(42, "offer-42", msku42));

        var offer42 = serviceOfferReplicaRepository.findOfferByKey(42, "offer-42");

        var msku163 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID).setCategoryId(163L));
        serviceOfferReplicaRepository.save(createOffer(42, "offer-163", msku163));

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setCategoryManagerLogin("pupkin"), ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12, offer21, offer31, offer32, offer33, offer42);
    }

    @Test
    public void testFilterByRootCategoryId() {
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(CATEGORY_ID).setStaffLogin("pupkin").setRole(CATMAN)
                .setFirstName("").setLastName("")
        );

        Msku msku42 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID).setCategoryId(SUB_CATEGORY_ID));
        serviceOfferReplicaRepository.save(createOffer(42, "offer-42", msku42));
        var offer42 = serviceOfferReplicaRepository.findOfferByKey(42, "offer-42");

        List<ResultAvailability> list1 = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setHierarchyCategoryIds(SUB_CATEGORY_ID),
            ShopSkuKeyLastFilter.all());

        assertThat(list1)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer42);

        List<ResultAvailability> list2 = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setHierarchyCategoryIds(CATEGORY_ID),
            ShopSkuKeyLastFilter.all());

        assertThat(list2)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12, offer21, offer31, offer32,
                offer33, offer42);
    }

    @Test
    public void testFindByStatus() {
        mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(msku2.getId())
            .setMskuStatus(MskuStatusValue.ARCHIVE));

        List<ResultAvailability> listRegular = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setMskuStatusValue(ExtendedMskuStatusValue.REGULAR),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(listRegular)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer31);

        List<ResultAvailability> listArchive = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setMskuStatusValue(ExtendedMskuStatusValue.ARCHIVE),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(listArchive)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer12, offer21);

        List<ResultAvailability> listEndOfLife = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setMskuStatusValue(ExtendedMskuStatusValue.END_OF_LIFE),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(listEndOfLife).isEmpty();

        List<ResultAvailability> listWithStatus = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setMskuStatusValue(ExtendedMskuStatusValue.WITH_ANY_STATUS),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(listWithStatus)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12, offer21, offer31);

        List<ResultAvailability> listWithoutStatus = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setMskuStatusValue(ExtendedMskuStatusValue.WITHOUT_STATUS),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(listWithoutStatus)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer32, offer33);
    }

    @Test
    public void testExtendedMskuStatusValueEnumCorrelation() {
        // test no fails
        controller.listWithBigFilter(new ExtendedResultAvailabilityFilter().setCategoryIds(CATEGORY_ID),
            ShopSkuKeyLastFilter.all());
        for (ExtendedMskuStatusValue statusValue : ExtendedMskuStatusValue.values()) {
            controller.listWithBigFilter(
                new ExtendedResultAvailabilityFilter()
                    .setCategoryIds(CATEGORY_ID)
                    .setMskuStatusValue(statusValue),
                ShopSkuKeyLastFilter.all()
            );
        }
    }

    @Test
    public void testFilterBySupplierId() {
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setSupplierIds(2, 3), ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer21, offer31, offer32, offer33);
    }

    @Test
    public void testFilterByEverySupplierType() {
        var suppliers = deepmindSupplierRepository.findByIdsMap(List.of(1, 2, 3));
        suppliers.get(1).setSupplierType(REAL_SUPPLIER).setRealSupplierId("0054");
        suppliers.get(2).setSupplierType(THIRD_PARTY);
        deepmindSupplierRepository.save(suppliers.values());

        // test won't fail
        for (SupplierType supplierType : SupplierType.values()) {
            controller.listWithBigFilter(
                new ExtendedResultAvailabilityFilter()
                    .setCategoryIds(CATEGORY_ID)
                    .setSupplierType(supplierType),
                ShopSkuKeyLastFilter.all());
        }
    }

    @Test
    public void testFilterBySupplierType() {
        updateSupplierType(1, REAL_SUPPLIER, "0054");
        updateSupplierType(2, THIRD_PARTY, null);
        updateSupplierType(3, BUSINESS, null);

        // real
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setSupplierType(SupplierType.REAL_SUPPLIER),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12);

        // third
        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setSupplierType(SupplierType.THIRD_PARTY),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer21);

        // fmcg
        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setSupplierType(BUSINESS),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer31, offer32, offer33);
    }

    @Test
    public void testFilterBySupplierTypeWithSupplierId() {
        updateSupplierType(1, REAL_SUPPLIER, "0054");
        updateSupplierType(2, REAL_SUPPLIER, "0054");
        updateSupplierType(3, BUSINESS, null);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setCategoryIds(CATEGORY_ID)
                .setSupplierType(SupplierType.REAL_SUPPLIER)
                .setSupplierIds(2, 3),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer21);
    }

    @Test
    public void testFilterByAvailabilitiesOnAnyWarehouseId() {
        insertIndexRows(
            indexRow(1, "offer-11", TOMILINO_ID),
            indexRow(1, "offer-11", ROSTOV_ID),
            indexRow(3, "offer-31", ROSTOV_ID),
            indexRow(3, "offer-32", MARSHRUT_ID)
        );

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setContainsAvailabilitiesOnAnyWarehouseId(true),
            ShopSkuKeyLastFilter.all()
        );

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer31, offer32);
    }

    @Test
    public void testFilterByAvailabilitiesOnNotAnyWarehouseId() {
        insertIndexRows(
            indexRow(1, "offer-11", TOMILINO_ID),
            indexRow(1, "offer-11", ROSTOV_ID),
            indexRow(3, "offer-31", ROSTOV_ID),
            indexRow(3, "offer-32", MARSHRUT_ID)
        );

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setDoesntContainAvailabilitiesOnAnyWarehouseId(true),
            ShopSkuKeyLastFilter.all()
        );

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer12, offer21, offer33);
    }

    @Test
    public void testFilterByAvailabilitiesOnWarehouseId() {
        insertIndexRows(
            indexRow(1, "offer-11", TOMILINO_ID),
            indexRow(1, "offer-11", ROSTOV_ID),
            indexRow(3, "offer-31", ROSTOV_ID),
            indexRow(3, "offer-32", MARSHRUT_ID)
        );

        // on tomilino
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setContainsAvailabilitiesOnWarehouseIds(TOMILINO_ID),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11);

        // on rostov
        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setContainsAvailabilitiesOnWarehouseIds(ROSTOV_ID),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer31);

        // on rostov and tomilino
        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setContainsAvailabilitiesOnWarehouseIds(ROSTOV_ID, TOMILINO_ID),
            ShopSkuKeyLastFilter.all()
        );
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11);
    }

    @Test
    public void testFilterByAnyHidings() {
        Hiding hiding1 = createHiding(skk45KDescr.getId(), "героин", offer11, USER1, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer12, USER1, null);
        Hiding hiding3 = createHiding(aboFaultyDescr.getId(), "Faulty", offer12, USER1, null);
        Hiding hiding4 = createHiding(skk45JDescr.getId(), "45j", offer12, USER1, null);

        hidingRepository.save(hiding1, hiding2, hiding3, hiding4);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setContainsAnyHiding(true)
                .setHidingReasonKeys("tra-la-la", "this-will-be-ignores"),
            ShopSkuKeyLastFilter.all()
        );

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12);
        Assertions.assertThat(list.get(0).getHidings())
            .containsExactlyInAnyOrder(
                createDisplayHiding(hiding1, "Стоп слово - героин")
            );
        Assertions.assertThat(list.get(1).getHidings())
            .containsExactlyInAnyOrder(
                createDisplayHiding(hiding2, "Стоп слово - жопа"),
                createDisplayHiding(hiding3, "Брак"),
                createDisplayHiding(hiding4, "Предложение невозможно разместить на Маркете")
            );
    }

    @Test
    public void testFilterBySubreasonsHidings() {
        Hiding hiding1 = createHiding(skk45KDescr.getId(), "героин", offer11, USER1, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer12, USER1, null);
        Hiding hiding3 = createHiding(aboFaultyDescr.getId(), "Faulty", offer12, USER1, null);
        Hiding hiding4 = createHiding(skk45YDescr.getId(), "45j", offer12, USER1, null);
        Hiding hiding5 = createHiding(skk45YDescr.getId(), "45j", offer31, USER1, null);
        Hiding hiding6 = createHiding(aboWrongGoodInfoDescr.getId(), "45j", offer32, USER1, null);

        hidingRepository.save(hiding1, hiding2, hiding3, hiding4, hiding5, hiding6);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setHidingReasonKeys(ABO_FAULTY_SUBREASON, SKK_45K_SUBREASON),
            ShopSkuKeyLastFilter.all()
        );

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer12);
    }

    @Test
    public void testFilterBySubreasonsAndStopWordsHidings() {
        Hiding hiding1 = createHiding(skk45KDescr.getId(), "героин", offer11, USER1, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer12, USER1, null);
        Hiding hiding3 = createHiding(aboFaultyDescr.getId(), "Faulty", offer21, USER1, null);
        Hiding hiding4 = createHiding(skk45YDescr.getId(), "45j", offer31, USER1, null);
        Hiding hiding5 = createHiding(aboFaultyDescr.getId(), "Faulty", offer32, USER1, null);

        hidingRepository.save(hiding1, hiding2, hiding3, hiding4, hiding5);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setHidingReasonKeys(ABO_FAULTY_SUBREASON, SKK_45K_SUBREASON)
                .setHidingStopWords("жопа"),
            ShopSkuKeyLastFilter.all()
        );

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer12, offer21, offer32);
    }

    @Test
    public void testFilterByStopWordsHidings() {
        Hiding hiding1 = createHiding(skk45KDescr.getId(), "героин", offer11, USER1, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer12, USER1, null);
        Hiding hiding3 = createHiding(aboFaultyDescr.getId(), "Faulty", offer21, USER1, null);
        Hiding hiding4 = createHiding(skk45YDescr.getId(), "45j", offer31, USER1, null);
        Hiding hiding5 = createHiding(aboFaultyDescr.getId(), "Faulty", offer32, USER1, null);

        hidingRepository.save(hiding1, hiding2, hiding3, hiding4, hiding5);

        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setHidingReasonKeys(SKK_45K_SUBREASON)
                .setHidingStopWords("жопа"),
            ShopSkuKeyLastFilter.all()
        );

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer12);
    }

    @Test
    public void testFilterBySearchHidings() {
        if (SystemUtils.IS_OS_WINDOWS) {
            // Encoding settings of embedded postgres does not allow to search russian text on Windows.
            return;
        }
        Hiding hiding1 = createHiding(skk45KDescr.getId(), "героин", offer11, USER2, "ё TRATA-ta");
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer12, USER2, null);
        Hiding hiding3 = createHiding(aboFaultyDescr.getId(), "Faulty", offer12, USER1, null);
        Hiding hiding4 = createHiding(skk45YDescr.getId(), "45j", offer12, USER1, null);

        hidingRepository.save(hiding1, hiding2, hiding3, hiding4);

        // by comment
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setHidingSearchText(" Ё trata"),
            ShopSkuKeyLastFilter.all()
        );

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11);

        // by user
        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setHidingSearchText(" пупкин      "),
            ShopSkuKeyLastFilter.all()
        );

        Assertions.assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer12);
    }

    @Test
    public void testSearchByContainsOnStock() {
        deepmindWarehouseRepository.save(
            new Warehouse().setId(21L).setName("Дропшип склад 1").setType(WarehouseType.DROPSHIP),
            new Warehouse().setId(22L).setName("Дропшип склад 2").setType(WarehouseType.DROPSHIP),
            new Warehouse().setId(23L).setName("Дропшип склад 3").setType(WarehouseType.DROPSHIP)
        );

        mskuStockRepository.insertBatch(
            // offer 11
            createStockInfo(offer11, MARSHRUT_ID, 0),
            createStockInfo(offer11, TOMILINO_ID, 1),
            createStockInfo(offer11, SOFINO_ID, 7),
            createStockInfo(offer11, 21, 0),
            createStockInfo(offer11, 22, 0),
            createStockInfo(offer11, 23, 17),

            // offer 12
            createStockInfo(offer12, TOMILINO_ID, 0),

            // offer 21
            createStockInfo(offer21, SOFINO_ID, 100500),
            createStockInfo(offer21, 21, 1),
            createStockInfo(offer21, 22, 5),

            // offer 31
            createStockInfo(offer31, 21, 1),
            createStockInfo(offer31, 22, 1),
            createStockInfo(offer31, 23, 1)
        );

        // number check
        List<ResultAvailability> list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setShopSkuKeys(offer11.getServiceOfferKey(), offer12.getServiceOfferKey(),
                    offer21.getServiceOfferKey(), offer31.getServiceOfferKey()),
            ShopSkuKeyLastFilter.all());
        Assertions.assertThat(list)
            .extracting(ResultAvailability::getFulfillmentFit)
            .containsExactly(8, 0, 100500, 0);
        Assertions.assertThat(list)
            .extracting(ResultAvailability::getDropshipFit)
            .containsExactly(17, 0, 6, 3);

        // ff check
        List<ResultAvailability> list1 = controller.listWithBigFilter(new ExtendedResultAvailabilityFilter()
            .setContainsOnFulfillmentStocks(true), ShopSkuKeyLastFilter.all());

        Assertions.assertThat(list1)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer21);

        // dropship check
        List<ResultAvailability> list2 = controller.listWithBigFilter(new ExtendedResultAvailabilityFilter()
            .setContainsOnDropshipStocks(true), ShopSkuKeyLastFilter.all());

        Assertions.assertThat(list2)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer21, offer31);

        // together check
        List<ResultAvailability> list3 = controller.listWithBigFilter(new ExtendedResultAvailabilityFilter()
            .setContainsOnDropshipStocks(true).setContainsOnFulfillmentStocks(true), ShopSkuKeyLastFilter.all());

        Assertions.assertThat(list3)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(offer11, offer21, offer31);
    }

    @Test
    public void testSearchByContainsOnStockWithZeroResult() {
        List<ResultAvailability> list = controller.listWithBigFilter(new ExtendedResultAvailabilityFilter()
            .setContainsOnFulfillmentStocks(true), ShopSkuKeyLastFilter.all());

        Assertions.assertThat(list).isEmpty();
    }

    @Test
    public void testDownloadFileSuccessfully() {
        MatrixAvailability matrixAvailability = MatrixAvailabilityUtils.offerDelisted(offer21.getServiceOfferKey());
        shopSkuMatrixAvailabilityService.addAvailability(offer21, MARSHRUT_ID, matrixAvailability);

        seasonalDictionaryRepository.save(
            new SeasonalDictionary().setId(1L).setName("Новогодний"),
            new SeasonalDictionary().setId(2L).setName("Зимний")
        );
        seasonalMskuRepository.save(
            new SeasonalMsku().setMskuId(offer21.getMskuId()).setSeasonalId(1L),
            new SeasonalMsku().setMskuId(offer31.getMskuId()).setSeasonalId(1L),
            new SeasonalMsku().setMskuId(offer31.getMskuId()).setSeasonalId(2L)
        );

        var request = new ResultSskuAvailabilityExportRequest()
            .setSupplierIds(2, 3);
        int exportId = controller.startExportWithBigFilter(request);
        ExcelFile file = excelFileDownloader.downloadExport(exportId);

        DeepmindAssertions.assertThat(file)
            // order of lines is important
            // offer21
            .containsValue(1, SUPPLIER_ID.getHeader(), offer21.getBusinessId())
            .containsValue(1, SSKU.getHeader(), offer21.getShopSku())
            .containsValue(1, MSKU_ID.getHeader(), offer21.getMskuId())
            .containsValue(1, "Маршрут (Котельники) [#145]", "1. " + matrixAvailability.shortRender())
            .containsValue(1, "Яндекс.Маркет (Ростов-на-Дону) [#147]", "")
            .containsValue(1, "Яндекс.Маркет (Томилино) [#171]", "")
            .containsValue(1, "Яндекс.Маркет (Софьино) [#172]", "")
            .containsValue(1, IN_TARGET_ASSORTMENT.getHeader(), "false")
            .containsValue(1, PRICEBAND_ID.getHeader(), "111")
            .containsValue(1, PRICEBAND_LABEL.getHeader(), "123-345")
            .containsValue(1, SEASONAL_MSKU.getHeader(), "Новогодний")
            // offer31
            .containsValue(2, SUPPLIER_ID.getHeader(), offer31.getBusinessId())
            .containsValue(2, SSKU.getHeader(), offer31.getShopSku())
            .containsValue(2, MSKU_ID.getHeader(), offer31.getMskuId())
            .containsValue(2, "Маршрут (Котельники) [#145]", "")
            .containsValue(2, "Яндекс.Маркет (Ростов-на-Дону) [#147]", "")
            .containsValue(2, "Яндекс.Маркет (Томилино) [#171]", "")
            .containsValue(2, "Яндекс.Маркет (Софьино) [#172]", "")
            .containsValue(2, IN_TARGET_ASSORTMENT.getHeader(), "true")
            .containsValue(2, SEASONAL_MSKU.getHeader(), "Новогодний, Зимний")
            // offer32
            .containsValue(3, SUPPLIER_ID.getHeader(), offer32.getBusinessId())
            .containsValue(3, SSKU.getHeader(), offer32.getShopSku())
            .containsValue(3, MSKU_ID.getHeader(), offer32.getMskuId())
            .containsValue(3, "Маршрут (Котельники) [#145]", "")
            .containsValue(3, "Яндекс.Маркет (Ростов-на-Дону) [#147]", "")
            .containsValue(3, "Яндекс.Маркет (Томилино) [#171]", "")
            .containsValue(3, "Яндекс.Маркет (Софьино) [#172]", "")
            .containsValue(3, IN_TARGET_ASSORTMENT.getHeader(), "false")
            .containsValue(3, SEASONAL_MSKU.getHeader(), "")
            .hasLastLine(4);
    }

    @Test
    public void testExportWithSelectedColumnsAndFilter() {
        MatrixAvailability matrixAvailability = MatrixAvailabilityUtils.offerDelisted(offer21.getServiceOfferKey());
        shopSkuMatrixAvailabilityService.addAvailability(offer21, MARSHRUT_ID, matrixAvailability);

        var filter = new ExtendedResultAvailabilityFilter().setShopSkus(
            offer21.getShopSku(), offer31.getShopSku(), offer32.getShopSku()
        );

        var request = new ResultSskuAvailabilityExportRequest()
            .setFilter(filter)
            .setColumns(List.of(
                new ResultSskuAvailabilityExportColumn().setKey(SUPPLIER_ID),
                new ResultSskuAvailabilityExportColumn().setKey(SSKU),
                new ResultSskuAvailabilityExportColumn().setKey(HIDING),
                new ResultSskuAvailabilityExportColumn().setKey(WAREHOUSE), // без warehouseId будет проигнорирована
                new ResultSskuAvailabilityExportColumn().setKey(WAREHOUSE).setWarehouseId(ROSTOV_ID),
                new ResultSskuAvailabilityExportColumn().setKey(WAREHOUSE).setWarehouseId(SOFINO_ID)
            ));

        int exportId = controller.startExportWithBigFilter(request);
        ExcelFile file = excelFileDownloader.downloadExport(exportId);

        DeepmindAssertions.assertThat(file)
            .containsHeadersExactly(
                SUPPLIER_ID.getHeader(),
                SSKU.getHeader(),
                HIDING.getHeader(),
                "Яндекс.Маркет (Ростов-на-Дону) [#147]",
                "Яндекс.Маркет (Софьино) [#172]"
            )
            .hasLastLine(3)
            // offer21
            .containsValue(1, SUPPLIER_ID.getHeader(), offer21.getBusinessId())
            .containsValue(1, SSKU.getHeader(), offer21.getShopSku())
            // offer31
            .containsValue(2, SUPPLIER_ID.getHeader(), offer31.getBusinessId())
            .containsValue(2, SSKU.getHeader(), offer31.getShopSku())
            // offer32
            .containsValue(3, SUPPLIER_ID.getHeader(), offer32.getBusinessId())
            .containsValue(3, SSKU.getHeader(), offer32.getShopSku());
    }

    @Test
    public void testUploadFailed() {
        Mockito.doThrow(new S3UploadException("Failed to upload file in unit test"))
            .when(excelS3Service).uploadAsExportFile(Mockito.any(ExcelS3Context.class));

        var request = new ResultSskuAvailabilityExportRequest().setSupplierIds(2, 3);
        int exportId = controller.startExportWithBigFilter(request);

        Assertions.assertThatThrownBy(() -> {
            excelFileDownloader.downloadExport(exportId);
        }).hasMessageContaining("Произошла ошибка. Напишите разработчикам.: " +
            "S3UploadException: Failed to upload file in unit test");
    }

    @Test
    public void testGeneratedFileIsTooLong() {
        controller.setExcelMaxRows(2);

        // проверяем, что количество записей, подходящих под условие больше максимального размера батча
        var request = new ResultSskuAvailabilityExportRequest().setSupplierIds(2, 3);
        List<ResultAvailability> list = controller.listWithBigFilter(request.getFilter(), ShopSkuKeyLastFilter.all());
        assertThat(list.size()).isGreaterThan(2);

        int exportId = controller.startExportWithBigFilter(request);

        Assertions.assertThatThrownBy(() -> {
            excelFileDownloader.downloadExport(exportId);
        }).hasMessageContaining("Файл получается слишком большим (2+ строк)");
    }

    @Test
    public void testCorrectlyGenerateFileWithBusinessOffers() {
        controller.setExcelBatchSize(3);

        serviceOfferReplicaRepository.save(
            createOffer(100, "offer", msku).setSupplierId(101),
            createOffer(100, "offer", msku).setSupplierId(102),
            createOffer(200, "offer", msku).setSupplierId(201),
            createOffer(200, "offer", msku).setSupplierId(202)
        );

        int exportId = controller.startExportWithBigFilter(new ResultSskuAvailabilityExportRequest()
            .setSupplierIds(List.of(1, 200, 100)));
        ExcelFile file = excelFileDownloader.downloadExport(exportId);

        DeepmindAssertions.assertThat(file)
            .containsValue(1, SUPPLIER_ID.getHeader(), 1)
            .containsValue(2, SUPPLIER_ID.getHeader(), 1)
            .containsValue(3, SUPPLIER_ID.getHeader(), 101)
            .containsValue(4, SUPPLIER_ID.getHeader(), 102)
            .containsValue(5, SUPPLIER_ID.getHeader(), 201)
            .containsValue(6, SUPPLIER_ID.getHeader(), 202)
            .hasLastLine(6);
    }

    @Test
    public void testExportEmptyResult() {
        int exportId = controller.startExportWithBigFilter(new ResultSskuAvailabilityExportRequest()
            .setSupplierIds(List.of(19191919)));
        ExcelFile file = excelFileDownloader.downloadExport(exportId);

        DeepmindAssertions.assertThat(file).isEmpty();
    }

    @Test
    public void testShouldWorkWithDeletedMsku() {
        Msku deletedMsku = deepmindMskuRepository.save(randomMsku()
            .setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID)
            .setDeleted(true)
        );
        serviceOfferReplicaRepository.save(
            createOffer(1, "offer-deleted", deletedMsku)
        );

        List<ResultAvailability> list =
            controller.listWithBigFilter(new ExtendedResultAvailabilityFilter()
                    .setSupplierIds(1).setShopSkus("offer-deleted"),
                ShopSkuKeyLastFilter.all()
            );

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactly(serviceOfferReplicaRepository.findOfferByKey(1, "offer-deleted"));
        assertThat(list)
            .extracting(ResultAvailability::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(new DisplayMsku(deletedMsku, category.getName(), VENDOR_NAME));
    }

    @Test
    public void testListOfferWithoutMsku() {
        Msku notSyncMsku = randomMsku().setCategoryId(CATEGORY_ID);
        Msku normalMsku = deepmindMskuRepository.save(randomMsku().setCategoryId(CATEGORY_ID));

        Optional<Msku> byId = deepmindMskuRepository.findById(notSyncMsku.getId());
        assertThat(byId.isPresent()).isFalse();

        serviceOfferReplicaRepository.save(
            createOffer(42, "offer-without-msku", notSyncMsku),
            createOffer(42, "normal-offer", normalMsku)
        );
        var withoutMsku = serviceOfferReplicaRepository.findOfferByKey(42, "offer-without-msku");
        var normal = serviceOfferReplicaRepository.findOfferByKey(42, "normal-offer");

        List<ResultAvailability> list =
            controller.listWithBigFilter(
                new ExtendedResultAvailabilityFilter().setSupplierIds(42).setShopSkus("offer-without-msku"),
                ShopSkuKeyLastFilter.all()
            );

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactlyInAnyOrder(withoutMsku);

        list = controller.listWithBigFilter(new ExtendedResultAvailabilityFilter().setSupplierIds(42),
            ShopSkuKeyLastFilter.all());
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactlyInAnyOrder(withoutMsku, normal);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setSupplierIds(42).setCategoryIds(CATEGORY_ID),
            ShopSkuKeyLastFilter.all());
        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactlyInAnyOrder(withoutMsku, normal);

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setMskuIds(notSyncMsku.getId()),
            ShopSkuKeyLastFilter.all());

        assertThat(list)
            .extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactlyInAnyOrder(withoutMsku);
    }

    @Test
    public void testFilterByBusinessIds() {
        serviceOfferReplicaRepository.save(
            createOffer(200, "offer", msku).setSupplierId(201),
            createOffer(200, "offer", msku).setSupplierId(202),
            createOffer(100, "offer", msku).setSupplierId(101),
            createOffer(100, "offer", msku).setSupplierId(102)
        );
        var expectedOffers = serviceOfferReplicaRepository.findOffers(
            new ServiceOfferReplicaFilter().setBusinessIds(List.of(200)));

        var list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setSupplierIds(200), ShopSkuKeyLastFilter.all());

        Assertions.assertThat(list).extracting(ResultAvailability::getOffer)
            .usingElementComparatorOnFields("businessOfferId", "businessId", "supplierId", "shopSku", "title")
            .containsExactlyInAnyOrderElementsOf(expectedOffers);
    }

    @Test
    public void testIterateOverBusinessOffers() {
        serviceOfferReplicaRepository.save(
            createOffer(100, "offer0", msku).setSupplierId(101),
            createOffer(100, "offer0", msku).setSupplierId(102),
            createOffer(100, "offer1", msku).setSupplierId(102),
            createOffer(200, "offer", msku).setSupplierId(201),
            createOffer(200, "offer", msku).setSupplierId(202)
        );

        var list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter().setSupplierIds(200, 100), ShopSkuKeyLastFilter.limit(3));

        Assertions.assertThat(list).extracting(ResultAvailability::getOffer)
            .extracting(v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()))
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(101, "offer0"),
                new ServiceOfferKey(102, "offer0"),
                new ServiceOfferKey(102, "offer1")
            );

        list = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter(),
            new ShopSkuKeyLastFilter(new ServiceOfferKey(101, "offer1"), 100)
        );
        Assertions.assertThat(list).extracting(ResultAvailability::getOffer)
            .extracting(v -> new ServiceOfferKey(v.getSupplierId(), v.getShopSku()))
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(102, "offer0"),
                new ServiceOfferKey(102, "offer1"),
                new ServiceOfferKey(201, "offer"),
                new ServiceOfferKey(202, "offer")
            );
    }

    @Test
    public void testExport() {
        deepmindWarehouseRepository.save(SORTING_CENTER_1);

        var archived = MatrixAvailabilityUtils.mskuArchived(msku);
        var availabilityCategory = MatrixAvailabilityUtils.mskuInCategory(
            CROSSDOCK_SOFINO_ID, "crossdock", 1, "Category 1", null);
        var mskuMA = MatrixAvailabilityUtils.mskuInWarehouse(false,
            msku, SORTING_CENTER_1, null, null, null);

        shopSkuMatrixAvailabilityService.addAvailability(offer11, ROSTOV_ID, availabilityCategory, archived);
        shopSkuMatrixAvailabilityService.addAvailability(offer11, CROSSDOCK_SOFINO_ID, availabilityCategory);
        shopSkuMatrixAvailabilityService.addAvailability(offer11, SORTING_CENTER_1.getId(), mskuMA);

        ResultSskuAvailabilityExportRequest req = new ResultSskuAvailabilityExportRequest();
        req.getFilter().setShopSkuKeys(offer11.getServiceOfferKey());

        int actionId = controller.startExportWithBigFilter(req);
        ExcelFile excelFile = excelFileDownloader.downloadExport(actionId);

        DeepmindAssertions.assertThat(excelFile)
            .doesntContainHeadersContaining("Все склады")
            .doesntContainHeadersContaining("все склады")
            .containsHeaders(
                header(ROSTOV_ID),
                header(MARSHRUT_ID),
                header(SOFINO_ID),
                header(TOMILINO_ID),
                header(CROSSDOCK_SOFINO_ID),
                header(SORTING_CENTER_1.getId())
            )
            .containsValue(1, header(ROSTOV_ID), "1. " + archived.shortRender()
                + "\n2. " + availabilityCategory.shortRender())
            .containsValue(1, header(MARSHRUT_ID), null)
            .containsValue(1, header(SOFINO_ID), null)
            .containsValue(1, header(TOMILINO_ID), null)
            .containsValue(1, header(CROSSDOCK_SOFINO_ID), "1. " + availabilityCategory.shortRender())
            .containsValue(1, header(SORTING_CENTER_1.getId()), "1. " + mskuMA.shortRender());
    }

    @Test
    @DbUnitDataSet(dataSource = "deepmindDataSource", before = "ResultSskuAvailabilityControllerTest.assort_ssku.csv")
    public void testFilterByAssortSsku() {
        var assortList = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setAssortTypes(List.of(AssortType.ASSORT)),
            ShopSkuKeyLastFilter.all());
        assertThat(assortList)
            .extracting(ResultAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "offer-12"),
                new ServiceOfferKey(3, "offer-31")
            );

        var subList = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setAssortTypes(List.of(AssortType.SUB)),
            ShopSkuKeyLastFilter.all());
        assertThat(subList)
            .extracting(ResultAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "offer-11"),
                new ServiceOfferKey(3, "offer-32"),
                new ServiceOfferKey(3, "offer-33")
            );

        var allList = controller.listWithBigFilter(
            new ExtendedResultAvailabilityFilter()
                .setAssortTypes(List.of(AssortType.SUB, AssortType.ASSORT)),
            ShopSkuKeyLastFilter.all());
        assertThat(allList)
            .extracting(ResultAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "offer-11"),
                new ServiceOfferKey(1, "offer-12"),
                new ServiceOfferKey(3, "offer-31"),
                new ServiceOfferKey(3, "offer-32"),
                new ServiceOfferKey(3, "offer-33")
            );
    }

    private String header(long warehouseId) {
        return String.format("%s", convertWarehouseToExcelHeader(whName(warehouseId)));
    }

    private Warehouse whName(long warehouseId) {
        return deepmindWarehouseRepository.getById(warehouseId);
    }

    public ServiceOfferReplica createOffer(int supplierId, String shopSku, Msku msku) {
        var supplier = deepmindSupplierRepository.findById(supplierId);
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(msku.getCategoryId())
            .setSeqId(0L)
            .setMskuId(msku.getId())
            .setSupplierType(supplier.orElseThrow().getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private Msku randomMsku() {
        return random.nextObject(Msku.class)
            .setDeleted(false);
    }

    private MskuStatus randomMskuStatus() {
        return random.nextObject(MskuStatus.class)
            .setSeasonId(null);
    }

    private Category randomCategory() {
        return random.nextObject(Category.class);
    }

    protected AvailabilityMatrixIndex indexRow(int supplierId, String shopSku, long warehouseId) {
        return new AvailabilityMatrixIndex()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId);
    }

    private DisplayHiding createDisplayHiding(Hiding hiding, String shortText) {
        var reasonKey = hidingReasonDescriptionRepository.findById(hiding.getReasonKeyId()).get().getReasonKey();
        return new DisplayHiding()
            .setSupplierId(hiding.getSupplierId())
            .setShopSku(hiding.getShopSku())
            .setReasonKey(reasonKey)
            .setStopWord(reasonKey.contains(SKK_45K_SUBREASON.toString()) ? hiding.getSubreasonId() : null)
            .setUserName(hiding.getUserName())
            .setComment(hiding.getComment())
            .setShortText(shortText);
    }

    private HidingReasonDescription createHidingDescription(HidingReason reason, String desc) {
        Preconditions.checkArgument(reason.isReason());
        return new HidingReasonDescription()
            .setType(HidingReasonType.REASON)
            .setReasonKey(reason.toString())
            .setExtendedDesc(desc);
    }

    private void insertIndexRows(AvailabilityMatrixIndex... indexRows) {
        Arrays.stream(indexRows)
            .map(hiding -> dslContext.newRecord(Tables.AVAILABILITY_MATRIX_INDEX, hiding))
            .forEach(TableRecordImpl::insert);
    }

    protected Hiding createHiding(long reasonKeyId, String subreasonId, ServiceOfferReplica serviceOffer, MboUser user,
                                  @Nullable String comment) {
        return new Hiding()
            .setReasonKeyId(reasonKeyId)
            .setSubreasonId(subreasonId)
            .setSupplierId(serviceOffer.getBusinessId())
            .setShopSku(serviceOffer.getShopSku())
            .setUserId(user != null ? user.getUid() : null)
            .setUserName(user != null ? user.getFullName() : null)
            .setComment(comment);
    }

    private HidingReasonDescription createHidingDescription(HidingReason reason,
                                                            HidingReason subreason,
                                                            String desc) {
        Preconditions.checkArgument(reason.isReason());
        Preconditions.checkArgument(subreason.isSubReason());
        return new HidingReasonDescription()
            .setType(HidingReasonType.REASON_KEY)
            .setReasonKey(String.format("%s_%s", reason.toString(), subreason.toString()))
            .setExtendedDesc(desc);
    }

    private MskuStockInfo createStockInfo(ServiceOfferReplica serviceOffer, long warehouseId, int fit) {
        return new MskuStockInfo()
            .setShopSkuKey(serviceOffer.getServiceOfferKey())
            .setWarehouseId((int) warehouseId)
            .setFitInternal(fit);
    }

    private void updateSupplierType(int id, SupplierType type, String realSupplierId) {
        var supplier = deepmindSupplierRepository.findById(id).orElseThrow();
        deepmindSupplierRepository.save(supplier.setSupplierType(type).setRealSupplierId(realSupplierId));
        jdbcTemplate.update("UPDATE msku.offer SET supplier_type = ?::mbo_category.supplier_type" +
            " WHERE supplier_id = ?", type.name(), id);
    }

    private MskuInfo mskuInfo(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(false);
    }

    private Msku msku(long mskuId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability, long mskuId) {
        if (deepmindSupplierRepository.findByIds(List.of(supplierId)).isEmpty()) {
            var supplier = new Supplier().setId(supplierId).setName("test_supplier_" + supplierId)
                .setSupplierType(THIRD_PARTY);
            deepmindSupplierRepository.save(supplier);
        }
        var offer =  new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(OfferAvailability.valueOf(availability.name()))
            .setModifiedByUser(false)
        );
    }
}
