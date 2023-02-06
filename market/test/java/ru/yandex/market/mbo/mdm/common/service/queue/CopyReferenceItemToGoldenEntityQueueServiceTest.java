package ru.yandex.market.mbo.mdm.common.service.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.CopyReferenceItemToGoldenEntityQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * TODO: MARKETMDM-1395 Add tests on service params copying, when they will be supported in metadata.
 */
public class CopyReferenceItemToGoldenEntityQueueServiceTest extends MdmBaseDbTestClass {
    private static final int BUSINESS = 1;
    private static final int SERVICE = 2;
    private static final String SHOP_SKU_1 = "1";
    private static final String SHOP_SKU_2 = "2";
    private static final ShopSkuKey BUSINESS_KEY_1 = new ShopSkuKey(BUSINESS, SHOP_SKU_1);
    private static final ShopSkuKey SERVICE_KEY_1 = new ShopSkuKey(SERVICE, SHOP_SKU_1);
    private static final ShopSkuKey BUSINESS_KEY_2 = new ShopSkuKey(BUSINESS, SHOP_SKU_2);
    private static final ShopSkuKey SERVICE_KEY_2 = new ShopSkuKey(SERVICE, SHOP_SKU_2);
    private static final ShopSkuKey UNKNOWN_KEY = new ShopSkuKey(123, "123");

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private CopyReferenceItemToGoldenEntityQueue copyReferenceItemToGoldenEntityQueue;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private CopyReferenceItemToGoldenEntityQueueService queueService;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private MdmParamCache mdmParamCache;

    private Random random;

    @Before
    public void setUp() throws Exception {
        random = new Random("19 июля - день пирожков с малиновым вареньем. Поздравляю!".hashCode());

        mdmSupplierRepository.insertOrUpdateAll(List.of(
            new MdmSupplier()
                .setId(BUSINESS)
                .setType(MdmSupplierType.BUSINESS),
            new MdmSupplier()
                .setId(SERVICE)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS)
                .setBusinessEnabled(true)
        ));
        sskuExistenceRepository.markExistence(List.of(SERVICE_KEY_1, SERVICE_KEY_2), true);

        storageKeyValueService.putValue(MdmProperties.COPY_REFERENCE_ITEM_TO_GOLDEN_ENTITY_ENABLED, true);
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void whenNoExistingCreateNewAndFillWithDataFromReferenceItems() {
        // given
        ReferenceItemWrapper businessReferenceItem = randomReferenceItemWithVgh(BUSINESS_KEY_1);
        ReferenceItemWrapper serviceReferenceItem = changeShopSkuKey(businessReferenceItem, SERVICE_KEY_1);
        referenceItemRepository.insertOrUpdateAll(List.of(businessReferenceItem, serviceReferenceItem));
        copyReferenceItemToGoldenEntityQueue.enqueue(BUSINESS_KEY_1, MdmEnqueueReason.DEVELOPER_TOOL);

        // when
        queueService.processQueueItems();

        // then
        Assertions.assertThat(allFlatSskus(goldSskuRepository.findSsku(BUSINESS_KEY_1).orElseThrow()))
            .containsExactlyInAnyOrder(
                serviceSskuConverter.toServiceSsku(businessReferenceItem),
                serviceSskuConverter.toServiceSsku(serviceReferenceItem)
            );
        Assertions.assertThat(copyReferenceItemToGoldenEntityQueue.getUnprocessedItemsCount()).isZero();
    }

    @Test
    public void whenExistingHasParamsNotFromReferenceItemKeepThem() {
        // given
        ReferenceItemWrapper businessReferenceItem = randomReferenceItemWithVgh(BUSINESS_KEY_1);
        ReferenceItemWrapper serviceReferenceItem = changeShopSkuKey(businessReferenceItem, SERVICE_KEY_1);
        referenceItemRepository.insertOrUpdateAll(List.of(businessReferenceItem, serviceReferenceItem));

        CommonSsku existing = new CommonSsku(BUSINESS_KEY_1);
        KnownMdmParams.INTERMEDIATE_DIMENSION_SSKU_LEVEL_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(existing::addBaseValue);
        goldSskuRepository.insertOrUpdateSsku(existing);

        copyReferenceItemToGoldenEntityQueue.enqueue(BUSINESS_KEY_1, MdmEnqueueReason.DEVELOPER_TOOL);

        // when
        queueService.processQueueItems();

        // then
        Assertions.assertThat(allFlatSskus(goldSskuRepository.findSsku(BUSINESS_KEY_1).orElseThrow()))
            .containsExactlyInAnyOrder(
                (ServiceSsku) serviceSskuConverter.toServiceSsku(businessReferenceItem)
                    .addParamValues(existing.getBaseValues()),
                (ServiceSsku) serviceSskuConverter.toServiceSsku(serviceReferenceItem)
                    .addParamValues(existing.getBaseValues())
            );
        Assertions.assertThat(copyReferenceItemToGoldenEntityQueue.getUnprocessedItemsCount()).isZero();
    }

    @Test
    public void whenExistingHasVghOverwriteThem() {
        // given
        ReferenceItemWrapper businessReferenceItem = randomReferenceItemWithVgh(BUSINESS_KEY_1);
        ReferenceItemWrapper serviceReferenceItem = changeShopSkuKey(businessReferenceItem, SERVICE_KEY_1);
        referenceItemRepository.insertOrUpdateAll(List.of(businessReferenceItem, serviceReferenceItem));

        CommonSsku existing = new CommonSsku(BUSINESS_KEY_1);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(existing::addBaseValue);
        goldSskuRepository.insertOrUpdateSsku(existing);

        copyReferenceItemToGoldenEntityQueue.enqueue(BUSINESS_KEY_1, MdmEnqueueReason.DEVELOPER_TOOL);

        // when
        queueService.processQueueItems();

        // then
        Assertions.assertThat(allFlatSskus(goldSskuRepository.findSsku(BUSINESS_KEY_1).orElseThrow()))
            .containsExactlyInAnyOrder(
                serviceSskuConverter.toServiceSsku(businessReferenceItem),
                serviceSskuConverter.toServiceSsku(serviceReferenceItem)
            )
            .doesNotContain(existing.getBaseSsku());
        Assertions.assertThat(copyReferenceItemToGoldenEntityQueue.getUnprocessedItemsCount()).isZero();
    }

    private ReferenceItemWrapper randomReferenceItemWithVgh(ShopSkuKey key) {
        return new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            key,
            MdmIrisPayload.MasterDataSource.SUPPLIER,
            ImpersonalSourceId.DATACAMP.name(),
            ItemWrapperTestUtil.generateShippingUnit(
                (double) random.nextInt(100),
                (double) random.nextInt(100),
                (double) random.nextInt(100),
                (double) random.nextInt(100),
                (double) random.nextInt(100),
                (double) random.nextInt(100)
            )
        ));
    }


    private static ReferenceItemWrapper changeShopSkuKey(ReferenceItemWrapper referenceItemWrapper, ShopSkuKey newKey) {
        MdmIrisPayload.Item updatedItem = referenceItemWrapper.getItem().toBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setShopSku(newKey.getShopSku())
                .setSupplierId(newKey.getSupplierId()))
            .build();
        return referenceItemWrapper.copy().setReferenceItem(updatedItem);
    }

    private static List<ServiceSsku> allFlatSskus(CommonSsku commonSsku) {
        ArrayList<ServiceSsku> result = new ArrayList<>(commonSsku.getServiceSskusWithInheritedBaseParams().values());
        result.add(commonSsku.getBaseSsku());
        return result;
    }
}
