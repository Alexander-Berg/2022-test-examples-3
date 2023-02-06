package ru.yandex.market.logistics.lrm.admin;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.repository.ydb.converter.ReturnRouteHistoryConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.ydb.integration.SessionContext;
import ru.yandex.market.ydb.integration.query.YdbQuery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ParametersAreNonnullByDefault
@DatabaseSetup("/database/admin/get-route-by-history-id/before/prepare.xml")
@DatabaseSetup(
    value = "/database/admin/get-route-by-history-id/before/add_segment_to_route_history.xml",
    type = DatabaseOperation.REFRESH
)
@DisplayName("Получение детальной карточки маршрута по идентификатору его истории")
class GetReturnRouteDetailsTest extends AbstractAdminIntegrationTest {
    private static final String GET_RETURN_ROUTE_BY_ID_PATH = "/admin/return-routes/%d";
    private static final String GET_RETURN_ROUTE_GRAPH_PATH = "/admin/return-routes/%d/return-graph";
    private static final String ROUTE_UUID = "4e1853ef-c1c7-43b3-bf06-d56540ce8180";

    @Autowired
    private ReturnRouteHistoryConverter converter;

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Маршрут не найден")
    void routeNotFound(
        @SuppressWarnings("unused") String displayName,
        String path,
        Long id,
        String message
    ) {
        RestAssuredTestUtils.assertNotFoundError(getRouteDetails(path, id), message);
    }

    @Nonnull
    private static Stream<Arguments> routeNotFound() {
        return Stream.of(
            Arguments.of(
                "Получение по ид: Маршрут есть в истории, нет в ydb",
                GET_RETURN_ROUTE_BY_ID_PATH,
                2L,
                "Failed to find RETURN_ROUTE with id e9c2433f-c7a9-44f4-9b47-82bc487eb94a"
            ),
            Arguments.of(
                "Получение по ид: История маршрута не найдена",
                GET_RETURN_ROUTE_BY_ID_PATH,
                12345L,
                "Failed to find RETURN_BOX_ROUTE_HISTORY with id 12345"
            ),
            Arguments.of(
                "Получение графа: Маршрут есть в истории, нет в ydb",
                GET_RETURN_ROUTE_GRAPH_PATH,
                2L,
                "Failed to find RETURN_ROUTE with id e9c2433f-c7a9-44f4-9b47-82bc487eb94a"
            ),
            Arguments.of(
                "Получение графа: История маршрута не найдена",
                GET_RETURN_ROUTE_GRAPH_PATH,
                12345L,
                "Failed to find RETURN_BOX_ROUTE_HISTORY with id 12345"
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибка при обращении к ydb")
    void ydbError(
        @SuppressWarnings("unused") String displayName,
        String path
    ) {
        String ydbErrorMessage = "YDB error";
        doThrow(new RuntimeException(ydbErrorMessage)).when(ydbTemplate).selectFirst(
            any(YdbQuery.YdbLimitedQuery.class),
            any(SessionContext.class),
            any()
        );

        RestAssuredTestUtils.assertInternalServerError(getRouteDetails(path, 1L), ydbErrorMessage);
    }

    @Nonnull
    private static Stream<Arguments> ydbError() {
        return Stream.of(
            Arguments.of(
                "Получение по ид",
                GET_RETURN_ROUTE_BY_ID_PATH
            ),
            Arguments.of(
                "Получение графа",
                GET_RETURN_ROUTE_GRAPH_PATH
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешное получение маршрута")
    void getRouteSuccess(
        @SuppressWarnings("unused") String displayName,
        String path,
        String routeFileContent,
        String expectedJsonPath
    ) {
        insertRouteToYdb(routeFileContent);
        RestAssuredTestUtils.assertJsonResponse(getRouteDetails(path, 1L), expectedJsonPath);
    }

    @Nonnull
    private static Stream<Arguments> getRouteSuccess() {
        return Stream.of(
            Arguments.of(
                "Получение по ид: в YDB маршрут от ПВЗ",
                GET_RETURN_ROUTE_BY_ID_PATH,
                "json/admin/get-return-route/route/pickup_point.json",
                "json/admin/get-return-route/response/by-id/pickup_point_route.json"
            ),
            Arguments.of(
                "Получение по ид: в YDB маршрут в Дропофф",
                GET_RETURN_ROUTE_BY_ID_PATH,
                "json/admin/get-return-route/route/dropoff.json",
                "json/admin/get-return-route/response/by-id/dropoff_route.json"
            ),
            Arguments.of(
                "Получение по ид: в YDB маршрут от СЦ",
                GET_RETURN_ROUTE_BY_ID_PATH,
                "json/admin/get-return-route/route/sorting_center.json",
                "json/admin/get-return-route/response/by-id/sorting_center_route.json"
            ),
            Arguments.of(
                "Получение графа: в YDB маршрут от ПВЗ",
                GET_RETURN_ROUTE_GRAPH_PATH,
                "json/admin/get-return-route/route/pickup_point.json",
                "json/admin/get-return-route/response/graph/pickup_point_route.json"
            ),
            Arguments.of(
                "Получение графа: в YDB маршрут в Дропофф",
                GET_RETURN_ROUTE_GRAPH_PATH,
                "json/admin/get-return-route/route/dropoff.json",
                "json/admin/get-return-route/response/graph/dropoff_route.json"
            ),
            Arguments.of(
                "Получение графа: в YDB маршрут от СЦ",
                GET_RETURN_ROUTE_GRAPH_PATH,
                "json/admin/get-return-route/route/sorting_center.json",
                "json/admin/get-return-route/response/graph/sorting_center_route.json"
            ),
            Arguments.of(
                "Получение графа: в YDB маршрут от СЦ с BACKWARD_WAREHOUSE сегментами",
                GET_RETURN_ROUTE_GRAPH_PATH,
                "json/admin/get-return-route/route/sorting_center_bwh.json",
                "json/admin/get-return-route/response/graph/sorting_center_bwh_route.json"
            )
        );
    }

    private void insertRouteToYdb(String routeFile) {
        ydbInsert(
            routeHistoryTableDescription,
            List.of(new ReturnRouteHistoryTableDescription.ReturnRouteHistory(
                ROUTE_UUID,
                jsonFile(routeFile),
                Instant.parse("2021-12-11T10:09:08.00Z")
            )),
            converter::convert
        );
    }

    @Nonnull
    private Response getRouteDetails(String path, Long id) {
        return RestAssured.get(path.formatted(id));
    }
}
