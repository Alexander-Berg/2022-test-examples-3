package ru.yandex.market.mboc.common.services.imports;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.TaskRecord;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.eats_supplier.EatsBusinessesCacheImpl;
import ru.yandex.market.mboc.common.services.eats_supplier.YtEatsLavkaPartner;
import ru.yandex.market.mboc.common.services.eats_supplier.YtEatsLavkaPartnersReader;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.partner.event.PartnerInfo;

public class SupplierImportServiceTest extends BaseDbTestClass {
    @Value("${mboc.beru.businessId}")
    private int beruBusinessId;
    @Value("${taskqueue.tables.schema}")
    private String taskQueueTablesSchema;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private OfferRepository offerRepository;

    private YtEatsLavkaPartnersReader ytEatsLavkaPartnersReader;
    private SupplierImportService supplierImportService;
    private TaskQueueRepository taskQueueRepository;

    @Before
    public void setUp() {
        storageKeyValueService.invalidateCache();
        ytEatsLavkaPartnersReader = Mockito.mock(YtEatsLavkaPartnersReader.class);

        taskQueueRepository = new TaskQueueRepository(
            namedParameterJdbcTemplate,
            transactionTemplate,
            taskQueueTablesSchema
        );
        var taskQueueRegistrator = new TaskQueueRegistrator(
            taskQueueRepository,
            new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
        );
        supplierImportService = new SupplierImportService(
            beruBusinessId,
            supplierRepository,
            offerRepository,
            TransactionHelper.MOCK,
            migrationStatusRepository,
            categoryInfoCache,
            taskQueueRegistrator,
            new EatsBusinessesCacheImpl(ytEatsLavkaPartnersReader),
            storageKeyValueService
        );
    }

