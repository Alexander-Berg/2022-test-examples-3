package ru.yandex.market.logistics.lrm.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@ParametersAreNonnullByDefault
@DisplayName("Получение гридов для детальной карточки возвратного сегмента")
@DatabaseSetup("/database/admin/get-grid-for-return-segment-detail/before/prepare.xml")
@DatabaseSetup(
    value = "/database/admin/get-grid-for-return-segment-detail/before/add_segment_id_to_route_history.xml",
    type = DatabaseOperation.REFRESH
)
class GetGridForReturnSegmentDetailCardTest extends AbstractAdminIntegrationTest {
    private static final long RETURN_SEGMENT_WITH_ALL_DATA = 1L;
    private static final long RETURN_SEGMENT_WITH_NO_DATA = 2L;

    private static final String GET_STATUS_HISTORY_PATH = "admin/returns/boxes/segments/status-history";
    private static final String GET_ROUTES_HISTORY_PATH = "admin/return-routes/search";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешное получение грида для детальной карточки сегмента")
    void getGridForReturnSegmentDetailCard(
        @SuppressWarnings("unused") String displayName,
        String path,
        Long segmentId,
        String expectedJson
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given()
                .params("segmentId", segmentId)
                .get(path),
            expectedJson
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridForReturnSegmentDetailCard() {
        return Stream.of(
            Arguments.of(
                "Получение истории статусов: у сегмента есть история статусов",
                GET_STATUS_HISTORY_PATH,
                RETURN_SEGMENT_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-segment-detail/has_status_history.json"
            ),
            Arguments.of(
                "Получение истории статусов: у сегмента нет истории статусов",
                GET_STATUS_HISTORY_PATH,
                RETURN_SEGMENT_WITH_NO_DATA,
                "json/admin/get-grid-for-return-segment-detail/no_status_history.json"
            ),
            Arguments.of(
                "Получение истории маршрутов: у сегмента есть история маршрутов",
                GET_ROUTES_HISTORY_PATH,
                RETURN_SEGMENT_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-segment-detail/has_route_history.json"
            ),
            Arguments.of(
                "Получение истории маршрутов: у сегмента нет истории маршрутов",
                GET_ROUTES_HISTORY_PATH,
                RETURN_SEGMENT_WITH_NO_DATA,
                "json/admin/get-grid-for-return-segment-detail/no_route_history.json"
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение грида для несуществующего сегмента")
    void getGridForNonExistingReturnBox(
        @SuppressWarnings("unused") String displayName,
        String path
    ) {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.given()
                .params("segmentId", 123456789L)
                .get(path),
            "Failed to find RETURN_SEGMENT with ids [123456789]"
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridForNonExistingReturnBox() {
        return Stream.of(
            Arguments.of(
                "Получение истории статусов для несуществующего грузоместа",
                GET_STATUS_HISTORY_PATH
            )
        );
    }
}
