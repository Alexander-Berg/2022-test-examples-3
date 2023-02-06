package ru.yandex.market.mbo.mdm.common.service.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManagerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author albina-gima
 * @date 2/25/22
 */
public class MdmQueuesManagerTest extends MdmBaseDbTestClass {
    private static final int BATCH_SIZE = 10;

    private static final long MSKU_1 = 10L;
    private static final long MSKU_2 = 20L;
    private static final long MSKU_3 = 30L;

    private static final int SUPPLIER_1 = 100;
    private static final int SUPPLIER_2 = 200;
    private static final int SUPPLIER_3 = 300;
    private static final int BUSINESS_1 = 500;
    private static final int BUSINESS_2 = 505;

    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(SUPPLIER_1, "cat");
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(SUPPLIER_2, "dog");
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(SUPPLIER_3, "parrot");

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmQueuesManagerImpl queueManager;
    @Autowired
    private SskuToRefreshRepository sskuQueue;
    @Autowired
    private MskuToRefreshRepository mskuQueue;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    @Before
    public void setUp() {
        storageKeyValueService.putValue(MdmProperties.BANNED_SUPPLIER_IDS_FOR_ENQUEUEING,
            List.of(BUSINESS_2, SUPPLIER_1, SUPPLIER_2));

        createAndSaveMappings();
    }

    @Test
    public void testGetBannedSuppliersReturnsCorrectListOfSuppliers() {
        // when
        Set<Integer> bannedSuppliers = queueManager.getBannedSuppliers();

        // then
        Assertions.assertThat(bannedSuppliers).isNotEmpty();
        Assertions.assertThat(bannedSuppliers).containsExactlyInAnyOrder(SUPPLIER_1, SUPPLIER_2, BUSINESS_2);
    }

    @Test
    public void testRemoveKeysOfBannedSuppliersShouldReturnKeysOfValidSuppliers() {
        // given
        List<ShopSkuKey> keysToEnqueue = List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3);

        // when
        Collection<ShopSkuKey> filteredSskus = queueManager.deduplicateAndRemoveKeysOfBannedSuppliers(keysToEnqueue);

        // then
        Assertions.assertThat(filteredSskus).containsExactly(SHOP_SKU_KEY_3);
    }

    @Test
    public void testEnqueueSskusShouldEnqueueKeysOfValidSuppliers() {
        // given
        List<ShopSkuKey> keysToEnqueue = List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3);
        createSuppliersAndMarkExistence(BUSINESS_1, SHOP_SKU_KEY_3);
        createSuppliersAndMarkExistence(BUSINESS_2, List.of(SHOP_SKU_KEY_2, SHOP_SKU_KEY_1));

        // when
        queueManager.enqueueSskus(keysToEnqueue, MdmEnqueueReason.DEFAULT);

        // then
        List<ShopSkuKey> enqueuedSskus = sskuQueue.getUnprocessedBatch(BATCH_SIZE)
            .stream()
            .map(info -> new ShopSkuKey(info.getSupplierId(), info.getShopSku()))
            .collect(Collectors.toList());

        Assertions.assertThat(enqueuedSskus).containsExactly(new ShopSkuKey(BUSINESS_1, SHOP_SKU_KEY_3.getShopSku()));
    }

    @Test
    public void testEnqueueMskusBySskusShouldEnqueueKeysOfValidSuppliers() {
        // given
        List<ShopSkuKey> keysToEnqueue = List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3);

        // when
        queueManager.enqueueMskusBySskus(keysToEnqueue, MdmEnqueueReason.DEFAULT);

        // then
        List<Long> enqueuedMskus = mskuQueue.getUnprocessedBatch(BATCH_SIZE)
            .stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());

        Assertions.assertThat(enqueuedMskus).containsExactly(MSKU_3);
    }

    @Test
    public void testEnqueueSskuBySuppliersShouldEnqueueEoxedBusinessKeysOfValidSuppliers() {
        // given
        List<Integer> suppliers = List.of(SUPPLIER_1, SUPPLIER_2, SUPPLIER_3);
        createSuppliersAndMarkExistence(BUSINESS_1, SHOP_SKU_KEY_3);

        // when
        queueManager.enqueueSskuBySuppliers(suppliers, MdmEnqueueReason.DEFAULT);

        // then
        List<ShopSkuKey> enqueuedSskus = sskuQueue.getUnprocessedBatch(BATCH_SIZE)
            .stream()
            .map(info -> new ShopSkuKey(info.getSupplierId(), info.getShopSku()))
            .collect(Collectors.toList());

        Assertions.assertThat(enqueuedSskus).containsExactly(new ShopSkuKey(BUSINESS_1, SHOP_SKU_KEY_3.getShopSku()));
    }

    private void createAndSaveMappings() {
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setMskuId(MSKU_1)
            .setSupplierId(SHOP_SKU_KEY_1.getSupplierId())
            .setShopSku(SHOP_SKU_KEY_1.getShopSku());

        MappingCacheDao mapping2 = new MappingCacheDao()
            .setMskuId(MSKU_2)
            .setSupplierId(SHOP_SKU_KEY_2.getSupplierId())
            .setShopSku(SHOP_SKU_KEY_2.getShopSku());

        MappingCacheDao mapping3 = new MappingCacheDao()
            .setMskuId(MSKU_3)
            .setSupplierId(SHOP_SKU_KEY_3.getSupplierId())
            .setShopSku(SHOP_SKU_KEY_3.getShopSku());

        mappingsCacheRepository.insertOrUpdateAll(List.of(mapping1, mapping2, mapping3));
    }

    private void createSuppliersAndMarkExistence(int businessId, ShopSkuKey serviceKeys) {
        createSuppliersAndMarkExistence(businessId, List.of(serviceKeys));
    }

    private void createSuppliersAndMarkExistence(int businessId, Collection<ShopSkuKey> serviceKeys) {
        var suppliers = new ArrayList();
        suppliers.add(
            new MdmSupplier()
                .setId(businessId)
                .setDeleted(false)
                .setType(MdmSupplierType.BUSINESS));
        serviceKeys.forEach(serviceKey ->
            suppliers.add(new MdmSupplier()
                .setBusinessId(businessId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setDeleted(false)
                .setBusinessEnabled(true)
                .setId(serviceKey.getSupplierId())));
        mdmSupplierRepository.insertOrUpdateAll(suppliers);

        sskuExistenceRepository.markExistence(serviceKeys, true);
    }
}
