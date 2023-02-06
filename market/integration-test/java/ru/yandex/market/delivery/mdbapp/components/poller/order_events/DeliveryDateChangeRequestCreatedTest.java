package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PossibleOrderChangeRepository;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderDeliveryDateRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus.PROCESSING;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

@RunWith(Parameterized.class)
public class DeliveryDateChangeRequestCreatedTest extends AllMockContextualTest {

    private static final Long ORDER_ID = 2106843L;
    private static final Long CHANGE_REQUEST_ID = 44292L;

    @Autowired
    @Qualifier("orderEventsPoller0")
    private OrderEventsPoller poller;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private OrderEventFailoverableService failoverService;

    @Autowired
    private PossibleOrderChangeRepository possibleOrderChangeRepository;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LomClient lomClient;

    @Parameterized.Parameter(0)
    public String eventFilePath;

    @Parameterized.Parameter(1)
    public ChangeOrderRequestReason reason;

    private MockRestServiceServer checkouterMockServer;

    @Before
    public void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    @Test
    public void testDeliveryDateChangeRequestShippingDelayedLOM() throws Exception {
        when(possibleOrderChangeRepository.existsByPartnerIdAndTypeAndEnabledTrue(any(), any())).thenReturn(true);

        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/" + eventFilePath
        );

        prepareMockServer(
            checkouterMockServer,
            "/change-requests"
        );

        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        poller.poll();

        countDownLatch.await(2, TimeUnit.SECONDS);
        checkouterMockServer.verify();
        verify(failoverService, never()).storeError(any(), anyString(), any());

        ArgumentCaptor<UpdateOrderDeliveryDateRequestDto> requestCaptor =
            ArgumentCaptor.forClass(UpdateOrderDeliveryDateRequestDto.class);
        verify(lomClient).updateOrderDeliveryDate(
            requestCaptor.capture()
        );

        softly.assertThat(requestCaptor.getValue()).isEqualTo(getUpdateOrderDeliveryDateRequestDto(reason));

        ArgumentCaptor<ChangeRequestPatchRequest> patchRequestArgumentCaptor =
            ArgumentCaptor.forClass(ChangeRequestPatchRequest.class);
        verify(checkouterAPI).updateChangeRequestStatus(eq(ORDER_ID), eq(CHANGE_REQUEST_ID), any(), any(),
            patchRequestArgumentCaptor.capture()
        );

        softly.assertThat(patchRequestArgumentCaptor.getValue().getStatus())
            .isEqualTo(PROCESSING);
    }

    @Parameterized.Parameters(name = "{index}: eventFilePath={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {
                "delivery_date_change_request_created_shipping_delayed_event.json",
                ChangeOrderRequestReason.SHIPPING_DELAYED
            },
            {
                "delivery_date_change_request_created_call_center_event.json",
                ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_USER
            },
            {
                "delivery_date_change_request_created_user_event.json",
                ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_USER
            },
        });
    }

    private UpdateOrderDeliveryDateRequestDto getUpdateOrderDeliveryDateRequestDto(
        ChangeOrderRequestReason reason
    ) {
        return UpdateOrderDeliveryDateRequestDto.builder()
            .barcode(String.valueOf(ORDER_ID))
            .dateMin(LocalDate.of(2019, 8, 28))
            .dateMax(LocalDate.of(2019, 8, 28))
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(18, 0))
            .reason(reason)
            .changeRequestExternalId(CHANGE_REQUEST_ID)
            .build();
    }
}
