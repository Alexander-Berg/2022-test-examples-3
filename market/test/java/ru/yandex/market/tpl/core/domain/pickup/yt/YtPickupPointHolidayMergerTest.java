package ru.yandex.market.tpl.core.domain.pickup.yt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.domain.pickup.holiday.PickupPointHolidayMerger;
import ru.yandex.market.tpl.core.domain.pickup.yt.repository.YtPickupPointHolidayRawResult;
import ru.yandex.market.tpl.core.domain.pickup.yt.repository.YtPickupPointRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class YtPickupPointHolidayMergerTest {

    @InjectMocks
    private YtPickupPointHolidayMerger ytPickupPointHolidayMerger;
    @Mock
    private YtPickupPointRepository ytPickupPointRepository;
    @Mock
    private PickupPointHolidayMerger pickupPointHolidayMerger;

    @DisplayName("Мерж с выходными днями")
    @Test
    void mergeTest() {
        LocalDate holidayDate = LocalDate.now();
        Set<Long> logisticPointIds = Set.of(1L, 2L);
        when(ytPickupPointRepository.getLogisticPointHolidays(logisticPointIds))
                .thenReturn(
                        List.of(
                                YtPickupPointHolidayRawResult.builder()
                                        .holidayDate(holidayDate.toString())
                                        .logisticPointId(1L)
                                        .build(),
                                YtPickupPointHolidayRawResult.builder()
                                        .holidayDate(holidayDate.toString())
                                        .logisticPointId(2L)
                                        .build()
                        )
                );

        ytPickupPointHolidayMerger.merge(logisticPointIds);

        verify(pickupPointHolidayMerger, atLeastOnce())
                .bulkMergeLogisticPointHoliday(
                        eq(logisticPointIds),
                        eq(Map.of(
                                1L, Set.of(holidayDate),
                                2L, Set.of(holidayDate)
                        ))
                );
    }

    @DisplayName("Нет выходных дней")
    @Test
    void mergeWithoutHolidayDateTest() {
        Set<Long> logisticPointIds = Set.of(1L);
        when(ytPickupPointRepository.getLogisticPointHolidays(logisticPointIds))
                .thenReturn(
                        List.of(
                                YtPickupPointHolidayRawResult.builder()
                                        .holidayDate("")
                                        .logisticPointId(1L)
                                        .build()
                        )
                );

        ytPickupPointHolidayMerger.merge(logisticPointIds);

        verify(pickupPointHolidayMerger, atLeastOnce())
                .bulkMergeLogisticPointHoliday(
                        eq(logisticPointIds),
                        eq(Map.of())
                );
    }

    @DisplayName("Не мержим, когда нет точек для обновления")
    @Test
    void noMergeHolidayDateTest() {
        Set<Long> logisticPointIds = Set.of();

        ytPickupPointHolidayMerger.merge(logisticPointIds);

        verify(pickupPointHolidayMerger, Mockito.never())
                .bulkMergeLogisticPointHoliday(eq(logisticPointIds), eq(Map.of()));
    }

}
