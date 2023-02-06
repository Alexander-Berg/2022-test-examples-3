package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.ShopSkuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.ShopSkuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;


public class SskuPeriodChangedExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {

    @Resource
    private StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    private ShopSkuAvailabilityChangedHandler shopSkuAvailabilityChangedHandler;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;

    private SskuPeriodChangedExecutor executor;

    @Before
    public void setUp() {
        executor = new SskuPeriodChangedExecutor(
            deepmindStorageKeyValueService,
            sskuAvailabilityMatrixRepository,
            shopSkuAvailabilityChangedHandler
        );
    }

    @Test
    public void testPeriodStart() {
        serviceOfferReplicaRepository.save(
            testOffer(123, "ssku-1")
        );
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(123)
                .setShopSku("ssku-1")
                .setDateFrom(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(new ServiceOfferKey(123, "ssku-1")));
        assertThat(runAtDay("2020-01-16")).isEmpty();
    }

    @Test
    public void testPeriodEnd() {
        serviceOfferReplicaRepository.save(
            testOffer(123, "ssku-1")
        );
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(123)
                .setShopSku("ssku-1")
                .setDateTo(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-15")).isEmpty();
        assertThat(runAtDay("2020-01-16")).containsExactly(changedTask(new ServiceOfferKey(123, "ssku-1")));
        assertThat(runAtDay("2020-01-17")).isEmpty();
    }

    @Test
    public void testPeriodOneDay() {
        serviceOfferReplicaRepository.save(
            testOffer(123, "ssku-1")
        );
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(123)
                .setShopSku("ssku-1")
                .setDateFrom(LocalDate.parse("2020-01-15"))
                .setDateTo(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(new ServiceOfferKey(123, "ssku-1")));
        assertThat(runAtDay("2020-01-16")).containsExactly(changedTask(new ServiceOfferKey(123, "ssku-1")));
        assertThat(runAtDay("2020-01-17")).isEmpty();
    }

    @Test
    public void testPeriodManyDays() {
        serviceOfferReplicaRepository.save(
            testOffer(123, "ssku-1")
        );
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(123)
                .setShopSku("ssku-1")
                .setDateFrom(LocalDate.parse("2020-01-15"))
                .setDateTo(LocalDate.parse("2020-01-20"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(new ServiceOfferKey(123, "ssku-1")));
        assertThat(runAtDay("2020-01-16")).isEmpty();
        assertThat(runAtDay("2020-01-17")).isEmpty();
        assertThat(runAtDay("2020-01-18")).isEmpty();
        assertThat(runAtDay("2020-01-19")).isEmpty();
        assertThat(runAtDay("2020-01-20")).isEmpty();
        assertThat(runAtDay("2020-01-21")).containsExactly(changedTask(new ServiceOfferKey(123, "ssku-1")));
        assertThat(runAtDay("2020-01-22")).isEmpty();
    }

    @Test
    public void testPeriodManyMsku() {
        serviceOfferReplicaRepository.save(
            testOffer(111, "ssku-1"),
            testOffer(222, "ssku-2"),
            testOffer(333, "ssku-3")
        );
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(111)
                .setShopSku("ssku-1")
                .setDateFrom(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID),
            new SskuAvailabilityMatrix()
                .setSupplierId(222)
                .setShopSku("ssku-2")
                .setDateTo(LocalDate.parse("2020-01-16"))
                .setWarehouseId(ROSTOV_ID),
            new SskuAvailabilityMatrix()
                .setSupplierId(333)
                .setShopSku("ssku-3")
                .setDateFrom(LocalDate.parse("2020-01-17"))
                .setDateTo(LocalDate.parse("2020-01-18"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(new ServiceOfferKey(111, "ssku-1")));
        assertThat(runAtDay("2020-01-16")).isEmpty();
        assertThat(runAtDay("2020-01-17")).containsExactlyInAnyOrder(changedTask(
            new ServiceOfferKey(222, "ssku-2"), new ServiceOfferKey(333, "ssku-3"))
        );
        assertThat(runAtDay("2020-01-18")).isEmpty();
        assertThat(runAtDay("2020-01-19")).containsExactly(changedTask(new ServiceOfferKey(333, "ssku-3")));
        assertThat(runAtDay("2020-01-20")).isEmpty();
    }

    @Test
    public void testOneMskuManyPeriods() {
        serviceOfferReplicaRepository.save(testOffer(111, "ssku-1"));
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(111)
                .setShopSku("ssku-1")
                .setDateFrom(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID),
            new SskuAvailabilityMatrix()
                .setSupplierId(111)
                .setShopSku("ssku-1")
                .setDateFrom(LocalDate.parse("2020-01-17"))
                .setDateTo(LocalDate.parse("2020-01-20"))
                .setWarehouseId(TOMILINO_ID),
            new SskuAvailabilityMatrix()
                .setSupplierId(111)
                .setShopSku("ssku-1")
                .setDateTo(LocalDate.parse("2020-01-20"))
                .setWarehouseId(SOFINO_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(new ServiceOfferKey(111, "ssku-1")));
        assertThat(runAtDay("2020-01-16")).isEmpty();
        assertThat(runAtDay("2020-01-17")).containsExactly(changedTask(new ServiceOfferKey(111, "ssku-1")));
        assertThat(runAtDay("2020-01-18")).isEmpty();
        assertThat(runAtDay("2020-01-19")).isEmpty();
        assertThat(runAtDay("2020-01-20")).isEmpty();
        assertThat(runAtDay("2020-01-21")).containsExactly(changedTask(new ServiceOfferKey(111, "ssku-1")));
        assertThat(runAtDay("2020-01-22")).isEmpty();
    }

    private List<ShopSkuAvailabilityChangedTask> runAtDay(String day) {
        try {
            Instant now = LocalDate.parse(day).atStartOfDay(ZoneOffset.UTC).toInstant();
            Clock clock = Clock.fixed(now, ZoneOffset.UTC);

            executor.setClock(clock);
            executor.execute();
            return getQueueTasks();
        } finally {
            executor.setClock(Clock.systemDefaultZone());
            taskQueueRepository.deleteAll();
        }
    }

    private ShopSkuAvailabilityChangedTask changedTask(ServiceOfferKey... keys) {
        return new ShopSkuAvailabilityChangedTask(Arrays.stream(keys).collect(Collectors.toSet()),
            "", "", Instant.now());
    }

    protected ServiceOfferReplica testOffer(int supplierId, String shopSku) {
        if (deepmindSupplierRepository.findByIds(List.of(supplierId)).isEmpty()) {
            var supplier = new Supplier().setId(supplierId).setName("test_supplier_" + supplierId)
                .setSupplierType(SupplierType.THIRD_PARTY);
            deepmindSupplierRepository.save(supplier);
        }
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(1111L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
