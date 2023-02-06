package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AvailabilityMatrixIndex;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.AVAILABILITY_MATRIX_INDEX;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.CROSSDOCK_SOFINO;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.SORTING_CENTER_1;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;

public class ReindexMatrixAvailabilityExecutorTest extends DeepmindBaseDbTestClass {

    @Resource(name = "deepmindDsl")
    private DSLContext dslContext;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;


    private ShopSkuMatrixAvailabilityServiceMock shopSkuMatrixAvailabilityService;
    private ReindexMatrixAvailabilityExecutor executor;

    private static Msku createMsku(long mskuId) {
        return new Msku().setId(mskuId).setTitle("msku " + mskuId).setCategoryId(-1L).setVendorId(-1L);
    }

    private static ServiceOfferReplica createOffer(
        int supplierId, String ssku, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.REAL_SUPPLIER)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }


    private static AvailabilityMatrixIndex indexRow(int supplierId, String ssku, long warehouseId) {
        return new AvailabilityMatrixIndex()
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setWarehouseId(warehouseId);
    }

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_DROPSHIP);
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        shopSkuMatrixAvailabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        executor = new ReindexMatrixAvailabilityExecutor(
            jdbcTemplate,
            transactionTemplate,
            serviceOfferReplicaRepository,
            deepmindWarehouseRepository,
            shopSkuMatrixAvailabilityService,
            deepmindStorageKeyValueService,
            100
        );
    }

    @Test
    public void testMatrixIndex() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);

        MatrixAvailability msku1 = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(100L), TOMILINO,
            null, null, null);
        MatrixAvailability category1 = MatrixAvailabilityUtils.mskuInCategory(ROSTOV_ID, "Rostov", 1,
            "category 1", null);
        MatrixAvailability msku2 = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(200L), TOMILINO,
            null, null, null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), MARSHRUT_ID, msku1);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), ROSTOV_ID, category1);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(2, "sku-2"), TOMILINO_ID, msku2);

        // run
        executor.execute();

        var rows = dslContext.selectFrom(AVAILABILITY_MATRIX_INDEX).fetchInto(AvailabilityMatrixIndex.class);
        Assertions.assertThat(rows)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                indexRow(1, "sku-1", MARSHRUT_ID),
                indexRow(1, "sku-1", ROSTOV_ID),
                indexRow(2, "sku-2", TOMILINO_ID)
            );
    }

    @Test
    public void testMatrixIndexUniqueKeyConstraint() {
        var offer1 = createOffer(100, "sku-1", 100L).setSupplierId(1);
        var offer2 = createOffer(100, "sku-1", 200L).setSupplierId(2);
        serviceOfferReplicaRepository.save(offer1, offer2);
        var servOffer1 = serviceOfferReplicaRepository.findOfferByKey(1, "sku-1");
        var servOffer2 = serviceOfferReplicaRepository.findOfferByKey(2, "sku-1");

        MatrixAvailability msku = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(100L), TOMILINO,
            null, null, null);
        shopSkuMatrixAvailabilityService.addAvailability(servOffer1, TOMILINO_ID, msku);
        shopSkuMatrixAvailabilityService.addAvailability(servOffer2, TOMILINO_ID, msku);

        // check validity of temp_availability_matrix_inde_supplier_id_shop_sku_warehous_idx
        // e.g. in case when we set businessId instead of supplierId to AvailabilityMatrixIndex
        var exec = new ReindexMatrixAvailabilityExecutor(
            jdbcTemplate,
            transactionTemplate,
            serviceOfferReplicaRepository,
            deepmindWarehouseRepository,
            shopSkuMatrixAvailabilityService,
            deepmindStorageKeyValueService,
            1
        );
        exec.execute();

        Assertions.assertThat(jdbcTemplate.queryForList(
            "select supplier_id, shop_sku, warehouse_id from msku.availability_matrix_index")
        ).containsExactlyInAnyOrder(
            Map.of("supplier_id", 1, "shop_sku", "sku-1", "warehouse_id", 171L),
            Map.of("supplier_id", 2, "shop_sku", "sku-1", "warehouse_id", 171L));
    }

    @Test
    public void testMatrixIndexCompletelyUpdated() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);

        jdbcTemplate.update("insert into msku.availability_matrix_index (supplier_id, shop_sku, warehouse_id) " +
            " values (1, 'sku-1', 100)");

        MatrixAvailability msku = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(200L), TOMILINO,
            null, null, null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(2, "sku-2"), TOMILINO_ID, msku);

        // run
        executor.execute();

        var rows = dslContext.selectFrom(AVAILABILITY_MATRIX_INDEX).fetchInto(AvailabilityMatrixIndex.class);
        Assertions.assertThat(rows)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                indexRow(2, "sku-2", TOMILINO_ID)
            );
    }

    @Test
    public void testMatrixIndexForCrossdock() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "sku-1");
        MatrixAvailability msku = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(200L),
            CROSSDOCK_SOFINO, null, null, null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), CROSSDOCK_SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(2, "sku-2"), CROSSDOCK_SOFINO_ID, msku);

        // run
        executor.execute();

        var rows = dslContext.selectFrom(AVAILABILITY_MATRIX_INDEX).fetchInto(AvailabilityMatrixIndex.class);
        Assertions.assertThat(rows)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                // sku_id is ignored, because delisted is ignored for crossdock
                indexRow(2, "sku-2", CROSSDOCK_SOFINO_ID)
            );
    }

    @Test
    public void testMatrixIndexForDropship() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "sku-1");
        MatrixAvailability msku = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(200L),
            SORTING_CENTER_1, null, null, null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(1, "sku-1"), SORTING_CENTER_1.getId(), delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferReplicaRepository.findOfferByKey(2, "sku-2"), SORTING_CENTER_1.getId(), msku);

        // run
        executor.execute();

        var rows = dslContext.selectFrom(AVAILABILITY_MATRIX_INDEX).fetchInto(AvailabilityMatrixIndex.class);
        Assertions.assertThat(rows)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                // sku_id is ignored, because delisted is ignored for dropship
                indexRow(2, "sku-2", SORTING_CENTER_1.getId())
            );
    }
}
