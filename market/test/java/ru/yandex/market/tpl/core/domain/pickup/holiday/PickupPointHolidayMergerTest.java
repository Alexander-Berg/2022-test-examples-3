package ru.yandex.market.tpl.core.domain.pickup.holiday;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator.generatePickupPoint;

@RequiredArgsConstructor
class PickupPointHolidayMergerTest extends TplAbstractTest {

    private static final LocalDate HOLIDAY_LOCAL_DATE = LocalDate.of(2020, 2, 20);
    private static final Long LOGISTIC_POINT_ID = 1L;
    private static final long NOT_EXIST_LOGISTIC_POINT_ID = 2L;
    private final PickupPointHolidayMerger pickupPointHolidayMerger;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;

    @DisplayName("Балковый мерж пустых коллекций")
    @Test
    void testEmptyBatchMerge() {
        Set<Long> logisticPointIds = Collections.emptySet();
        Map<Long, Set<LocalDate>> logisticPointIdToHolidayDate = Collections.emptyMap();

        pickupPointHolidayMerger.bulkMergeLogisticPointHoliday(
                logisticPointIds,
                logisticPointIdToHolidayDate
        );
    }

    @DisplayName("Добавление выходного дня для логистической точки")
    @Test
    void testAddHolidayToRoutePoint() {
        PickupPoint generatePickupPoint = generatePickupPoint(LOGISTIC_POINT_ID);
        PickupPoint pickupPoint = pickupPointRepository.save(generatePickupPoint);
        Set<Long> logisticPointId = Set.of(pickupPoint.getLogisticPointId());
        Set<LocalDate> ytLocalDateSet = Set.of(LocalDate.now());
        Map<Long, Set<LocalDate>> ytLogisticPointIdToHolidayDate = Map.of(
                LOGISTIC_POINT_ID, ytLocalDateSet
        );
        assertNull(pickupPoint.getLastHolidaySyncAt());

        pickupPointHolidayMerger.bulkMergeLogisticPointHoliday(
                logisticPointId,
                ytLogisticPointIdToHolidayDate
        );

        PickupPoint pickupPointWithHoliday =
                pickupPointRepository.findAllWithHolidaysByLogisticPointIdIn(logisticPointId).get(0);
        assertNotNull(pickupPointWithHoliday);
        Set<PickupPointHoliday> holidays = pickupPointWithHoliday.getHolidays();
        assertNotNull(holidays);
        Set<LocalDate> localDateSet =
                holidays.stream().map(PickupPointHoliday::getHolidayDate).collect(Collectors.toSet());
        assertNotNull(pickupPointWithHoliday.getLastHolidaySyncAt());
        assertEquals(localDateSet, ytLocalDateSet);
    }

    @DisplayName("Удаление выходного дня у логистической точки")
    @Test
    void testDeleteHolidayToRoutePoint() {
        PickupPoint generatePickupPoint = generatePickupPoint(LOGISTIC_POINT_ID);
        generatePickupPoint.setHolidays(Set.of(new PickupPointHoliday(generatePickupPoint, HOLIDAY_LOCAL_DATE)));
        Instant lastSyncHolidayAt = clock.instant().minusSeconds(100);
        generatePickupPoint.setLastHolidaySyncAt(lastSyncHolidayAt);
        PickupPoint pickupPoint = pickupPointRepository.save(generatePickupPoint);
        Set<Long> logisticPointId = Set.of(pickupPoint.getLogisticPointId());
        Map<Long, Set<LocalDate>> ytLogisticPointIdToHolidayDate = Map.of(
                LOGISTIC_POINT_ID, Collections.emptySet()
        );

        pickupPointHolidayMerger.bulkMergeLogisticPointHoliday(
                logisticPointId,
                ytLogisticPointIdToHolidayDate
        );

        PickupPoint pickupPointAfterMerge =
                pickupPointRepository.findAllWithHolidaysByLogisticPointIdIn(logisticPointId).get(0);
        assertNotNull(pickupPointAfterMerge);
        Set<PickupPointHoliday> holidays = pickupPointAfterMerge.getHolidays();
        assertNotNull(holidays);
        Set<LocalDate> localDateSet =
                holidays.stream().map(PickupPointHoliday::getHolidayDate).collect(Collectors.toSet());
        assertTrue(localDateSet.isEmpty());
        assertNotNull(pickupPointAfterMerge.getLastHolidaySyncAt());
        assertNotEquals(pickupPointAfterMerge.getLastHolidaySyncAt(), lastSyncHolidayAt);
    }

