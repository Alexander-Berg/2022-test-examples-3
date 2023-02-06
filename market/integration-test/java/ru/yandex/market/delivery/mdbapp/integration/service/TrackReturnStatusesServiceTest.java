package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequestDeliveryStatusHistory;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnDeliveryStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestDeliveryStatusHistoryRepository;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.pvz.client.logistics.dto.ReturnDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestsResponseDto;
import ru.yandex.market.pvz.client.logistics.model.ReturnStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

class TrackReturnStatusesServiceTest extends MockContextualTest {

    private static final Long RETURN_ORDER_ID = 1L;
    private static final String RETURN_ID = "123";
    private static final String RETURN_BARCODE = "VOZVRAT_TAR_456";
    private static final Long RETURN_SC_PARTNER_ID = 2L;
    private static final Instant FIXED_TIME = LocalDate.of(2021, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

    @Autowired
    private TrackReturnStatusesService trackReturnStatuses;

    @Autowired
    private PvzLogisticsClient pvzLogisticsClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private ReturnRequestService returnRequestService;

    @Autowired
    private ReturnRequestDeliveryStatusHistoryRepository returnRequestDeliveryStatusHistoryRepository;

    @Autowired
    private CheckouterReturnApi checkouterReturnApi;

    @Autowired
    private TestableClock clock;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    public void setup() {
        clock.setFixed(FIXED_TIME, ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        featureProperties.setEnableFbyReturnTracking(false);
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReceivedFbs() {
        mock(
            ReturnStatus.RECEIVED,
            List.of(),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTest(
            ReturnDeliveryStatus.SENDER_SENT,
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        List<ReturnRequestDeliveryStatusHistory> statusHistories =
            returnRequestDeliveryStatusHistoryRepository.findAll();
        softly.assertThat(statusHistories.size()).isEqualTo(1);
        softly.assertThat(statusHistories.get(0).getReturnId()).isEqualTo(123);
        softly.assertThat(statusHistories.get(0).getDeliveryStatus()).isEqualTo(ReturnDeliveryStatus.SENDER_SENT);
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests-fby.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReceivedFbyEnabled() {
        featureProperties.setEnableFbyReturnTracking(true);
        mock(
            ReturnStatus.RECEIVED,
            List.of(),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTest(
            ReturnDeliveryStatus.SENDER_SENT,
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        List<ReturnRequestDeliveryStatusHistory> statusHistories =
            returnRequestDeliveryStatusHistoryRepository.findAll();
        softly.assertThat(statusHistories.size()).isEqualTo(1);
        softly.assertThat(statusHistories.get(0).getReturnId()).isEqualTo(123);
        softly.assertThat(statusHistories.get(0).getDeliveryStatus()).isEqualTo(ReturnDeliveryStatus.SENDER_SENT);
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests-fby.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReceivedFbyDisabled() {
        featureProperties.setEnableFbyReturnTracking(false);
        mock(
            ReturnStatus.RECEIVED,
            List.of(),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTestNoInteraction();
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReceivedCheckouterError() {
        mock(
            ReturnStatus.RECEIVED,
            List.of(),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );
        when(checkouterReturnApi.changeReturnDeliveryStatus(
            RETURN_ORDER_ID,
            Long.parseLong(RETURN_ID),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT,
            ClientRole.SYSTEM,
            null
        ))
            .thenThrow(new RuntimeException("Error"));

        run();

        verifyTest(
            ReturnDeliveryStatus.SENDER_SENT,
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        List<ReturnRequestDeliveryStatusHistory> statusHistories =
            returnRequestDeliveryStatusHistoryRepository.findAll();
        softly.assertThat(statusHistories.size()).isEqualTo(1);
        softly.assertThat(statusHistories.get(0).getReturnId()).isEqualTo(123);
        softly.assertThat(statusHistories.get(0).getDeliveryStatus()).isEqualTo(ReturnDeliveryStatus.SENDER_SENT);
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests_duplicate.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReceivedDuplicate() {
        mock(
            ReturnStatus.RECEIVED,
            List.of(),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTestNoInteraction();
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReadyToIm() {
        mock(
            ReturnStatus.RECEIVED,
            List.of(
                new OrderStatus(
                    OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM,
                    DateTime.fromOffsetDateTime(FIXED_TIME.atOffset(ZoneOffset.UTC).minusHours(1)),
                    ""
                ),
                new OrderStatus(
                    OrderStatusType.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,
                    DateTime.fromOffsetDateTime(FIXED_TIME.atOffset(ZoneOffset.UTC)),
                    ""
                )
            ),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTest(
            ReturnDeliveryStatus.READY_FOR_PICKUP,
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.READY_FOR_PICKUP
        );
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesReturned() {
        mock(
            ReturnStatus.RECEIVED,
            List.of(
                new OrderStatus(
                    OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM,
                    DateTime.fromOffsetDateTime(FIXED_TIME.atOffset(ZoneOffset.UTC)),
                    ""
                )
            ),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTest(
            ReturnDeliveryStatus.DELIVERED,
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.DELIVERED
        );
    }

    @Test
    @Sql(value = "/data/service/trackReturnStatuses/return-requests.sql")
    @Sql(value = "/data/service/trackReturnStatuses/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    public void trackStatusesNull() {
        mock(
            null,
            List.of(),
            ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT
        );

        run();

        verifyTestNoInteraction();
    }

    private void run() {
        trackReturnStatuses.trackStatuses();
    }

    private void mock(
        ReturnStatus pvzReturnStatus,
        List<OrderStatus> scHistory,
        ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus checkouterReturnStatus
    ) {
        if (pvzReturnStatus != null) {
            when(pvzLogisticsClient.getReturnRequests(List.of(RETURN_ID))).thenReturn(
                ReturnRequestsResponseDto.builder()
                    .returnRequests(List.of(
                        ReturnDto.builder()
                            .returnId(RETURN_ID)
                            .status(pvzReturnStatus)
                            .build()
                    ))
                    .build()
            );
        } else {
            when(pvzLogisticsClient.getReturnRequests(List.of(RETURN_ID))).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND)
            );
        }
        when(fulfillmentClient.getOrderHistory(
            ResourceId.builder()
                .setYandexId(RETURN_BARCODE)
                .setPartnerId(RETURN_BARCODE)
                .build(),
            new Partner(RETURN_SC_PARTNER_ID)
        ))
            .thenReturn(
                new OrderStatusHistory(
                    scHistory,
                    ResourceId.builder().build()
                )
            );
        when(checkouterReturnApi.changeReturnDeliveryStatus(
            RETURN_ORDER_ID,
            Long.parseLong(RETURN_ID),
            checkouterReturnStatus,
            ClientRole.SYSTEM,
            null
        ))
            .thenReturn(new Return());
    }

    private void verifyTest(
        ReturnDeliveryStatus returnDeliveryStatus,
        ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus checkouterStatus
    ) {
        verify(returnRequestService).updateReturnRequestDeliveryStatus(RETURN_ID, returnDeliveryStatus, FIXED_TIME);
        verify(checkouterReturnApi).changeReturnDeliveryStatus(
            RETURN_ORDER_ID,
            Long.parseLong(RETURN_ID),
            checkouterStatus,
            ClientRole.SYSTEM,
            null
        );

    }

    private void verifyTestNoInteraction() {
        verify(returnRequestService, never()).updateReturnRequestDeliveryStatus(anyString(), any(), any());
        verify(checkouterReturnApi, never()).changeReturnDeliveryStatus(anyLong(), anyLong(), any(), any(), anyLong());
    }
}
