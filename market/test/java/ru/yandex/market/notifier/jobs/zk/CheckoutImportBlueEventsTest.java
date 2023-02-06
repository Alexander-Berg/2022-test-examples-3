package ru.yandex.market.notifier.jobs.zk;

import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateCashReceiptPrintedEvent;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateReturnRefund;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateSubstatusUpdatedWaitingUserDeliveryInput;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateSubstusUpdatedWaitngBankDecision;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateTrackCheckpointChangedEvent;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateUnpaidEventForCredit;
import static ru.yandex.market.notifier.jobs.zk.CheckoutImportWorkerJobStubTest.generateUnpaidEventForTinkoffCredit;

/**
 * @author Nikolai Iusiumbeli
 * date: 25/01/2018
 */
public class CheckoutImportBlueEventsTest extends AbstractServicesTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;
    @Mock
    private CheckouterReturnClient checkouterReturnClient;

    @Test
    public void testEventsCreateOnTracksChange() {
        OrderHistoryEvent event = generateTrackCheckpointChangedEvent(new ClientInfo(ClientRole.SYSTEM, 0L),
                OrderStatus.DELIVERY, 1,
                BlueEventProcessor.DELIVERY_TRANSPORTATION_RECIPIENT_CHECKPOINT, BlueEventProcessor
                        .DELIVERY_ATTEMPT_FAIL_CHECKPOINT);
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(0);
    }

    @Test
    public void testEventsCreateOnCastRefundReceiptPrinted() {
        OrderHistoryEvent event = generateCashReceiptPrintedEvent();
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);
    }

    @Test
    public void testUnpaidCreditSubstatusUpdateEvent() {
        OrderHistoryEvent event = generateSubstusUpdatedWaitngBankDecision();
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);
    }

    @Test
    public void testSkipUnpaidEventForCredit() {
        OrderHistoryEvent event = generateUnpaidEventForCredit();
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(0);
    }

    @Test
    public void testEventsReturnRefund() {
        OrderHistoryEvent event = generateReturnRefund();
        event.setHistoryId(123L);
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());

        when(checkouterClient.returns()).thenReturn(checkouterReturnClient);
        Return returnValue = new Return();
        returnValue.setStatus(ReturnStatus.REFUNDED);
        when(checkouterReturnClient.getReturn(
                MockitoHamcrest.argThat(
                        Matchers.allOf(
                                Matchers.hasProperty("clientRole", Matchers.is(ClientRole.SYSTEM)),
                                Matchers.hasProperty("clientId", Matchers.is(1L))
                        )
                ),
                MockitoHamcrest.argThat(
                        Matchers.allOf(
                                Matchers.hasProperty("returnId", Matchers.is(1234L)),
                                Matchers.hasProperty("includeBankDetails", Matchers.is(false))
                        )
                )
        ))
                .thenReturn(returnValue);

        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);
    }

    @Test
    public void testUnpaidWaitingUserDeliveryInputUpdateEvent() throws PersNotifyClientException {
        OrderHistoryEvent event = generateSubstatusUpdatedWaitingUserDeliveryInput();
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);
        eventTestUtils.assertEmailWasSent(event);
        List<NotificationEventSource> emails = eventTestUtils.getSentEmails();
        assertThat(emails).hasSize(1);
        NotificationEventSource email = emails.get(0);
        assertThat(email.getNotificationSubtype())
                .isEqualTo(NotificationSubtype.ORDER_UNPAID_WAITING_USER_DELIVERY_INPUT);
        List<NotificationEventSource> pushList = eventTestUtils.getSentPushes();
        assertThat(pushList).hasSize(1);
        NotificationEventSource push = pushList.get(0);
        assertThat(push.getNotificationSubtype())
                .isEqualTo(NotificationSubtype.PUSH_STORE_UNPAID_WAITING_USER_DELIVERY_INPUT);
        assertThat(push.getData().get("push_data_store_push_deeplink_v1")).matches("yamarket://my/orders/\\d+");
    }

    @Test
    public void testSkipUnpaidEventForTinkoffCredit() {
        OrderHistoryEvent event = generateUnpaidEventForTinkoffCredit();
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(0);
    }

    @Test
    @DisplayName("Не отправляем нификации по событиям для бизнесовых заказов")
    public void testDoNotSendNotificationForBusinessOrderEvent() {
        OrderHistoryEvent event = generateSubstatusUpdatedWaitingUserDeliveryInput();
        event.getOrderAfter().getBuyer().setBusinessBalanceId(123L);
        event.getOrderBefore().getBuyer().setBusinessBalanceId(123L);
        eventTestUtils.mockEvent(event);

        eventTestUtils.assertHasNewNotifications(0);
    }

    @Test
    @DisplayName("Отправляем нификации по событиям для небизнесовых заказов")
    public void testSendNotificationForNotBusinessOrderEvent() throws PersNotifyClientException {
        OrderHistoryEvent event = generateSubstatusUpdatedWaitingUserDeliveryInput();
        event.getOrderAfter().getBuyer().setBusinessBalanceId(null);
        event.getOrderBefore().getBuyer().setBusinessBalanceId(null);

        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(1);
        eventTestUtils.assertEmailWasSent(event);
        List<NotificationEventSource> emails = eventTestUtils.getSentEmails();
        assertThat(emails).hasSize(1);
        List<NotificationEventSource> pushList = eventTestUtils.getSentPushes();
        assertThat(pushList).hasSize(1);
    }
}
