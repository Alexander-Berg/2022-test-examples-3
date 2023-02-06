package ru.yandex.market.tsup.core.pipeline.cube.route.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteCourierDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleSubtypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.cube.RouteScheduleCreatorCube;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleCourierData;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleId;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleModificationConvertedData;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleStatus;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleSubtype;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleType;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;

public class RouteScheduleCreatorCubeTest extends AbstractContextualTest {

    @Autowired
    private RouteScheduleCreatorCube cube;

    @Autowired
    private TransportManagerClient tmClient;

    @Test
    void execute() {
        var scheduleDto = RouteScheduleDto.builder()
            .routeId(1L)
            .runId(null)
            .type(RouteScheduleTypeDto.LINEHAUL)
            .status(RouteScheduleStatusDto.ACTIVE)
            .points(List.of(RouteSchedulePointDto.builder()
                    .index(0)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(12, 0))
                    .timeTo(LocalTime.of(14, 0))
                    .maxPallets(15)
                .build()
            ))
            .movingPartnerId(100L)
            .maxPallet(33)
            .price(10_000_000L)
            .daysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
            .holidays(Set.of())
            .startDate(LocalDate.of(2021, 1, 1))
            .subtype(RouteScheduleSubtypeDto.MAIN)
            .courier(new RouteCourierDto(1L, 5L))
            .build();

        Mockito.when(tmClient.findOrCreateRouteSchedule(scheduleDto))
            .thenReturn(RouteScheduleDto.builder().id(11L).build());

        var data = new RouteScheduleModificationConvertedData(
            null,
            1L,
            null,
            RouteScheduleType.LINEHAUL,
            RouteScheduleSubtype.MAIN,
            RouteScheduleStatus.ACTIVE,
            List.of(new PointParams(null, null, 0, 60, LocalTime.of(12, 0), LocalTime.of(14, 0), 15, null, null)),
            100L,
            33,
            10_000_000L,
            null,
            List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            null,
            LocalDate.of(2021, 1, 1),
            null,
            new RouteScheduleCourierData(1L, 5L)
        );

        var result = cube.execute(data);
        softly.assertThat(result).isEqualTo(new RouteScheduleId(11L));
    }

    @Test
    void executeWithRunId() {
        var scheduleDto = RouteScheduleDto.builder()
            .routeId(1L)
            .type(RouteScheduleTypeDto.LINEHAUL)
            .subtype(RouteScheduleSubtypeDto.DUTY)
            .runId("10001")
            .status(RouteScheduleStatusDto.ACTIVE)
            .points(List.of(RouteSchedulePointDto.builder()
                .index(0)
                .daysOffset(0)
                .timeFrom(LocalTime.of(12, 0))
                .timeTo(LocalTime.of(14, 0))
                .maxPallets(15)
                .build()
            ))
            .movingPartnerId(100L)
            .maxPallet(33)
            .price(10_000_000L)
            .daysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
            .holidays(Set.of())
            .startDate(LocalDate.of(2021, 1, 1))
            .build();

        Mockito.when(tmClient.findOrCreateRouteSchedule(scheduleDto))
            .thenReturn(RouteScheduleDto.builder().id(11L).build());

        var data = new RouteScheduleModificationConvertedData(
            null,
            1L,
            10001L,
            RouteScheduleType.LINEHAUL,
            RouteScheduleSubtype.DUTY,
            RouteScheduleStatus.ACTIVE,
            List.of(new PointParams(null, null, 0, 60, LocalTime.of(12, 0), LocalTime.of(14, 0), 15, null, null)),
            100L,
            33,
            10_000_000L,
            null,
            List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            null,
            LocalDate.of(2021, 1, 1),
            null,
            null
        );

        var result = cube.execute(data);
        softly.assertThat(result).isEqualTo(new RouteScheduleId(11L));
    }


}
