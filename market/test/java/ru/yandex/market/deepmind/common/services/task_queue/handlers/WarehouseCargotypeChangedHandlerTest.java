package ru.yandex.market.deepmind.common.services.task_queue.handlers;

import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.WarehouseCargotypeChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

/**
 * Tests of {@link WarehouseCargotypeChangedHandler}.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class WarehouseCargotypeChangedHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final long MSKU_404040 = 404040;
    private static final long MSKU_505050 = 505050;
    private static final long MSKU_100000 = 100000;

    @Resource
    private WarehouseCargotypeChangedHandler warehouseCargotypeChangedHandler;

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));

        Msku msku404040 = TestUtils.newMsku(MSKU_404040, 1).setCargoTypes(1L, 2L, 3L);
        Msku msku505050 = TestUtils.newMsku(MSKU_505050, 2).setCargoTypes(1L);
        Msku msku100000 = TestUtils.newMsku(MSKU_100000, 3).setCargoTypes(11L);
        Msku msku100001 = TestUtils.newMsku(100001, 1).setCargoTypes(11L);
        Msku msku100002 = TestUtils.newMsku(100002, 2).setCargoTypes(100L);
        Msku msku100003 = TestUtils.newMsku(100003, 3).setCargoTypes(100L);
        deepmindMskuRepository.save(msku404040, msku505050, msku100000, msku100001, msku100002, msku100003);
        clearQueue();
    }

    @Test
    public void testProcess() {
        List<Long> cargoTypes = List.of(1L);
        warehouseCargotypeChangedHandler.registerCargotypeChanges(cargoTypes, getClass().getSimpleName());

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"), // sku4 changed, because it linked with msku #404040
                changedSsku(77, "sku5")  // sku5 changed, because it linked with msku #505050
            );
    }

    @Test
    public void testProcessOfEmptyCargotypes() {
        warehouseCargotypeChangedHandler.registerCargotypeChanges(List.of(), getClass().getSimpleName());

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus).isEmpty();
    }

    @Test
    public void testProcessSeveralCargotypes() {
        List<Long> cargoTypes = List.of(2L, 3L, 11L);
        warehouseCargotypeChangedHandler.registerCargotypeChanges(cargoTypes, getClass().getSimpleName());

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"), // sku4 changed, because it linked with msku #404040
                changedSsku(77, "sku6")  // sku5 changed, because it linked with msku #100000
            );
    }
}
