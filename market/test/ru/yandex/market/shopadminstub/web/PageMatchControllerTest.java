package ru.yandex.market.shopadminstub.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class PageMatchControllerTest extends AbstractTestBase {

    @Test
    public void shouldReturnOk() throws Exception {
        mockMvc.perform(get("/pagematch"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
