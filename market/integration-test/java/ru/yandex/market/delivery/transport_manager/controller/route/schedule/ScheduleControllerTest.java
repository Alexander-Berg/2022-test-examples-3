package ru.yandex.market.delivery.transport_manager.controller.route.schedule;

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
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
public class ScheduleControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешное создание расписания")
    @DatabaseSetup("/repository/route/after/create_route.xml")
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/create_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route_schedule/after/refresh_trips_tasks_after_creation.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRouteScheduleSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/schedule/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/create/request.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/create/response.json", "hash"));
    }

    @Test
    @DisplayName("Успешное создание расписания")
    @DatabaseSetup("/repository/route/after/create_route.xml")
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/create_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route_schedule/after/refresh_trips_tasks_after_creation.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRouteScheduleSuccessWithRunId() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/schedule/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/create/request.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/create/response.json", "hash"));
    }

    @Test
    @DisplayName("Успешное создание разового расписания со слотами")
    @DatabaseSetup("/repository/route/after/create_route.xml")
    @DatabaseSetup(
        value = "/repository/health/dbqueue/empty.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/create_schedule_single_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route_schedule/after/refresh_trips_tasks_after_creation.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRouteSingleTimeScheduleSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/schedule/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/create/request_single_day.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/create/response_single_day.json", "hash"));
    }

    @Test
    @DisplayName("Успешное получение расписания")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    void getScheduleInfo() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routes/schedule/100")
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent(
                    "controller/routes/schedule/get/response.json",
                    "hash", "created", "updated"
                )
            );
    }

    @Test
    @DisplayName("Успешное получение разового расписания со слотами")
    @DatabaseSetup("/repository/route/before/one_schedule_single_day.xml")
    void getScheduleSingleDayInfo() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routes/schedule/100")
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent(
                    "controller/routes/schedule/get/response_single_day.json",
                    "hash", "created", "updated"
                )
            );
    }

    @Test
    @DisplayName("Успешное получение старого перемещения")
    @DatabaseSetup("/repository/route/full_routes.xml")
    @ExpectedDatabase(
        value = "/repository/route/full_routes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createRouteReturnOld() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                put("/routes/createOrGet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/routes/create/request_return_old.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/create/response_return_old.json", true));
    }

    @Test
    @DisplayName("Информация для обновления расписания")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    void getUpdateScheduleInfo() throws Exception {
        clock.setFixed(Instant.parse("2020-01-03T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routes/schedule/updateInfo/100")
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/routes/schedule/update/info_response.json")
            );
    }

    @Test
    @DisplayName("Информация для обновления расписания. Последний рейс сегодня, но он уже закончился")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    void getUpdateScheduleInfoWithTodayLastDate() throws Exception {
        clock.setFixed(Instant.parse("2020-03-01T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
            get("/routes/schedule/updateInfo/100")
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/routes/schedule/update/info_response.json")
            );
    }

    @Test
    @DisplayName("Информация для обновления расписания без рейсов")
    @DatabaseSetup("/repository/route/before/dummy_schedule.xml")
    void getUpdateScheduleInfoDummy() throws Exception {
        clock.setFixed(Instant.parse("2020-01-03T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                get("/routes/schedule/updateInfo/200")
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/routes/schedule/update/info_response_dummy.json")
            );
    }

    @Test
    @DisplayName("Успешное обновление расписания")
    @DatabaseSetup("/repository/route/before/two_trips.xml")
    @DatabaseSetup(
        value = "/repository/route/before/empty_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/update_two_trips.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route/after/update_schedule_db_queue.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSchedule() throws Exception {
        clock.setFixed(Instant.parse("2021-02-28T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                put("/routes/schedule/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Не-ASCII символы некорректно обрабатываются свежими версиями mockMvc
                    // https://github.com/spring-projects/spring-framework/issues/23219
                    // временное решение здесь и ниже - простановка accept, хотя он и deprecated
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/update/request.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/update/response.json", "hash"));
    }


    @Test
    @DisplayName("Успешное обновление разового расписания со слотами")
    @DatabaseSetup("/repository/route/before/two_trips_single_day.xml")
    @DatabaseSetup(
        value = "/repository/route/before/empty_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/update_two_trips_single_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route/after/update_schedule_db_queue.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateScheduleSingleDay() throws Exception {
        clock.setFixed(Instant.parse("2020-01-07T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                put("/routes/schedule/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Не-ASCII символы некорректно обрабатываются свежими версиями mockMvc
                    // https://github.com/spring-projects/spring-framework/issues/23219
                    // временное решение здесь и ниже - простановка accept, хотя он и deprecated
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/update/request_single_day.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/update/response_single_day.json", "hash"));
    }

    @Test
    @DisplayName("Успешное обновление расписания, первое расписание - не активное")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    @DatabaseSetup(
        value = "/repository/route/before/empty_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/update_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route/after/update_schedule_db_queue.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateScheduleOldScheduleNotActive() throws Exception {
        clock.setFixed(Instant.parse("2020-01-07T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                put("/routes/schedule/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/update/request.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/update/response.json", "hash", "created", "updated"));
    }

    @Test
    @DisplayName("Успешное обновление разового расписания со слотами, первое расписание - не активное")
    @DatabaseSetup("/repository/route/before/one_schedule_single_day.xml")
    @DatabaseSetup(
        value = "/repository/route/before/empty_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/update_schedule_single_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route/after/update_schedule_db_queue.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateScheduleOldScheduleNotActiveSingleDay() throws Exception {
        clock.setFixed(Instant.parse("2020-01-07T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                put("/routes/schedule/update")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(extractFileContent("controller/routes/schedule/update/request_single_day.json")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/routes/schedule/update/response_single_day.json", "hash"));

        mockMvc.perform(
                get("/routes/schedule/byRoute/" + 1)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent(
                    "controller/routes/schedule/search/response_search_by_route_after_update_single_date.json",
                    JSONCompareMode.LENIENT,
                    "hash"
                )
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка обновления расписания")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    void updateScheduleError() {
        clock.setFixed(Instant.parse("2020-01-08T15:00:00.01Z"), ZoneOffset.UTC);
        mockMvc.perform(put("/routes/schedule/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/routes/schedule/update/bad_request.json"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Для лайнхолов перемещающий партнер не может быть пустым"));
    }

    @Test
    @DisplayName("Успешное выключения расписания")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    @DatabaseSetup(
        value = "/repository/route/before/empty_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/route/after/turn_off_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(
        value = "/repository/route/after/turn_off_schedule_db_queue.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void turnOffSchedule() throws Exception {
        clock.setFixed(Instant.parse("2020-01-08T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(
                post("/routes/schedule/100/turnOff?lastDate=2020-03-02")

            )
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибочное выключение расписания (уже есть отправленный рейс на эту дату)")
    @DatabaseSetup("/repository/route/before/one_schedule.xml")
    void turnOffScheduleFail() {
        clock.setFixed(Instant.parse("2020-01-08T15:00:00.00Z"), ZoneOffset.UTC);
        mockMvc.perform(post("/routes/schedule/100/turnOff?lastDate=2020-02-29")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/routes/schedule/update/bad_request.json"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Дата измения должна быть больше, чем 2020-03-01"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение расписаний по маршруту")
    @DatabaseSetup(
        value = {
            "/repository/route/full_routes.xml",
            "/repository/route_schedule/full_schedules.xml",
            "/repository/route_schedule/holidays.xml"
        }
    )
    void searchRouteSchedulesByRouteId() {
        mockMvc.perform(
                get("/routes/schedule/byRoute/" + 20)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent(
                    "controller/routes/schedule/search/response_search_by_route.json",
                    JSONCompareMode.LENIENT,
                    "hash"
                )
            );
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное обновление комментов")
    @DatabaseSetup(
        value = {
            "/repository/route/full_routes.xml",
            "/repository/route_schedule/route_schedule_with_comment.xml",
        }
    )
    @ExpectedDatabase(
        value = "/repository/route_schedule/after/route_schedule_with_comment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateCommentTest() {
        mockMvc.perform(
                put("/routes/schedule/" + 100 + "/comment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/routes/schedule/update/request_update_comment.json"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comment").value("Тестовый коммент"));
    }

}
