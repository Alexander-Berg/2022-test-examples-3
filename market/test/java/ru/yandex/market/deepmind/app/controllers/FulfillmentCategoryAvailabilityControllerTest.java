package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.pojo.CategoryAvailabilityWebFilter;
import ru.yandex.market.deepmind.app.utils.DeepmindUtils;
import ru.yandex.market.deepmind.app.web.CategoryAvailability;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.StatsCategoryMatrix;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.StatsCategoryMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityFilter;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.services.LockType;
import ru.yandex.market.deepmind.common.services.audit.AvailabilityMatrixAuditService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.MbocErrors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public class FulfillmentCategoryAvailabilityControllerTest extends DeepmindBaseAppDbTestClass {
    private static final long CATEGORY_ID1 = 1;
    private static final long CATEGORY_ID2 = 2;
    private static final long NEW_CATEGORY_ID = 3;

    @Resource
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Resource
    private StatsCategoryMatrixRepository statsCategoryMatrixRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;

    private FulfillmentCategoryAvailabilityController controller;
    private CrossdockCategoryAvailabilityController crossdockController;

    private CategoryAvailabilityMatrix matrix1;
    private CategoryAvailabilityMatrix matrix2;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);

        matrix1 = new CategoryAvailabilityMatrix()
            .setCategoryId(CATEGORY_ID1)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        matrix2 = new CategoryAvailabilityMatrix()
            .setCategoryId(CATEGORY_ID2)
            .setWarehouseId(MARSHRUT_ID)
            .setAvailable(false);
        categoryAvailabilityMatrixRepository.save(matrix1, matrix2);

        var categoryCachingService = new DeepmindCategoryCachingServiceMock();
        categoryCachingService.addCategories(
            YamlTestUtil.readCategoriesFromResources("categories/category-tree.yml"));
        var fulfillmentWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT
        );
        var crossdockWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_CROSSDOCK
        );
        controller = new FulfillmentCategoryAvailabilityController(
            categoryAvailabilityMatrixRepository, null,
            fulfillmentWarehouseService, categoryCachingService,
            statsCategoryMatrixRepository, Mockito.mock(AvailabilityMatrixAuditService.class)
        );
        crossdockController = new CrossdockCategoryAvailabilityController(
            categoryAvailabilityMatrixRepository, null,
            crossdockWarehouseService, categoryCachingService,
            statsCategoryMatrixRepository, Mockito.mock(AvailabilityMatrixAuditService.class)
        );
    }

    @Test
    public void testList() {
        var page = controller.list(new CategoryAvailabilityWebFilter());
        assertThat(page)
            .usingElementComparatorIgnoringFields("auditInfo")
            .containsExactlyInAnyOrder(
                new CategoryAvailability(matrix1),
                new CategoryAvailability(matrix2)
            );

        CategoryAvailabilityMatrix matrixCrossDock = new CategoryAvailabilityMatrix()
            .setCategoryId(CATEGORY_ID2)
            .setWarehouseId(CROSSDOCK_SOFINO_ID)
            .setAvailable(false);

        categoryAvailabilityMatrixRepository.save(Collections.singletonList(matrixCrossDock));

        page = crossdockController.list(new CategoryAvailabilityWebFilter());
        assertThat(page)
            .usingElementComparatorIgnoringFields("auditInfo")
            .containsExactlyInAnyOrder(
                new CategoryAvailability(matrixCrossDock)
            );
    }

    @Test
    public void testListWithWarehouseIdAndLockedTypeTest() {
        var matrixId1 = 111L;
        var matrixId2 = 222L;
        categoryAvailabilityMatrixRepository.save(List.of(
            new CategoryAvailabilityMatrix()
                .setCategoryId(matrixId1)
                .setAvailable(true)
                .setWarehouseId(TOMILINO_ID),
            new CategoryAvailabilityMatrix()
                .setCategoryId(matrixId2)
                .setAvailable(false)
                .setWarehouseId(TOMILINO_ID)
        ));

        var dataPage = controller.list(new CategoryAvailabilityWebFilter()
            .setCategoryIds(List.of(matrixId1, matrixId2)));
        assertThat(dataPage)
            .extracting(CategoryAvailability::getCategoryId)
            .containsExactlyInAnyOrder(matrixId1, matrixId2);

        dataPage = controller.list(new CategoryAvailabilityWebFilter()
            .setCategoryIds(List.of(matrixId1, matrixId2))
            .setLockType(LockType.EXPLICIT_LOCK)
            .setLockedAtWarehouseId(TOMILINO_ID));
        assertThat(dataPage)
            .extracting(CategoryAvailability::getCategoryId)
            .containsExactlyInAnyOrder(matrixId2);

        dataPage = controller.list(new CategoryAvailabilityWebFilter()
            .setCategoryIds(List.of(matrixId1, matrixId2))
            .setLockType(LockType.EXPLICIT_PERMISSION)
            .setLockedAtWarehouseId(TOMILINO_ID));
        assertThat(dataPage)
            .extracting(CategoryAvailability::getCategoryId)
            .containsExactlyInAnyOrder(matrixId1);
    }

    @Test
    public void testListOffersWithBlockReasonExplicit() {
        var category1 = category(111);
        var category2 = category(222);
        var category3 = category(333);
        var category4 = category(444);
        var category5 = category(555);
        var category6 = category(666);
        var category7 = category(777);
        deepmindCategoryRepository
            .insertBatch(category1, category2, category3, category4, category5, category6, category7);
        categoryAvailabilityMatrixRepository.save(
            categoryMatrix(category1.getCategoryId(), ROSTOV_ID, BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS),
            categoryMatrix(category2.getCategoryId(), ROSTOV_ID, BlockReasonKey.CATEGORY_GOODS_STORAGE_CONDITIONS),
            categoryMatrix(category3.getCategoryId(), ROSTOV_ID, BlockReasonKey.CATEGORY_TOP_UP_SCHEME),
            categoryMatrix(category4.getCategoryId(), ROSTOV_ID, BlockReasonKey.CATEGORY_GOODS_STORAGE_SPACE),
            categoryMatrix(category5.getCategoryId(), ROSTOV_ID, BlockReasonKey.CATEGORY_SAFETY_REQUIREMENTS),
            categoryMatrix(category6.getCategoryId(), ROSTOV_ID, BlockReasonKey.OTHER),
            categoryMatrix(category7.getCategoryId(), ROSTOV_ID, null)
        );

        var result = controller
            .list(new CategoryAvailabilityWebFilter().setBlockReasonKeys(Set.of(
                BlockReasonKey.CATEGORY_GOODS_STORAGE_SPACE,
                BlockReasonKey.CATEGORY_TOP_UP_SCHEME,
                BlockReasonKey.CATEGORY_GOODS_STORAGE_CONDITIONS,
                BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS,
                BlockReasonKey.CATEGORY_SAFETY_REQUIREMENTS,
                BlockReasonKey.OTHER
            )));
        assertThat(result)
            .extracting(CategoryAvailability::getCategoryId)
            .containsExactlyInAnyOrder(category1.getCategoryId(), category2.getCategoryId(), category3.getCategoryId(),
                category4.getCategoryId(), category5.getCategoryId(), category6.getCategoryId());
    }

    private Category category(long id) {
        return new Category()
            .setCategoryId(id)
            .setName("name_" + id);
    }

    private CategoryAvailabilityMatrix categoryMatrix(long id, long whId, BlockReasonKey blockReasonKey) {
        return new CategoryAvailabilityMatrix()
            .setCategoryId(id)
            .setWarehouseId(whId)
            .setAvailable(false)
            .setCreatedAt(Instant.now())
            .setCreatedLogin("login")
            .setBlockReasonKey(blockReasonKey);
    }

    @Test
    public void testDeleteUpdateInsert() {
        CategoryAvailabilityMatrix matrix1Deleted = new CategoryAvailabilityMatrix(matrix1).setAvailable(null);
        CategoryAvailabilityMatrix matrix2Changed = new CategoryAvailabilityMatrix(matrix2).setAvailable(true);
        CategoryAvailabilityMatrix matrixNew = new CategoryAvailabilityMatrix().setAvailable(false)
            .setWarehouseId(SOFINO_ID)
            .setCategoryId(NEW_CATEGORY_ID);

        controller.save(List.of(
            new CategoryAvailability(matrix1Deleted),
            new CategoryAvailability(matrix2Changed),
            new CategoryAvailability(matrixNew)
        ));

        assertThat(categoryAvailabilityMatrixRepository
            .find(new CategoryAvailabilityFilter()))
            .usingElementComparatorOnFields("categoryId", "warehouseId", "available")
            .containsExactlyInAnyOrder(matrix2Changed, matrixNew);
    }

    @Test
    public void testDelete() {
        CategoryAvailabilityMatrix matrix1Deleted = new CategoryAvailabilityMatrix(matrix1).setAvailable(null);

        controller.save(List.of(
            new CategoryAvailability(matrix1Deleted)
        ));

        assertThat(categoryAvailabilityMatrixRepository
            .find(new CategoryAvailabilityFilter()))
            .usingElementComparatorOnFields("categoryId", "warehouseId", "available")
            .containsExactlyInAnyOrder(matrix2);
    }

    @Test
    public void testUpdate() {
        CategoryAvailabilityMatrix matrix2Changed = new CategoryAvailabilityMatrix(matrix2).setAvailable(true);

        controller.save(List.of(
            new CategoryAvailability(matrix2Changed)
        ));

        assertThat(categoryAvailabilityMatrixRepository
            .find(new CategoryAvailabilityFilter()))
            .usingElementComparatorOnFields("categoryId", "warehouseId", "available")
            .containsExactlyInAnyOrder(matrix1, matrix2Changed);
    }

    @Test
    public void testSaveWithWrongBlockReason() {
        CategoryAvailabilityMatrix matrix2Changed = new CategoryAvailabilityMatrix(matrix2).setAvailable(true);

        assertThatThrownBy(() -> controller.save(List.of(
            new CategoryAvailability(matrix2Changed).setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK)
        ))).hasMessageContaining(MbocErrors.get().invalidBlockReasonKeys(
            DeepmindUtils.AvailabilityMatrixType.CATEGORY.name(),
            BlockReasonKey.SSKU_DEADSTOCK.getLiteral()).toString());

        assertThat(categoryAvailabilityMatrixRepository
            .find(new CategoryAvailabilityFilter()))
            .usingElementComparatorOnFields("categoryId", "warehouseId", "available")
            .containsExactlyInAnyOrder(matrix1,
                new CategoryAvailabilityMatrix(matrix2).setAvailable(false));
    }

    @Test
    public void testInsert() {
        CategoryAvailabilityMatrix matrixNew = new CategoryAvailabilityMatrix().setAvailable(false)
            .setWarehouseId(SOFINO_ID)
            .setCategoryId(NEW_CATEGORY_ID);

        controller.save(List.of(
            new CategoryAvailability(matrixNew)
        ));

        assertThat(categoryAvailabilityMatrixRepository
            .find(new CategoryAvailabilityFilter()))
            .usingElementComparatorOnFields("categoryId", "warehouseId", "available")
            .containsExactlyInAnyOrder(matrix1, matrix2, matrixNew);
    }

    @Test
    public void cantInsertNotProperWarehouse() {
        CategoryAvailabilityMatrix matrixNew = new CategoryAvailabilityMatrix().setAvailable(false)
            .setWarehouseId(CROSSDOCK_SOFINO_ID)
            .setCategoryId(NEW_CATEGORY_ID);

        Assertions.assertThatThrownBy(() -> controller.save(List.of(new CategoryAvailability(matrixNew))))
            .hasMessageContaining("Bad request: passed warehouses: [-172]");
    }

    @Test
    public void cantListNotProperWarehouse() {
        Assertions.assertThatThrownBy(() -> {
            controller.list(new CategoryAvailabilityWebFilter().setWarehouseIds(List.of(CROSSDOCK_SOFINO_ID)));
        }).hasMessageContaining("Bad request: passed warehouses: [-172]");
    }

    /*
    root
    => 2
    ==> 3
    ===> 4
    ==> 5
    => 6
    ==> 7
     */
    @Test
    public void testListSeveralHierarchyCategoryIds() {
        CategoryAvailabilityMatrix matrix3 = new CategoryAvailabilityMatrix()
            .setCategoryId(3L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix4 = new CategoryAvailabilityMatrix()
            .setCategoryId(4L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix5 = new CategoryAvailabilityMatrix()
            .setCategoryId(5L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix6 = new CategoryAvailabilityMatrix()
            .setCategoryId(6L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix7 = new CategoryAvailabilityMatrix()
            .setCategoryId(7L)
            .setWarehouseId(MARSHRUT_ID)
            .setAvailable(false);

        categoryAvailabilityMatrixRepository.save(Arrays.asList(matrix3, matrix4, matrix5, matrix6, matrix7));

        var page = controller.list(new CategoryAvailabilityWebFilter().setHierarchyCategoryIds(List.of(3L, 7L)));

        // возвращаем все категории вверх/вниз от переданных id
        assertThat(page)
            .usingElementComparatorIgnoringFields("auditInfo")
            .containsExactlyInAnyOrder(
                new CategoryAvailability(matrix2),
                new CategoryAvailability(matrix3),
                new CategoryAvailability(matrix4),
                new CategoryAvailability(matrix6),
                new CategoryAvailability(matrix7)
            );
    }

    /*
    root
    => 2
    ==> 3
    ===> 4
    ==> 5
    => 6
    ==> 7
     */
    @Test
    public void testListSeveralCategoryIds() {
        CategoryAvailabilityMatrix matrix3 = new CategoryAvailabilityMatrix()
            .setCategoryId(3L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix4 = new CategoryAvailabilityMatrix()
            .setCategoryId(4L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix5 = new CategoryAvailabilityMatrix()
            .setCategoryId(5L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix6 = new CategoryAvailabilityMatrix()
            .setCategoryId(6L)
            .setAvailable(true)
            .setWarehouseId(MARSHRUT_ID);
        CategoryAvailabilityMatrix matrix7 = new CategoryAvailabilityMatrix()
            .setCategoryId(7L)
            .setWarehouseId(MARSHRUT_ID)
            .setAvailable(false);

        categoryAvailabilityMatrixRepository.save(Arrays.asList(matrix3, matrix4, matrix5, matrix6, matrix7));

        var page = controller.list(new CategoryAvailabilityWebFilter().setCategoryIds(List.of(3L, 7L)));

        assertThat(page)
            .usingElementComparatorIgnoringFields("auditInfo")
            .containsExactlyInAnyOrder(
                new CategoryAvailability(matrix3),
                new CategoryAvailability(matrix7)
            );
    }

    @Test
    public void testStats() {
        deepmindMskuRepository.save(
            createMsku(100, 1),
            createMsku(101, 1),
            createMsku(102, 1),
            createMsku(200, 2),
            createMsku(201, 2).setDeleted(true)
        );

        mskuAvailabilityMatrixRepository.save(
            matrix(100, MARSHRUT_ID, false),
            matrix(101, MARSHRUT_ID, false),
            matrix(102, CROSSDOCK_SOFINO_ID, false),
            matrix(200, ROSTOV_ID, true),
            matrix(201, ROSTOV_ID, true)
        );

        statsCategoryMatrixRepository.sync();

        var statsCategoryMatrices = controller.statsCategoryMatrix();
        var crossdockMatrices = crossdockController.statsCategoryMatrix();

        Assertions.assertThat(statsCategoryMatrices)
            .containsExactlyInAnyOrder(
                new StatsCategoryMatrix(MARSHRUT_ID, 1L, 0, 2),
                new StatsCategoryMatrix(ROSTOV_ID, 2L, 1, 0)
            );
        Assertions.assertThat(crossdockMatrices)
            .containsExactlyInAnyOrder(
                new StatsCategoryMatrix(CROSSDOCK_SOFINO_ID, 1L, 0, 1)
            );
    }

    @Test
    public void dontCalculateDataNotInInterval() {
        deepmindMskuRepository.save(
            createMsku(100, 1),
            createMsku(101, 1),
            createMsku(102, 1),
            createMsku(103, 1)
        );

        mskuAvailabilityMatrixRepository.save(
            matrix(100, MARSHRUT_ID, false),
            matrix(101, MARSHRUT_ID, false)
                .setFromDate(LocalDate.parse("2020-08-01")),
            matrix(102, MARSHRUT_ID, false)
                .setToDate(LocalDate.parse("2020-07-01")),
            matrix(103, MARSHRUT_ID, false)
                .setFromDate(LocalDate.parse("2020-07-01")).setToDate(LocalDate.parse("2020-08-01"))
        );

        statsCategoryMatrixRepository.syncData(LocalDate.parse("2020-07-15"));

        var statsCategoryMatrices = controller.statsCategoryMatrix();
        Assertions.assertThat(statsCategoryMatrices)
            .containsExactlyInAnyOrder(
                new StatsCategoryMatrix(MARSHRUT_ID, 1L, 0, 2)
            );

        statsCategoryMatrixRepository.syncData(LocalDate.parse("2020-12-15"));
        statsCategoryMatrices = controller.statsCategoryMatrix();
        Assertions.assertThat(statsCategoryMatrices)
            .containsExactlyInAnyOrder(
                new StatsCategoryMatrix(MARSHRUT_ID, 1L, 0, 2)
            );

        statsCategoryMatrixRepository.syncData(LocalDate.parse("2020-08-01"));
        statsCategoryMatrices = controller.statsCategoryMatrix();
        Assertions.assertThat(statsCategoryMatrices)
            .containsExactlyInAnyOrder(
                new StatsCategoryMatrix(MARSHRUT_ID, 1L, 0, 3)
            );
    }

    private Msku createMsku(long mskuId, long categoryId) {
        return new Msku()
            .setTitle("Msku #" + mskuId)
            .setId(mskuId)
            .setCategoryId(categoryId)
            .setVendorId(0L)
            .setModifiedTs(Instant.now())
            .setSkuType(SkuTypeEnum.SKU);
    }

    private MskuAvailabilityMatrix matrix(long mskuId, long warehouseId, boolean available) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId)
            .setAvailable(available);
    }
}