    @DisplayName("Удаление всех выходных если из YT не пришли значения выходных дней")
    @Test
    void testDeleteAllHolidays() {
        PickupPoint generatePickupPoint = generatePickupPoint(LOGISTIC_POINT_ID);
        generatePickupPoint.setHolidays(Set.of(new PickupPointHoliday(generatePickupPoint, HOLIDAY_LOCAL_DATE)));
        Instant lastSyncHolidayAt = clock.instant().minusSeconds(100);
        generatePickupPoint.setLastHolidaySyncAt(lastSyncHolidayAt);
        PickupPoint pickupPoint = pickupPointRepository.save(generatePickupPoint);
        Set<Long> logisticPointId = Set.of(pickupPoint.getLogisticPointId());
        Map<Long, Set<LocalDate>> ytLogisticPointIdToHolidayDate = Map.of();

        pickupPointHolidayMerger.bulkMergeLogisticPointHoliday(
                logisticPointId,
                ytLogisticPointIdToHolidayDate
        );

        PickupPoint pickupPointAfterMerge =
                pickupPointRepository.findAllWithHolidaysByLogisticPointIdIn(logisticPointId).get(0);
        assertNotNull(pickupPointAfterMerge);
        Set<PickupPointHoliday> holidays = pickupPointAfterMerge.getHolidays();
        assertNotNull(holidays);
        Set<LocalDate> localDateSet =
                holidays.stream().map(PickupPointHoliday::getHolidayDate).collect(Collectors.toSet());
        assertTrue(localDateSet.isEmpty());
        assertNotNull(pickupPointAfterMerge.getLastHolidaySyncAt());
        assertNotEquals(pickupPointAfterMerge.getLastHolidaySyncAt(), lastSyncHolidayAt);
    }

    @DisplayName("Добавление выходного дня для логистической точки, которой не существует")
    @Test
    void testAddHolidayToNotExistRoutePoint() {
        PickupPoint generatePickupPoint = generatePickupPoint(LOGISTIC_POINT_ID);
        PickupPoint pickupPoint = pickupPointRepository.save(generatePickupPoint);
        Set<Long> logisticPointId = Set.of(pickupPoint.getLogisticPointId());
        Set<LocalDate> ytLocalDateSet = Set.of(LocalDate.now());
        Map<Long, Set<LocalDate>> ytLogisticPointIdToHolidayDate = Map.of(
                NOT_EXIST_LOGISTIC_POINT_ID, ytLocalDateSet
        );
        assertNull(pickupPoint.getLastHolidaySyncAt());

        pickupPointHolidayMerger.bulkMergeLogisticPointHoliday(
                logisticPointId,
                ytLogisticPointIdToHolidayDate
        );

        PickupPoint pickupPointWithHoliday =
                pickupPointRepository.findAllWithHolidaysByLogisticPointIdIn(logisticPointId).get(0);
        assertNotNull(pickupPointWithHoliday);
        Set<PickupPointHoliday> holidays = pickupPointWithHoliday.getHolidays();
        assertNotNull(holidays);
        Set<LocalDate> localDateSet =
                holidays.stream().map(PickupPointHoliday::getHolidayDate).collect(Collectors.toSet());
        assertTrue(localDateSet.isEmpty());
    }

    @DisplayName("Попытка обновления для логистической точки, у которой уже есть такой выходной")
    @Test
    void testAddHolidayToRoutePointWithSameHoliday() {
        PickupPoint generatePickupPoint = generatePickupPoint(LOGISTIC_POINT_ID);
        generatePickupPoint.setHolidays(Set.of(new PickupPointHoliday(generatePickupPoint, HOLIDAY_LOCAL_DATE)));
        Instant lastSyncHolidayAt = clock.instant().minusSeconds(100);
        generatePickupPoint.setLastHolidaySyncAt(lastSyncHolidayAt);
        PickupPoint pickupPoint = pickupPointRepository.save(generatePickupPoint);
        Set<Long> logisticPointId = Set.of(pickupPoint.getLogisticPointId());
        Set<LocalDate> ytLocalDateSet = Set.of(HOLIDAY_LOCAL_DATE);
        Map<Long, Set<LocalDate>> ytLogisticPointIdToHolidayDate = Map.of(
                LOGISTIC_POINT_ID, ytLocalDateSet
        );

        pickupPointHolidayMerger.bulkMergeLogisticPointHoliday(
                logisticPointId,
                ytLogisticPointIdToHolidayDate
        );

        PickupPoint pickupPointWithHoliday =
                pickupPointRepository.findAllWithHolidaysByLogisticPointIdIn(logisticPointId).get(0);
        assertNotNull(pickupPointWithHoliday);
        Set<PickupPointHoliday> holidays = pickupPointWithHoliday.getHolidays();
        assertNotNull(holidays);
        assertEquals(pickupPoint.getHolidays(), holidays);
        assertNotEquals(pickupPoint.getLastHolidaySyncAt(), pickupPointWithHoliday.getLastHolidaySyncAt());
    }
}
