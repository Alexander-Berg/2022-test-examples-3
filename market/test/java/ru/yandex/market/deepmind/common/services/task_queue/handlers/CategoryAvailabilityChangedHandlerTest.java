package ru.yandex.market.deepmind.common.services.task_queue.handlers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.CategoryAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.CategoryAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository.UpdateVersionTsStats;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

import static ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability.Reason;
import static ru.yandex.market.deepmind.common.category.CategoryTree.ROOT_CATEGORY_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;


/**
 * Tests of {@link CategoryAvailabilityChangedHandler}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryAvailabilityChangedHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final long CATEGORY_ID_12 = 12L;
    private static final long CATEGORY_ID_22 = 22L;
    private static final long CATEGORY_ID_33 = 33L;
    private static final long CATEGORY_ID_44 = 44L;

    @Resource
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private List<Category> categories = new ArrayList<>();

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));

        category(ROOT_CATEGORY_ID,
            category(CATEGORY_ID_12),
            category(CATEGORY_ID_22,
                category(CATEGORY_ID_44,
                    category(CATEGORY_ID_33)
                )
            )
        );
        deepmindCategoryRepository.insertBatch(categories);

        clearQueue();
    }

    @Test
    public void testSave() {
        categoryAvailabilityMatrixRepository.save(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false),
            matrix(CATEGORY_ID_33, SOFINO_ID, false)
        );

        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(CATEGORY_ID_12),
            changedTask(CATEGORY_ID_33)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"),
                changedSsku(101, "sku100"),
                changedSsku(201, "sku200"),
                changedSsku(102, "sku100")
            );
    }

    @Test
    public void testDelete() {
        // previously create
        CategoryAvailabilityMatrix matrix = categoryAvailabilityMatrixRepository.save(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false)
        );
        taskQueueRepository.deleteAll();

        // then delete
        categoryAvailabilityMatrixRepository.delete(matrix.getId());

        // Check only unique rows in queue
        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(CATEGORY_ID_12)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(101, "sku100"),
                changedSsku(201, "sku200"),
                changedSsku(102, "sku100")
            );
    }

    @Test
    public void testDoubleSave() {
        categoryAvailabilityMatrixRepository.save(List.of(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false)
        ));
        taskQueueRepository.deleteAll();

        // second save
        categoryAvailabilityMatrixRepository.save(List.of(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false)
        ));

        // check no rows in queue, because repository won't resave unchanged data
        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();
    }

    @Test
    public void testDoubleDelete() {
        categoryAvailabilityMatrixRepository.save(matrix(CATEGORY_ID_12, TOMILINO_ID, false));
        categoryAvailabilityMatrixRepository.deleteByEntries(matrix(CATEGORY_ID_12, TOMILINO_ID, false));
        taskQueueRepository.deleteAll();

        categoryAvailabilityMatrixRepository.deleteByEntries(matrix(CATEGORY_ID_12, TOMILINO_ID, false));

        // check no rows in queue, because repository won't resave unchanged data
        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();
    }

    @Test
    public void testSaveInParentCategoryWillAlsoChangeSskuInLeafCategories() {
        categoryAvailabilityMatrixRepository.save(
            matrix(CATEGORY_ID_22, TOMILINO_ID, false)
        );

        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(CATEGORY_ID_22),
            changedTask(CATEGORY_ID_33),
            changedTask(CATEGORY_ID_44)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                // category 22
                changedSsku(77, "sku5"),
                changedSsku(77, "sku6"),
                // category 33
                changedSsku(60, "sku4")
            );
    }

    /**
     * Тест проверяет, что если одновременно сохраняются блокировки и у родительской и дочерней категории,
     * то в очередь добавится плоский список всех под-под-категорий.
     */
    @Test
    public void testSingleTaskIfBothParentAndLeafCategoryAreSaved() {
        categoryAvailabilityMatrixRepository.save(
            matrix(CATEGORY_ID_22, TOMILINO_ID, false),
            matrix(CATEGORY_ID_22, SOFINO_ID, false),
            matrix(CATEGORY_ID_33, MARSHRUT_ID, false)
        );

        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(CATEGORY_ID_22),
            changedTask(CATEGORY_ID_33),
            changedTask(CATEGORY_ID_44)
        );
    }

    @Test
    public void testSkipRootCategoryChange() {
        categoryAvailabilityMatrixRepository.save(matrix(ROOT_CATEGORY_ID, TOMILINO_ID, false));

        var queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();
    }

    @Test
    public void testSkipRootCategoryChangeInChangesSskuRepository() {
        serviceOfferReplicaRepository.save(
            new ServiceOfferReplica()
                .setBusinessId(1)
                .setSupplierId(1)
                .setShopSku("root-category-ssku")
                .setTitle("title")
                .setMskuId(1L)
                .setSeqId(0L)
                .setSupplierType(SupplierType.THIRD_PARTY)
                .setCategoryId(ROOT_CATEGORY_ID)
                .setAcceptanceStatus(OfferAcceptanceStatus.OK)
                .setModifiedTs(Instant.now())
        );

        UpdateVersionTsStats stats = UpdateVersionTsStats.builder()
            .availabilityTs(Instant.now())
            .queueTs(Instant.now())
            .reason(Reason.MSKU_IN_CATEGORY)
            .build();
        changedSskuRepository.updateVersionTsByCategory(ROOT_CATEGORY_ID, stats);

        // no changes, because update is skipped
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus).isEmpty();
    }

    @Test
    public void testSingleTaskEventIfCategoryIsSavedInSeveralWarehouses() {
        categoryAvailabilityMatrixRepository.save(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false),
            matrix(CATEGORY_ID_12, SOFINO_ID, true)
        );

        // Check only unique rows in queue
        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(CATEGORY_ID_12)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(101, "sku100"),
                changedSsku(102, "sku100"),
                changedSsku(201, "sku200")
            );
    }

    @Test
    public void testSingleTaskEventIfCategoryIsDeletedInSeveralWarehouses() {
        categoryAvailabilityMatrixRepository.save(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false),
            matrix(CATEGORY_ID_12, SOFINO_ID, false),
            matrix(CATEGORY_ID_12, MARSHRUT_ID, false)
        );
        taskQueueRepository.deleteAll();
        categoryAvailabilityMatrixRepository.deleteByEntries(
            matrix(CATEGORY_ID_12, TOMILINO_ID, false),
            matrix(CATEGORY_ID_12, SOFINO_ID, false)
        );

        // Check only unique rows in queue
        List<CategoryAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(CATEGORY_ID_12)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(101, "sku100"),
                changedSsku(201, "sku200"),
                changedSsku(102, "sku100")
            );
    }

    public CategoryAvailabilityMatrix matrix(long categoryId, long warehouseId, boolean available) {
        return new CategoryAvailabilityMatrix()
            .setCategoryId(categoryId).setWarehouseId(warehouseId).setAvailable(available);
    }

    private Category category(long id, Category... children) {
        Category category = new Category()
            .setName(String.valueOf(id))
            .setCategoryId(id)
            .setParentCategoryId(-1L)
            .setPublished(true);
        for (Category child : children) {
            child.setParentCategoryId(id);
        }
        categories.add(category);
        return category;
    }

    public CategoryAvailabilityChangedTask changedTask(long categoryId) {
        return new CategoryAvailabilityChangedTask(categoryId, "", Instant.now());
    }
}
