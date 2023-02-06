package ru.yandex.market.deliveryintegrationtests.delivery.tests.tsup;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleDto;


@Resource.Classpath({"delivery/delivery.properties"})
@Slf4j
@DisplayName("Tsup Test")
@Epic("Tsup")
public class CreateAndDeleteScheduleTest extends AbstractTsupTest {

    private static final Long TEST_ROUTE_ID = 117L;

    @Test
    @DisplayName("Тsup: Создание и выключение расписания")
    void createAndDeleteRouteTest() {
        turnOffOldSchedules(TEST_ROUTE_ID);
        var nextMonth = LocalDate.now().plusMonths(1);
        TSUP_STEPS.createSchedulePipeline(TEST_ROUTE_ID, nextMonth, nextMonth);
        var schedules = TSUP_STEPS.getRouteSchedules(TEST_ROUTE_ID);
        Assertions.assertEquals(1, schedules.size(), "У маршрута не единственное расписание");
        long scheduleId = schedules.get(0).getId();
        TSUP_STEPS.turnOffSchedule(scheduleId, nextMonth.minusDays(10).format(DateTimeFormatter.ISO_DATE));
        TSUP_STEPS.verifyRouteHasNoSchedules(TEST_ROUTE_ID);
    }

    private void turnOffOldSchedules(long routeId) {
        var oldSchedules = TSUP_STEPS.getRouteMayBeEmptySchedules(routeId);
        oldSchedules.forEach(s -> TSUP_STEPS.turnOffSchedule(
            s.getId(),
            LocalDate.of(1990, 1, 1).format(DateTimeFormatter.ISO_DATE)
        ));
    }

}
