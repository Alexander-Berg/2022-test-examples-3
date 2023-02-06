package ru.yandex.market.logistics.lrm.admin.controller;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.admin.LrmPlugin;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.admin.model.request.CreateReturnSegmentStatusDto;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.logistics.lrm.utils.ValidationErrorFields;

@ParametersAreNonnullByDefault
@DisplayName("Создание статуса возвратного сегмента")
@DatabaseSetup("/database/admin/create-segment-status/before/prepare.xml")
class AdminReturnSegmentStatusHistoryControllerTest extends AbstractIntegrationTest {
    private static final LocalDateTime FIXED_LOCAL_DATE_TIME = LocalDateTime.of(2021, 11, 11, 11, 12, 13);
    private static final String CREATE_SLUG = "/admin/" + LrmPlugin.SLUG_RETURN_SEGMENTS_STATUS_HISTORY;

    @Test
    @DisplayName("Статус успешно создан")
    @ExpectedDatabase(
        value = "/database/admin/create-segment-status/after/status_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCreation() {
        RestAssuredTestUtils.assertSuccessResponseWithBody(
            createStatus(getValidCreateDto(), 1),
            "1"
        );
    }

    @Test
    @DisplayName("Создание статуса для несуществующего сегмента")
    void failedDueNonexistentSegment() {
        RestAssuredTestUtils.assertNotFoundError(
            createStatus(getValidCreateDto(), 1234567),
            "Failed to find RETURN_SEGMENT with ids [1234567]"
        );
    }

    private CreateReturnSegmentStatusDto getValidCreateDto() {
        return getValidDtoBuilder().build();
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибки валидации в теле запроса на создание маршрута")
    void failedDueValidationOnDtoErrors(
        String errorField,
        CreateReturnSegmentStatusDto createRequest
    ) {
        RestAssuredTestUtils.assertValidationErrors(
            createStatus(createRequest, 1),
            ValidationErrorFields.builder()
                .code("NotNull")
                .field(errorField)
                .message("Обязательно для заполнения")
                .objectName("createReturnSegmentStatusDto")
                .errorsPrefix(ValidationErrorFields.DEFAULT_ERRORS_PREFIX)
                .build()
        );
    }

    @Nonnull
    private static Stream<Arguments> failedDueValidationOnDtoErrors() {
        return Stream.of(
            Arguments.of(
                "message",
                getValidDtoBuilder().message(null).build()
            ),
            Arguments.of(
                "changeTime",
                getValidDtoBuilder().changeTime(null).build()
            ),
            Arguments.of(
                "status",
                getValidDtoBuilder().status(null).build()
            )
        );
    }

    @Nonnull
    private Response createStatus(CreateReturnSegmentStatusDto createDto, int segmentId) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createDto)
            .queryParam("parentId", segmentId)
            .post(CREATE_SLUG);
    }

    @Nonnull
    private static CreateReturnSegmentStatusDto.CreateReturnSegmentStatusDtoBuilder getValidDtoBuilder() {
        return CreateReturnSegmentStatusDto.builder()
            .changeTime(FIXED_LOCAL_DATE_TIME)
            .status(AdminReturnSegmentStatus.IN)
            .message(FormattedTextObject.of("Очень нужен статус"));
    }
}
