package ru.yandex.market.ff.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.listener.event.RequestStatusChangeEvent;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemFreeze;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Status;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusEvent;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.WITHDRAW_IS_READY;

/**
 * @author avetokhin 25/09/17.
 */
class SyncRequestStatusesServiceTest extends AbstractSyncRequestStatusesServiceTest {

    private static final Long REQ_ID_1 = 1L;
    private static final Long REQ_ID_2 = 2L;
    private static final Long REQ_ID_3 = 3L;
    private static final Long REQ_ID_4 = 4L;
    private static final Long REQ_ID_5 = 5L;
    private static final Long REQ_ID_7 = 7L;
    private static final Long REQ_ID_8 = 8L;
    private static final Long REQ_ID_9 = 9L;
    private static final Long REQ_ID_10 = 10L;
    private static final Long REQ_ID_11 = 11L;
    private static final Long REQ_ID_12 = 12L;

    private static final String REQ_EXT_ID_1 = "11";
    private static final String REQ_EXT_ID_2 = "22";
    private static final String REQ_EXT_ID_3 = "33";
    private static final String REQ_EXT_ID_4 = "44";
    private static final String REQ_EXT_ID_5 = "55";
    private static final String REQ_EXT_ID_7 = "77";
    private static final String REQ_EXT_ID_8 = "121";
    private static final String REQ_EXT_ID_9 = "99";
    private static final String REQ_EXT_ID_10 = "1010";
    private static final String REQ_EXT_ID_11 = "1111";
    private static final String REQ_EXT_ID_12 = "1212";

    private static final ResourceId RES_ID_1 = ResourceId.builder()
            .setYandexId(REQ_ID_1.toString())
            .setPartnerId(REQ_EXT_ID_1)
            .build();
    private static final ResourceId RES_ID_2 = ResourceId.builder()
            .setYandexId(REQ_ID_2.toString())
            .setPartnerId(REQ_EXT_ID_2)
            .build();
    private static final ResourceId RES_ID_3 = ResourceId.builder()
            .setYandexId(REQ_ID_3.toString())
            .setPartnerId(REQ_EXT_ID_3)
            .build();
    private static final ResourceId RES_ID_4 = ResourceId.builder()
            .setYandexId(REQ_ID_4.toString())
            .setPartnerId(REQ_EXT_ID_4)
            .build();
    private static final ResourceId RES_ID_5 = ResourceId.builder()
            .setYandexId(REQ_ID_5.toString())
            .setPartnerId(REQ_EXT_ID_5)
            .build();
    private static final ResourceId RES_ID_7 = ResourceId.builder()
            .setYandexId(REQ_ID_7.toString())
            .setPartnerId(REQ_EXT_ID_7)
            .build();
    private static final ResourceId RES_ID_8 = ResourceId.builder()
            .setYandexId(REQ_ID_8.toString())
            .setPartnerId(REQ_EXT_ID_8)
            .build();
    private static final ResourceId RES_ID_9 = ResourceId.builder()
            .setYandexId(REQ_ID_9.toString())
            .setPartnerId(REQ_EXT_ID_9)
            .build();
    private static final ResourceId RES_ID_10 = ResourceId.builder()
            .setYandexId(REQ_ID_10.toString())
            .setPartnerId(REQ_EXT_ID_10)
            .build();
    private static final ResourceId RES_ID_11 = ResourceId.builder()
            .setYandexId(REQ_ID_11.toString())
            .setPartnerId(REQ_EXT_ID_11)
            .build();
    private static final ResourceId RES_ID_12 = ResourceId.builder()
            .setYandexId(REQ_ID_12.toString())
            .setPartnerId(REQ_EXT_ID_12)
            .build();

    private static final Long SERVICE_ID_1 = 555L;
    private static final Long SERVICE_ID_2 = 666L;

    private static final Partner PARTNER_1 = new Partner(SERVICE_ID_1);
    private static final Partner PARTNER_2 = new Partner(SERVICE_ID_2);

    @Autowired
    private FulfillmentClient fulfillmentClient;



    @Autowired
    private SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer;

