package ru.yandex.market.ff.tms;

import java.util.Set;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.dbqueue.producer.CreateAutoAdditionalSupplyOnUnknownBoxesQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.DivergenceActQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.LesReturnBoxEventQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.dbqueue.service.CreateAutoAdditionalSupplyService;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.model.dbqueue.LesReturnBoxEventPayload;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.MbiNotificationService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.service.TransferOutboundService;
import ru.yandex.market.ff.service.implementation.UtilizationTransferService;
import ru.yandex.market.ff.service.util.MbiNotificationTypes;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Функциональный тест для {@link RequestFinalizerExecutor}.
 *
 * @author avetokhin 24/01/18.
 */
class RequestFinalizerExecutorTest extends IntegrationTest {

    private static final long SUPPLIER_ID = 1L;
    private static final long WITHDRAW_ID = 3L;
    private static final long FIT_TRANSFER_ID = 9L;
    private static final long SURPLUS_TRANSFER_ID = 10L;
    private static final long RETURN_ID = 13;


    private static final String NN_UPDATE_DETAILS_OK_PARAMS_XML = ""
            + "<request-info>"
            + "<id>1</id>"
            + "<service-request-id>11</service-request-id>"
            + "<destination-warehouse-id>121</destination-warehouse-id>"
            + "<destination-warehouse-name>test</destination-warehouse-name>"
            + "<merchandise-receipt-date>01 января</merchandise-receipt-date>"
            + "<merchandise-receipt-time>00:00</merchandise-receipt-time>"
            + "</request-info>";

    private static final String NN_UPDATE_DETAILS_ERR_PARAMS_XML = ""
            + "<request-info>"
            + "<id>2</id>"
            + "<service-request-id>22</service-request-id>"
            + "<destination-warehouse-id>121</destination-warehouse-id>"
            + "<destination-warehouse-name>test</destination-warehouse-name>"
            + "<merchandise-receipt-date>01 января</merchandise-receipt-date>"
            + "<merchandise-receipt-time>00:00</merchandise-receipt-time>"
            + "</request-info>";

    private static final String NN_UPDATE_WITHDRAW_DETAILS_ERR_PARAMS_XML = ""
            + "<request-info>"
            + "<id>3</id>"
            + "<service-request-id>33</service-request-id>"
            + "<source-warehouse-id>121</source-warehouse-id>"
            + "<source-warehouse-name>test</source-warehouse-name>"
            + "<merchandise-receipt-date>01 января</merchandise-receipt-date>"
            + "<merchandise-receipt-time>00:00</merchandise-receipt-time>"
            + "</request-info>";


    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private ShopRequestModificationService shopRequestModificationService;

    @Autowired
    private DivergenceActQueueProducer divergenceActQueueProducer;

    @Autowired
    @Qualifier("requestTypesToEmailSecondaryDivergenceAct")
    private Set<RequestType> allowedRequestTypes;

    @Autowired
    private MbiNotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;

    @Autowired
    private UtilizationTransferService utilizationTransferService;

    @Autowired
    private SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer;

    @Autowired
    private LesReturnBoxEventQueueProducer lesReturnBoxEventQueueProducer;

    @Autowired
    private TransferOutboundService transferOutboundService;

    @Autowired
    private CreateAutoAdditionalSupplyOnUnknownBoxesQueueProducer createAutoAdditionalSupplyOnUnknownBoxesQueueProducer;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    @Autowired
    private CreateAutoAdditionalSupplyService createAutoAdditionalSupplyService;

    private RequestFinalizerExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new RequestFinalizerExecutor(shopRequestFetchingService, shopRequestModificationService,
                divergenceActQueueProducer, allowedRequestTypes, notificationService,
                transactionTemplate, Executors.newSingleThreadExecutor(), concreteEnvironmentParamService,
                utilizationTransferService, lesReturnBoxEventQueueProducer,
                transferOutboundService, requestSubTypeService,
                createAutoAdditionalSupplyService, historyAgent);
        jdbcTemplate.update("update request_subtype set send_les_event_with_request_fact = true " +
                "where id in (7, 1107)");
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-request/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfully() throws StockStorageFreezeNotFoundException {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        executor.doJob(null);

        verify(sendMbiNotificationQueueProducer, times(3)).produceSingle(argumentCaptor.capture());
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getSupplierId()).isEqualTo(SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getNotificationType())
                .isEqualTo(MbiNotificationTypes.SUPPLY_FINISHED_WITHOUT_PROBLEMS);

