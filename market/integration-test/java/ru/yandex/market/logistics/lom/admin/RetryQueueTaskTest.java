package ru.yandex.market.logistics.lom.admin;

import java.time.Instant;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты на ручку перевыставления задач из админки")
class RetryQueueTaskTest extends AbstractRetryQueueTaskTest {

    @Test
    @Override
    @SneakyThrows
    @DisplayName("Перевыставить задачу бизнес-процесса, для которого срок последнего изменения еще не истек")
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    void retryTaskErrorNonExpiredProcess() {
        clock.setFixed(Instant.parse("2019-11-13T12:00:30.00Z"), ZoneOffset.UTC);
        retry()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Перевыставление процесса 1 невозможно, т.к. ещё не истекло время (60с) "
                    + "с момента последнего изменения статуса процесса (2019-11-13T12:00:00Z)"
            ));
    }

    @Test
    @Override
    @SneakyThrows
    @DatabaseSetup(
        "/service/business_process_state/ds_create_order_external_1_success_response_processing_succeeded.xml"
    )
    @DisplayName("Перевыставить задачу бизнес-процесса, который находится в терминальном успешном статусе")
    void retryTaskErrorNonTerminalStatusProcess() {
        clock.setFixed(Instant.parse("2019-11-13T12:30:00.00Z"), ZoneOffset.UTC);
        retry()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Перевыставление процесса 1 невозможно, т.к. он находится в успешном терминальном статусе "
                    + "(SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)"
            ));
    }

    @Nonnull
    @Override
    @SneakyThrows
    ResultActions retry() {
        return mockMvc.perform(
            post("/admin/business-processes/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}")
        );
    }
}

