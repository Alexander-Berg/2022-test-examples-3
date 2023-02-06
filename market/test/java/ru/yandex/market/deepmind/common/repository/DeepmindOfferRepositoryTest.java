package ru.yandex.market.deepmind.common.repository;

import java.time.Instant;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;

public class DeepmindOfferRepositoryTest extends DeepmindBaseDbTestClass {

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private DeepmindOfferRepository deepmindOfferRepository;

    @Before
    public void setUp() throws Exception {
        deepmindSupplierRepository.save(
            new Supplier().setId(1).setName("name1").setSupplierType(SupplierType.THIRD_PARTY),
            new Supplier().setId(2).setName("name2").setSupplierType(SupplierType.THIRD_PARTY)
        );
        deepmindOfferRepository.save(
            new ServiceOfferReplica()
                .setSupplierId(1)
                .setShopSku("sku-1")
                .setBusinessId(11)
                .setMskuId(111L)
                .setTitle("title-1")
                .setSeqId(1L)
                .setModifiedTs(Instant.now())
                .setSupplierType(SupplierType.THIRD_PARTY)
                .setCategoryId(1L),

            new ServiceOfferReplica()
                .setSupplierId(2)
                .setShopSku("sku-2")
                .setBusinessId(22)
                .setMskuId(222L)
                .setTitle("title-2")
                .setSeqId(2L)
                .setModifiedTs(Instant.now())
                .setSupplierType(SupplierType.THIRD_PARTY)
                .setCategoryId(2L)
        );
    }

    @Test
    public void testFindAll() {
        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactlyInAnyOrder(
                new ServiceOfferReplica()
                    .setSupplierId(1)
                    .setShopSku("sku-1")
                    .setBusinessId(11)
                    .setMskuId(111L)
                    .setTitle("title-1")
                    .setSeqId(1L)
                    .setSupplierType(SupplierType.THIRD_PARTY)
                    .setCategoryId(1L),

                new ServiceOfferReplica()
                    .setSupplierId(2)
                    .setShopSku("sku-2")
                    .setBusinessId(22)
                    .setMskuId(222L)
                    .setTitle("title-2")
                    .setSeqId(2L)
                    .setSupplierType(SupplierType.THIRD_PARTY)
                    .setCategoryId(2L)
            );
    }

    @Test
    public void testFindByKey() {
        Assertions.assertThat(deepmindOfferRepository.findOfferByKey(2, "sku-2"))
            .usingRecursiveComparison()
            .ignoringFields("modifiedTs")
            .isEqualTo(new ServiceOfferReplica()
                .setSupplierId(2)
                .setShopSku("sku-2")
                .setBusinessId(22)
                .setMskuId(222L)
                .setTitle("title-2")
                .setSeqId(2L)
                .setSupplierType(SupplierType.THIRD_PARTY)
                .setCategoryId(2L)
            );
    }

    @Test
    public void testDeleteByKey() {
        deepmindOfferRepository.delete(new ServiceOfferKey(1, "sku-1"));

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorIgnoringFields("modifiedTs")
            .containsExactly(
                new ServiceOfferReplica()
                    .setSupplierId(2)
                    .setShopSku("sku-2")
                    .setBusinessId(22)
                    .setMskuId(222L)
                    .setTitle("title-2")
                    .setSeqId(2L)
                    .setSupplierType(SupplierType.THIRD_PARTY)
                    .setCategoryId(2L)
            );
    }

}
