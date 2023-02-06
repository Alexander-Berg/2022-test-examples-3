package ru.yandex.market.mboc.tms.executors.datacamp;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.OfferResendToDataCampQueueRepository;
import ru.yandex.market.mboc.common.services.datacamp.SendDataCampOfferStatesService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OfferResendToDataCampExecutorTest extends BaseDbTestClass {

    private OfferResendToDataCampExecutor executor;

    @SpyBean
    private OfferResendToDataCampQueueRepository resendToDataCampQueueRepository;
    @MockBean
    private SendDataCampOfferStatesService sendDataCampOfferStatesService;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;




    @Before
    public void setUp() throws Exception {
        executor = new OfferResendToDataCampExecutor(
            resendToDataCampQueueRepository,
            sendDataCampOfferStatesService,
            offerBatchProcessor,
            storageKeyValueService
        );
    }

    @Test
    public void resendOk() {
        var savedOffer = createSimpleOffer(1);
        resendToDataCampQueueRepository.enqueueByIds(List.of(savedOffer.getId()), LocalDateTime.now());

        doNothing()
            .when(sendDataCampOfferStatesService).manualSendOfferStates(List.of(savedOffer));

        executor.execute();

        assertTrue(resendToDataCampQueueRepository.findAll().isEmpty());

        verify(sendDataCampOfferStatesService, times(1))
            .manualSendOfferStates(List.of(savedOffer));
        verify(resendToDataCampQueueRepository, times(1)).dequeue(anyList());
        verify(resendToDataCampQueueRepository, never()).updateBatch(anyList());
    }

    @Test
    public void resendFailed() {
        var offer = createSimpleOffer(1);
        var queueItemCreated = LocalDateTime.now();
        var errorMessage = "Send failed";
        resendToDataCampQueueRepository.enqueueByIds(List.of(offer.getId()), queueItemCreated);

        doThrow(new RuntimeException(errorMessage))
            .when(sendDataCampOfferStatesService).manualSendOfferStates(List.of(offer));

        executor.execute();

        var offerQueueItem = resendToDataCampQueueRepository.findById(offer.getId());

        assertNotNull(offerQueueItem);
        assertTrue(offerQueueItem.getLastProcessedTs().isAfter(queueItemCreated));
        assertEquals(1, offerQueueItem.getAttempt());
        assertEquals(errorMessage, offerQueueItem.getLastError());

        verify(sendDataCampOfferStatesService, times(1))
            .manualSendOfferStates(List.of(offer));
        verify(resendToDataCampQueueRepository, never()).dequeue(anyList());
        verify(resendToDataCampQueueRepository, times(1)).updateBatch(anyList());
    }

    private Offer createSimpleOffer(int offerId) {
        var supplier = supplierRepository.insert(OfferTestUtils.simpleSupplier(offerId));
        var offer = OfferTestUtils.simpleOffer(offerId)
            .setBusinessId(supplier.getId());
        offerRepository.insertOffer(offer);
        return offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
    }
}
