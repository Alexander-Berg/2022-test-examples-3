package ru.yandex.market.fulfillment.wrap.core.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.fulfillment.wrap.core.service.HealthService;
import ru.yandex.market.fulfillment.wrap.core.service.HealthServiceImpl;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest extends SoftAssertionSupport {

    @InjectMocks
    private HealthController controller;

    @Spy
    private HealthService healthService = new HealthServiceImpl();

    private MockMvc mockMvc;

    @BeforeEach
    void createControllerWithMocks() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testOkResponse() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));
    }
}
