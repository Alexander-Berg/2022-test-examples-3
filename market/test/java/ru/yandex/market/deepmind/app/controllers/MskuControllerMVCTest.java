package ru.yandex.market.deepmind.app.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.services.SskuMskuStatusHelperServiceImpl;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilter;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilterConverter;
import ru.yandex.market.deepmind.common.availability.msku.MskuAvailabilityMatrixChecker;
import ru.yandex.market.deepmind.common.background.BackgroundExportService;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.mocks.GlobalVendorsCachingServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindCargoTypeSnapshotRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.StatsMskuMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.audit.AvailabilityMatrixAuditService;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceImpl;
import ru.yandex.market.deepmind.common.services.category.DeepmindDatabaseCategoryCachingService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.storage.StorageKeyValueRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueRepositoryMock;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;

/**
 * Tests of {@link MskuController}.
 *
 * @deprecated Move all tests to {@link MskuControllerTest}.
 */
@Deprecated
@SuppressWarnings("checkstyle:magicnumber")
public class MskuControllerMVCTest extends DeepmindBaseAppDbTestClass {
    private static final long ROOT_CATEGORY_ID = CategoryTree.ROOT_CATEGORY_ID;
    private static final long CATEGORY_ID_11 = 11L;
    private static final long CATEGORY_ID_12 = 12L;
    private static final long CATEGORY_ID_21_CHILD = 21L;
    private static final long OTHER_CATEGORY_ID = 31L;

    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    private MskuInfoRepository mskuInfoRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private DeepmindCargoTypeSnapshotRepository deepmindCargoTypeSnapshotRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private StatsMskuMatrixRepository statsMskuMatrixRepository;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;

    private MskuController mskuController;
    private EnhancedRandom random;
    private StorageKeyValueRepository storageKeyValueRepository;

