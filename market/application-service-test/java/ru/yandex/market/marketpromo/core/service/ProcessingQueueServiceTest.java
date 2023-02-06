package ru.yandex.market.marketpromo.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.application.context.CategoryInterfacePromo;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.dao.internal.ProcessingStageDao;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.utils.PromoTestHelper;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestType;
import ru.yandex.market.marketpromo.service.ProcessingQueueService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

public class ProcessingQueueServiceTest extends ServiceTestBase {

    private static final String CAG_PROMO_ID = "cheapest-as-gift$e7f7ec4b-81f7-4744-b90a-35178b2ff062";


    public static final ProcessingRequestType REQUEST_TYPE = ProcessingRequestType.EXPORT_ASSORTMENT;

    @Autowired
    private ProcessingQueueService processingQueueService;
    @Autowired
    private ProcessingStageDao processingRequestDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    @CategoryInterfacePromo
    private ObjectMapper objectMapper;
    private String key;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        final PromoKey promoKey = IdentityUtils.decodePromoId(CAG_PROMO_ID);
        promoDao.replace(PromoTestHelper.defaultPromo(promoKey));
        key = objectMapper.writeValueAsString(AssortmentRequest.builder(promoKey).build());
//        processingQueueService.enqueue(REQUEST_TYPE, , key);
    }

//    @Test
//    void shouldReturnSuccessRequests() {
//        final List<ProcessingRequest> requestsInQueue = processingQueueService.getRequestsInQueue(1);
//        assertThat(requestsInQueue, hasSize(1));
//
//        final ProcessingRequest requestById = processingQueueService.getRequestById(requestsInQueue.get(0).getId());
//        assertThat(requestById, notNullValue());
//        assertThat(requestById.getStatus(), equalTo(ProcessingRequestStatus.IN_QUEUE));
//    }
//
//    @Test
//    void shouldExpireRequests() {
//        final List<ProcessingRequest> requestsInQueue = processingQueueService.getRequestsInQueue(1);
//        final String result = "result";
//        processingRequestDao.updateRequest(requestsInQueue.get(0).getId(), ProcessingRequestStatus.PROCESSED, result);
//
//        final ProcessingRequest requestById = processingQueueService.getRequestById(requestsInQueue.get(0).getId());
//        assertThat(requestById, notNullValue());
//        assertThat(requestById.getStatus(), equalTo(ProcessingRequestStatus.PROCESSED));
//        assertThat(requestById.getResult(), equalTo(result));
//
//        clock.setFixed(Instant.now().plus(1, ChronoUnit.DAYS), clock.getZone());
//        promoDao.updatePromo(IdentyUtils.decodePromoId(CAG_PROMO_ID).getId());
//        final ProcessingRequest requestAfterUpd = processingQueueService.getRequestById(requestsInQueue.get(0).getId());
//
//        assertThat(requestAfterUpd, notNullValue());
//        assertThat(requestAfterUpd.getStatus(), equalTo(ProcessingRequestStatus.EXPIRED));
//        assertThat(requestAfterUpd.getResult(), equalTo(result));
//
//        final String token = processingQueueService.enqueue(REQUEST_TYPE, , key);
//        final ProcessingRequest reRunRequest = processingQueueService.getRequestById(token);
//
//        assertThat(reRunRequest, notNullValue());
//        assertThat(reRunRequest.getStatus(), equalTo(ProcessingRequestStatus.IN_QUEUE));
//        assertThat(reRunRequest.getResult(), nullValue());
//    }
//
//
//    @Test
//    void shouldReturnProcessingRequests() {
//        final List<ProcessingRequest> requestsInQueue = processingQueueService.getRequestsInQueue(1);
//
//        processingRequestDao.updateRequest(requestsInQueue.get(0).getId(), ProcessingRequestStatus.PROCESSING);
//
//        final ProcessingRequest requestById = processingQueueService.getRequestById(requestsInQueue.get(0).getId());
//        assertThat(requestById, notNullValue());
//        assertThat(requestById.getStatus(), equalTo(ProcessingRequestStatus.PROCESSING));
//    }
//
//    @Test
//    void shouldRerunExpiredRequests() {
//        shouldRerunRequestByStatus(ProcessingRequestStatus.EXPIRED);
//    }
//
//    @Test
//    void shouldRerunCancelledRequests() {
//        shouldRerunRequestByStatus(ProcessingRequestStatus.CANCELLED);
//    }
//
//    private void shouldRerunRequestByStatus(ProcessingRequestStatus cancelled) {
//        final List<ProcessingRequest> requestsInQueue = processingQueueService.getRequestsInQueue(1);
//        assertThat(requestsInQueue, hasSize(1));
//
//        final ProcessingRequest requestById = processingQueueService.getRequestById(requestsInQueue.get(0).getId());
//        assertThat(requestById, notNullValue());
//
//        processingRequestDao.updateRequest(requestById.getId(), cancelled);
//
//        final String token = processingQueueService.enqueue(REQUEST_TYPE, , key);
//
//        final ProcessingRequest reEnqueuedRequest = processingQueueService.getRequestById(token);
//        assertThat(reEnqueuedRequest, notNullValue());
//        assertThat(reEnqueuedRequest.getStatus(), equalTo(ProcessingRequestStatus.IN_QUEUE));
//        assertThat(reEnqueuedRequest.getResult(), nullValue());
//    }
}
