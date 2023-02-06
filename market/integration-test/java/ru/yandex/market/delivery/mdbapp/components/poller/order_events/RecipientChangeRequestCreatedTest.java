package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

public class RecipientChangeRequestCreatedTest extends AllMockContextualTest {

    @Autowired
    @Qualifier("orderEventsPoller48")
    private OrderEventsPoller poller;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private OrderEventFailoverableService failoverService;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private CheckouterAPI checkouterAPI;
    private MockRestServiceServer checkouterMockServer;

    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
        countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
    }

    @Test
    public void testRecipientChangeRequest() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/delivery_recipient_change_request_created_shipping_delayed_event.json"
        );

        prepareMockServer(
            checkouterMockServer,
            "/change-requests"
        );

        poller.poll();

        countDownLatch.await(2, TimeUnit.SECONDS);
        verify(failoverService, never()).storeError(any(), anyString(), any());

        verify(lomClient).updateOrderRecipient(createUpdateRecipientRequest());

        checkouterMockServer.verify();
        verify(checkouterAPI).updateChangeRequestStatus(anyLong(), anyLong(), any(), isNull(), any());
    }

    private UpdateOrderRecipientRequestDto createUpdateRecipientRequest() {
        return UpdateOrderRecipientRequestDto.builder()
            .barcode("2106833")
            .contact(
                OrderContactDto.builder()
                    .contactType(ContactType.RECIPIENT)
                    .lastName("Иванов")
                    .firstName("Иван")
                    .middleName("Иванович")
                    .phone("79234563434")
                    .personalPhoneId("mdb-mock-personal-phone-id")
                    .personalFullnameId("mdb-mock-personal-fullname-id")
                    .build()
            )
            .email("mail@mail.ru")
            .personalEmailId("mdb-mock-personal-email-id")
            .checkouterRequestId(44292L)
            .build();
    }

    @Test
    public void testRecipientChangeRequestNoTrackCode() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/delivery_recipient_change_request_applied_and_no_trackcode_event.json"
        );

        poller.poll();

        countDownLatch.await(2, TimeUnit.SECONDS);
        verify(failoverService, never()).storeError(any(), anyString(), any());

        checkouterMockServer.verify();
    }
}
