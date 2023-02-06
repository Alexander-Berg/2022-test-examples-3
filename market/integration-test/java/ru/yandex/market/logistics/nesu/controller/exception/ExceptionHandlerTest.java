package ru.yandex.market.logistics.nesu.controller.exception;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработка ошибок с помощью ControllerExceptionHandler")
class ExceptionHandlerTest extends AbstractContextualTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Ошибка при запросе в другой компонент")
    void httpTemplateExceptionHandlerTest() throws Exception {
        String body = "{\"some\": \"content\"}";
        HttpTemplateException expectedException = new HttpTemplateException(404, body);
        when(lomClient.getOrder(
            1L,
            Set.of(OptionalOrderPart.CHANGE_REQUESTS, OptionalOrderPart.UPDATE_RECIPIENT_ENABLED)
        ))
            .thenThrow(expectedException);

        mockMvc.perform(
            get("/back-office/orders/1")
                .param("userId", "1")
                .param("senderId", "1")
                .param("shopId", "1")
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json(body));
    }

    @Test
    @DisplayName("Неподдерживаемый метод")
    void unsupportedMethod() throws Exception {
        mockMvc.perform(post("/api/ping"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(errorMessage("Request method 'POST' not supported"));
    }
}
