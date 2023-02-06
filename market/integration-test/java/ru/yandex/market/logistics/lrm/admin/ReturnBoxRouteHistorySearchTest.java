package ru.yandex.market.logistics.lrm.admin;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.admin.model.filter.AdminReturnRouteHistorySearchFilterDto;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@ParametersAreNonnullByDefault
@DisplayName("Поиск истории маршрутов по фильтру")
@DatabaseSetup("/database/admin/search-route-history/before/prepare.xml")
class ReturnBoxRouteHistorySearchTest extends AbstractAdminIntegrationTest {

    private static final String SEARCH_ROUTE_HISTORY_PATH = "/admin/return-routes/search";

    @MethodSource
    @DisplayName("Поиск истории маршрутов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void searchRouteHistory(
        @SuppressWarnings("unused") String displayName,
        AdminReturnRouteHistorySearchFilterDto filter,
        String expectedResponsePath
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured
                .given()
                .params(toParams(filter))
                .get(SEARCH_ROUTE_HISTORY_PATH),
            expectedResponsePath
        );
    }

    @Nonnull
    private static Stream<Arguments> searchRouteHistory() {
        return Stream.of(
            Arguments.of(
                "Поиск по идентификатору грузоместа и сегмента",
                filter().setBoxId(1L).setSegmentId(1L),
                "json/admin/search-route-history/box_id_and_segment_id.json"
            ),
            Arguments.of(
                "Поиск только по идентификатору грузоместа",
                filter().setBoxId(2L),
                "json/admin/search-route-history/only_box_id.json"
            ),
            Arguments.of(
                "Поиск только по идентификатору сегмента",
                filter().setSegmentId(2L),
                "json/admin/search-route-history/only_return_segment_id.json"
            ),
            Arguments.of(
                "Поиск с пустым фильтром",
                filter(),
                "json/admin/search-route-history/empty_filter.json"
            )
        );
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() {
        RestAssuredTestUtils.assertJsonParameter(
            RestAssured
                .given()
                .params(Map.of("size", "1", "boxId", "1"))
                .get(SEARCH_ROUTE_HISTORY_PATH),
            "totalCount",
            4
        );
    }

    @Nonnull
    private static AdminReturnRouteHistorySearchFilterDto filter() {
        return new AdminReturnRouteHistorySearchFilterDto();
    }
}
