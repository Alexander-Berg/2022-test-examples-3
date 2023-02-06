package ru.yandex.market.logistics.lrm.admin;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.specification.MultiPartSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

@DisplayName("Массовое изменение возвратного склада")
@ParametersAreNonnullByDefault
class CreateChangeReturnWarehouseTaskTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2022-01-02T03:04:05.00Z");
    private static final String PATH = "/admin/returns/create-change-return-warehouse-task";

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидный EXCEL-файл")
    @MethodSource
    @ExpectedDatabase(
        value = "/database/admin/create-change-return-warehouse-task/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validExcel(String displayName, String fileLocation) {
        RestAssuredTestUtils.assertSuccessResponseWithEmptyBody(
            RestAssured.given()
                .multiPart(getMultipartFromFile(fileLocation))
                .when()
                .post(PATH)
        );
    }

    @Nonnull
    static Stream<Arguments> validExcel() {
        return Stream.of(
            Arguments.of(
                "Валидный файл",
                "xlsx/change-return-warehouse/valid.xlsx"
            ),
            Arguments.of(
                "Лишний столбец",
                "xlsx/change-return-warehouse/valid_extra_column.xlsx"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Невалидный EXCEL-файл")
    @MethodSource
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidExcel(String displayName, String fileLocation, String errorMessage) {
        RestAssuredTestUtils.assertInternalServerError(
            RestAssured.given()
                .multiPart(getMultipartFromFile(fileLocation))
                .when()
                .post(PATH),
            errorMessage
        );
    }

    @Nonnull
    static Stream<Arguments> invalidExcel() {
        return Stream.of(
            Arguments.of(
                "Неправильный тип данных",
                "xlsx/change-return-warehouse/invalid_wrong_datatype.xlsx",
                "Cannot deserialize value of type `long` from String \"wrong\": "
                    + "not a valid `long` value\n at [Source: UNKNOWN; byte offset: #UNKNOWN] "
                    + "(through reference chain: ru.yandex.market.logistics.lrm.queue.payload."
                    + "ChangeReturnWarehousePayload$ChangeReturnWarehouseRequest$"
                    + "ChangeReturnWarehouseRequestBuilder[\"logisticPointId\"])"
            ),
            Arguments.of(
                "Пропущенный столбец",
                "xlsx/change-return-warehouse/invalid_missed_column.xlsx",
                "Both returnId and logisticPointId columns are required"
            ),
            Arguments.of(
                "Отсутствует строка заголовка",
                "xlsx/change-return-warehouse/invalid_missed_header.xlsx",
                "Got an empty file. Probably header row is missed or invalid"
            )
        );
    }

    @Test
    @DisplayName("Не EXCEL-файл")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notExcel() {
        RestAssuredTestUtils.assertInternalServerError(
            RestAssured.given()
                .multiPart(getMultipart("test-content".getBytes(StandardCharsets.UTF_8)))
                .when()
                .post(PATH),
            "No valid entries or contents found, this is not a valid OOXML (Office Open XML) file"
        );
    }

    @Nonnull
    private MultiPartSpecification getMultipart(byte[] content) {
        return new MultiPartSpecBuilder(content)
            .mimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .controlName("request")
            .fileName("data.xlsx")
            .build();
    }

    @Nonnull
    private MultiPartSpecification getMultipartFromFile(String location) {
        byte[] content = IntegrationTestUtils.extractFileContentInBytes(location);
        return getMultipart(content);
    }
}
