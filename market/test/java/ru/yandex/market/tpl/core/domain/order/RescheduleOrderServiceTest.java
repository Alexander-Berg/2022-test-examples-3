package ru.yandex.market.tpl.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor
class RescheduleOrderServiceTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TestDataFactory testDataFactory;
    private final RescheduleOrderService rescheduleOrderService;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;

    private final Clock clock;

    private Order pickupOrder;
    private Order clientOrder;

    @BeforeEach
    void setUp() {

        ClockUtil.initFixed(clock);

        pickupOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                        .pickupPoint(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L))
                        .build());

        clientOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build());

    }

    @Test
    void rescheduleTest() {
        var orderId = transactionTemplate.execute(s -> {
            var order = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryServiceId(DELIVERY_SERVICE_ID)
                            .deliveryInterval(DateTimeUtil.PICKUP_INTERVAL)
                            .deliveryDate(LocalDate.now())
                            .flowStatus(OrderFlowStatus.TRANSMITTED_TO_RECIPIENT_AND_NOT_PACK_RETURN_BOXES)
                            .build());

            var interval = new Interval(
                    order.getDelivery().getDeliveryIntervalFrom().plus(1, ChronoUnit.DAYS),
                    order.getDelivery().getDeliveryIntervalTo().plus(1, ChronoUnit.DAYS)
            );
            rescheduleOrderService.rescheduleOrder(order, interval, OrderDeliveryRescheduleReasonType.OTHER, Source.CRM_OPERATOR);
            return order.getId();
        });
        var o = orderRepository.findByIdOrThrow(orderId);
        assertThat(o.getOrderFlowStatus()).isNotNull();
    }

    @DisplayName("Перенос даты заказа с самовывозом из постамата")
    @Test
    void shouldReschedulePickupOrder() {
        //given
        var now = LocalDateTime.now(clock);

        LocalDate tomorrow = now.toLocalDate().plusDays(1);

        //when
        Interval updatedInterval = rescheduleOrderService.getAndValidateDeliveryInterval(pickupOrder,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0), tomorrow);

        //then
        assertThat(updatedInterval)
                .extracting(di -> di.toLocalTimeInterval(DateTimeUtil.DEFAULT_ZONE_ID))
                .isEqualTo(PartnerSubType.LOCKER.getTimeInterval());
    }

    @DisplayName("Валидность переноса без изменения интервала")
    @Test
    void getAndValidateDeliveryInterval() {
        var now = LocalDateTime.now(clock);

        assertThrows(TplInvalidParameterException.class,
                () -> rescheduleOrderService.getAndValidateDeliveryInterval(clientOrder,
                        now.toLocalTime().plusHours(1),
                        now.toLocalTime().plusHours(3), now.toLocalDate().minusDays(1)));

        assertThrows(TplInvalidParameterException.class,
                () -> rescheduleOrderService.getAndValidateDeliveryInterval(clientOrder,
                        now.toLocalTime(),
                        now.toLocalTime().plusHours(1), now.toLocalDate()));

        assertThat(rescheduleOrderService.getAndValidateDeliveryInterval(clientOrder, now.toLocalTime().plusHours(1),
                now.toLocalTime().plusHours(3), LocalDate.now(clock)))
                .extracting(e -> e.getEnd()).isEqualTo(now.plusHours(3)
                .toInstant(DateTimeUtil.DEFAULT_ZONE_ID));

        assertThat(rescheduleOrderService.getAndValidateDeliveryInterval(clientOrder, LocalTime.now(clock),
                now.toLocalTime().plusHours(1), now.toLocalDate().plusDays(1)))
                .extracting(e -> e.getEnd()).isEqualTo(now.plusHours(1).plusDays(1)
                .toInstant(DateTimeUtil.DEFAULT_ZONE_ID));
    }
}
