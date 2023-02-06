package ru.yandex.market.logistics.management.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("CRUD возвратных маршрутов через админку")
@DatabaseSetup({
    "/data/controller/admin/returnRoute/prepare_data.xml",
    "/data/controller/admin/returnRoute/tm_movement_service_code.xml"
})
@ParametersAreNonnullByDefault
class AdminReturnRouteControllerTest extends AbstractContextualTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchRoutesArguments")
    @SneakyThrows
    @DisplayName("Получение списка возвратных маршрутов по фильтру")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE)
    void searchRoutes(String caseName, MultiValueMap<String, String> params, String expectedResponseFile) {
        mockMvc.perform(get("/admin/lms/return-route")
            .params(params))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/returnRoute/response/" + expectedResponseFile));
    }

    @Nonnull
    private static Stream<Arguments> searchRoutesArguments() {
        return Stream.of(
            Arguments.of("Поиск без параметров", wrapParams(Map.of()), "get_all.json"),
            Arguments.of("Поиск по отправителю", wrapParams(Map.of("partnerFrom", "5")), "get_by_partnerFrom.json"),
            Arguments.of("Поиск по получателю", wrapParams(Map.of("partnerTo", "6")), "get_by_partnerTo.json"),
            Arguments.of("Поиск с пустым результатом", wrapParams(Map.of("partnerFrom", "100500")),
                "get_empty_list.json")
        );
    }

    @Nonnull
    private static MultiValueMap<String, String> wrapParams(Map<String, String> params) {
        return new LinkedMultiValueMap<>(params.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> List.of(e.getValue())
            )));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE)
    void getRoute() {
        mockMvc.perform(get("/admin/lms/return-route/104_102_101"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/returnRoute/response/104_102_101.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение несуществующего возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE)
    void getRoute404() {
        mockMvc.perform(get("/admin/lms/return-route/103_102_101"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find logistic route with id=103_102_101"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное удаление возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/after/deletion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteSuccessfully() {
        mockMvc.perform(delete("/admin/lms/return-route/104_102_101"))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Попытка удаления несуществующего маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNonexistentRoute() {
        mockMvc.perform(delete("/admin/lms/return-route/100500_100600_100700"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find logistic route with id=100500_100600_100700"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Попытка удаления маршрута, не соответствующего спецификации возвратного")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteUnsuitableRoute() {
        mockMvc.perform(delete("/admin/lms/return-route/104_102"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Route with id=104_102 is not \"Возвратный маршрут\""));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение формы создания нового возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    void getNewRouteForm() {
        mockMvc.perform(get("/admin/lms/return-route/new"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/returnRoute/response/new.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание нового возвратного маршрута с переиспользованием сегменов складов")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/after/creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRouteWithReusableSegments() {
        mockMvc.perform(post("/admin/lms/return-route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"logisticPointFrom\":10005, \"logisticPointTo\":10006}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/return-route/105_1_106"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание новго возвратного маршрута при наличии дублирующихся сегменов склада СЦ")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @DatabaseSetup(type = DatabaseOperation.INSERT,
        value = "/data/controller/admin/returnRoute/duplicate_sc_segment.xml")
    void createRouteWithDuplicatedSortingCenterSegments() {
        mockMvc.perform(post("/admin/lms/return-route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"logisticPointFrom\":10005, \"logisticPointTo\":10006}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", anyOf(
                equalTo("http://localhost/admin/lms/return-route/105_1_106"),
                equalTo("http://localhost/admin/lms/return-route/105_1_126")
            )));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка создания новго возвратного маршрута при наличии дублирующихся сегменов склада ФФ")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @DatabaseSetup(type = DatabaseOperation.INSERT,
        value = "/data/controller/admin/returnRoute/duplicate_ff_segment.xml")
    void cantCreateRouteWithDuplicatedFulfillmentSegments() {
        mockMvc.perform(post("/admin/lms/return-route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"logisticPointFrom\":10005, \"logisticPointTo\":10006}"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Звену маршрута [type=WAREHOUSE, partnerId=6, logisticPointId=10006] " +
                "подходит сразу несколько логистических сегментов: [106, 126]"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание новго возвратного маршрута с созданием всех сегментов")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/after/creation_from_scratch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createRouteFromScratch() {
        mockMvc.perform(post("/admin/lms/return-route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"logisticPointFrom\":10011, \"logisticPointTo\":10010}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/return-route/1_2_3"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getInvalidCreationRequests")
    @SneakyThrows
    @DisplayName("Попытка создания невалидного возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @DatabaseSetup("/data/controller/admin/returnRoute/prepare_data_invalid_creation.xml")
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/prepare_data_invalid_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/returnRoute/no_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateInvalid(String caseName, Integer pointFrom, Integer pointTo, String message) {
        mockMvc.perform(post("/admin/lms/return-route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"logisticPointFrom\":" + pointFrom + ", \"logisticPointTo\":" + pointTo + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(message));

    }

    private static Stream<Arguments> getInvalidCreationRequests() {
        return Stream.of(
            Arguments.of("Без точки отправления", null, 10010,
                "Для логистического сегмента не указан ни партнер, ни логистическая точка"),
            Arguments.of("Точка отправления - ПВЗ", 10021, 10010,
                "Для сегмента [партнер 2, логистическая точка 10021] должна быть указана логистическая точка одного " +
                    "из типов: [WAREHOUSE]"),
            Arguments.of("Партнер отправления - FULFILLMENT", 10011, 10010,
                "Для сегмента [партнер 1, логистическая точка 10011] должен быть указан партнер одного из типов: " +
                    "[DELIVERY, SORTING_CENTER]"),
            Arguments.of("Партнер отправления - DROPSHIP", 10040, 10010,
                "Для сегмента [партнер 4, логистическая точка 10040] должен быть указан партнер одного из типов: " +
                    "[DELIVERY, SORTING_CENTER]"),
            Arguments.of("Партнер отправления - SUPPLIER", 10050, 10010,
                "Для сегмента [партнер 5, логистическая точка 10050] должен быть указан партнер одного из типов: " +
                    "[DELIVERY, SORTING_CENTER]"),
            Arguments.of("Без точки получения", 10020, null,
                "Для логистического сегмента не указан ни партнер, ни логистическая точка"),
            Arguments.of("Точка получения - ПВЗ", 10020, 10011,
                "Для сегмента [партнер 1, логистическая точка 10011] должна быть указана логистическая точка одного " +
                    "из типов: [WAREHOUSE]"),
            Arguments.of("Партнер получения - DELIVERY", 10030, 10020,
                "Для сегмента [партнер 2, логистическая точка 10020] должен быть указан партнер одного из типов: " +
                    "[FULFILLMENT]"),
            Arguments.of("Партнер получения - SORTING_CENTER", 10020, 10030,
                "Для сегмента [партнер 3, логистическая точка 10030] должен быть указан партнер одного из типов: " +
                    "[FULFILLMENT]"),
            Arguments.of("Партнер получения - DROPSHIP", 10020, 10040,
                "Для сегмента [партнер 4, логистическая точка 10040] должен быть указан партнер одного из типов: " +
                    "[FULFILLMENT]"),
            Arguments.of("Партнер получения - SUPPLIER", 10020, 10050,
                "Для сегмента [партнер 5, логистическая точка 10050] должен быть указан партнер одного из типов: " +
                    "[FULFILLMENT]")
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Активация возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/returnRoute/active_service.xml")
    void testActivation() {
        mockMvc.perform(put("/admin/lms/return-route/104_102_101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":true}"))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Активация возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @DatabaseSetup({
        "/data/controller/admin/returnRoute/prepare_data.xml",
        "/data/controller/admin/returnRoute/tm_movement_service_code.xml",
        "/data/controller/admin/returnRoute/active_service.xml"
    })
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/returnRoute/inactive_service.xml")
    void testDeactivation() {
        mockMvc.perform(put("/admin/lms/return-route/104_102_101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":false}"))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение расписания")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE)
    @DatabaseSetup({
        "/data/controller/admin/returnRoute/prepare_data.xml",
        "/data/controller/admin/returnRoute/tm_movement_service_code.xml",
        "/data/controller/admin/returnRoute/active_service.xml",
        "/data/controller/admin/returnRoute/schedule_days.xml",
        "/data/controller/admin/returnRoute/other_schedule.xml"
    })
    void testGetSchedule() {
        mockMvc.perform(get("/admin/lms/return-route/104_102_101/schedule"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/returnRoute/response/get_schedule.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение формы создания расписания для возвратного маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    void getNewScheduleForm() {
        mockMvc.perform(get("/admin/lms/return-route/104_102_101/schedule/new"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/returnRoute/response/new_schedule.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Добавление первого расписания")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/returnRoute/inactive_service.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/returnRoute/after/schedule_days.xml")
    void testAddSchedule() {
        mockMvc.perform(post("/admin/lms/return-route/104_102_101/schedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"monday\":\"12:00-14:00\", \"wednesday\":\"15:00-18:00\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", endsWith("/admin/lms/return-route/104_102_101")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Удаление расписания")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    @DatabaseSetup({
        "/data/controller/admin/returnRoute/prepare_data.xml",
        "/data/controller/admin/returnRoute/tm_movement_service_code.xml",
        "/data/controller/admin/returnRoute/active_service.xml",
        "/data/controller/admin/returnRoute/schedule_days.xml",
        "/data/controller/admin/returnRoute/other_schedule.xml"
    })
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/returnRoute/after/schedule_days_after_deletion.xml")
    void testDeleteSchedule() {
        mockMvc.perform(post("/admin/lms/return-route/104_102_101/schedule/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ids\":[2]}"))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Невозможность создания дублирующего маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_RETURN_ROUTE_EDIT)
    void testDuplicateRouteCreation() {
        mockMvc.perform(post("/admin/lms/return-route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"logisticPointFrom\":10005, \"logisticPointTo\":10001}"))
            .andExpect(status().isConflict())
            .andExpect(status().reason("Requested route already exists"));
    }
}
