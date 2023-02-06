package ru.yandex.market.tsup.core.converter;

import java.time.LocalTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;


class RouteScheduleToTMConverterUnitTest {

    @Test
    void convertPoints() {
        Assertions.assertThat(RouteScheduleToTMConverter.convertPoints(List.of(
                pointParams(0, null, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                pointParams(1, 720, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                pointParams(2, 720, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                routeSchedulePointDto(0, 0, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDto(1, 0, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                routeSchedulePointDto(2, 1, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints1() {
        Assertions.assertThat(RouteScheduleToTMConverter.convertPoints(List.of(
                pointParams(0, null, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                pointParams(1, 2160, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                pointParams(2, 720, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                routeSchedulePointDto(0, 0, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDto(1, 1, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                routeSchedulePointDto(2, 2, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints2() {
        Assertions.assertThat(RouteScheduleToTMConverter.convertPoints(List.of(
                pointParams(0, null, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                pointParams(1, 0, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                pointParams(2, 1440, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                routeSchedulePointDto(0, 0, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                routeSchedulePointDto(1, 1, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                routeSchedulePointDto(2, 2, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints3() {
        Assertions.assertThat(RouteScheduleToTMConverter.convertPoints(List.of(
                pointParams(0, null, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                pointParams(1, 0, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                pointParams(2, 1260, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                pointParams(3, 1380, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                pointParams(4, 1500, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                pointParams(5, 1380, LocalTime.of(2, 0), LocalTime.of(3, 0))
            )))
            .containsExactlyInAnyOrder(
                routeSchedulePointDto(0, 0, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                routeSchedulePointDto(1, 1, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                routeSchedulePointDto(2, 1, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                routeSchedulePointDto(3, 3, LocalTime.of(0, 0), LocalTime.of(1, 0)),
                routeSchedulePointDto(4, 4, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                routeSchedulePointDto(5, 5, LocalTime.of(2, 0), LocalTime.of(3, 0))
            );
    }

    @Test
    void convertPoints4() {
        Assertions.assertThat(RouteScheduleToTMConverter.convertPoints(List.of(
                pointParams(1, 60, LocalTime.of(3, 0), LocalTime.of(4, 0)),
                pointParams(0, null, LocalTime.of(1, 0), LocalTime.of(2, 0))
            )))
            .containsExactlyInAnyOrder(
                routeSchedulePointDto(0, 0, LocalTime.of(1, 0), LocalTime.of(2, 0)),
                routeSchedulePointDto(1, 0, LocalTime.of(3, 0), LocalTime.of(4, 0))
            );
    }

    @Test
    void convertPoints5() {
        Assertions.assertThat(RouteScheduleToTMConverter.convertPoints(List.of(
                pointParams(0, null, LocalTime.of(23, 30), LocalTime.of(0, 0)),
                pointParams(1, 1, LocalTime.of(0, 1), LocalTime.of(1, 0)),
                pointParams(2, 59, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                pointParams(3, 60, LocalTime.of(4, 0), LocalTime.of(5, 0))
            )))
            .containsExactlyInAnyOrder(
                routeSchedulePointDto(0, 0, LocalTime.of(23, 30), LocalTime.of(0, 0)),
                routeSchedulePointDto(1, 1, LocalTime.of(0, 1), LocalTime.of(1, 0)),
                routeSchedulePointDto(2, 1, LocalTime.of(2, 0), LocalTime.of(3, 0)),
                routeSchedulePointDto(3, 1, LocalTime.of(4, 0), LocalTime.of(5, 0))
            );
    }

    private PointParams pointParams(int index, Integer transitionTime, LocalTime startTime, LocalTime endTime) {
        PointParams params = new PointParams();
        params.setIndex(index);
        params.setTransitionTime(transitionTime);
        params.setArrivalStartTime(startTime);
        params.setArrivalEndTime(endTime);
        return params;
    }

    private RouteSchedulePointDto routeSchedulePointDto(
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
