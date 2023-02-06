package ru.yandex.market.logistics.lrm.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

import static ru.yandex.market.logistics.lrm.admin.LrmPlugin.SLUG_RETURN_CONTROL_POINTS;

@DisplayName("Получение таблиц для детальной карточки контрольной точки")
@DatabaseSetup("/database/admin/get-grid-for-control-point-detail/before/prepare.xml")
@ParametersAreNonnullByDefault
class GetGridForControlPointDetailTest extends AbstractAdminIntegrationTest {

    private static final String GET_CONTROL_POINT_STATUS_HISTORY_PATH =
        "/admin/" + SLUG_RETURN_CONTROL_POINTS + "/status-history";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успех")
    void success(
        @SuppressWarnings("unused") String displayName,
        String path,
        Long controlPointId,
        String expectedJson
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given()
                .params("controlPointId", controlPointId)
                .get(path),
            expectedJson
        );
    }

    @Nonnull
    private static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of(
                "История статусов: у контрольной точки есть история статусов",
                GET_CONTROL_POINT_STATUS_HISTORY_PATH,
                1L,
                "json/admin/get-grid-for-control-point-detail/has_status_history.json"
            ),
            Arguments.of(
                "История статусов: у контрольной точки нет истории статусов",
                GET_CONTROL_POINT_STATUS_HISTORY_PATH,
                2L,
                "json/admin/get-grid-for-control-point-detail/no_status_history.json"
            )
        );
    }
}
