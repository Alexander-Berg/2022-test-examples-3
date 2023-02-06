package ru.yandex.market.deepmind.common.services.task_queue.handlers;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.SupplierAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.SupplierAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SupplierAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;


/**
 * Tests of {@link SupplierAvailabilityChangedHandler}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SupplierAvailabilityChangedHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    @Resource
    private SupplierAvailabilityMatrixRepository supplierAvailabilityMatrixRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));
        clearQueue();
    }

    @Test
    public void testSave() {
        supplierAvailabilityMatrixRepository.save(
            matrix(77, TOMILINO_ID, false),
            matrix(60, SOFINO_ID, false)
        );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(77),
            changedTask(60)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"),
                changedSsku(77, "sku5"),
                changedSsku(77, "sku6")
            );
    }

    @Test
    public void testDelete() {
        // previously create
        SupplierAvailabilityMatrix matrix = supplierAvailabilityMatrixRepository.save(matrix(77, TOMILINO_ID, false));
        taskQueueRepository.deleteAll();

        // then delete
        supplierAvailabilityMatrixRepository.delete(matrix.getId());

        // Check only unique rows in queue
        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(77)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(77, "sku5"),
                changedSsku(77, "sku6")
            );
    }

    @Test
    public void testDoubleSave() {
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            matrix(77, TOMILINO_ID, false)
        ));
        taskQueueRepository.deleteAll();

        // second save
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            matrix(77, TOMILINO_ID, false)
        ));

        // check no rows in queue, because repository won't resave unchanged data
        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();
    }

    @Test
    public void testDoubleDelete() {
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            matrix(77, TOMILINO_ID, false).setDeleted(true)
        ));
        taskQueueRepository.deleteAll();

        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            matrix(77, TOMILINO_ID, false).setDeleted(true)
        ));

        // check no rows in queue, because repository won't resave unchanged data
        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();
    }

    @Test
    public void testSingleTaskEventIfSupplierIsSavedInSeveralWarehouses() {
        supplierAvailabilityMatrixRepository.save(
            matrix(77, TOMILINO_ID, false),
            matrix(77, SOFINO_ID, true)
        );

        // Check only unique rows in queue
        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(77)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(77, "sku5"),
                changedSsku(77, "sku6")
            );
    }

    @Test
    public void testSingleTaskEventIfSupplierIsDeletedInSeveralWarehouses() {
        supplierAvailabilityMatrixRepository.save(
            matrix(77, TOMILINO_ID, false).setDeleted(true),
            matrix(77, SOFINO_ID, false).setDeleted(true),
            matrix(77, MARSHRUT_ID, false).setDeleted(false)
        );

        // Check only unique rows in queue
        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(77)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(77, "sku5"),
                changedSsku(77, "sku6")
            );
    }

    private SupplierAvailabilityChangedTask changedTask(int supplierId) {
        return new SupplierAvailabilityChangedTask(supplierId, "", Instant.now());
    }

    private SupplierAvailabilityMatrix matrix(int supplierId, long warehouseId, boolean available) {
        return new SupplierAvailabilityMatrix()
            .setSupplierId(supplierId).setWarehouseId(warehouseId).setAvailable(available);
    }
}
