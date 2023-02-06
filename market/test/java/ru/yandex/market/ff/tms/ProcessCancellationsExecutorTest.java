package ru.yandex.market.ff.tms;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.RequestCancellationService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemFreeze;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link ProcessCancellationsExecutor}.
 *
 * @author avetokhin 29/05/2018.
 */
class ProcessCancellationsExecutorTest extends IntegrationTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private RequestCancellationService cancellationService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    private ProcessCancellationsExecutor executor;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new ProcessCancellationsExecutor(shopRequestFetchingService, cancellationService, executorService,
                requestSubTypeService, historyAgent);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-cancellation/before.xml")
    @ExpectedDatabase(value = "classpath:tms/process-cancellation/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testExecute() throws GatewayApiException, StockStorageFreezeNotFoundException {
        when(stockStorageOutboundClient.getFreezes("2")).thenReturn(List.of(
                SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, false)
        ));
        executor.doJob(null);

        // Отменено изъятие из VALIDATED
        verify(stockStorageOutboundClient).getFreezes("2");
        verify(stockStorageOutboundClient).unfreezeStocks(2);

        // Отменено изъятие из ACCEPTED_BY_SERVICE
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId("3")
                .setPartnerId("33")
                .build();
        verify(fulfillmentClient).cancelOutbound(resourceId, new Partner(121L));

        final ResourceId cancelResourceId = ResourceId.builder()
                .setYandexId("4")
                .setPartnerId("44")
                .build();

        // Отменена поставка из ACCEPTED_BY_SERVICE
        verify(fulfillmentClient).cancelInbound(
                cancelResourceId,
                new ru.yandex.market.logistic.gateway.common.model.common.Partner(121L)
        );
    }

    @Test
    @DatabaseSetup("classpath:tms/process-cancellation/before-with-partner-info.xml")
    @ExpectedDatabase(value = "classpath:tms/process-cancellation/after-with-partner-info.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testCancelRequestWithPartnerInfo() {
        when(stockStorageOutboundClient.getFreezes("2")).thenReturn(List.of(
                SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, false)
        ));

        //try not to catch LazyInitializationException in LgwRequestServiceImpl.getApiSpecificType
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-cancellation/before-with-partner-info-24-type.xml")
    @ExpectedDatabase(value = "classpath:tms/process-cancellation/after-with-partner-info-24-type.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testCancelRequestWithPartnerInfoAnd24Type() {
        when(stockStorageOutboundClient.getFreezes("2")).thenReturn(List.of(
                SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, false)
        ));

        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-supply-cancellation-before-confirmation/before.xml")
    @ExpectedDatabase(
            value = "classpath:tms/process-supply-cancellation-before-confirmation/after.xml",
            assertionMode = NON_STRICT
    )
    void executeOnReceiptBeforeConfirmation() {
        executor.doJob(null);

        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-supply-cancellation-after-confirmation/before.xml")
    @ExpectedDatabase(
            value = "classpath:tms/process-supply-cancellation-after-confirmation/after.xml",
            assertionMode = NON_STRICT
    )
    void executeOnReceiptAfterConfirmation() {
        executor.doJob(null);

        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-cancellation/on-supply-cancelled-in-sent-to-service-status.xml")
    @ExpectedDatabase(
            value = "classpath:tms/process-cancellation/on-supply-cancelled-in-sent-to-service-status.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void executeCancelledSupplyInSentToServiceStatus() {
        executor.doJob(null);

        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-cancellation/on-xdoc-supply-cancelled-in-sent-to-service-status.xml")
    @ExpectedDatabase(
            value = "classpath:tms/process-cancellation/on-xdoc-supply-cancelled-in-sent-to-service-status.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void executeCancelledXdockSupplyInSentToServiceStatus() {
        executor.doJob(null);

        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:tms/process-cancellation/on-xdoc-supply-cancelled-in-accepted-by-service-status.xml")
    @ExpectedDatabase(
            value = "classpath:tms/process-cancellation/after-xdoc-supply-cancelled-in-accepted-by-service-status.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void executeCancelledXdockSupplyInAcceptedByServiceStatus() throws GatewayApiException {
        executor.doJob(null);
        verify(fulfillmentClient, times(1)).cancelInbound(any(), any());
    }
}
