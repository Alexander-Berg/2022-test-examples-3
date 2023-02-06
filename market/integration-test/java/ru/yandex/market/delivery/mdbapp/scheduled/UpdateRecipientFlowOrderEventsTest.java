package ru.yandex.market.delivery.mdbapp.scheduled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.Mockito.verify;
import static steps.orderSteps.OrderEventSteps.getOrderHistoryEvent;
import static steps.orderSteps.OrderSteps.getRedMultipleOrderWithOneParcelWithoutTracks;

public class UpdateRecipientFlowOrderEventsTest extends MockContextualTest {

    @Autowired
    private OrderEventsGateway gateway;

    @Autowired
    private TaskLifecycleListener taskListener;

    private CountDownLatch countDownLatch;

    @Autowired
    private LomClient lomClient;

    @Before
    public void setUp() {
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        countDownLatch = new CountDownLatch(1);
        mockedTaskListener.setFinishedLatch(countDownLatch);
    }

    @Test
    public void testUpdateRecipientSucceeded() throws Exception {
        gateway.processEvent(getOrderHistoryEventMultiParcel());
        countDownLatch.await(2, TimeUnit.SECONDS);

        verify(lomClient).updateOrderRecipient(
            UpdateOrderRecipientRequestDto.builder()
                .barcode("123")
                .contact(
                    OrderContactDto.builder()
                        .contactType(ContactType.RECIPIENT)
                        .lastName("RecipientLastName")
                        .firstName("RecipientFirstName")
                        .middleName("RecipientMiddleName")
                        .phone("71234567891")
                        .personalPhoneId("mdb-mock-personal-phone-id")
                        .personalFullnameId("mdb-mock-personal-fullname-id")
                        .comment(null)
                        .build()
                )
                .email("test-recipient@test.com")
                .personalEmailId("mdb-mock-personal-email-id")
                .checkouterRequestId(-1L)
                .build()
        );
    }

    private OrderHistoryEvent getOrderHistoryEventMultiParcel() {
        Order redOrderBefore = getRedMultipleOrderWithOneParcelWithoutTracks();
        Order redOrderAfter = getRedMultipleOrderWithOneParcelWithoutTracks();
        redOrderBefore.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        redOrderAfter.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);

        OrderHistoryEvent event = getOrderHistoryEvent();
        event.setOrderBefore(redOrderBefore);
        event.setOrderAfter(redOrderAfter);
        event.setType(HistoryEventType.ORDER_BUYER_UPDATED);

        return event;
    }
}
