package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.availability.web.AvailabilityValue;
import ru.yandex.market.deepmind.app.availability.web.DisplayMskuAvailability;
import ru.yandex.market.deepmind.app.availability.web.MskuUpdateAvailabilityRequest;
import ru.yandex.market.deepmind.app.pojo.DisplayMsku;
import ru.yandex.market.deepmind.app.pojo.DisplayMskuStatusInfo;
import ru.yandex.market.deepmind.app.services.SskuMskuStatusHelperService;
import ru.yandex.market.deepmind.app.services.SskuMskuStatusHelperServiceImpl;
import ru.yandex.market.deepmind.app.utils.DeepmindUtils;
import ru.yandex.market.deepmind.app.web.DisplayMskuInfo;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilter;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilter.ExtendedMskuStatusValue;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilterConverter;
import ru.yandex.market.deepmind.app.web.availability.DisplayInheritAvailabilities;
import ru.yandex.market.deepmind.common.ExcelFileDownloader;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.background.BackgroundExportService;
import ru.yandex.market.deepmind.common.background.BaseBackgroundExportable;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.StatsMskuMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.mocks.GlobalVendorsCachingServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCargoTypeSnapshotRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.StatsMskuMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.LockType;
import ru.yandex.market.deepmind.common.services.MskuInfoFeatures;
import ru.yandex.market.deepmind.common.services.audit.AvailabilityMatrixAuditService;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditRecorder;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditRecorder.MskuStatusAuditInformation;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceImpl;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.common.utils.excel.ExcelUtils;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus;
import ru.yandex.market.mboc.common.infrastructure.sql.BaseAuditRecorder.PropertyChange;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;
import ru.yandex.market.mboc.common.web.DataPage;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.deepmind.app.exportable.MskuAvailabilityExportable.IN_TARGET_ASSORTMENT;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.AVAILABLE;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.AVAILABLE_INHERITED;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.MSKU_ID_KEY;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.NOT_AVAILABLE;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.NOT_AVAILABLE_INHERITED;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.PRICEBAND_ID;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.PRICEBAND_LABEL;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.mboc.common.offers.model.Offer.AcceptanceStatus.OK;
import static ru.yandex.market.mboc.common.services.category.CategoryTree.ROOT_CATEGORY_ID;

public class MskuControllerTest extends DeepmindBaseAppDbTestClass {
    private static final long VENDOR_ID = 23;
    private static final String VENDOR_NAME = "VendorName";
    private static final long CATEGORY_ID = 147;
    private static final String CATEGORY_NAME = "CategoryName";

    private MskuController mskuController;

    @Resource(name = "deepmindDsl")
    private DSLContext dsl;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Resource
    private DeepmindCargoTypeSnapshotRepository deepmindCargoTypeSnapshotRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private MskuInfoRepository mskuInfoRepository;
    @Resource
    private StatsMskuMatrixRepository statsMskuMatrixRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private MskuStatusRepository mskuStatusRepository;
    private BackgroundServiceMock backgroundServiceMock;
    private DeepmindCategoryCachingServiceMock categoryCachingService;
    private GlobalVendorsCachingServiceMock globalVendorsCachingService;
    private SskuMskuStatusHelperService sskuMskuStatusHelperService;
    private ExcelFileDownloader excelFileDownloader;
    private MboAuditServiceMock mboAuditServiceMock;

    private EnhancedRandom random;

    private Msku msku;
    private Msku msku2;
    private Msku npdMsku;
    private Msku noStatusMsku;
    private DisplayMsku displayMsku;
    private DisplayMsku displayMsku2;
    private DisplayMsku npdDisplayMsku;
    private DisplayMsku displayNoStatusMsku;
    private Season season;
    private MskuStatus mskuStatus;
    private List<MskuAvailabilityMatrix> mskuAvailabilityMatrices;
    private Warehouse marshrut;
    private Warehouse sofino;
    private Warehouse tomilino;
    private Warehouse rostov;
    private BackgroundExportService backgroundExportService;
    private AvailableWarehouseService availableWarehouseService;

    @AfterClass
    public static void clearAuth() {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
    }

