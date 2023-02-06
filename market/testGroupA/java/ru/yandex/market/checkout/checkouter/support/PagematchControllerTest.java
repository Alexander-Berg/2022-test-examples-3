package ru.yandex.market.checkout.checkouter.support;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.market.checkout.application.AbstractWebTestBase;

public class PagematchControllerTest extends AbstractWebTestBase {
    @Test
    public void printResult() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/pagematch"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

}
