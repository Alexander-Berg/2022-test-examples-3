package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.dto.IdDto;
import ru.yandex.market.logistics.nesu.jobs.producer.DropoffRegistrationProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.resourceNotFoundMatcher;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;

@DatabaseSetup("/repository/dropoff/dropoff_registration_state.xml")
@DisplayName("Перевыставление регистрации дропоффа")
@ParametersAreNonnullByDefault
class DropoffRegistrationRetryTest extends AbstractContextualTest {

    @Autowired
    private DropoffRegistrationProducer dropoffRegistrationProducer;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
        doNothing().when(dropoffRegistrationProducer).produceTask(anyLong());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(dropoffRegistrationProducer, lmsClient);
    }

    @Test
    @DisplayName("Успешное перевыставление")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/dropoff_registration_state_retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void retrySucceed() throws Exception {
        when(lmsClient.getLogisticsPoint(300L)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .id(300L)
                .name(String.format("point%d", 300L))
                .partnerId(301L)
                .schedule(Set.of(
                    new ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(21, 0)),
                    new ScheduleDayResponse(1L, 2, LocalTime.of(10, 0), LocalTime.of(21, 0))
                ))
                .build()
        ));

        retry(new IdDto().setId(3L))
            .andExpect(status().isOk())
            .andExpect(content().string("4"));

        verify(lmsClient).getLogisticsPoint(300L);
        verify(dropoffRegistrationProducer).produceTask(4);
    }

    @Test
    @DisplayName("Перевыставление регистрации без ошибки")
    void retryRegistrationWithoutError() throws Exception {
        retry(new IdDto().setId(2L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Dropoff registration status is not ERROR"));
    }

    @Test
    @DisplayName("Неверный ID регистрации")
    void retryIdNotFound() throws Exception {
        retry(new IdDto().setId(4L))
            .andExpect(status().isNotFound())
            .andExpect(resourceNotFoundMatcher(ResourceType.DROPOFF_REGISTRATION_STATE, List.of(4L)));
    }

    @Test
    @DisplayName("Не указан ID регистрации")
    void retryEmptyId() throws Exception {
        retry(new IdDto())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError("id", "must not be null", "idDto", "NotNull")));
    }

    @Nonnull
    private ResultActions retry(IdDto idDto) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.POST,
            "/admin/dropoff-registration/retry",
            idDto
        ));
    }

}