    @Before
    public void setUp() {
        random = TestUtils.createMskuRandom();
        storageKeyValueRepository = new StorageKeyValueRepositoryMock();
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        deepmindCategoryRepository.insert(category(ROOT_CATEGORY_ID, "root", CategoryTree.NO_ROOT_ID));
        deepmindCategoryRepository.insert(category(CATEGORY_ID_11, "cat1", ROOT_CATEGORY_ID));
        deepmindCategoryRepository.insert(category(CATEGORY_ID_21_CHILD, "cat1_child1", CATEGORY_ID_11));
        deepmindCategoryRepository.insert(category(CATEGORY_ID_12, "cat2", ROOT_CATEGORY_ID));
        deepmindCategoryRepository.insert(category(OTHER_CATEGORY_ID, "other_cat", ROOT_CATEGORY_ID));
        deepmindCategoryManagerRepository.save(
            categoryManager("pupkin", CATEGORY_ID_11),
            categoryManager("pupkin", CATEGORY_ID_21_CHILD),
            categoryManager("pupkin", CATEGORY_ID_12),
            categoryManager("not_pupkin", OTHER_CATEGORY_ID)
        );
        // util services
        var backgroundServiceMock = new BackgroundServiceMock();
        var backgroundActionStatusService = new BackgroundExportService(backgroundServiceMock,
            transactionTemplate, new ExcelS3ServiceMock());

        // msku status
        var mboAuditServiceMock = new MboAuditServiceMock();
        var mskuStatusAuditService = new MskuStatusAuditService(mboAuditServiceMock);

        // category
        var globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        var deepmindCargoTypeCachingService =
            new DeepmindCargoTypeCachingServiceImpl(deepmindCargoTypeSnapshotRepository);
        var executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("mboc-key-value-cache-thread")
            .build());
        var storageKeyValueService = new StorageKeyValueServiceImpl(storageKeyValueRepository, executorService);
        var deepmindDatabaseCategoryCachingService = new DeepmindDatabaseCategoryCachingService(
                deepmindCategoryRepository,
                storageKeyValueService,
                new CurrentThreadExecutorService(),
                Long.MAX_VALUE
            );
        var mskuAvailabilityMatrixChecker = new MskuAvailabilityMatrixChecker(
            mskuAvailabilityMatrixRepository, categoryAvailabilityMatrixRepository, mskuStatusRepository,
            deepmindCargoTypeCachingService, deepmindDatabaseCategoryCachingService, seasonRepository
        );
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        var sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        var sskuMskuStatusHelperService = new SskuMskuStatusHelperServiceImpl(serviceOfferReplicaRepository,
            backgroundServiceMock, sskuMskuStatusService, sskuMskuStatusValidationService, sskuStatusRepository,
            deepmindMskuRepository, mskuStatusRepository, transactionTemplate);
        var extendedMskuFilterConverter = new ExtendedMskuFilterConverter(
            deepmindSupplierRepository, deepmindCategoryManagerRepository, deepmindCategoryTeamRepository,
            deepmindDatabaseCategoryCachingService
        );
        var deepmindAvailableWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT);

        mskuController = new MskuController(deepmindDatabaseCategoryCachingService, globalVendorsCachingService,
            deepmindMskuRepository, mskuInfoRepository, mskuStatusRepository,
            mskuAvailabilityMatrixRepository,
            seasonRepository, deepmindAvailableWarehouseService, mskuAvailabilityMatrixChecker,
            extendedMskuFilterConverter, backgroundServiceMock,
            sskuMskuStatusHelperService,
            mskuStatusAuditService, Mockito.mock(AvailabilityMatrixAuditService.class), serviceOfferReplicaRepository,
            sskuAvailabilityMatrixRepository, new TransactionTemplateMock(), deepmindCargoTypeCachingService,
            backgroundActionStatusService, statsMskuMatrixRepository);
    }

    private CategoryManager categoryManager(String login, long categoryId) {
        return new CategoryManager().setStaffLogin(login)
            .setCategoryId(categoryId).setRole(CATMAN)
            .setFirstName("").setLastName("");
    }

    @Test
    public void searchByCategoryManager() {
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_12));
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(OTHER_CATEGORY_ID));

        var page = mskuController.list(new ExtendedMskuFilter()
            .setCategoryManagerLogin("pupkin"), OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(displayMskuInfo -> displayMskuInfo.getMsku().getId())
            .containsExactlyInAnyOrder(msku1.getId(), msku2.getId());
    }

    @Test
    public void searchByHierarchy() {
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(ROOT_CATEGORY_ID));
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_21_CHILD));
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_12));
        Msku msku3 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(OTHER_CATEGORY_ID));

        var page = mskuController.list(new ExtendedMskuFilter()
                .setHierarchyCategoryIds(List.of(CATEGORY_ID_11, OTHER_CATEGORY_ID)),
            OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(displayMskuInfo -> displayMskuInfo.getMsku().getId())
            .containsExactlyInAnyOrder(msku1.getId(), msku2.getId(), msku3.getId());
    }

    @Test
    public void searchCategoryId() {
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(ROOT_CATEGORY_ID));
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_21_CHILD));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(OTHER_CATEGORY_ID));

        var page = mskuController.list(new ExtendedMskuFilter()
                .setCategoryIds(List.of(CATEGORY_ID_11, OTHER_CATEGORY_ID)),
            OffsetFilter.all());

        Assertions.assertThat(page.getItems())
            .extracting(displayMskuInfo -> displayMskuInfo.getMsku().getId())
            .containsExactlyInAnyOrder(msku1.getId(), msku2.getId());
    }

    @Test
    public void searchShouldIntersectCategoriesFromDifferentSources() {
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(ROOT_CATEGORY_ID));
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_12));
        Msku msku1child = TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_21_CHILD);
        deepmindMskuRepository.save(msku1child);
        deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(OTHER_CATEGORY_ID));

        var page = mskuController.list(new ExtendedMskuFilter()
                .setCategoryIds(List.of(OTHER_CATEGORY_ID))
                .setHierarchyCategoryIds(List.of(CATEGORY_ID_11))
                .setCategoryManagerLogin("pupkin"),
            OffsetFilter.all());
        Assertions.assertThat(page.getItems()).isEmpty();

        page = mskuController.list(new ExtendedMskuFilter()
                .setHierarchyCategoryIds(List.of(CATEGORY_ID_11))
                .setCategoryManagerLogin("pupkin"),
            OffsetFilter.all());
        Assertions.assertThat(page.getItems())
            .extracting(displayMskuInfo -> displayMskuInfo.getMsku().getId())
            .containsExactlyInAnyOrder(msku1.getId(), msku1child.getId());
    }

    @Test
    public void testDeleteUpdateInsert() {
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        MskuAvailabilityMatrix availabilityMatrixToDelete = mskuAvailabilityMatrixRepository.save(
            matrix(msku2, 1L, true));
        MskuAvailabilityMatrix availabilityMatrixToUpdate = mskuAvailabilityMatrixRepository.save(
            matrix(msku1, 1L, false));
        MskuAvailabilityMatrix availabilityMatrixToInsert = matrix(msku1, 2L, true);

        availabilityMatrixToDelete = availabilityMatrixToDelete.setAvailable(null);
        availabilityMatrixToUpdate = availabilityMatrixToUpdate.setAvailable(true);

        mskuController.save(List.of(availabilityMatrixToDelete,
            availabilityMatrixToUpdate,
            availabilityMatrixToInsert));

        assertThat(mskuAvailabilityMatrixRepository
            .find(new MskuAvailabilityMatrixRepository.Filter()))
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available")
            .containsExactlyInAnyOrder(availabilityMatrixToInsert, availabilityMatrixToUpdate);
    }

    @Test
    public void shouldEraseAllOtherAvailabilitiesForMskuWarehousePair() {
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        Msku msku2 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));

        MskuAvailabilityMatrix availabilityMatrixToDelete = mskuAvailabilityMatrixRepository.save(
            matrix(msku1, 1L, true));
        MskuAvailabilityMatrix availabilityMatrixErased1 = mskuAvailabilityMatrixRepository.save(
            matrix(msku1, 1L, false)
                .setFromDate(LocalDate.now())
                .setToDate(LocalDate.now().plusDays(2)));

        MskuAvailabilityMatrix availabilityMatrixToUpdate = mskuAvailabilityMatrixRepository.save(
            matrix(msku1, 2L, false));
        MskuAvailabilityMatrix availabilityMatrixErased2 = mskuAvailabilityMatrixRepository.save(
            matrix(msku1, 2L, false)
                .setFromDate(LocalDate.now())
                .setToDate(LocalDate.now().plusDays(2)));
        MskuAvailabilityMatrix availabilityMatrixToInsert = matrix(msku2, 1L, true);
        MskuAvailabilityMatrix availabilityMatrixErased3 = mskuAvailabilityMatrixRepository.save(
            matrix(msku2, 1L, false)
                .setFromDate(LocalDate.now())
                .setToDate(LocalDate.now().plusDays(2)));

        MskuAvailabilityMatrix availabilityMatrixNotErased = mskuAvailabilityMatrixRepository.save(
            matrix(msku2, 2L, true)
                .setFromDate(LocalDate.now())
                .setToDate(LocalDate.now().plusDays(2)));

        availabilityMatrixToDelete = availabilityMatrixToDelete.setAvailable(null);
        availabilityMatrixToUpdate = availabilityMatrixToUpdate.setAvailable(true);

        mskuController.save(List.of(
            availabilityMatrixToUpdate,
            availabilityMatrixToInsert,
            availabilityMatrixToDelete
        ));

        List<MskuAvailabilityMatrix> found = mskuAvailabilityMatrixRepository
            .find(new MskuAvailabilityMatrixRepository.Filter());
        assertThat(found)
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available")
            .containsExactlyInAnyOrder(availabilityMatrixToInsert, availabilityMatrixToUpdate,
                availabilityMatrixNotErased);
        assertThat(found)
            .extracting(MskuAvailabilityMatrix::getId)
            .doesNotContain(availabilityMatrixErased1.getId(),
                availabilityMatrixErased2.getId(),
                availabilityMatrixErased3.getId());
    }

    @Test
    public void updateAndInserShouldDropDatesFromRequest() {
        Msku msku1 = deepmindMskuRepository.save(TestUtils.randomMsku(random).setCategoryId(CATEGORY_ID_11));
        MskuAvailabilityMatrix availabilityMatrixToUpdate = mskuAvailabilityMatrixRepository.save(
            matrix(msku1, 1L, false));
        MskuAvailabilityMatrix availabilityMatrixToInsert = matrix(msku1, 2L, true);

        availabilityMatrixToUpdate = availabilityMatrixToUpdate.setAvailable(true);

        mskuController.save(List.of(
            availabilityMatrixToUpdate,
            availabilityMatrixToInsert
        ));

        List<MskuAvailabilityMatrix> found = mskuAvailabilityMatrixRepository
            .find(new MskuAvailabilityMatrixRepository.Filter());
        availabilityMatrixToInsert.setFromDate(null).setToDate(null);
        availabilityMatrixToUpdate.setFromDate(null).setToDate(null);
        assertThat(found)
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "fromDate", "toDate")
            .containsExactlyInAnyOrder(availabilityMatrixToInsert, availabilityMatrixToUpdate);
    }

    private static Category category(long id, String name, long parentId) {
        return new Category()
            .setCategoryId(id)
            .setName(name)
            .setParentCategoryId(parentId)
            .setPublished(true);
    }

    private static MskuAvailabilityMatrix matrix(Msku msku, long warehouseId, boolean available) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(msku.getId())
            .setWarehouseId(warehouseId)
            .setAvailable(available);
    }
}
