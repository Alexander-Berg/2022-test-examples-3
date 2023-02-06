package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.TinkoffDeliveryService;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditServiceImpl;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor.QueuedCallExecution;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WaitingUserDeliveryInputProcessorTest extends AbstractWebTestBase {

    @InjectMocks
    private WaitingUserDeliveryInputProcessor waitingUserDeliveryInputProcessor;
    @Mock
    private OrderEditServiceImpl orderEditService;
    @Mock
    private OrderUpdateService orderUpdateService;
    @Mock
    private OrderService orderService;
    @Mock
    private TinkoffDeliveryService tinkoffDeliveryService;
    @Value("${market.checkouter.QCProcessors.WaitingUserDeliveryInputProcessor.delayQcCallMinutes:1440}")
    private Long delayQcCallMinutes;
    private Order orderMock;
    private Delivery deliveryMock;
    private OrderEditOptions orderEditOptions;
    private DeliveryOption deliveryOption;
    private Set<DeliveryOption> deliveryOptions;

    @BeforeEach
    public void setUp() {
        deliveryOption = new DeliveryOption();
        deliveryOption.setDeliveryServiceId(42L);
        deliveryOption.setFromDate(LocalDate.now(getClock()));
        deliveryOption.setToDate(LocalDate.now(getClock()));
        deliveryOption.setShipmentDate(LocalDate.now(getClock()));
        deliveryOption.setTimeIntervalOptions(Set.of(new TimeInterval()));

        var secondDeliveryOption = new DeliveryOption();
        secondDeliveryOption.setDeliveryServiceId(43L);
        secondDeliveryOption.setFromDate(LocalDate.now(getClock()));
        secondDeliveryOption.setToDate(LocalDate.now(getClock()).plus(1, DAYS));
        secondDeliveryOption.setShipmentDate(LocalDate.now(getClock()));
        secondDeliveryOption.setTimeIntervalOptions(Set.of(new TimeInterval()));

        var thirdDeliveryOption = new DeliveryOption();
        thirdDeliveryOption.setDeliveryServiceId(44L);
        thirdDeliveryOption.setFromDate(LocalDate.now(getClock()).plus(2, DAYS));
        thirdDeliveryOption.setToDate(LocalDate.now(getClock()).plus(4, DAYS));
        thirdDeliveryOption.setShipmentDate(LocalDate.now(getClock()));
        thirdDeliveryOption.setTimeIntervalOptions(Set.of(new TimeInterval()));

        orderEditOptions = new OrderEditOptions();
        deliveryOptions = Set.of(
                secondDeliveryOption,
                thirdDeliveryOption,
                deliveryOption
        );
        orderEditOptions.setDeliveryOptions(deliveryOptions);

        initMocks();
        setFixedTime(getClock().instant());
    }

    @Test
    public void wrongOrderSubStatusTest() {
        when(orderMock.getSubstatus()).thenReturn(OrderSubstatus.WAITING_TINKOFF_DECISION);

        var result = execute(getClock().instant().minus(delayQcCallMinutes, MINUTES));

        assertThat(Objects.requireNonNull(result).isSuccess()).isTrue();

        verify(tinkoffDeliveryService, never()).deliveryOptionIsActual(orderMock);
        verify(orderEditService, never()).getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(),
                anyBoolean(), anyBoolean());
        verify(orderEditService, never()).editOrder(eq(orderMock.getId()), any(), any(), any(), any(), any());
    }

    @Test
    public void wrongOrderPaymentMethodTest() {
        when(orderMock.getPaymentMethod()).thenReturn(PaymentMethod.YANDEX);

        var result = execute(getClock().instant().minus(delayQcCallMinutes, MINUTES));

        assertThat(Objects.requireNonNull(result).isSuccess()).isTrue();

        verify(tinkoffDeliveryService, never()).deliveryOptionIsActual(orderMock);
        verify(orderEditService, never()).getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(),
                anyBoolean(), anyBoolean());
        verify(orderEditService, never()).editOrder(eq(orderMock.getId()), any(), any(), any(), any(), any());
    }

    @Test
    public void deliveryDatesIsActualTest() {
        when(tinkoffDeliveryService.deliveryOptionIsActual(orderMock)).thenReturn(true);

        var result = execute(getClock().instant().minus(delayQcCallMinutes, MINUTES));

        assertThat(Objects.requireNonNull(result).isSuccess()).isTrue();

        verify(tinkoffDeliveryService, times(1)).deliveryOptionIsActual(orderMock);
        verify(orderUpdateService, times(1))
                .updateOrderStatusAuto(orderMock.getId(), orderMock.getStatus(), ClientInfo.SYSTEM);
        verify(orderEditService, never()).getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(),
                anyBoolean(), anyBoolean());
        verify(orderEditService, never()).editOrder(eq(orderMock.getId()), any(), any(), any(), any(), any());
    }


    @Test
    public void noDeliveryOptionsFoundTest() {
        orderEditOptions.setDeliveryOptions(Collections.emptySet());

        var exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> execute(getClock().instant().minus(delayQcCallMinutes, MINUTES))
        );

        assertThat(exception.getMessage()).isEqualTo("No deliveryOption found");
        verify(tinkoffDeliveryService, times(1)).deliveryOptionIsActual(orderMock);
        verify(orderEditService, times(1))
                .getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(), anyBoolean(), anyBoolean());
        verify(orderEditService, never()).editOrder(eq(orderMock.getId()), any(), any(), any(), any(), any());
    }

    @Test
    public void noTimeIntervalsFoundTestSuccess() {
        when(orderMock.getDelivery()).thenReturn(null);
        orderEditOptions.getDeliveryOptions().forEach(option -> option.setTimeIntervalOptions(Collections.emptySet()));

        var result = execute(getClock().instant().minus(delayQcCallMinutes, MINUTES));
        assertThat(Objects.requireNonNull(result).isSuccess()).isTrue();

        verify(tinkoffDeliveryService, times(1)).deliveryOptionIsActual(orderMock);
        verify(orderEditService, times(1))
                .getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(), anyBoolean(), anyBoolean());

        var expectedRgbSet = Set.of(orderMock.getRgb());
        verify(orderEditService, times(1))
                .editOrder(
                        eq(orderMock.getId()),
                        eq(ClientInfo.SYSTEM),
                        eq(expectedRgbSet),
                        any()
                );
    }

    @Test
    public void successTest() {
        var result = execute(getClock().instant().minus(delayQcCallMinutes, MINUTES));
        assertThat(Objects.requireNonNull(result).isSuccess()).isTrue();

        verify(tinkoffDeliveryService, times(1)).deliveryOptionIsActual(orderMock);
        verify(orderEditService, times(1))
                .getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(), anyBoolean(), anyBoolean());

        var expectedOrderEditRequest = new OrderEditRequest();
        expectedOrderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .shipmentDate(deliveryOption.getShipmentDate())
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .deliveryOption(deliveryOption)
                .timeInterval(deliveryOption.getTimeIntervalOptions().iterator().next())
                .build());
        var expectedRgbSet = Set.of(orderMock.getRgb());
        verify(orderEditService, times(1))
                .editOrder(
                        orderMock.getId(),
                        ClientInfo.SYSTEM,
                        expectedRgbSet,
                        expectedOrderEditRequest
                );
    }


    @ParameterizedTest
    @EnumSource(names = {"DELIVERY", "PICKUP", "POST"}, value = DeliveryType.class)
    public void successWithDifferentDeliveryTypesTest(DeliveryType deliveryType) {
        when(deliveryMock.getType()).thenReturn(deliveryType);
        orderEditOptions.getDeliveryOptions().forEach(option -> option.setTimeIntervalOptions(Collections.emptySet()));

        var result = execute(getClock().instant().minus(delayQcCallMinutes, MINUTES));
        assertThat(Objects.requireNonNull(result).isSuccess()).isTrue();

        verify(tinkoffDeliveryService, times(1)).deliveryOptionIsActual(orderMock);
        verify(orderEditService, times(1))
                .getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(), anyBoolean(), anyBoolean());

        var expectedRgbSet = Set.of(orderMock.getRgb());
        verify(orderEditService, times(1))
                .editOrder(
                        eq(orderMock.getId()),
                        eq(ClientInfo.SYSTEM),
                        eq(expectedRgbSet),
                        any()
                );
    }

    @Test
    public void orderStatusDidNotChangeAfterTheDeliveryOptionWasUpdatedTest() {
        when(orderEditService.editOrder(
                eq(orderMock.getId()),
                eq(ClientInfo.SYSTEM),
                any(),
                any()
        )).thenAnswer(invocation -> emptyList());

        var exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> execute(getClock().instant().minus(delayQcCallMinutes, MINUTES))
        );

        assertThat(exception.getMessage()).isEqualTo(
                "The order status didn't change after the delivery option was updated");

        verify(tinkoffDeliveryService, times(1)).deliveryOptionIsActual(orderMock);
        verify(orderEditService, times(1))
                .getOrderEditOptions(eq(orderMock.getId()), any(), any(), any(), anyBoolean(), anyBoolean());

        var expectedOrderEditRequest = new OrderEditRequest();
        expectedOrderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .shipmentDate(deliveryOption.getShipmentDate())
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES)
                .deliveryOption(deliveryOption)
                .timeInterval(deliveryOption.getTimeIntervalOptions().iterator().next())
                .build());
        var expectedRgbSet = Set.of(orderMock.getRgb());
        verify(orderEditService, times(1))
                .editOrder(
                        orderMock.getId(),
                        ClientInfo.SYSTEM,
                        expectedRgbSet,
                        expectedOrderEditRequest
                );
    }


    private void initMocks() {
        deliveryMock = Mockito.mock(Delivery.class);
        when(deliveryMock.getType()).thenReturn(DeliveryType.DELIVERY);

        orderMock = Mockito.mock(Order.class);
        when(orderMock.getId()).thenReturn(1L);
        when(orderMock.getRgb()).thenReturn(Color.BLUE);
        when(orderMock.getPaymentMethod()).thenReturn(PaymentMethod.TINKOFF_CREDIT);
        when(orderMock.getStatus()).thenReturn(OrderStatus.UNPAID);
        when(orderMock.getSubstatus()).thenReturn(OrderSubstatus.WAITING_USER_DELIVERY_INPUT);
        when(orderMock.getDelivery()).thenReturn(deliveryMock);

        when(orderService.getOrder(orderMock.getId())).thenReturn(orderMock);

        when(orderEditService.getOrderEditOptions(
                eq(orderMock.getId()),
                eq(ClientInfo.SYSTEM),
                any(),
                any(OrderEditOptionsRequest.class),
                anyBoolean(), anyBoolean())
        ).thenReturn(orderEditOptions);

        when(orderEditService.editOrder(
                eq(orderMock.getId()),
                eq(ClientInfo.SYSTEM),
                any(),
                any()
        )).thenAnswer(invocation -> {
                    when(orderMock.getStatus()).thenReturn(OrderStatus.PROCESSING);
                    when(orderMock.getSubstatus()).thenReturn(OrderSubstatus.STARTED);
                    return emptyList();
                }
        );

    }

    private ExecutionResult execute(Instant instant) {
        return waitingUserDeliveryInputProcessor.process(
                new QueuedCallExecution(
                        orderMock.getId(),
                        null,
                        0,
                        instant,
                        orderMock.getId()
                )
        );
    }
}
