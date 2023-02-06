package ru.yandex.market.logistics.lrm.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@ParametersAreNonnullByDefault
@DisplayName("Получение гридов для детальной карточки грузоместа")
@DatabaseSetup("/database/admin/get-grid-for-return-box-detail/before/prepare.xml")
@DatabaseSetup(
    value = "/database/admin/get-grid-for-return-box-detail/before/route_to_segments.xml",
    type = DatabaseOperation.REFRESH
)
class GetGridForReturnBoxDetailCardTest extends AbstractAdminIntegrationTest {
    private static final long RETURN_BOX_WITH_ALL_DATA = 1L;
    private static final long RETURN_BOX_WITH_NO_DATA = 2L;

    private static final String GET_SEGMENTS_PATH = "admin/returns/boxes/segments";
    private static final String GET_STATUS_HISTORY_PATH = "admin/returns/boxes/status-history";
    private static final String GET_ROUTES_HISTORY_PATH = "admin/return-routes/search";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешное получение грида для детальной карточки грузоместа")
    void getGridForReturnDetailCard(
        @SuppressWarnings("unused") String displayName,
        String path,
        Long boxId,
        String expectedJson
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given()
                .params("boxId", boxId)
                .get(path),
            expectedJson
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridForReturnDetailCard() {
        return Stream.of(
            Arguments.of(
                "Получение сегментов: у грузоместа есть сегменты",
                GET_SEGMENTS_PATH,
                RETURN_BOX_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-box-detail/has_segments.json"
            ),
            Arguments.of(
                "Получение сегментов: у грузоместа нет сегментов",
                GET_SEGMENTS_PATH,
                RETURN_BOX_WITH_NO_DATA,
                "json/admin/get-grid-for-return-box-detail/no_segments.json"
            ),
            Arguments.of(
                "Получение истории статусов: у грузоместа есть история статусов",
                GET_STATUS_HISTORY_PATH,
                RETURN_BOX_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-box-detail/has_status_history.json"
            ),
            Arguments.of(
                "Получение истории статусов: у грузоместа нет истории статусов",
                GET_STATUS_HISTORY_PATH,
                RETURN_BOX_WITH_NO_DATA,
                "json/admin/get-grid-for-return-box-detail/no_status_history.json"
            ),
            Arguments.of(
                "Получение истории маршрутов: у грузоместа есть история маршрутов",
                GET_ROUTES_HISTORY_PATH,
                RETURN_BOX_WITH_ALL_DATA,
                "json/admin/get-grid-for-return-box-detail/has_route_history.json"
            ),
            Arguments.of(
                "Получение истории маршрутов: у грузоместа нет истории маршрутов",
                GET_ROUTES_HISTORY_PATH,
                RETURN_BOX_WITH_NO_DATA,
                "json/admin/get-grid-for-return-box-detail/no_route_history.json"
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение грида для несуществующего грузоместа")
    void getGridForNonExistingReturnBox(
        @SuppressWarnings("unused") String displayName,
        String path
    ) {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.given()
                .params("boxId", 123456789L)
                .get(path),
            "Failed to find RETURN_BOX with id 123456789"
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridForNonExistingReturnBox() {
        return Stream.of(
            Arguments.of(
                "Получение возвратных сегментов для несуществующего грузоместа",
                GET_SEGMENTS_PATH
            ),
            Arguments.of(
                "Получение истории статусов для несуществующего грузоместа",
                GET_STATUS_HISTORY_PATH
            )
        );
    }

    @Test
    @DisplayName("Фильтрация по идентификатору сегмента на гриде истории маршрутов")
    void getRouteHistoryForSegment() {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given()
                .params(
                    "boxId",
                    1L,
                    "segmentId",
                    1L
                )
                .get(GET_ROUTES_HISTORY_PATH),
            "json/admin/get-grid-for-return-box-detail/route_history_for_segment.json"
        );
    }
}
