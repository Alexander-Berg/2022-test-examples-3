package ru.yandex.market.checkout.pushapi.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;

public class PageMatcherControllerTest extends AbstractWebTestBase {
    @Test
    public void shouldWork() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pagematch"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
