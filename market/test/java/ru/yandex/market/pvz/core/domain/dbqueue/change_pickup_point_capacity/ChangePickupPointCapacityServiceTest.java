package ru.yandex.market.pvz.core.domain.dbqueue.change_pickup_point_capacity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderUpdateCapacityParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacity;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;


@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ChangePickupPointCapacityServiceTest {

    private static final int CAPACITY = 10;
    private static final int STORAGE_PERIOD = 2;
    private static final Long ORDER_ID = 5L;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;
    private final ChangePickupPointCapacityService capacityService;
    private final PickupPointCapacityRepository capacityRepository;

    private PickupPoint pickupPoint;

    @BeforeEach
    public void setup() {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
        pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .capacity(CAPACITY)
                        .storagePeriod(STORAGE_PERIOD)
                        .build());
    }

    @Test
    void whenStatusFromNullToCreated() {
        //Это создание новых заказов, занимаем место по deliveryDate для обоих
        // clock == 15.01, clock + 2 == 17, занимаем 17-19
        OrderUpdateCapacityParams order1 = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(2), null
        );
        // clock == 15.01, clock + 4 == 19, занимаем 19-21
        OrderUpdateCapacityParams order2 = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(4), null
        );
        capacityService.recalculateCapacity(order1);
        capacityService.recalculateCapacity(order2);

        // 16.01 записей нет
        checkCapacityByDateIsNull(order1.getPickupPointId(), LocalDate.now(clock).plusDays(1));
        // 17.01
        checkCountByDateIsEqualToNumber(order1.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        // 18.01
        checkCountByDateIsEqualToNumber(order1.getPickupPointId(), LocalDate.now(clock).plusDays(3), 1);
        // 19.01 - тут два заказа на пвз
        checkCountByDateIsEqualToNumber(order1.getPickupPointId(), LocalDate.now(clock).plusDays(4), 2);
        // 20.01
        checkCountByDateIsEqualToNumber(order1.getPickupPointId(), LocalDate.now(clock).plusDays(5), 1);
        // 21.01
        checkCountByDateIsEqualToNumber(order1.getPickupPointId(), LocalDate.now(clock).plusDays(6), 1);
        // 22.01 записей нет
        checkCapacityByDateIsNull(order1.getPickupPointId(), LocalDate.now(clock).plusDays(7));
    }

    @Test
    void whenDeliveryDateIsIncreased() {
        /*
        Заказ прибудет позже ожидаемого, занимаем сначала место по deliveryDate, потом это место освобождаем и опять
        занимаем по новой deliveryDate
         */
        // clock == 15.01, clock + 1 == 16, занимаем место 16, 17, 18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // clock == 17.01, clock + 1 == 18, освобождаем место 17-18 (16 уже прошло, не трогаем),
        // занимаем место 18-19
        order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1),
                LocalDate.now(clock).minusDays(1)
        );
        capacityService.recalculateCapacity(order);

        // 15.01, записей нет
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(2));
        // 16.01, прошедший день не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 17.01, сегодня, освободили
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 0);
        // 18.01-19.01
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(3), 1);
        // 20.01 записей нет
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(4));
    }

    @Test
    void whenDeliveryDateIsDecreased() {
        /*
        Заказ прибудет раньше ожидаемого, занимаем сначала место по deliveryDate, потом это место освобождаем и опять
        занимаем по новой deliveryDate
         */
        // clock == 15.01, clock + 4 == 19, занимаем 19-21
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(4), null
        );
        capacityService.recalculateCapacity(order);
        // clock == 15.01, clock + 2 == 17, освобождаем 19-21, занимаем 17-19
        order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(2),
                LocalDate.now(clock).plusDays(4)
        );
        capacityService.recalculateCapacity(order);

        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(1));
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(3), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(4), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(5), 0);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(6), 0);
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(7));
    }

    @Test
    void whenArrivedToPickupPoint() {
        /*
         Создали новый заказ занимаем место по deliveryDate, проходит два дня, принимаем заказ на ПВЗ, освобождаем
         место по deliveryDate и занимаем по expirationDate.
         */
        // clock == 15.01, clock + 1 == 16, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // clock == 17.01, 16 не освобождаем, так как дата прошла, освобождаем 17-18, занимаем 17-19
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 16.01 не освобождали т.к. прошедшая дата
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 17-19 заняли
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        // 20.01 - пусто
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(3));
    }

    @Test
    void whenStatusFromCreatedToArrivedToPickupPointToCreated() {
        /*
         Создали новый заказ занимаем место по deliveryDate, принимаем заказ на ПВЗ, освобождаем
         место по deliveryDate и занимаем по expirationDate. Затем ПВЗ говорит что принял заказ ошибочно и мы с помощью
         мануальной ручки меняем статус заказа на CREATED. Освобождаем ранее занимаемое место по expirationDate
         и занимаем по deliveryDate
         */
        // clock == 15.01, clock + 1 == 16, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // заказ приняли 17, освобождаем 17-18 и занимаем 17-19
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-18T12:00:00Z"), clock.getZone());
        // заказ ошибочно приняли, освобождаем 17-19 и занимаем 18 (16-17 уже прошло)
        order = buildOrderParams(
                order.getExpirationDate(), null, PvzOrderStatus.CREATED,
                PvzOrderStatus.ARRIVED_TO_PICKUP_POINT, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 15.01 ничего не делали
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(3));
        // 16-17 не освобождали, т.к. прошли эти даты
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(2), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 18.01 заняли
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 1);
        // 19.01 освободили
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 0);
        // 20.01 не трогали
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(2));
    }

    @Test
    void whenTransmittedToRecipient() {
        /*
         Создали новый заказ занимаем место по deliveryDate, принимаем заказ на ПВЗ, освобождаем
         место по deliveryDate и занимаем по expirationDate. Выдаем заказ. Освобождаем ранее занимаемое место по
         expirationDate
         */
        // clock == 15.01, clock + 1 == 16, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // clock == 17.01, 16 не трогаем, освобождаем 17-18, занимаем 17-19
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-18T12:00:00Z"), clock.getZone());
        // 17 не трогаем, освобождаем 18-19
        order = buildOrderParams(
                order.getExpirationDate(), null, PvzOrderStatus.TRANSMITTED_TO_RECIPIENT,
                PvzOrderStatus.ARRIVED_TO_PICKUP_POINT, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 15 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(3));
        // 16-17 не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(2), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 18-19 освободили
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 0);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 0);
        // 20 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(2));
    }

    @Test
    void whenStatusFromCreatedToReadyForReturn() {
        /*
         Создали новый заказ занимаем место по deliveryDate, затем мануальной ручкой переводим в статус
         готов к возврату, освобождаем место
         */
        // clock == 15.01, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // 16 не трогаем, освобождаем 17-18
        order = buildOrderParams(
                null, null, PvzOrderStatus.READY_FOR_RETURN,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 15.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(2));
        // 16 не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 17-18 освободили
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 0);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 0);
        // 19.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(2));
    }

    @Test
    void whenStatusFromCanceledToReadyForReturn() {
        /*
         Создали новый заказ занимаем место по deliveryDate, затем заказ отменяется, освобождаем место при переходе
         в статус CANCELED, затем принимаем его на ПВЗ номинально переводим в ARRIVED_TO_PICKUP_POINT и не занимаем
         место по expirationDate и сразу переводим в READY_FOR_RETURN, освобождать место по deliveryDate не нужно
         */
        // clock == 15.01, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // 16 не трогаем, освобождаем 17-18
        order = buildOrderParams(
                null, null, PvzOrderStatus.CANCELLED,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // ничего не освобождаем, уже было освобождено
        order = buildOrderParams(
                null, null, PvzOrderStatus.READY_FOR_RETURN,
                PvzOrderStatus.ARRIVED_TO_PICKUP_POINT, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 15.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(2));
        // 16 не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 17-18 освободили
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 0);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 0);
        // 19.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(2));
    }

    @Test
    void whenStatusFromTransmittedToRecipientToArrivedToPickupPoint() {
        /*
         Создали новый заказ занимаем место по deliveryDate, принимаем заказ на ПВЗ, освобождаем
         место по deliveryDate и занимаем по expirationDate. Выдаем заказ. Освобождаем ранее занимаемое место по
         expirationDate. Отменяем выдачу, занимаем обратно место по expirationDate
         */
        // clock == 15.01, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // 16 не изменяем, 17-18 освобождаем, занимаем 17-19
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // освобождаем 17-19
        order = buildOrderParams(
                order.getExpirationDate(), null, PvzOrderStatus.TRANSMITTED_TO_RECIPIENT,
                PvzOrderStatus.ARRIVED_TO_PICKUP_POINT, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // занимаем 17-19
        order = buildOrderParams(
                order.getExpirationDate(), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.TRANSMITTED_TO_RECIPIENT, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 15.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(2));
        // 16 не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        // 17-19 занято
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        // 20 не трогали
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(3));
    }

    @Test
    void whenStoragePeriodExtended() {
        /*
         Создали новый заказ, занимаем место по deliveryDate, принимаем заказ на ПВЗ, освобождаем место по deliveryDate
         и занимаем по expirationDate. Увеличиваем срок хранения, до занимаем место
         */
        // clock == 15.01, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // 16 не изменяем, 17-18 освобождаем, занимаем 17-19
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-19T12:00:00Z"), clock.getZone());
        // продлеваем срок хранения до 21, занимаем 20-21 числа, 19 уже занято
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), order.getExpirationDate(),
                PvzOrderStatus.STORAGE_PERIOD_EXTENDED, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT, order.getDeliveryDate(),
                null
        );
        capacityService.recalculateCapacity(order);

        // 15.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(4));
        // 16 не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(3), 1);
        // 17 - 19 было занято
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(2), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 1);
        // 20-21 продлили срок хранения
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        // 22.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(3));
    }

    @Test
    void whenStatusFromStoragePeriodExtendedToArrivedToPickupPoint() {
        /*
         Повторяет тест whenStoragePeriodExtended() (создали новый заказ, занимаем место по deliveryDate, принимаем
         заказ на ПВЗ, освобождаем место по deliveryDate и занимаем по expirationDate. Увеличиваем срок хранения,
         до занимаем место) только переводим из STORAGE_PERIOD_EXTENDED в ARRIVED_TO_PICKUP_POINT, казалось бы ситуация
         нереальная, но в мануальной ручке по смене статуса это возможно, так что логику предусмотрел и нужно
         протестить. Занимаемое место измениться не должно
         */
        // clock == 15.01, занимаем 16-18
        OrderUpdateCapacityParams order = buildOrderParams(
                null, null, PvzOrderStatus.CREATED, null, LocalDate.now(clock).plusDays(1), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-17T12:00:00Z"), clock.getZone());
        // 16 не изменяем, 17-18 освобождаем, занимаем 17-19
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.CREATED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        clock.setFixed(Instant.parse("2021-01-19T12:00:00Z"), clock.getZone());
        // продлеваем срок хранения до 21, занимаем 20-21 числа, 19 уже занято
        order = buildOrderParams(
                LocalDate.now(clock).plusDays(STORAGE_PERIOD), order.getExpirationDate(),
                PvzOrderStatus.STORAGE_PERIOD_EXTENDED, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT, order.getDeliveryDate(),
                null
        );
        capacityService.recalculateCapacity(order);

        order = buildOrderParams(
                order.getExpirationDate(), null, PvzOrderStatus.ARRIVED_TO_PICKUP_POINT,
                PvzOrderStatus.STORAGE_PERIOD_EXTENDED, order.getDeliveryDate(), null
        );
        capacityService.recalculateCapacity(order);

        // 15.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).minusDays(4));
        // 16 не освобождали
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(3), 1);
        // 17 - 19 было занято
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(2), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).minusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock), 1);
        // 20-21 продлили срок хранения
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(1), 1);
        checkCountByDateIsEqualToNumber(order.getPickupPointId(), LocalDate.now(clock).plusDays(2), 1);
        // 22.01 ничего не было
        checkCapacityByDateIsNull(order.getPickupPointId(), LocalDate.now(clock).plusDays(3));
    }

    private void checkCapacityByDateIsNull(Long pickupPointId, LocalDate date) {
        PickupPointCapacity capacity = capacityRepository.findByPickupPointIdAndDate(
                pickupPointId, date
        );
        assertThat(capacity).isNull();
    }

    private void checkCountByDateIsEqualToNumber(Long pickupPointId, LocalDate date, int number) {
        PickupPointCapacity capacity = capacityRepository.findByPickupPointIdAndDate(
                pickupPointId, date
        );
        assertThat(capacity.getOrderCount()).isEqualTo(number);
    }

    private OrderUpdateCapacityParams buildOrderParams(
            LocalDate expirationDate, LocalDate expirationDateOld, PvzOrderStatus status,
            PvzOrderStatus statusOld, LocalDate deliveryDate, LocalDate deliveryDateOld
    ) {
        return OrderUpdateCapacityParams.builder()
                .capacityChanged(true)
                .pickupPointId(pickupPoint.getId())
                .expirationDate(expirationDate)
                .expirationDateOld(expirationDateOld)
                .pickupPointStoragePeriod(STORAGE_PERIOD)
                .status(status)
                .statusOld(statusOld)
                .id(ORDER_ID)
                .deliveryDate(deliveryDate)
                .deliveryDateOld(deliveryDateOld)
                .build();
    }

}
