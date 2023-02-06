package ru.yandex.market.delivery.transport_manager.controller.route;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
public class RouteControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешное создание маршрута")
    @ExpectedDatabase(
        value = "/repository/route/after/create_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createRouteSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
            put("/routes/createOrGet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/routes/create/request.json")
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/create/response.json", "created", "updated"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_route.xml")
    @DisplayName("Успешный поиск маршрута")
    void searchRouteSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
            post("/routes/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/routes/search/request.json")
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/search/response.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_active_route_unsuccessful.xml")
    @DisplayName("Успешный поиск маршрута, у которого есть расписания, но не активные")
    void searchRouteNotActiveSchedule() throws Exception {
        clock.setFixed(Instant.parse("2021-01-01T21:00:00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                        post("/routes/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extractFileContent("controller/routes/search/request.json")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/routes/search/response_schedule_active.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_route.xml")
    @DisplayName("Успешный поиск маршрута по имени маршрута")
    void searchRouteByNameSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
            post("/routes/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/routes/search/request_search_by_name.json")
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/search/response.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_certain_route.xml")
    @DisplayName("Успешный поиск маршрута по множеству параметров")
    void searchCertainRouteSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
            post("/routes/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/routes/search/request_certain.json")
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/search/response_certain.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_archive_route.xml")
    @DisplayName("Успешный поиск архивных маршрутов")
    void searchArchiveRouteEmpty() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                        post("/routes/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extractFileContent("controller/routes/search/request.json")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/routes/search/empty_response.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_archive_route.xml")
    @DisplayName("Успешный поиск архивных маршрутов")
    void searchArchiveRoute() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                        post("/routes/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extractFileContent("controller/routes/search/request_archive.json")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/routes/search/response_archive.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_active_route_end_date_null.xml")
    @DisplayName("Успешный поиск активных маршрутов с пустой датой окончания")
    void searchOnlyActiveRoute() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/routes/search/request_active.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/search/response_active.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_active_route_end_date_good.xml")
    @DisplayName("Успешный поиск активных маршрутов с корректной датой окончания")
    void searchOnlyActiveRouteWithEndDate() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/routes/search/request_active.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/search/response_active.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_active_route_unsuccessful.xml")
    @DisplayName("Не успешный поиск активных маршрутов")
    void searchOnlyActiveRouteWithNoRoute() throws Exception {
        clock.setFixed(Instant.parse("2021-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/routes/search/request_active.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/search/response_active_empty.json"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/search_certain_route.xml")
    @DisplayName("Неудачное удаление маршрута")
    void deleteRouteFail() throws Exception {
        mockMvc.perform(
            post("/routes/1/delete")
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Нельзя удалять маршруты, к которым привязаны расписания"));
    }

    @Test
    @DatabaseSetup("/repository/route/before/one_route.xml")
    @ExpectedDatabase(
        value = "/repository/route/after/delete_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Удачное удаление маршрута")
    void deleteRouteSuccess() throws Exception {
        mockMvc.perform(
            post("/routes/1/delete")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/repository/route/before/one_route.xml")
    @ExpectedDatabase(
        value = "/repository/route/after/archive_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Удачная архивация маршрута")
    void archiveRouteSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-10-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/1/changeStatus?status=ARCHIVE")
            )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    @DisplayName("Неудачная архивация маршрута")
    void archiveRouteFailure() throws Exception {
        clock.setFixed(Instant.parse("2020-10-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/1/changeStatus?status=ARCHIVE")
            )
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    @DisplayName("Удачная архивация маршрута со старыми расписаниями")
    void archiveRouteOldSchedule() throws Exception {
        clock.setFixed(Instant.parse("2122-10-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/1/changeStatus?status=ARCHIVE")
            )
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное изменение наименования маршрута")
    @DatabaseSetup("/repository/route/before/change_name.xml")
    @ExpectedDatabase(
            value = "/repository/route/after/change_name.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeRouteNameSuccess() {
        mockMvc.perform(
                post("/routes/10/changeName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent("controller/routes/schedule/update/request_change_name.json"))
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка изменение наименования маршрута из за дублей имени")
    @DatabaseSetup("/repository/route/before/change_name.xml")
    void changeRouteNameFail() {
        mockMvc.perform(
                post("/routes/10/changeName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent("controller/routes/schedule/update/request_change_name_fail.json"))
        ).andExpect(status().is4xxClientError());
    }

}
