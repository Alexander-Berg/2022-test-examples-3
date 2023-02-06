package ru.yandex.market.logistics.lom.service.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.MessageAware;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/service/async/before/all_queue_tasks.xml")
class AsyncResultErrorResponseProcessingSucceededTest extends AsyncResultServiceTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("errorPayloadArguments")
    @DisplayName("Успешно обработан ответ о неуспехе операции")
    void errorResponseProcessingSucceeded(
        QueueType queueType,
        MockHttpServletRequestBuilder requestBuilder,
        MessageAware payload
    ) throws Exception {
        AsyncResultService<?, ?> service = getService(queueType);
        doReturn(ProcessingResult.success()).when(service).processError(any(), any());
        performRequest(requestBuilder, payload)
            .andExpect(status().isOk())
            .andExpect(noContent());
        assertBusinessProcessStateUpdated(
            queueType,
            BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED,
            payload.getMessage()
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("errorPayloadArguments")
    @DisplayName("Ответ о неуспехе операции не обработан")
    void errorResponseProcessingUnprocessed(
        QueueType queueType,
        MockHttpServletRequestBuilder requestBuilder,
        MessageAware payload
    ) throws Exception {
        AsyncResultService<?, ?> service = getService(queueType);
        doReturn(ProcessingResult.success()).when(service).processError(any(), any());
        performRequest(requestBuilder, payload)
            .andExpect(status().isOk())
            .andExpect(noContent());
        assertBusinessProcessStateUpdated(
            queueType,
            BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_SUCCEEDED,
            payload.getMessage()
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("errorPayloadArguments")
    @DisplayName("При обработке ответа операция была перезапущена, статус БП не поменялся")
    @ExpectedDatabase(
        value = "/service/async/before/all_queue_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorResponseProcessingRestarted(
        QueueType queueType,
        MockHttpServletRequestBuilder requestBuilder,
        MessageAware payload
    ) throws Exception {
        AsyncResultService<?, ?> service = getService(queueType);
        doReturn(ProcessingResult.restarted()).when(service).processError(any(), any());
        performRequest(requestBuilder, payload)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }
}
