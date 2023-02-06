package ru.yandex.market.logistics.cs.dayoff;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.domain.entity.Day;
import ru.yandex.market.logistics.cs.domain.entity.DaysOffForExport;
import ru.yandex.market.logistics.cs.domain.entity.DaysoffDates;
import ru.yandex.market.logistics.cs.repository.DaysOffForExportRepository;
import ru.yandex.market.logistics.cs.service.ServiceDayOffService;

import static java.time.temporal.ChronoUnit.DAYS;

@DisplayName("Агрегация дейоффов по сервисам")
@ParametersAreNonnullByDefault
@DatabaseSetup("/repository/dayoff/before/service_dayoff.xml")
class DaysOffTriggerTest extends AbstractDayOffTest {
    private static final int ONE_HUNDRED_DAYS = 100;
    private static final int ONE_DAY = 1;
    private static final LocalDate NOW = LocalDate.now(ZoneId.of("UTC"));
    private static final LocalDate NOW_PLUS_HUNDRED = NOW.plus(ONE_HUNDRED_DAYS, DAYS);
    private static final LocalDate NOW_MINUS_ONE = NOW.minus(ONE_DAY, DAYS);
    private static final DaysOffForExport EXPECTED_DEFAULT_1ST_SERVICE = serviceDaysOff(
        SERVICE_ID_10, List.of(NOW_MINUS_ONE, NOW, NOW_PLUS_HUNDRED)
    );
    public static final DaysOffForExport EXPECTED_DEFAULT_2ND_SERVICE = serviceDaysOff(SERVICE_ID_20, List.of(NOW));

    @Autowired
    private DaysOffForExportRepository daysOffForExportRepository;

    @Autowired
    private ServiceDayOffService dayOffDayService;

    @Test
    @DisplayName("Создание дейоффов обновляет табличку с аггрегированными данными")
    void addDaysOff() {
        assertContainsOnly(EXPECTED_DEFAULT_1ST_SERVICE, EXPECTED_DEFAULT_2ND_SERVICE);
    }

    @Test
    @DisplayName("Удаление дейоффов корректно обновляет таблицу")
    void deleteDaysOff() {
        dayOffDayRepository.deleteById(1L);
        dayOffDayRepository.deleteById(4L);

        assertContainsOnly(serviceDaysOff(SERVICE_ID_10, List.of(NOW_MINUS_ONE, NOW_PLUS_HUNDRED)));
    }

    @Test
    @DisplayName("Добавление дейоффов обновляет табличку с агрегированынми данными")
    void updateDaysOffForService() {
        DaysOffForExport expectedServiceDayoffs = serviceDaysOff(SERVICE_ID_20, List.of(NOW_MINUS_ONE, NOW));

        dayOffDayRepository.saveAll(List.of(dayOff(SERVICE_ID_20, NOW_MINUS_ONE)));

        assertContainsOnly(EXPECTED_DEFAULT_1ST_SERVICE, expectedServiceDayoffs);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/dayoff/before/old_dayoffs.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Дейоффы вне временной рамки [-1, +∞) дней не добавляются")
    void daysOffOutsideTimeWindowArentAdded() {
        assertContainsOnly(EXPECTED_DEFAULT_1ST_SERVICE, EXPECTED_DEFAULT_2ND_SERVICE);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/dayoff/before/old_dayoffs.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("При удалении дейоффов по капасити, удаляется строка с сервисом, если для него нет актуальных дней")
    void deleteIfNoneDayOffsInsideTimeWindow() {
        dayOffDayService.updateDayOffs(1L, NOW);
        assertContainsOnly(serviceDaysOff(SERVICE_ID_10, List.of(NOW_MINUS_ONE, NOW_PLUS_HUNDRED)));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/dayoff/before/old_dayoffs.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("При удалении дейоффов для сервисов, удаляются все сервисы, если для них нет актуальных дней")
    void deleteForServices() {
        dayOffDayService.removeDayOffs(List.of(10L, 20L));
        softly.assertThat(daysOffForExportRepository.findAll()).isEmpty();
    }

    private void assertContainsOnly(DaysOffForExport... servicesToDaysOff) {
        List<DaysOffForExport> daysOffForExports = daysOffForExportRepository.findAll();
        softly.assertThat(daysOffForExports)
            .usingElementComparatorIgnoringFields("days")
            .containsExactlyInAnyOrder(servicesToDaysOff);
    }

    @Nonnull
    private static DaysOffForExport serviceDaysOff(Long serviceId, List<LocalDate> dates) {
        return DaysOffForExport.builder()
            .serviceId(serviceId)
            .daysOffDates(
                DaysoffDates.builder()
                    .daysOff(dates)
                    .days(
                        dates.stream()
                            .map(
                                day -> Day.builder()
                                    .created(LocalDate.now().atTime(LocalTime.NOON))
                                    .dayOff(day)
                                    .build()
                            )
                            .collect(Collectors.toList())
                    )
                    .build()
            )
            .build();
    }
}
