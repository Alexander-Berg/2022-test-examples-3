package ru.yandex.market.logistics.lom.service.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/service/async/before/all_queue_tasks.xml")
class AsyncResultSuccessResponseProcessingSucceededTest extends AsyncResultServiceTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("successPayloadArguments")
    @DisplayName("Успешно обработан ответ об успехе операции")
    void successResponseProcessingSucceeded(
        QueueType queueType,
        MockHttpServletRequestBuilder requestBuilder,
        Object payload
    ) throws Exception {
        AsyncResultService<?, ?> service = getService(queueType);
        doReturn(ProcessingResult.success()).when(service).processSuccess(any(), any());
        performRequest(requestBuilder, payload)
            .andExpect(status().isOk())
            .andExpect(noContent());
        assertBusinessProcessStateUpdated(queueType, BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_SUCCEEDED);
    }
}
