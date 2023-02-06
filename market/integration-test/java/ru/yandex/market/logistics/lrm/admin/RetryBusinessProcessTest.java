package ru.yandex.market.logistics.lrm.admin;

import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.dbqueue.dto.ActionEntityIdDto;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lrm.service.task.BusinessProcessStateService;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.logistics.lrm.utils.ValidationErrorFields;

@ParametersAreNonnullByDefault
@DisplayName("Перевыставление одного бизнес-процесса")
@DatabaseSetup("/database/admin/retry-business-process/before/prepare.xml")
class RetryBusinessProcessTest extends AbstractAdminIntegrationTest {

    private static final String RETRY_BUSINESS_PROCESS_PATH = "/admin/business-process-states/retry";
    private static final String INCORRECT_RETRY_MESSAGE_PREFIX =
        "Бизнес-процессы, которые не были перевыставлены: \n";

    private static final Instant NOW = Instant.parse("2021-11-11T11:11:11.00Z");

    @Autowired
    private BusinessProcessStateService businessProcessStateService;

    @BeforeEach
    void setUp() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @DisplayName("Бизнес-процесс не подходит под условия для перевыставления")
    void incorrectForRetry(
        @SuppressWarnings("unused") String displayName,
        Long businessProcessId,
        String expectedMessage
    ) {
        RestAssuredTestUtils.assertUnprocessableEntityServerError(
            retryProcess(businessProcessId),
            INCORRECT_RETRY_MESSAGE_PREFIX + expectedMessage
        );
    }

    @Nonnull
    private static Stream<Arguments> incorrectForRetry() {
        return Stream.of(
            Arguments.of(
                "Процесс не найден",
                10000L,
                "Процесс с идентификатором 10000 не найден."
            ),
            Arguments.of(
                "Процесс не в статусе FAIL",
                1L,
                "Процесс 1 не в ошибочном статусе."
            ),
            Arguments.of(
                "Существует активный процесс с такими же данными",
                2L,
                "Для данных процесса 2 существует процесс не в ошибочном статусе."
            ),
            Arguments.of(
                "Процесс не в ошибочном статусе и не задана queueName",
                3L,
                """
                    Процесс 3 не в ошибочном статусе.
                    Для данных процесса 3 существует процесс не в ошибочном статусе.\
                    """
            ),
            Arguments.of(
                "Процесс не в статусе FAIL",
                7L,
                "Процесс 7 не в ошибочном статусе."
            )
        );
    }

    @Test
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    @DisplayName("Ошибка валидации: передан null вместо ид")
    void validationError() {
        RestAssuredTestUtils.assertValidationErrors(
            retryProcess(null),
            ValidationErrorFields.builder()
                .field("id")
                .code("NotNull")
                .message("must not be null")
                .objectName("actionEntityIdDto")
                .errorsPrefix(ValidationErrorFields.DEFAULT_ERRORS_PREFIX)
                .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/database/admin/retry-business-process/after/create-storage-unit-in-sc-retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное перевыставление создания грузомест в СЦ")
    void successRetryForCreateStorageUnitsInSc() {
        RestAssuredTestUtils.assertSuccessResponseWithEmptyBody(retryProcess(5L));
        softly.assertThat(businessProcessStateService.getById(5L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
    }

    @Test
    @ExpectedDatabase(
        value = "/database/admin/retry-business-process/after/delete-segment-in-sc-retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное перевыставление удаления сегмента СЦ")
    void successRetryForDeleteSegmentInSc() {
        RestAssuredTestUtils.assertSuccessResponseWithEmptyBody(retryProcess(4L));
        softly.assertThat(businessProcessStateService.getById(4L).getStatus())
            .isEqualTo(BusinessProcessStatus.RETRIED);
    }

    @Nonnull
    private Response retryProcess(@Nullable Long processId) {
        return RestAssured.given()
            .accept(ContentType.JSON)
            .body(new ActionEntityIdDto().setId(processId))
            .contentType(ContentType.JSON)
            .post(RETRY_BUSINESS_PROCESS_PATH);
    }
}
