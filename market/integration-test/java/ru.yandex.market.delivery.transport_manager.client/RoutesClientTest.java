package ru.yandex.market.delivery.transport_manager.client;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteCreationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointCreationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointPairCreationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteSearchFilterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleHolidayDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteSchedulePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteUpdateNameDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.ScheduleUpdateCommentDto;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class RoutesClientTest extends AbstractClientTest {
    @Autowired
    private TransportManagerClient transportManagerClient;

    @Test
    @DisplayName("Создание маршрута")
    void createRoute() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/createOrGet")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/routes/create.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/create.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        List<RoutePointPairCreationDto> routePointPairs = List.of(
            new RoutePointPairCreationDto(
                new RoutePointCreationDto(0, 1, 101),
                new RoutePointCreationDto(2, 5, 105)
            ),
            new RoutePointPairCreationDto(
                new RoutePointCreationDto(1, 2, 102),
                new RoutePointCreationDto(2, 5, 105)
            )
        );

        transportManagerClient.createOrGetRoute(new RouteCreationDto(null, routePointPairs));
    }

    @Test
    @DisplayName("Создание расписания маршрута")
    void createRouteSchedule() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/schedule/create")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/routes/schedule/create.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/schedule/create.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        transportManagerClient.findOrCreateRouteSchedule(RouteScheduleDto.builder()
            .routeId(1L)
            .type(RouteScheduleTypeDto.LINEHAUL)
            .status(RouteScheduleStatusDto.ACTIVE)
            .movingPartnerId(12345L)
            .maxPallet(33)
            .price(100_000_00L)
            .daysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
            .holidays(ImmutableSet.of(
                new RouteScheduleHolidayDto(LocalDate.of(2021, 1, 1)),
                new RouteScheduleHolidayDto(LocalDate.of(2021, 1, 2))
            ))
            .points(getRouteSchedulePoints())
            .build());
    }

    @Test
    @DisplayName("Удаление маршрута")
    void deleteRoute() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/100/delete")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess());

        transportManagerClient.deleteRouteById(100);
    }

    @Test
    @DisplayName("Получение информации для обновления расписания")
    void getSchedule() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/schedule/1")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/schedule/create.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        transportManagerClient.getScheduleInfo(1);
    }

    @Test
    @DisplayName("Получение информации для обновления расписания")
    void getUpdateScheduleInfo() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/schedule/updateInfo/1")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/schedule/update_info.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        transportManagerClient.getUpdateScheduleInfo(1);
    }

    @Test
    @DisplayName("Обновление расписания маршрута")
    void updateRouteSchedule() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/schedule/update")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/routes/schedule/create.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/schedule/create.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        transportManagerClient.updateRouteSchedule(RouteScheduleDto.builder()
            .routeId(1L)
            .type(RouteScheduleTypeDto.LINEHAUL)
            .status(RouteScheduleStatusDto.ACTIVE)
            .daysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
            .holidays(ImmutableSet.of(
                new RouteScheduleHolidayDto(LocalDate.of(2021, 1, 1)),
                new RouteScheduleHolidayDto(LocalDate.of(2021, 1, 2))
            ))
            .movingPartnerId(12345L)
            .maxPallet(33)
            .price(100_000_00L)
            .points(getRouteSchedulePoints())
            .build());
    }

    @Test
    @DisplayName("Обновление расписания маршрута")
    void updateScheduleComment() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/schedule/1/comment")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/routes/schedule/update_comment.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/schedule/create.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        transportManagerClient.updateScheduleComment(1L, new ScheduleUpdateCommentDto("test_comment"));
    }

    @Test
    @DisplayName("Выключение расписания маршрута")
    void tornOffRouteSchedule() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/schedule/100/turnOff?lastDate=2021-01" +
                "-01")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess());

        transportManagerClient.turnOffRouteSchedule(100, LocalDate.of(2021, 1, 1));
    }

    @Test
    @DisplayName("Успешный поиск маршрута")
    void searchRouteSuccess() throws Exception {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/search")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/routes/search.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/routes/search.json"),
                    MediaType.APPLICATION_JSON
                )
            );
        var request = new RouteSearchFilterDto();
        request.setLogisticPointIds(Set.of(101L));
        transportManagerClient.searchRoutes(request, PageRequest.UNPAGED);
    }

    @Test
    @DisplayName("Успешное изменение наименования маршрута")
    void changeNameSuccess() {
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/routes/100/changeName")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonRequestContent("request/routes/change_name.json"))
                .andRespond(
                        withSuccess(
                                extractFileContent("response/routes/schedule/create.json"),
                                MediaType.APPLICATION_JSON
                        )
                );

        transportManagerClient.changeRouteName(100L, new RouteUpdateNameDto("ФФЦ Минас Тирит - СЦ Осгилиат"));
    }

    private static List<RouteSchedulePointDto> getRouteSchedulePoints() {
        return List.of(
            RouteSchedulePointDto.builder()
                .index(0)
                .daysOffset(0)
                .timeFrom(LocalTime.parse("12:30:00"))
                .timeTo(LocalTime.parse("14:00:00"))
                .build(),
            RouteSchedulePointDto.builder()
                .index(1)
                .daysOffset(1)
                .timeFrom(LocalTime.parse("11:30:00"))
                .timeTo(LocalTime.parse("12:00:00"))
                .build(),
            RouteSchedulePointDto.builder()
                .index(2)
                .daysOffset(2)
                .timeFrom(LocalTime.parse("15:30:00"))
                .timeTo(LocalTime.parse("16:00:00"))
                .build(),
            RouteSchedulePointDto.builder()
                .index(3)
                .daysOffset(2)
                .timeFrom(LocalTime.parse("16:00:00"))
                .timeTo(LocalTime.parse("17:00:00"))
                .build()
        );
    }
}
