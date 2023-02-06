package ru.yandex.market.tsup.service.calendaring;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointPairDto;
import ru.yandex.market.tpl.common.data_provider.primitive.SimpleIdFilter;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleStatus;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleSubtype;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleType;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.RouteScheduleModificationPayload;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.ScheduleInfo;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.TransportInfo;
import ru.yandex.market.tsup.service.data_provider.aggregate.route.RouteProvider;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.PointBookingInfoDto;

class CalendaringSlotRequirementsGeneratorTest {

    public static final LocalDate DATE = LocalDate.of(2022, 1, 25);

    @Test
    void getSchedulePointsBookingRequirements() {
        RouteProvider routeProvider = Mockito.mock(RouteProvider.class);

        Mockito
            .when(routeProvider.provide(Mockito.eq(new SimpleIdFilter(TestData.ROUTE_ID)), Mockito.isNull()))
            .thenReturn(
                RouteDto.builder()
                    .pointPairs(List.of(
                        new RoutePointPairDto(
                            RoutePointDto.builder()
                                .id(2L)
                                .index(1)
                                .partnerId(404L)
                                .logisticPointId(100000000)
                                .build(),
                            RoutePointDto.builder()
                                .id(3L)
                                .index(2)
                                .partnerId(171L)
                                .logisticPointId(100000001)
                                .build()
                        ),
                        new RoutePointPairDto(
                            RoutePointDto.builder()
                                .id(1L)
                                .index(0)
                                .partnerId(404L)
                                .logisticPointId(100000000)
                                .build(),
                            RoutePointDto.builder()
                                .id(4L)
                                .index(3)
                                .partnerId(172L)
                                .logisticPointId(100000002)
                                .build()
                        )
                    ))
                    .build()
            );

        CalendaringSlotRequirementsGenerator generator = new CalendaringSlotRequirementsGenerator(routeProvider);

        List<PointBookingInfoDto> actual =
            generator.getSchedulePointsBookingRequirements(new RouteScheduleModificationPayload(
                1L,
                1L,
                new ScheduleInfo(null, List.of(1), DATE, DATE, Collections.emptyList(), Collections.emptyList()),
                new TransportInfo(9001L, 33, 10_000_00L, 10_000_00L),
                RouteScheduleType.XDOC_TRANSPORT,
                RouteScheduleSubtype.MAIN,
                RouteScheduleStatus.ACTIVE,
                List.of(
                    TestData.POINT_PARAMS_1,
                    TestData.POINT_PARAMS_2,
                    TestData.POINT_PARAMS_3,
                    TestData.POINT_PARAMS_4
                ),
                true
            ));

        List<PointBookingInfoDto> expected = List.of(
            TestData.BOOKING_INFO_1_2,
            TestData.BOOKING_INFO_3,
            TestData.BOOKING_INFO_4
        );

        Assertions.assertEquals(
            expected,
            actual
        );

        Mockito.verify(routeProvider).provide(Mockito.eq(new SimpleIdFilter(TestData.ROUTE_ID)), Mockito.isNull());
        Mockito.verifyNoMoreInteractions(routeProvider);
    }
}
