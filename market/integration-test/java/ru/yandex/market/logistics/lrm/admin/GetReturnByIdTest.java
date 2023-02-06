package ru.yandex.market.logistics.lrm.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

@ParametersAreNonnullByDefault
@DisplayName("Получение детальной карточки возврата по его идентификатору")
class GetReturnByIdTest extends AbstractAdminIntegrationTest {

    private static final String GET_RETURN_BY_ID_PATH = "/admin/returns/";

    @Test
    @DisplayName("Возврат с заданным идентификатором не найден")
    void returnNotFound() {
        RestAssuredTestUtils.assertNotFoundError(
            getReturn(10000L),
            "Failed to find RETURN with ids [10000]"
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/database/admin/get-return-by-id/before/prepare.xml")
    @DisplayName("Получение возврата по идентификатору")
    void getReturnById(
        @SuppressWarnings("unused") String displayName,
        Long returnId,
        String expectedJsonPath,
        @Nullable Long logisticsPointId
    ) {
        RestAssuredTestUtils.assertJsonResponse(getReturn(returnId), expectedJsonPath);
        verifyLmsGetLogisticsPoint(logisticsPointId);
    }

    @Nonnull
    private static Stream<Arguments> getReturnById() {
        return Stream.of(
            Arguments.of(
                "Получение возврата со всеми полями на детальной карточке",
                1L,
                "json/admin/get-return-by-id/all_fields_filled.json",
                111L
            ),
            Arguments.of(
                "Получение возврата с минимальным набором полей",
                2L,
                "json/admin/get-return-by-id/required_fields.json",
                null
            )
        );
    }

    @Nonnull
    private Response getReturn(Long id) {
        return RestAssured.get(GET_RETURN_BY_ID_PATH + id);
    }
}
