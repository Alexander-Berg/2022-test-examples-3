package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropoff;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import dto.requests.lms.HolidayDto;
import dto.requests.lms.HolidayNewDto;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;
import toolkit.Delayer;

@Slf4j
@Epic("Dropoff")
@Tag("ExcludeRegress")
@Resource.Classpath("delivery.properties")
@DisplayName("Dropoff Dayoffs Test")
public class DropoffDayoffTest extends AbstractDropoffTest {

    private static final int DAYOFF_INTERVAL = 5;

    @Test
    @DisplayName("Проверка корректной отработки джобы проставления дейоффов")
    public void correctExecutingDropoffDayoffJob() {
        List<HolidayNewDto> newDayoffs = IntStreamEx.range(0, DAYOFF_INTERVAL)
            .mapToObj(dayOffset -> new HolidayNewDto(LocalDate.now().plusDays(dayOffset + 1)))
            .toList();

        //Создаем дейоффы на следующие DAYOFF_INTERVAL дней
        LMS_STEPS.createDayoffsForLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID, newDayoffs);

        try {
            NESU_STEPS.runYtUpdateDropoffDayoffJob();

            //Дожидаемся отработки джобы
            Delayer.delay(12, TimeUnit.MINUTES);

            //Проверяем проставления дейоффов в несу
            NESU_STEPS.waitAvailableShipmentOption(
                SHOP_PARTNER_ID,
                SHOP_ID,
                USER_ID,
                DROPOFF_LOGISTIC_POINT_ID,
                (dropoff) -> Assertions.assertTrue(
                    containsStreamPredicate(newDayoffs, dropoff.getDayoffs()).allMatch(e -> e.equals(Boolean.TRUE)),
                    "После проставления дейоффов в лмс, данные в несу не обновились"
                )
            );

        } finally {
            log.info("Очищаем тестовые данные после падения теста");
            clearDayoffChanges(newDayoffs);
        }

        NESU_STEPS.runYtUpdateDropoffDayoffJob();

        //Дожидаемся отработки джобы
        Delayer.delay(12, TimeUnit.MINUTES);

        //Проверяем удаление дейоффов в несу
        NESU_STEPS.waitAvailableShipmentOption(
            SHOP_PARTNER_ID,
            SHOP_ID,
            USER_ID,
            DROPOFF_LOGISTIC_POINT_ID,
            (dropoff) -> Assertions.assertTrue(
                containsStreamPredicate(newDayoffs, dropoff.getDayoffs()).noneMatch(e -> e.equals(Boolean.TRUE)),
                "После удаления дейоффов в лмс, данные в несу не обновились"
            )
        );
    }

    @Nonnull
    private Stream<Boolean> containsStreamPredicate(
        List<HolidayNewDto> requiredDayoffs,
        List<LocalDate> actualDayoffs
    ) {
        return requiredDayoffs.stream().map(HolidayNewDto::getDay).map(actualDayoffs::contains);
    }

    private void clearDayoffChanges(List<HolidayNewDto> createdDayoffs) {
        Set<Long> dayoffIds = LMS_STEPS.getHolidaysForLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID).stream()
            .filter(dayoff -> this.hasDayoffBeenCreated(dayoff, createdDayoffs))
            .map(HolidayDto::getId)
            .collect(Collectors.toSet());

        LMS_STEPS.deleteHolidaysForLogisticsPoint(DROPOFF_LOGISTIC_POINT_ID, dayoffIds);
    }

    private boolean hasDayoffBeenCreated(HolidayDto dayoff, List<HolidayNewDto> createdDayoffs) {
        return createdDayoffs.stream().map(HolidayNewDto::getDay).anyMatch(day -> day.equals(dayoff.getDay()));
    }
}
