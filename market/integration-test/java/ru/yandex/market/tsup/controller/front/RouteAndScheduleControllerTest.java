package ru.yandex.market.tsup.controller.front;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointPairDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleHolidayDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulesListDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteShortcutDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteUpdateNameDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.ScheduleUpdateInfoDto;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.controller.dto.RouteUpdateNameRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class RouteAndScheduleControllerTest extends AbstractContextualTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TransportManagerClient tmClient;

    @BeforeEach
    void setUp() {

        Mockito.when(tmClient.searchRoutes(any(), any()))
            .thenReturn(page(
                List.of(
                    new RouteShortcutDto(
                        1L,
                        "Первый маршрут",
                        RouteStatusDto.ACTIVE,
                        10L,
                        20L,
                        List.of(10L, 20L),
                        true,
                        true),
                    new RouteShortcutDto(
                        2L,
                        "Второй маршрут",
                        RouteStatusDto.ACTIVE,
                        11L,
                        21L,
                        List.of(11L, 21L),
                        false,
                        false)
                )
            ));

        Set<Long> ids = Set.of(10L, 20L, 11L, 21L);
        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(List.of(
                        partnerResponse(1L, "Софьино 1"),
                        partnerResponse(2L, "Софьино 2"),
                        partnerResponse(3L, "Софьино 3")
                ));

        Mockito.when(lmsClient.getLogisticsPoints(logisticsPointFilter(ids)))
            .thenReturn(List.of(
                logisticsPointResponse(10L, "первый", 1L, ""),
                logisticsPointResponse(20L, "второй", 2L, ""),
                logisticsPointResponse(11L, "третий", 3L, "")
            ));

        Mockito.when(tmClient.searchSchedulesByRoute(1L)).thenReturn(new RouteSchedulesListDto(
            List.of(routeScheduleDtoRegular(1L, null), routeScheduleDtoRegular(2L,
                LocalDate.of(2300, 1, 1)), routeScheduleDtoRegular(3L, LocalDate.of(2021, 1, 2)))
        ));
        clock.setFixed(Instant.parse("2021-01-03T18:35:24.00Z"), ZoneId.of("Europe/Moscow"));
    }

    @Test
    void search() throws Exception {

        mockMvc.perform(get("/routes?page=2&size=10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/route_search.json", true));
    }

    @Test
    void searchByActivityStatus() throws Exception {

        mockMvc.perform(get("/routes?page=2&size=10")
                        .param("activityStatus", "ALL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/route/route_search.json", true));
    }

    @SneakyThrows
    @Test
    void searchRouteById() {
        Mockito.when(lmsClient.getLogisticsPoints(any()))
            .thenReturn(List.of(
                logisticsPointResponse(100L, "первая", 1L, "Новинский бульвар 8"),
                logisticsPointResponse(200L, "вторая", 2L, "Новинский бульвар 9"),
                logisticsPointResponse(101L, "третья", 3L, "Новинский бульвар 10"),
                logisticsPointResponse(202L, "четвертая", 4L, "Новинский бульвар 11")
            ));

        Mockito.when(lmsClient.searchPartners(any()))
            .thenReturn(
                List.of(
                    partnerResponse(1L, "partner-name-1"),
                    partnerResponse(2L, "partner-name-2"),
                    partnerResponse(3L, "partner-name-3"),
                    partnerResponse(6L, "partner-name-4")
                )
            );

        Mockito.when(tmClient.searchRouteById(1L))
            .thenReturn(routeDto(1L));

        mockMvc.perform(get("/routes/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/route_1.json", JSONCompareMode.STRICT_ORDER));
    }


    @SneakyThrows
    @Test
    void archiveRoute() {
        mockMvc.perform(post("/routes/1/changeStatus")
                .param("status", "ARCHIVE")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(tmClient, Mockito.times(1))
            .changeRouteStatus(1, RouteStatusDto.ARCHIVE);
    }

    @SneakyThrows
    @Test
    void deleteRoute() {
        mockMvc.perform(delete("/routes/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(tmClient, Mockito.times(1)).deleteRouteById(1);
    }

    @SneakyThrows
    @Test
    void updateRouteName() {
        Mockito.when(lmsClient.getLogisticsPoints(any()))
                .thenReturn(List.of(
                        logisticsPointResponse(100L, "первая", 1L, "Новинский бульвар 8"),
                        logisticsPointResponse(200L, "вторая", 2L, "Новинский бульвар 9"),
                        logisticsPointResponse(101L, "третья", 3L, "Новинский бульвар 10"),
                        logisticsPointResponse(202L, "четвертая", 4L, "Новинский бульвар 11")
                ));

        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(
                        List.of(
                                partnerResponse(1L, "partner-name-1"),
                                partnerResponse(2L, "partner-name-2"),
                                partnerResponse(3L, "partner-name-3"),
                                partnerResponse(6L, "partner-name-4")
                        )
                );

        Mockito.when(tmClient.changeRouteName(anyLong(), any()))
                .thenReturn(routeDto(1L));

        var newRouteName = "ФФЦ Минас Тирит - СЦ Осгилиат";
        var request = new RouteUpdateNameRequest(newRouteName);

        mockMvc.perform(post("/routes/1/changeName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(IntegrationTestUtils.jsonContent("fixture/route/route_1.json", JSONCompareMode.STRICT_ORDER));

        Mockito.verify(tmClient, Mockito.times(1))
                .changeRouteName(1, new RouteUpdateNameDto(newRouteName));
    }

    @SneakyThrows
    @Test
    void deleteRouteFailed() {
        Mockito.doThrow(new HttpTemplateException(404, "Нельзя удалять маршруты, к которым привязаны расписания"))
            .when(tmClient).deleteRouteById(2);
        mockMvc.perform(delete("/routes/2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(MockMvcResultMatchers.content().json("{\"message\": \"Нельзя удалять маршруты, к которым " +
                "привязаны расписания\"}"));
        Mockito.verify(tmClient, Mockito.times(1)).deleteRouteById(2);
    }

    @SneakyThrows
    @Test
    void searchScheduleByRoute() {
        mockMvc.perform(get("/routes/schedule/byRoute/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/schedule/route_schedules.json",
                JSONCompareMode.LENIENT));
    }

    @SneakyThrows
    @Test
    void scheduleByRoute() {
        mockMvc.perform(get("/routes/1/schedule")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/schedule/route_schedules.json",
                JSONCompareMode.LENIENT));
    }

    @SneakyThrows
    @Test
    void getUpdateInfo() {
        clock.setFixed(Instant.parse("2019-01-01T18:00:00.00Z"), ZoneId.systemDefault());
        Mockito.when(tmClient.getUpdateScheduleInfo(1L)).thenReturn(new ScheduleUpdateInfoDto(
            LocalDate.of(2021, 1, 1)
        ));
        mockMvc.perform(get("/routes/schedule/1/updateInfo")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/schedule/get_update_info.json"));
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/repository/schedule/after/turn_off.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnOffSchedule() {
        clock.setFixed(Instant.parse("2019-01-01T18:00:00.00Z"), ZoneId.systemDefault());
        Mockito.when(tmClient.getScheduleInfo(1L)).thenReturn(new RouteScheduleDto());
        mockMvc.perform(post("/routes/schedule/1/turnOff?lastDate=2021-01-01")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/repository/schedule/after/turn_off_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnOffScheduleError() {
        clock.setFixed(Instant.parse("2019-01-01T18:00:00.00Z"), ZoneId.systemDefault());
        var schedule = new RouteScheduleDto();
        schedule.setEndDate(LocalDate.of(2020, 1, 1));
        Mockito.when(tmClient.getScheduleInfo(1L)).thenReturn(schedule);
        mockMvc.perform(post("/routes/schedule/1/turnOff?lastDate=2021-01-01")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @SneakyThrows
    @Test
    void transportationTypes() {
        mockMvc
            .perform(get("/routes/schedule/options/scheduleType"))
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/schedule/types.json"));
    }

    @Test
    void getSubtypesTest() throws Exception {
        mockMvc
            .perform(get("/routes/schedule/subtypes"))
            .andExpect(IntegrationTestUtils.jsonContent("fixture/route/schedule/route_schedule_subtypes.json"));
    }

    private static Page<RouteShortcutDto> page(List<RouteShortcutDto> dtos) {
        Page<RouteShortcutDto> page = new Page<>();
        page.setData(dtos).setTotalElements(dtos.size());

        return page;
    }

    private static LogisticsPointFilter logisticsPointFilter(Set<Long> ids) {
        return LogisticsPointFilter.newBuilder()
            .ids(ids)
            .build();
    }

    private static LogisticsPointResponse logisticsPointResponse(Long id, String name, Long partnerId, String address) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .name(name)
            .active(true)
            .partnerId(partnerId)
            .address(
                Address.newBuilder()
                    .shortAddressString(address)
                    .build()
            )
            .build();
    }

    private static PartnerResponse partnerResponse(Long id, String name) {
        return PartnerResponse.newBuilder()
            .id(id)
            .name(name)
            .build();
    }

    private static RouteDto routeDto(Long id) {
        return RouteDto.builder()
            .id(id)
            .name("route-1")
            .status(RouteStatusDto.ACTIVE)
            .pointPairs(List.of(
                routePointPairDto(1, 100L, 0, 1L),
                routePointPairDto(3, 101L, 2, 3L)
            ))
            .build();
    }

    private static RoutePointPairDto routePointPairDto(long id, Long logPointId, int index, Long partnerId) {
        return RoutePointPairDto.builder()
            .outboundPoint(
                routePointDto(id, logPointId, index, partnerId)
            )
            .inboundPoint(
                routePointDto(id * 2, logPointId * 2, index + 1, partnerId * 2)
            )
            .build();
    }

    private static RoutePointDto routePointDto(Long id, Long logPointId, int index, Long partnerId) {
        return RoutePointDto.builder()
            .id(id)
            .logisticPointId(logPointId)
            .index(index)
            .partnerId(partnerId)
            .build();
    }

    private RouteScheduleDto routeScheduleDtoRegular(Long id, LocalDate endDate) {
        return RouteScheduleDto.builder()
            .routeId(1L)
            .startDate(LocalDate.of(2021, 1, 1))
            .endDate(endDate)
            .name("route_schedule" + id)
            .id(id)
            .status(RouteScheduleStatusDto.ACTIVE)
            .type(RouteScheduleTypeDto.LINEHAUL)
            .movingPartnerId(22154342L)
            .maxPallet(33)
            .price(10_000L)
            .daysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
            .holidays(Set.of(
                new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 1)),
                new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 2)),
                new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 3)),
                new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 3, 8))
            ))
            .points(List.of(
                routeSchedulePointDtoTm(0, 0, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                routeSchedulePointDtoTm(1, 1, LocalTime.of(12, 20), LocalTime.of(13, 0)),
                routeSchedulePointDtoTm(2, 2, LocalTime.of(12, 20), LocalTime.of(13, 0)),
                routeSchedulePointDtoTm(3, 3, LocalTime.of(12, 20), LocalTime.of(13, 0))
            ))
            .build();
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
