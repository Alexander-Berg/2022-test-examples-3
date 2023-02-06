package ru.yandex.market.logistics.lrm.admin;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

import static ru.yandex.market.logistics.lrm.admin.LrmPlugin.SLUG_RETURN_CONTROL_POINTS;

@DisplayName("Получение детальной карточки для контрольной точки по ее идентификатору")
@DatabaseSetup("/database/admin/get-control-point-by-id/before/prepare.xml")
@ParametersAreNonnullByDefault
class GetControlPointByIdTest extends AbstractAdminIntegrationTest {

    private static final String GET_CONTROL_POINT_PATH = "/admin/" + SLUG_RETURN_CONTROL_POINTS;

    @Test
    @DisplayName("Контрольная точка не найдена")
    void controlPointNotFound() {
        RestAssuredTestUtils.assertNotFoundError(
            getControlPoint(10000L),
            "Failed to find CONTROL_POINT with id 10000"
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успех")
    void success(
        @SuppressWarnings("unused") String displayName,
        Long controlPointId,
        String expectedJsonPath,
        Long logisticsPointId,
        Set<Long> partnerIds
    ) {
        RestAssuredTestUtils.assertJsonResponse(getControlPoint(controlPointId), expectedJsonPath);
        verifyLmsGetLogisticsPoints(Set.of(logisticsPointId));
        verifyLmsGetPartners(partnerIds);
    }

    @Nonnull
    private static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of(
                "Контрольная точка со всеми полями на детальной карточке",
                1L,
                "json/admin/get-control-point-by-id/all_fields_filled.json",
                1200L,
                Set.of(300L, 400L)
            ),
            Arguments.of(
                "Контрольная точка с минимальным набором полей",
                2L,
                "json/admin/get-control-point-by-id/required_fields.json",
                1600L,
                Set.of(559L)
            )
        );
    }

    @Nonnull
    private Response getControlPoint(Long id) {
        return RestAssured.get(GET_CONTROL_POINT_PATH + "/" + id);
    }
}
