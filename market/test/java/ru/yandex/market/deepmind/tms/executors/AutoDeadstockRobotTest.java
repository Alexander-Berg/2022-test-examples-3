package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.ShopSkuWKey;
import ru.yandex.market.deepmind.common.availability.ssku.SskuAvailabilityFilter;
import ru.yandex.market.deepmind.common.availability.task_queue.events.ShopSkuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AssortSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.DeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AssortSskuRepository;
import ru.yandex.market.deepmind.common.repository.DeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.AUTO_DEADSTOCK_ROBOT;
import static ru.yandex.market.deepmind.common.services.lifecycle.LifecycleGroupAndRunReducer.PACKING_MATERIALS_CATEGORY_ID;
import static ru.yandex.market.deepmind.tms.executors.AutoDeadstockRobot.BLOCK_DEADSTOCK_COMMENT;
import static ru.yandex.market.deepmind.tms.executors.AutoDeadstockRobot.UNBLOCK_DEADSTOCK_COMMENT;

public class AutoDeadstockRobotTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {

    private AutoDeadstockRobot autoDeadstockRobot;

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    private DeadstockStatusRepository deadstockStatusRepository;
    @Resource
    private AssortSskuRepository assortSskuRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    @Before
    public void setUp() {
        autoDeadstockRobot = new AutoDeadstockRobot(jdbcTemplate, sskuAvailabilityMatrixRepository);
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            newOffer(77, "sku1"),
            newOffer(77, "sku2"),
            newOffer(84, "sku2"),
            newOffer(84, "sku3"),
            newOffer(1002, "sku3"),
            newOffer(465852, "sku4")
        );
// ассортиментные товары
        deepmindSupplierRepository.save(
            new Supplier().setId(111111).setName("name1"),
            new Supplier().setId(222222).setName("name2"),
            new Supplier().setId(333333).setName("name3")
        );
        // по идее АТ имееют 1 категорию на все подSSKU
        serviceOfferReplicaRepository.save(
            newOffer(111111, "assort-ssku-1", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-1-1", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-1-2", PACKING_MATERIALS_CATEGORY_ID + 8L),


            newOffer(222222, "assort-ssku-2", PACKING_MATERIALS_CATEGORY_ID + 1L),
            newOffer(222222, "sub-ssku-2-1", PACKING_MATERIALS_CATEGORY_ID + 1L),
            newOffer(222222, "sub-ssku-2-2", PACKING_MATERIALS_CATEGORY_ID + 1L),
            newOffer(222222, "sub-ssku-2-3", PACKING_MATERIALS_CATEGORY_ID + 1L),

            newOffer(333333, "assort-ssku-3", PACKING_MATERIALS_CATEGORY_ID + 5L),
            newOffer(333333, "sub-ssku-3-1", PACKING_MATERIALS_CATEGORY_ID + 5L),
            newOffer(333333, "sub-ssku-3-2", PACKING_MATERIALS_CATEGORY_ID + 5L),
            newOffer(333333, "sub-ssku-3-3", PACKING_MATERIALS_CATEGORY_ID + 5L)
        );

        Instant now = LocalDateTime.now().minusDays(50).toInstant(ZoneOffset.UTC);

        assortSskuRepository.save(
            new AssortSsku(111111, "sub-ssku-1-1", "assort-ssku-1", now),
            new AssortSsku(111111, "sub-ssku-1-2", "assort-ssku-1", now),

            new AssortSsku(222222, "sub-ssku-2-1", "assort-ssku-2", now),
            new AssortSsku(222222, "sub-ssku-2-2", "assort-ssku-2", now),
            new AssortSsku(222222, "sub-ssku-2-3", "assort-ssku-2", now),

            new AssortSsku(333333, "sub-ssku-3-1", "assort-ssku-3", now),
            new AssortSsku(333333, "sub-ssku-3-2", "assort-ssku-3", now),
            new AssortSsku(333333, "sub-ssku-3-3", "assort-ssku-3", now)
        );

