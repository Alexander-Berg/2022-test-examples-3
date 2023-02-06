package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.db.monitoring.DbMonitoringUnit;
import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.task_queue.events.ShopSkuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.ShopSkuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.INCONSISTENT_SHOP_SKU;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_VALID_INCONSISTENT_ITEMS_LIMIT;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;
import static ru.yandex.market.deepmind.tms.executors.UploadOfferHidingsToLogbrokerCheckExecutor.DEFAULT_BATCH_SIZE;

public class UploadOfferHidingsToLogbrokerCheckExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final Warehouse SORTING_CENTER_231 = new Warehouse()
        .setId(231L).setName("Сортировочный центр 231")
        .setType(WarehouseType.SORTING_CENTER)
        .setUsingType(WarehouseUsingType.USE_FOR_DROPSHIP);
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private PartnerRelationRepository partnerRelationRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private ShopSkuAvailabilityChangedHandler shopSkuAvailabilityChangedHandler;
    @Resource
    private DbMonitoring deepmindDbMonitoring;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private ShopSkuMatrixAvailabilityServiceMock availabilityService;
    private UploadOfferHidingsToLogbrokerCheckExecutor executor;

    private StorageKeyValueServiceMock storageKeyValueService;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        deepmindWarehouseRepository.save(SORTING_CENTER_231);
        var suppliers = YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml");
        deepmindSupplierRepository.save(suppliers);
        partnerRelationRepository.save(
            relation(1, PartnerRelationType.CROSSDOCK, 1001L, CROSSDOCK_SOFINO_ID),
            relation(2, PartnerRelationType.CROSSDOCK, 1002L, CROSSDOCK_ROSTOV_ID),
            relation(3, PartnerRelationType.DROPSHIP, 1003L, SORTING_CENTER_231.getId())
        );
        clearQueue();

        availabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        storageKeyValueService = new StorageKeyValueServiceMock();
        executor = new UploadOfferHidingsToLogbrokerCheckExecutor(
            changedSskuRepository,
            serviceOfferReplicaRepository,
            partnerRelationRepository,
            deepmindWarehouseRepository,
            availabilityService,
            shopSkuAvailabilityChangedHandler,
            deepmindDbMonitoring,
            storageKeyValueService
        );
    }

    @After
    public void tearDown() {
        executor.setBatchSize(DEFAULT_BATCH_SIZE);
        // MBO-27956
        DbMonitoringUnit unit = deepmindDbMonitoring.getOrCreateUnit(INCONSISTENT_SHOP_SKU);
        unit.ok();

        storageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_VALID_INCONSISTENT_ITEMS_LIMIT, null);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void testResynchronizeOffersIfTheyAreInInvalidState() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_VALID_INCONSISTENT_ITEMS_LIMIT, 1);

        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);
        clearQueue();
        Instant now = Instant.now();
        changedSskuRepository.insertBatch(List.of(
            new ChangedSsku().setId(1L).setUseForFulfillment(false).setSupplierId(offer1.getBusinessId())
                .setShopSku(offer1.getShopSku())
                .setVersionTs(now).setHidingUploadedVersionTs(now).setHidingUploadedStatus(HidingStatus.NOT_HIDDEN),
            new ChangedSsku().setId(2L).setUseForFulfillment(false).setSupplierId(offer2.getBusinessId())
                .setShopSku(offer2.getShopSku())
                .setVersionTs(now).setHidingUploadedVersionTs(now).setHidingUploadedStatus(HidingStatus.HIDDEN)
        ));
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils
            .mskuInWarehouse(false, createMsku(1L), TOMILINO, null, null, null);
        availabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(3, "sku-3"), SORTING_CENTER_231.getId(), mskuAvailability);

        executor.execute();

        List<ShopSkuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            new ShopSkuAvailabilityChangedTask(
                Set.of(new ServiceOfferKey(1, "sku-1"), new ServiceOfferKey(2, "sku-2"),
                    new ServiceOfferKey(3, "sku-3")),
                "InconsistentShopSkus", null, null)
        );
        ComplexMonitoring.Result result = deepmindDbMonitoring.fetchTotalResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).contains(
            "Some shop skus are in inconsistent state (supplier_id, shop_sku): (1,'sku-1'),(2,'sku-2')...+1"
        );
    }

    @Test
    public void testNoMonitoringIfLimitIsBig() {
        storageKeyValueService.invalidateCache();
        storageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_VALID_INCONSISTENT_ITEMS_LIMIT, 100);

        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);
        clearQueue();
        Instant now = Instant.now();
        changedSskuRepository.insertBatch(List.of(
            new ChangedSsku().setId(1L).setUseForFulfillment(false).setSupplierId(offer1.getBusinessId())
                .setShopSku(offer1.getShopSku())
                .setVersionTs(now).setHidingUploadedVersionTs(now).setHidingUploadedStatus(HidingStatus.NOT_HIDDEN),
            new ChangedSsku().setId(2L).setUseForFulfillment(false).setSupplierId(offer2.getBusinessId())
                .setShopSku(offer2.getShopSku())
                .setVersionTs(now).setHidingUploadedVersionTs(now).setHidingUploadedStatus(HidingStatus.HIDDEN)
        ));
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(3, "sku-3"), SORTING_CENTER_231.getId(), mskuAvailability);

        executor.execute();

        List<ShopSkuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            new ShopSkuAvailabilityChangedTask(Set.of(
                new ServiceOfferKey(1, "sku-1"),
                new ServiceOfferKey(2, "sku-2"),
                new ServiceOfferKey(3, "sku-3")
            ), "InconsistentShopSkus", null, null)
        );
        ComplexMonitoring.Result result = deepmindDbMonitoring.fetchTotalResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testUploadNotHiddenIfNotToWarehouse() {
        // remove TO_WAREHOUSE
        PartnerRelation partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(1, 1001L);
        partnerRelationRepository.save(partnerRelation.setToWarehouseId(null));

        var offer1 = createOffer(1, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer1);

        clearQueue();
        Instant now = Instant.now();
        changedSskuRepository.insertBatch(List.of(
            new ChangedSsku().setId(1L).setUseForFulfillment(false).setSupplierId(offer1.getBusinessId())
                .setShopSku(offer1.getShopSku())
                .setVersionTs(now).setHidingUploadedVersionTs(now).setHidingUploadedStatus(HidingStatus.NOT_HIDDEN)
        ));
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), CROSSDOCK_SOFINO_ID, mskuAvailability);

        executor.execute();

        List<ShopSkuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();

        ComplexMonitoring.Result result = deepmindDbMonitoring.fetchTotalResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    private PartnerRelation relation(int supplierId, PartnerRelationType type, long fromWhId, @Nullable Long toWhId) {
        return new PartnerRelation()
            .setSupplierId(supplierId)
            .setRelationType(type)
            .setFromWarehouseIds(fromWhId)
            .setToWarehouseId(toWhId);
    }

    private Msku createMsku(long mskuId) {
        return new Msku().setId(mskuId).setTitle("msku " + mskuId).setCategoryId(-1L).setVendorId(-1L);
    }

    private ServiceOfferReplica createOffer(
        int supplierId, String ssku, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
