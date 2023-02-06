package ru.yandex.market.tsup.service.data_provider.entity.route.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleSubtypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulesListDto;
import ru.yandex.market.tpl.common.data_provider.primitive.SimpleIdFilter;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleSubtype;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleListDto;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.ScheduleInfoDto;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.TransportInfoDto;

import static ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleStatusDto.ACTIVE;
import static ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleStatusDto.DRAFT;
import static ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleTypeDto.LINEHAUL;

class RouteScheduleListFrontProviderTest extends AbstractContextualTest {

    @Autowired
    private RouteScheduleListFrontProvider routeScheduleListFrontProvider;
    @Autowired
    private TransportManagerClient transportManagerClient;

    @Test
    void provide() {
        var routeSchedules = List.of(
            routeScheduleDto("schedule-1", 1L, RouteScheduleStatusDto.ACTIVE),
            routeScheduleDto("schedule-2", 1L, RouteScheduleStatusDto.DRAFT),
            routeScheduleDto("schedule-3", 1L, RouteScheduleStatusDto.ACTIVE)
        );
        Mockito.when(transportManagerClient.searchSchedulesByRoute(1L))
            .thenReturn(new RouteSchedulesListDto(routeSchedules));

        softly.assertThat(routeScheduleListFrontProvider.provide(new SimpleIdFilter(1L),
                null
            )
        ).isEqualTo(
            new RouteScheduleListDto(
                List.of(
                    ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleDto
                        .builder()
                        .subtype(RouteScheduleSubtype.MAIN)
                        .points(List.of(
                                routeSchedulePointDtoTsup(0, null),
                                routeSchedulePointDtoTsup(1, 1380),
                                routeSchedulePointDtoTsup(2, 1380)
                            )
                        )
                        .type(LINEHAUL)
                        .status(ACTIVE)
                        .routeId(1L)
                        .name("schedule-1")
                        .firstPointTime(LocalTime.of(9, 0))
                        .scheduleInfo(ScheduleInfoDto.builder()
                            .daysOfWeek(List.of(1, 2, 3))
                            .holidays(List.of())
                            .startDate(LocalDate.parse("2022-01-01"))
                            .holidaysIntervals(List.of())
                            .regular(true)
                            .build())
                        .transportInfo(TransportInfoDto.builder()
                            .priceRuble(10L)
                            .movingPartnerId(22154342L)
                            .movingPartnerName("ВелесТорг")
                            .maxPallet(33)
                            .build())
                        .isActive(true)
                        .build(),
                    ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleDto
                        .builder()
                        .points(List.of(
                                routeSchedulePointDtoTsup(0, null),
                                routeSchedulePointDtoTsup(1, 1380),
                                routeSchedulePointDtoTsup(2, 1380)
                            )
                        )
                        .type(LINEHAUL)
                        .subtype(RouteScheduleSubtype.MAIN)
                        .status(ACTIVE)
                        .routeId(1L)
                        .name("schedule-3")
                        .firstPointTime(LocalTime.of(9, 0))
                        .scheduleInfo(ScheduleInfoDto.builder()
                            .daysOfWeek(List.of(1, 2, 3))
                            .holidays(List.of())
                            .startDate(LocalDate.parse("2022-01-01"))
                            .holidaysIntervals(List.of())
                            .regular(true)
                            .build())
                        .transportInfo(TransportInfoDto.builder()
                            .priceRuble(10L)
                            .movingPartnerId(22154342L)
                            .movingPartnerName("ВелесТорг")
                            .maxPallet(33)
                            .build())
                        .isActive(true)
                        .build(),
                    ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleDto
                        .builder()
                        .points(List.of(
                                routeSchedulePointDtoTsup(0, null),
                                routeSchedulePointDtoTsup(1, 1380),
                                routeSchedulePointDtoTsup(2, 1380)
                            )
                        )
                        .type(LINEHAUL)
                        .subtype(RouteScheduleSubtype.MAIN)
                        .status(DRAFT)
                        .routeId(1L)
                        .name("schedule-2")
                        .firstPointTime(LocalTime.of(9, 0))
                        .scheduleInfo(ScheduleInfoDto.builder()
                            .daysOfWeek(List.of(1, 2, 3))
                            .holidays(List.of())
                            .startDate(LocalDate.parse("2022-01-01"))
                            .holidaysIntervals(List.of())
                            .regular(true)
                            .build())
                        .transportInfo(TransportInfoDto.builder()
                            .priceRuble(10L)
                            .movingPartnerId(22154342L)
                            .movingPartnerName("ВелесТорг")
                            .maxPallet(33)
                            .build())
                        .isActive(false)
                        .build())
            )
        );
    }

    private RouteScheduleDto routeScheduleDto(String name, long routeId, RouteScheduleStatusDto status) {
        return RouteScheduleDto.builder()
            .name(name)
            .routeId(routeId)
            .type(RouteScheduleTypeDto.LINEHAUL)
            .subtype(RouteScheduleSubtypeDto.MAIN)
            .status(status)
            .price(1000L)
            .movingPartnerId(22154342L)
            .maxPallet(33)
            .startDate(LocalDate.parse("2022-01-01"))
            .daysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
            .points(List.of(
                routeSchedulePointDto(0, 0),
                routeSchedulePointDto(1, 1),
                routeSchedulePointDto(2, 2)
            ))
            .build();
    }

    private RouteSchedulePointDto routeSchedulePointDto(int index, int dayOffset) {
        return RouteSchedulePointDto.builder()
            .index(index)
            .timeTo(LocalTime.of(10, 0))
            .timeFrom(LocalTime.of(9, 0))
            .daysOffset(dayOffset)
            .build();
    }

    private ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteSchedulePointDto
    routeSchedulePointDtoTsup(int index, Integer transitionTime) {
        return ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteSchedulePointDto.builder()
            .index(index)
            .timeTo(LocalTime.of(10, 0))
            .timeFrom(LocalTime.of(9, 0))
            .transitionTime(transitionTime)
            .build();
    }
}
