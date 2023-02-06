package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.MskuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.MskuFilter;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.msku.CargoType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_KGT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public class AutoMultiSskuRobotTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final long MULTISSKU_CARGOTYPE_LMS_ID = 303L;
    private static final String SOME_USER = "some-user";
    @Resource
    private NamedParameterJdbcOperations namedParameterJdbcTemplate;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private MskuAvailabilityChangedHandler mskuAvailabilityChangedHandler;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private AutoMultiSskuRobot robot;

    private List<Category> categories = new ArrayList<>();
    private StorageKeyValueServiceMock keyValService = new StorageKeyValueServiceMock();


    @Before
    public void setUp() {
        keyValService.putValue("auto_kgt_and_multissku_robot_run_flag", true);
        robot = new AutoMultiSskuRobot(namedParameterJdbcTemplate, namedParameterJdbcTemplate,
            mskuAvailabilityChangedHandler, keyValService, TransactionHelper.MOCK, new CurrentThreadExecutorService());
        robot.setBatchSize(1);

        deepmindWarehouseRepository.save(
            warehouse(TOMILINO_ID, "Томилино"),
            warehouse(SOFINO_KGT_ID, "Соьино КГТ")
        );
        category(0,
            category(1,
                category(1_1,
                    category(1_1_1).setLeaf(true),
                    category(1_1_2).setLeaf(true)
                ),
                category(1_2,
                    category(1_2_1).setLeaf(true),
                    category(1_2_2).setLeaf(true)
                )
            )
        );

        categories = deepmindCategoryRepository.insertBatch(categories);

        categoryAvailabilityMatrixRepository.save(new CategoryAvailabilityMatrix()
            .setCategoryId(0L)
            .setAvailable(false)
            .setWarehouseId(SOFINO_KGT_ID)
        );

        AtomicLong ids = new AtomicLong();
        List<Msku> mskus = categories.stream()
            .filter(Category::isLeaf)
            .flatMap(c -> Stream.of(mskuIdCategory(ids.incrementAndGet(), c),
                mskuIdCategory(ids.incrementAndGet(), c), mskuIdCategory(ids.incrementAndGet(), c)))
            .collect(Collectors.toList());
        deepmindMskuRepository.save(mskus);
        deepmindSupplierRepository.save(
            supplier(111, SupplierType.THIRD_PARTY),
            supplier(222, SupplierType.THIRD_PARTY),
            supplier(333, SupplierType.THIRD_PARTY)
        );
    }

    @Test
    public void checkInitialState() {
        assertThat(deepmindWarehouseRepository.findAll()).hasSize(2);
        assertThat(categoryAvailabilityMatrixRepository.findAll()).hasSize(1);
        assertThat(deepmindCategoryRepository.findAll()).hasSameSizeAs(categories);
        assertThat(deepmindMskuRepository.findAll()).hasSize(
            categories.stream().filter(Category::isLeaf).collect(Collectors.toSet()).size() * 3);
    }

    @Test
    public void testInsertUnblock() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class))
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(
                mskus.get(0).getId(),
                mskus.get(1).getId()
            );
    }

    @Test
    public void testSkipCategoryWhileInsertUnblock() {
        keyValService.putValue("auto_kgt_and_multissku_robot_skip_list", List.of("111", "112"));
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).isNotPresent();
        Assertions.assertThat(matrix1).isNotPresent();
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class)).isEmpty();
    }

    @Test
    public void testNotInsertUnblockIfManual() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        mskuAvailabilityMatrixRepository.save(new MskuAvailabilityMatrix()
            .setWarehouseId(SOFINO_KGT_ID)
            .setAvailable(false)
            .setCreatedLogin(SOME_USER)
            .setMarketSkuId(mskus.get(1).getId()));
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        //stayed locked
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(false);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class))
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(
                mskus.get(0).getId()
            );
    }

    @Test
    public void testNotInsertUnblockIfCategoryUnblocked() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(CargoType.HEAVY_GOOD.lmsId());
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(2).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        categoryAvailabilityMatrixRepository.save(new CategoryAvailabilityMatrix()
            .setCategoryId(1_1L)
            .setAvailable(true)
            .setWarehouseId(SOFINO_KGT_ID)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).isNotPresent();
        Assertions.assertThat(matrix1).isNotPresent();
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class)).isEmpty();
    }

    @Test
    public void dontDoExcessJob() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class))
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(
                mskus.get(0).getId(),
                mskus.get(1).getId()
            );

        clearQueue();

        // second run
        robot.execute();

        matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class)).isEmpty();
    }

    @Test
    public void testDeletionUnblock() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class))
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(
                mskus.get(0).getId(),
                mskus.get(1).getId()
            );

        clearQueue();

        var newMsku = deepmindMskuRepository.findById(mskus.get(1).getId()).orElseThrow();
        deepmindMskuRepository.save(newMsku.setCargoTypes());
        // second run
        robot.execute();

        matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).isNotPresent();
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class)).hasSize(1);
    }

    @Test
    public void testDeletionUnblockByCategory() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class))
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(
                mskus.get(0).getId(),
                mskus.get(1).getId()
            );

        clearQueue();

        categoryAvailabilityMatrixRepository.save(new CategoryAvailabilityMatrix()
            .setCategoryId(1_1_1L)
            .setAvailable(true)
            .setWarehouseId(SOFINO_KGT_ID)
        );
        // second run
        robot.execute();

        matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).isNotPresent();
        Assertions.assertThat(matrix1).isNotPresent();
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class)).hasSize(2);
    }

    @Test
    public void dontDeleteOthersAvailability() {
        List<Msku> mskus = deepmindMskuRepository.find(new MskuFilter().setCategoryIds(1_1_1));
        assertThat(mskus).hasSize(3);

        mskus.get(0).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        mskus.get(1).setCargoTypes(MULTISSKU_CARGOTYPE_LMS_ID);
        deepmindMskuRepository.save(mskus);
        serviceOfferReplicaRepository.save(
            offer(111, "shop-sku-111", mskus.get(0).getId(), 1_1_1L),
            offer(222, "shop-sku-222", mskus.get(1).getId(), 1_1_1L),
            offer(333, "shop-sku-333", mskus.get(2).getId(), 1_1_1L)
        );
        clearQueue();

        robot.execute();

        var matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        var matrix1 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(1).getId());
        var matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix2).isNotPresent();

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class))
            .flatExtracting(MskuAvailabilityChangedTask::getMskuIds)
            .containsExactlyInAnyOrder(
                mskus.get(0).getId(),
                mskus.get(1).getId()
            );

        mskuAvailabilityMatrixRepository.save(new MskuAvailabilityMatrix()
            .setWarehouseId(TOMILINO_ID)
            .setAvailable(false)
            .setCreatedLogin(SOME_USER)
            .setMarketSkuId(mskus.get(1).getId()));

        clearQueue();

        var newMsku = deepmindMskuRepository.findById(mskus.get(1).getId()).orElseThrow();
        deepmindMskuRepository.save(newMsku.setCargoTypes());
        // second run
        robot.execute();

        matrix0 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(0).getId());
        matrix1 = mskuAvailabilityMatrixRepository.findByKey(mskus.get(1).getId(), SOFINO_KGT_ID);
        matrix2 = mskuAvailabilityMatrixRepository.findByMskuId(mskus.get(2).getId());
        var matrix3 = mskuAvailabilityMatrixRepository.findByKey(mskus.get(1).getId(), TOMILINO_ID);
        Assertions.assertThat(matrix0).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(true);
        Assertions.assertThat(matrix1).isNotPresent();
        Assertions.assertThat(matrix2).isNotPresent();
        //stayed the same
        Assertions.assertThat(matrix3).get().extracting(MskuAvailabilityMatrix::getAvailable).isEqualTo(false);

        Assertions.assertThat(getQueueTasksOfType(MskuAvailabilityChangedTask.class)).hasSize(1);
    }

    private Msku mskuIdCategory(long id, Category c) {
        return new Msku()
            .setId(id)
            .setTitle("msku_of_category_" + c.getCategoryId())
            .setCategoryId(c.getCategoryId())
            .setVendorId(-1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    private Category category(long id, Category... children) {
        Category category = new Category().setCategoryId(id).setParentCategoryId(-1L).setPublished(true);
        category.setName(String.format("Category with id = %d", category.getCategoryId()));
        for (Category child : children) {
            child.setParentCategoryId(id);
        }
        categories.add(category);
        return category;
    }

    private Warehouse warehouse(long id, String name) {
        return new Warehouse()
            .setId(id).setName(name).setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
            .setType(WarehouseType.FULFILLMENT);
    }

    private MskuAvailabilityMatrix matrix(Msku msku, long warehouseId, boolean available, String login) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(msku.getId())
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setCreatedLogin(login)
            .setBlockReasonKey(BlockReasonKey.OTHER);
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, long mskuId, long categoryId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private Supplier supplier(Integer id, SupplierType supplierType) {
        return new Supplier().setId(id).setName(id.toString()).setSupplierType(supplierType);
    }
}
