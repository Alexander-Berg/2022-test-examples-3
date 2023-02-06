package ru.yandex.market.logistics.lom.service.async;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.exception.http.base.BadRequestException;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/service/async/before/all_queue_tasks.xml")
class AsyncResultErrorResponseProcessingFailedTest extends AsyncResultServiceTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("errorPayloadArguments")
    @DisplayName("Ошибка обработки ответа о неуспехе операции")
    void errorResponseProcessingFailed(
        QueueType queueType,
        MockHttpServletRequestBuilder requestBuilder,
        Object payload
    ) throws Exception {
        AsyncResultService<?, ?> service = getService(queueType);
        doThrow(new BadRequestException(MESSAGE)).when(service).processError(any(), any());
        performRequest(requestBuilder, payload)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(MESSAGE));
        assertBusinessProcessStateUpdated(
            queueType,
            BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED,
            MESSAGE
        );
    }
}
