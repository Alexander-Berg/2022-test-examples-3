package ru.yandex.market.logistics.lrm.admin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.dbqueue.dto.ActionDto;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lrm.service.task.BusinessProcessStateService;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.logistics.lrm.utils.ValidationErrorFields;

@ParametersAreNonnullByDefault
@DisplayName("Перевыставление нескольких бизнес-процессов по их идентификаторам")
@DatabaseSetup("/database/admin/retry-business-process/before/prepare.xml")
class RetryBusinessProcessesTest extends AbstractAdminIntegrationTest {

    private static final String RETRY_BUSINESS_PROCESSES_PATH = "/admin/business-process-states/retry-list";
    private static final String INCORRECT_RETRY_MESSAGE_PREFIX =
        "Бизнес-процессы, которые не были перевыставлены: \n";

    @Autowired
    private BusinessProcessStateService businessProcessStateService;

    @Test
    @ExpectedDatabase(
        value = "/database/admin/retry-business-process/after/multiply_retried_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Все бизнес-процессы успешно перевыставлены: переданы несколько идентификаторов")
    void allRetriedSuccess() {
        RestAssuredTestUtils.assertSuccessResponseWithEmptyBody(retryProcesses(Set.of(4L, 5L)));
        softly.assertThat(businessProcessStateService.getById(4L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
        softly.assertThat(businessProcessStateService.getById(5L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
    }

    @Test
    @ExpectedDatabase(
        value = "/database/admin/retry-business-process/after/delete-segment-in-sc-retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("1 идентификатор: успешное перевыставление удаления сегмента СЦ")
    void successRetryForDeleteSegmentInSc() {
        RestAssuredTestUtils.assertSuccessResponseWithEmptyBody(retryProcesses(Set.of(4L)));
        softly.assertThat(businessProcessStateService.getById(4L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
    }

    @Test
    @ExpectedDatabase(
        value = "/database/admin/retry-business-process/after/create-storage-unit-in-sc-retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("1 идентификатор: успешное перевыставление создания грузомест в СЦ")
    void successRetryForCreateStorageUnitsInSc() {
        RestAssuredTestUtils.assertSuccessResponseWithEmptyBody(retryProcesses(Set.of(5L)));
        softly.assertThat(businessProcessStateService.getById(5L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
    }

    @MethodSource
    @DisplayName("Ошибки валидации тела запроса")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void validationErrors(
        @SuppressWarnings("unused") String displayName,
        Set<Long> ids,
        ValidationErrorFields expectedValidationErrors
    ) {
        RestAssuredTestUtils.assertValidationErrors(
            retryProcesses(ids),
            expectedValidationErrors
        );
    }

    @Nonnull
    private static Stream<Arguments> validationErrors() {
        return Stream.of(
            Arguments.of(
                "Передан пустой сет идентификаторов",
                Set.of(),
                ValidationErrorFields.builder()
                    .field("ids")
                    .code("NotEmpty")
                    .objectName("actionDto")
                    .message("must not be empty")
                    .errorsPrefix(ValidationErrorFields.DEFAULT_ERRORS_PREFIX)
                    .build()
            ),
            Arguments.of(
                "Передан null в сете идентификаторов",
                new HashSet<>(Arrays.asList(1L, null, 2L)),
                ValidationErrorFields.builder()
                    .field("ids[]")
                    .code("NotNull")
                    .objectName("actionDto")
                    .message("must not be null")
                    .errorsPrefix(ValidationErrorFields.DEFAULT_ERRORS_PREFIX)
                    .build()
            )
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/database/admin/retry-business-process/after/multiply_retried_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Были перевыставлены только корректные для перевыставления процессы")
    void retriedOnlyCorrectForRetryProcesses() {
        RestAssuredTestUtils.assertUnprocessableEntityServerError(
            retryProcesses(Set.of(7L, 5L, 4L, 3L, 2L, 1L, 100000L)),
            INCORRECT_RETRY_MESSAGE_PREFIX +
                """
                    Процесс 1 не в ошибочном статусе.
                    Для данных процесса 2 существует процесс не в ошибочном статусе.
                    Процесс 3 не в ошибочном статусе.
                    Для данных процесса 3 существует процесс не в ошибочном статусе.
                    Процесс 7 не в ошибочном статусе.
                    Процесс с идентификатором 100000 не найден.\
                    """
        );

        softly.assertThat(businessProcessStateService.getById(4L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
        softly.assertThat(businessProcessStateService.getById(5L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
    }

    @Nonnull
    private Response retryProcesses(Set<Long> ids) {
        return RestAssured.given()
            .accept(ContentType.JSON)
            .body(new ActionDto().setIds(ids))
            .contentType(ContentType.JSON)
            .post(RETRY_BUSINESS_PROCESSES_PATH);
    }
}
