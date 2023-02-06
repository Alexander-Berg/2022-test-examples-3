package ru.yandex.market.logistics.nesu.base;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.entity.DisableDropoffReason;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractDropoffDisablingTest extends AbstractContextualTest {

    private static final Long LOGISTIC_POINT_ID = 1L;
    private static final LocalDateTime START_CLOSING_DATE_TIME = LocalDateTime.of(2021, 11, 27, 17, 0);
    private static final LocalDateTime CLOSING_DATE_TIME = LocalDateTime.of(2021, 12, 6, 17, 0);
    protected static final DisableDropoffReason REASON = new DisableDropoffReason()
        .setAdminId(1L)
        .setCode("UNPROFITABLE")
        .setReadableName("Нерентабельность");

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @BeforeEach
    void setup() {
        mockYtDropoffRepository(true);
        clock.setFixed(Instant.parse("2021-11-26T17:00:00Z"), CommonsConstants.MSK_TIME_ZONE);
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибка валидации дат")
    void failValidationWithValidator(
        @SuppressWarnings("unused") String name,
        Long logisticPointId,
        LocalDateTime startClosingDateTime,
        LocalDateTime closingDateTime,
        ValidationErrorData.ValidationErrorDataBuilder error
    ) throws Exception {
        createDisablingDropoffRequest(
            logisticPointId,
            startClosingDateTime,
            closingDateTime
        )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error.forObject(getObjectName())));
    }

    @Nonnull
    private static Stream<Arguments> failValidationWithValidator() {
        return Stream.of(
            Arguments.of(
                "StartClosingDate из прошлого",
                LOGISTIC_POINT_ID,
                LocalDateTime.of(2021, 11, 25, 17, 0),
                CLOSING_DATE_TIME,
                fieldErrorBuilder(
                    "startClosingDate",
                    "Date must not be in the past",
                    "ValidDropoffDisablingDates"
                )
            ),
            Arguments.of(
                "ClosingDate из прошлого",
                LOGISTIC_POINT_ID,
                START_CLOSING_DATE_TIME,
                LocalDateTime.of(2021, 11, 25, 17, 0),
                fieldErrorBuilder(
                    "closingDate",
                    "Date must not be in the past",
                    "ValidDropoffDisablingDates"
                )
            ),
            Arguments.of(
                "ClosingDate раньше StartClosingDate",
                LOGISTIC_POINT_ID,
                START_CLOSING_DATE_TIME,
                LocalDateTime.of(2021, 11, 27, 16, 0),
                fieldErrorBuilder(
                    "closingDate",
                    "Closing date must not be before start closing date",
                    "ValidDropoffDisablingDates"
                )
            )
        );
    }

    @Test
    @DisplayName("Ошибка: уже существует заявка в нетерминальном статусе.")
    @DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
    void failValidationAsMethod() throws Exception {
        createDisablingDropoffRequest(
            LOGISTIC_POINT_ID,
            START_CLOSING_DATE_TIME,
            CLOSING_DATE_TIME
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "There is one more request with id=1 in non-terminal status for logisticPoint with id=1"
            ));
    }

    @Test
    @DisplayName("Ошибка: не существует такой причины отключения.")
    void notExistsReason() throws Exception {
        createDisablingDropoffRequest(
            LOGISTIC_POINT_ID,
            START_CLOSING_DATE_TIME,
            CLOSING_DATE_TIME
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(getNotFoundReasonMessage()));
    }

    @Nonnull
    protected abstract String getNotFoundReasonMessage();

    @Test
    @DatabaseSetup("/repository/dropoff/disabling/before/reason.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/success_create_new_disabling_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание заявки на отключение дропоффа")
    void successCreateDisablingDropoffRequest() throws Exception {
        createDisablingDropoffRequest(
            LOGISTIC_POINT_ID,
            START_CLOSING_DATE_TIME,
            CLOSING_DATE_TIME
        )
            .andExpect(status().isOk())
            .andExpect(responseContent());
    }

    @Test
    @DisplayName("Ошибка: точка не дропофф")
    void errorLogisticPointIsNotDropoff() throws Exception {
        mockYtDropoffRepository(false);
        createDisablingDropoffRequest(
            LOGISTIC_POINT_ID,
            START_CLOSING_DATE_TIME,
            CLOSING_DATE_TIME
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Logistic point with id=1 is not dropoff"));
    }

    private void mockYtDropoffRepository(boolean isDropoff) {
        doReturn(isDropoff ? List.of(new Object()) : List.of()).when(yqlJdbcTemplate).queryForList(
            "SELECT * FROM `is_dropoff` WHERE id=1"
        );
    }

    @Nonnull
    protected abstract ResultActions createDisablingDropoffRequest(
        Long logisticPointId,
        LocalDateTime startClosingDateTime,
        LocalDateTime closingDateTime
    ) throws Exception;

    @Nonnull
    protected abstract ResultMatcher responseContent();

    @Nonnull
    protected abstract String getObjectName();
}
