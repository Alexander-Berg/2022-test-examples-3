package ru.yandex.market.deepmind.common.repository.service_offer;

import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;

public class ServiceOfferReplicaRepositoryImplTest extends DeepmindBaseDbTestClass {
    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Autowired
    private BeruId beruId;

    @Before
    public void init() {
        var suppliers = YamlTestUtil.readSuppliersFromResource("service_offers/service-suppliers.yml");
        deepmindSupplierRepository.save(suppliers);
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("service_offers/service-offers.yml"));
    }

    @Test
    public void testFindOffersByBizServiceId() {
        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findOffers(
            new ServiceOfferReplicaFilter().setBusinessIds(1, 200));

        Assertions.assertThat(serviceOffers)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                new ServiceOfferReplica().setBusinessId(1).setSupplierId(1).setShopSku("sku11"),
                new ServiceOfferReplica().setBusinessId(1).setSupplierId(1).setShopSku("sku12"),
                new ServiceOfferReplica().setBusinessId(200).setSupplierId(201).setShopSku("sku200"),
                new ServiceOfferReplica().setBusinessId(200).setSupplierId(202).setShopSku("sku200")
            );
    }

    @Test
    public void testFilterByServiceKeys() {
        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findOffersByKeys(List.of(
            new ServiceOfferKey(201, "sku200"),
            new ServiceOfferKey(101, "sku100"),
            new ServiceOfferKey(1, "sku11")
        ));

        Assertions.assertThat(serviceOffers)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                new ServiceOfferReplica().setBusinessId(1).setSupplierId(1).setShopSku("sku11"),
                new ServiceOfferReplica().setBusinessId(100).setSupplierId(101).setShopSku("sku100"),
                new ServiceOfferReplica().setBusinessId(200).setSupplierId(201).setShopSku("sku200")
            );
    }

    @Test
    public void testSearchByServiceKeysAndByBaseFilter() {
        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findOffersByKeys(
            new ServiceOfferReplicaFilter()
                .setBusinessIds(100, 1),
            List.of(
                new ServiceOfferKey(201, "sku200"),
                new ServiceOfferKey(202, "sku202"),
                new ServiceOfferKey(101, "sku100"),
                new ServiceOfferKey(1, "sku11")
            )
        );

        Assertions.assertThat(serviceOffers)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                new ServiceOfferReplica().setBusinessId(1).setSupplierId(1).setShopSku("sku11"),
                new ServiceOfferReplica().setBusinessId(100).setSupplierId(101).setShopSku("sku100")
            );
    }

    @Test
    public void testSearchByNotExistingServiceKeys() {
        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findOffersByKeys(List.of(
            new ServiceOfferKey(201, "sku200"),
            new ServiceOfferKey(202, "sku200"),
            new ServiceOfferKey(393837, "i-am-no-exist")
        ));

        Assertions.assertThat(serviceOffers)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                new ServiceOfferReplica().setBusinessId(200).setSupplierId(201).setShopSku("sku200"),
                new ServiceOfferReplica().setBusinessId(200).setSupplierId(202).setShopSku("sku200")
            );
    }

    @Test
    public void testSearchByShopSkuKeysIfTheyDuplicates() {
        deepmindSupplierRepository.save(
            supplier(500, 500, SupplierType.BUSINESS),
            supplier(501, 500, SupplierType.THIRD_PARTY),
            supplier(502, 500, SupplierType.THIRD_PARTY),
            supplier(600, 600, SupplierType.BUSINESS),
            supplier(700, 700, SupplierType.BUSINESS),
            supplier(701, 700, SupplierType.FIRST_PARTY)
        );

        serviceOfferReplicaRepository.save(
            offer(500, 501, "sku1"),
            offer(500, 502, "sku1"),
            offer(500, 501, "sku2"),
            offer(500, 502, "sku2"),
            offer(600, 600, "sku2"),
            offer(700, 701, "sku2"),
            offer(700, 701, "sku3")
        );

        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findOffersByKeys(List.of(
            new ServiceOfferKey(501, "sku1"),
            new ServiceOfferKey(502, "sku2")
        ));

        Assertions.assertThat(serviceOffers)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                new ServiceOfferReplica().setBusinessId(500).setSupplierId(501).setShopSku("sku1"),
                new ServiceOfferReplica().setBusinessId(500).setSupplierId(502).setShopSku("sku2")
            );
    }

    @Test
    public void testFindOffers1PGreaterByRealShopSku() {
        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findAll();
        serviceOfferReplicaRepository.deleteByEntities(serviceOffers);
        deepmindSupplierRepository.deleteAll();
        deepmindSupplierRepository.save(
            supplier(111, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000111"),
            supplier(222, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000222"),
            supplier(333, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000333")
        );
        serviceOfferReplicaRepository.save(
            offer(beruId.getBusinessId(), 111, "sku-111", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 111, "sku-112", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 111, "sku-113", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 222, "sku-221", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 222, "sku-222", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 333, "sku-331", SupplierType.REAL_SUPPLIER)
        );
        var result = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setGreaterRealShopSku("000222.sku-221"));
        Assertions.assertThat(result)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 222, "sku-222", SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 333, "sku-331", SupplierType.REAL_SUPPLIER)
            );
    }

    @Test
    public void testFindOffers1PLessOrEqualByRealShopSku() {
        List<ServiceOfferReplica> serviceOffers = serviceOfferReplicaRepository.findAll();
        deepmindSupplierRepository.deleteAll();
        serviceOfferReplicaRepository.deleteByEntities(serviceOffers);
        deepmindSupplierRepository.save(
            supplier(111, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000111"),
            supplier(222, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000222"),
            supplier(333, null, SupplierType.REAL_SUPPLIER).setRealSupplierId("000333")
        );
        serviceOfferReplicaRepository.save(
            offer(beruId.getBusinessId(), 111, "sku-111", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 111, "sku-112", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 111, "sku-113", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 222, "sku-221", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 222, "sku-222", SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 333, "sku-331", SupplierType.REAL_SUPPLIER)
        );
        var result = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setLessOrEqualRealShopSku("000222.sku-221"));
        Assertions.assertThat(result)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 111, "sku-111", SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 111, "sku-112", SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 111, "sku-113", SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 222, "sku-221", SupplierType.REAL_SUPPLIER)
            );
    }

    public Supplier supplier(int id, Integer businessId, SupplierType type) {
        return new Supplier()
            .setId(id).setBusinessId(businessId)
            .setName("Supplier")
            .setSupplierType(type);
    }

    private ServiceOfferReplica offer(
        int businessId, int supplierId, String shopSku, SupplierType supplierType) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(111L)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ServiceOfferReplica offer(
        int businessId, int supplierId, String shopSku) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(111L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
