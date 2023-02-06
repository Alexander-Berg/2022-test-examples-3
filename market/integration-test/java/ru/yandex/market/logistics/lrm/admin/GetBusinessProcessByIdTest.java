package ru.yandex.market.logistics.lrm.admin;

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

import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@DisplayName("Поиск фоновых процессов в админке по идентификатору")
@ParametersAreNonnullByDefault
@DatabaseSetup("/database/admin/get-business-process-by-id/before/prepare.xml")
class GetBusinessProcessByIdTest extends AbstractAdminIntegrationTest {
    private static final String GET_BUSINESS_PROCESS_STATES_PATH = "/admin/business-process-states/";

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успех")
    void success(
        @SuppressWarnings("unused") String name,
        long processId,
        String responsePath
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given().get(GET_BUSINESS_PROCESS_STATES_PATH + processId),
            responsePath
        );
    }

    @Nonnull
    private static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of("Все поля", 1, "json/admin/get-business-process-by-id/all_fields.json"),
            Arguments.of("Только обязательные поля", 2, "json/admin/get-business-process-by-id/required_fields.json")
        );
    }

    @Test
    @DisplayName("Процесс не найден")
    void processNotFound() {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.given().get(GET_BUSINESS_PROCESS_STATES_PATH + 100),
            "Failed to find BUSINESS_PROCESS with id 100"
        );
    }
}
