package ru.yandex.market.mboc.tms.executors;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.queue.MappedMskuChangesQueueRepository;
import ru.yandex.market.mboc.common.queue.QueueItem;
import ru.yandex.market.mboc.common.queue.QueueService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.mapping;

public class ProcessMappedMskuChangesExecutorTest extends BaseDbTestClass {

    private AtomicLong idsGenerator;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    @Autowired
    private MappedMskuChangesQueueRepository mappedMskuChangesQueueRepository;
    @Autowired
    @Qualifier("mappedMskuChangesQueueService")
    private QueueService<QueueItem> mappedMskuChangesQueueService;

    private OfferMappingActionService offerMappingActionService;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    private ProcessMappedMskuChangesExecutor executor;

    @Before
    public void setUp() {
        idsGenerator = new AtomicLong();

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            Mockito.mock(NeedContentStatusService.class),
            Mockito.mock(OfferCategoryRestrictionCalculator.class),
            offerDestinationCalculator,
            storageKeyValueService
        );
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        categoryCachingServiceMock.addCategory(OfferTestUtils.defaultCategory());

        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();

        executor = createExecutor();
    }

    @Test
    public void testMappingDataUpdated() {
        Model model = new Model()
            .setId(OfferTestUtils.TEST_SKU_ID)
            .setSkuParentModelId(OfferTestUtils.TEST_MODEL_ID + 1)
            .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID + 1)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID + 1)
            .setModelType(SimpleModel.ModelType.PARTNER_SKU);

        modelStorageCachingServiceMock.addModel(model);

        var offerBefore = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setVendorId(OfferTestUtils.TEST_VENDOR_ID)
            .setModelId(OfferTestUtils.TEST_MODEL_ID)
            .updateApprovedSkuMapping(
                mapping(OfferTestUtils.TEST_SKU_ID, Offer.SkuType.FAST_SKU),
                Offer.MappingConfidence.PARTNER_SELF
            );

        offerRepository.insertOffer(offerBefore);

        mappedMskuChangesQueueService.enqueue(
            new QueueItem(model.getId())
        );

        executor.execute();

        var offerAfter = offerRepository.findOfferByBusinessSkuKey(offerBefore.getBusinessSkuKey());

        MbocAssertions.assertThat(offerAfter).isEqualToIgnoreContent(
            offerBefore
                .setCategoryIdForTests(model.getCategoryId(), Offer.BindingKind.APPROVED)
                .setVendorId(model.getVendorId())
                .setModelId(model.getSkuParentModelId())
                .updateApprovedSkuMapping(
                    mapping(OfferTestUtils.TEST_SKU_ID, Offer.SkuType.fromModel(model))
                )
        );
    }

    @Test
    public void testAllBatchesProcessed() {
        int smallBatchSize = 2;
        QueueService<QueueItem> mappedMskuChangesQueueServiceSpied = Mockito.spy(new QueueService<>(
            mappedMskuChangesQueueRepository, smallBatchSize, smallBatchSize
        ));

        var executorSmallBatch = createExecutor(mappedMskuChangesQueueServiceSpied);

        mappedMskuChangesQueueService.enqueue(
            // 1st batch
            new QueueItem(idsGenerator.incrementAndGet()),
            new QueueItem(idsGenerator.incrementAndGet()),
            // 2nd batch
            new QueueItem(idsGenerator.incrementAndGet()),
            new QueueItem(idsGenerator.incrementAndGet()),
            // 3rd batch
            new QueueItem(idsGenerator.incrementAndGet())
        );

        executorSmallBatch.execute();

        List<QueueItem> inQueue = mappedMskuChangesQueueRepository.findAll();

        // 3 not empty invocations + 1 with empty batch
        verify(mappedMskuChangesQueueServiceSpied, times(4)).handleQueueBatch(any());
        assertThat(inQueue).isEmpty();
    }

    private ProcessMappedMskuChangesExecutor createExecutor() {
        return createExecutor(mappedMskuChangesQueueService);
    }

    private ProcessMappedMskuChangesExecutor createExecutor(QueueService<QueueItem> mappedMskuChangesQueueService) {
        return new ProcessMappedMskuChangesExecutor(
            mappedMskuChangesQueueService,
            offerBatchProcessor,
            modelStorageCachingServiceMock,
            offerMappingActionService,
            storageKeyValueService
        );
    }
}
