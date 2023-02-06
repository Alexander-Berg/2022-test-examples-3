package ru.yandex.market.tsup.service.data_provider.entity.route.schedule;

import java.time.LocalTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.tsup.core.converter.RouteScheduleFromTMConverter;

class RouteScheduleListFrontProviderUnitTest {

    @Test
    void convertPoints() {
        Assertions.assertThat(RouteScheduleFromTMConverter.convert(List.of(
                routeSchedulePointDtoTm(0, 0, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDtoTm(1, 0, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                routeSchedulePointDtoTm(2, 1, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                schedulePointTsup(0, null, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                schedulePointTsup(1, 720, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                schedulePointTsup(2, 720, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints1() {
        Assertions.assertThat(RouteScheduleFromTMConverter.convert(List.of(
                routeSchedulePointDtoTm(0, 0, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDtoTm(1, 1, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                routeSchedulePointDtoTm(2, 2, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                schedulePointTsup(0, null, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                schedulePointTsup(1, 2160, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                schedulePointTsup(2, 720, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints2() {
        Assertions.assertThat(RouteScheduleFromTMConverter.convert(List.of(
                routeSchedulePointDtoTm(0, 0, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                routeSchedulePointDtoTm(1, 1, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                routeSchedulePointDtoTm(2, 1, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                routeSchedulePointDtoTm(3, 3, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDtoTm(4, 4, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                routeSchedulePointDtoTm(5, 5, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                schedulePointTsup(0, null, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                schedulePointTsup(1, 0, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                schedulePointTsup(2, 1260, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                schedulePointTsup(3, 1380, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                schedulePointTsup(4, 1500, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                schedulePointTsup(5, 1380, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints3() {
        Assertions.assertThat(RouteScheduleFromTMConverter.convert(List.of(
                routeSchedulePointDtoTm(0, 0, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDtoTm(1, 1, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                routeSchedulePointDtoTm(2, 2, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                schedulePointTsup(0, null, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                schedulePointTsup(1, 2160, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                schedulePointTsup(2, 720, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints4() {
        Assertions.assertThat(RouteScheduleFromTMConverter.convert(List.of(
                routeSchedulePointDtoTm(1, 0, LocalTime.of(3, 0), LocalTime.of(4, 0)),
                routeSchedulePointDtoTm(0, 0, LocalTime.of(1, 0), LocalTime.of(2, 0))
            )))
            .containsExactlyInAnyOrder(

                schedulePointTsup(0, null, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                schedulePointTsup(1, 60, LocalTime.of(3, 0), LocalTime.of(4, 0))
            );
    }



    private ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteSchedulePointDto schedulePointTsup(
        int index,
        Integer transitionTime,
        LocalTime startTime,
        LocalTime endTime
    ) {
        ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteSchedulePointDto params =
            new ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteSchedulePointDto();
        params.setIndex(index);
        params.setTransitionTime(transitionTime);
        params.setTimeFrom(startTime);
        params.setTimeTo(endTime);
        return params;
    }

    private RouteSchedulePointDto routeSchedulePointDtoTm(
        int index,
        int dayOffset,
        LocalTime timeFrom,
        LocalTime timeTo
    ) {
        RouteSchedulePointDto point = new RouteSchedulePointDto();
        point.setDaysOffset(dayOffset)
            .setIndex(index)
            .setTimeTo(timeTo)
            .setTimeFrom(timeFrom);
        return point;
    }

}
