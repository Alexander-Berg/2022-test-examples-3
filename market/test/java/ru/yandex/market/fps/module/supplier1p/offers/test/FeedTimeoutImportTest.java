package ru.yandex.market.fps.module.supplier1p.offers.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.fps.module.supplier1p.offers.Feed;
import ru.yandex.market.fps.module.supplier1p.offers.XlsxByMarketTemplateWritingService;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.ProcessingStatus;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.RequestMode;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.Response;
import ru.yandex.market.mboc.http.MboMappingsService;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(InternalModuleSupplier1pOffersTestConfiguration.class)
public class FeedTimeoutImportTest extends AbstractFeedTest {
    public static final int MBOC_VERIFY_REQUEST_ID = 123;
    private final TriggerServiceImpl triggerService;
    private final RetryTaskProcessor retryTaskProcessor;
    private final FastRetryTasksQueue fastRetryTasksQueue;
    private final TimerTestUtils timerTestUtils;
    private final TxService txService;
    private final DbService dbService;

    @Autowired
    @SuppressWarnings("checkstyle:ParameterNumber")
    public FeedTimeoutImportTest(
            BcpService bcpService,
            AttachmentsService attachmentsService,
            MboMappingsService mboMappingsService,
            TriggerServiceImpl triggerService,
            RetryTaskProcessor retryTaskProcessor,
            FastRetryTasksQueue fastRetryTasksQueue,
            TimerTestUtils timerTestUtils,
            XlsxByMarketTemplateWritingService xlsxByMarketTemplateWritingService,
            TxService txService,
            DbService dbService,
            SupplierTestUtils supplierTestUtils) {
        super(supplierTestUtils, bcpService, attachmentsService, mboMappingsService,
                xlsxByMarketTemplateWritingService);
        this.triggerService = triggerService;
        this.retryTaskProcessor = retryTaskProcessor;
        this.fastRetryTasksQueue = fastRetryTasksQueue;
        this.timerTestUtils = timerTestUtils;
        this.txService = txService;
        this.dbService = dbService;
    }

    /**
     * Валидация падает после наступления таймаута
     */
    @Test
    public void testValidationCrashingAfterTimeout() {
        Response validationInProgressResponse = Response.newBuilder()
                .setRequestId(MBOC_VERIFY_REQUEST_ID)
                .setProcessingStatus(ProcessingStatus.IN_PROGRESS)
                .build();
        doReturn(validationInProgressResponse)
                .when(mboMappingsService)
                .uploadExcelFile(argThat(x -> x.getMode() == RequestMode.VERIFY));

        doReturn(validationInProgressResponse)
                .when(mboMappingsService)
                .getUploadStatus(argThat(x -> x.getRequestId() == MBOC_VERIFY_REQUEST_ID));

        // Then

        try {
            triggerService.withAsyncTriggersMode(() -> {
                var feedGid = txService.doInTx(() -> createFeed().getGid());

                // start validation
                retryTaskProcessor.processPendingTasksWithReset(fastRetryTasksQueue);
                // check upload status
                retryTaskProcessor.processPendingTasksWithReset(fastRetryTasksQueue);
                retryTaskProcessor.processPendingTasksWithReset(fastRetryTasksQueue);
                retryTaskProcessor.processPendingTasksWithReset(fastRetryTasksQueue);

                triggerService.withSyncTriggersMode(() ->
                        timerTestUtils.simulateTimerExpiration(feedGid, Feed.VALIDATION_TIMER)
                );

                verifyNoInteractions(xlsxByMarketTemplateWritingService);

                Assertions.assertEquals(
                        Feed.Statuses.VALIDATION_CRASHED,
                        txService.doInTx(() -> dbService.<Feed>get(feedGid).getStatus())
                );
            });
        } finally {
            txService.runInTx(() -> dbService.createQuery("DELETE FROM feed").executeUpdate());
        }
    }
}
