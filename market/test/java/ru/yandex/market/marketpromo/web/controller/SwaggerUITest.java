package ru.yandex.market.marketpromo.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ru.yandex.market.marketpromo.test.MockedWebTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SwaggerUITest extends MockedWebTestBase {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void shouldRespondOnHttpPageCall() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().is2xxSuccessful());
    }
}
