package ru.yandex.market.pers.pay.tms.monitoring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.service.PaymentService;
import ru.yandex.market.pers.pay.service.TmsPaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CheckPaymentContentStatesExecutorTest extends PersPayTest {
    private static final long MODEL_ID = 12345;
    private static final long UID = 68934789;

    @Autowired
    private CheckPaymentContentStatesExecutor executor;

    @Autowired
    protected PaymentService paymentService;

    @Autowired
    private TmsPaymentService tmsPaymentService;

    @Autowired
    private ComplexMonitoring complicatedMonitoring;

    @Test
    public void testFireWithNewState() throws Exception {
        long payId = createTestPayment(MODEL_ID, UID);
        tmsPaymentService.changeState(payId, PersPayState.NEW, "Test");
        shiftUpdTimeIntoThePast(payId,23, "hour");
        executor.checkPaymentContentNonTerminalStates();

        shiftUpdTimeIntoThePast(payId,24, "hour");
        executor.checkPaymentContentNonTerminalStates();

        ArgumentCaptor<String> monitoringMessageArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(complicatedMonitoring, times(1))
            .addTemporaryWarning(any(), monitoringMessageArgumentCaptor.capture(), anyLong(), any());
        List<String> messages = monitoringMessageArgumentCaptor.getAllValues();
        assertEquals(1, messages.size());
        assertEquals("Stuck payments state=count: {NEW=1}", messages.get(0));
    }

    @Test
    public void testFireWithWaitModerationState() throws Exception {
        long payId = createTestPayment(MODEL_ID, UID);
        tmsPaymentService.changeState(payId, PersPayState.WAIT_MODERATION, "Test");
        shiftUpdTimeIntoThePast(payId, 6, "day");
        executor.checkPaymentContentNonTerminalStates();

        shiftUpdTimeIntoThePast(payId, 8, "day");
        executor.checkPaymentContentNonTerminalStates();

        ArgumentCaptor<String> monitoringMessageArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(complicatedMonitoring, times(1))
            .addTemporaryWarning(any(), monitoringMessageArgumentCaptor.capture(), anyLong(), any());
        List<String> messages = monitoringMessageArgumentCaptor.getAllValues();
        assertEquals(1, messages.size());
        assertEquals("Stuck payments state=count: {WAIT_MODERATION=1}", messages.get(0));
    }

    @Test
    public void testFireWithWaitModerationRetryState() throws Exception {
        long payId = createTestPayment(MODEL_ID, UID);
        tmsPaymentService.changeState(payId, PersPayState.WAIT_MODERATION_RETRY, "Test");
        shiftUpdTimeIntoThePast(payId, TmsPaymentService.PAYMENT_RETRY_EXPIRE_HOURS, "hour");
        executor.checkPaymentContentNonTerminalStates();

        shiftUpdTimeIntoThePast(payId, TmsPaymentService.PAYMENT_RETRY_EXPIRE_HOURS + 2, "hour");
        executor.checkPaymentContentNonTerminalStates();

        ArgumentCaptor<String> monitoringMessageArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(complicatedMonitoring, times(1))
            .addTemporaryWarning(any(), monitoringMessageArgumentCaptor.capture(), anyLong(), any());
        List<String> messages = monitoringMessageArgumentCaptor.getAllValues();
        assertEquals(1, messages.size());
        assertEquals("Stuck payments state=count: {WAIT_MODERATION_RETRY=1}", messages.get(0));
    }

    @Test
    public void testFireWithTwoProblemStates() throws Exception {
        long payId = createTestPayment(MODEL_ID, UID);
        tmsPaymentService.changeState(payId, PersPayState.WAIT_CONTENT, "Test");
        shiftUpdTimeIntoThePast(payId,8, "day");

        payId = createTestPayment(MODEL_ID + 1, UID + 1);
        tmsPaymentService.changeState(payId, PersPayState.WAIT_MODERATION, "Test");
        shiftUpdTimeIntoThePast(payId, 8, "day");

        executor.checkPaymentContentNonTerminalStates();

        ArgumentCaptor<String> monitoringMessageArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(complicatedMonitoring, times(1))
            .addTemporaryWarning(any(), monitoringMessageArgumentCaptor.capture(), anyLong(), any());
        List<String> messages = monitoringMessageArgumentCaptor.getAllValues();
        assertEquals(1, messages.size());
        assertEquals("Stuck payments state=count: {WAIT_CONTENT=1, WAIT_MODERATION=1}", messages.get(0));
    }

    private long createTestPayment(long modelId, long userId) {
        return paymentService.savePaymentForTests(PersPayment.builderModelGradeUid(modelId, userId)
            .payer(PersPayerType.MARKET, "test")
            .amount(10));
    }

    private void shiftUpdTimeIntoThePast(long payId, int time, String timeUnit) {
        String query = String.format("update pay.payment set upd_time = upd_time - interval '%d' %s where id = ?",
            time, timeUnit);
        jdbcTemplate.update(query, payId);
    }
}