        clearQueue();
    }

    @Test
    public void testSingleStatusDetect() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 2);
        deadstockStatusRepository.save(status1);

        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                new SskuAvailabilityMatrix()
                    .setAvailable(false)
                    .setSupplierId(77)
                    .setWarehouseId(ROSTOV_ID)
                    .setShopSku("sku1")
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
                    .setComment(BLOCK_DEADSTOCK_COMMENT)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK)
            );
        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        assertThat(tasks)
            .usingElementComparatorOnFields("shopSkuKeys")
            .containsExactlyInAnyOrder(
                new ShopSkuAvailabilityChangedTask().setShopSkuKeys(Set.of(new ServiceOfferKey(77, "sku1")))
            );
    }

    @Test
    public void testSeveralStatusesDetect() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        DeadstockStatus status2 = deadstockStatus(84, "sku2", SOFINO_ID, 2);
        DeadstockStatus status3 = deadstockStatus(1002, "sku3", SOFINO_ID, 3);
        DeadstockStatus status4 = deadstockStatus(465852, "sku4", SOFINO_ID, 4);
        deadstockStatusRepository.save(status1, status2, status3, status4);

        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                new SskuAvailabilityMatrix()
                    .setAvailable(false)
                    .setSupplierId(77)
                    .setWarehouseId(ROSTOV_ID)
                    .setShopSku("sku1")
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
                    .setComment(BLOCK_DEADSTOCK_COMMENT)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK),
                new SskuAvailabilityMatrix()
                    .setAvailable(false)
                    .setSupplierId(84)
                    .setWarehouseId(SOFINO_ID)
                    .setShopSku("sku2")
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
                    .setComment(BLOCK_DEADSTOCK_COMMENT)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK),
                new SskuAvailabilityMatrix()
                    .setAvailable(false)
                    .setSupplierId(1002)
                    .setWarehouseId(SOFINO_ID)
                    .setShopSku("sku3")
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
                    .setComment(BLOCK_DEADSTOCK_COMMENT)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK),
                new SskuAvailabilityMatrix()
                    .setAvailable(false)
                    .setSupplierId(465852)
                    .setWarehouseId(SOFINO_ID)
                    .setShopSku("sku4")
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
                    .setComment(BLOCK_DEADSTOCK_COMMENT)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK)
            );

        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        assertThat(tasks)
            .usingElementComparatorOnFields("shopSkuKeys")
            .containsExactlyInAnyOrder(
                new ShopSkuAvailabilityChangedTask().setShopSkuKeys(Set.of(
                    new ServiceOfferKey(77, "sku1"),
                    new ServiceOfferKey(84, "sku2"),
                    new ServiceOfferKey(1002, "sku3"),
                    new ServiceOfferKey(465852, "sku4")
                ))
            );
    }

    @Test
    public void testDeleteAvailability() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        DeadstockStatus status2 = deadstockStatus(84, "sku2", SOFINO_ID, 2);
        status1 = deadstockStatusRepository.save(status1);
        status2 = deadstockStatusRepository.save(status2);

        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false),
                sskuMatrix(84, SOFINO_ID, "sku2", false)
            );

        deadstockStatusRepository.delete(status1.getId());
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(84, SOFINO_ID, "sku2", false),
                sskuMatrix(77, ROSTOV_ID, "sku1", null)
            );
    }

    @Test
    public void testDeleteAndOnceMoreCreateAvailability() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        DeadstockStatus status2 = deadstockStatus(84, "sku2", SOFINO_ID, 2);
        status1 = deadstockStatusRepository.save(status1);
        status2 = deadstockStatusRepository.save(status2);

        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false),
                sskuMatrix(84, SOFINO_ID, "sku2", false)
            );

        deadstockStatusRepository.delete(status1.getId());
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(84, SOFINO_ID, "sku2", false),
                sskuMatrix(77, ROSTOV_ID, "sku1", null)
            );

        status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 0);
        deadstockStatusRepository.save(status1);

        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false),
                sskuMatrix(84, SOFINO_ID, "sku2", false)
            );
    }

    @Test
    public void testChangeAvailabilityCreatedByUserBeforeDeadstock() {
        SecurityUtil.wrapWithLogin("user", () -> {
            sskuAvailabilityMatrixRepository.save(
                sskuMatrix(77, ROSTOV_ID, "sku1", true),
                sskuMatrix(77, SOFINO_ID, "sku2", null),
                sskuMatrix(84, TOMILINO_ID, "sku3", false)
            );
        });

        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 0);
        DeadstockStatus status2 = deadstockStatus(77, "sku2", SOFINO_ID, 0);
        DeadstockStatus status3 = deadstockStatus(84, "sku3", TOMILINO_ID, 0);
        DeadstockStatus status4 = deadstockStatus(84, "sku2", TOMILINO_ID, 0);
        deadstockStatusRepository.save(status1, status2, status3, status4);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available",
                "createdLogin", "modifiedLogin")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
                    .setCreatedLogin("user").setModifiedLogin(AUTO_DEADSTOCK_ROBOT),
                sskuMatrix(77, SOFINO_ID, "sku2", false)
                    .setCreatedLogin("user").setModifiedLogin(AUTO_DEADSTOCK_ROBOT),
                // Эту блокировку не трогаем, так как она и так available = false
                sskuMatrix(84, TOMILINO_ID, "sku3", false)
                    .setCreatedLogin("user"),
                // новая блокировка
                sskuMatrix(84, TOMILINO_ID, "sku2", false)
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
            );
    }

    @Test
    public void testChangeAvailabilityChangedByUserAfterDeadstock() {
        Instant threeDaysAgo = LocalDateTime.now().minusDays(3).toInstant(ZoneOffset.UTC);
        Instant today = LocalDateTime.now().toInstant(ZoneOffset.UTC);

        sskuAvailabilityMatrixRepository.save(
            sskuMatrix(77, ROSTOV_ID, "sku1", true)
                .setCreatedLogin(AUTO_DEADSTOCK_ROBOT).setCreatedAt(threeDaysAgo)
                .setModifiedLogin("manager1").setModifiedAt(today),
            sskuMatrix(77, SOFINO_ID, "sku2", null)
                .setCreatedLogin(AUTO_DEADSTOCK_ROBOT).setCreatedAt(threeDaysAgo)
                .setModifiedLogin("manager2").setModifiedAt(today),
            sskuMatrix(84, TOMILINO_ID, "sku3", false)
                .setCreatedLogin(AUTO_DEADSTOCK_ROBOT).setCreatedAt(threeDaysAgo)
                .setModifiedLogin("manager3").setModifiedAt(today)
        );

        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 2);
        DeadstockStatus status2 = deadstockStatus(77, "sku2", SOFINO_ID, 2);
        DeadstockStatus status3 = deadstockStatus(84, "sku3", TOMILINO_ID, 2);
        DeadstockStatus status4 = deadstockStatus(84, "sku2", TOMILINO_ID, 2);
        deadstockStatusRepository.save(status1, status2, status3, status4);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available",
                "createdLogin", "modifiedLogin")
            .containsExactlyInAnyOrder(
                // Эту блокировку не трогаем, так как она выставлена после появления признака deadstock
                sskuMatrix(77, ROSTOV_ID, "sku1", true)
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT).setCreatedAt(threeDaysAgo)
                    .setModifiedLogin("manager1").setModifiedAt(today),
                // Эту блокировку не трогаем, так как она выставлена после появления признака deadstock
                sskuMatrix(77, SOFINO_ID, "sku2", null)
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT).setCreatedAt(threeDaysAgo)
                    .setModifiedLogin("manager2").setModifiedAt(today),
                // Эту блокировку не трогаем, так как она выставлена после появления признака deadstock
                sskuMatrix(84, TOMILINO_ID, "sku3", false)
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT).setCreatedAt(threeDaysAgo)
                    .setModifiedLogin("manager3").setModifiedAt(today),
                // новая блокировка
                sskuMatrix(84, TOMILINO_ID, "sku2", false)
                    .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
            );
    }

    @Test
    public void testDontChangeAvailabilityChangedByUserAfterDeadstock() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        DeadstockStatus status2 = deadstockStatus(77, "sku2", SOFINO_ID, 2);
        DeadstockStatus status3 = deadstockStatus(84, "sku3", TOMILINO_ID, 2);
        status1 = deadstockStatusRepository.save(status1);
        status2 = deadstockStatusRepository.save(status2);
        status3 = deadstockStatusRepository.save(status3);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false),
                sskuMatrix(77, SOFINO_ID, "sku2", false),
                sskuMatrix(84, TOMILINO_ID, "sku3", false)
            );

        // снимаем блокировку как это бы сделал пользователь
        SskuAvailabilityMatrix matrix = sskuAvailabilityMatrixRepository.find(
                new SskuAvailabilityFilter().setShopSkuWKeys(Set.of(new ShopSkuWKey(status2))))
            .get(0);
        matrix.setAvailable(null);
        sskuAvailabilityMatrixRepository.save(matrix);

        // resync
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false),
                sskuMatrix(77, SOFINO_ID, "sku2", null),
                sskuMatrix(84, TOMILINO_ID, "sku3", false)
            );
    }

    @Test
    public void testDontDeleteAvailabilityChangedByUserAfterDeadstock() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        DeadstockStatus status2 = deadstockStatus(77, "sku2", SOFINO_ID, 2);
        DeadstockStatus status3 = deadstockStatus(84, "sku3", TOMILINO_ID, 2);
        status1 = deadstockStatusRepository.save(status1);
        status2 = deadstockStatusRepository.save(status2);
        status3 = deadstockStatusRepository.save(status3);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false),
                sskuMatrix(77, SOFINO_ID, "sku2", false),
                sskuMatrix(84, TOMILINO_ID, "sku3", false)
            );

        // меняем блокировку как это бы сделал пользователь
        SskuAvailabilityMatrix matrix = sskuAvailabilityMatrixRepository.find(
                new SskuAvailabilityFilter().setShopSkuWKeys(Set.of(new ShopSkuWKey(status2))))
            .get(0);
        matrix.setAvailable(false);
        sskuAvailabilityMatrixRepository.save(matrix);

        // delete all statuses
        deadstockStatusRepository.delete(List.of(status1.getId(), status2.getId(), status3.getId()));
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", null),
                sskuMatrix(77, SOFINO_ID, "sku2", false),
                sskuMatrix(84, TOMILINO_ID, "sku3", null)
            );
    }

    @Test
    public void testLockAvailabilityCreatedByUserIfDeadstock() {
        SskuAvailabilityMatrix matrix = sskuMatrix(77, ROSTOV_ID, "sku1", false);
        sskuAvailabilityMatrixRepository.save(matrix);

        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        deadstockStatusRepository.save(status1);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
            );
    }

    @Test
    public void testLockAvailabilityOnlyInsertedDeadstockStatuses() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        status1 = deadstockStatusRepository.save(status1);
        autoDeadstockRobot.execute();
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
            );

        SskuAvailabilityMatrix matrix = sskuAvailabilityMatrixRepository
            .findByKeys(Set.of(new ShopSkuWKey(77, "sku1", ROSTOV_ID))).get(0);
        sskuAvailabilityMatrixRepository.save(matrix.setAvailable(false));

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
            );

        deadstockStatusRepository.save(status1);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
            );
    }

    @Test
    public void testUnlockDeadstockAvailabilityByManualChange() {
        DeadstockStatus status1 = deadstockStatus(77, "sku1", ROSTOV_ID, 1);
        deadstockStatusRepository.save(status1);
        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
            );

        SskuAvailabilityMatrix matrix = sskuAvailabilityMatrixRepository
            .findByKeys(Set.of(new ShopSkuWKey(77, "sku1", ROSTOV_ID))).get(0);
        sskuAvailabilityMatrixRepository.save(matrix.setAvailable(false));

        autoDeadstockRobot.execute();

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", false)
            );
    }

    // ASSORTMENT

    @Test
    public void testBlockSingle() {

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 13),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-1-2", SOFINO_ID, 10),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 3),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 7),

            deadstockStatus(222222, "sub-ssku-2-2", SOFINO_ID, 9)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-1", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID)
            );
    }

    @Test
    public void testBlockSeveral() {

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 1),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 2),
            deadstockStatus(111111, "sub-ssku-1-2", SOFINO_ID, 3),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 4),
            deadstockStatus(222222, "sub-ssku-2-2", TOMILINO_ID, 5),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 6),

            deadstockStatus(222222, "sub-ssku-2-2", SOFINO_ID, 7)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-1", SOFINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID)
            );
    }

    @Test
    public void testUnblockSingle() {

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 6),
            deadstockStatus(111111, "sub-ssku-1-2", TOMILINO_ID, 15),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 22),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 7),
            deadstockStatus(222222, "sub-ssku-2-2", TOMILINO_ID, 6),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 10),

            deadstockStatus(222222, "sub-ssku-2-3", SOFINO_ID, 9)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID)
            );
    }

    @Test
    public void testUnblockSeveral() {

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 9),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 15),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 4),
            deadstockStatus(222222, "sub-ssku-2-2", TOMILINO_ID, 3),

            deadstockStatus(222222, "sub-ssku-2-3", SOFINO_ID, 0)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                unblockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID)
            );
    }

    @Test
    public void testBlockAndUnblockSeveral() {

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                unblockedMatrix(333333, "assort-ssku-3", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),

                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),

                unblockedMatrix(333333, "sub-ssku-3-1", SOFINO_ID),
                unblockedMatrix(333333, "sub-ssku-3-2", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 9),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-1-2", SOFINO_ID, 4),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 23),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 35),

            deadstockStatus(222222, "sub-ssku-2-1", SOFINO_ID, 7),
            deadstockStatus(222222, "sub-ssku-2-2", SOFINO_ID, 8),
            deadstockStatus(222222, "sub-ssku-2-3", SOFINO_ID, 4),

            deadstockStatus(333333, "sub-ssku-3-1", SOFINO_ID, 0),
            deadstockStatus(333333, "sub-ssku-3-2", SOFINO_ID, 0),
            deadstockStatus(333333, "sub-ssku-3-3", SOFINO_ID, 4),

            deadstockStatus(333333, "sub-ssku-3-3", TOMILINO_ID, 5)

        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                unblockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(111111, "assort-ssku-1", SOFINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", SOFINO_ID),
                unblockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),

                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),

                blockedMatrix(333333, "sub-ssku-3-1", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID),
                unblockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                unblockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", SOFINO_ID)
            );
    }

    @Test
    public void testNoChanges() {
        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                unblockedMatrix(333333, "assort-ssku-3", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID)
            )
        );

        var expected = sskuAvailabilityMatrixRepository.findAll();

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-1-2", TOMILINO_ID, 4),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 23),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 35),
            deadstockStatus(222222, "sub-ssku-2-2", TOMILINO_ID, 7),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 8),

            deadstockStatus(222222, "sub-ssku-2-1", SOFINO_ID, 4),
            deadstockStatus(222222, "sub-ssku-2-2", SOFINO_ID, 5),
            deadstockStatus(222222, "sub-ssku-2-3", SOFINO_ID, 2),

            deadstockStatus(333333, "sub-ssku-3-1", TOMILINO_ID, 1),
            deadstockStatus(333333, "sub-ssku-3-2", TOMILINO_ID, 8),
            deadstockStatus(333333, "sub-ssku-3-3", TOMILINO_ID, 17),

            deadstockStatus(333333, "sub-ssku-3-3", SOFINO_ID, 15)

        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .containsExactlyInAnyOrder(
                expected.toArray(SskuAvailabilityMatrix[]::new)
            );
    }

    @Test
    public void testBlockPackingMaterials() {
        Instant now = LocalDateTime.now().minusDays(50).toInstant(ZoneOffset.UTC);

        deepmindSupplierRepository.save(
            new Supplier().setId(444444).setName("name4")
        );
        // по идее АТ имееют 1 категорию на все подSSKU
        serviceOfferReplicaRepository.save(
            newOffer(444444, "assort-ssku-2", PACKING_MATERIALS_CATEGORY_ID)
        );

        assortSskuRepository.save(
            new AssortSsku(444444, "sub-ssku-2-1", "assort-ssku-2", now),
            new AssortSsku(444444, "sub-ssku-2-2", "assort-ssku-2", now),
            new AssortSsku(444444, "sub-ssku-2-3", "assort-ssku-2", now)
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-1-2", TOMILINO_ID, 4),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 23),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 35),
            deadstockStatus(222222, "sub-ssku-2-2", TOMILINO_ID, 7),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 8),

            deadstockStatus(222222, "sub-ssku-2-1", SOFINO_ID, 4),
            deadstockStatus(222222, "sub-ssku-2-2", SOFINO_ID, 5),
            deadstockStatus(222222, "sub-ssku-2-3", SOFINO_ID, 2),

            deadstockStatus(333333, "sub-ssku-3-1", TOMILINO_ID, 1),
            deadstockStatus(333333, "sub-ssku-3-2", TOMILINO_ID, 8),
            deadstockStatus(333333, "sub-ssku-3-3", TOMILINO_ID, 17),

            deadstockStatus(333333, "sub-ssku-3-3", SOFINO_ID, 15),

            deadstockStatus(444444, "sub-ssku-2-1", TOMILINO_ID, 1),
            deadstockStatus(444444, "sub-ssku-2-2", TOMILINO_ID, 8),
            deadstockStatus(444444, "sub-ssku-2-3", TOMILINO_ID, 17)

        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),


                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID)
            );
    }

    @Test
    public void testUnblockUserBlocksAreNotChanged() {
        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID).setModifiedLogin("not robot"),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID).setCreatedLogin("user")
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 9),

            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 23),
            deadstockStatus(222222, "sub-ssku-2-3", TOMILINO_ID, 35)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID).setModifiedLogin("not robot"),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID).setCreatedLogin("user"),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID)
            );
    }

    @Test
    public void testBlockRewriteUserBlock() {
        var unblockedByUserAfterDead = sskuMatrix(111111, SOFINO_ID, "assort-ssku-1", true)
            .setCreatedLogin("autoUser")
            .setModifiedLogin("user just now")
            .setCreatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        var blockedByUser = blockedMatrix(333333, "assort-ssku-3", TOMILINO_ID)
            .setCreatedLogin("some user")
            .setCreatedAt(LocalDateTime.now().minusDays(5).toInstant(ZoneOffset.UTC));
        sskuAvailabilityMatrixRepository.save(
            unblockedByUserAfterDead,
                blockedByUser,
            unblockedMatrix(111111, "assort-ssku-1", TOMILINO_ID)
                .setCreatedLogin("not robot 7 days ago")
                .setCreatedAt(LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC)),
            sskuMatrix(111111, ROSTOV_ID, "assort-ssku-1", true)
                .setModifiedLogin("not robot 5 days ago")
                .setCreatedAt(LocalDateTime.now().minusDays(5).toInstant(ZoneOffset.UTC)),
            sskuMatrix(333333, ROSTOV_ID, "assort-ssku-3", null)
                .setModifiedLogin("someUser")
                .setCreatedAt(LocalDateTime.now().minusDays(5).toInstant(ZoneOffset.UTC))
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-1-2", TOMILINO_ID, 4),

            deadstockStatus(111111, "sub-ssku-1-1", ROSTOV_ID, 2),
            deadstockStatus(111111, "sub-ssku-1-2", ROSTOV_ID, 4),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 9),
            deadstockStatus(111111, "sub-ssku-1-2", SOFINO_ID, 8),

            deadstockStatus(333333, "sub-ssku-3-3", TOMILINO_ID, 5),

            deadstockStatus(333333, "sub-ssku-3-1", ROSTOV_ID, 4),
            deadstockStatus(333333, "sub-ssku-3-2", ROSTOV_ID, 3),
            deadstockStatus(333333, "sub-ssku-3-3", ROSTOV_ID, 2)
            );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                unblockedByUserAfterDead,
                blockedByUser,
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID)
                    .setCreatedLogin("not robot 7 days ago")
                    .setModifiedLogin(AUTO_DEADSTOCK_ROBOT),
                blockedMatrix(111111, "assort-ssku-1", ROSTOV_ID)
                    .setCreatedLogin("autoUser")
                    .setModifiedLogin(AUTO_DEADSTOCK_ROBOT),
                blockedMatrix(333333, "assort-ssku-3", ROSTOV_ID)
                    .setCreatedLogin("autoUser")
                    .setModifiedLogin(AUTO_DEADSTOCK_ROBOT),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-1", ROSTOV_ID),
                blockedMatrix(111111, "sub-ssku-1-2", ROSTOV_ID),
                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-1", ROSTOV_ID),
                blockedMatrix(333333, "sub-ssku-3-2", ROSTOV_ID),
                blockedMatrix(333333, "sub-ssku-3-3", ROSTOV_ID)
            );
    }

    @Test
    public void testUnblockWhenBlockedAndSubSskuReplacedWithNewOne() {
        // initial data
        Instant now = LocalDateTime.now().minusDays(50).toInstant(ZoneOffset.UTC);

        serviceOfferReplicaRepository.save(
            newOffer(111111, "assort-ssku-11", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-1", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-2", PACKING_MATERIALS_CATEGORY_ID + 8L)
        );

        assortSskuRepository.save(List.of(
            new AssortSsku(111111, "sub-ssku-11-1", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-2", "assort-ssku-11", now)
        ));

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                blockedMatrix(111111, "assort-ssku-11", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-11-1", SOFINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-11-2", SOFINO_ID, 4),

            deadstockStatus(111111, "sub-ssku-11-1", TOMILINO_ID, 23),
            deadstockStatus(111111, "sub-ssku-11-2", TOMILINO_ID, 35)
        );
        deadstockStatusRepository.save(deadStockStatus);

        // data changes
        assortSskuRepository.deleteByEntities(
            new AssortSsku(111111, "sub-ssku-11-1", "assort-ssku-11", now)
        );
        serviceOfferReplicaRepository.save(
            newOffer(111111, "sub-ssku-11-3", PACKING_MATERIALS_CATEGORY_ID + 8L)
        );
        assortSskuRepository.save(
            new AssortSsku(111111, "sub-ssku-11-3", "assort-ssku-11", now)
        );

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                unblockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                unblockedMatrix(111111, "assort-ssku-11", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID)
            );
    }

    @Test
    public void testUnblockWhenNoDeadstockLeft() {

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 23),

            deadstockStatus(333333, "sub-ssku-3-1", TOMILINO_ID, 1),
            deadstockStatus(333333, "sub-ssku-3-2", TOMILINO_ID, 8),
            deadstockStatus(333333, "sub-ssku-3-3", TOMILINO_ID, 17),

            deadstockStatus(333333, "sub-ssku-3-3", SOFINO_ID, 15)

        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                unblockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID),

                unblockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID)
            );
    }

    @Test
    public void testUnblockWhenSskuDeleted() {
        Instant now = LocalDateTime.now().minusDays(50).toInstant(ZoneOffset.UTC);

        assortSskuRepository.save(List.of(
            new AssortSsku(111111, "sub-ssku-11-1", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-2", "assort-ssku-11", now)
        ));

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(222222, "sub-ssku-2-1", TOMILINO_ID, 9)
        );
        deadstockStatusRepository.save(deadStockStatus);

        assortSskuRepository.deleteByEntities(
            new AssortSsku(111111, "sub-ssku-11-1", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-2", "assort-ssku-11", now)
        );
        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                unblockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID)
            );
    }

    @Test
    public void testUnblockWhenBlockedAndSubSskuDeleted() {
        // initial data
        Instant now = LocalDateTime.now().minusDays(50).toInstant(ZoneOffset.UTC);

        serviceOfferReplicaRepository.save(
            newOffer(111111, "assort-ssku-11", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-1", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-2", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-3", PACKING_MATERIALS_CATEGORY_ID + 8L)
        );

        assortSskuRepository.save(List.of(
            new AssortSsku(111111, "sub-ssku-11-1", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-2", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-3", "assort-ssku-11", now)
        ));

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                blockedMatrix(111111, "assort-ssku-11", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-3", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-3", TOMILINO_ID)
            )
        );


        // data changes
        assortSskuRepository.deleteByEntities(
            new AssortSsku(111111, "sub-ssku-11-3", "assort-ssku-11", now)
        );
        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-11-1", SOFINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-11-2", SOFINO_ID, 6),

            deadstockStatus(111111, "sub-ssku-11-1", TOMILINO_ID, 23),
            deadstockStatus(111111, "sub-ssku-11-2", TOMILINO_ID, 3)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                blockedMatrix(111111, "assort-ssku-11", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", SOFINO_ID),
                unblockedMatrix(111111, "sub-ssku-11-3", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-11-3", TOMILINO_ID)
            );
    }

    @Test
    public void testBlockWhenSubBlockedAndSubSskuDeleted() {
        // initial data
        Instant now = LocalDateTime.now().minusDays(50).toInstant(ZoneOffset.UTC);

        serviceOfferReplicaRepository.save(
            newOffer(111111, "assort-ssku-11", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-1", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-2", PACKING_MATERIALS_CATEGORY_ID + 8L),
            newOffer(111111, "sub-ssku-11-3", PACKING_MATERIALS_CATEGORY_ID + 8L)
        );

        assortSskuRepository.save(List.of(
            new AssortSsku(111111, "sub-ssku-11-1", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-2", "assort-ssku-11", now),
            new AssortSsku(111111, "sub-ssku-11-3", "assort-ssku-11", now)
        ));

        sskuAvailabilityMatrixRepository.save(List.of(
                unblockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                unblockedMatrix(111111, "assort-ssku-11", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID)
            )
        );


        // data changes
        assortSskuRepository.deleteByEntities(
            new AssortSsku(111111, "sub-ssku-11-3", "assort-ssku-11", now)
        );
        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-11-1", SOFINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-11-2", SOFINO_ID, 6),

            deadstockStatus(111111, "sub-ssku-11-1", TOMILINO_ID, 23),
            deadstockStatus(111111, "sub-ssku-11-2", TOMILINO_ID, 3)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                blockedMatrix(111111, "assort-ssku-11", TOMILINO_ID),
                blockedMatrix(111111, "assort-ssku-11", SOFINO_ID),

                blockedMatrix(111111, "sub-ssku-11-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-11-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-11-2", TOMILINO_ID)
            );
    }

    @Test
    public void testBlockAndUnblockAllTypesSsskus() {

        sskuAvailabilityMatrixRepository.save(List.of(
                blockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                unblockedMatrix(333333, "assort-ssku-3", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),

                blockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),

                unblockedMatrix(333333, "sub-ssku-3-1", SOFINO_ID),
                unblockedMatrix(333333, "sub-ssku-3-2", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),

                blockedMatrix(77, "sku1", ROSTOV_ID),
                blockedMatrix(84, "sku2", SOFINO_ID)
            )
        );

        var deadStockStatus = List.of(
            deadstockStatus(111111, "sub-ssku-1-1", TOMILINO_ID, 9),

            deadstockStatus(111111, "sub-ssku-1-1", SOFINO_ID, 5),
            deadstockStatus(111111, "sub-ssku-1-2", SOFINO_ID, 4),

            deadstockStatus(222222, "sub-ssku-2-1", SOFINO_ID, 7),
            deadstockStatus(222222, "sub-ssku-2-2", SOFINO_ID, 8),
            deadstockStatus(222222, "sub-ssku-2-3", SOFINO_ID, 4),

            deadstockStatus(333333, "sub-ssku-3-1", SOFINO_ID, 0),
            deadstockStatus(333333, "sub-ssku-3-2", SOFINO_ID, 0),
            deadstockStatus(333333, "sub-ssku-3-3", SOFINO_ID, 4),

            deadstockStatus(333333, "sub-ssku-3-3", TOMILINO_ID, 5),

            deadstockStatus(77, "sku1", SOFINO_ID, 1),
            deadstockStatus(84, "sku2", ROSTOV_ID, 2),
            deadstockStatus(1002, "sku3", SOFINO_ID, 3),
            deadstockStatus(465852, "sku4", SOFINO_ID, 4)
        );
        deadstockStatusRepository.save(deadStockStatus);

        autoDeadstockRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt", "modifiedLogin")
            .containsExactlyInAnyOrder(
                unblockedMatrix(111111, "assort-ssku-1", TOMILINO_ID),
                blockedMatrix(111111, "assort-ssku-1", SOFINO_ID),
                unblockedMatrix(222222, "assort-ssku-2", TOMILINO_ID),
                blockedMatrix(222222, "assort-ssku-2", SOFINO_ID),
                blockedMatrix(333333, "assort-ssku-3", SOFINO_ID),
                unblockedMatrix(333333, "assort-ssku-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", TOMILINO_ID),
                unblockedMatrix(111111, "sub-ssku-1-2", TOMILINO_ID),

                unblockedMatrix(222222, "sub-ssku-2-1", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-2", TOMILINO_ID),
                unblockedMatrix(222222, "sub-ssku-2-3", TOMILINO_ID),
                blockedMatrix(222222, "sub-ssku-2-1", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-2", SOFINO_ID),
                blockedMatrix(222222, "sub-ssku-2-3", SOFINO_ID),

                blockedMatrix(333333, "sub-ssku-3-1", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-2", SOFINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", SOFINO_ID),
                unblockedMatrix(333333, "sub-ssku-3-1", TOMILINO_ID),
                unblockedMatrix(333333, "sub-ssku-3-2", TOMILINO_ID),
                blockedMatrix(333333, "sub-ssku-3-3", TOMILINO_ID),

                blockedMatrix(111111, "sub-ssku-1-1", SOFINO_ID),
                blockedMatrix(111111, "sub-ssku-1-2", SOFINO_ID),

                unblockedMatrix(77, "sku1", ROSTOV_ID),
                unblockedMatrix(84, "sku2", SOFINO_ID),
                blockedMatrix(77, "sku1", SOFINO_ID),
                blockedMatrix(84, "sku2", ROSTOV_ID),
                blockedMatrix(1002, "sku3", SOFINO_ID),
                blockedMatrix(465852, "sku4", SOFINO_ID)
            );
    }

    private DeadstockStatus deadstockStatus(int supplierId, String shopSku, long warehouse, int daysToSubstract) {
        Instant date = Instant.now().minus(daysToSubstract, ChronoUnit.DAYS);
        return new DeadstockStatus()
            .setShopSku(shopSku)
            .setSupplierId(supplierId)
            .setWarehouseId(warehouse)
            .setDeadstockSince(date.atZone(ZoneId.systemDefault()).toLocalDate())
            .setImportTs(date);
    }

    private SskuAvailabilityMatrix blockedMatrix(int supplierId, String shopSku, long warehouseId) {
        return sskuMatrix(supplierId, warehouseId, shopSku, false)
            .setCreatedLogin(AUTO_DEADSTOCK_ROBOT)
            .setComment(BLOCK_DEADSTOCK_COMMENT)
            .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK);
    }

    private SskuAvailabilityMatrix unblockedMatrix(int supplierId, String shopSku, long warehouseId) {
        return sskuMatrix(supplierId, warehouseId, shopSku, null)
            .setCreatedLogin(null)
            .setComment(UNBLOCK_DEADSTOCK_COMMENT)
            .setBlockReasonKey(null);
    }

    private SskuAvailabilityMatrix sskuMatrix(int supplierId, long rostovId, String sku1, @Nullable Boolean available) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(supplierId)
            .setWarehouseId(rostovId)
            .setShopSku(sku1)
            .setAvailable(available);
    }

    private ServiceOfferReplica newOffer(
        int supplierId, String shopSku) {
        var supplier = deepmindSupplierRepository.findById(supplierId);
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(11L)
            .setSupplierType(supplier.orElseThrow().getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ServiceOfferReplica newOffer(
        int supplierId, String shopSku, Long categoryId) {
        return newOffer(supplierId, shopSku).setCategoryId(categoryId);
    }
}
