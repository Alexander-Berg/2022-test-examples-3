package ru.yandex.market.checkout.checkouter.order.status.actions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TinkoffCourierFlowStartedStatusActionTest extends AbstractWebTestBase {

    @Autowired
    private TinkoffCourierFlowStartedStatusAction tinkoffCourierFlowStartedStatusAction;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Value("${market.checkouter.credit.flow.CreateWaitingBankDecisionQcCallAction.qcCallExecutionDelayInMinutes:1440}")
    private long qcCallExecutionDelayInMinutes = -1;

    private static Arguments createTestArguments(
            String name,
            PaymentMethod paymentMethod,
            OrderSubstatus toSubStatus,
            boolean shouldRun
    ) {
        var order = mock(Order.class);
        when(order.getId()).thenReturn(System.nanoTime());
        when(order.getPaymentMethod()).thenReturn(paymentMethod);
        when(order.getStatus()).thenReturn(toSubStatus.getStatus());
        when(order.getPaymentId()).thenReturn(42L);

        var statusTransition = mock(StatusTransition.class);
        when(statusTransition.getTo()).thenReturn(toSubStatus.getStatus());
        when(statusTransition.getSubTo()).thenReturn(toSubStatus);

        return Arguments.of(
                name,
                order,
                statusTransition,
                shouldRun
        );
    }

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                createTestArguments(
                        "TINKOFF_CREDIT, WAITING_USER_DELIVERY_INPUT -> false",
                        PaymentMethod.TINKOFF_CREDIT,
                        OrderSubstatus.WAITING_USER_DELIVERY_INPUT,
                        false
                ),
                createTestArguments(
                        "TINKOFF_CREDIT, WAITING_TINKOFF_DECISION -> true",
                        PaymentMethod.TINKOFF_CREDIT,
                        OrderSubstatus.WAITING_TINKOFF_DECISION,
                        true
                ),
                createTestArguments(
                        "YANDEX, WAITING_USER_DELIVERY_INPUT -> false",
                        PaymentMethod.YANDEX,
                        OrderSubstatus.WAITING_USER_DELIVERY_INPUT,
                        false
                ),
                createTestArguments(
                        "TINKOFF_CREDIT, WAITING_USER_DELIVERY_INPUT -> false",
                        PaymentMethod.YANDEX,
                        OrderSubstatus.WAITING_TINKOFF_DECISION,
                        false
                )
        );
    }

    @BeforeEach
    public void setUp() {
        assertThat(qcCallExecutionDelayInMinutes).isGreaterThan(0L);
        setFixedTime(Instant.now(getClock()));
        OrderUpdateService orderUpdateService = mock(OrderUpdateService.class);
        tinkoffCourierFlowStartedStatusAction.setClock(getClock());
        tinkoffCourierFlowStartedStatusAction.setOrderUpdateService(orderUpdateService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void parameterizedTestData(
            String name,
            Order order,
            StatusTransition statusTransition,
            boolean shouldRun
    ) {
        var clientInfo = mock(ClientInfo.class);
        assertThat(tinkoffCourierFlowStartedStatusAction.shouldRun(order, statusTransition, clientInfo))
                .isEqualTo(shouldRun);

        if (shouldRun) {
            transactionTemplate.execute(status -> {
                tinkoffCourierFlowStartedStatusAction.process(order, statusTransition, clientInfo);
                var queuedCalls = queuedCallService.findQueuedCalls(
                        CheckouterQCType.CHECK_WAITING_BANK_DECISION_PAYMENT_STATUS,
                        order.getPaymentId());
                assertThat(queuedCalls.size()).isEqualTo(1);
                var queuedCall = queuedCalls.stream().findFirst()
                        .orElseThrow(IllegalStateException::new);
                assertThat(queuedCall.getNextTryAt()).isEqualTo(Instant.now(getClock())
                        .plus(qcCallExecutionDelayInMinutes, ChronoUnit.MINUTES));
                return null;
            });
        }
    }
}
