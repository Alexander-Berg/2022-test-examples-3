package ru.yandex.market.logistics.cs.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.service.LmsSnapshotService;

class LmsSnapshotControllerTest extends AbstractIntegrationTest {

    @MockBean
    private LmsSnapshotService lmsSnapshotService;

    @Test
    @DisplayName("Тест вызова ручки снапшотирования")
    void testSnapshot() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/snapshot"))
            .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(lmsSnapshotService).snapshot();
    }

}
