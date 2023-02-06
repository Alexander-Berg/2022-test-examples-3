package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.services.SskuMskuStatusHelperServiceImpl;
import ru.yandex.market.deepmind.app.utils.DeepmindUtils;
import ru.yandex.market.deepmind.app.web.ssku_availability.DisplayShopSkuAvailability;
import ru.yandex.market.deepmind.app.web.ssku_availability.DisplaySskuStatus;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityToSave;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityValue;
import ru.yandex.market.deepmind.app.web.ssku_availability.WebShopSkuStatusToSave;
import ru.yandex.market.deepmind.common.ExcelFileDownloader;
import ru.yandex.market.deepmind.common.availability.warehouse.AvailableWarehouseFactory;
import ru.yandex.market.deepmind.common.background.BackgroundExportService;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AlmostDeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.DeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.hiding.DisplayHiding;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.mocks.GlobalVendorsCachingServiceMock;
import ru.yandex.market.deepmind.common.mocks.SskuStatusAuditServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AlmostDeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.DeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.audit.AvailabilityMatrixAuditService;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceMock;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.mboc.common.audit.SskuStatusAuditService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.web.IServiceOffer;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public abstract class BaseShopSkuControllerTest extends DeepmindBaseAppDbTestClass {

    protected ShopSkuStatusController statusController;
    protected ShopSkuAvailabilityController availabilityController;

    @Resource
    protected SupplierRepository deepmindSupplierRepository;
    @Resource
    protected SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    protected DeadstockStatusRepository deadstockStatusRepository;
    @Resource
    protected AlmostDeadstockStatusRepository almostDeadstockStatusRepository;
    @Resource
    protected HidingRepository hidingRepository;
    @Resource
    protected MskuRepository deepmindMskuRepository;
    @Resource
    protected MskuInfoRepository mskuInfoRepository;
    @Resource
    protected MskuStatusRepository mskuStatusRepository;
    @Resource
    protected MskuStockRepository mskuStockRepository;
    @Resource
    protected DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    protected SskuStatusRepository sskuStatusRepository;
    @Resource
    protected TransactionTemplate transactionTemplate;
    @Resource
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Resource
    protected HidingReasonDescriptionRepository hidingReasonDescriptionRepository;
    @Resource
    protected DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    protected DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    protected DeepmindUtils deepmindUtils;

    protected BackgroundServiceMock backgroundServiceMock;
    protected ExcelS3ServiceMock excelS3ServiceMock;

    protected ExcelFileDownloader excelFileDownloader;
    protected Warehouse marshrut;
    protected Warehouse sofino;
    protected Warehouse tomilino;
    protected Warehouse rostov;
    protected BackgroundExportService backgroundExportService;
    protected SskuStatusAuditService sskuStatusAuditService;
    protected AvailableWarehouseFactory availableWarehouseFactory;

    protected ShopSkuMatrixAvailabilityServiceMock shopSkuMatrixAvailabilityService;


    @Before
    public void setUp() {
        sskuStatusAuditService = new SskuStatusAuditServiceMock();
        var deepmindCategoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();

        var offersConverter = new OffersConverterImpl(jdbcTemplate.getJdbcOperations(), new BeruIdMock(),
            deepmindSupplierRepository);
        deepmindUtils = new DeepmindUtils(
            offersConverter,
            deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository,
            deepmindCategoryCachingServiceMock
        );

        backgroundServiceMock = new BackgroundServiceMock();
        excelS3ServiceMock = new ExcelS3ServiceMock();

        excelFileDownloader = new ExcelFileDownloader(backgroundServiceMock, excelS3ServiceMock);
        backgroundExportService = new BackgroundExportService(backgroundServiceMock, transactionTemplate,
            excelS3ServiceMock);

        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        var sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        shopSkuMatrixAvailabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        var sskuMskuStatusHelperService = new SskuMskuStatusHelperServiceImpl(serviceOfferReplicaRepository,
            backgroundServiceMock,
            sskuMskuStatusService, sskuMskuStatusValidationService, sskuStatusRepository, deepmindMskuRepository,
            mskuStatusRepository, transactionTemplate);

        statusController = new ShopSkuStatusController(
            serviceOfferReplicaRepository,
            deepmindSupplierRepository,
            sskuStatusRepository,
            sskuMskuStatusHelperService,
            deepmindCategoryCachingServiceMock,
            TransactionHelper.MOCK,
            backgroundExportService,
            deepmindUtils,
            sskuStatusAuditService
        );
        availableWarehouseFactory = new AvailableWarehouseFactory(
            new AvailableWarehouseServiceImpl(
                deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT
            ),
            new AvailableWarehouseServiceImpl(
                deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_CROSSDOCK
            ),
            new AvailableWarehouseServiceImpl(
                deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_DROPSHIP
            )
        );
        availabilityController = new ShopSkuAvailabilityController(
            serviceOfferReplicaRepository,
            deepmindSupplierRepository,
            sskuAvailabilityMatrixRepository,
            shopSkuMatrixAvailabilityService,
            deadstockStatusRepository,
            almostDeadstockStatusRepository,
            hidingRepository,
            sskuStatusRepository,
            deepmindCategoryCachingServiceMock,
            deepmindMskuRepository,
            mskuInfoRepository,
            new GlobalVendorsCachingServiceMock(),
            TransactionHelper.MOCK,
            backgroundServiceMock,
            backgroundExportService,
            new DeepmindCargoTypeCachingServiceMock(),
            availableWarehouseFactory,
            deepmindUtils,
            Mockito.mock(AvailabilityMatrixAuditService.class),
            statusController
        );

        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        marshrut = deepmindWarehouseRepository.findById(MARSHRUT_ID).orElseThrow();
        sofino = deepmindWarehouseRepository.findById(SOFINO_ID).orElseThrow();
        tomilino = deepmindWarehouseRepository.findById(TOMILINO_ID).orElseThrow();
        rostov = deepmindWarehouseRepository.findById(ROSTOV_ID).orElseThrow();

        availabilityController.setSaveBatchSize(2);
        availabilityController.setExportBatchSize(2);
        statusController.setSaveBatchSize(2);
        statusController.setExportBatchSize(2);
        deepmindMskuRepository.save(
            TestUtils.newMsku(1111),
            TestUtils.newMsku(3333).setTitle("test_search")
        );
    }

    protected ShopSkuAvailabilityToSave toSave(int supplierId, String shopSku, long warehouseId,
                                               ShopSkuAvailabilityValue available, String from, String to) {
        return new ShopSkuAvailabilityToSave()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId)
            .setAvailabilityValue(available)
            .setDateFrom(from == null ? null : LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .setDateTo(to == null ? null : LocalDate.parse(to, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    protected IServiceOffer displayOffer(int supplierId, String shopSku) {
        ServiceOfferReplica offerReplica = new ServiceOfferReplica();
        offerReplica.setSupplierId(supplierId);
        offerReplica.setShopSku(shopSku);
        return offerReplica;
    }

    protected SskuAvailabilityMatrix sskuAvailabilityMatrix(int supplierId, String shopSku, long warehouseId,
                                                            Boolean available, String from, String to) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setDateFrom(from == null ? null : LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .setDateTo(to == null ? null : LocalDate.parse(to, DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .setCreatedLogin(DEEPMIND_APP_TEST_USER);
    }

    protected DisplayShopSkuAvailability.DisplayAvailability displayAvailability(long warehouseId,
                                                                                 boolean available,
                                                                                 String from,
                                                                                 String to) {
        return new DisplayShopSkuAvailability.DisplayAvailability()
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setDateFrom(from == null ? null : LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .setDateTo(to == null ? null : LocalDate.parse(to, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    protected DeadstockStatus deadstockStatus(int supplierId, String shopSku, long warehouseId, String from) {
        return new DeadstockStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId)
            .setDeadstockSince(LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .setImportTs(Instant.now());
    }

    protected AlmostDeadstockStatus almostDeadstockStatus(int supplierId, String shopSku,
                                                          long warehouseId, String from) {
        return new AlmostDeadstockStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId)
            .setAlmostDeadstockSince(LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    protected DisplayShopSkuAvailability.Deadstock displayDeadstock(long warehouseId, String from) {
        return new DisplayShopSkuAvailability.Deadstock()
            .setWarehouseId(warehouseId)
            .setDeadstockSince(LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    protected DisplayShopSkuAvailability.AlmostDeadstock displayAlmostDeadstock(long warehouseId, String from) {
        return new DisplayShopSkuAvailability.AlmostDeadstock()
            .setWarehouseId(warehouseId)
            .setAlmostDeadstockSince(LocalDate.parse(from, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    protected DisplayHiding displayHiding(String reasonKey, String username, String stopWord) {
        return new DisplayHiding()
            .setReasonKey(reasonKey)
            .setUserName(username)
            .setStopWord(stopWord);
    }

    protected DisplayShopSkuAvailability find(List<DisplayShopSkuAvailability> list, int supplierId, String shopSku) {
        return list.stream()
            .filter(it -> Objects.equals(it.getShopSkuKey(), new ServiceOfferKey(supplierId, shopSku)))
            .findFirst()
            .orElseThrow();
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability) {
        insertOffer(supplierId, shopSku, availability, 111);
    }

    protected Supplier create1PSupplier(int id, String rsId) {
        return new Supplier().setId(id)
            .setName("test").setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId(rsId);
    }

    protected Supplier create3pSupplier(int id) {
        return new Supplier().setId(id).setName("test").setSupplierType(SupplierType.THIRD_PARTY);
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability, long mskuId) {
        insertOffer(supplierId, shopSku, availability, mskuId, OfferAcceptanceStatus.OK);
    }

    protected void insertOffer(int supplierId, String shopSku, OfferAvailability availability, long mskuId,
                               OfferAcceptanceStatus status) {
        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        Supplier supplier;
        if (suppliers.isEmpty()) {
            supplier = new Supplier().setId(supplierId)
                .setName("test_supplier_" + supplierId)
                .setSupplierType(SupplierType.THIRD_PARTY);
            deepmindSupplierRepository.save(supplier);
        } else {
            supplier = suppliers.get(0);
        }
        ServiceOfferReplica offer = createOffer(supplierId, shopSku, mskuId, status, supplier);
        serviceOfferReplicaRepository.save(offer);
        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(availability)
        );
    }

    protected ServiceOfferReplica createOffer(int bizId, String shopSku, long mskuId, OfferAcceptanceStatus status,
                                Supplier supplier) {
        return new ServiceOfferReplica()
            .setBusinessId(bizId)
            .setSupplierId(supplier.getId())
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplier.getSupplierType())
            .setModifiedTs(Instant.now())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(status);
    }

    protected void insertHiding(int supplierId, String shopSku, String reasonKey, String subreasonId, String username) {
        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(new HidingReasonDescription()
            .setReasonKey(reasonKey).setExtendedDesc("").setType(HidingReasonType.REASON_KEY));
        var description = hidingReasonDescriptionRepository.findByReasonKeys(reasonKey).get(0);
        hidingRepository.save(createHiding(supplierId, shopSku, subreasonId, username)
            .setReasonKeyId(description.getId()));
    }

    protected Hiding createHiding(int supplierId, String shopSku, String subreasonId, String username) {
        return new Hiding(supplierId, shopSku, Instant.now(), username,
            "test_", subreasonId, -1L, null, -1L);
    }

    protected WebShopSkuStatusToSave sskuState(int supplierId, String shopSku, OfferAvailability availability) {
        return sskuState(supplierId, shopSku, availability, null);
    }

    protected WebShopSkuStatusToSave sskuState(int supplierId, String shopSku, OfferAvailability availability,
                                               String comment) {
        return new WebShopSkuStatusToSave()
            .setShopSkuKey(new ServiceOfferKey(supplierId, shopSku))
            .setNewAvailabilityStatus(availability)
            .setComment(comment);
    }

    protected DisplaySskuStatus displaySskuStatus(int supplierId, String shopSku, OfferAvailability availability) {
        return displaySskuStatus(supplierId, shopSku, availability, null, null);
    }

    protected DisplaySskuStatus displaySskuStatus(int supplierId, String shopSku, OfferAvailability availability,
                                                  String comment,
                                                  Instant statusFinishAt) {
        var sskuStatus = sskuStatus(supplierId, shopSku, availability, comment, statusFinishAt);
        return new DisplaySskuStatus(supplierId, shopSku, sskuStatus);
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

    protected Msku msku(long mskuId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    protected MskuInfo mskuInfo(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(false);
    }
}
