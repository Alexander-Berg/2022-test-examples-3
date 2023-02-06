package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

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
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus.APPLIED;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

@RunWith(Parameterized.class)
public class DeliveryDateChangeRequestNegativeTest extends AllMockContextualTest {

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
    private CheckouterAPI checkouterAPI;

    @Autowired
    private DeliveryClient deliveryClient;

    @Parameterized.Parameter()
    public String responseFilePath;

    private MockRestServiceServer checkouterMockServer;

    @Before
    public void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    @Test
    public void testBlackListedDeliveryService() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/delivery_date_change_request_created_black_list_ds_event.json"
        );

        prepareMockServer(
            checkouterMockServer,
            "/change-requests"
        );

        poller.poll();

        checkouterMockServer.verify();
        verify(failoverService, never()).storeError(any(), anyString(), any());

        ArgumentCaptor<ChangeRequestPatchRequest> captor = ArgumentCaptor.forClass(ChangeRequestPatchRequest.class);
        verify(checkouterAPI).updateChangeRequestStatus(
            eq(ORDER_ID),
            eq(CHANGE_REQUEST_ID),
            any(),
            any(),
            captor.capture()
        );

        softly.assertThat(captor.getValue())
            .extracting(ChangeRequestPatchRequest::getStatus)
            .isEqualTo(APPLIED);
    }

    @Test
    public void testAppliedChangeRequestNotProcessed() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            responseFilePath
        );

        poller.poll();

        checkouterMockServer.verify();
        verify(failoverService, never()).storeError(any(), anyString(), any());

        verify(deliveryClient, never()).updateOrderDeliveryDate(
            any(OrderDeliveryDate.class),
            any(Partner.class),
            anyLong()
        );

        verify(checkouterAPI, never()).updateChangeRequestStatus(
            eq(ORDER_ID),
            eq(CHANGE_REQUEST_ID),
            any(),
            any(),
            any(ChangeRequestPatchRequest.class)
        );
    }

    @Nonnull
    @Parameterized.Parameters(name = "{index}: eventFilePath={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {
                "/data/events/delivery_date_change_request_created_applied_status_event.json"
            },
            {
                "/data/events/delivery_date_change_request_created_clarified_by_delivery_event.json"
            },
        });
    }
}
