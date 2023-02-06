package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
@DisplayName("Перевыставление подзадачи запроса на отключение дропоффа")
class DropoffDisablingSubtaskRetryTest extends AbstractContextualTest {

    @Test
    @DisplayName("Перевыставление подзадачи")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/disabling_dropoff_subtask_retry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelRequest() throws Exception {
        mockMvc.perform(post("/admin/dropoff-disabling/subtask/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1}")
            )
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибки статусов")
    @MethodSource
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/before/disabling_dropoff_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void retrySubtaskStatusError(
        @SuppressWarnings("unused") String displayName,
        long id,
        String errorMessage
    ) throws Exception {
        mockMvc.perform(post("/admin/dropoff-disabling/subtask/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": " + id + "}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Nonnull
    private static Stream<Arguments> retrySubtaskStatusError() {
        return Stream.of(
            Arguments.of("Подзадача не в статусе ERROR", 0L, "Subtask status is not ERROR"),
            Arguments.of("Запрос в терминальном статусе", 6L, "Request status must not be terminal")
        );
    }

    @Test
    @DisplayName("Ошибка. Подзадача не найдена")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/before/disabling_dropoff_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelRequestNotFound() throws Exception {
        mockMvc.perform(post("/admin/dropoff-disabling/subtask/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 7}")
            )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DROPOFF_DISABLING_SUBTASK] with ids [7]"));
    }
}
