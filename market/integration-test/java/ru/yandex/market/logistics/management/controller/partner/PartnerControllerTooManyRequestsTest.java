package ru.yandex.market.logistics.management.controller.partner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.exception.TooManyRequestsException;
import ru.yandex.market.logistics.management.util.InFlightRequestLimiter;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PartnerControllerTooManyRequestsTest extends AbstractContextualTest {

    @MockBean(name = "partnersSearchLimiter")
    private InFlightRequestLimiter requestLimiter;

    @Test
    @DisplayName("429 код ошибки при превышении лимита запросов при поиске партнеров")
    void searchPartnersAboveRequestLimit() throws Exception {
        doThrow(new TooManyRequestsException("message")).when(requestLimiter).wrap(any());

        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partners/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isTooManyRequests());
    }
}
