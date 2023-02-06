package ru.yandex.market.deepmind.common.services.task_queue.handlers;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.task_queue.events.MskuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;


/**
 * Tests of {@link MskuAvailabilityChangedHandler}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MskuAvailabilityChangedHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final long MSKU_404040 = 404040;
    private static final long MSKU_505050 = 505050;

    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        var offers = YamlTestUtil.readOffersFromResources("availability/offers.yml");
        Map<Long, Msku> mskus = new HashMap<>();
        offers.forEach(offer -> {
            mskus.put(offer.getMskuId(), TestUtils.newMsku(offer.getMskuId(), 1));
        });
        serviceOfferReplicaRepository.save(offers);
        deepmindMskuRepository.save(mskus.values());
        clearQueue();
    }

    @Test
    public void testSave() {
        mskuAvailabilityMatrixRepository.save(
            matrix(MSKU_404040, TOMILINO_ID, false),
            matrix(MSKU_505050, SOFINO_ID, false)
        );

        List<MskuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(MSKU_404040, MSKU_505050)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"),
                changedSsku(77, "sku5")
            );
    }

    @Test
    public void testDelete() {
        // previously create
        MskuAvailabilityMatrix matrix = mskuAvailabilityMatrixRepository.save(matrix(MSKU_404040, TOMILINO_ID, false));
        taskQueueRepository.deleteAll();

        // then delete
        mskuAvailabilityMatrixRepository.delete(matrix.getId());

        // Check only unique rows in queue
        List<MskuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(MSKU_404040)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4")
            );
    }

    @Test
    public void testSingleTaskEventIfMskuIsSavedInSeveralWarehouses() {
        mskuAvailabilityMatrixRepository.save(
            matrix(MSKU_404040, TOMILINO_ID, false),
            matrix(MSKU_404040, SOFINO_ID, true)
        );

        // Check only unique rows in queue
        List<MskuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(MSKU_404040)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4")
            );
    }

    @Test
    public void testSingleTaskEventIfMskuIsDeletedInSeveralWarehouses() {
        List<MskuAvailabilityMatrix> matrices = mskuAvailabilityMatrixRepository.save(
            matrix(MSKU_404040, TOMILINO_ID, false),
            matrix(MSKU_404040, SOFINO_ID, false),
            matrix(MSKU_404040, MARSHRUT_ID, false)
        );
        taskQueueRepository.deleteAll();
        mskuAvailabilityMatrixRepository.delete(
            matrices.get(0).getId(),
            matrices.get(1).getId()
        );

        // Check only unique rows in queue
        List<MskuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(MSKU_404040)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4")
            );
    }

    public MskuAvailabilityMatrix matrix(long mskuId, long warehouseId, boolean available) {
        return new MskuAvailabilityMatrix()
            .setMarketSkuId(mskuId).setWarehouseId(warehouseId).setAvailable(available);
    }

    public MskuAvailabilityChangedTask changedTask(long... mskuIds) {
        return new MskuAvailabilityChangedTask(Arrays.stream(mskuIds).boxed().collect(Collectors.toSet()),
            MatrixAvailability.Reason.MSKU.name(), "", Instant.now());
    }
}
