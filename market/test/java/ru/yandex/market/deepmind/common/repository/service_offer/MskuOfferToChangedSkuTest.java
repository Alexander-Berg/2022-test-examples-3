package ru.yandex.market.deepmind.common.repository.service_offer;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kravchenko-aa
 * @date 13.05.2020
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MskuOfferToChangedSkuTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    @Autowired
    private ChangedSskuRepository changedSskuRepository;
    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(
            new Supplier().setName("111").setId(111).setBusinessId(100).setName("supplier111"),
            new Supplier().setName("222").setId(222).setBusinessId(100).setName("supplier222")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000401"),
            new Supplier().setName("333").setId(333).setBusinessId(100).setName("supplier333"),
            new Supplier().setName("444").setId(444).setName("supplier444"),
            new Supplier().setName("555").setId(555).setName("supplier555"),
            new Supplier().setName("666").setId(666).setName("supplier666")
        );
        serviceOfferReplicaRepository.save(
            offer(111, "sku-111"),
            offer(222, "sku-222"),
            offer(333, "sku-333")
        );
        clearQueue();
    }

    @Test
    public void createEvents() {
        serviceOfferReplicaRepository.save(
            serviceOfferReplicaRepository.findOfferByKey(111, "sku-111").setMskuId(123L),
            serviceOfferReplicaRepository.findOfferByKey(222, "sku-222").setTitle("new_title"), // skipped
            offer(444, "sku-444")
        );

        execute();

        assertThat(changedSskuRepository.findAll())
            .usingElementComparatorOnFields("shopSku")
            .containsExactlyInAnyOrder(
                new ChangedSsku().setSupplierId(111).setShopSku("sku-111"), // mapping changed
                new ChangedSsku().setSupplierId(444).setShopSku("sku-444") // new offer
            );
    }


    @Test
    public void insertOfferTest() {
        serviceOfferReplicaRepository.save(
            offer(444, "sku-444")
        );

        execute();

        assertThat(changedSskuRepository.findAll())
            .usingElementComparatorOnFields("shopSku")
            .containsExactlyInAnyOrder(
                new ChangedSsku().setSupplierId(444).setShopSku("sku-444") // new offer
            );
    }

    @Test
    public void updateOfferMappingTest() {
        serviceOfferReplicaRepository.save(
            serviceOfferReplicaRepository.findOfferByKey(111, "sku-111").setMskuId(123L)
        );

        execute();

        assertThat(changedSskuRepository.findAll())
            .usingElementComparatorOnFields("shopSku")
            .containsExactlyInAnyOrder(
                new ChangedSsku().setSupplierId(111).setShopSku("sku-111") // mapping changed
            );
    }

    @Test
    public void updateOfferNotMappingTest() {
        serviceOfferReplicaRepository.save(
            serviceOfferReplicaRepository.findOfferByKey(222, "sku-222").setTitle("new_title") // skipped
        );

        execute();

        assertThat(changedSskuRepository.findAll()).isEmpty();
    }

    @Test
    public void offerWillBeProcessedIfAnotherAuditActionPresent() {
        serviceOfferReplicaRepository.save(
            offer(444, "sku-444")
        );

        execute();

        var changedSskuOld = changedSskuRepository.findAll().get(0);
        //Обновили offer еще раз, таймстемп должен тоже измениться
        serviceOfferReplicaRepository.save(
            serviceOfferReplicaRepository.findOfferByKey(444, "sku-444").setMskuId(123L)
        );

        execute();

        var changedSskuNew = changedSskuRepository.findAll().get(0);
        assertThat(changedSskuOld.getVersionTs()).isNotEqualTo(changedSskuNew.getVersionTs());
    }

    @Test
    public void deleteOfferTest() {
        serviceOfferReplicaRepository.delete(new ServiceOfferKey(111, "sku-111"));

        execute();

        // It's empty now, because we generate changed_ssku rows based on msku.offer
        // where we have no rows after deletion. It will cause extra rows in YT uploads, but in future we are going to
        // delete such offer in uploads too.
        assertThat(changedSskuRepository.findAll()).isEmpty();
    }

    private ServiceOfferReplica offer(int supplierId, String ssku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(33L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
