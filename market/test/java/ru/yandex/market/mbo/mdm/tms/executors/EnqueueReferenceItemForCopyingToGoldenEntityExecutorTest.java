package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.CopyReferenceItemToGoldenEntityQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class EnqueueReferenceItemForCopyingToGoldenEntityExecutorTest extends MdmBaseDbTestClass {
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
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private CopyReferenceItemToGoldenEntityQueue copyReferenceItemToGoldenEntityQueue;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private Random random;
    private EnqueueReferenceItemForCopyingToGoldenEntityExecutor executor;

    @Before
    public void setUp() throws Exception {
        random = new Random("19 июля - день пирожков с малиновым вареньем. Поздравляю!".hashCode());

        executor = new EnqueueReferenceItemForCopyingToGoldenEntityExecutor(
            super.jdbcTemplate,
            super.transactionTemplate,
            storageKeyValueService,
            mdmSskuGroupManager,
            copyReferenceItemToGoldenEntityQueue
        );

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

        storageKeyValueService.putValue(MdmProperties.SHOULD_RUN_ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY, true);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void testEnqueuing() {
        // given
        referenceItemRepository.insertOrUpdateAll(List.of(
            randomReferenceItemWithVgh(BUSINESS_KEY_1),
            randomReferenceItemWithVgh(SERVICE_KEY_1),  // service key will be skipped
            randomReferenceItemWithVgh(BUSINESS_KEY_2),
            randomReferenceItemWithVgh(SERVICE_KEY_2), // service key will be skipped
            randomReferenceItemWithVgh(UNKNOWN_KEY)
        ));

        // when
        executor.execute();

        // then
        List<ShopSkuKey> enqueuedKeys = copyReferenceItemToGoldenEntityQueue.findAll().stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        Assertions.assertThat(enqueuedKeys).containsExactly(BUSINESS_KEY_1, BUSINESS_KEY_2, UNKNOWN_KEY);
        // job traversed all gold pv repository - no more runs will be done
        Assertions.assertThat(
            storageKeyValueService.getBool(MdmProperties.SHOULD_RUN_ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY, false)
        ).isFalse();
    }

    @Test
    public void testOneRunLimit() {
        // given
        storageKeyValueService.putValue(MdmProperties.ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY_BATCH_SIZE, 1);
        storageKeyValueService.putValue(MdmProperties.ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY_ONE_RUN_LIMIT, 2);
        storageKeyValueService.invalidateCache();

        referenceItemRepository.insertOrUpdateAll(List.of(
            randomReferenceItemWithVgh(BUSINESS_KEY_1),
            randomReferenceItemWithVgh(BUSINESS_KEY_2),
            randomReferenceItemWithVgh(SERVICE_KEY_1),  // service key will be skipped
            randomReferenceItemWithVgh(SERVICE_KEY_2), // service key will be skipped
            randomReferenceItemWithVgh(UNKNOWN_KEY)
        ));

        // when
        executor.execute();

        // then
        List<ShopSkuKey> enqueuedKeys = copyReferenceItemToGoldenEntityQueue.findAll().stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        Assertions.assertThat(enqueuedKeys).containsExactly(BUSINESS_KEY_1, BUSINESS_KEY_2);
        // job traversed not all gold pv repository - there will be next run
        Assertions.assertThat(
            storageKeyValueService.getBool(MdmProperties.SHOULD_RUN_ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY, false)
        ).isTrue();
        // offset saved
        Assertions.assertThat(storageKeyValueService.getValue(
            MdmProperties.ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY_OFFSET,
            ShopSkuKey.class
        )).isEqualTo(BUSINESS_KEY_2);
    }

    @Test
    public void testOffset() {
        // given
        storageKeyValueService.putValue(MdmProperties.ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY_OFFSET, BUSINESS_KEY_1);

        referenceItemRepository.insertOrUpdateAll(List.of(
            randomReferenceItemWithVgh(BUSINESS_KEY_1),
            randomReferenceItemWithVgh(SERVICE_KEY_1),  // service key will be skipped
            randomReferenceItemWithVgh(BUSINESS_KEY_2),
            randomReferenceItemWithVgh(SERVICE_KEY_2), // service key will be skipped
            randomReferenceItemWithVgh(UNKNOWN_KEY)
        ));

        // when
        executor.execute();

        // then
        List<ShopSkuKey> enqueuedKeys = copyReferenceItemToGoldenEntityQueue.findAll().stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        Assertions.assertThat(enqueuedKeys).containsExactly(BUSINESS_KEY_2, UNKNOWN_KEY);
        // job traversed all gold pv repository - no more runs will be done
        Assertions.assertThat(
            storageKeyValueService.getBool(MdmProperties.SHOULD_RUN_ENQUEUE_REFERENCE_ITEM_TO_GOLDEN_ENTITY, false)
        ).isFalse();
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
}
