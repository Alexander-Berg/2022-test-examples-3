package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class ResultCorrectionReasonControllerTest extends ControllerTest {

    @Test
    public void testGetResultCorrectionReasons() throws Exception {
        mockMvc.perform(get("/result-correction-reasons"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$.length()").value(6));
    }

}
