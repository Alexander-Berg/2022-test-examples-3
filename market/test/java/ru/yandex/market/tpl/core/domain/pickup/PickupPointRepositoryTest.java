package ru.yandex.market.tpl.core.domain.pickup;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.domain.pickup.holiday.PickupPointHoliday;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator.generatePickupPoint;

@RequiredArgsConstructor
class PickupPointRepositoryTest extends TplAbstractTest {

    private final PickupPointRepository pickupPointRepository;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    @Test
    void save() {
        PickupPoint savedPickupPoint = transactionTemplate.execute(t -> {
            PickupPoint pickupPoint = generatePickupPoint(1L);
            return pickupPointRepository.save(pickupPoint);
        });

        Order loadedOrder = transactionTemplate.execute(t -> {
            Order lockerOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .recipientPhone("phone1")
                    .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                    .pickupPoint(savedPickupPoint)
                    .build());
            Order saved = orderRepository.save(lockerOrder);
            return orderRepository.getOne(saved.getId());
        });

        assertNotNull(loadedOrder);
        assertTrue(loadedOrder.isPickupPointDelivery());
        assertThat(loadedOrder.getPickupPoint().getCode()).isEqualTo(savedPickupPoint.getCode());
        savedPickupPoint.getHolidays().add(new PickupPointHoliday(savedPickupPoint, LocalDate.now()));
        pickupPointRepository.saveAndFlush(savedPickupPoint);
    }

    @Test
    void findAllByLastSyncAtBeforeOrLastSyncAtIsNull() {
        PickupPoint pickupPoint = pickupPointRepository.save(generatePickupPoint(1L));
        PickupPoint pickupPoint2 = pickupPointRepository.save(generatePickupPoint(2L));
        pickupPoint2.setLastSyncAt(Instant.now().minus(2, ChronoUnit.DAYS));
        PickupPoint pickupPoint3 = pickupPointRepository.save(generatePickupPoint(3L));
        pickupPoint3.setLastSyncAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        var toBeSynced = pickupPointRepository.findAllByLastSyncAtBeforeOrLastSyncAtIsNull(
                Instant.now(),
                Pageable.unpaged()
        );
        assertThat(toBeSynced)
                .contains(pickupPoint.getLogisticPointId(), pickupPoint2.getLogisticPointId());
    }

    @Test
    void findAllByLastSyncAtBeforeOrLastSyncAtIsNullPageable() {
        LongStreamEx.longs()
                .limit(10)
                .mapToObj(PickupPointGenerator::generatePickupPoint)
                .forEach(pickupPointRepository::save);
        int pageSize = 5;
        var toBeSynced = pickupPointRepository.findAllByLastSyncAtBeforeOrLastSyncAtIsNull(
                Instant.now(),
                PageRequest.of(0, pageSize)
        );
        assertThat(toBeSynced).hasSize(pageSize);
    }

    @DisplayName("Метод поиска логистических точек с фетчем выходных")
    @Test
    void findLogisticPointIdsByLastHolidaySyncAtBeforeTest() {
        LongStreamEx.longs()
                .limit(10)
                .mapToObj(PickupPointGenerator::generatePickupPoint)
                .peek(pickupPoint ->
                        pickupPoint.setHolidays(Set.of(new PickupPointHoliday(pickupPoint, LocalDate.now()))))
                .forEach(pickupPointRepository::save);
        int pageSize = 5;
        var logisticPoints = new HashSet<>(pickupPointRepository.findLogisticPointIdsByLastHolidaySyncAtBefore(
                Instant.now(),
                PageRequest.of(0, pageSize)
        ));

        var allWithHolidaysByLogisticPointIdIn =
                pickupPointRepository.findAllWithHolidaysByLogisticPointIdIn(logisticPoints);

        assertThat(allWithHolidaysByLogisticPointIdIn).hasSize(pageSize);
        assertThat(allWithHolidaysByLogisticPointIdIn.stream())
                .allMatch(pickupPoint -> !pickupPoint.getHolidays().isEmpty());
    }

    @DisplayName("Поиск точек для обновления возращают пустой список")
    @Test
    void test() {
        pickupPointRepository.deleteAll();
        int pageSize = 5;

        List<Long> lastHolidaySyncAtBefore =
                pickupPointRepository.findLogisticPointIdsByLastHolidaySyncAtBefore(
                        Instant.now(),
                        PageRequest.of(0, pageSize)
                );

        assertThat(lastHolidaySyncAtBefore).isEmpty();
    }

}
