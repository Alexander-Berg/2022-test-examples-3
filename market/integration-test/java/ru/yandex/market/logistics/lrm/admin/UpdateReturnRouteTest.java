package ru.yandex.market.logistics.lrm.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.lrm.admin.model.request.CreateReturnRouteDto;
import ru.yandex.market.logistics.lrm.admin.model.request.UpdateReturnRouteDto;
import ru.yandex.market.logistics.lrm.repository.ydb.converter.ReturnRouteHistoryConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.lrm.service.route.RouteService;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DisplayName("Обновление маршрута для сегмента")
@DatabaseSetup("/database/admin/update-route/before/prepare.xml")
@DatabaseSetup(
    value = "/database/admin/update-route/before/route_history_segment_ids.xml",
    type = DatabaseOperation.REFRESH
)
class UpdateReturnRouteTest extends ReturnRouteChangingTest {
    private static final String ROUTE_UUID = "e9c2433f-c7a9-44f4-9b47-82bc487eb94a";
    private static final String ROUTE_IN_YDB_JSON_PATH =
        "json/admin/change-route/update/request/sorting_center_existing.json";
    private static final long ACTUAL_ROUTE_HISTORY_ID = 102L;

    @Autowired
    private ReturnRouteHistoryConverter converter;

    @Autowired
    private RouteService routeService;

    @MethodSource
    @DisplayName("Невалидный json маршрута")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void invalidRoute(
        @SuppressWarnings("unused") String displayName,
        String jsonPath,
        String errorMessagePath
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            updateRoute(jsonPath, 123456789L),
            HttpStatus.EXPECTATION_FAILED.value(),
            errorMessagePath
        );
    }


    @MethodSource
    @DisplayName("Маршрут не должен обновиться, некорректный запрос")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void noNeedToChangeRoute(
        @SuppressWarnings("unused") String displayName,
        Long routeHistoryId,
        String errorMessage
    ) {
        insertRouteToYdb();
        RestAssuredTestUtils.assertUnprocessableEntityServerError(
            updateRoute(ROUTE_IN_YDB_JSON_PATH, routeHistoryId),
            errorMessage
        );
    }

    @Nonnull
    private static Stream<Arguments> noNeedToChangeRoute() {
        return Stream.of(
            Arguments.of(
                "Маршрут не изменился",
                ACTUAL_ROUTE_HISTORY_ID,
                "Маршрут не был изменён"
            ),
            Arguments.of(
                "Неактуальный маршрут для сегмента не меняется",
                101L,
                "Маршрут не актуален для сегмента"
            ),
            Arguments.of(
                "Маршрут не привязан к сегменту",
                103L,
                "К маршруту не привязан сегмент"
            )
        );
    }

    @Test
    @DisplayName("Маршрут не найден в ydb")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void routeNotFoundInYdb() {
        RestAssuredTestUtils.assertNotFoundError(
            updateRoute(ROUTE_IN_YDB_JSON_PATH, ACTUAL_ROUTE_HISTORY_ID),
            "Failed to find RETURN_ROUTE with id e9c2433f-c7a9-44f4-9b47-82bc487eb94a"
        );
    }

    @Test
    @DisplayName("Отсутствует запись в истории маршрутов с заданным идентификатором")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void routeHistoryNotExists() {
        RestAssuredTestUtils.assertNotFoundError(
            updateRoute(ROUTE_IN_YDB_JSON_PATH, 999L),
            "Failed to find RETURN_BOX_ROUTE_HISTORY with id 999"
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Маршрут обновлён, создана таска на парсинг маршрута")
    @ExpectedDatabase(
        value = "/database/admin/update-route/after/route_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sucessUpdate() {
        insertRouteToYdb();
        RestAssuredTestUtils.assertJsonResponse(
            updateRoute("json/admin/change-route/update/request/updated_route.json", ACTUAL_ROUTE_HISTORY_ID),
            "json/admin/change-route/update/response/updated_route.json"
        );

        ReturnRouteHistoryTableDescription.ReturnRouteHistory routeHistory = routeService.findRouteByUuid(
            UUID.fromString(TEST_UUID)
        );
        softly.assertThat(routeHistory.route())
            .isEqualTo(objectMapper.readTree(extractFileContent(
                "json/admin/change-route/update/request/updated_route.json"
            )));
    }

    @Test
    @DisplayName("Невалидный json")
    void invalidJson() {
        RestAssuredTestUtils.assertInternalServerError(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(
                     CreateReturnRouteDto.builder().route(FormattedTextObject.of("{")).build()
                )
                .put("/admin/return-routes/1"),
            "Ошибка при парсинге маршрута"
        );
    }

    @Nonnull
    private Response updateRoute(String newRouteJsonPath, Long routeHistoryId) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                new UpdateReturnRouteDto().setRoute(FormattedTextObject.of(extractFileContent(newRouteJsonPath)))
            )
            .put("/admin/return-routes/" + routeHistoryId);
    }

    private void insertRouteToYdb() {
        ydbInsert(
            routeHistoryTableDescription,
            List.of(new ReturnRouteHistoryTableDescription.ReturnRouteHistory(
                ROUTE_UUID,
                jsonFile(ROUTE_IN_YDB_JSON_PATH),
                Instant.parse("2021-12-11T10:09:08.00Z")
            )),
            converter::convert
        );
    }
}
