package ru.yandex.market.tpl.core.domain.reschedule.dbqueue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import yandex.market.combinator.v0.CombinatorGrpc;
import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.holiday.PickupPointHoliday;
import ru.yandex.market.tpl.core.domain.pickup.holiday.PickupPointHolidayRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.DEFAULT_DS_ID;

@RequiredArgsConstructor
public class RescheduleOrderProcessingServiceTest extends TplAbstractTest {

    private final RescheduleOrderProcessingService rescheduleOrderProcessingService;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterService sortingCenterService;
    private final CombinatorGrpc.CombinatorBlockingStub combinatorBlockingStub;
    private final TestDataFactory testDataFactory;
    private final PickupPointHolidayRepository pickupPointHolidayRepository;
    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    private Order order;

    @BeforeEach
    public void setUp() {
        LocalDate now = LocalDate.now(clock);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .deliveryDate(now)
                .deliveryServiceId(DEFAULT_DS_ID)
                .build());
        SortingCenter sortingCenter = sortingCenterService.findSortCenterForDs(DEFAULT_DS_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED,
                false
        );
    }

    @Test
    public void orderShouldRescheduleAndSaveHistory() {

        RescheduleOrderPayload payload = new RescheduleOrderPayload(
                "requestId",
                order.getId(),
                Instant.now(clock),
                LocalDate.now(clock).plusDays(1)
        );

        rescheduleOrderProcessingService.processPayload(payload);

        Order updated = orderRepository.findByIdOrThrow(order.getId());

        Assertions.assertEquals(updated.getDelivery().getDeliveryDateAtDefaultTimeZone(),
                LocalDate.now(clock).plusDays(1));

        List<OrderFlowStatusHistory> history = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        Assertions.assertEquals(history.size(), 3);
        Assertions.assertEquals(history.get(0).getOrderFlowStatusAfter(),
                OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP);
    }

    @Test
    public void orderShouldRescheduleAndSaveHistoryDateFromCombinator() {
        SortingCenter sortingCenter = sortingCenterService.findSortCenterForDs(DEFAULT_DS_ID);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter,
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED,
                true
        );

        LocalDate afterTomorrow = LocalDate.now(clock).plusDays(2);
        LocalTimeInterval interval = LocalTimeInterval.valueOf("10:00-18:00");

        CombinatorOuterClass.PostponeDeliveryResponse response =
                CombinatorOuterClass.PostponeDeliveryResponse.newBuilder()
                        .addAllOptions(List.of(
                                CombinatorOuterClass.DeliveryOption.newBuilder()
                                        .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                                .setYear(afterTomorrow.getYear())
                                                .setMonth(afterTomorrow.getMonthValue())
                                                .setDay(afterTomorrow.getDayOfMonth())
                                                .build())
                                        .setInterval(CombinatorOuterClass.DeliveryInterval.newBuilder()
                                                .setFrom(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval.getStart().getHour())
                                                        .setMinute(interval.getStart().getMinute())
                                                        .build())
                                                .setTo(CombinatorOuterClass.Time.newBuilder()
                                                        .setHour(interval.getEnd().getHour())
                                                        .setMinute(interval.getEnd().getMinute())
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .build();
        Mockito.when(combinatorBlockingStub.postponeDelivery(any())).thenReturn(response);

        RescheduleOrderPayload payload = new RescheduleOrderPayload(
                "requestId",
                order.getId(),
                Instant.now(clock),
                LocalDate.now(clock)
        );

        rescheduleOrderProcessingService.processPayload(payload);

        Order updated = orderRepository.findByIdOrThrow(order.getId());

        Assertions.assertEquals(updated.getDelivery().getDeliveryDateAtDefaultTimeZone(), afterTomorrow);

        List<OrderFlowStatusHistory> history = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        Assertions.assertEquals(history.size(), 3);
        Assertions.assertEquals(history.get(0).getOrderFlowStatusAfter(),
                OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP);
    }

    @Test
    public void orderShouldRescheduleAndSaveHistoryOrderIsPickup() {
        LocalDate now = LocalDate.now(clock);
        transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L);
            pickupPointHolidayRepository.save(new PickupPointHoliday(pickupPoint, now));
            order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                    .deliveryDate(now)
                    .deliveryServiceId(DEFAULT_DS_ID)
                    .pickupPoint(pickupPoint)
                    .build());
            return null;
        });


        RescheduleOrderPayload payload = new RescheduleOrderPayload(
                "requestId",
                order.getId(),
                Instant.now(clock),
                LocalDate.now(clock).plusDays(1)
        );

        rescheduleOrderProcessingService.processPayload(payload);

        Order updated = orderRepository.findByIdOrThrow(order.getId());

        Assertions.assertEquals(updated.getDelivery().getDeliveryDateAtDefaultTimeZone(),
                now.plusDays(1));

        List<OrderFlowStatusHistory> history = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        Assertions.assertEquals(history.size(), 3);
        Assertions.assertEquals(history.get(0).getOrderFlowStatusAfter(),
                OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP);
    }

}