    @Test
    public void testImportInvalid() {
        int businessId = 100;
        int supplierId = 1;

        var noIdNoName = PartnerInfo.PartnerInfoEvent.newBuilder()
            .build();
        var differentSupplierType = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(supplierId)
            .setBusinessId(businessId)
            .setName("different supplier type test")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.ONE_P)
            .build();
        var missingRealId = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(2L)
            .setName("missing real id test")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.REAL)
            .build();

        supplierRepository.insert(new Supplier(businessId, "business").setType(MbocSupplierType.BUSINESS));
        supplierRepository.insert(new Supplier(supplierId, "test").setType(MbocSupplierType.THIRD_PARTY));
        offerRepository.insertOffer(OfferTestUtils
            .createOffer(1, businessId, "ssku1", 1)
            .addNewServiceOfferIfNotExistsForTests(
                new Supplier(supplierId, "3P").setType(MbocSupplierType.THIRD_PARTY))
        );

        Map<PartnerInfo.PartnerInfoEvent, List<String>> results = new HashMap<>();

        supplierImportService.importSuppliers(
            List.of(noIdNoName, differentSupplierType, missingRealId),
            results::put
        );

        Assertions.assertThat(results)
            .containsOnly(
                Map.entry(noIdNoName, List.of("Id is missing", "Name is missing")),
                Map.entry(differentSupplierType, List.of(
                    "Supplier type [FIRST_PARTY] is different from existing supplier's type [THIRD_PARTY]")),
                Map.entry(missingRealId, List.of("Missing real supplier id for real supplier"))
            );
    }

    @Test
    public void testDifferentSupplierTypeFor1P() {
        int supplierId = 1;
        var differentSupplierType = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(supplierId)
            .setBusinessId(100)
            .setName("different supplier type test")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setIsDropshipBySeller(true)
            .setRealSupplierId("real-id")
            .build();

        supplierRepository.insert(new Supplier(supplierId, "test")
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("real-id"));
        offerRepository.insertOffer(OfferTestUtils
            .createOffer(1, supplierId, "ssku1", 1)
            .addNewServiceOfferIfNotExistsForTests(
                new Supplier(supplierId, "1P").setType(MbocSupplierType.REAL_SUPPLIER)
            )
        );

        Map<PartnerInfo.PartnerInfoEvent, List<String>> validationErrors = new HashMap<>();

        supplierImportService.importSuppliers(
            List.of(differentSupplierType),
            validationErrors::put
        );

        Assertions.assertThat(validationErrors)
            .containsOnly(
                Map.entry(differentSupplierType, List.of(
                    "Supplier type [THIRD_PARTY] is different from existing supplier's type [REAL_SUPPLIER]"))
            );

    }

    @Test
    public void testImportNew() {
        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org")
            .setBusinessId(100L)
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(true)
            .setIsUnitedCatalog(false)
            .setSellsMedicine(false)
            .setSellsJewerly(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Supplier actualSupplier = supplierRepository.findById(Math.toIntExact(event.getId()));

        Assertions.assertThat(actualSupplier).isEqualTo(new Supplier()
            .setId(Math.toIntExact(event.getId()))
            .setName(event.getName())
            .setDomain(event.getDomain())
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName(event.getOrganizationName())
            .setMbiBusinessId(Math.toIntExact(event.getBusinessId()))
            .setRealSupplierId(null)
            .setNewContentPipeline(event.getNewContentPipeline())
            .setDropship(event.getIsDropship())
            .setFulfillment(event.getIsFullfilment())
            .setCrossdock(event.getIsCrossdock())
            .setClickAndCollect(event.getIsClickAndCollect())
            .setDropshipBySeller(event.getIsDropshipBySeller())
            .setDatacamp(event.getIsUnitedCatalog())
            .setSellsMedicine(event.getSellsMedicine())
            .setSellsJewelry(event.getSellsJewerly()));
    }

    @Test
    public void testImportReal() {
        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.REAL)
            .setOrganizationName("some org")
            .setRealSupplierId("real-id")
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(true)
            .setIsUnitedCatalog(false)
            .setSellsMedicine(true)
            .setSellsJewerly(false)
            .build();

        supplierRepository.insert(new Supplier()
            .setId(beruBusinessId)
            .setName("Beru business")
            .setType(MbocSupplierType.BUSINESS)
        );

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Supplier actualSupplier = supplierRepository.findById(Math.toIntExact(event.getId()));

        Assertions.assertThat(actualSupplier).isEqualTo(new Supplier()
            .setId(Math.toIntExact(event.getId()))
            .setName(event.getName())
            .setDomain(event.getDomain())
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setOrganizationName(event.getOrganizationName())
            .setBusinessId(null)
            .setMbiBusinessId(beruBusinessId)
            .setRealSupplierId(event.getRealSupplierId())
            .setNewContentPipeline(false)
            .setDropship(event.getIsDropship())
            .setFulfillment(event.getIsFullfilment())
            .setCrossdock(event.getIsCrossdock())
            .setClickAndCollect(event.getIsClickAndCollect())
            .setDropshipBySeller(event.getIsDropshipBySeller())
            .setDatacamp(event.getIsUnitedCatalog())
            .setSellsMedicine(event.getSellsMedicine())
            .setSellsJewelry(event.getSellsJewerly()));
    }

    @Test
    public void testUpdateExisting() {
        Supplier business = new Supplier(100, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier supplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(100)
            .setMbiBusinessId(100);

        supplierRepository.insert(business.copy());
        supplierRepository.insert(supplier.copy());

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test update")
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org update")
            .setBusinessId(100L)
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(true)
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll())
            .containsExactlyInAnyOrder(
                supplier
                    .setName(event.getName())
                    .setDomain(event.getDomain())
                    .setOrganizationName(event.getOrganizationName())
                    .setNewContentPipeline(event.getNewContentPipeline())
                    .setDropship(event.getIsDropship())
                    .setFulfillment(event.getIsFullfilment())
                    .setCrossdock(event.getIsCrossdock())
                    .setClickAndCollect(event.getIsClickAndCollect())
                    .setDropshipBySeller(event.getIsDropshipBySeller())
                    .setDatacamp(event.getIsUnitedCatalog()),
                business
                    .setNewContentPipeline(event.getNewContentPipeline())
                    .setDropship(event.getIsDropship())
                    .setFulfillment(event.getIsFullfilment())
                    .setCrossdock(event.getIsCrossdock())
                    .setClickAndCollect(event.getIsClickAndCollect())
                    .setDropshipBySeller(event.getIsDropshipBySeller())
            );
    }

    @Test
    public void testUpdateExistingBusiness() {
        Supplier business = new Supplier(100, "business")
            .setDomain("domain")
            .setType(MbocSupplierType.BUSINESS)
            .setOrganizationName("some org")
            .setBusinessId(null)
            .setMbiBusinessId(null)
            .setNewContentPipeline(false);
        Supplier supplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(100)
            .setMbiBusinessId(100)
            .setNewContentPipeline(true);

        supplierRepository.insert(business.copy());
        supplierRepository.insert(supplier.copy());

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(100L)
            .setName("business update")
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org update")
            .setBusinessId(0)
            .setNewContentPipeline(false)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll())
            .containsExactlyInAnyOrder(
                supplier,
                business.setName(event.getName())
                    .setDomain(event.getDomain())
                    .setOrganizationName(event.getOrganizationName())
                    .setNewContentPipeline(true)
            );

    }

    @Test
    public void testCanNowSellJewelry() {
        Supplier business = new Supplier(100, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier supplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(100)
            .setMbiBusinessId(100)
            .setSellsJewelry(false);

        long jewelryCategoryId1 = 19990;
        long jewelryCategoryId2 = 19995;
        categoryInfoRepository.insertBatch(
            new CategoryInfo(jewelryCategoryId1).addTag(CategoryInfo.CategoryTag.JEWELRY),
            new CategoryInfo(jewelryCategoryId2).addTag(CategoryInfo.CategoryTag.JEWELRY)
        );

        supplierRepository.insert(business.copy());
        supplierRepository.insert(supplier.copy());

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test update")
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org update")
            .setBusinessId(100L)
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(true)
            .setIsUnitedCatalog(true)
            .setSellsJewerly(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll())
            .containsExactlyInAnyOrder(
                supplier
                    .setName(event.getName())
                    .setDomain(event.getDomain())
                    .setOrganizationName(event.getOrganizationName())
                    .setNewContentPipeline(event.getNewContentPipeline())
                    .setDropship(event.getIsDropship())
                    .setFulfillment(event.getIsFullfilment())
                    .setCrossdock(event.getIsCrossdock())
                    .setClickAndCollect(event.getIsClickAndCollect())
                    .setDropshipBySeller(event.getIsDropshipBySeller())
                    .setDatacamp(event.getIsUnitedCatalog())
                    .setSellsJewelry(event.getSellsJewerly()),
                business
                    .setNewContentPipeline(event.getNewContentPipeline())
                    .setDropship(event.getIsDropship())
                    .setFulfillment(event.getIsFullfilment())
                    .setCrossdock(event.getIsCrossdock())
                    .setClickAndCollect(event.getIsClickAndCollect())
                    .setDropshipBySeller(event.getIsDropshipBySeller())
            );
        Assertions.assertThat(taskQueueRepository.findAll())
            .extracting(TaskRecord::getTaskData)
            .containsExactlyInAnyOrder(
                "{\"filter\":{\"categoryIds\":[" + jewelryCategoryId1 + ","
                    + jewelryCategoryId2 + "],\"vendorIds\":[],\"businessIds\":[" + business.getId() + "]}}"
            );
    }

    @Test
    public void testUpdateExistingWithChangedTypeAndNoOffers() {
        int supplierId = 1;
        var differentSupplierType = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(supplierId)
            .setBusinessId(100L)
            .setName("different supplier type test")
            .setType(PartnerInfo.MbiPartnerType.SHOP)
            .setIsDropshipBySeller(true)
            .build();
        supplierRepository.insert(new Supplier(supplierId, "test").setType(MbocSupplierType.MARKET_SHOP));

        Map<PartnerInfo.PartnerInfoEvent, List<String>> validationErrors = new HashMap<>();

        supplierImportService.importSuppliers(
            List.of(differentSupplierType),
            validationErrors::put
        );

        Assertions.assertThat(validationErrors).isEmpty();

        Assertions.assertThat(supplierRepository.findById(supplierId))
            .extracting(Supplier::getType)
            .isEqualTo(MbocSupplierType.DSBS);
    }

    @Test
    public void testSupplierAddedAndRemovedFromBusiness() {
        Supplier business = new Supplier(100, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier supplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(100)
            .setMbiBusinessId(100)
            .setNewContentPipeline(true);

        Supplier supplierToMove = new Supplier()
            .setId(2)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(100)
            .setMbiBusinessId(100)
            .setNewContentPipeline(false)
            .setDropship(true)
            .setFulfillment(true)
            .setCrossdock(true)
            .setClickAndCollect(true)
            .setDropshipBySeller(true)
            .setDatacamp(true);

        supplierRepository.insert(business.copy());
        supplierRepository.insert(supplier.copy());
        supplierRepository.insert(supplierToMove.copy());

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(2L)
            .setName(supplierToMove.getName())
            .setDomain(supplierToMove.getDomain())
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName(supplierToMove.getOrganizationName())
            .setBusinessId(business.getId())
            .setNewContentPipeline(false)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(true)
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll())
            .containsExactlyInAnyOrder(
                supplier,
                supplierToMove,
                business
                    .setNewContentPipeline(false)
                    .setDropship(true)
                    .setFulfillment(true)
                    .setCrossdock(true)
                    .setClickAndCollect(true)
                    .setDropshipBySeller(true)
            );

        supplierToMove = supplierRepository.findById(supplierToMove.getId());
        supplierToMove.setBusinessId(null);
        supplierRepository.update(supplierToMove);

        event = event.toBuilder()
            .setBusinessId(0)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll())
            .containsExactlyInAnyOrder(
                supplier,
                supplierToMove
                    .setMbiBusinessId(null),
                business
                    .setNewContentPipeline(true)
                    .setDropship(false)
                    .setFulfillment(false)
                    .setCrossdock(false)
                    .setClickAndCollect(false)
                    .setDropshipBySeller(false)
            );
    }

    @Test
    public void testLinkSupplierInUnitedCatalogAndNotLinked() {
        Supplier business = new Supplier(100, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier fakeBusiness = new Supplier(1234, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier supplierChanged = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(null);
        Supplier supplierNoChanged = new Supplier()
            .setId(2)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(1234)
            .setMbiBusinessId(100);

        supplierRepository.insertBatch(business.copy(), fakeBusiness.copy(),
            supplierChanged.copy(), supplierNoChanged.copy());

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org")
            .setBusinessId(100L)
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        // supplier is auto linked to business
        Supplier updated = supplierRepository.findById(supplierChanged.getId());
        Assertions.assertThat(updated).extracting(Supplier::getBusinessId)
            .isEqualTo(100);

        event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(2L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org")
            .setBusinessId(100L)
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        // supplier already linked to other business, not changed
        Supplier notUpdated = supplierRepository.findById(supplierNoChanged.getId());
        Assertions.assertThat(notUpdated).extracting(Supplier::getBusinessId)
            .isEqualTo(1234);
    }

    @Test
    public void testLinkSupplierToBusinessNotFound() {
        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org")
            .setBusinessId(100L) // business not found
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        // supplier is auto linked to business
        Supplier updated = supplierRepository.findById(1);
        // saved without business link
        Assertions.assertThat(updated).extracting(Supplier::getMbiBusinessId).isEqualTo(100);
        Assertions.assertThat(updated).extracting(Supplier::getBusinessId).isNull();
    }

    @Test
    public void testChangeBusinessWithouMigration() {
        Supplier business = new Supplier(100, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier fakeBusiness = new Supplier(1234, "business")
            .setType(MbocSupplierType.BUSINESS);
        Supplier supplierChanged = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(100);
        Supplier supplierNoChanged = new Supplier()
            .setId(2)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setOrganizationName("some org")
            .setBusinessId(1234);

        var migrationStatus = new MigrationStatus()
            .setMigrationStatus(MigrationStatusType.ACTIVE)
            .setSupplierId(1)
            .setTargetBusinessId(1234)
            .setSourceBusinessId(100);
        migrationStatusRepository.save(migrationStatus);

        supplierRepository.insertBatch(business.copy(), fakeBusiness.copy(),
            supplierChanged.copy(), supplierNoChanged.copy());

        var event1 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org")
            .setBusinessId(1234L)
            .setIsUnitedCatalog(true)
            .build();

        var event2 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(2L)
            .setName("test")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setOrganizationName("some org")
            .setBusinessId(100L)
            .setIsUnitedCatalog(true)
            .build();

        Map<PartnerInfo.PartnerInfoEvent, List<String>> validationErrors = new HashMap<>();

        supplierImportService.importSuppliers(List.of(event1, event2), validationErrors::put);

        // supplier is auto linked to business
        Supplier updated = supplierRepository.findById(supplierChanged.getId());
        Assertions.assertThat(updated).extracting(Supplier::getBusinessId)
            .isEqualTo(1234);

        Supplier notUpdated = supplierRepository.findById(supplierNoChanged.getId());
        Assertions.assertThat(notUpdated).extracting(Supplier::getBusinessId)
            .isEqualTo(1234);

        Assertions.assertThat(validationErrors).hasSize(1);
        Assertions.assertThat(validationErrors.get(event2))
            .contains("Trying to change business id to 100 for supplier=2 without migration");
    }

    @Test
    public void testImportNotInUnitedShopOnBusiness() {
        Supplier business = new Supplier(100, "business")
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        Supplier supplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setOrganizationName("some org")
            .setBusinessId(business.getId())
            .setMbiBusinessId(business.getId());

        supplierRepository.insert(business.copy());
        supplierRepository.insert(supplier.copy());

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test update")
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.SHOP)
            .setOrganizationName("some org update")
            .setBusinessId(100L)
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(false)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll())
            .containsExactlyInAnyOrder(
                supplier
                    .setName(event.getName())
                    .setDomain(event.getDomain())
                    .setOrganizationName(event.getOrganizationName())
                    .setNewContentPipeline(event.getNewContentPipeline())
                    .setDropship(event.getIsDropship())
                    .setFulfillment(event.getIsFullfilment())
                    .setCrossdock(event.getIsCrossdock())
                    .setClickAndCollect(event.getIsClickAndCollect())
                    .setDropshipBySeller(event.getIsDropshipBySeller())
                    .setDatacamp(event.getIsUnitedCatalog())
                    .setBusinessId(null),
                business
            );

    }

    @Test
    public void testEatsImport() {
        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(100L)
            .setName("eats_1") // currently uses name
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setOrganizationName("some org update")
//            .setBusinessId(100L) no business
            .setNewContentPipeline(false)  // Eats is always new content pipeline
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        List<Supplier> suppliers = supplierRepository.findAll();
        Assertions.assertThat(suppliers).hasSize(1);

        var supplier = suppliers.get(0);
        Assertions.assertThat(supplier.isEats()).isTrue();
        Assertions.assertThat(supplier.getEffectiveServiceSupplierType()).isEqualTo(MbocSupplierType.THIRD_PARTY);
        Assertions.assertThat(supplier.isNewContentPipeline()).isTrue();
    }

    @Test
    public void testEatsImportFromMbiExport() {
        storageKeyValueService.putValue(SupplierImportService.EATS_BUSINESSES_FROM_YT_EXPORT, true);
        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(100L)
            .setName("1") // currently uses name
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setOrganizationName("some org update")
//            .setBusinessId(100L) no business
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(true)
            .build();
        Mockito.doAnswer(invocation -> {
            YtEatsLavkaPartner ytEatsLavkaPartner = new YtEatsLavkaPartner();
            ytEatsLavkaPartner.setBusinessId(event.getId());
            Consumer<YtEatsLavkaPartner> consumer = invocation.getArgument(0);
            consumer.accept(ytEatsLavkaPartner);
            return null;
        }).when(ytEatsLavkaPartnersReader).readYtEatsLavkaPartners(Mockito.any());

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        List<Supplier> suppliers = supplierRepository.findAll();
        Assertions.assertThat(suppliers).hasSize(1);

        var supplier = suppliers.get(0);
        Assertions.assertThat(supplier.isEats()).isTrue();
        Assertions.assertThat(supplier.getEffectiveServiceSupplierType()).isEqualTo(MbocSupplierType.THIRD_PARTY);
        Assertions.assertThat(supplier.isNewContentPipeline()).isTrue();
    }

    @Test
    public void whenAddBusinessAfterSupplierThenSupplierMatchesToIt() {
        int businessId = 157;

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test update")
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.SHOP)
            .setOrganizationName("some org update")
            .setBusinessId(businessId)
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(true)
            .build();

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        var newSupplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setOrganizationName("some org")
            .setMbiBusinessId(businessId)
            .setName(event.getName())
            .setDomain(event.getDomain())
            .setOrganizationName(event.getOrganizationName())
            .setNewContentPipeline(event.getNewContentPipeline())
            .setDropship(event.getIsDropship())
            .setFulfillment(event.getIsFullfilment())
            .setCrossdock(event.getIsCrossdock())
            .setClickAndCollect(event.getIsClickAndCollect())
            .setDropshipBySeller(event.getIsDropshipBySeller())
            .setDatacamp(event.getIsUnitedCatalog())
            .setBusinessId(null);

        Assertions.assertThat(supplierRepository.findAll()).containsExactly(newSupplier);

        var businessEvent = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(businessId)
            .setName("test business")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setOrganizationName("some org")
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(false)
            .build();


        supplierImportService.importSuppliers(Collections.singletonList(businessEvent), (x, y) -> {
        });

        Supplier updatedSupplier = newSupplier.setBusinessId(businessId);

        var newBusiness = new Supplier()
            .setId(businessId)
            .setName(businessEvent.getName())
            .setDomain(businessEvent.getDomain())
            .setType(MbocSupplierType.BUSINESS)
            .setOrganizationName(businessEvent.getOrganizationName())
            .setNewContentPipeline(businessEvent.getNewContentPipeline());

        Assertions.assertThat(supplierRepository.findAll()).containsExactlyInAnyOrder(updatedSupplier, newBusiness);
    }

    @Test
    public void whenAddedEventWithoutBusinessesThenOk() {
        var businessEvent = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(100)
            .setName("test business")
            .setDomain("domain")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setOrganizationName("some org")
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(false)
            .build();

        var newBusiness = new Supplier()
            .setId(100)
            .setName(businessEvent.getName())
            .setDomain(businessEvent.getDomain())
            .setType(MbocSupplierType.BUSINESS)
            .setOrganizationName(businessEvent.getOrganizationName())
            .setNewContentPipeline(businessEvent.getNewContentPipeline());

        supplierImportService.importSuppliers(Collections.singletonList(businessEvent), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll()).containsExactly(newBusiness);

        var event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test update")
            .setDomain("domain update")
            .setType(PartnerInfo.MbiPartnerType.SHOP)
            .setOrganizationName("some org update")
            .setBusinessId(100)
            .setNewContentPipeline(true)
            .setIsDropship(true)
            .setIsFullfilment(true)
            .setIsCrossdock(true)
            .setIsClickAndCollect(true)
            .setIsDropshipBySeller(false)
            .setIsUnitedCatalog(true)
            .build();

        var newSupplier = new Supplier()
            .setId(1)
            .setName("test")
            .setDomain("domain")
            .setType(MbocSupplierType.MARKET_SHOP)
            .setOrganizationName("some org")
            .setMbiBusinessId(100)
            .setName(event.getName())
            .setDomain(event.getDomain())
            .setOrganizationName(event.getOrganizationName())
            .setNewContentPipeline(event.getNewContentPipeline())
            .setDropship(event.getIsDropship())
            .setFulfillment(event.getIsFullfilment())
            .setCrossdock(event.getIsCrossdock())
            .setClickAndCollect(event.getIsClickAndCollect())
            .setDropshipBySeller(event.getIsDropshipBySeller())
            .setDatacamp(event.getIsUnitedCatalog())
            .setBusinessId(100);

        supplierImportService.importSuppliers(Collections.singletonList(event), (x, y) -> {
        });

        Assertions.assertThat(supplierRepository.findAll()).containsExactlyInAnyOrder(newBusiness, newSupplier);
    }
}
