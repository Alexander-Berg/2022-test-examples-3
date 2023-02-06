package ru.yandex.market.mboc.tms.executors.orchestrator;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.CheckCategorySyncQueueRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.tms.service.orchestrator.OrchestratorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.misc.test.Assert.assertThrows;

public class OrchestratorSenderExecutorTest extends BaseDbTestClass {
    @Autowired
    private CheckCategorySyncQueueRepository checkCategorySyncQueueRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    private OrchestratorService orchestratorService;
    private OrchestratorSenderExecutor orchestratorSenderExecutor;

    @Before
    public void setUp() throws Exception {
        storageKeyValueService.putValue(OrchestratorSenderExecutor.QUEUE_BATCH_SIZE_KEY, 10);
        orchestratorService = Mockito.mock(OrchestratorService.class);
        orchestratorSenderExecutor = new OrchestratorSenderExecutor(
            checkCategorySyncQueueRepository, orchestratorService, storageKeyValueService
        );
    }

    @Test
    public void executeOk() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer = OfferTestUtils.simpleOffer(supplier);
        offerRepository.insertOffers(offer);
        checkCategorySyncQueueRepository.enqueueByIds(List.of(offer.getId()), LocalDateTime.now());
        var queueItemsBefore = checkCategorySyncQueueRepository.findAll();
        assertEquals(queueItemsBefore.size(), 1);

        orchestratorSenderExecutor.execute();

        var queueItemsAfter = checkCategorySyncQueueRepository.findAll();
        assertEquals(queueItemsAfter.size(), 0);
    }

    @Test
    public void executeGotError() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer = OfferTestUtils.simpleOffer(supplier);
        offerRepository.insertOffers(offer);
        checkCategorySyncQueueRepository.enqueueByIds(List.of(offer.getId()), LocalDateTime.now());
        var queueItemsBefore = checkCategorySyncQueueRepository.findAll();
        assertEquals(queueItemsBefore.size(), 1);

        var errorMsg = "Houston we have a problem...";
        doThrow(new RuntimeException(errorMsg))
            .when(orchestratorService).migrateOfferSkuCategoryIfInvalid(anyList());

        assertThrows(() -> orchestratorSenderExecutor.execute(),
            RuntimeException.class,
            e -> e.getMessage().contains(errorMsg)
        );

        var queueItemsAfter = checkCategorySyncQueueRepository.findAll();
        assertEquals(queueItemsAfter.size(), 1);

        var offerQueueItem = queueItemsAfter.get(0);
        assertEquals(offerQueueItem.getLastError(), errorMsg);
        assertEquals(offerQueueItem.getAttempt(), 1);
    }
}
