package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_RETURN_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.AUTO_CONTENT_ROBOT;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.FIRST_PARTY;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.REAL_SUPPLIER;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.THIRD_PARTY;

public class AutoContentRobotTest extends DeepmindBaseDbTestClass {

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;

    private AutoContentRobot autoContentRobot;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        var msku = TestUtils.newMsku(1111);
        deepmindMskuRepository.save(msku);

        deepmindSupplierRepository.save(
            new Supplier().setId(1).setName("supplier1"),
            new Supplier().setId(2).setName("supplier1"),
            new Supplier().setId(3).setName("supplier1"),
            new Supplier().setId(4).setName("supplier1"),
            new Supplier().setId(5).setName("supplier1")
        );

        autoContentRobot = new AutoContentRobot(jdbcTemplate, sskuAvailabilityMatrixRepository);
        autoContentRobot.setBatchSize(2);
    }

    @Test
    public void testBlock() {
        serviceOfferReplicaRepository.save(
            offer(1, "ssku-1", THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "ssku-2", THIRD_PARTY, OfferAcceptanceStatus.TRASH),
            offer(3, "ssku-3", THIRD_PARTY, OfferAcceptanceStatus.OK),
            offer(4, "ssku-4", FIRST_PARTY, OfferAcceptanceStatus.NEW),
            offer(5, "ssku-5", REAL_SUPPLIER, OfferAcceptanceStatus.TRASH)
        );
        sskuAvailabilityMatrixRepository.save(
            matrix(1, "ssku-1", ROSTOV_ID, null, "test-user", "test-user")
                .setBlockReasonKey(null)
        );

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                matrix(1, "ssku-1", ROSTOV_ID, false, "test-user", AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", TOMILINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", MARSHRUT_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null),

                matrix(2, "ssku-2", ROSTOV_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(2, "ssku-2", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(2, "ssku-2", TOMILINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(2, "ssku-2", MARSHRUT_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(2, "ssku-2", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null)
            );
    }

    @Test
    public void testNotBlockIfAvailableByUser() {
        sskuAvailabilityMatrixRepository.save(
            matrix(1, "ssku-1", ROSTOV_ID, true, "test-user", null),
            matrix(1, "ssku-1", SOFINO_ID, true, "test-user", null)
        );

        serviceOfferReplicaRepository.save(
            offer(1, "ssku-1", THIRD_PARTY, OfferAcceptanceStatus.NEW)
        );

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                matrix(1, "ssku-1", ROSTOV_ID, true, "test-user", null),
                matrix(1, "ssku-1", SOFINO_ID, true, "test-user", null),

                matrix(1, "ssku-1", TOMILINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", MARSHRUT_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null)
            );
    }

    @Test
    public void testUnblock() {
        sskuAvailabilityMatrixRepository.save(
            matrix(1, "ssku-1", ROSTOV_ID, false, AUTO_CONTENT_ROBOT, null),
            matrix(1, "ssku-1", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
            matrix(1, "ssku-1", TOMILINO_ID, true, "test-user", AUTO_CONTENT_ROBOT),
            matrix(1, "ssku-1", MARSHRUT_ID, false, "test-user", AUTO_CONTENT_ROBOT)
        );

        serviceOfferReplicaRepository.save(
            offer(1, "ssku-1", THIRD_PARTY, OfferAcceptanceStatus.OK)
        );

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                matrix(1, "ssku-1", ROSTOV_ID, null, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null),
                matrix(1, "ssku-1", SOFINO_ID, null, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null),
                matrix(1, "ssku-1", TOMILINO_ID, true, "test-user", AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", MARSHRUT_ID, null, "test-user", AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null)
            );
    }

    @Test
    public void testUnblockAndThenBlockAgain() {
        sskuAvailabilityMatrixRepository.save(
            matrix(1, "ssku-1", ROSTOV_ID, false, AUTO_CONTENT_ROBOT, null),
            matrix(1, "ssku-1", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
            matrix(1, "ssku-1", TOMILINO_ID, true, "test-user", AUTO_CONTENT_ROBOT),
            matrix(1, "ssku-1", MARSHRUT_ID, false, "test-user", AUTO_CONTENT_ROBOT)
        );

        var offer = serviceOfferReplicaRepository.save(offer(1, "ssku-1", THIRD_PARTY, OfferAcceptanceStatus.OK))
            .get(0);

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                matrix(1, "ssku-1", ROSTOV_ID, null, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null),
                matrix(1, "ssku-1", SOFINO_ID, null, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null),
                matrix(1, "ssku-1", TOMILINO_ID, true, "test-user", AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", MARSHRUT_ID, null, "test-user", AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null)
            );

        offer.setAcceptanceStatus(OfferAcceptanceStatus.NEW);
        serviceOfferReplicaRepository.save(offer);

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                matrix(1, "ssku-1", ROSTOV_ID, false, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", SOFINO_ID, false, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", TOMILINO_ID, true, "test-user", AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", MARSHRUT_ID, false, "test-user", AUTO_CONTENT_ROBOT)
            );
    }

    @Test
    public void testModifyExistingMatrix() {
        sskuAvailabilityMatrixRepository.save(
            matrix(1, "ssku-1", TOMILINO_ID, null, "test-user", "test-user"),
            matrix(1, "ssku-1", MARSHRUT_ID, null, "test-user", "test-user")
        );

        serviceOfferReplicaRepository.save(
            offer(1, "ssku-1", THIRD_PARTY, OfferAcceptanceStatus.TRASH)
        );

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                matrix(1, "ssku-1", ROSTOV_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", TOMILINO_ID, false, "test-user", AUTO_CONTENT_ROBOT),
                matrix(1, "ssku-1", MARSHRUT_ID, false, "test-user", AUTO_CONTENT_ROBOT)
            );
    }

    @Test
    public void comboTest() {
        sskuAvailabilityMatrixRepository.save(
            matrix(1, "ssku-1", ROSTOV_ID, true, "test-user", null),
            matrix(1, "ssku-1", SOFINO_ID, false, "test-user", null),
            matrix(2, "ssku-2", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
            matrix(2, "ssku-2", TOMILINO_ID, false, "test-user", AUTO_CONTENT_ROBOT)
        );

        serviceOfferReplicaRepository.save(
            offer(1, "ssku-1", THIRD_PARTY, OfferAcceptanceStatus.NEW),
            offer(2, "ssku-2", THIRD_PARTY, OfferAcceptanceStatus.OK),
            offer(3, "ssku-3", THIRD_PARTY, OfferAcceptanceStatus.TRASH)
        );

        autoContentRobot.execute();

        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "modifiedAt", "createdAt", "comment")
            .containsExactlyInAnyOrder(
                // записи не трогались, так как пользователь выставил от своего имени блокировку
                matrix(1, "ssku-1", ROSTOV_ID, true, "test-user", null),
                matrix(1, "ssku-1", SOFINO_ID, false, "test-user", null),

                // записи создались
                matrix(1, "ssku-1", TOMILINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", MARSHRUT_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(1, "ssku-1", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null),

                // Записи удалены
                matrix(2, "ssku-2", SOFINO_ID, null, AUTO_CONTENT_ROBOT, AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null),
                matrix(2, "ssku-2", TOMILINO_ID, null, "test-user", AUTO_CONTENT_ROBOT)
                    .setBlockReasonKey(null),

                // записи создались
                matrix(3, "ssku-3", ROSTOV_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(3, "ssku-3", SOFINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(3, "ssku-3", TOMILINO_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(3, "ssku-3", MARSHRUT_ID, false, AUTO_CONTENT_ROBOT, null),
                matrix(3, "ssku-3", SOFINO_RETURN_ID, false, AUTO_CONTENT_ROBOT, null)
            );
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, MbocSupplierType supplierType,
        OfferAcceptanceStatus acceptanceStatus) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(1111L)
            .setSupplierType(SupplierType.valueOf(supplierType.name()))
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(acceptanceStatus);
    }

    private SskuAvailabilityMatrix matrix(int supplierId, String shopSku, long warehouseId, Boolean available,
                                          String createdLogin, String modifiedLogin) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setCreatedLogin(createdLogin)
            .setModifiedLogin(modifiedLogin)
            .setBlockReasonKey(BlockReasonKey.SSKU_ACCEPTANCE_STATUS_NEW_OR_TRASH);
    }
}
