package ru.yandex.market.deepmind.common.repository;

import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;

public class MskuInfoRepositoryTest extends DeepmindBaseDbTestClass {

    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private MskuInfoRepository mskuInfoRepository;
    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Autowired
    private SupplierRepository deepmindSupplierRepository;

    @Test
    public void findByShopSkuKeysMapTest() {
        insertOffer(1, "ssku-1", 111222L);
        insertOffer(1, "ssku-2", 333444L);
        insertOffer(2, "ssku-1", 111222L);
        deepmindMskuRepository.save(msku(111222L), msku(333444L));
        var mskuInfo1 = mskuInfo(111222L);
        var mskuInfo2 = mskuInfo(333444L);
        mskuInfoRepository.save(mskuInfo1, mskuInfo2);

        var shopSkuKey1 = new ServiceOfferKey(1, "ssku-1");
        var shopSkuKey2 = new ServiceOfferKey(1, "ssku-2");
        var shopSkuKey3 = new ServiceOfferKey(2, "ssku-1");
        var mapping = mskuInfoRepository.findByShopSkuKeysMap(List.of(shopSkuKey1, shopSkuKey2, shopSkuKey3));

        Assertions
            .assertThat(mapping.get(shopSkuKey1))
            .isEqualTo(mskuInfo1);

        Assertions
            .assertThat(mapping.get(shopSkuKey2))
            .isEqualTo(mskuInfo2);

        Assertions
            .assertThat(mapping.get(shopSkuKey3))
            .isEqualTo(mskuInfo1);
    }

    @Test
    public void findCorefixByShopSkuKeysTest() {
        insertOffer(1, "ssku-1", 111222L);
        insertOffer(1, "ssku-2", 333444L);
        insertOffer(2, "ssku-1", 111222L);
        deepmindMskuRepository.save(msku(111222L), msku(333444L));
        var mskuInfo1 = mskuInfoCorefix(111222L);
        var mskuInfo2 = mskuInfo(333444L);
        mskuInfoRepository.save(mskuInfo1, mskuInfo2);

        var shopSkuKey1 = new ServiceOfferKey(1, "ssku-1");
        var shopSkuKey2 = new ServiceOfferKey(1, "ssku-2");
        var shopSkuKey3 = new ServiceOfferKey(2, "ssku-1");
        var mapping = mskuInfoRepository.findCorefixByShopSkuKeys(List.of(shopSkuKey1, shopSkuKey2, shopSkuKey3));

        Assertions
            .assertThat(mapping.size())
            .isEqualTo(2);

        Assertions
            .assertThat(mapping.get(0))
            .isEqualTo(shopSkuKey1);

        Assertions
            .assertThat(mapping.get(1))
            .isEqualTo(shopSkuKey3);
    }

    private void insertOffer(int supplierId, String shopSku, long mskuId) {
        insertOffer(supplierId, shopSku, SupplierType.REAL_SUPPLIER, mskuId, 0);
    }

    private void insertOffer(int supplierId, String shopSku, SupplierType supplierType,
                             long mskuId, long categoryId) {
        var supplier = new Supplier()
            .setId(supplierId)
            .setName("test_supplier_" + supplierId)
            .setSupplierType(supplierType);
        if (supplierType == SupplierType.REAL_SUPPLIER) {
            supplier.setRealSupplierId("00004" + supplierId);
        }
        var offer = new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);

        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        if (suppliers.isEmpty()) {
            deepmindSupplierRepository.save(supplier);
        }
        serviceOfferReplicaRepository.save(offer);
    }

    private Msku msku(long mskuId) {
        return new Msku()
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    private MskuInfo mskuInfo(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(false);
    }

    private MskuInfo mskuInfoCorefix(long mskuId) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(true);
    }
}
