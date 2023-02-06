package ru.yandex.market.delivery.transport_manager.facade.route.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.facade.route.schedule.validator.RouteScheduleCommonValidator;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleSubtypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;

class RouteScheduleCommonValidatorTest extends AbstractContextualTest {

    @Autowired
    private RouteScheduleCommonValidator validator;

    @Test
    @DisplayName("Успешная валидация простого расписания с переходом дат")
    void validatePointsSuccess() {
        validator.validate(schedule(List.of(
            point(0, "23:30", "00:30", 0),
            point(1, "14:00", "15:00", 1)
        )));
    }

    @Test
    @DisplayName("Пустой список точек расписания")
    void validatePointsEmpty() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(schedule(null)),
            "Пустое расписание точек"
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(schedule(List.of())),
            "Пустое расписание точек"
        );
    }

    @Test
    @DisplayName("Некорректные индексы")
    void validatePointsInvalidIndexes() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(schedule(List.of(
                point(0, "10:00", "11:00", 0),
                point(0, "12:00", "13:00", 0),
                point(2, "14:00", "15:00", 0),
                point(3, "17:00", "19:00", 0)
            ))),
            "Неверная последовательность/нумерация расписания точек"
        );
    }

    @Test
    @DisplayName("Несовместимые тип и подтип")
    void validateTypeAndSubtype() {

        var scheduleWithErrorType = schedule(List.of(
            point(0, "23:30", "00:30", 0),
            point(1, "14:00", "15:00", 1)
        ))
            .setType(RouteScheduleTypeDto.XDOC_TRANSPORT)
            .setSubtype(RouteScheduleSubtypeDto.SUPPLEMENTARY_4);


        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(scheduleWithErrorType),
            "Подтип SUPPLEMENTARY_4 несовместим с типом XDOC_TRANSPORT"
        );
    }

    private RouteScheduleDto schedule(List<RouteSchedulePointDto> points) {
        return RouteScheduleDto.builder()
            .daysOfWeek(List.of(DayOfWeek.values()))
            .points(points)
            .build();
    }

    private RouteSchedulePointDto point(int index, String timeFrom, String timeTo, int offset) {
        return RouteSchedulePointDto.builder()
            .index(index)
            .timeFrom(LocalTime.parse(timeFrom))
            .timeTo(LocalTime.parse(timeTo))
            .daysOffset(offset)
            .build();
    }
}
