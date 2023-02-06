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

@DisplayName("Получение детальной карточки грузоместа")
@ParametersAreNonnullByDefault
class GetReturnBoxByIdTest extends AbstractAdminIntegrationTest {

    private static final String BOX_BY_ID_PATH = "admin/returns/boxes/";

    @Test
    @DisplayName("Коробка с заданным идентификатором не найдена")
    void entityForDetailNotFound() {
        RestAssuredTestUtils.assertNotFoundError(
            getBox(123456789L),
            "Failed to find RETURN_BOX with id 123456789"
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/database/admin/get-return-box-by-id/before/prepare.xml")
    @DisplayName("Получение грузоместа по идентификатору")
    void getBoxById(
        @SuppressWarnings("unused") String displayName,
        Long boxId,
        String expectedJsonPath,
        @Nullable Long destinationLogisticsPointId
    ) {
        RestAssuredTestUtils.assertJsonResponse(getBox(boxId), expectedJsonPath);
        verifyLmsGetLogisticsPoint(destinationLogisticsPointId);
    }

    @Nonnull
    private static Stream<Arguments> getBoxById() {
        return Stream.of(
            Arguments.of(
                "Получение грузоместа со всеми полями на детальной карточке",
                1L,
                "json/admin/get-return-box-by-id/all_fields_filled.json",
                123L
            ),
            Arguments.of(
                "Получение грузоместа с минимальным набором полей",
                2L,
                "json/admin/get-return-box-by-id/required_fields_filled.json",
                null
            )
        );
    }

    @Nonnull
    private Response getBox(Long boxId) {
        return RestAssured.get(BOX_BY_ID_PATH + boxId);
    }
}
