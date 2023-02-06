package ru.yandex.market.pvz.core.domain.order;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.DELIVERED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderDeliveryCommitterTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final OrderRepository orderRepository;

    private final OrderDeliveryCommitter orderDeliveryCommitter;

    /**
     * Выдача:       10.01.2020 20:59
     * Закрытие ПВЗ: 10.01.2020 21:00
     * Фиксация:     10.01.2020 21:40
     */
    @Test
    void commitThisDayOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(20, 59));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder().build(),
                OrderDeliveryType.UNKNOWN,
                OrderPaymentType.PREPAID
        );

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 40));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
    }

    /**
     * Выдача:       10.01.2020 20:59
     * Закрытие ПВЗ: 10.01.2020 21:00
     * Фиксация:     10.01.2020 21:40
     */
    @Test
    void commitThisDayOnDemandOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(20, 59));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(TestOrderFactory.OrderParams.builder()
                                .type(OrderType.ON_DEMAND)
                                .build())
                        .build(),
                OrderDeliveryType.PASSPORT,
                OrderPaymentType.PREPAID);

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 40));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
    }

    /**
     * Выдача:       10.01.2020 20:59
     * Закрытие ПВЗ: 10.01.2020 21:00
     * Фиксация:     10.01.2020 21:25
     */
    @Test
    void notCommitThisDayOrderBecauseOfNotEnoughDelayBetweenClosingAndCommit() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(20, 59));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder().build(),
                OrderDeliveryType.PASSPORT,
                OrderPaymentType.PREPAID
        );

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 25));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
    }

    /**
     * Выдача:       10.01.2020 21:10
     * Закрытие ПВЗ: 10.01.2020 21:00
     * Фиксация:     10.01.2020 21:35
     */
    @Test
    void notCommitThisDayOrderBecauseOfNotEnoughDelayBetweenTransmitAndCommit() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 10));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder().build(),
                OrderDeliveryType.UNKNOWN,
                OrderPaymentType.PREPAID
        );

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 35));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
    }

    /**
     * Выдача:       10.01.2020 20:59
     * Закрытие ПВЗ: 10.01.2020 23:59
     * Фиксация:     11.01.2020 00:05
     */
    @Test
    void commitYesterdayOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build()
                                ))
                                .build())
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(20, 59));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder().build(),
                OrderDeliveryType.PASSPORT,
                OrderPaymentType.PREPAID
        );

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 11),
                LocalTime.of(0, 5));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
    }

    /**
     * Выдача:       10.01.2020 21:30
     * Закрытие ПВЗ: 10.01.2020 21:00
     * Фиксация:     10.01.2020 21:25
     */
    @Test
    void notCommitInvalidTransmitTimeOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 30));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder().build(),
                OrderDeliveryType.PASSPORT,
                OrderPaymentType.PREPAID
        );

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 10),
                LocalTime.of(21, 25));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
    }

    /**
     * Выдача:       31.12.2020 23:55
     * Закрытие ПВЗ: 31.12.2020 23:59
     * Фиксация:     01.01.2020 00:40
     */
    @Test
    void commitLastYearOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .timeTo(LocalTime.of(23, 59))
                                                .build()
                                ))
                                .build())
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime transmitOrderTime = LocalDateTime.of(
                LocalDate.of(2020, 12, 31),
                LocalTime.of(23, 55));
        clock.setFixed(transmitOrderTime.toInstant(zone), zone);

        var order = orderFactory.deliverOrder(
                TestOrderFactory.CreateOrderBuilder.builder().build(),
                OrderDeliveryType.PASSPORT,
                OrderPaymentType.PREPAID
        );

        LocalDateTime commitTime = LocalDateTime.of(
                LocalDate.of(2021, 1, 1),
                LocalTime.of(0, 40));
        clock.setFixed(commitTime.toInstant(zone), zone);
        orderDeliveryCommitter.commit();

        var committed = orderRepository.findByIdOrThrow(order.getId());
        assertThat(committed.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
    }

}
