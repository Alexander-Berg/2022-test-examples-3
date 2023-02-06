package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository.UpdateVersionTsStats;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.tms.executors.CountOfferHiddenStatsExecutor.Stats;

public class CountOfferHiddenStatsExecutorTest extends DeepmindBaseDbTestClass {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ChangedSskuRepository changedSskuRepository;
    @Resource
    private PartnerRelationRepository partnerRelationRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private CountOfferHiddenStatsExecutor executor;

    @Before
    public void setUp() {
        executor = new CountOfferHiddenStatsExecutor(jdbcTemplate, new StorageKeyValueServiceMock());

        deepmindSupplierRepository.save(
            new Supplier().setId(1).setName("supplier 1"),
            new Supplier().setId(2).setName("supplier 2"),
            new Supplier().setId(3).setName("supplier 3")
        );

        serviceOfferReplicaRepository.save(
            createOffer(1, "sku1"),
            createOffer(1, "sku2"),
            createOffer(2, "sku1"),
            createOffer(2, "sku2"),
            createOffer(3, "sku")
        );

        partnerRelationRepository.save(
            partnerRelation(1, PartnerRelationType.CROSSDOCK),
            partnerRelation(2, PartnerRelationType.DROPSHIP)
        );

        // обновляем версию, чтобы проставить тип поставщика
        changedSskuRepository.updateVersionTsBySupplier(1, UpdateVersionTsStats.builder().build());
        changedSskuRepository.updateVersionTsBySupplier(2, UpdateVersionTsStats.builder().build());
        changedSskuRepository.updateVersionTsBySupplier(3, UpdateVersionTsStats.builder().build());
    }

    @Test
    public void testStats() {
        changedSskuRepository.updateBatch(
            changedSskuRepository.findByShopSkuKey(1, "sku1").setHidingUploadedStatus(HidingStatus.HIDDEN),
            changedSskuRepository.findByShopSkuKey(1, "sku2").setHidingUploadedStatus(HidingStatus.HIDDEN),
            changedSskuRepository.findByShopSkuKey(2, "sku1").setHidingUploadedStatus(HidingStatus.HIDDEN),
            changedSskuRepository.findByShopSkuKey(2, "sku2").setHidingUploadedStatus(HidingStatus.NOT_HIDDEN),
            changedSskuRepository.findByShopSkuKey(3, "sku").setHidingUploadedStatus(HidingStatus.NOT_HIDDEN)
        );

        var stats = executor.getStats();
        Assertions.assertThat(stats)
            .containsOnly(
                stats(PartnerRelationType.CROSSDOCK, HidingStatus.HIDDEN, 2),
                stats(PartnerRelationType.CROSSDOCK, HidingStatus.NOT_HIDDEN, 0),
                stats(PartnerRelationType.DROPSHIP, HidingStatus.HIDDEN, 1),
                stats(PartnerRelationType.DROPSHIP, HidingStatus.NOT_HIDDEN, 1)
            );
    }

    @Test
    public void testStatsWithZeroHidings() {
        changedSskuRepository.updateBatch(
            changedSskuRepository.findByShopSkuKey(2, "sku1").setHidingUploadedStatus(HidingStatus.NOT_HIDDEN),
            changedSskuRepository.findByShopSkuKey(2, "sku2").setHidingUploadedStatus(HidingStatus.NOT_HIDDEN)
        );

        var stats = executor.getStats();
        Assertions.assertThat(stats)
            .containsOnly(
                stats(PartnerRelationType.DROPSHIP, HidingStatus.HIDDEN, 0),
                stats(PartnerRelationType.DROPSHIP, HidingStatus.NOT_HIDDEN, 2),
                stats(PartnerRelationType.CROSSDOCK, HidingStatus.HIDDEN, 0),
                stats(PartnerRelationType.CROSSDOCK, HidingStatus.NOT_HIDDEN, 0)
            );
    }

    private PartnerRelation partnerRelation(int supplierId, PartnerRelationType type) {
        return new PartnerRelation().setSupplierId(supplierId).setRelationType(type)
            .setFromWarehouseIds(1L).setToWarehouseId(2L);
    }

    private ServiceOfferReplica createOffer(int supplierId, String shopSku) {
        var supplier = deepmindSupplierRepository.findById(supplierId).orElseThrow();
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(supplier.getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }


    private Map.Entry<Stats, Integer> stats(PartnerRelationType type, HidingStatus status, int count) {
        return Map.entry(new Stats(type, status), count);
    }
}