    @Before
    public void setUp() throws Exception {
        SecurityContextAuthenticationUtils.setAuthenticationToken();
        mboAuditServiceMock = new MboAuditServiceMock();

        var mboUsersRepository = Mockito.mock(MboUsersRepository.class);
        var mskuStatusAuditRecorder = new MskuStatusAuditRecorder(mboAuditServiceMock, mboUsersRepository);
        mskuStatusAuditRecorder.setAuditEnabled(true);

        mskuStatusRepository = new MskuStatusRepository(dsl);
        mskuStatusRepository.addObserver(mskuStatusAuditRecorder);

        random = TestUtils.createMskuRandom();
        categoryCachingService = new DeepmindCategoryCachingServiceMock();
        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        globalVendorsCachingService.addVendor(new CachedGlobalVendor(VENDOR_ID, VENDOR_NAME));
        backgroundServiceMock = new BackgroundServiceMock();
        var excelS3Service = new ExcelS3ServiceMock();
        backgroundExportService = new BackgroundExportService(backgroundServiceMock, transactionTemplate,
            excelS3Service);
        excelFileDownloader = new ExcelFileDownloader(backgroundServiceMock, excelS3Service);

        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        marshrut = deepmindWarehouseRepository.findById(MARSHRUT_ID).orElseThrow();
        sofino = deepmindWarehouseRepository.findById(SOFINO_ID).orElseThrow();
        tomilino = deepmindWarehouseRepository.findById(TOMILINO_ID).orElseThrow();
        rostov = deepmindWarehouseRepository.findById(ROSTOV_ID).orElseThrow();

        Category rootCategory = randomCategory()
            .setParentCategoryId(CategoryTree.NO_ROOT_ID)
            .setCategoryId(ROOT_CATEGORY_ID)
            .setName(CATEGORY_NAME);
        Category category = randomCategory()
            .setParentCategoryId(rootCategory.getCategoryId())
            .setCategoryId(CATEGORY_ID)
            .setName(CATEGORY_NAME);
        Category subCategory = randomCategory()
            .setParentCategoryId(category.getCategoryId())
            .setCategoryId(42)
            .setName(CATEGORY_NAME);
        categoryCachingService.addCategory(rootCategory);
        categoryCachingService.addCategory(category);
        categoryCachingService.addCategory(subCategory);

        msku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(category.getCategoryId()));
        msku2 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(category.getCategoryId()));
        npdMsku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(category.getCategoryId()).setTitle("TestTitle"));
        noStatusMsku = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(category.getCategoryId()).setTitle("noStatusMsku"));

        displayMsku = new DisplayMsku(msku, CATEGORY_NAME, VENDOR_NAME);
        displayMsku2 = new DisplayMsku(msku2, CATEGORY_NAME, VENDOR_NAME);
        npdDisplayMsku = new DisplayMsku(npdMsku, CATEGORY_NAME, VENDOR_NAME);
        displayNoStatusMsku = new DisplayMsku(noStatusMsku, CATEGORY_NAME, VENDOR_NAME);

        season = seasonRepository.save(new Season().setName("testSeason"));

        mskuStatus = mskuStatusRepository.save(randomMskuStatus()
                .setComment("Comment")
            .setMarketSkuId(msku.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));

        mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(npdMsku.getId())
            .setMskuStatus(MskuStatusValue.REGULAR));

        var deepmindCargoTypeCachingService =
            new DeepmindCargoTypeCachingServiceImpl(deepmindCargoTypeSnapshotRepository);

        var mskuAvailabilityMatrixChecker = new MskuAvailabilityMatrixChecker(
            mskuAvailabilityMatrixRepository, categoryAvailabilityMatrixRepository, mskuStatusRepository,
            deepmindCargoTypeCachingService, categoryCachingService, seasonRepository
        );

        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        var sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        sskuMskuStatusHelperService = new SskuMskuStatusHelperServiceImpl(serviceOfferReplicaRepository,
            backgroundServiceMock, sskuMskuStatusService, sskuMskuStatusValidationService, sskuStatusRepository,
            deepmindMskuRepository, mskuStatusRepository, transactionTemplate);

        var extendedMskuFilterConverter = new ExtendedMskuFilterConverter(
            deepmindSupplierRepository, deepmindCategoryManagerRepository, deepmindCategoryTeamRepository,
            categoryCachingService
        );
        availableWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT
        );

        mskuController = new MskuController(categoryCachingService, globalVendorsCachingService,
            deepmindMskuRepository, mskuInfoRepository, mskuStatusRepository,
            mskuAvailabilityMatrixRepository,
            seasonRepository, availableWarehouseService, mskuAvailabilityMatrixChecker,
            extendedMskuFilterConverter, backgroundServiceMock,
            sskuMskuStatusHelperService,
            new MskuStatusAuditService(mboAuditServiceMock),
            Mockito.mock(AvailabilityMatrixAuditService.class), serviceOfferReplicaRepository,
            sskuAvailabilityMatrixRepository, new TransactionTemplateMock(),
            deepmindCargoTypeCachingService,
            backgroundExportService, statsMskuMatrixRepository);
    }

    @Test
    public void testMskuStatusHistoryOutput() {
        mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL);
        mskuStatus.setSeasonId(season.getId());
        mskuStatusRepository.save(mskuStatus);

        // добавляем одну секунду, чтобы тест не флапал DEEPMIND-704
        var response = mskuController.showMskuStatusHistory(mskuStatus.getMarketSkuId(),
            Instant.now().plusSeconds(1).toString());

        assertThat(response.getAuditInfoList().size())
            .isGreaterThan(1);

        assertThat(response.getAuditInfoList())
            .isSortedAccordingTo(Comparator.comparing(info -> info.getModifiedTs().toEpochMilli(),
                Comparator.reverseOrder()));

        assertThat(response.getAuditInfoList().stream()
            .min(Comparator.comparing(DisplayMskuStatusInfo::getModifiedTs)).get().getModifiedTs())
            .isEqualTo(response.getLastTs());
    }

    @Test
    public void testShowAuditLogInLegacyFormat() {
        // legacy save
        var timestamp = Instant.now().minus(10, DAYS).toEpochMilli();
        mboAuditServiceMock.writeActions(MboAudit.WriteActionsRequest.newBuilder()
            .addActions(MskuStatusAuditRecorder.mskuStatusAction(
                new MskuStatusAuditInformation(mskuStatus),
                new PropertyChange(MskuStatusAuditService.MSKU_STATUS_AUDIT_PROPERTY_PREFIX +
                    "mskuStatus", MskuStatusValue.NPD.toString(), MskuStatusValue.SEASONAL.toString()),
                timestamp
            ))
            .addActions(MskuStatusAuditRecorder.mskuStatusAction(
                new MskuStatusAuditInformation(mskuStatus),
                new PropertyChange(MskuStatusAuditService.MSKU_STATUS_AUDIT_PROPERTY_PREFIX +
                    "comment", null, "Hello world"),
                timestamp
            ))
            .build());

        // second save
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.END_OF_LIFE));

        // добавляем одну секунду, чтобы тест не флапал DEEPMIND-704
        var response = mskuController.showMskuStatusHistory(mskuStatus.getMarketSkuId(),
            Instant.now().plusSeconds(1).toString());

        assertThat(response.getAuditInfoList()).hasSize(3);
        assertThat(response.getAuditInfoList().get(0).getChangeFields())
            .contains("mskuStatus: END_OF_LIFE");
        assertThat(response.getAuditInfoList().get(1).getChangeFields())
            .contains("mskuStatus: REGULAR")
            .contains("comment: Comment");
        assertThat(response.getAuditInfoList().get(2).getChangeFields())
            .contains("mskuStatus: SEASONAL")
            .contains("comment: Hello world");
    }

    @Test
    public void testListRegularStatus() {
        populateDbWithRandomAvailabilities();
        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setMarketSkuIds(List.of(msku.getId())), OffsetFilter.firstPage());

        assertThat(dataPage.getTotalCount()).isEqualTo(1);
        DisplayMskuInfo result = dataPage.getItems().get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getSeason()).isNull();
        assertThat(result.getAvailabilityMatrixList())
            .containsExactlyInAnyOrderElementsOf(mskuAvailabilityMatrices);
        assertThat(result.getStatus()).isEqualToComparingFieldByField(mskuStatus.getMskuStatus());
    }

    @Test
    public void testListSeasonalStatus() {
        populateDbWithRandomAvailabilities();
        mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL);
        mskuStatus.setSeasonId(season.getId());
        mskuStatusRepository.save(mskuStatus);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setMarketSkuIds(List.of(msku.getId())), OffsetFilter.firstPage());

        assertThat(dataPage.getTotalCount()).isEqualTo(1);
        DisplayMskuInfo result = dataPage.getItems().get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getSeason()).isEqualToComparingFieldByField(season);
        assertThat(result.getAvailabilityMatrixList())
            .containsExactlyInAnyOrderElementsOf(mskuAvailabilityMatrices);
        assertThat(result.getStatus()).isEqualToComparingFieldByField(mskuStatus.getMskuStatus());
    }

    @Test
    public void testListNpdStatus() {
        populateDbWithRandomAvailabilities();
        mskuStatus.setMskuStatus(MskuStatusValue.NPD);
        mskuStatus.setNpdStartDate(LocalDate.now());
        mskuStatusRepository.save(mskuStatus);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setMarketSkuIds(List.of(msku.getId())), OffsetFilter.firstPage());

        assertThat(dataPage.getTotalCount()).isEqualTo(1);
        DisplayMskuInfo result = dataPage.getItems().get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getSeason()).isNull();
        assertThat(result.getAvailabilityMatrixList())
            .containsExactlyInAnyOrderElementsOf(mskuAvailabilityMatrices);
        assertThat(result.getStatus()).isEqualToComparingFieldByField(mskuStatus.getMskuStatus());
    }

    @Test
    public void testListNoStatus() {
        populateDbWithRandomAvailabilities();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.EMPTY));

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setMarketSkuIds(List.of(msku.getId())), OffsetFilter.firstPage());

        assertThat(dataPage.getTotalCount()).isEqualTo(1);
        DisplayMskuInfo result = dataPage.getItems().get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(displayMsku);
        assertThat(result.getSeason()).isNull();
        assertThat(result.getAvailabilityMatrixList())
            .containsExactlyInAnyOrderElementsOf(mskuAvailabilityMatrices);
        assertThat(result.getStatus()).isEqualTo(DisplayMskuInfo.DisplayMskuStatusValue.EMPTY);
    }

    @Test
    public void testListWithMskuInfo() {
        populateDbWithRandomAvailabilities();
        var mskuInfo1 = mskuInfo(msku.getId());
        var mskuInfo2 = mskuInfo(msku2.getId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setMarketSkuIds(List.of(msku.getId(), msku2.getId())),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);
    }

    @Test
    public void testListWithCoreFixFilter() {
        var mskuInfo1 = mskuInfo(msku.getId());
        var mskuInfo2 = mskuInfo(msku2.getId());
        var mskuInfo3 = mskuInfo(npdMsku.getId()).setInTargetAssortment(true);
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(true)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures()),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2, mskuInfo3);
    }

    @Test
    public void testListWithMskuInfoFeaturesFilter() {
        var mskuInfo1 = mskuInfo(msku.getId()).setPricebandId(123L);
        var mskuInfo2 = mskuInfo(msku2.getId()).setPricebandId(123L).setPricebandLabel("123_label");
        var mskuInfo3 = mskuInfo(npdMsku.getId())
            .setInTargetAssortment(true).setPricebandId(234L).setPricebandLabel("234_label");
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandId(123L)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandLabel("234_label")),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(true).setPricebandId(234L)
                        .setPricebandLabel("234_label")),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures()),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2, mskuInfo3);
    }

    @Test
    public void testListWithMskuInfoPriceLimitsFilter() {
        var mskuInfo1 = mskuInfo(msku.getId()).setPrice(300.0).setPricebandId(111L);
        var mskuInfo2 = mskuInfo(msku2.getId()).setPrice(1300.0).setPricebandId(111L);
        var mskuInfo3 = mskuInfo(npdMsku.getId()).setPrice(5300.0).setPricebandId(222L);
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(200.0)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2, mskuInfo3);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(
                        new MskuInfoFeatures().setFromPriceInclusive(350.0).setToPriceInclusive(3000.0)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo2);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(
                        new MskuInfoFeatures().setFromPriceInclusive(300.0).setToPriceInclusive(1300.0)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setToPriceInclusive(3000.0)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId()))
                    .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(200.0).setPricebandId(111L)),
                OffsetFilter.all());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);
    }

    @Test
    public void testListWithWarehouseIdAndLockedTypeTest() {
        mskuAvailabilityMatrixRepository.save(
            randomMskuAvailabilityMatrix()
                .setAvailable(false)
                .setMarketSkuId(msku2.getId())
                .setWarehouseId(TOMILINO_ID),
            randomMskuAvailabilityMatrix()
                .setAvailable(true)
                .setMarketSkuId(msku.getId())
                .setWarehouseId(TOMILINO_ID)
        );

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId())),
                OffsetFilter.all());
        assertThat(dataPage.getItems())
            .extracting(item -> item.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId(), msku2.getId());

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId()))
                    .setLockedAtWarehouseId(TOMILINO_ID)
                    .setLockType(LockType.EXPLICIT_LOCK),
                OffsetFilter.all());
        assertThat(dataPage.getItems())
            .extracting(item -> item.getMsku().getId())
            .containsExactlyInAnyOrder(msku2.getId());

        dataPage = mskuController
            .list(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId(), msku2.getId()))
                    .setLockedAtWarehouseId(TOMILINO_ID)
                    .setLockType(LockType.EXPLICIT_PERMISSION),
                OffsetFilter.all());
        assertThat(dataPage.getItems())
            .extracting(item -> item.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId());
    }

    @Test
    public void testFilterByTitle() {
        populateDbWithRandomAvailabilities();
        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setSearchText("Title"), OffsetFilter.firstPage());

        assertThat(dataPage.getTotalCount()).isEqualTo(1);
        DisplayMskuInfo result = dataPage.getItems().get(0);
        assertThat(result.getMsku()).isEqualToComparingFieldByField(npdDisplayMsku);
    }

    @Test
    public void testFilterByCategoryManager() {
        populateDbWithRandomAvailabilities();
        deepmindCategoryManagerRepository.save(newManagerCategory("pupkin", CATEGORY_ID));

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setCategoryManagerLogin("pupkin"), OffsetFilter.firstPage());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(displayMsku, displayMsku2, npdDisplayMsku, displayNoStatusMsku);
    }

    @Test
    public void testFilterByCategoryManagerInTree() {
        populateDbWithRandomAvailabilities();
        deepmindCategoryManagerRepository.save(newManagerCategory("pupkin", 42L));

        Msku msku3 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID).setCategoryId(42L));
        DisplayMsku displayMsku3 = new DisplayMsku(msku3, CATEGORY_NAME, VENDOR_NAME);

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setCategoryManagerLogin("pupkin")
                .setCategoryIds(List.of(42L)), OffsetFilter.firstPage());

        assertThat(dataPage.getItems())
            .extracting(DisplayMskuInfo::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(displayMsku3);
    }

    @Test
    public void testFilterByRootCategoryId() {
        populateDbWithRandomAvailabilities();
        deepmindCategoryManagerRepository.save(newManagerCategory("pupkin", CATEGORY_ID));

        Msku msku3 = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID).setCategoryId(42L));
        DisplayMsku displayMsku3 = new DisplayMsku(msku3, CATEGORY_NAME, VENDOR_NAME);

        DataPage<DisplayMskuInfo> dataPage1 = mskuController
            .list(new ExtendedMskuFilter().setHierarchyCategoryIds(List.of(42L))
                .setCategoryIds(List.of(42L)), OffsetFilter.firstPage());

        assertThat(dataPage1.getItems())
            .extracting(DisplayMskuInfo::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(displayMsku3);

        DataPage<DisplayMskuInfo> dataPage2 = mskuController
            .list(new ExtendedMskuFilter().setHierarchyCategoryIds(List.of(CATEGORY_ID))
                    .setCategoryIds(List.of(CATEGORY_ID)),
                OffsetFilter.firstPage());

        assertThat(dataPage2.getItems())
            .extracting(DisplayMskuInfo::getMsku)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(displayMsku, displayMsku2, npdDisplayMsku, displayNoStatusMsku);
    }

    @Test
    public void testFindByStatus() {
        var mskuWithEmptyStatus = deepmindMskuRepository.save(randomMsku().setVendorId(VENDOR_ID)
            .setCategoryId(CATEGORY_ID).setTitle("mskuWithEmptyStatus"));

        // msku -> REGULAR
        // npdMsku -> REGULAR
        // msku2 -> ARCHIVE
        // noStatusMsku -> no rows in DB
        // mskuWithEmptyStatus -> EMPTY
        mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(msku2.getId())
            .setMskuStatus(MskuStatusValue.ARCHIVE));
        mskuStatusRepository.save(randomMskuStatus()
            .setMarketSkuId(mskuWithEmptyStatus.getId())
            .setMskuStatus(MskuStatusValue.EMPTY));

        DataPage<DisplayMskuInfo> dataPageRegular = mskuController.list(
            new ExtendedMskuFilter().setMskuStatusValue(ExtendedMskuStatusValue.REGULAR),
            OffsetFilter.firstPage()
        );
        assertThat(dataPageRegular.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId(), npdMsku.getId());

        DataPage<DisplayMskuInfo> dataPageArchive = mskuController.list(
            new ExtendedMskuFilter().setMskuStatusValue(ExtendedMskuStatusValue.ARCHIVE),
            OffsetFilter.firstPage()
        );
        assertThat(dataPageArchive.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(msku2.getId());

        DataPage<DisplayMskuInfo> dataPageEndOfLife = mskuController.list(
            new ExtendedMskuFilter().setMskuStatusValue(ExtendedMskuStatusValue.END_OF_LIFE),
            OffsetFilter.firstPage()
        );
        assertThat(dataPageEndOfLife.getItems()).isEmpty();

        DataPage<DisplayMskuInfo> dataPageWithStatus = mskuController.list(
            new ExtendedMskuFilter().setMskuStatusValue(ExtendedMskuStatusValue.WITH_ANY_STATUS),
            OffsetFilter.firstPage()
        );
        assertThat(dataPageWithStatus.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId(), msku2.getId(), npdMsku.getId());

        DataPage<DisplayMskuInfo> dataPageWithoutStatus = mskuController.list(
            new ExtendedMskuFilter().setMskuStatusValue(ExtendedMskuStatusValue.WITHOUT_STATUS),
            OffsetFilter.firstPage()
        );
        assertThat(dataPageWithoutStatus.getItems()).extracting(i -> i.getMsku().getId())
            .contains(noStatusMsku.getId(), mskuWithEmptyStatus.getId());
    }

    @Test
    public void testFindByBusinessId() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-3p-11", msku),
            offer(1, "offer-12", msku2),
            offer(2, "offer-21", msku),
            offer(3, "offer-31", msku),
            offer(465852, "offer-beru-1", msku),
            offer(465852, "offer-beru-2", msku2),
            offer(77, "offer-real-1", msku2),
            offer(77, "offer-real-2", msku),
            offer(200, "offer-service-1", msku2).setSupplierId(201),
            offer(200, "offer-service-2", npdMsku).setSupplierId(201),
            offer(200, "offer-service-2", npdMsku).setSupplierId(202),
            offer(200, "offer-service-3", npdMsku).setSupplierId(201)
        );

        var page = mskuController.list(new ExtendedMskuFilter().setSupplierIds(List.of(200)), OffsetFilter.all());
        assertThat(page.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(msku2.getId(), npdMsku.getId());

        page = mskuController.list(new ExtendedMskuFilter().setSupplierIds(List.of(202)), OffsetFilter.all());
        assertThat(page.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(npdMsku.getId());
    }

    @Test
    public void testFindBySupplierType() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(
            supplier(1, null, SupplierType.THIRD_PARTY),
            supplier(2, null, SupplierType.THIRD_PARTY),
            supplier(3, null, SupplierType.THIRD_PARTY),
            supplier(465852, null, SupplierType.FIRST_PARTY),
            supplier(77, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000042"),
            supplier(200, null, SupplierType.THIRD_PARTY),
            supplier(201, null, SupplierType.THIRD_PARTY),
            supplier(202, null, SupplierType.THIRD_PARTY)
        );
        serviceOfferReplicaRepository.save(
            offer(1, SupplierType.THIRD_PARTY, "offer-3p-11", msku),
            offer(1, SupplierType.THIRD_PARTY, "offer-3p-12", msku),
            offer(2, SupplierType.THIRD_PARTY, "offer-3p-21", msku),
            offer(3, SupplierType.THIRD_PARTY, "offer-3p-31", msku),
            offer(465852, SupplierType.FIRST_PARTY, "offer-beru-1", msku2),
            offer(465852, SupplierType.FIRST_PARTY, "offer-beru-2", msku2),
            offer(77, SupplierType.REAL_SUPPLIER, "offer-real-1", npdMsku),
            offer(77, SupplierType.REAL_SUPPLIER, "offer-real-2", npdMsku),
            offer(200, SupplierType.THIRD_PARTY, "offer-service-1", noStatusMsku).setSupplierId(201),
            offer(200, SupplierType.THIRD_PARTY, "offer-service-2", noStatusMsku).setSupplierId(201),
            offer(200, SupplierType.THIRD_PARTY, "offer-service-2", noStatusMsku).setSupplierId(202),
            offer(200, SupplierType.THIRD_PARTY, "offer-service-3", noStatusMsku).setSupplierId(201)
        );

        var page = mskuController.list(new ExtendedMskuFilter()
            .setSupplierType(SupplierType.FIRST_PARTY), OffsetFilter.all());
        assertThat(page.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(msku2.getId());

        page = mskuController.list(new ExtendedMskuFilter()
            .setSupplierType(SupplierType.THIRD_PARTY), OffsetFilter.all());
        assertThat(page.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId(), noStatusMsku.getId());

        page = mskuController.list(new ExtendedMskuFilter()
            .setSupplierType(SupplierType.REAL_SUPPLIER), OffsetFilter.all());
        assertThat(page.getItems()).extracting(i -> i.getMsku().getId())
            .containsExactlyInAnyOrder(npdMsku.getId());
    }

    @Test
    public void testExtendedMskuStatusValueEnumCorrelation() {
        populateDbWithRandomAvailabilities();
        // test no fails
        mskuController.list(new ExtendedMskuFilter(), OffsetFilter.firstPage());
        for (ExtendedMskuStatusValue statusValue : ExtendedMskuStatusValue.values()) {
            mskuController.list(new ExtendedMskuFilter().setMskuStatusValue(statusValue), OffsetFilter.firstPage());
        }
    }

    @Test
    public void testBlockedSskuData() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-11", msku),
            offer(1, "offer-12", msku2),
            offer(2, "offer-21", msku),
            offer(3, "offer-31", msku),
            offer(42, "offer-421", msku),
            offer(43, "offer-431", msku)
        );
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(1).setShopSku("offer-11")
                .setAvailable(false).setWarehouseId(TOMILINO_ID).setCreatedLogin("unit-test"),
            new SskuAvailabilityMatrix()
                .setSupplierId(1).setShopSku("offer-12")
                .setAvailable(false).setWarehouseId(SOFINO_ID).setCreatedLogin("unit-test"),
            new SskuAvailabilityMatrix()
                .setSupplierId(2).setShopSku("offer-21")
                .setAvailable(false).setWarehouseId(TOMILINO_ID).setCreatedLogin("unit-test"),
            new SskuAvailabilityMatrix()
                .setSupplierId(3).setShopSku("offer-31")
                .setAvailable(null).setWarehouseId(SOFINO_ID).setCreatedLogin("unit-test"),
            new SskuAvailabilityMatrix()
                .setSupplierId(43).setShopSku("offer-431")
                .setAvailable(false).setWarehouseId(SOFINO_ID).setCreatedLogin("unit-test")
        );

        // get data
        DataPage<DisplayMskuInfo> infos = mskuController.list(new ExtendedMskuFilter()
                .setMarketSkuIds(List.of(msku.getId(), msku2.getId(), npdMsku.getId())),
            OffsetFilter.all()
        );
        var resultMap = infos.getItems().stream()
            .collect(Collectors.toMap(s -> s.getMsku().getId(), DisplayMskuInfo::getBlockedSskuByWarehouseId));

        // assert
        assertThat(resultMap.keySet())
            .containsExactlyInAnyOrder(msku.getId(), msku2.getId(), npdMsku.getId());
        assertThat(resultMap.get(msku.getId()))
            .containsAllEntriesOf(Map.of(
                TOMILINO_ID, List.of(new ServiceOfferKey(1, "offer-11"), new ServiceOfferKey(2, "offer-21")),
                SOFINO_ID, List.of(new ServiceOfferKey(43, "offer-431"))
            ));
        assertThat(resultMap.get(msku2.getId()))
            .containsAllEntriesOf(Map.of(
                SOFINO_ID, List.of(new ServiceOfferKey(1, "offer-12"))
            ));
        assertThat(resultMap.get(npdMsku.getId()))
            .isEmpty();
    }

    @Test
    public void testSskuSearchTextFilter() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-11", msku),
            offer(1, "offer-12", msku2),
            offer(2, "offer-21", msku),
            offer(3, "offer-31", msku),
            offer(42, "offer-421", msku),
            offer(43, "offer-431", msku),
            offer(43, "offer-2222", npdMsku)
        );
        DataPage<DisplayMskuInfo> infos = mskuController.list(new ExtendedMskuFilter().setSskuSearchText("offer-1"),
            OffsetFilter.all()
        );

        assertThat(infos.getItems())
            .extracting(info -> info.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId(), msku2.getId());
    }

    @Test
    public void testShopSkuSearchTextFilter() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-11", msku),
            offer(1, "offer-12", msku2),
            offer(2, "offer-21", msku),
            offer(3, "offer-31", msku),
            offer(42, "offer-421", msku),
            offer(43, "offer-431", msku),
            offer(43, "offer-2222", npdMsku)
        );
        DataPage<DisplayMskuInfo> infos = mskuController.list(new ExtendedMskuFilter()
            .setShopSkuSearchText("offer-12 offer-2222"), OffsetFilter.all()
        );

        assertThat(infos.getItems())
            .extracting(info -> info.getMsku().getId())
            .containsExactlyInAnyOrder(msku2.getId(), npdMsku.getId());

        infos = mskuController.list(new ExtendedMskuFilter().setShopSkuSearchText("offer-11"), OffsetFilter.all());

        assertThat(infos.getItems())
            .extracting(info -> info.getMsku().getId())
            .containsExactlyInAnyOrder(msku.getId());
    }

    @Test
    public void testShopSkuSearchTextFirstPartySupplierFilter() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        deepmindSupplierRepository.save(supplier(777888, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000056"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-11", msku),
            offer(777888, "sku32", npdMsku)
        );
        DataPage<DisplayMskuInfo> infos = mskuController.list(new ExtendedMskuFilter()
            .setShopSkuSearchText("000056.sku32"), OffsetFilter.all()
        );

        assertThat(infos.getItems())
            .extracting(info -> info.getMsku().getId())
            .containsExactlyInAnyOrder(npdMsku.getId());
    }

    @Test
    public void testShopSkuSearchTextMultipleFirstPartySuppliersFilter() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        deepmindSupplierRepository.save(supplier(777888, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000056"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-11", msku),
            offer(777888, "sku32", npdMsku),
            offer(77, "sku333", msku2)
        );
        DataPage<DisplayMskuInfo> infos = mskuController.list(new ExtendedMskuFilter()
            .setShopSkuSearchText("000056.sku32 000042.sku333"), OffsetFilter.all()
        );

        assertThat(infos.getItems())
            .extracting(info -> info.getMsku().getId())
            .containsExactlyInAnyOrder(npdMsku.getId(), msku2.getId());
    }

    @Test
    public void testShopSkuSearchTextFirstAndThirdPartySupplierFilter() {
        populateDbWithRandomAvailabilities();
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        deepmindSupplierRepository.save(supplier(777888, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000056"));
        serviceOfferReplicaRepository.save(
            offer(1, "offer-11", msku),
            offer(1, "offer-12", msku2),
            offer(2, "offer-21", msku),
            offer(3, "offer-31", msku),
            offer(42, "offer-421", msku),
            offer(43, "offer-431", msku),
            offer(777888, "sku32", npdMsku).setSupplierType(SupplierType.REAL_SUPPLIER),
            offer(77, "sku333", npdMsku).setSupplierType(SupplierType.REAL_SUPPLIER)
        );
        DataPage<DisplayMskuInfo> infos = mskuController.list(new ExtendedMskuFilter()
            .setShopSkuSearchText("offer-12, 000056.sku32 000042.sku333"), OffsetFilter.all()
        );

        assertThat(infos.getItems())
            .extracting(info -> info.getMsku().getId())
            .containsExactlyInAnyOrder(msku2.getId(), npdMsku.getId());
    }

    @Test
    public void testListOffersWithBlockReasonExplicit() {
        var msku1 = TestUtils.randomMsku(random);
        var msku222 = TestUtils.randomMsku(random);
        var msku3 = TestUtils.randomMsku(random);
        var msku4 = TestUtils.randomMsku(random);
        var msku5 = TestUtils.randomMsku(random);
        var msku6 = TestUtils.randomMsku(random);
        var msku7 = TestUtils.randomMsku(random);
        deepmindMskuRepository.save(msku1, msku222, msku3, msku4, msku5, msku6, msku7);
        mskuAvailabilityMatrixRepository.save(
            mskuMatrix(msku1.getId(), ROSTOV_ID, BlockReasonKey.MSKU_LEGAL_REQUIREMENTS),
            mskuMatrix(msku222.getId(), ROSTOV_ID, BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS),
            mskuMatrix(msku3.getId(), ROSTOV_ID, BlockReasonKey.MSKU_TOP_UP_SCHEME),
            mskuMatrix(msku4.getId(), ROSTOV_ID, BlockReasonKey.MSKU_GOODS_STORAGE_SPACE),
            mskuMatrix(msku5.getId(), ROSTOV_ID, BlockReasonKey.MSKU_SAFETY_REQUIREMENTS),
            mskuMatrix(msku6.getId(), ROSTOV_ID, BlockReasonKey.OTHER),
            mskuMatrix(msku7.getId(), ROSTOV_ID, null)
        );

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setBlockReasonKeys(Set.of(
                BlockReasonKey.MSKU_LEGAL_REQUIREMENTS,
                BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS,
                BlockReasonKey.MSKU_TOP_UP_SCHEME,
                BlockReasonKey.MSKU_GOODS_STORAGE_SPACE,
                BlockReasonKey.MSKU_SAFETY_REQUIREMENTS,
                BlockReasonKey.OTHER
            )), OffsetFilter.all());
        assertThat(dataPage.getItems())
            .extracting(d -> d.getMsku().getId())
            .containsExactlyInAnyOrder(msku1.getId(), msku222.getId(), msku3.getId(),
                msku4.getId(), msku5.getId(), msku6.getId());
    }

    private MskuAvailabilityMatrix mskuMatrix(long mskuId, long whId, BlockReasonKey blockReasonKey) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(mskuId)
            .setWarehouseId(whId)
            .setCreatedAt(Instant.now())
            .setCreatedLogin("login")
            .setAvailable(null)
            .setBlockReasonKey(blockReasonKey);
    }

    @Test
    public void testMissingCargotypes() {
        // change cargotypes
        var newRostov = deepmindWarehouseRepository.findById(ROSTOV_ID).get()
            .setCargoTypeLmsIds(1L, 2L);
        var newTomilino = deepmindWarehouseRepository.findById(TOMILINO_ID).get()
            .setCargoTypeLmsIds(2L, 3L);
        var newSofino = deepmindWarehouseRepository.findById(SOFINO_ID).get()
            .setCargoTypeLmsIds();
        deepmindWarehouseRepository.save(newRostov, newTomilino, newSofino);

        // create msku
        Msku noCargo = deepmindMskuRepository.save(TestUtils.randomMsku(random)
            .setCategoryId(ROOT_CATEGORY_ID)
            .setCargoTypes((Long[]) null));
        Msku cargoOk = deepmindMskuRepository.save(TestUtils.randomMsku(random)
            .setCategoryId(ROOT_CATEGORY_ID)
            .setCargoTypes(2L));
        Msku cargoNotOk = deepmindMskuRepository.save(TestUtils.randomMsku(random)
            .setCategoryId(ROOT_CATEGORY_ID)
            .setCargoTypes(2L, 4L));

        // act
        var page = mskuController.list(new ExtendedMskuFilter()
                .setMarketSkuIds(List.of(noCargo.getId(), cargoOk.getId(),
                    cargoNotOk.getId())),
            OffsetFilter.all()
        );

        // assert
        var inheritAvailabilities = page.getItems().stream()
            .collect(Collectors.toMap(v -> v.getMsku().getId(), DisplayMskuInfo::getInheritAvailabilities));

        Assertions.assertThat(inheritAvailabilities.get(noCargo.getId()))
            .contains(
                DisplayInheritAvailabilities.from(newRostov, List.of()),
                DisplayInheritAvailabilities.from(newTomilino, List.of()),
                DisplayInheritAvailabilities.from(newSofino, List.of())
            );

        Assertions.assertThat(inheritAvailabilities.get(cargoOk.getId()))
            .contains(
                DisplayInheritAvailabilities.from(newRostov, List.of()),
                DisplayInheritAvailabilities.from(newTomilino, List.of()),
                DisplayInheritAvailabilities.from(newSofino, List.of(
                    MatrixAvailabilityUtils.mskuMissingCargoTypes(cargoOk, newSofino, List.of(2L), "null #2")))
            );

        Assertions.assertThat(inheritAvailabilities.get(cargoNotOk.getId()))
            .contains(
                DisplayInheritAvailabilities.from(newRostov, List.of(
                    MatrixAvailabilityUtils.mskuMissingCargoTypes(cargoNotOk, newRostov, List.of(4L), "null #4"))),
                DisplayInheritAvailabilities.from(newTomilino, List.of(
                    MatrixAvailabilityUtils.mskuMissingCargoTypes(cargoNotOk, newTomilino, List.of(4L), "null #4"))),
                DisplayInheritAvailabilities.from(newSofino, List.of(
                    MatrixAvailabilityUtils.mskuMissingCargoTypes(cargoNotOk, newSofino, List.of(2L, 4L),
                        "null #2, null #4")))
            );
    }

    @Test
    public void testCategoryAvailability() {
        categoryAvailabilityMatrixRepository.save(
            new CategoryAvailabilityMatrix().setCategoryId(ROOT_CATEGORY_ID)
                .setWarehouseId(ROSTOV_ID)
                .setAvailable(true),
            new CategoryAvailabilityMatrix().setCategoryId(ROOT_CATEGORY_ID)
                .setWarehouseId(TOMILINO_ID)
                .setAvailable(false)
        );

        Msku m1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(ROOT_CATEGORY_ID));
        Msku m2 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID));

        var page = mskuController.list(new ExtendedMskuFilter()
                .setMarketSkuIds(List.of(m1.getId(), m2.getId())),
            OffsetFilter.all());

        // assert
        var inheritAvailabilities = page.getItems().stream()
            .collect(Collectors.toMap(v -> v.getMsku().getId(), DisplayMskuInfo::getInheritAvailabilities));

        Assertions.assertThat(inheritAvailabilities.get(m1.getId()))
            .contains(
                DisplayInheritAvailabilities.from(rostov, List.of()),
                DisplayInheritAvailabilities.from(tomilino, List.of(
                    MatrixAvailabilityUtils.mskuInCategory(tomilino, ROOT_CATEGORY_ID, CATEGORY_NAME,
                        null, null))),
                DisplayInheritAvailabilities.from(sofino, List.of())
            );

        Assertions.assertThat(inheritAvailabilities.get(m2.getId()))
            .contains(
                DisplayInheritAvailabilities.from(rostov, List.of()),
                DisplayInheritAvailabilities.from(tomilino, List.of(
                    MatrixAvailabilityUtils.mskuInCategory(tomilino, CATEGORY_ID, CATEGORY_NAME, null, null))),
                DisplayInheritAvailabilities.from(sofino, List.of())
            );
    }

    @Test
    public void testSaveIntervals() {
        populateDbWithRandomAvailabilities();
        mskuAvailabilityMatrixRepository.save(
            randomMskuAvailabilityMatrix()
                .setAvailable(false)
                .setMarketSkuId(msku2.getId())
                .setWarehouseId(TOMILINO_ID)
        );

        DataPage<DisplayMskuInfo> dataPage = mskuController
            .list(new ExtendedMskuFilter().setMarketSkuIds(List.of(msku2.getId())), OffsetFilter.firstPage());
        MskuAvailabilityMatrix matrix = dataPage.getItems().get(0).getAvailabilityMatrixList().get(0);

        LocalDate now = LocalDate.now();
        matrix.setFromDate(now);
        matrix.setToDate(now.plus(7, DAYS));
        mskuController.save(new ArrayList<>(List.of(matrix)));

        List<MskuAvailabilityMatrix> inDb = mskuAvailabilityMatrixRepository.find(
            new MskuAvailabilityMatrixRepository.Filter().setMskuId(msku2.getId()));
        assertThat(inDb).usingElementComparatorIgnoringFields("modifiedAt").containsExactlyInAnyOrder(matrix);
    }

    @Test
    public void testSaveAsyncBlockOneMsku() {
        //Блокируем один MSKU по фильтру на складе "Ростов"
        var blockReasonKey = BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS;
        var request = new MskuUpdateAvailabilityRequest()
            .setByFilter(new MskuUpdateAvailabilityRequest.MskuRequestWithFilter()
                .setFilter(new ExtendedMskuFilter()
                    .setMarketSkuIds(List.of(msku.getId()))
                )
                .setAvailabilityByWarehouse(List.of(
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(ROSTOV_ID)
                ))
                .setBlockReasonKey(blockReasonKey)
            );

        int actionId = mskuController.saveAsync(request);
        checkBackgroundAction(actionId, "Готово. Обновлены блокировки для 1 MSKU");
    }

    @Test
    public void testSaveWithWrongBlockReason() {
        mskuAvailabilityMatrixRepository.save(
            availabilityMatrix(msku, ROSTOV_ID, true)
        );
        var blockReasonKey = BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK;
        var errorMessage = MbocErrors.get().invalidBlockReasonKeys(
            DeepmindUtils.AvailabilityMatrixType.MSKU.name(),
            BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK.getLiteral()).toString();
        assertThatThrownBy(() ->
            mskuController.save(List.of(mskuMatrix(msku.getId(), ROSTOV_ID, blockReasonKey)))
        ).hasMessageContaining(errorMessage);

        assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("available", "marketSkuId", "warehouseId")
            .contains(
                availabilityMatrix(msku, ROSTOV_ID, true)
            );

        var request = new MskuUpdateAvailabilityRequest()
            .setByFilter(new MskuUpdateAvailabilityRequest.MskuRequestWithFilter()
                .setFilter(new ExtendedMskuFilter())
                .setAvailabilityByWarehouse(List.of(
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(ROSTOV_ID)
                ))
                .setBlockReasonKey(blockReasonKey)
            );

        int actionId = mskuController.saveAsync(request);
        var action = backgroundServiceMock.getAction(actionId);
        assertThat(action).isNotNull();
        assertThat(action.getStatus()).isEqualTo(BackgroundActionStatus.ActionStatus.FAILED);
        assertThat(action.getMessage()).contains(errorMessage);

        assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("available", "marketSkuId", "warehouseId")
            .contains(
                availabilityMatrix(msku, ROSTOV_ID, true)
            );
    }

    @Test
    public void testUpdateAsyncSingleMsku() {
        mskuAvailabilityMatrixRepository.save(
            availabilityMatrix(msku, ROSTOV_ID, true)
        );

        var blockReasonKey = BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS;
        var request = new MskuUpdateAvailabilityRequest()
            .setByMsku(List.of(
                new DisplayMskuAvailability()
                    .setMarketSkuId(msku.getId())
                    .setAvailable(AvailabilityValue.BLOCKED)
                    .setWarehouseId(ROSTOV_ID)
                    .setBlockReasonKey(blockReasonKey)
            ));

        int actionId = mskuController.saveAsync(request);
        checkBackgroundAction(actionId, "Готово. Обновлены блокировки для 1 MSKU");

        assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("available", "marketSkuId", "warehouseId")
            .containsExactlyInAnyOrder(
                availabilityMatrix(msku, ROSTOV_ID, false).setBlockReasonKey(blockReasonKey)
            );
    }

    @Test
    public void testSaveAsyncBlockByWarehouse() {
        //Блокируем все MSKU на складе "Софино"
        var request = new MskuUpdateAvailabilityRequest()
            .setByFilter(new MskuUpdateAvailabilityRequest.MskuRequestWithFilter()
                .setFilter(new ExtendedMskuFilter())
                .setAvailabilityByWarehouse(List.of(
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(SOFINO_ID)
                ))
            );

        int actionId = mskuController.saveAsync(request);
        checkBackgroundAction(actionId, "Готово. Обновлены блокировки для 4 MSKU");

        assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("available", "marketSkuId", "warehouseId")
            .containsExactlyInAnyOrder(
                availabilityMatrix(msku, SOFINO_ID, false),
                availabilityMatrix(msku2, SOFINO_ID, false),
                availabilityMatrix(npdMsku, SOFINO_ID, false),
                availabilityMatrix(noStatusMsku, SOFINO_ID, false)
            );
        clearAuth();
    }

    @Test
    public void testSaveAsyncRemoveAvailability() {
        //msku запрещён на складе "Ростов"
        mskuAvailabilityMatrixRepository.save(availabilityMatrix(msku, ROSTOV_ID, false));

        //Удалим все блокировки со склада "Ростов"
        var request = new MskuUpdateAvailabilityRequest()
            .setByFilter(new MskuUpdateAvailabilityRequest.MskuRequestWithFilter()
                .setFilter(new ExtendedMskuFilter())
                .setAvailabilityByWarehouse(List.of(
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.NOT_SET)
                        .setWarehouseId(ROSTOV_ID)
                ))
            );

        int actionId = mskuController.saveAsync(request);
        checkBackgroundAction(actionId, "Готово. Обновлены блокировки для 4 MSKU");

        assertThat(mskuAvailabilityMatrixRepository.findAll()).isEmpty();
    }

    @Test
    public void testSaveAsyncChangeAvailabilityByFilter() {
        //msku запрещён на складе "Ростов"
        mskuAvailabilityMatrixRepository.save(availabilityMatrix(npdMsku, ROSTOV_ID, false));

        //Разрешаем npdMsku на складе "Ростов" по фильтру
        var request = new MskuUpdateAvailabilityRequest()
            .setByFilter(new MskuUpdateAvailabilityRequest.MskuRequestWithFilter()
                .setFilter(new ExtendedMskuFilter()
                    .setSearchText("TestTitle")
                )
                .setAvailabilityByWarehouse(List.of(
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.AVAILABLE)
                        .setWarehouseId(ROSTOV_ID)
                ))
            )
            .setComment("test_comment_async");

        int actionId = mskuController.saveAsync(request);
        checkBackgroundAction(actionId, "Готово. Обновлены блокировки для 1 MSKU");

        assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("available", "marketSkuId", "warehouseId", "comment")
            .containsExactlyInAnyOrder(
                availabilityMatrix(npdMsku, ROSTOV_ID, true).setComment("test_comment_async")
            );
    }

    @Test
    public void testSaveAsync() {
        Msku msku1 = msku;
        Msku msku3 = npdMsku;
        Msku msku4 = noStatusMsku;
        mskuController.setSaveBatchSize(2);

        /*
        GIVEN
        msku1 запрещен на складах "Ростов", "Томилино", "Софино"
        msku2 разрешён на складах "Ростов", "Томилино", "Софино"
        на msku3 и msku4 нет никаких блокировок
         */
        mskuAvailabilityMatrixRepository.save(
            availabilityMatrix(msku1, ROSTOV_ID, false),
            availabilityMatrix(msku1, TOMILINO_ID, false),
            availabilityMatrix(msku1, SOFINO_ID, false),

            availabilityMatrix(msku2, ROSTOV_ID, true),
            availabilityMatrix(msku2, TOMILINO_ID, true),
            availabilityMatrix(msku2, SOFINO_ID, true)
        );

        /*
        WHEN
        Убираем все блокировки со склада "Ростов"
        Разрешаем все msku на складе "Томилино"
        Блокируем все msku на складе "Софино"
        msku3 запрещаем на складе "Маршрут"
         */
        var request = new MskuUpdateAvailabilityRequest()
            .setByFilter(new MskuUpdateAvailabilityRequest.MskuRequestWithFilter()
                .setFilter(new ExtendedMskuFilter())
                .setAvailabilityByWarehouse(List.of(
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.NOT_SET)
                        .setWarehouseId(ROSTOV_ID),
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.AVAILABLE)
                        .setWarehouseId(TOMILINO_ID),
                    new MskuUpdateAvailabilityRequest.MskuWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(SOFINO_ID)
                ))
            )
            .setByMsku(List.of(
                new DisplayMskuAvailability()
                    .setMarketSkuId(msku3.getId())
                    .setAvailable(AvailabilityValue.BLOCKED)
                    .setWarehouseId(MARSHRUT_ID)
            ));

        int actionId = mskuController.saveAsync(request);
        checkBackgroundAction(actionId, "Готово. Обновлены блокировки для 4 MSKU");

        /*
        THEN
        На складе "Ростов" нет блокировок.
        На складе "Томилино" все msku разрешены.
        На складе "Софино" все msku запрещены.
        msku3 запрещен на складе "Маршрут"
         */
        assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("available", "marketSkuId", "warehouseId")
            .containsExactlyInAnyOrder(
                availabilityMatrix(msku1, TOMILINO_ID, true),
                availabilityMatrix(msku2, TOMILINO_ID, true),
                availabilityMatrix(msku3, TOMILINO_ID, true),
                availabilityMatrix(msku4, TOMILINO_ID, true),

                availabilityMatrix(msku1, SOFINO_ID, false),
                availabilityMatrix(msku2, SOFINO_ID, false),
                availabilityMatrix(msku3, SOFINO_ID, false),
                availabilityMatrix(msku4, SOFINO_ID, false),

                availabilityMatrix(msku3, MARSHRUT_ID, false)
            );
    }

    @Test
    public void testCountMsku() {
        deepmindMskuRepository.save(
            randomMsku(),
            randomMsku().setCategoryId(123L),
            randomMsku().setTitle("test msku 1"),
            randomMsku().setTitle("test msku 2")
        );

        int totalCount = mskuController.countMsku(new ExtendedMskuFilter());
        int byCategoryCount = mskuController.countMsku(new ExtendedMskuFilter().setCategoryIds(List.of(123L)));
        int byTextCount = mskuController.countMsku(new ExtendedMskuFilter().setSearchText("test msku"));

        assertThat(totalCount).isEqualTo(8); // 4 создали в этом тесте + 4 создали в setUp()
        assertThat(byCategoryCount).isEqualTo(1);
        assertThat(byTextCount).isEqualTo(2);
    }

    @Test
    public void testSaveComment() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(1111).setCategoryId(ROOT_CATEGORY_ID),
            TestUtils.newMsku(2222).setCategoryId(ROOT_CATEGORY_ID),
            TestUtils.newMsku(3333).setCategoryId(ROOT_CATEGORY_ID),
            TestUtils.newMsku(4444).setCategoryId(ROOT_CATEGORY_ID)
        );

        mskuAvailabilityMatrixRepository.save(
            availabilityMatrix(1111, ROSTOV_ID, false).setComment("Will be modified"),
            availabilityMatrix(2222, ROSTOV_ID, false),
            availabilityMatrix(3333, ROSTOV_ID, false).setComment("Will be deleted comment")
        );

        mskuController.save(List.of(
            availabilityMatrix(1111, ROSTOV_ID, false).setComment("#1 Modified comment"),
            availabilityMatrix(2222, ROSTOV_ID, false).setComment("#2 Created comment"),
            availabilityMatrix(3333, ROSTOV_ID, false),
            availabilityMatrix(4444, ROSTOV_ID, false).setComment("#3 New availability comment")
        ));

        var result = mskuController.list(new ExtendedMskuFilter(), OffsetFilter.all())
            .getItems()
            .stream()
            .filter(it -> !it.getAvailabilityMatrixList().isEmpty())
            .collect(Collectors.toMap(
                displayMskuInfo -> displayMskuInfo.getMsku().getId(),
                it -> {
                    String comment = it.getAvailabilityMatrixList().get(0).getComment();
                    return comment == null ? "" : comment;
                }
            ));

        assertThat(result)
            .containsOnly(
                Map.entry(1111L, "#1 Modified comment"),
                Map.entry(2222L, "#2 Created comment"),
                Map.entry(3333L, ""),
                Map.entry(4444L, "#3 New availability comment")
            );
    }

    @Test
    public void testSaveWithBlockReasonKey() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(2222).setCategoryId(ROOT_CATEGORY_ID),
            TestUtils.newMsku(3333).setCategoryId(ROOT_CATEGORY_ID)
        );

        mskuAvailabilityMatrixRepository.save(
            availabilityMatrix(2222, ROSTOV_ID, false),
            availabilityMatrix(3333, ROSTOV_ID, false).setComment("Will be deleted comment")
        );

        var blockReasonKey = BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS;
        mskuController.save(List.of(
            availabilityMatrix(2222, ROSTOV_ID, false).setComment("#2 Created comment"),
            availabilityMatrix(3333, ROSTOV_ID, false).setBlockReasonKey(blockReasonKey)
        ));

        var result = mskuController.list(new ExtendedMskuFilter(), OffsetFilter.all());

        assertThat(result.getItems().stream().filter(d -> d.getMsku().getId() == 3333).findFirst())
            .get()
            .extracting(x -> x.getAvailability(ROSTOV_ID).get().getBlockReasonKey())
            .isEqualTo(blockReasonKey);
    }

    @Test
    public void exportShouldNotFindDeletedMkus() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(1111).setCategoryId(ROOT_CATEGORY_ID),
            TestUtils.newMsku(2222).setCategoryId(ROOT_CATEGORY_ID)
        );
        deepmindMskuRepository.delete(2222L);

        var actionId = mskuController.exportToExcelAsync(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(1111L, 2222L)));

        var excelFile = excelFileDownloader.downloadExport(actionId);
        DeepmindAssertions.assertThat(excelFile)
            .hasLastLine(1)
            .containsValue(1, MSKU_ID_KEY, 1111);
    }

    @Test
    public void exportSimpleExcel() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(1111).setCategoryId(ROOT_CATEGORY_ID),
            TestUtils.newMsku(2222).setCategoryId(CATEGORY_ID)
        );
        var mskuInfo1 = mskuInfo(1111L).setPricebandId(111L).setPricebandLabel("123-345");
        var mskuInfo2 = mskuInfo(2222L).setInTargetAssortment(true);
        mskuInfoRepository.save(mskuInfo1, mskuInfo2);

        mskuController.save(List.of(
            availabilityMatrix(1111, ROSTOV_ID, false).setComment("#1 Modified comment"),
            availabilityMatrix(1111, SOFINO_ID, true).setComment("#1 Modified comment")
        ));

        // save inherited
        categoryAvailabilityMatrixRepository.save(
            new CategoryAvailabilityMatrix().setCategoryId(ROOT_CATEGORY_ID)
                .setWarehouseId(ROSTOV_ID)
                .setAvailable(true),
            new CategoryAvailabilityMatrix().setCategoryId(ROOT_CATEGORY_ID)
                .setWarehouseId(TOMILINO_ID)
                .setAvailable(false)
        );

        int actionId = mskuController.exportToExcelAsync(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(1111L, 2222L)));
        ExcelFile excelFile = excelFileDownloader.downloadExport(actionId);

        DeepmindAssertions.assertThat(excelFile)
            // 1
            .containsValue(1, MSKU_ID_KEY, 2222L)
            .containsValue(1, IN_TARGET_ASSORTMENT, true)
            .containsValue(1, PRICEBAND_ID, null)
            .containsValue(1, PRICEBAND_LABEL, null)
            // availability
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(marshrut), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(sofino), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(tomilino), NOT_AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(rostov), AVAILABLE_INHERITED)
            // 2
            .containsValue(2, MSKU_ID_KEY, 1111L)
            .containsValue(2, IN_TARGET_ASSORTMENT, false)
            .containsValue(2, PRICEBAND_ID, 111)
            .containsValue(2, PRICEBAND_LABEL, "123-345")
            // availability
            .containsValue(2, ExcelUtils.convertWarehouseToExcelHeader(marshrut), AVAILABLE_INHERITED)
            .containsValue(2, ExcelUtils.convertWarehouseToExcelHeader(sofino), AVAILABLE)
            .containsValue(2, ExcelUtils.convertWarehouseToExcelHeader(tomilino), NOT_AVAILABLE_INHERITED)
            .containsValue(2, ExcelUtils.convertWarehouseToExcelHeader(rostov), NOT_AVAILABLE)
            .hasLastLine(2);
    }

    @Test
    public void exportMatrixWithPreviousDate() {
        deepmindMskuRepository.save(TestUtils.newMsku(1111).setCategoryId(ROOT_CATEGORY_ID));

        mskuController.save(List.of(
            availabilityMatrix(1111, ROSTOV_ID, false)
                .setToDate(LocalDate.now().minusDays(1)),
            availabilityMatrix(1111, TOMILINO_ID, false)
                .setToDate(LocalDate.now().plusDays(1)),
            availabilityMatrix(1111, ROSTOV_ID, true)
                .setToDate(LocalDate.now())
        ));

        int actionId = mskuController.exportToExcelAsync(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(1111L)));
        ExcelFile excelFile = excelFileDownloader.downloadExport(actionId);

        DeepmindAssertions.assertThat(excelFile)
            .containsValue(1, MSKU_ID_KEY, 1111L)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(marshrut), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(sofino), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(tomilino),
                NOT_AVAILABLE + " до " + BaseBackgroundExportable.format(LocalDate.now().plusDays(1)))
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(rostov),
                AVAILABLE + " до " + BaseBackgroundExportable.format(LocalDate.now()));
    }

    @Test
    public void testStats() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(offer(42, "ssku1", msku));
        serviceOfferReplicaRepository.save(offer(42, "ssku2", msku));
        serviceOfferReplicaRepository.save(offer(42, "ssku3", msku2));

        statsMskuMatrixRepository.sync();

        var data = mskuController.statsMskuMatrix(List.of(msku.getId(), msku2.getId()));
        Assertions.assertThat(data).isEmpty();

        //          MARSHRUT TOMILIMO
        // ssku1    0      +
        // ssku2    +      -
        // ssku3    -      + (до 07/07/2021)
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(42).setShopSku("ssku1")
                .setWarehouseId(TOMILINO_ID).setAvailable(true),
            new SskuAvailabilityMatrix()
                .setSupplierId(42).setShopSku("ssku2")
                .setWarehouseId(MARSHRUT_ID).setAvailable(true),
            new SskuAvailabilityMatrix()
                .setSupplierId(42).setShopSku("ssku2")
                .setWarehouseId(TOMILINO_ID).setAvailable(false),
            new SskuAvailabilityMatrix()
                .setSupplierId(42).setShopSku("ssku3")
                .setWarehouseId(MARSHRUT_ID).setAvailable(false),
            new SskuAvailabilityMatrix()
                .setSupplierId(42).setShopSku("ssku3")
                .setWarehouseId(TOMILINO_ID).setAvailable(true)
                .setDateTo(LocalDate.parse("2021-07-07"))
        );

        statsMskuMatrixRepository.sync();

        data = mskuController.statsMskuMatrix(List.of(msku.getId(), msku2.getId()));
        Assertions.assertThat(data).containsExactlyInAnyOrder(
            new StatsMskuMatrix().setMskuId(msku.getId()).setWarehouseId(MARSHRUT_ID)
                .setAvailableCount(1).setNotAvailableCount(0),
            new StatsMskuMatrix().setMskuId(msku.getId()).setWarehouseId(TOMILINO_ID)
                .setAvailableCount(1).setNotAvailableCount(1),
            new StatsMskuMatrix().setMskuId(msku2.getId()).setWarehouseId(MARSHRUT_ID)
                .setAvailableCount(0).setNotAvailableCount(1)
        );
    }

    private void checkBackgroundAction(int actionId, String state) {
        var action = backgroundServiceMock.getAction(actionId);
        assertThat(action).isEqualToComparingFieldByField(BackgroundActionStatus.success(state));
    }

    private MskuAvailabilityMatrix availabilityMatrix(Msku msku, long warehouseId, boolean available) {
        return new MskuAvailabilityMatrix()
            .setAvailable(available)
            .setMarketSkuId(msku.getId())
            .setWarehouseId(warehouseId);
    }

    private MskuAvailabilityMatrix availabilityMatrix(long mskuId, long warehouseId, boolean available) {
        return new MskuAvailabilityMatrix()
            .setAvailable(available)
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId);
    }

    private ru.yandex.market.mboc.common.offers.model.Offer createOffer(int supplierId, String shopSku, Msku msku) {
        return new ru.yandex.market.mboc.common.offers.model.Offer()
            .setBusinessId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setShopCategoryName("Category " + msku.getCategoryId())
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setCategoryIdForTests(99L, ru.yandex.market.mboc.common.offers.model.Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(
                new ru.yandex.market.mboc.common.offers.model.Offer.Mapping(msku.getId(), DateTimeUtils.dateTimeNow()),
                ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT)
            .setServiceOffers(supplierId);
    }

    private ru.yandex.market.mboc.common.offers.model.Offer createOffer(
        int supplierId, SupplierType supplierType, String shopSku, Msku msku) {
        return createOffer(supplierId, shopSku, msku)
            .setServiceOffers(List.of(new ru.yandex.market.mboc.common.offers.model.Offer.ServiceOffer(
                supplierId, MbocSupplierType.valueOf(supplierType.name()), OK)));
    }

    private ServiceOfferReplica offer(int supplierId, String shopSku, Msku msku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(msku.getId())
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ServiceOfferReplica offer(int supplierId, SupplierType supplierType, String shopSku, Msku msku) {
        return offer(supplierId, shopSku, msku).setSupplierType(supplierType);
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

    private void populateDbWithRandomAvailabilities() {
        List<MskuAvailabilityMatrix> randomList = IntStream.range(0, 10).boxed()
            .map(i -> randomMskuAvailabilityMatrix().setMarketSkuId(msku.getId()))
            .collect(Collectors.toList());
        mskuAvailabilityMatrices = mskuAvailabilityMatrixRepository.save(randomList);
    }

    private MskuAvailabilityMatrix randomMskuAvailabilityMatrix() {
        return random.nextObject(MskuAvailabilityMatrix.class)
            .setId(null)
            .setFromDate(null)
            .setToDate(null)
            .setBlockReasonKey(null);
    }

    private CategoryManager newManagerCategory(String login, long categoryId) {
        return new CategoryManager().setStaffLogin(login).setCategoryId(categoryId).setRole(CATMAN)
            .setFirstName("").setLastName("");
    }

    private Supplier supplier(int id, @Nullable Integer businessId, SupplierType type) {
        return new Supplier().setId(id).setBusinessId(businessId).setName("Supplier").setSupplierType(type);
    }

    private MskuInfo mskuInfo(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(false);
    }
}
