package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.availability.web.AvailabilityValue;
import ru.yandex.market.deepmind.app.availability.web.SupplierAvailabilityUpdateRequest;
import ru.yandex.market.deepmind.app.availability.web.SupplierWebFilter;
import ru.yandex.market.deepmind.app.exportable.SupplierExportable;
import ru.yandex.market.deepmind.app.pojo.SupplierAvailability;
import ru.yandex.market.deepmind.app.utils.DeepmindUtils;
import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.ExcelFileDownloader;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.availability.task_queue.events.SupplierAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.background.BackgroundExportService;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.StatsSupplierMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SupplierAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindCargoTypeSnapshotRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.StatsSupplierMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.LockType;
import ru.yandex.market.deepmind.common.services.audit.AvailabilityMatrixAuditService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.BUSINESS;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.FIRST_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.utils.excel.ExcelUtils.convertWarehouseToExcelHeader;

/**
 * Tests of {@link SupplierAvailabilityController}.
 */
public class SupplierAvailabilityControllerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final int SUPPLIER_ID1 = 1;
    private static final int SUPPLIER_ID2 = 420;
    private static final int SUPPLIER_ID3 = 78;
    private static final int NEW_SUPPLIER = 3;
    private static final long MARKET_SKU_ID = 342;
    private static final long CATEGORY_ID = 42;
    private static final Warehouse TOMILINO = new Warehouse()
        .setId(TOMILINO_ID)
        .setName("uno")
        .setCargoTypeLmsIds(2L)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setType(WarehouseType.FULFILLMENT);
    private static final Warehouse MARSHRUT = new Warehouse()
        .setId(MARSHRUT_ID)
        .setName("dos")
        .setCargoTypeLmsIds(3L)
        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
        .setType(WarehouseType.FULFILLMENT);
    private static final Warehouse DROPSHIP = new Warehouse()
        .setId(42354L)
        .setName("dropship warehouse")
        .setType(WarehouseType.DROPSHIP);

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private DeepmindCargoTypeSnapshotRepository deepmindCargoTypeSnapshotRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;
    @Autowired
    private StatsSupplierMatrixRepository statsSupplierMatrixRepository;
    @Autowired
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Autowired
    private SupplierAvailabilityMatrixRepository supplierAvailabilityMatrixRepository;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private ExcelFileDownloader excelFileDownloader;
    private SupplierAvailabilityController controller;
    private SupplierAvailabilityMatrix matrix1;
    private SupplierAvailabilityMatrix matrix2;
    private BackgroundServiceMock backgroundServiceMock;
    private AvailableWarehouseService availableWarehouseService;

    @AfterClass
    public static void clearAuth() {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
    }

    @Before
    public void setUp() {
        SecurityContextAuthenticationUtils.setAuthenticationToken();
        deepmindWarehouseRepository.save(TOMILINO, MARSHRUT, DROPSHIP);

        deepmindSupplierRepository.save(
            YamlTestUtil.readSuppliersFromResource("availability/suppliers-for-availability-page.yml"));
        matrix1 = matrix(SUPPLIER_ID1, true, TOMILINO_ID);
        matrix2 = matrix(SUPPLIER_ID2, true, TOMILINO_ID);
        supplierAvailabilityMatrixRepository.save(List.of(matrix1, matrix2));

        // удаляем все записи из очереди, чтобы в тестах были чистые данные
        taskQueueRepository.deleteAll();

        deepmindCargoTypeSnapshotRepository.save(
            cargotype(2L, "Красный"),
            cargotype(3L, "Радиокативный"),
            cargotype(4L, "Грязный")
        );

        deepmindCategoryRepository.insert(
            new Category()
                .setCategoryId(CATEGORY_ID)
                .setName("Category 42")
                .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
        );
        var excelS3Service = new ExcelS3ServiceMock();
        backgroundServiceMock = new BackgroundServiceMock();
        excelFileDownloader = new ExcelFileDownloader(backgroundServiceMock, excelS3Service);
        availableWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT);
        controller = new SupplierAvailabilityController(
            deepmindSupplierRepository,
            supplierAvailabilityMatrixRepository,
            deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository,
            backgroundServiceMock,
            new BackgroundExportService(backgroundServiceMock, transactionTemplate, excelS3Service),
                availableWarehouseService, Mockito.mock(AvailabilityMatrixAuditService.class),
            new TransactionTemplateMock(), statsSupplierMatrixRepository);
    }

    @Test
    public void testList() {
        var page = controller.list(new SupplierWebFilter(), OffsetFilter.all());
        assertThat(page.getTotalCount()).isEqualTo(17L);

        // total count should considering only suppliers
        supplierAvailabilityMatrixRepository.save(List.of(
            matrix(SUPPLIER_ID1, true, 1L),
            matrix(SUPPLIER_ID1, true, 2L),
            matrix(SUPPLIER_ID1, true, 3L),
            matrix(SUPPLIER_ID1, true, 4L)));

        page = controller.list(new SupplierWebFilter(), OffsetFilter.all());
        assertThat(page.getTotalCount()).isEqualTo(17L);
    }

    @Test
    public void testListOffersWithBlockReasonExplicit() {
        var supplier1 = supplier(111);
        var supplier2 = supplier(222);
        var supplier3 = supplier(333);
        var supplier4 = supplier(444);
        var supplier5 = supplier(555);
        deepmindSupplierRepository.save(supplier1, supplier2, supplier3, supplier4, supplier5);
        supplierAvailabilityMatrixRepository.save(
            supplierMatrix(supplier1.getId(), MARSHRUT_ID, BlockReasonKey.SUPPLIER_LOW_SL),
            supplierMatrix(supplier2.getId(), MARSHRUT_ID, BlockReasonKey.SUPPLIER_DEBT),
            supplierMatrix(supplier3.getId(), MARSHRUT_ID, BlockReasonKey.SUPPLIER_NOT_READY_TO_SUPPLY_WH),
            supplierMatrix(supplier4.getId(), MARSHRUT_ID, BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_UNBLOCK),
            supplierMatrix(supplier5.getId(), MARSHRUT_ID, null)
        );

        var result = controller
            .list(new SupplierWebFilter().setBlockReasonKeys(Set.of(
                BlockReasonKey.SUPPLIER_DEBT,
                BlockReasonKey.SUPPLIER_LOW_SL,
                BlockReasonKey.SUPPLIER_NOT_READY_TO_SUPPLY_WH,
                BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_UNBLOCK,
                BlockReasonKey.OTHER
            )), OffsetFilter.all());
        assertThat(result.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(supplier1.getId(), supplier2.getId(), supplier3.getId(), supplier4.getId());
    }

    private Supplier supplier(int id) {
        return new Supplier()
            .setId(id)
            .setSupplierType(FIRST_PARTY)
            .setName("name_" + id)
            .setFulfillment(true);
    }

    private SupplierAvailabilityMatrix supplierMatrix(int id, long warehouseId, BlockReasonKey blockReasonKey) {
        return new SupplierAvailabilityMatrix()
            .setSupplierId(id)
            .setWarehouseId(warehouseId)
            .setAvailable(false)
            .setBlockReasonKey(blockReasonKey);
    }

    @Test
    public void testOffsetAndLimit() {
        var page = controller.list(new SupplierWebFilter(), OffsetFilter.limit(1));
        assertThat(page.getTotalCount()).isEqualTo(17L);
        assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactly(1);

        // this part checks that limit are placed in right place
        page = controller.list(new SupplierWebFilter()
            .setExistingAvailabilities(true), OffsetFilter.limit(10));

        assertThat(page.getTotalCount()).isEqualTo(2L);
        assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactly(SUPPLIER_ID1, SUPPLIER_ID2);
    }

    @Test
    public void testFilterByBusinessIdShouldReturnServiceSuppliers() {
        var page = controller.list(new SupplierWebFilter()
            .setSupplierIds(List.of(100)), OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(101, 102);

        page = controller.list(new SupplierWebFilter()
            .setSupplierIds(List.of(200, 101, 202)), OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(101, 201, 202);
    }

    @Test
    public void testFilterByBusinessAndSupplierIds() {
        var page = controller.list(new SupplierWebFilter()
            .setSupplierIdsStr(" 100"), OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(101, 102);

        page = controller.list(new SupplierWebFilter()
            .setSupplierIdsStr("200, 101,202,,"), OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(101, 201, 202);
    }

    @Test
    public void testListWithWarehouseIdAndLockedTypeTest() {
        var supplierId1 = 78;
        var supplierId2 = 79;
        supplierAvailabilityMatrixRepository.save(List.of(
            matrix(supplierId1, true, TOMILINO_ID),
            matrix(supplierId2, false, TOMILINO_ID))
        );

        var result = controller.list(new SupplierWebFilter()
            .setSupplierIdsStr(supplierId1 + ", " + supplierId2), OffsetFilter.all());
        assertThat(result.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(supplierId1, supplierId2);

        result = controller.list(new SupplierWebFilter()
            .setSupplierIdsStr(supplierId1 + ", " + supplierId2)
            .setLockedAtWarehouseId(TOMILINO_ID)
            .setLockType(LockType.EXPLICIT_LOCK), OffsetFilter.all());
        assertThat(result.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(supplierId2);

        result = controller.list(new SupplierWebFilter()
            .setSupplierIdsStr(supplierId1 + ", " + supplierId2)
            .setLockedAtWarehouseId(TOMILINO_ID)
            .setLockType(LockType.EXPLICIT_PERMISSION), OffsetFilter.all());
        assertThat(result.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(supplierId1);
    }

    @Test
    public void testDontShowBusinessSuppliers() {
        var business = deepmindSupplierRepository.find(new SupplierRepository.Filter()
            .setSupplierTypes(List.of(BUSINESS)));
        Assertions.assertThat(business).isNotEmpty();

        var page = controller.list(new SupplierWebFilter(), OffsetFilter.all());
        Assertions.assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplier)
            .extracting(ru.yandex.market.mboc.common.dict.Supplier::getType)
            .doesNotContain(MbocSupplierType.BUSINESS);
    }

    @Test
    public void testDeleteAndGet() {
        SupplierAvailabilityMatrix matrixDeleted1 = new SupplierAvailabilityMatrix(matrix1).setAvailable(null);
        SupplierAvailabilityMatrix matrixDeleted2 = new SupplierAvailabilityMatrix(matrix2).setAvailable(null);

        controller.save(List.of(
            getByWarehouse(matrixDeleted1),
            getByWarehouse(matrixDeleted2)
        ));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                new SupplierAvailabilityMatrix(matrix1).setDeleted(true),
                new SupplierAvailabilityMatrix(matrix2).setDeleted(true)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrixDeleted1.getSupplierId()),
                changedTask(matrixDeleted2.getSupplierId())
            );

        // get data
        var page = controller.list(new SupplierWebFilter(), OffsetFilter.all());

        List<SupplierAvailability.SupplierAvailabilityByWarehouse> flatList = page.getItems().stream()
            .flatMap(v -> v.getAvailabilitiesList().stream())
            .collect(Collectors.toList());
        assertThat(page.getItems()).hasSize(17);
        Assertions.assertThat(flatList).isEmpty();
    }

    @Test
    public void testDeleteUpdateInsert() {
        var matrixDeleted = new SupplierAvailabilityMatrix(matrix1).setAvailable(null);
        var matrixUpdated = new SupplierAvailabilityMatrix(matrix2).setAvailable(false);
        var matrixNew = new SupplierAvailabilityMatrix()
            .setAvailable(true).setWarehouseId(1L).setSupplierId(NEW_SUPPLIER);

        controller.save(List.of(
            getByWarehouse(matrixDeleted),
            getByWarehouse(matrixUpdated),
            getByWarehouse(matrixNew)
        ));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                new SupplierAvailabilityMatrix(matrix1).setDeleted(true),
                new SupplierAvailabilityMatrix(matrix2).setAvailable(false).setDeleted(false),
                matrixNew.setDeleted(false)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrixDeleted.getSupplierId()),
                changedTask(matrixUpdated.getSupplierId()),
                changedTask(matrixNew.getSupplierId())
            );
    }

    @Test
    public void testDeleteThenInsert() {
        // delete data
        var matrixDelete = new SupplierAvailabilityMatrix()
            .setSupplierId(SUPPLIER_ID1).setAvailable(null).setWarehouseId(TOMILINO_ID);
        controller.save(List.of(getByWarehouse(matrixDelete)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                new SupplierAvailabilityMatrix(matrix2).setDeleted(false),
                new SupplierAvailabilityMatrix(matrix1).setDeleted(true)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrixDelete.getSupplierId())
            );

        // save this data again
        SupplierAvailabilityMatrix matrixAgain = matrix(SUPPLIER_ID1, false, TOMILINO_ID);

        controller.save(List.of(getByWarehouse(matrixAgain)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                new SupplierAvailabilityMatrix(matrix2).setDeleted(false),
                new SupplierAvailabilityMatrix(matrix1).setAvailable(false).setDeleted(false)
            );

        queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrixDelete.getSupplierId()),
                changedTask(matrixDelete.getSupplierId())
            );
    }

    @Test
    public void testSavingBlockReasonKey() {
        // save data
        SupplierAvailabilityMatrix matrix = matrix(SUPPLIER_ID1, false, MARSHRUT_ID);

        controller.save(List.of(getByWarehouse(matrix)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix, matrix1, matrix2
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrix.getSupplierId())
            );

        matrix.setBlockReasonKey(BlockReasonKey.SUPPLIER_DEBT).setAvailable(true);
        // save again
        controller.save(List.of(getByWarehouse(matrix)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix, matrix1, matrix2
            );
    }

    @Test
    public void testDoubleSave() {
        // save data
        SupplierAvailabilityMatrix matrix = matrix(SUPPLIER_ID1, false, MARSHRUT_ID)
            .setBlockReasonKey(BlockReasonKey.SUPPLIER_DEBT);

        controller.save(List.of(getByWarehouse(matrix)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix, matrix1, matrix2
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrix.getSupplierId())
            );

        // save again
        controller.save(List.of(getByWarehouse(matrix)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix, matrix1, matrix2
            );

        // при повторном сохранении не должно быть записей в очереди
        queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrix.getSupplierId())
            );
    }

    @Test
    public void testDoubleDelete() {
        // delete data
        var matrixDelete = new SupplierAvailabilityMatrix()
            .setSupplierId(SUPPLIER_ID1).setAvailable(null).setWarehouseId(TOMILINO_ID);

        controller.save(List.of(getByWarehouse(matrixDelete)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                new SupplierAvailabilityMatrix(matrix2).setDeleted(false),
                new SupplierAvailabilityMatrix(matrix1).setDeleted(true)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrixDelete.getSupplierId())
            );

        // save this data again
        controller.save(List.of(getByWarehouse(matrixDelete)));

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                new SupplierAvailabilityMatrix(matrix2).setDeleted(false),
                new SupplierAvailabilityMatrix(matrix1).setDeleted(true)
            );

        // при повторном сохранении не должно быть записей в очереди
        queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(matrixDelete.getSupplierId())
            );
    }

    @Test
    public void testFilterBySupplierType() {
        var page = controller.list(
            new SupplierWebFilter().setSupplierType(SupplierType.FIRST_PARTY), OffsetFilter.all());
        assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplier)
            .usingElementComparatorOnFields("id", "type", "realSupplierId")
            .containsExactlyInAnyOrder(
                new ru.yandex.market.mboc.common.dict.Supplier().setId(465852).setType(MbocSupplierType.FIRST_PARTY),
                new ru.yandex.market.mboc.common.dict.Supplier().setId(102).setType(MbocSupplierType.REAL_SUPPLIER)
                    .setRealSupplierId("000102"),
                new ru.yandex.market.mboc.common.dict.Supplier().setId(77).setType(MbocSupplierType.REAL_SUPPLIER)
                    .setRealSupplierId("000042"),
                new ru.yandex.market.mboc.common.dict.Supplier().setId(80).setType(MbocSupplierType.REAL_SUPPLIER)
                    .setRealSupplierId("000043")
            );
    }

    @Test
    public void testFilterByAvailabilityPresented() {
        var page = controller.list(new SupplierWebFilter()
            .setExistingAvailabilities(true), OffsetFilter.all());
        assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(1, 420);

        page = controller.list(new SupplierWebFilter()
            .setExistingAvailabilities(false), OffsetFilter.all());
        assertThat(page.getItems()).hasSize(17);
    }

    @Test
    public void testFilterByApprovedMappingPresented() {
        serviceOfferReplicaRepository.save(createOffer(99, "test", MARKET_SKU_ID));

        var page = controller.list(new SupplierWebFilter().setExistingMappings(true), OffsetFilter.all());

        assertThat(page.getItems())
            .usingElementComparatorOnFields("supplierId")
            .containsExactly(new SupplierAvailability()
                .setSupplier(new ru.yandex.market.mboc.common.dict.Supplier().setId(99)));

        page = controller.list(new SupplierWebFilter().setExistingMappings(false), OffsetFilter.all());
        assertThat(page.getItems()).hasSize(17);
    }

    @Test
    public void testFilterByCategoryManager() {
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(CATEGORY_ID).setStaffLogin("test_login").setRole(CATMAN)
                .setFirstName("").setLastName("")
        );

        serviceOfferReplicaRepository.save(
            createOffer(41, "test1", MARKET_SKU_ID).setCategoryId(CATEGORY_ID),
            createOffer(42, "test2", 1L).setCategoryId(CATEGORY_ID),
            createOffer(43, "test3", 1L).setCategoryId(CATEGORY_ID + 10)
        );

        var page = controller.list(new SupplierWebFilter()
            .setManagerLogin("test_login"), OffsetFilter.all());

        assertThat(page.getItems())
            .extracting(SupplierAvailability::getSupplierId)
            .containsExactlyInAnyOrder(41, 42);
    }

    @Test
    public void testSaveAsyncByFilter() {
        //Блокируем в Ростове всех REAL_SUPPLIER поставщиков (77, 102)
        var blockReasonKey = BlockReasonKey.SUPPLIER_DEBT;
        var request = new SupplierAvailabilityUpdateRequest()
            .setByFilter(new SupplierAvailabilityUpdateRequest.SupplierRequestWithFilter()
                .setFilter(new SupplierWebFilter()
                    .setSupplierType(SupplierType.REAL_SUPPLIER)
                )
                .setAvailabilityByWarehouse(List.of(
                    new SupplierAvailabilityUpdateRequest.SupplierWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(ROSTOV_ID)
                ))
                .setBlockReasonKey(blockReasonKey)
            )
            .setComment("test_comment_async");
        controller.saveAsync(request);

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "comment", "blockReasonKey")
            .containsExactlyInAnyOrder(
                matrix(77, false, ROSTOV_ID).setComment("test_comment_async")
                    .setBlockReasonKey(blockReasonKey),
                matrix(102, false, ROSTOV_ID).setComment("test_comment_async")
                    .setBlockReasonKey(blockReasonKey),
                matrix(80, false, ROSTOV_ID).setComment("test_comment_async")
                    .setBlockReasonKey(blockReasonKey),
                matrix1, matrix2
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(77),
                changedTask(80),
                changedTask(102)
            );
    }

    @Test
    public void testSaveWithWrongBlockReason() {
        var blockReasonKey = BlockReasonKey.MSKU_IN_SEASON;
        var errorMessage = MbocErrors.get().invalidBlockReasonKeys(
            DeepmindUtils.AvailabilityMatrixType.SUPPLIER.name(),
            BlockReasonKey.MSKU_IN_SEASON.getLiteral()).toString();
        assertThatThrownBy(() -> controller.save(List.of(
            new SupplierAvailability.SupplierAvailabilityByWarehouse()
                .setSupplierId(matrix1.getSupplierId())
                .setWarehouseId(matrix1.getWarehouseId())
                .setAvailable(false)
                .setBlockReasonKey(blockReasonKey),
            new SupplierAvailability.SupplierAvailabilityByWarehouse()
                .setSupplierId(matrix2.getSupplierId())
                .setWarehouseId(matrix2.getWarehouseId())
                .setAvailable(false)
                .setBlockReasonKey(blockReasonKey)
        ))).hasMessageContaining(errorMessage);

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "comment", "blockReasonKey")
            .contains(matrix1, matrix2);

        var request = new SupplierAvailabilityUpdateRequest()
            .setByFilter(new SupplierAvailabilityUpdateRequest.SupplierRequestWithFilter()
                .setFilter(new SupplierWebFilter()
                    .setSupplierType(SupplierType.REAL_SUPPLIER)
                )
                .setAvailabilityByWarehouse(List.of(
                    new SupplierAvailabilityUpdateRequest.SupplierWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(TOMILINO_ID)
                ))
                .setBlockReasonKey(blockReasonKey)
            )
            .setComment("test_comment_async");
        var actionId = controller.saveAsync(request);
        var action = backgroundServiceMock.getAction(actionId);
        assertThat(action).isNotNull();
        assertThat(action.getStatus()).isEqualTo(BackgroundActionStatus.ActionStatus.FAILED);
        assertThat(action.getMessage()).contains(errorMessage);

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "comment", "blockReasonKey")
            .contains(matrix1, matrix2);

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .isEmpty();
    }

    @Test
    public void testSaveAsyncBySupplier() {
        var request = new SupplierAvailabilityUpdateRequest()
            .setBySupplier(List.of(new SupplierAvailability.SupplierAvailabilityByWarehouse()
                .setSupplierId(SUPPLIER_ID3)
                .setWarehouseId(MARSHRUT_ID)
                .setAvailable(false)
            ));
        controller.saveAsync(request);

        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                matrix(SUPPLIER_ID3, false, MARSHRUT_ID),
                matrix1, matrix2
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(SUPPLIER_ID3)
            );
    }

    @Test
    public void testSaveAsyncByWarehouse() {
        controller.setBatchSize(2);
        /*
        GIVEN
        Поставщик 1 заблокирован на складах "Ростов", "Томилино", "Софино"
        Поставщик 2 разрешен на складах "Ростов", "Томилино", "Софино"
        На поставщика 3 нет блокировок
         */
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            matrix(SUPPLIER_ID1, false, ROSTOV_ID),
            matrix(SUPPLIER_ID1, false, TOMILINO_ID),
            matrix(SUPPLIER_ID1, false, SOFINO_ID),

            matrix(SUPPLIER_ID2, true, ROSTOV_ID),
            matrix(SUPPLIER_ID2, true, TOMILINO_ID),
            matrix(SUPPLIER_ID2, true, SOFINO_ID)
        ));

        /*
        WHEN
        Блокируем на складе "Ростов" поставщиков 1, 2, 3
        Разрешаем на складе "Томилино" поставщиков 1, 2, 3
        Удаляем блокировки со склада "Софино" для поставщиков 1, 2, 3
        */
        var request = new SupplierAvailabilityUpdateRequest()
            .setByFilter(new SupplierAvailabilityUpdateRequest.SupplierRequestWithFilter()
                .setFilter(new SupplierWebFilter()
                    .setSupplierIds(List.of(SUPPLIER_ID1, SUPPLIER_ID2, SUPPLIER_ID3))
                )
                .setAvailabilityByWarehouse(List.of(
                    new SupplierAvailabilityUpdateRequest.SupplierWarehouseAvailability()
                        .setAvailable(AvailabilityValue.BLOCKED)
                        .setWarehouseId(ROSTOV_ID),
                    new SupplierAvailabilityUpdateRequest.SupplierWarehouseAvailability()
                        .setAvailable(AvailabilityValue.AVAILABLE)
                        .setWarehouseId(TOMILINO_ID),
                    new SupplierAvailabilityUpdateRequest.SupplierWarehouseAvailability()
                        .setAvailable(AvailabilityValue.NOT_SET)
                        .setWarehouseId(SOFINO_ID)
                ))
            );
        controller.saveAsync(request);

        /*
        THEN
        На складе "Ростов" всё заблокировано
        На складе "Томилино" всё разрешено
        Блокировки на складе "Софино" удалены
         */
        assertThat(supplierAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "warehouseId", "available", "deleted")
            .containsExactlyInAnyOrder(
                matrix(SUPPLIER_ID1, false, ROSTOV_ID),
                matrix(SUPPLIER_ID2, false, ROSTOV_ID),
                matrix(SUPPLIER_ID3, false, ROSTOV_ID),

                matrix(SUPPLIER_ID1, true, TOMILINO_ID),
                matrix(SUPPLIER_ID2, true, TOMILINO_ID),
                matrix(SUPPLIER_ID3, true, TOMILINO_ID),

                matrix(SUPPLIER_ID1, false, SOFINO_ID).setDeleted(true),
                matrix(SUPPLIER_ID2, true, SOFINO_ID).setDeleted(true)
            );
    }

    @Test
    public void testCountSuppliers() {
        int totalCount = controller.count(new SupplierWebFilter());
        int firstPartyCount = controller.count(new SupplierWebFilter().setSupplierType(SupplierType.REAL_SUPPLIER));

        assertThat(totalCount).isEqualTo(17);
        assertThat(firstPartyCount).isEqualTo(3);
    }

    @Test
    public void exportShouldConvertSupplierAvailabilitiesToLines() {
        //            TOMIL MARSH
        // supplier1  true false
        // supplier2  null false
        // supplier3  true null
        // supplier42  null null
        // supplier101 null null
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            matrix(1, true, TOMILINO.getId()),
            matrix(1, false, MARSHRUT.getId()),
            matrix(2, false, MARSHRUT.getId()),
            matrix(3, true, TOMILINO.getId())
        ));

        var filter = new SupplierWebFilter()
            .setSupplierIds(List.of(1, 2, 3, 42, 101));

        var exportId = controller.exportAllToExcelWithBigFilterAsync(filter);
        var excelFile = excelFileDownloader.downloadExport(exportId);

        List<String> headers = new ArrayList<>(SupplierExportable.HEADERS);
        availableWarehouseService.getAvailableWarehouses()
            .forEach(warehouse -> headers.add(convertWarehouseToExcelHeader(warehouse)));
        DeepmindAssertions.assertThat(excelFile)
            .containsHeadersExactly(headers)
            .containsValue(1, SupplierExportable.SUPPLIER_ID_KEY, 1)
            .containsValue(1, SupplierExportable.SUPPLIER_NAME_KEY, "Test supplier 1")
            .containsValue(1, SupplierExportable.SUPPLIER_TYPE_KEY, "3P")
            .containsValue(1, convertWarehouseToExcelHeader(TOMILINO), SupplierExportable.AVAILABLE)
            .containsValue(1, convertWarehouseToExcelHeader(MARSHRUT), SupplierExportable.NOT_AVAILABLE)
            .containsValue(2, SupplierExportable.SUPPLIER_ID_KEY, 2)
            .containsValue(2, SupplierExportable.SUPPLIER_NAME_KEY, "Test supplier 2")
            .containsValue(2, SupplierExportable.SUPPLIER_TYPE_KEY, "3P")
            .containsValue(2, convertWarehouseToExcelHeader(TOMILINO), SupplierExportable.NOTHING)
            .containsValue(2, convertWarehouseToExcelHeader(MARSHRUT), SupplierExportable.NOT_AVAILABLE)
            .containsValue(3, SupplierExportable.SUPPLIER_ID_KEY, 3)
            .containsValue(3, SupplierExportable.SUPPLIER_NAME_KEY, "Test supplier 3")
            .containsValue(3, SupplierExportable.SUPPLIER_TYPE_KEY, "3P")
            .containsValue(3, convertWarehouseToExcelHeader(TOMILINO), SupplierExportable.AVAILABLE)
            .containsValue(3, convertWarehouseToExcelHeader(MARSHRUT), SupplierExportable.NOTHING)
            .containsValue(4, SupplierExportable.SUPPLIER_ID_KEY, 42)
            .containsValue(4, SupplierExportable.SUPPLIER_NAME_KEY, "Test supplier 42")
            .containsValue(4, SupplierExportable.SUPPLIER_TYPE_KEY, "3P")
            .containsValue(4, SupplierExportable.BIZ_ID, null)
            .containsValue(4, convertWarehouseToExcelHeader(TOMILINO), SupplierExportable.NOTHING)
            .containsValue(4, convertWarehouseToExcelHeader(MARSHRUT), SupplierExportable.NOTHING)
            .containsValue(5, SupplierExportable.SUPPLIER_ID_KEY, 101)
            .containsValue(5, SupplierExportable.SUPPLIER_NAME_KEY, "Service supplier 101")
            .containsValue(5, SupplierExportable.SUPPLIER_TYPE_KEY, "3P")
            .containsValue(5, SupplierExportable.BIZ_ID, 100)
            .containsValue(5, convertWarehouseToExcelHeader(TOMILINO), SupplierExportable.NOTHING)
            .containsValue(5, convertWarehouseToExcelHeader(MARSHRUT), SupplierExportable.NOTHING)
            .hasLastLine(5);
    }

    @Test
    public void testStats() {
        serviceOfferReplicaRepository.save(createOffer(100, "ssku1001", MARKET_SKU_ID));
        serviceOfferReplicaRepository.save(createOffer(100, "ssku1002", MARKET_SKU_ID));
        serviceOfferReplicaRepository.save(createOffer(101, "ssku1011", MARKET_SKU_ID));

        statsSupplierMatrixRepository.sync();

        var data = controller.statsSupplierMatrix(List.of(100, 101));
        Assertions.assertThat(data).isEmpty();

        //          MARSHRUT TOMILIMO
        // ssku1001    0      +
        // ssku1002    +      -
        // ssku1011    -      + (до 07/07/2021)
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(100).setShopSku("ssku1001")
                .setWarehouseId(TOMILINO_ID).setAvailable(true),
            new SskuAvailabilityMatrix()
                .setSupplierId(100).setShopSku("ssku1002")
                .setWarehouseId(MARSHRUT_ID).setAvailable(true),
            new SskuAvailabilityMatrix()
                .setSupplierId(100).setShopSku("ssku1002")
                .setWarehouseId(TOMILINO_ID).setAvailable(false),
            new SskuAvailabilityMatrix()
                .setSupplierId(101).setShopSku("ssku1011")
                .setWarehouseId(MARSHRUT_ID).setAvailable(false),
            new SskuAvailabilityMatrix()
                .setSupplierId(101).setShopSku("ssku1011")
                .setWarehouseId(TOMILINO_ID).setAvailable(true)
                .setDateTo(LocalDate.parse("2021-07-07"))
        );

        statsSupplierMatrixRepository.sync();

        data = controller.statsSupplierMatrix(List.of(100, 101));
        Assertions.assertThat(data).containsExactlyInAnyOrder(
            new StatsSupplierMatrix().setSupplierId(100).setWarehouseId(MARSHRUT_ID)
                .setAvailableCount(1).setNotAvailableCount(0),
            new StatsSupplierMatrix().setSupplierId(100).setWarehouseId(TOMILINO_ID)
                .setAvailableCount(1).setNotAvailableCount(1),
            new StatsSupplierMatrix().setSupplierId(101).setWarehouseId(MARSHRUT_ID)
                .setAvailableCount(0).setNotAvailableCount(1)
        );
    }

    private SupplierAvailability.SupplierAvailabilityByWarehouse getByWarehouse(SupplierAvailabilityMatrix matrix) {
        SupplierAvailability.SupplierAvailabilityByWarehouse availability =
            new SupplierAvailability.SupplierAvailabilityByWarehouse();

        availability.setSupplierId(matrix.getSupplierId());
        availability.setWarehouseId(matrix.getWarehouseId());
        availability.setAvailable(matrix.getAvailable());
        availability.setBlockReasonKey(matrix.getBlockReasonKey());
        return availability;
    }

    private SupplierAvailabilityMatrix matrix(int supplierId, boolean available, long warehouseId) {
        return new SupplierAvailabilityMatrix().setSupplierId(supplierId)
            .setAvailable(available).setWarehouseId(warehouseId).setDeleted(false);
    }

    private static ServiceOfferReplica createOffer(int supplierId, String ssku, @Nullable Long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private SupplierAvailabilityChangedTask changedTask(int supplierId) {
        return new SupplierAvailabilityChangedTask(supplierId, "", Instant.now());
    }

    private CargoTypeSnapshot cargotype(long id, String description) {
        return new CargoTypeSnapshot()
            .setId(id)
            .setDescription(description);
    }

}