        assertions.assertThat(argumentCaptor.getAllValues().get(1).getSupplierId()).isEqualTo(SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getNotificationType())
                .isEqualTo(MbiNotificationTypes.SUPPLY_FINISHED_WITH_PROBLEMS);

        assertions.assertThat(argumentCaptor.getAllValues().get(2).getSupplierId()).isEqualTo(SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(2).getNotificationType())
                .isEqualTo(MbiNotificationTypes.WITHDRAW_FINISHED_WITH_PROBLEM);

        verify(stockStorageOutboundClient).unfreezeStocks(WITHDRAW_ID);
        verify(stockStorageOutboundClient).unfreezeStocks(FIT_TRANSFER_ID);
        verify(stockStorageOutboundClient).unfreezeStocks(SURPLUS_TRANSFER_ID);
        verifyNoMoreInteractions(stockStorageOutboundClient);

        ArgumentCaptor<LesReturnBoxEventPayload> returnBoxQueueArgumentCaptor =
                ArgumentCaptor.forClass(LesReturnBoxEventPayload.class);
        verify(lesReturnBoxEventQueueProducer).produceSingle(returnBoxQueueArgumentCaptor.capture());
        assertions.assertThat(returnBoxQueueArgumentCaptor.getAllValues().get(0).getEntityId()).isEqualTo(RETURN_ID);
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-without-sending-act.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-request/after-without-sending-act.xml",
            assertionMode = NON_STRICT)
    void executeSuccessfullyWithoutSendingAct() throws StockStorageFreezeNotFoundException {
        executor.doJob(null);

        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        verify(sendMbiNotificationQueueProducer, times(3)).produceSingle(argumentCaptor.capture());
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getSupplierId()).isEqualTo(SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getNotificationType())
                .isEqualTo(MbiNotificationTypes.SUPPLY_FINISHED_WITHOUT_PROBLEMS);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getData())
                .isEqualTo(NN_UPDATE_DETAILS_OK_PARAMS_XML);

        assertions.assertThat(argumentCaptor.getAllValues().get(1).getSupplierId()).isEqualTo(SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getNotificationType())
                .isEqualTo(MbiNotificationTypes.SUPPLY_FINISHED_WITH_PROBLEMS);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getData())
                .isEqualTo(NN_UPDATE_DETAILS_ERR_PARAMS_XML);

        assertions.assertThat(argumentCaptor.getAllValues().get(2).getSupplierId()).isEqualTo(SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(2).getNotificationType())
                .isEqualTo(MbiNotificationTypes.WITHDRAW_FINISHED_WITH_PROBLEM);
        assertions.assertThat(argumentCaptor.getAllValues().get(2).getData())
                .isEqualTo(NN_UPDATE_WITHDRAW_DETAILS_ERR_PARAMS_XML);

        verify(stockStorageOutboundClient).unfreezeStocks(WITHDRAW_ID);
        verify(stockStorageOutboundClient).unfreezeStocks(FIT_TRANSFER_ID);
        verify(stockStorageOutboundClient).unfreezeStocks(SURPLUS_TRANSFER_ID);
        verifyNoMoreInteractions(stockStorageOutboundClient);
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-with-internal.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-request/after-with-internal.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyWithInternalRequestFinalization() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-with-utilization-transfers.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-request/after-with-utilization-transfers.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyWithDifferentUtilizationTransfers() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-return.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-request/after-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyWithAutoCreationOfAdditionalSupply() {
        executor.doJob(null);
    }


    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-empty-utilization-transfer-with-not-active-outbound.xml")
    @ExpectedDatabase(
            value = "classpath:tms/finalize-request/after-empty-utilization-transfer-with-not-active-outbound.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyForEmptyUtilizationTransferWithoutActiveOutbounds() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-empty-utilization-transfer-without-outbounds.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-request/after-empty-utilization-transfer-without-outbounds.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyForEmptyUtilizationTransferWithoutOutbounds() {
        executor.doJob(null);
    }


    @Test
    @DatabaseSetup("classpath:tms/finalize-request/before-utilization-transfer-with-update-count.xml")
    @ExpectedDatabase(
            value = "classpath:tms/finalize-request/after-utilization-transfer-with-update-count.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyForUtilizationTransferWithUpdateFactCount() {
        executor.doJob(null);
    }
}