    @BeforeEach
    void init() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.CANCELLED, "2000-01-05T00:00:00");
        InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, "2000-01-10T00:00:00");
        InboundStatus status3 = inboundStatus(RES_ID_3, StatusCode.ACCEPTED, "2000-01-10T00:00:00");
        InboundStatus status9 = inboundStatus(RES_ID_9, StatusCode.ARRIVED, "2000-01-07T00:00:00");
        InboundStatus status10 = inboundStatus(RES_ID_10, StatusCode.ARRIVED, "2000-01-09T00:00:00");

        OutboundStatus status4 = outboundStatus(RES_ID_4, StatusCode.TRANSFERRED, "2000-01-10T00:00:00");
        OutboundStatus status5 = outboundStatus(RES_ID_5, StatusCode.ASSEMBLED, "2000-01-09T00:00:00");
        OutboundStatus status7 = outboundStatus(RES_ID_7, StatusCode.CANCELLED, "2000-01-10T00:00:00");
        OutboundStatus status11 = outboundStatus(RES_ID_11, StatusCode.ARRIVED, "2000-01-09T00:30:00");
        OutboundStatus status12 = outboundStatus(RES_ID_12, StatusCode.ASSEMBLED, "2000-03-09T00:30:00");

        TransferStatus status8 = transferStatus(RES_ID_8, TransferStatusType.ERROR, "2000-01-10T00:00:00");

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2, RES_ID_3, RES_ID_9, RES_ID_10),
                PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2, status3, status9, status10));
        when(fulfillmentClient.getOutboundsStatus(Arrays.asList(RES_ID_4, RES_ID_11), PARTNER_1))
                .thenReturn(Arrays.asList(status4, status11));
        when(fulfillmentClient.getOutboundsStatus(Arrays.asList(RES_ID_5, RES_ID_7), PARTNER_2))
                .thenReturn(Arrays.asList(status5, status7));
        when(fulfillmentClient.getOutboundsStatus(Collections.singletonList(RES_ID_12), PARTNER_1))
                .thenReturn(Collections.singletonList(status12));
        when(fulfillmentClient.getTransfersStatus(Collections.singletonList(RES_ID_8), PARTNER_1))
                .thenReturn(Collections.singletonList(status8));

        // История первой заявки.
        InboundStatusHistory inboundHistory = new InboundStatusHistory(
                Collections.singletonList(status(StatusCode.CANCELLED, "2000-01-05T00:00:00")),
                RES_ID_1
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_1, PARTNER_1))
                .thenReturn(inboundHistory);

        // История второй заявки.
        inboundHistory = new InboundStatusHistory(
                Arrays.asList(
                        status(StatusCode.ACCEPTANCE, "2000-01-08T00:00:00"),
                        status(StatusCode.ARRIVED, "2000-01-09T00:00:00"),
                        status(StatusCode.ACCEPTED, "2000-01-10T00:00:00")
                ),
                RES_ID_2
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_2, PARTNER_1))
                .thenReturn(inboundHistory);

        // История третьей заявки.
        inboundHistory = new InboundStatusHistory(
                Arrays.asList(
                        status(StatusCode.ACCEPTANCE, "2000-01-10T00:00:00"),
                        status(StatusCode.ARRIVED, "2000-01-09T00:00:00"),
                        status(StatusCode.ACCEPTED, "2000-01-11T00:00:00"),
                        status(StatusCode.ACCEPTED, "2000-01-11T00:00:00"),
                        status(StatusCode.ARRIVED, "2000-01-09T00:00:00"),
                        status(StatusCode.ACCEPTED, "2000-01-11T00:00:00")
                ),
                RES_ID_3
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_3, PARTNER_1))
                .thenReturn(inboundHistory);

        // История девятой заявки.
        inboundHistory = new InboundStatusHistory(
                Collections.singletonList(
                        status(StatusCode.ARRIVED, "2000-01-07T00:00:00")
                ),
                RES_ID_9
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_9, PARTNER_1))
                .thenReturn(inboundHistory);

        // История деcятой заявки.
        inboundHistory = new InboundStatusHistory(
                Collections.singletonList(
                        status(StatusCode.ARRIVED, "2000-01-09T00:00:00")
                ),
                RES_ID_10
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_10, PARTNER_1))
                .thenReturn(inboundHistory);

        // История четвертой заявки.
        OutboundStatusHistory outboundHistory = new OutboundStatusHistory(
                RES_ID_4,
                Arrays.asList(
                        status(StatusCode.ASSEMBLING, "2000-01-08T00:00:00"),
                        status(StatusCode.ASSEMBLED, "2000-01-09T00:00:00")
                )
        );
        when(fulfillmentClient.getOutboundHistory(RES_ID_4, PARTNER_1))
                .thenReturn(outboundHistory);

        // История пятой заявки.
        outboundHistory = new OutboundStatusHistory(
                RES_ID_5,
                Arrays.asList(
                        status(StatusCode.ASSEMBLING, "2000-01-08T00:00:00"),
                        status(StatusCode.ASSEMBLED, "2000-01-09T00:00:00"),
                        status(StatusCode.TRANSFERRED, "2000-01-10T00:00:00")
                )
        );
        when(fulfillmentClient.getOutboundHistory(RES_ID_5, PARTNER_2))
                .thenReturn(outboundHistory);

        // История одиннадцатой заявки.
        outboundHistory = new OutboundStatusHistory(
                RES_ID_11,
                Arrays.asList(
                        status(StatusCode.ARRIVED, "2000-01-09T00:00:00"),
                        status(StatusCode.ARRIVED, "2000-01-09T00:30:00")
                )
        );
        when(fulfillmentClient.getOutboundHistory(RES_ID_11, PARTNER_1))
                .thenReturn(outboundHistory);

        // История собранного изъятия.
        outboundHistory = new OutboundStatusHistory(
                RES_ID_7,
                Collections.singletonList(status(StatusCode.CANCELLED, "2000-01-10T00:00:00"))
        );
        when(fulfillmentClient.getOutboundHistory(RES_ID_7, PARTNER_2))
                .thenReturn(outboundHistory);

        outboundHistory = new OutboundStatusHistory(
                RES_ID_12,
                List.of(
                        status(StatusCode.ASSEMBLING, "2000-02-09T00:30:00"),
                        status(StatusCode.ASSEMBLED, "2000-03-09T00:30:00")
                )
        );

        when(fulfillmentClient.getOutboundHistory(RES_ID_12, PARTNER_1))
                .thenReturn(outboundHistory);

        // История трансфера.
        TransferStatusHistory transferHistory = new TransferStatusHistory(
                ImmutableList.of(
                        new TransferStatusEvent(TransferStatusType.ERROR, new DateTime("2000-01-05T00:00:10"))),
                RES_ID_8
        );

        when(fulfillmentClient.getTransferHistory(RES_ID_8, PARTNER_1))
                .thenReturn(transferHistory);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before.xml")
    @ExpectedDatabase(value = "classpath:service/sync-statuses/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testExecute() throws StockStorageFreezeNotFoundException {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        when(stockStorageOutboundClient.getFreezes(String.valueOf(REQ_ID_7))).thenReturn(List.of(
                SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, false)
        ));

        syncRequests(Set.of(REQ_ID_1, REQ_ID_2, REQ_ID_3, REQ_ID_4, REQ_ID_5, REQ_ID_7,
                REQ_ID_8, REQ_ID_9, REQ_ID_10, REQ_ID_11), false);

        verify(sendMbiNotificationQueueProducer, times(2)).produceSingle(argumentCaptor.capture());
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getSupplierId()).isEqualTo(1L);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getNotificationType())
                .isEqualTo(WITHDRAW_IS_READY);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getData()).isEqualTo(
                "<request-info><id>5</id><service-request-id>55</service-request-id>" +
                "<source-warehouse-id>666</source-warehouse-id>" +
                "<source-warehouse-name>test</source-warehouse-name>" +
                "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                "<merchandise-receipt-time>09:09</merchandise-receipt-time>" +
                "<ready-withdrawals-map><7>77</7></ready-withdrawals-map>" +
                "</request-info>");

        assertions.assertThat(argumentCaptor.getAllValues().get(1).getSupplierId()).isEqualTo(1L);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getNotificationType())
                .isEqualTo(WITHDRAW_IS_READY);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getData()).isEqualTo(
                "<request-info><id>4</id><service-request-id>44</service-request-id>" +
                        "<source-warehouse-id>555</source-warehouse-id>" +
                        "<source-warehouse-name>test</source-warehouse-name>" +
                        "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>09:09</merchandise-receipt-time>" +
                        "<ready-withdrawals-map><4>44</4></ready-withdrawals-map>" +
                        "</request-info>");

        // Анфриз после отмены изъятия.
        verify(stockStorageOutboundClient).getFreezes(String.valueOf(REQ_ID_7));
        verify(stockStorageOutboundClient).unfreezeStocks(REQ_ID_7);

        // Анфриз после реджекта трансфера.
        verify(stockStorageOutboundClient).unfreezeStocks(REQ_ID_8);

        verifyNoMoreInteractions(stockStorageOutboundClient);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-orders-sync.xml")
    @ExpectedDatabase(value = "classpath:service/sync-statuses/after-orders-sync.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testOrdersSync() {

        var status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, "2000-01-10T00:00:00");  //FF In

        var status4 = outboundStatus(RES_ID_4, StatusCode.TRANSFERRED, "2000-01-10T00:00:00"); //FF Out
        when(fulfillmentClient.getInboundsStatus(Collections.singletonList(RES_ID_2), PARTNER_2))
                .thenReturn(Collections.singletonList(status2));
        when(fulfillmentClient.getOutboundsStatus(Collections.singletonList(RES_ID_4), PARTNER_2))
                .thenReturn(Collections.singletonList(status4));
        when(fulfillmentClient.getInboundHistory(RES_ID_2, PARTNER_2))
                .thenReturn(new InboundStatusHistory(Arrays.asList(
                        status(StatusCode.CREATED, "2000-01-04T00:00:00"),
                        status(StatusCode.ARRIVED, "2000-01-04T21:00:00"),
                        status(StatusCode.ACCEPTANCE, "2000-01-04T22:00:00"),
                        status(StatusCode.ACCEPTED, "2000-01-04T23:00:00")),
                        RES_ID_2));
        when(fulfillmentClient.getOutboundHistory(RES_ID_4, PARTNER_2))
                .thenReturn(new OutboundStatusHistory(RES_ID_4, Arrays.asList(
                        status(StatusCode.CREATED, "2000-01-04T00:00:00"),
                        status(StatusCode.ASSEMBLING, "2000-01-04T21:00:00"),
                        status(StatusCode.ASSEMBLED, "2000-01-04T22:00:00"),
                        status(StatusCode.TRANSFERRED, "2000-01-04T23:00:00"))));
        syncRequests(Set.of(REQ_ID_2, REQ_ID_4), false);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-with-different-date-at-status-history-and-request.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/before-with-different-date-at-status-history-and-request.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void testShouldNotTryingToUpdateHistoryIfSameLastStatusDifferentUpdatedAt() {
        OutboundStatus status11 = outboundStatus(RES_ID_11, StatusCode.ARRIVED, "2000-01-09T00:00:00");
        when(fulfillmentClient.getOutboundsStatus(Collections.singletonList(RES_ID_11), PARTNER_1))
                .thenReturn(Collections.singletonList(status11));

        syncRequests(Set.of(REQ_ID_11), false);

        verify(fulfillmentClient).getOutboundsStatus(Collections.singletonList(RES_ID_11), PARTNER_1);
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-with-different-date-on-status-and-history-requests.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/after-with-different-date-on-status-and-history-requests.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void testShouldUpdateOnlyHistoryIfGotDifferentDatesOnStatusAndHistoryRequestAndKnowLasStatus() {
        OutboundStatus status11 = outboundStatus(RES_ID_11, StatusCode.ACCEPTANCE, "2000-01-08T23:59:00");
        when(fulfillmentClient.getOutboundsStatus(Collections.singletonList(RES_ID_11), PARTNER_1))
                .thenReturn(Collections.singletonList(status11));

        OutboundStatusHistory outboundHistory = new OutboundStatusHistory(
                RES_ID_11,
                List.of(
                        status(StatusCode.ARRIVED, "2000-01-04T12:00:00"),
                        status(StatusCode.ACCEPTANCE, "2000-01-09T00:00:00")
                )
        );
        when(fulfillmentClient.getOutboundHistory(RES_ID_11, PARTNER_1))
                .thenReturn(outboundHistory);
        syncRequests(Set.of(REQ_ID_11), false);
    }

    /**
     * Для трансферов ВМС присылает даты обновления статусов в неправильной таймзоне. Таким образом,
     * может получиться, что у статуса в истории updatedAt меньше, чем у предыдущего. В таком случае надо
     * обновлять статус заявки и добавлять статус в историю.
     */
    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-updated-at-less-than-for-previous-status.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/after-updated-at-less-than-for-previous-status.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void testShouldUpdateStatusAndHistoryIfUpdatedBeforePreviousStatus() {
        TransferStatus status8 = transferStatus(RES_ID_8, TransferStatusType.PROCESSING, "2000-01-03T22:00:00");
        when(fulfillmentClient.getTransfersStatus(Collections.singletonList(RES_ID_8), PARTNER_1))
                .thenReturn(Collections.singletonList(status8));

        TransferStatusHistory transferHistory = new TransferStatusHistory(
                List.of(
                        new TransferStatusEvent(TransferStatusType.PROCESSING, new DateTime("2000-01-03T22:00:00"))),
                RES_ID_8
        );
        when(fulfillmentClient.getTransferHistory(RES_ID_8, PARTNER_1))
                .thenReturn(transferHistory);
        syncRequests(Set.of(REQ_ID_8), false);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-with-two-statuses-with-same-timestamp.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/after-with-two-statuses-with-same-timestamp.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void testShouldUpdateOnHigherStatusIfHavingSameTimestampAndDifferentOrder() {
        TransferStatus status8 = transferStatus(RES_ID_8, TransferStatusType.COMPLETED, "2000-01-03T22:00:00");
        when(fulfillmentClient.getTransfersStatus(Collections.singletonList(RES_ID_8), PARTNER_1))
                .thenReturn(Collections.singletonList(status8));

        TransferStatusHistory transferHistory = new TransferStatusHistory(
                List.of(new TransferStatusEvent(TransferStatusType.COMPLETED, new DateTime("2000-01-03T22:00:00")),
                        new TransferStatusEvent(TransferStatusType.PROCESSING, new DateTime("2000-01-03T22:00:00"))),
                RES_ID_8
        );
        when(fulfillmentClient.getTransferHistory(RES_ID_8, PARTNER_1))
                .thenReturn(transferHistory);
        syncRequests(Set.of(REQ_ID_8), false);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-merging-status-history.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/after-merging-status-history.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void multipleEventsInCaseOfMultipleNewStatuses() {
        InboundStatus latestStatus = inboundStatus(RES_ID_8, StatusCode.ACCEPTED, "2000-01-04T23:00:00");
        when(fulfillmentClient.getInboundsStatus(Collections.singletonList(RES_ID_8), PARTNER_1))
                .thenReturn(Collections.singletonList(latestStatus));

        when(fulfillmentClient.getInboundHistory(RES_ID_8, PARTNER_1))
                .thenReturn(
                        new InboundStatusHistory(Arrays.asList(
                                status(StatusCode.CREATED, "2000-01-04T00:00:00"),
                                status(StatusCode.ARRIVED, "2000-01-04T22:00:00"),
                                status(StatusCode.ACCEPTED, "2000-01-04T23:00:00")),
                                RES_ID_8));

        syncRequests(Set.of(REQ_ID_8), false);
        ArgumentCaptor<RequestStatusChangeEvent> captor =
                ArgumentCaptor.forClass(RequestStatusChangeEvent.class);
        Mockito.verify(requestStatusChangeListenerMock, times(2)).onApplicationEvent(captor.capture());
        List<RequestStatusChangeEvent> allEvents = captor.getAllValues();

        assertions.assertThat(allEvents.get(0).getPreviousStatus()).isEqualTo(RequestStatus.ACCEPTED_BY_SERVICE);
        assertions.assertThat(allEvents.get(0).getNewStatus()).isEqualTo(RequestStatus.ARRIVED_TO_SERVICE);

        assertions.assertThat(allEvents.get(1).getPreviousStatus()).isEqualTo(RequestStatus.ARRIVED_TO_SERVICE);
        assertions.assertThat(allEvents.get(1).getNewStatus()).isEqualTo(RequestStatus.PROCESSED);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-with-sub-requests.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/after-with-sub-requests.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void syncStatusesForOutboundWithSubOutbounds() {
        syncRequests(Set.of(REQ_ID_12), false);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before-with-quota-and-slot.xml")
    @ExpectedDatabase(
            value = "classpath:service/sync-statuses/after-with-quota-and-slot.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldReleaseQuotaAndBookedSlotOnCancelByWarehouse() {
        OutboundStatus status = outboundStatus(RES_ID_12, StatusCode.CANCELLED, "2000-03-09T00:30:00");
        when(fulfillmentClient.getOutboundsStatus(Collections.singletonList(RES_ID_12), PARTNER_1))
                .thenReturn(Collections.singletonList(status));

        OutboundStatusHistory outboundHistory = new OutboundStatusHistory(
                RES_ID_12,
                List.of(status(StatusCode.CANCELLED, "2000-03-09T00:30:00"))
        );

        when(fulfillmentClient.getOutboundHistory(RES_ID_12, PARTNER_1))
                .thenReturn(outboundHistory);
        syncRequests(Set.of(REQ_ID_12), false);
    }

    private static InboundStatus inboundStatus(
            final ResourceId resourceId, final StatusCode statusCode, final String dateTime) {

        return new InboundStatus(resourceId, new Status(statusCode, new DateTime(dateTime)));
    }

    private static ru.yandex.market.logistic.gateway.common.model.common.InboundStatus inboundStatus(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId resourceId,
            ru.yandex.market.logistic.gateway.common.model.common.StatusCode statusCode, final String dateTime) {

        return new ru.yandex.market.logistic.gateway.common.model.common.InboundStatus(
                resourceId,
                new ru.yandex.market.logistic.gateway.common.model.common.Status(
                        statusCode,
                        new ru.yandex.market.logistic.gateway.common.model.utils.DateTime(dateTime),
                        ""));
    }

    private static OutboundStatus outboundStatus(final ResourceId resourceId, final StatusCode statusCode,
                                                 final String dateTime) {
        return new OutboundStatus(resourceId, new Status(statusCode, new DateTime(dateTime)));
    }

    private static ru.yandex.market.logistic.gateway.common.model.common.OutboundStatus outboundStatus(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId resourceId,
            ru.yandex.market.logistic.gateway.common.model.common.StatusCode statusCode,
            final String dateTime) {
        return new ru.yandex.market.logistic.gateway.common.model.common.OutboundStatus(resourceId,
                new ru.yandex.market.logistic.gateway.common.model.common.Status(
                        statusCode,
                        new ru.yandex.market.logistic.gateway.common.model.utils.DateTime(dateTime),
                        "")
        );
    }

    private static TransferStatus transferStatus(final ResourceId resourceId,
                                                 final TransferStatusType statusCode,
                                                 final String dateTime) {
        return new TransferStatus(resourceId, new TransferStatusEvent(statusCode, new DateTime(dateTime)));
    }

    private static Status status(final StatusCode statusCode, final String dateTime) {
        return new Status(statusCode, new DateTime(dateTime));
    }

    private static ru.yandex.market.logistic.gateway.common.model.common.Status status(
            ru.yandex.market.logistic.gateway.common.model.common.StatusCode statusCode, String dateTime) {
        return new ru.yandex.market.logistic.gateway.common.model.common.Status(
                statusCode,
                new ru.yandex.market.logistic.gateway.common.model.utils.DateTime(dateTime),
                ""
        );
    }
}
