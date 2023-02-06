package ru.yandex.market.crm.operatorwindow;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.crm.operatorwindow.jmf.entity.FraudConfirmationTicket;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.utils.DomainException;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CloseFraudOrderOnFraudTicketClosingTest extends AbstractModuleOwTest {

    private static final Long ORDER_NUMBER = 123L;
    private static final boolean SHOULD_CALL_CANCELLATION = true;
    private static final boolean SHOULD_NOT_CALL_CANCELLATION = false;

    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private CheckouterAPI checkouterAPI;
    @Inject
    private TriggerServiceImpl triggerService;

    private static Stream<Arguments> fraudOrderCancellationCheckData() {
        return Stream.of(
                Arguments.of(getPendingCheckouterOrder(), SHOULD_CALL_CANCELLATION, "Pending"),
                Arguments.of(getCancelRequestedCheckouterOrder(), SHOULD_NOT_CALL_CANCELLATION, "CancelRequested"),
                Arguments.of(getCanceledCheckouterOrder(), SHOULD_NOT_CALL_CANCELLATION, "Cancelled"));
    }

    private static Order getPendingCheckouterOrder() {
        Order mockOrder = mock(Order.class);
        when(mockOrder.getId()).thenReturn(ORDER_NUMBER);
        when(mockOrder.getStatus()).thenReturn(OrderStatus.PENDING);
        when(mockOrder.getSubstatus()).thenReturn(OrderSubstatus.ANTIFRAUD);
        return mockOrder;
    }

    private static Order getCancelRequestedCheckouterOrder() {
        Order mockOrder = getPendingCheckouterOrder();
        when(mockOrder.getCancellationRequest()).thenReturn(mock(CancellationRequest.class));
        return mockOrder;
    }

    private static Order getCanceledCheckouterOrder() {
        Order mockOrder = getPendingCheckouterOrder();
        when(mockOrder.getStatus()).thenReturn(OrderStatus.CANCELLED);
        return mockOrder;
    }

    @BeforeEach
    public void setUp() {
        reset(checkouterAPI);
    }

    @AfterEach
    private void tearDown() {
        reset(checkouterAPI);
    }

    /**
     * Должны получить {@link DomainException} если запрошенный заказ не найден на стороне чекаутера
     */
    @Test
    @Transactional
    public void shouldThrowDomainExceptionWhenOrderIsNotFound() {
        Ticket ticket = getOwFraudTicket();

        Mockito.doThrow(OrderNotFoundException.class).when(checkouterAPI).getOrder(any(), any());

        Assertions.assertThrows(DomainException.class, () ->
                bcpService.edit(ticket, FraudConfirmationTicket.STATUS, FraudConfirmationTicket.STATUS_RESOLVED));
    }

    /**
     * <ol>
     *  <li>При закрытии обращения с ожидающим подтверждения фрод-заказом, чекаутер должен получить запрос на
     *      отмену заказа</li>
     *  <li>Если чекаутер получал запрос на отмену заказа ранее, но еще не успел отменить заказ, мы не должны
     *      отправить повторный запрос</li>
     *  <li>Запрос на отмену не должен быть отправлен чекаутеру, если заказ уже отменен</li>
     * </ol>
     */
    @ParameterizedTest(name = "order type = {2}, should cancel = {1}")
    @MethodSource(value = "fraudOrderCancellationCheckData")
    @Transactional
    public void fraudOrderCancellationCheck(Order checkouterOrder, boolean shouldCancel, String orderType) {

        Ticket ticket = getOwFraudTicket();

        when(checkouterAPI.getOrder(any(), any())).thenReturn(checkouterOrder);

        bcpService.edit(ticket, FraudConfirmationTicket.STATUS, FraudConfirmationTicket.STATUS_RESOLVED);

        verify(checkouterAPI, times(shouldCancel ? 1 : 0))
                .createCancellationRequest(eq(ORDER_NUMBER), any(CompatibleCancellationRequest.class),
                        any(ClientRole.class), anyLong());
    }

    private Ticket getOwFraudTicket() {
        return ticketTestUtils.createTicket(FraudConfirmationTicket.FQN, Map.of(
                FraudConfirmationTicket.STATUS, FraudConfirmationTicket.STATUS_PROCESSING,
                FraudConfirmationTicket.ORDER, getOwFraudOrder(),
                FraudConfirmationTicket.SERVICE, Constants.Service.BERU_FRAUD_CONFIRMATION
        ));
    }

    private ru.yandex.market.ocrm.module.order.domain.Order getOwFraudOrder() {
        return orderTestUtils.createOrder(Map.of(
                ru.yandex.market.ocrm.module.order.domain.Order.NUMBER, ORDER_NUMBER,
                ru.yandex.market.ocrm.module.order.domain.Order.SUB_STATUS, OrderSubstatus.ANTIFRAUD.name()));
    }

}
